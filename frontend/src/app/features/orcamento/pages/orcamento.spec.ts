import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { UIChart } from 'primeng/chart';
import {
  CATEGORIAS,
  RESUMO_ANUAL,
  pagina,
  previstoRealizado,
  provedoresDeTeste,
  valores,
} from '../../../../testing/dados-de-teste';
import { ResumoAnual } from '../../../shared/api/api.models';
import { OrcamentoAno } from './orcamento-ano/orcamento-ano';
import { OrcamentoMes } from './orcamento-mes/orcamento-mes';
import { OrcamentoPrevisto } from './orcamento-previsto/orcamento-previsto';

describe('Orçamento', () => {
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
  });

  afterEach(() => http.verify());

  const texto = (el: HTMLElement) => el.textContent!.replace(/\s+/g, ' ');

  describe('mês', () => {
    async function abrir(relatorio = previstoRealizado()) {
      const fixture = TestBed.createComponent(OrcamentoMes);
      const confirmacao = fixture.debugElement.injector.get(ConfirmationService);
      vi.spyOn(confirmacao, 'confirm').mockImplementation((c) => {
        c.accept?.();
        return confirmacao;
      });
      TestBed.tick();
      http.expectOne('/api/orcamentos/2026/11').flush(relatorio);
      await fixture.whenStable();
      return fixture;
    }

    function item(categoriaId: string, nome: string, previsto: number, realizado: number) {
      return {
        categoriaId,
        nome,
        fonteReceita: null,
        ativa: true,
        valores: valores(previsto, realizado),
      };
    }

    function grupo(
      categoriaId: string,
      nome: string,
      previsto: number,
      realizado: number,
      subitens: ReturnType<typeof item>[] = [],
    ) {
      return { ...item(categoriaId, nome, previsto, realizado), subitens };
    }

    it('mostra o resumo, o que pede atenção e abre os subitens', async () => {
      const fixture = await abrir();
      const el = fixture.nativeElement as HTMLElement;

      expect(texto(el)).toContain('Orçamento de novembro');
      expect(texto(el)).toContain('acima do previsto');
      expect(texto(el)).toContain('Alimentação passou do previsto');
      expect(texto(el)).toContain('Supermercado puxou a alta');
      expect(texto(el)).toContain('No ritmo');
      expect(texto(el)).not.toContain('Pet');
      expect(texto(el)).toContain('Receitas ativas');
      expect(texto(el)).toContain('Ativa');
      expect(el.querySelector('.subitens')).toBeNull();

      el.querySelector<HTMLButtonElement>('button.alternar[aria-expanded="false"]')!.click();
      await fixture.whenStable();
      expect(texto(el.querySelector('.subitens')!)).toContain('Supermercado');
      fixture.componentInstance['alternar']('cat-alimentacao');
      expect(fixture.componentInstance['abertos']().size).toBe(0);
    });

    it('abre e recolhe todos os grupos e esconde subitem zerado', async () => {
      const relatorio = previstoRealizado();
      relatorio.despesas.grupos[0].subitens.push(item('cat-padaria', 'Padaria', 0, 0));
      relatorio.despesas.grupos[1] = grupo('cat-moradia', 'Moradia', 200, 0, [
        item('cat-luz', 'Conta de luz', 200, 0),
        item('cat-gas', 'Gás', 0, 0),
      ]);
      const fixture = await abrir(relatorio);
      const el = fixture.nativeElement as HTMLElement;
      const botao = () =>
        [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
          /Abrir tudo|Recolher tudo/.test(b.textContent!),
        )!;

      expect(texto(botao())).toContain('Abrir tudo');
      botao().click();
      await fixture.whenStable();
      expect(el.querySelectorAll('.subitens').length).toBe(2);
      expect(texto(el)).toContain('Conta de luz');
      expect(texto(el)).not.toContain('Padaria');
      expect(texto(el)).not.toContain('Gás');
      expect(texto(botao())).toContain('Recolher tudo');

      botao().click();
      await fixture.whenStable();
      expect(el.querySelector('.subitens')).toBeNull();
    });

    it('avisa grupo no limite, subitem acima e gasto sem previsto', async () => {
      const relatorio = previstoRealizado();
      relatorio.despesas.grupos = [
        grupo('cat-alimentacao', 'Alimentação', 1000, 950, [
          item('cat-mercado', 'Supermercado', 600, 700),
          item('cat-feira', 'Feira', 400, 250),
        ]),
        grupo('cat-educacao', 'Educação', 1500, 1500, [
          item('cat-mens', 'Mensalidade', 1500, 1500),
        ]),
        grupo('cat-lazer', 'Lazer', 0, 80, [item('cat-cinema', 'Cinema', 0, 80)]),
      ];
      const fixture = await abrir(relatorio);
      const el = fixture.nativeElement as HTMLElement;
      const titulos = [...el.querySelectorAll('.lista .medio')].map((t) => t.textContent);

      expect(titulos).toEqual([
        'Supermercado passou do previsto',
        'Alimentação está no limite',
        'Educação está no limite',
        'Lazer não tem previsto',
      ]);
      expect(texto(el)).toContain('em Alimentação');
      expect(texto(el)).toContain('Usou todo o previsto');
      expect(texto(el)).toContain('Definir previsto');
      expect(texto(el)).toContain('Sem previsto');
    });

    it('diz que está tudo no ritmo e trata mês sem previsto', async () => {
      const relatorio = previstoRealizado();
      relatorio.despesas.total = valores(0, 300);
      relatorio.despesas.grupos = [];
      relatorio.receitas.total = valores(0, 8200);
      relatorio.receitas.grupos = [grupo('cat-renda', 'Renda extra', 0, 8200)];
      relatorio.saldo = valores(0, -100);
      const fixture = await abrir(relatorio);
      const el = fixture.nativeElement as HTMLElement;

      expect(texto(el)).toContain('Tudo no ritmo');
      expect(texto(el)).toContain('Sem previsto de despesas neste mês.');
      expect(texto(el)).toContain('Sem previsto de receitas');
      expect(texto(el)).toContain('Nenhuma despesa prevista ou realizada.');
      expect(el.querySelector('.valor.des')).not.toBeNull();
    });

    it('mostra o que ainda resta e receita sem grupos', async () => {
      const relatorio = previstoRealizado();
      relatorio.despesas.total = valores(1200, 600);
      relatorio.despesas.grupos = [grupo('cat-moradia', 'Moradia', 1200, 600)];
      relatorio.receitas.grupos = [];
      const fixture = await abrir(relatorio);
      const el = fixture.nativeElement as HTMLElement;

      expect(texto(el)).toContain('Restam');
      expect(texto(el)).toContain('Nenhuma receita prevista ou recebida.');
      expect(el.querySelector('button.alternar')).toBeNull();
    });

    it('copia o previsto do mês anterior', async () => {
      const fixture = await abrir();
      const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
      fixture.componentInstance['confirmarCopia']();
      const pedido = http.expectOne('/api/orcamentos/2026/10/copiar');
      expect(pedido.request.body).toEqual({ meses: ['2026-11'], substituirExistentes: false });
      pedido.flush({ meses: [{ competencia: '2026-11', gravadas: 3, mantidas: 1 }] });
      TestBed.tick();
      http.expectOne('/api/orcamentos/2026/11').flush(previstoRealizado());
      await fixture.whenStable();
      expect(avisos).toHaveBeenCalledWith(
        expect.objectContaining({ detail: '3 categorias receberam o previsto de outubro.' }),
      );

      fixture.componentInstance['confirmarCopia']();
      http
        .expectOne('/api/orcamentos/2026/10/copiar')
        .flush({ meses: [{ competencia: '2026-11', gravadas: 0, mantidas: 4 }] });
      TestBed.tick();
      http.expectOne('/api/orcamentos/2026/11').flush(previstoRealizado());
      await fixture.whenStable();
      fixture.componentInstance['confirmarCopia']();
      http
        .expectOne('/api/orcamentos/2026/10/copiar')
        .flush(null, { status: 500, statusText: 'x' });
      expect(avisos).toHaveBeenCalledTimes(3);
    });

    it('mostra o vazio sem previsto nem realizado', async () => {
      const vazio = previstoRealizado();
      vazio.despesas.total = valores(0, 0);
      vazio.receitas.total = valores(0, 0);
      vazio.despesas.grupos = [];
      vazio.receitas.grupos = [];
      const fixture = await abrir(vazio);
      expect(texto(fixture.nativeElement)).toContain('Sem orçamento em novembro');
    });

    it('mostra o erro e tenta de novo', async () => {
      const fixture = TestBed.createComponent(OrcamentoMes);
      TestBed.tick();
      http.expectOne('/api/orcamentos/2026/11').flush(null, { status: 500, statusText: 'x' });
      await fixture.whenStable();
      expect(texto(fixture.nativeElement)).toContain('Não foi possível carregar o orçamento');
      fixture.componentInstance['tentarDeNovo']();
      TestBed.tick();
      http.expectOne('/api/orcamentos/2026/11').flush(previstoRealizado());
    });
  });

  describe('ano', () => {
    async function abrir(resumo: ResumoAnual = RESUMO_ANUAL, ano = 2026) {
      const fixture = TestBed.createComponent(OrcamentoAno);
      TestBed.tick();
      if (ano !== 2026) {
        http.expectOne('/api/orcamentos/2026').flush(RESUMO_ANUAL);
        await fixture.whenStable();
        const rotulo = `Ir para ${ano > 2026 ? 2027 : 2025}`;
        fixture.nativeElement.querySelector(`[aria-label="${rotulo}"]`).click();
        TestBed.tick();
      }
      http.expectOne(`/api/orcamentos/${ano}`).flush(resumo);
      await fixture.whenStable();
      return fixture.nativeElement as HTMLElement;
    }

    const linha = (el: HTMLElement, nome: string) =>
      [...el.querySelectorAll<HTMLTableRowElement>('tbody tr')].find(
        (tr) => tr.querySelector('th')?.textContent?.trim() === nome,
      );
    const celulas = (tr: HTMLTableRowElement | undefined) => [...tr!.querySelectorAll('td')];

    it('mostra os números do ano, os gráficos e a tabela com cores', async () => {
      const el = await abrir();

      const kpis = [...el.querySelectorAll('.kpi')].map((k) => texto(k as HTMLElement).trim());
      expect(kpis[0]).toContain('R$ 32.200,00');
      expect(kpis[2]).toContain('84,7% do que entrou · meta 30%');
      expect(el.querySelectorAll('.kpi .delta.ok')).toHaveLength(1);
      expect(kpis[3]).toContain('de 10% em jan para 25% em nov');
      expect(el.querySelectorAll('p-chart')).toHaveLength(2);
      expect(el.querySelector('.grafico .area')!.getAttribute('aria-label')).toMatch(
        /em novembro, R\$\s8\.200,00 realizados/,
      );
      expect(el.querySelector('th.atual')!.textContent).toContain('nov');

      const alimentacao = celulas(linha(el, 'Alimentação'));
      expect(alimentacao[2].className).toContain('estourou');
      expect(alimentacao[3].className).toContain('limite');
      expect(alimentacao[4].className).toContain('vazio');
      expect(alimentacao[11]).toMatchObject({ className: expect.stringContaining('futuro') });
      expect(alimentacao[11].textContent!.trim()).toBe('1.000');
      expect(celulas(linha(el, 'Lazer'))[1].getAttribute('title')).toContain('80,00 sem previsto');
      expect(celulas(linha(el, 'Receitas ativas'))[0].textContent!.trim()).toBe('8.000');
      expect(celulas(linha(el, 'Receitas ativas'))[4].className).toContain('vazio');
      expect(linha(el, 'Total de receitas')).toBeDefined();
      expect(linha(el, 'Total de despesas')!.classList).toContain('des');

      const evolucao = celulas(linha(el, 'Evolução dos gastos')).map((c) => c.textContent!.trim());
      expect(evolucao.slice(0, 4)).toEqual(['–', '▼ 5%', '▲ 10%', '0%']);
      expect(celulas(linha(el, 'Guardou'))[0].className).toContain('ok');
      expect(celulas(linha(el, 'Guardou'))[4].className).toContain('vazio');
      expect(celulas(linha(el, 'Independenciômetro'))[10].textContent!.trim()).toBe('25%');
    });

    it('anima os gráficos, menos quando o sistema pede menos movimento', async () => {
      const animacao = async () => {
        const fixture = TestBed.createComponent(OrcamentoAno);
        TestBed.tick();
        http.expectOne('/api/orcamentos/2026').flush(RESUMO_ANUAL);
        await fixture.whenStable();
        const grafico = fixture.debugElement.query(By.directive(UIChart));
        return (grafico.componentInstance as UIChart).options().animation;
      };

      expect(await animacao()).toEqual({ duration: 700, easing: 'easeOutQuart' });

      vi.stubGlobal('matchMedia', (consulta: string) => ({ matches: consulta.includes('reduce') }));
      expect(await animacao()).toBe(false);
      vi.unstubAllGlobals();
    });

    it('abre e recolhe as subcategorias sem mostrar as zeradas', async () => {
      const el = await abrir();
      expect(linha(el, 'Supermercado')).toBeUndefined();
      expect(texto(el)).not.toContain('Pet');

      const botao = el.querySelector<HTMLButtonElement>('button.alternar')!;
      botao.click();
      TestBed.tick();
      expect(botao.getAttribute('aria-expanded')).toBe('true');
      expect(linha(el, 'Supermercado')!.className).toContain('sub');
      expect(linha(el, 'Padaria')).toBeUndefined();

      botao.click();
      TestBed.tick();
      expect(linha(el, 'Supermercado')).toBeUndefined();
    });

    it('marca guardado abaixo da meta e negativo e ano sem despesa', async () => {
      const resumo = structuredClone(RESUMO_ANUAL);
      resumo.indicadores = { ...resumo.indicadores, guardou: -500, percentualGuardado: 0.1 };
      resumo.meses = resumo.meses.map((m) => ({
        ...m,
        despesas: valores(1000, 0),
        guardou: m.mes === 1 ? -200 : m.mes === 2 ? 800 : m.guardou,
        percentualGuardado: m.mes === 2 ? 0.1 : m.percentualGuardado,
      }));
      const el = await abrir(resumo);

      const kpis = [...el.querySelectorAll('.kpi')].map((k) => texto(k as HTMLElement));
      expect(el.querySelectorAll('.kpi .indicador')[2].className).toContain('des');
      expect(el.querySelector('.kpi .delta.amb')).not.toBeNull();
      expect(kpis[3]).toContain('—');
      expect(kpis[3]).toContain('Sem despesas no ano');
      const guardou = celulas(linha(el, 'Guardou'));
      expect(guardou[0].className).toContain('des');
      expect(guardou[1].className).toContain('amb');
      expect(celulas(linha(el, 'Independenciômetro'))[0].className).toContain('vazio');
    });

    it('mostra um único mês de despesa e ano sem receita', async () => {
      const resumo = structuredClone(RESUMO_ANUAL);
      resumo.indicadores = { ...resumo.indicadores, entrou: 0 };
      resumo.meses = resumo.meses.map((m) => ({
        ...m,
        receitas: valores(8000, 0),
        despesas: valores(1000, m.mes === 5 ? 400 : 0),
        guardou: m.mes === 5 ? -400 : 0,
        independenciometro: m.mes === 5 ? 0.5 : 0,
      }));
      const el = await abrir(resumo);

      const kpis = [...el.querySelectorAll('.kpi')].map((k) => texto(k as HTMLElement));
      expect(kpis[2]).toContain('Sem receita no ano');
      expect(kpis[3]).toContain('50% em mai');
      expect(kpis[3]).not.toContain('para');
      expect(celulas(linha(el, 'Guardou'))[4].getAttribute('title')).toContain('400,00');
    });

    it('mostra o ano passado inteiro e o ano que vem só com previsto', async () => {
      let el = await abrir(RESUMO_ANUAL, 2025);
      expect(texto(el)).toContain('com total e média do ano.');
      expect(el.querySelector('th.atual')).toBeNull();

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          ...provedoresDeTeste(),
        ],
      });
      http = TestBed.inject(HttpTestingController);
      el = await abrir(RESUMO_ANUAL, 2027);
      expect(texto(el)).toContain('O que você já planejou para o ano.');
      expect(el.querySelector('.grafico .area')!.getAttribute('aria-label')).toContain(
        'Receitas previstas para 2027',
      );
      expect(celulas(linha(el, 'Alimentação'))[0].className).toContain('futuro');
      expect(celulas(linha(el, 'Lazer'))[0].className).toContain('vazio');
      expect(celulas(linha(el, 'Evolução dos gastos'))[2].className).toContain('vazio');
    });

    it('mostra o vazio, o erro e tenta de novo', async () => {
      const el = await abrir({ ...RESUMO_ANUAL, grupos: [] });
      expect(texto(el)).toContain('Nada previsto ou realizado em 2026.');

      el.querySelector<HTMLButtonElement>('[aria-label="Ir para 2025"]')!.click();
      TestBed.tick();
      http.expectOne('/api/orcamentos/2025').flush(null, { status: 500, statusText: 'x' });
      await TestBed.inject(ApplicationRef).whenStable();
      expect(texto(el)).toContain('Não foi possível carregar o orçamento de 2025.');
      el.querySelector<HTMLButtonElement>('app-estado-erro button')!.click();
      TestBed.tick();
      http.expectOne('/api/orcamentos/2025').flush(RESUMO_ANUAL);
    });
  });

  describe('editar previsto', () => {
    async function abrir() {
      const fixture = TestBed.createComponent(OrcamentoPrevisto);
      TestBed.tick();
      http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
      http.expectOne('/api/orcamentos/2026/11').flush(previstoRealizado());
      await fixture.whenStable();
      return fixture;
    }

    it('edita só as folhas e mostra a soma do grupo', async () => {
      const fixture = await abrir();
      const tela = fixture.componentInstance;
      const linhas = tela['blocos']()[0].linhas;

      expect(linhas.map((l) => [l.nome, l.editavel])).toEqual([
        ['Alimentação', false],
        ['Padaria', true],
        ['Supermercado', true],
        ['Moradia', false],
        ['Conta de luz', true],
      ]);
      expect(tela['valorDe']('cat-mercado')).toBe(1000);
      tela['editar']('cat-padaria', 150);
      expect(tela['somaDe'](linhas[0])).toBe(1150);
      tela['editar']('cat-mercado', 1000);
      expect(tela['alterados']()).toEqual([['cat-padaria', 150]]);
      tela['descartar']();
      expect(tela['alterados']()).toEqual([]);
    });

    it('salva no mês e repete até dezembro', async () => {
      const fixture = await abrir();
      const tela = fixture.componentInstance;
      tela['editar']('cat-padaria', null);
      tela['editar']('cat-luz', 250);
      tela['repetirAteDezembro'].set(true);
      tela['salvar']();

      const definir = http.expectOne('/api/orcamentos/2026/11');
      expect(definir.request.body).toEqual({
        itens: [{ categoriaId: 'cat-luz', valorPrevisto: 250 }],
      });
      definir.flush(previstoRealizado());
      const repetir = http.expectOne('/api/orcamentos/repetir');
      expect(repetir.request.body).toEqual({
        categoriaId: 'cat-luz',
        valorPrevisto: 250,
        meses: ['2026-12'],
        substituirExistentes: true,
      });
      repetir.flush({ meses: [] });
      TestBed.tick();
      http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
      http.expectOne('/api/orcamentos/2026/11').flush(previstoRealizado());
      expect(tela['alterados']()).toEqual([]);
    });

    it('avisa quando não salva e limpa as edições ao trocar de mês', async () => {
      const fixture = await abrir();
      const tela = fixture.componentInstance;
      const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
      tela['salvar']();
      tela['editar']('cat-padaria', 80);
      tela['salvar']();
      http.expectOne('/api/orcamentos/2026/11').flush(null, { status: 400, statusText: 'x' });
      expect(avisos).toHaveBeenCalledTimes(1);

      tela['trocarMes']({ ano: 2026, mes: 12 });
      expect(tela['alterados']()).toEqual([]);
      expect(tela['podeRepetir']()).toBe(false);
      TestBed.tick();
      http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina([]));
      http.expectOne('/api/orcamentos/2026/12').flush(null, { status: 500, statusText: 'x' });
      await fixture.whenStable();
      expect(texto(fixture.nativeElement)).toContain('Não foi possível carregar');
    });
  });
});
