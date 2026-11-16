import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CategoriasApi } from './categorias-api';
import { ContasApi } from './contas-api';
import { InicioApi } from './inicio-api';
import { LancamentosApi } from './lancamentos-api';
import { OrcamentoApi } from './orcamento-api';
import { TransferenciasApi } from './transferencias-api';

describe('serviços da API', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function pedido(metodo: string, url: string) {
    return http.expectOne((r) => r.method === metodo && r.urlWithParams === url);
  }

  it('lançamentos', () => {
    const api = TestBed.inject(LancamentosApi);
    api.listar({ dataInicio: '2026-11-01', contaId: undefined }).subscribe();
    pedido(
      'GET',
      '/api/lancamentos-financeiros?sort=dataCompetencia,desc&dataInicio=2026-11-01',
    ).flush({});
    api.buscar('l1').subscribe();
    pedido('GET', '/api/lancamentos-financeiros/l1').flush({});
    api.atualizar('l1', {} as never).subscribe();
    pedido('PUT', '/api/lancamentos-financeiros/l1').flush({});
    api.pagar('l1', '2026-11-15').subscribe();
    expect(pedido('PATCH', '/api/lancamentos-financeiros/l1/pagar').request.body).toEqual({
      dataPagamento: '2026-11-15',
    });
    api.cancelar('l1').subscribe();
    pedido('DELETE', '/api/lancamentos-financeiros/l1').flush(null);
    api.fatura('c3', 2026, 11).subscribe();
    pedido('GET', '/api/lancamentos-financeiros/faturas?contaId=c3&ano=2026&mes=11').flush({});
    api.pagarFatura({} as never).subscribe();
    pedido('PATCH', '/api/lancamentos-financeiros/faturas/pagar').flush({});
  });

  it('contas', () => {
    const api = TestBed.inject(ContasApi);
    let contas: unknown;
    api.listar(true).subscribe((c) => (contas = c));
    pedido('GET', '/api/contas?ativa=true&size=200&sort=nome').flush({ content: [1] });
    expect(contas).toEqual([1]);
    api.resumo().subscribe();
    pedido('GET', '/api/contas/resumo').flush({});
    api.criar({} as never).subscribe();
    pedido('POST', '/api/contas').flush({});
    api.atualizar('c1', {} as never).subscribe();
    pedido('PUT', '/api/contas/c1').flush({});
    api.corrigirSaldoInicial('c1', 10).subscribe();
    pedido('PATCH', '/api/contas/c1/corrigir-saldo-inicial').flush({});
    api.inativar('c1').subscribe();
    pedido('DELETE', '/api/contas/c1').flush(null);
    api.ativar('c1').subscribe();
    pedido('PATCH', '/api/contas/c1/ativar').flush({});
  });

  it('categorias', () => {
    const api = TestBed.inject(CategoriasApi);
    api.listar().subscribe();
    pedido('GET', '/api/categorias?size=500&sort=nome').flush({ content: [] });
    api.criar({} as never).subscribe();
    pedido('POST', '/api/categorias').flush({});
    api.atualizar('x', {} as never).subscribe();
    pedido('PUT', '/api/categorias/x').flush({});
    api.inativar('x').subscribe();
    pedido('DELETE', '/api/categorias/x').flush(null);
    api.ativar('x').subscribe();
    pedido('PATCH', '/api/categorias/x/ativar').flush({});
  });

  it('transferências, orçamento e início', () => {
    const transferencias = TestBed.inject(TransferenciasApi);
    transferencias.listar('2026-11-01', '2026-11-30').subscribe();
    pedido(
      'GET',
      '/api/transferencias?dataInicio=2026-11-01&dataFim=2026-11-30&size=200&sort=dataTransferencia,desc',
    ).flush({ content: [] });
    transferencias.criar({} as never).subscribe();
    pedido('POST', '/api/transferencias').flush({});
    transferencias.cancelar('t1').subscribe();
    pedido('DELETE', '/api/transferencias/t1').flush(null);

    const orcamento = TestBed.inject(OrcamentoApi);
    orcamento.doMes(2026, 11).subscribe();
    pedido('GET', '/api/orcamentos/2026/11').flush({});
    orcamento.doAno(2026).subscribe();
    pedido('GET', '/api/orcamentos/2026').flush({});
    orcamento.definir(2026, 11, []).subscribe();
    pedido('PUT', '/api/orcamentos/2026/11').flush({});
    orcamento.repetir('x', 10, ['2026-12'], true).subscribe();
    pedido('POST', '/api/orcamentos/repetir').flush({});
    orcamento.copiar(2026, 10, ['2026-11'], false).subscribe();
    pedido('POST', '/api/orcamentos/2026/10/copiar').flush({});

    TestBed.inject(InicioApi).buscar(2026, 11).subscribe();
    pedido('GET', '/api/dashboard-financeiro/inicio?ano=2026&mes=11').flush({});
  });
});
