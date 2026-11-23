import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { INICIO_VAZIO, inicio, provedoresDeTeste } from '../../../../../testing/dados-de-teste';
import { Inicio } from './inicio';

const URL = '/api/dashboard-financeiro/inicio';

describe('Inicio', () => {
  let fixture: ComponentFixture<Inicio>;
  let http: HttpTestingController;

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
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(Inicio);
    TestBed.tick();
    http.expectOne('/api/configuracao-inicial').flush({
      rendaPassivaDesejada: 6000,
      etapa: 'CONCLUIDO',
      pausado: false,
      ano: 2026,
      mes: 11,
    });
  });

  afterEach(() => http.verify());

  const el = () => fixture.nativeElement as HTMLElement;
  const texto = () => el().textContent!.replace(/\s+/g, ' ');

  async function responder(corpo: object, ano = 2026, mes = 11): Promise<void> {
    TestBed.tick();
    http.expectOne(`${URL}?ano=${ano}&mes=${mes}`).flush(corpo);
    await fixture.whenStable();
  }

  it('mostra esqueleto enquanto carrega e depois os indicadores do mês', async () => {
    expect(el().querySelector('p-skeleton')).toBeTruthy();
    await responder(inicio());

    expect(texto()).toContain('Resumo de novembro');
    expect(texto()).toContain('37,5% do que entrou · meta 30%');
    expect(texto()).toContain('2,6% a mais que outubro');
    expect(texto()).toContain('5,7% a menos que outubro');
    expect(texto()).toContain('83,3%');
    expect(texto()).toContain('Restam R$ 1.000,00 do previsto');
    expect(texto()).toContain('Acima do previsto');
    expect(texto()).toContain('A renda passiva cobre 21,2% das despesas do mês');
    expect(texto()).toContain('1,2 pontos a mais que outubro');
  });

  it('retoma automaticamente apenas a configuração não pausada', async () => {
    await responder(inicio());
    fixture.componentInstance['configuracao'].reload();
    TestBed.tick();
    http.expectOne('/api/configuracao-inicial').flush({
      rendaPassivaDesejada: null,
      etapa: 'BOAS_VINDAS',
      pausado: false,
      ano: 2026,
      mes: 11,
    });
    await fixture.whenStable();
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/configuracao-inicial', {
      replaceUrl: true,
    });
  });

  it('oferece retomar sem redirecionar quando o usuário pausou', async () => {
    await responder(inicio());
    fixture.componentInstance['configuracao'].reload();
    TestBed.tick();
    http.expectOne('/api/configuracao-inicial').flush({
      rendaPassivaDesejada: 6000,
      etapa: 'ORCAMENTO',
      pausado: true,
      ano: 2026,
      mes: 11,
    });
    await fixture.whenStable();
    expect(texto()).toContain('Retomar configuração');
    expect(TestBed.inject(Router).navigateByUrl).not.toHaveBeenCalled();
  });

  it('lista o que precisa de atenção e manda compra do cartão para a fatura', async () => {
    await responder(inicio());

    expect(texto()).toContain('Conta de luz está atrasado');
    expect(texto()).toContain('Cinema vence em breve');
    expect(texto()).toContain('Supermercado passou do previsto');
    expect(texto()).toContain('Conta de luz está no limite do previsto');
    expect(el().querySelector('[aria-label="Pagar Conta de luz hoje"]')).toBeTruthy();
    expect(el().querySelector('[aria-label="Pagar Cinema hoje"]')).toBeNull();
    expect(texto()).toContain('Ver fatura');
  });

  it('paga uma conta atrasada com a data de hoje e recarrega', async () => {
    await responder(inicio());
    const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');

    el().querySelector<HTMLButtonElement>('[aria-label="Pagar Conta de luz hoje"]')!.click();
    const pedido = http.expectOne('/api/lancamentos-financeiros/luz/pagar');
    expect(pedido.request.body).toEqual({ dataPagamento: '2026-11-15' });
    pedido.flush({});
    await responder(inicio());

    expect(avisos).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Pago' }));
  });

  it('avisa quando o pagamento falha', async () => {
    await responder(inicio());
    const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');

    fixture.componentInstance['pagar'](inicio().paraPagar.itens[0]);
    http
      .expectOne('/api/lancamentos-financeiros/luz/pagar')
      .flush(
        { message: 'O lançamento informado já está pago.' },
        { status: 400, statusText: 'Bad Request' },
      );

    expect(avisos).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        detail: 'O lançamento informado já está pago.',
      }),
    );
  });

  it('alterna o gráfico da sobra para tabela', async () => {
    await responder(inicio());
    expect(el().querySelector('.grafico')?.getAttribute('aria-label')).toContain(
      '1 mês ficou abaixo da meta de 30%',
    );

    fixture.componentInstance['vista'].set('Tabela');
    await fixture.whenStable();

    expect(el().querySelector('table')).toBeTruthy();
    expect(texto()).toContain('23,1% · abaixo');
  });

  it('mostra o estado vazio quando o mês não tem movimento', async () => {
    await responder(INICIO_VAZIO);
    expect(texto()).toContain('Nenhum lançamento em novembro');
  });

  it('mostra sem previsto, mês igual ao anterior e independência total', async () => {
    const dados = inicio();
    await responder({
      ...dados,
      sobrou: -100,
      meta30: { ...dados.meta30, atingiu: false },
      comparativoMesAnterior: {
        diferencaReceitas: 0,
        evolucaoReceitas: 0,
        diferencaDespesas: 50,
        evolucaoDespesas: 0,
      },
      orcamento: { ...dados.orcamento, despesasPrevistas: 0, gruposMaisUsados: [] },
      independenciometro: { ...dados.independenciometro, proximoMarco: null, variacao: 0 },
      paraPagar: { total: 0, quantidade: 0, itens: [] },
      categoriasAcimaDoPrevisto: [],
      evolucao: [{ ...dados.evolucao[1] }],
    });

    expect(texto()).toContain('Sem previsto');
    expect(texto()).toContain('Igual a outubro');
    expect(texto()).toContain('R$ 50,00 a mais que outubro');
    expect(texto()).toContain('Tudo em dia');
    expect(texto()).toContain('já cobre todas as despesas');
    expect(texto()).toContain('Definir previsto');
    expect(el().querySelector('.grafico')?.getAttribute('aria-label')).toContain(
      'Todos os meses atingiram',
    );
  });

  it('mostra quanto passou do previsto', async () => {
    const dados = inicio();
    await responder({
      ...dados,
      orcamento: { ...dados.orcamento, restaDasDespesas: -50, percentualDespesasUsado: 1.05 },
      independenciometro: { ...dados.independenciometro, variacao: -0.01 },
    });

    expect(texto()).toContain('R$ 50,00 acima do previsto');
    expect(texto()).toContain('1 ponto a menos que outubro');
  });

  it('mostra o erro e tenta de novo', async () => {
    http.expectOne(`${URL}?ano=2026&mes=11`).flush(null, { status: 500, statusText: 'Erro' });
    await fixture.whenStable();
    expect(texto()).toContain('Não foi possível carregar o resumo de novembro.');

    el().querySelector<HTMLButtonElement>('app-estado-erro button')!.click();
    await responder(inicio());
    expect(texto()).toContain('Sobra do mês');
  });

  it('troca de mês pelo seletor', async () => {
    await responder(inicio());
    el().querySelector<HTMLButtonElement>('[aria-label="Ir para outubro de 2026"]')!.click();
    await responder(inicio({ mes: 10 }), 2026, 10);
    expect(texto()).toContain('Resumo de outubro');
  });
});
