import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import {
  CATEGORIAS,
  CONTAS,
  pagina,
  previstoRealizado,
  provedoresDeTeste,
} from '../../../../../testing/dados-de-teste';
import { ConfiguracaoInicial } from '../../../../shared/api/configuracao-inicial-api';
import { ConfiguracaoInicialPagina } from './configuracao-inicial';

const URL = '/api/configuracao-inicial';
const CONFIG: ConfiguracaoInicial = {
  rendaPassivaDesejada: 6000,
  etapa: 'OBJETIVO',
  pausado: false,
  ano: 2026,
  mes: 11,
};

describe('Configuração inicial', () => {
  let fixture: ComponentFixture<ConfiguracaoInicialPagina>;
  let http: HttpTestingController;
  let router: Router;
  const tela = () => fixture.componentInstance;

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
    fixture = TestBed.createComponent(ConfiguracaoInicialPagina);
  });
  afterEach(() => http.verify());

  async function carregar(config: Partial<ConfiguracaoInicial> = {}) {
    http.expectOne(URL).flush({ ...CONFIG, ...config });
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina(CONTAS));
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
    http.expectOne('/api/orcamentos/2026/11').flush(previstoRealizado());
    await fixture.whenStable();
  }

  function confirmar(etapa: string) {
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.etapa).toBe(etapa);
    req.flush(req.request.body);
  }

  it('valida o objetivo e salva antes de avançar', async () => {
    await carregar({ rendaPassivaDesejada: null });
    tela()['continuar']();
    expect(tela()['erro']()).toContain('maior que zero');
    http.expectNone(URL);
    tela()['objetivo'].set(7000);
    tela()['continuar']();
    expect(tela()['etapa']()).toBe('OBJETIVO');
    confirmar('ORCAMENTO');
    await fixture.whenStable();
    expect(tela()['etapa']()).toBe('ORCAMENTO');
    expect(tela()['configuracao']()?.rendaPassivaDesejada).toBe(7000);
  });

  it('mantém o objetivo e salva pausa antes de navegar', async () => {
    await carregar();
    tela()['objetivo'].set(7500);
    tela()['pausar']();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    const req = http.expectOne(URL);
    expect(req.request.body).toEqual({ ...CONFIG, rendaPassivaDesejada: 7500, pausado: true });
    req.flush(req.request.body);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('grava somente categorias editadas e preserva o mês se a troca falhar', async () => {
    await carregar({ etapa: 'ORCAMENTO' });
    tela()['editar']('cat-mercado', 1250);
    tela()['trocarMes']({ ano: 2026, mes: 12 });
    const pedido = http.expectOne('/api/orcamentos/2026/11');
    expect(pedido.request.body).toEqual({
      itens: [{ categoriaId: 'cat-mercado', valorPrevisto: 1250 }],
    });
    pedido.flush(previstoRealizado());
    http.expectOne('/api/orcamentos/2026/12').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(tela()['mes']()).toEqual({ ano: 2026, mes: 11 });
    expect(tela()['erro']()).toBeTruthy();
    http.expectNone(URL);
  });

  it('mostra receitas, despesas e sobra previstas somando as edições', async () => {
    await carregar({ etapa: 'ORCAMENTO' });
    const base = tela()['previsto']();
    tela()['editar']('cat-mercado', base.despesas + 1000);
    tela()['editar']('cat-salario', null);
    const depois = tela()['previsto']();
    expect(depois.despesas).toBeGreaterThan(base.despesas);
    expect(tela()['sobra']()).toBe(depois.saldo);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Sobra prevista');
    expect(fixture.nativeElement.querySelector('app-previsto-por-grupo')).not.toBeNull();
  });

  it('troca o mês somente depois de carregar e persistir a nova competência', async () => {
    await carregar({ etapa: 'ORCAMENTO' });
    tela()['trocarMes']({ ano: 2027, mes: 1 });
    http.expectOne('/api/orcamentos/2027/1').flush({ ...previstoRealizado(), ano: 2027, mes: 1 });
    expect(tela()['mes']().mes).toBe(11);
    confirmar('ORCAMENTO');
    expect(tela()['mes']()).toEqual({ ano: 2027, mes: 1 });
  });

  it('exige conta e reutiliza uma conta cadastrada sem duplicar', async () => {
    await carregar({ etapa: 'CONTAS' });
    tela()['contas'].set([]);
    tela()['continuar']();
    expect(tela()['erro']()).toContain('primeira conta');
    tela()['abrirConta']();
    expect(tela()['modoConta']()).toBe('criar');
    tela()['contaSalva'](CONTAS[0]);
    tela()['contaSalva']({ ...CONTAS[0], nome: 'Nome atualizado' });
    expect(tela()['contas']()).toHaveLength(1);
    tela()['continuar']();
    confirmar('LANCAMENTO');
    tela()['continuar']();
    confirmar('LANCAMENTO');
    expect(router.navigate).toHaveBeenCalledWith(['/lancar'], {
      queryParams: { configuracaoInicial: true },
    });
  });

  it('conclui sem lançar e permite revisar o objetivo', async () => {
    await carregar({ etapa: 'LANCAMENTO' });
    tela()['concluir']();
    confirmar('CONCLUIDO');
    http.expectNone('/api/lancamentos-financeiros');
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Seu ponto de partida está pronto');
    tela()['continuar']();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
    tela()['irPara']('OBJETIVO');
    confirmar('OBJETIVO');
    tela()['voltar']();
    confirmar('BOAS_VINDAS');
  });

  it('permite tentar novamente após falha de carregamento', async () => {
    http.expectOne(URL).flush({}, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();
    expect(tela()['erroInicial']()).toBe(true);
    tela()['carregar']();
    await carregar();
    expect(tela()['erroInicial']()).toBe(false);
  });
});
