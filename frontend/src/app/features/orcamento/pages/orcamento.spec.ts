import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import {
  CATEGORIAS,
  RESUMO_ANUAL,
  pagina,
  previstoRealizado,
  provedoresDeTeste,
  valores,
} from '../../../../testing/dados-de-teste';
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

    it('mostra totais, status por grupo e abre os subitens', async () => {
      const fixture = await abrir();
      const el = fixture.nativeElement as HTMLElement;

      expect(texto(el)).toContain('Acima do previsto');
      expect(texto(el)).toContain('No ritmo');
      expect(texto(el)).not.toContain('Pet');
      expect(texto(el)).toContain('Receitas ativas');

      el.querySelector<HTMLButtonElement>('button.linha[aria-expanded="false"]')!.click();
      await fixture.whenStable();
      expect(texto(el)).toContain('Supermercado');
      fixture.componentInstance['alternar']('cat-alimentacao');
      expect(fixture.componentInstance['abertos']().size).toBe(0);
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

  it('ano lista categorias com movimento e troca de ano', async () => {
    const fixture = TestBed.createComponent(OrcamentoAno);
    TestBed.tick();
    http.expectOne('/api/orcamentos/2026').flush(RESUMO_ANUAL);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(texto(el)).toContain('Supermercado');
    expect(texto(el)).not.toContain('Pet');
    expect(texto(el)).toContain('Nada previsto ou realizado em 2026.');
    expect(el.querySelector('th.sub')?.textContent).toContain('Supermercado');

    el.querySelector<HTMLButtonElement>('[aria-label="Ir para 2025"]')!.click();
    TestBed.tick();
    http.expectOne('/api/orcamentos/2025').flush(null, { status: 500, statusText: 'x' });
    await fixture.whenStable();
    expect(texto(el)).toContain('Não foi possível carregar o orçamento de 2025.');
    el.querySelector<HTMLButtonElement>('[aria-label="Ir para 2026"]')!.click();
    TestBed.tick();
    http.expectOne('/api/orcamentos/2026').flush(RESUMO_ANUAL);
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
