import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import {
  CATEGORIAS,
  CONTAS,
  inicio,
  lancamento,
  pagina,
  provedoresDeTeste,
} from '../../../../../testing/dados-de-teste';
import { Lancar } from './lancar';

describe('Lancar', () => {
  let fixture: ComponentFixture<Lancar>;
  let http: HttpTestingController;
  let router: Router;

  function criar(id?: string): void {
    fixture = TestBed.createComponent(Lancar);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    TestBed.tick();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ...provedoresDeTeste(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  const el = () => fixture.nativeElement as HTMLElement;
  const texto = () => el().textContent!.replace(/\s+/g, ' ');
  const tela = () => fixture.componentInstance;

  async function carregarNovo(): Promise<void> {
    criar();
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina(CONTAS));
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
    http.expectOne('/api/dashboard-financeiro/inicio').flush(inicio());
    await fixture.whenStable();
  }

  function botao(rotulo: string): HTMLButtonElement {
    return Array.from(el().querySelectorAll<HTMLButtonElement>('button')).find((b) =>
      b.textContent?.includes(rotulo),
    )!;
  }

  it('abre com a conta sugerida e as categorias mais usadas', async () => {
    await carregarNovo();

    expect(tela()['contaId']()).toBe('c2');
    const chips = Array.from(el().querySelectorAll('.chip')).map((c) => c.textContent?.trim());
    expect(chips).toEqual(['Supermercado', 'Padaria']);
    expect(texto()).toContain('Já paguei');
  });

  it('não envia sem valor nem categoria', async () => {
    await carregarNovo();
    botao('Lançar e fazer outro').click();
    await fixture.whenStable();

    expect(texto()).toContain('Informe um valor maior que zero.');
    expect(texto()).toContain('Escolha uma categoria.');
  });

  it('lança uma despesa paga em três toques e volta para a lista', async () => {
    await carregarNovo();
    const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
    tela()['valor'].set(45.9);
    botao('Supermercado').click();
    tela()['salvar'](false);

    const pedido = http.expectOne('/api/lancamentos-financeiros');
    expect(pedido.request.body).toEqual({
      contaId: 'c2',
      categoriaId: 'cat-mercado',
      descricao: 'Supermercado',
      tipo: 'DESPESA',
      valor: 45.9,
      dataCompetencia: '2026-11-15',
      dataVencimento: '2026-11-15',
      dataPagamento: '2026-11-15',
      observacao: null,
    });
    pedido.flush(lancamento());

    expect(router.navigateByUrl).toHaveBeenCalledWith('/lancamentos');
    expect(avisos).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Lançamento salvo' }));
  });

  it('lança e limpa para o próximo, mantendo conta e tipo', async () => {
    await carregarNovo();
    tela()['escolherTipo']('RECEITA');
    tela()['valor'].set(8000);
    tela()['categoriaId'].set('cat-salario');
    tela()['maisDetalhes'].set(true);
    tela()['descricao'].set('  Salário de novembro ');
    tela()['observacao'].set('Empresa X');
    tela()['pago'].set(false);
    tela()['vencimento'].set(new Date(2026, 10, 20));
    await fixture.whenStable();
    expect(texto()).toContain('Já recebi');
    tela()['salvar'](true);

    const pedido = http.expectOne('/api/lancamentos-financeiros');
    expect(pedido.request.body).toEqual(
      expect.objectContaining({
        tipo: 'RECEITA',
        descricao: 'Salário de novembro',
        dataVencimento: '2026-11-20',
        dataPagamento: null,
        observacao: 'Empresa X',
      }),
    );
    pedido.flush(lancamento());

    expect(tela()['valor']()).toBeNull();
    expect(tela()['categoriaId']()).toBeNull();
    expect(tela()['tipo']()).toBe('RECEITA');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('compra no cartão fica pendente e data futura não deixa marcar pago', async () => {
    await carregarNovo();
    tela()['escolherConta']('c3');
    await fixture.whenStable();
    expect(texto()).toContain('Entra na fatura do cartão');
    expect(texto()).toContain('Data da compra');

    tela()['escolherConta']('c1');
    tela()['data'].set(new Date(2026, 10, 30));
    await fixture.whenStable();
    expect(texto()).toContain('Data futura fica pendente.');
    expect(tela()['ficaPago']()).toBe(false);
  });

  it('faz transferência entre contas', async () => {
    await carregarNovo();
    tela()['escolherTipo']('TRANSFERENCIA');
    tela()['escolherTipo']('TRANSFERENCIA');
    tela()['valor'].set(300);
    tela()['salvar'](false);
    await fixture.whenStable();
    expect(texto()).toContain('Escolha a conta de destino.');

    tela()['contaDestinoId'].set('c1');
    tela()['escolherConta']('c1');
    expect(tela()['contaDestinoId']()).toBeNull();
    tela()['contaDestinoId'].set('c2');
    tela()['salvar'](false);

    expect(http.expectOne('/api/transferencias').request.body).toEqual({
      contaOrigemId: 'c1',
      contaDestinoId: 'c2',
      valor: 300,
      dataTransferencia: '2026-11-15',
      descricao: 'Transferência para Carteira',
    });
  });

  it('mostra o erro da API ao salvar', async () => {
    await carregarNovo();
    const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
    tela()['valor'].set(10);
    tela()['categoriaId'].set('cat-mercado');
    tela()['salvar'](false);
    http
      .expectOne('/api/lancamentos-financeiros')
      .flush({ message: 'A conta informada está inativa.' }, { status: 400, statusText: 'Bad' });

    expect(avisos).toHaveBeenCalledWith(
      expect.objectContaining({ detail: 'A conta informada está inativa.' }),
    );
    expect(tela()['salvando']()).toBe(false);
  });

  it('sem sugestão usa a primeira conta e segue sem as mais usadas', async () => {
    criar();
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina(CONTAS));
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
    http
      .expectOne('/api/dashboard-financeiro/inicio')
      .flush(null, { status: 500, statusText: 'x' });
    await fixture.whenStable();

    expect(tela()['contaId']()).toBe('c1');
    expect(el().querySelector('.chip')).toBeNull();
  });

  it('edita um lançamento existente', async () => {
    criar('l9');
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina(CONTAS));
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
    http.expectOne('/api/lancamentos-financeiros/l9').flush(
      lancamento({
        id: 'l9',
        contaId: 'c3',
        dataCompra: '2026-11-02',
        status: 'PENDENTE',
        observacao: 'parcela',
        dataPagamento: null,
      }),
    );
    await fixture.whenStable();

    expect(texto()).toContain('Editar lançamento');
    expect(tela()['observacao']()).toBe('parcela');
    tela()['salvar'](false);
    const pedido = http.expectOne('/api/lancamentos-financeiros/l9');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body.dataCompetencia).toBe('2026-11-02');
    pedido.flush(lancamento());
  });

  it('mostra erro quando não consegue abrir e tenta de novo', async () => {
    criar();
    http
      .expectOne('/api/contas?ativa=true&size=200&sort=nome')
      .flush(null, { status: 0, statusText: '' });
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome');
    http.expectOne('/api/dashboard-financeiro/inicio');
    await fixture.whenStable();
    expect(texto()).toContain('Não foi possível abrir o formulário.');

    tela()['tentarDeNovo']();
    TestBed.tick();
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina(CONTAS));
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
    http.expectOne('/api/dashboard-financeiro/inicio').flush(inicio());
    await fixture.whenStable();
    expect(texto()).toContain('Novo lançamento');
  });
});
