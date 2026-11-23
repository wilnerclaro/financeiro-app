import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { Select } from 'primeng/select';
import {
  CATEGORIAS,
  CONTAS,
  categoria,
  inicio,
  lancamento,
  pagina,
  provedoresDeTeste,
} from '../../../../../testing/dados-de-teste';
import { Lancar } from './lancar';
import { ContaDialogo } from '../../../contas/components/conta-dialogo/conta-dialogo';

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

  function escolherCategoria(grupoId: string, subcategoriaId: string | null = null): void {
    tela()['escolherGrupo'](grupoId);
    tela()['subcategoriaId'].set(subcategoriaId);
  }

  function botao(rotulo: string): HTMLButtonElement {
    return Array.from(el().querySelectorAll<HTMLButtonElement>('button')).find((b) =>
      b.textContent?.includes(rotulo),
    )!;
  }

  it('abre com a conta sugerida e a categoria em lista suspensa, sem chips', async () => {
    await carregarNovo();

    expect(tela()['contaId']()).toBe('c2');
    expect(el().querySelector('.chip')).toBeNull();
    expect(el().querySelector('#grupo')).not.toBeNull();
    expect(texto()).toContain('Escolha a categoria antes');
    expect(texto()).toContain('Já paguei');
  });

  it('agrupa as categorias do tipo escolhido pela categoria pai', async () => {
    await carregarNovo();

    expect(
      tela()
        ['grupos']()
        .map((g) => [g.label, g.subcategorias.map((s) => s.label)]),
    ).toEqual([
      ['Alimentação', ['Padaria', 'Supermercado']],
      ['Moradia', ['Conta de luz']],
      ['Pet', []],
    ]);
    tela()['escolherTipo']('RECEITA');
    expect(
      tela()
        ['grupos']()
        .map((g) => g.label),
    ).toEqual(['Receitas ativas']);
  });

  it('a subcategoria depende do grupo e é opcional', async () => {
    await carregarNovo();
    escolherCategoria('cat-alimentacao', 'cat-padaria');
    expect(tela()['categoriaId']()).toBe('cat-padaria');

    tela()['escolherGrupo']('cat-moradia');
    expect(tela()['subcategoriaId']()).toBeNull();
    expect(tela()['categoriaId']()).toBe('cat-moradia');
    await fixture.whenStable();
    expect(texto()).toContain('Opcional. Sem ela, entra direto em Moradia.');

    tela()['escolherGrupo']('cat-pet');
    await fixture.whenStable();
    expect(tela()['subcategorias']()).toEqual([]);
    expect(texto()).toContain('Nenhuma ainda');
  });

  it('cadastra a categoria que não achou na pesquisa e já deixa escolhida', async () => {
    await carregarNovo();
    const seletor = { hide: vi.fn() } as unknown as Select;
    tela()['filtroDoGrupo'].set('Educação');
    tela()['cadastrarGrupo'](seletor);
    expect(seletor.hide).toHaveBeenCalled();
    expect(tela()['novaCategoria']()).toEqual({
      tipo: 'DESPESA',
      categoriaPaiId: null,
      nome: 'Educação',
    });
    expect(
      tela()
        ['gruposParaNovaCategoria']()
        .map((g) => g.label),
    ).toEqual(['Alimentação', 'Moradia', 'Pet']);

    const educacao = categoria({
      id: 'cat-educacao',
      categoriaPaiId: null,
      nomeCategoriaPai: null,
      nome: 'Educação',
    });
    tela()['categoriaCriada'](educacao);
    expect(tela()['novaCategoria']()).toBeNull();
    expect(tela()['grupoId']()).toBe('cat-educacao');

    tela()['filtroDaSubcategoria'].set('Cursos');
    tela()['cadastrarSubcategoria'](seletor);
    expect(tela()['novaCategoria']()).toEqual(
      expect.objectContaining({ categoriaPaiId: 'cat-educacao', nome: 'Cursos' }),
    );
    tela()['categoriaCriada'](
      categoria({
        id: 'cat-cursos',
        categoriaPaiId: 'cat-educacao',
        nomeCategoriaPai: 'Educação',
        nome: 'Cursos',
      }),
    );
    expect(tela()['grupoId']()).toBe('cat-educacao');
    expect(tela()['subcategoriaId']()).toBe('cat-cursos');
    expect(
      tela()
        ['grupos']()
        .find((g) => g.value === 'cat-educacao')?.subcategorias,
    ).toEqual([{ label: 'Cursos', value: 'cat-cursos' }]);
    expect(tela()['nomeDaCategoria']()).toBe('Cursos');
  });

  it('subcategoria cujo grupo está inativo vira grupo com o caminho no nome', async () => {
    criar();
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina(CONTAS));
    http
      .expectOne('/api/categorias?ativa=true&size=500&sort=nome')
      .flush(pagina(CATEGORIAS.filter((c) => c.id !== 'cat-moradia')));
    http.expectOne('/api/dashboard-financeiro/inicio').flush(inicio());
    await fixture.whenStable();

    expect(
      tela()
        ['grupos']()
        .map((g) => g.label),
    ).toContain('Moradia › Conta de luz');
  });

  it('não envia sem valor nem categoria', async () => {
    await carregarNovo();
    botao('Lançar e fazer outro').click();
    await fixture.whenStable();

    expect(texto()).toContain('Informe um valor maior que zero.');
    expect(texto()).toContain('Escolha uma categoria.');
  });

  it('repete somente a conclusão quando o lançamento já foi salvo', async () => {
    await carregarNovo();
    fixture.componentRef.setInput('configuracaoInicial', 'true');
    tela()['valor'].set(50);
    escolherCategoria('cat-alimentacao', 'cat-mercado');
    tela()['salvar'](false);
    http.expectOne('/api/lancamentos-financeiros').flush(lancamento());
    const configuracao = {
      rendaPassivaDesejada: 6000,
      etapa: 'LANCAMENTO',
      pausado: false,
      ano: 2026,
      mes: 11,
    };
    http.expectOne('/api/configuracao-inicial').flush(configuracao);
    http
      .expectOne('/api/configuracao-inicial')
      .flush({}, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();
    expect(texto()).toContain('Seu lançamento foi registrado');
    tela()['salvar'](false);
    http.expectNone('/api/lancamentos-financeiros');
    botao('Tentar concluir novamente').click();
    http.expectOne('/api/configuracao-inicial').flush(configuracao);
    const conclusao = http.expectOne('/api/configuracao-inicial');
    expect(conclusao.request.method).toBe('PUT');
    expect(conclusao.request.body.etapa).toBe('CONCLUIDO');
    conclusao.flush({ ...configuracao, etapa: 'CONCLUIDO' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/configuracao-inicial', {
      replaceUrl: true,
    });
    http.expectNone('/api/lancamentos-financeiros');
  });

  it('cadastra a primeira conta sem perder o lançamento preenchido', async () => {
    criar();
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina([]));
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
    http.expectOne('/api/dashboard-financeiro/inicio').flush(inicio());
    await fixture.whenStable();
    tela()['valor'].set(1500);
    escolherCategoria('cat-alimentacao', 'cat-mercado');
    tela()['descricao'].set('Compra planejada');

    botao('Cadastre uma conta primeiro').click();
    await fixture.whenStable();
    const dialogo = fixture.debugElement.query(By.directive(ContaDialogo))
      .componentInstance as ContaDialogo;
    expect(dialogo.modo()).toBe('criar');
    dialogo['nome'].set('Minha conta');
    dialogo['salvar']();
    const pedido = http.expectOne('/api/contas');
    expect(pedido.request.method).toBe('POST');
    pedido.flush({ ...CONTAS[0], nome: 'Minha conta' });
    await fixture.whenStable();

    expect(dialogo.modo()).toBeNull();
    expect(tela()['contaId']()).toBe(CONTAS[0].id);
    expect(tela()['opcoesDeConta']()).toContainEqual({ label: 'Minha conta', value: CONTAS[0].id });
    expect(tela()['valor']()).toBe(1500);
    expect(tela()['categoriaId']()).toBe('cat-mercado');
    expect(tela()['descricao']()).toBe('Compra planejada');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('lança uma despesa paga em três toques e volta para a lista', async () => {
    await carregarNovo();
    const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
    tela()['valor'].set(45.9);
    escolherCategoria('cat-alimentacao', 'cat-mercado');
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
    escolherCategoria('cat-renda', 'cat-salario');
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
    expect(tela()['grupoId']()).toBeNull();
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
    escolherCategoria('cat-alimentacao', 'cat-mercado');
    tela()['salvar'](false);
    http
      .expectOne('/api/lancamentos-financeiros')
      .flush({ message: 'A conta informada está inativa.' }, { status: 400, statusText: 'Bad' });

    expect(avisos).toHaveBeenCalledWith(
      expect.objectContaining({ detail: 'A conta informada está inativa.' }),
    );
    expect(tela()['salvando']()).toBe(false);
  });

  it('sem sugestão usa a primeira conta', async () => {
    criar();
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina(CONTAS));
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
    http
      .expectOne('/api/dashboard-financeiro/inicio')
      .flush(null, { status: 500, statusText: 'x' });
    await fixture.whenStable();

    expect(tela()['contaId']()).toBe('c1');
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
    expect(tela()['grupoId']()).toBe('cat-alimentacao');
    expect(tela()['subcategoriaId']()).toBe('cat-mercado');
    tela()['salvar'](false);
    const pedido = http.expectOne('/api/lancamentos-financeiros/l9');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body.dataCompetencia).toBe('2026-11-02');
    pedido.flush(lancamento());
  });

  it('edita um lançamento feito direto no grupo', async () => {
    criar('l8');
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina(CONTAS));
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
    http
      .expectOne('/api/lancamentos-financeiros/l8')
      .flush(lancamento({ id: 'l8', categoriaId: 'cat-moradia', nomeCategoria: 'Moradia' }));
    await fixture.whenStable();

    expect(tela()['grupoId']()).toBe('cat-moradia');
    expect(tela()['subcategoriaId']()).toBeNull();
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
