import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Confirmation, ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { Menu } from 'primeng/menu';
import {
  TRANSFERENCIA,
  lancamento,
  pagina,
  provedoresDeTeste,
} from '../../../../../testing/dados-de-teste';
import { Lancamentos } from './lancamentos';

const URL_LANCAMENTOS =
  '/api/lancamentos-financeiros?sort=dataCompetencia,desc&dataInicio=2026-11-01&dataFim=2026-11-30&size=500';
const URL_TRANSFERENCIAS =
  '/api/transferencias?dataInicio=2026-11-01&dataFim=2026-11-30&size=200&sort=dataTransferencia,desc';

const LANCAMENTOS = [
  lancamento({ id: 'pago', descricao: 'Mercado', dataCompetencia: '2026-11-15' }),
  lancamento({
    id: 'luz',
    descricao: 'Conta de luz',
    nomeCategoria: 'Energia',
    valor: 200,
    status: 'ATRASADO',
    dataPagamento: null,
    dataCompetencia: '2026-11-08',
  }),
  lancamento({
    id: 'salario',
    descricao: 'Salário',
    tipo: 'RECEITA',
    valor: 8000,
    dataCompetencia: '2026-11-05',
  }),
  lancamento({
    id: 'cinema',
    descricao: 'Cinema',
    status: 'PENDENTE',
    dataCompra: '2026-11-02',
    dataPagamento: null,
    dataCompetencia: '2026-11-12',
  }),
  lancamento({ id: 'velho', descricao: 'Cancelado', status: 'CANCELADO', valor: 999 }),
];

describe('Lancamentos', () => {
  let fixture: ComponentFixture<Lancamentos>;
  let http: HttpTestingController;
  let confirmar: (c: Confirmation) => void;

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
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(Lancamentos);
    const confirmacao = fixture.debugElement.injector.get(ConfirmationService);
    confirmar = (c) => c.accept?.();
    vi.spyOn(confirmacao, 'confirm').mockImplementation((c) => {
      confirmar(c);
      return confirmacao;
    });
    TestBed.tick();
  });

  afterEach(() => http.verify());

  const el = () => fixture.nativeElement as HTMLElement;
  const texto = () => el().textContent!.replace(/\s+/g, ' ');
  const tela = () => fixture.componentInstance;

  async function responder(lancamentos = LANCAMENTOS, transferencias = [TRANSFERENCIA]) {
    TestBed.tick();
    http.expectOne(URL_LANCAMENTOS).flush(pagina(lancamentos));
    http.expectOne(URL_TRANSFERENCIAS).flush(pagina(transferencias));
    await fixture.whenStable();
  }

  function acoesDe(lanc: (typeof LANCAMENTOS)[number]): MenuItem[] {
    const menu = { toggle: vi.fn() } as unknown as Menu;
    tela()['abrirAcoes'](menu, new Event('click'), lanc);
    return tela()['acoes']();
  }

  it('agrupa por dia, soma sem os cancelados e mostra as transferências', async () => {
    await responder();

    expect(texto()).toContain('Hoje');
    expect(texto()).toContain('Domingo, 08/11/2026');
    expect(texto()).toContain('R$ 8.000,00');
    expect(texto()).toContain('R$ 300,00');
    expect(texto()).toContain('compra em 02/11');
    expect(texto()).toContain('Banco Azul → Carteira');
    expect(tela()['totais']()).toEqual({ receitas: 8000, despesas: 400, saldo: 7600 });
  });

  it('filtra por situação, tipo e busca sem acento', async () => {
    await responder();

    tela()['escolherFiltro']('apagar');
    expect(
      tela()
        ['filtrados']()
        .map((l) => l.id),
    ).toEqual(['luz', 'cinema']);
    tela()['escolherFiltro']('atrasados');
    expect(
      tela()
        ['filtrados']()
        .map((l) => l.id),
    ).toEqual(['luz']);
    tela()['escolherFiltro']('receitas');
    expect(
      tela()
        ['filtrados']()
        .map((l) => l.id),
    ).toEqual(['salario']);
    tela()['escolherFiltro']('despesas');
    expect(tela()['filtrados']()).toHaveLength(4);
    tela()['escolherFiltro']('todos');
    tela()['busca'].set('ENERGIA');
    expect(
      tela()
        ['filtrados']()
        .map((l) => l.id),
    ).toEqual(['luz']);
    tela()['busca'].set('salario');
    expect(
      tela()
        ['filtrados']()
        .map((l) => l.id),
    ).toEqual(['salario']);
    tela()['busca'].set('nada disso');
    await fixture.whenStable();
    expect(texto()).toContain('Nenhum lançamento com esse filtro.');
  });

  it('começa no filtro que veio na rota', async () => {
    fixture.componentRef.setInput('filtro', 'atrasados');
    await responder();
    expect(
      tela()
        ['filtrados']()
        .map((l) => l.id),
    ).toEqual(['luz']);
  });

  it('oferece pagar só o que está em aberto e paga com a data escolhida', async () => {
    await responder();
    expect(acoesDe(LANCAMENTOS[0]).map((a) => a.label)).toEqual(['Editar', 'Cancelar lançamento']);
    expect(acoesDe(LANCAMENTOS[4])).toEqual([]);

    acoesDe(LANCAMENTOS[1])[0].command!({});
    expect(tela()['aPagar']()?.id).toBe('luz');
    tela()['dataDoPagamento'].set(new Date(2026, 10, 14));
    tela()['confirmarPagamento']();
    const pedido = http.expectOne('/api/lancamentos-financeiros/luz/pagar');
    expect(pedido.request.body).toEqual({ dataPagamento: '2026-11-14' });
    pedido.flush({});
    expect(tela()['aPagar']()).toBeNull();
    await responder();
  });

  it('avisa se o pagamento falha e permite fechar o diálogo', async () => {
    await responder();
    const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
    tela()['abrirPagamento'](LANCAMENTOS[1]);
    tela()['confirmarPagamento']();
    http
      .expectOne('/api/lancamentos-financeiros/luz/pagar')
      .flush({ message: 'Falhou.' }, { status: 400, statusText: 'x' });
    expect(avisos).toHaveBeenCalledWith(expect.objectContaining({ detail: 'Falhou.' }));
    tela()['fecharPagamento']();
    tela()['confirmarPagamento']();
  });

  it('edita pelo menu de ações', async () => {
    await responder();
    acoesDe(LANCAMENTOS[0])[0].command!({});
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/lancar', 'pago']);
  });

  it('cancela lançamento e transferência depois de confirmar', async () => {
    await responder();
    acoesDe(LANCAMENTOS[0])[1].command!({});
    http.expectOne('/api/lancamentos-financeiros/pago').flush(null);
    await responder();

    tela()['confirmarCancelamentoDaTransferencia'](TRANSFERENCIA);
    http.expectOne('/api/transferencias/t1').flush(null);
    await responder();
  });

  it('avisa quando o cancelamento falha', async () => {
    await responder();
    const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
    tela()['confirmarCancelamento'](LANCAMENTOS[1]);
    http
      .expectOne('/api/lancamentos-financeiros/luz')
      .flush(null, { status: 500, statusText: 'x' });
    tela()['confirmarCancelamentoDaTransferencia']({ ...TRANSFERENCIA, cancelada: true });
    http.expectOne('/api/transferencias/t1').flush(null, { status: 500, statusText: 'x' });
    expect(avisos).toHaveBeenCalledTimes(2);
  });

  it('mostra vazio, erro e tenta de novo', async () => {
    await responder([], []);
    expect(texto()).toContain('Nenhum lançamento em novembro');

    tela()['tentarDeNovo']();
    TestBed.tick();
    http.expectOne(URL_LANCAMENTOS).flush(null, { status: 500, statusText: 'x' });
    http.expectOne(URL_TRANSFERENCIAS);
    await fixture.whenStable();
    expect(texto()).toContain('Não foi possível carregar os lançamentos de novembro.');
  });
});
