import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { Menu } from 'primeng/menu';
import {
  CARTAO,
  CONTAS,
  conta,
  fatura,
  pagina,
  provedoresDeTeste,
} from '../../../testing/dados-de-teste';
import { ContaDialogo } from './components/conta-dialogo/conta-dialogo';
import { FaturaDialogo } from './components/fatura-dialogo/fatura-dialogo';
import { ImportacaoDeExtrato } from '../importar/state/importacao-de-extrato';
import { Contas } from './pages/contas/contas';
import { rotuloDoTipo } from './tipos-de-conta';

const URL_CONTAS = '/api/contas?size=200&sort=nome';
const RESUMO = {
  patrimonio: 1580,
  porTipo: [{ tipo: 'CONTA_CORRENTE', total: 1500, quantidadeDeContas: 1 }],
};

describe('Contas', () => {
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

  describe('tela', () => {
    let fixture: ComponentFixture<Contas>;

    async function abrir(contas = [...CONTAS, conta({ id: 'c9', nome: 'Antiga', ativa: false })]) {
      fixture = TestBed.createComponent(Contas);
      const confirmacao = fixture.debugElement.injector.get(ConfirmationService);
      vi.spyOn(confirmacao, 'confirm').mockImplementation((c) => {
        c.accept?.();
        return confirmacao;
      });
      TestBed.tick();
      http.expectOne(URL_CONTAS).flush(pagina(contas));
      http.expectOne('/api/contas/resumo').flush(RESUMO);
      await fixture.whenStable();
    }

    async function recarregar() {
      TestBed.tick();
      http.expectOne(URL_CONTAS).flush(pagina(CONTAS));
      http.expectOne('/api/contas/resumo').flush(RESUMO);
      await fixture.whenStable();
    }

    function acoesDe(c: (typeof CONTAS)[number]): MenuItem[] {
      fixture.componentInstance['abrirAcoes'](
        { toggle: vi.fn() } as unknown as Menu,
        new Event('x'),
        c,
      );
      return fixture.componentInstance['acoes']();
    }

    it('mostra patrimônio, cartões e esconde as inativas até pedir', async () => {
      await abrir();
      const el = fixture.nativeElement as HTMLElement;
      expect(texto(el)).toContain('R$ 1.580,00');
      expect(texto(el)).toContain('Fecha dia 5 · vence dia 12');
      expect(texto(el)).toContain('− R$ 20,00 desde o saldo inicial');
      expect(texto(el)).not.toContain('Antiga');

      fixture.componentInstance['mostrarInativas'].set(true);
      await fixture.whenStable();
      expect(texto(el)).toContain('Antiga');
    });

    it('abre os diálogos pelo menu e recarrega ao salvar', async () => {
      await abrir();
      const tela = fixture.componentInstance;
      tela['novaConta']();
      expect(tela['modo']()).toBe('criar');
      acoesDe(CONTAS[0])[1].command!({});
      expect(tela['modo']()).toBe('editar');
      acoesDe(CONTAS[0])[2].command!({});
      expect(tela['modo']()).toBe('saldo');
      tela['aoSalvar']();
      expect(tela['modo']()).toBeNull();
      await recarregar();
    });

    it('importa o extrato da conta bancária e recarrega o saldo depois', async () => {
      await abrir();
      const importacao = TestBed.inject(ImportacaoDeExtrato);
      const corrente = acoesDe(CONTAS[0]);
      expect(corrente[0].label).toBe('Importar extrato');
      corrente[0].command!({});
      expect(importacao.aberta()).toBe(true);
      expect(importacao.contaSugerida()).toBe(CONTAS[0].id);
      expect(acoesDe(CONTAS[1]).map((a) => a.label)).not.toContain('Importar extrato');
      importacao.registrarConclusao();
      await recarregar();
    });

    it('inativa e reativa', async () => {
      await abrir();
      const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
      acoesDe(CONTAS[1])[2].command!({});
      http.expectOne('/api/contas/c2').flush(null);
      await recarregar();
      acoesDe(conta({ id: 'c9', ativa: false }))[0].command!({});
      http.expectOne('/api/contas/c9/ativar').flush({});
      await recarregar();
      fixture.componentInstance['confirmarInativacao'](CONTAS[1]);
      http.expectOne('/api/contas/c2').flush(null, { status: 400, statusText: 'x' });
      fixture.componentInstance['reativar'](CONTAS[1]);
      http.expectOne('/api/contas/c2/ativar').flush(null, { status: 400, statusText: 'x' });
      expect(avisos).toHaveBeenCalledTimes(4);
    });

    it('mostra vazio e erro', async () => {
      await abrir([]);
      expect(texto(fixture.nativeElement)).toContain('Nenhuma conta cadastrada');

      fixture.componentInstance['dados'].reload();
      TestBed.tick();
      http.expectOne(URL_CONTAS).flush(null, { status: 500, statusText: 'x' });
      http.expectOne('/api/contas/resumo');
      await fixture.whenStable();
      expect(texto(fixture.nativeElement)).toContain('Não foi possível carregar as contas.');
    });
  });

  describe('diálogo de conta', () => {
    let fixture: ComponentFixture<ContaDialogo>;

    async function abrir(modo: 'criar' | 'editar' | 'saldo', c: typeof CARTAO | null = null) {
      fixture = TestBed.createComponent(ContaDialogo);
      fixture.componentRef.setInput('conta', c);
      fixture.componentRef.setInput('modo', modo);
      await fixture.whenStable();
      return fixture.componentInstance;
    }

    it('seleciona o saldo ao focar ou clicar para permitir substituir o valor', async () => {
      await abrir('criar');
      const saldo = fixture.nativeElement.querySelector('#conta-saldo') as HTMLInputElement;

      saldo.dispatchEvent(new FocusEvent('focus'));
      expect(saldo.selectionStart).toBe(0);
      expect(saldo.selectionEnd).toBe(saldo.value.length);

      saldo.setSelectionRange(3, 3);
      saldo.click();
      expect(saldo.selectionStart).toBe(0);
      expect(saldo.selectionEnd).toBe(saldo.value.length);
    });

    it('cria cartão exigindo os dias da fatura', async () => {
      const tela = await abrir('criar');
      const salvo = vi.fn();
      fixture.componentInstance.salvo.subscribe(salvo);
      tela['salvar']();
      expect(tela['erros']().nome).toBe('Informe o nome da conta.');
      tela['nome'].set(' Nubank ');
      tela['tipo'].set('CARTAO_CREDITO');
      tela['salvar']();
      expect(tela['erros']().dias).toContain('dia de fechamento');
      tela['diaFechamento'].set(3);
      tela['diaVencimento'].set(10);
      tela['salvar']();
      const pedido = http.expectOne('/api/contas');
      expect(pedido.request.body).toEqual({
        nome: 'Nubank',
        tipo: 'CARTAO_CREDITO',
        diaFechamento: 3,
        diaVencimento: 10,
        saldoInicial: 0,
      });
      pedido.flush(CARTAO);
      expect(salvo).toHaveBeenCalledWith(CARTAO);
      expect(tela['titulo']()).toBe('Nova conta');
    });

    it('edita sem mexer no saldo e corrige o saldo inicial', async () => {
      let tela = await abrir('editar', conta());
      expect(tela['titulo']()).toBe('Editar conta');
      tela['salvar']();
      expect(http.expectOne('/api/contas/c1').request.body).toEqual({
        nome: 'Banco Azul',
        tipo: 'CONTA_CORRENTE',
        diaFechamento: null,
        diaVencimento: null,
      });

      tela = await abrir('saldo', conta());
      expect(tela['titulo']()).toBe('Corrigir saldo inicial');
      tela['saldoInicial'].set(null);
      tela['salvar']();
      expect(tela['erros']().saldo).toBe('Informe o saldo inicial.');
      tela['saldoInicial'].set(900);
      tela['salvar']();
      const pedido = http.expectOne('/api/contas/c1/corrigir-saldo-inicial');
      expect(pedido.request.body).toEqual({ saldoInicial: 900 });
      pedido.flush(null, { status: 400, statusText: 'x' });
      expect(tela['salvando']()).toBe(false);
    });
  });

  describe('fatura', () => {
    let fixture: ComponentFixture<FaturaDialogo>;

    async function abrir(dados = fatura()) {
      fixture = TestBed.createComponent(FaturaDialogo);
      fixture.componentRef.setInput('contas', CONTAS);
      fixture.componentRef.setInput('cartao', CARTAO);
      TestBed.tick();
      http
        .expectOne('/api/lancamentos-financeiros/faturas?contaId=c3&ano=2026&mes=11')
        .flush(dados);
      await fixture.whenStable();
      return fixture.componentInstance;
    }

    it('paga as compras pendentes com a conta escolhida', async () => {
      const tela = await abrir();
      const paga = vi.fn();
      tela.paga.subscribe(paga);
      expect(tela['opcoesDePagamento']().map((o) => o.value)).toEqual(['c1', 'c2']);
      expect(tela['contaPagamentoId']()).toBe('c1');
      expect(texto(fixture.nativeElement)).toContain('Fechada');

      tela['iniciarPagamento']();
      tela['contaPagamentoId'].set('c2');
      tela['pagar']();
      const pedido = http.expectOne('/api/lancamentos-financeiros/faturas/pagar');
      expect(pedido.request.body).toEqual({
        contaId: 'c3',
        ano: 2026,
        mes: 11,
        contaPagamentoId: 'c2',
        dataPagamento: '2026-11-15',
      });
      pedido.flush(fatura());
      expect(paga).toHaveBeenCalled();
      TestBed.tick();
      http
        .expectOne('/api/lancamentos-financeiros/faturas?contaId=c3&ano=2026&mes=11')
        .flush(
          fatura({ lancamentos: fatura().lancamentos.map((l) => ({ ...l, status: 'PAGO' })) }),
        );
      await fixture.whenStable();
      expect(tela['quitada']()).toBe(true);
    });

    it('avisa quando o pagamento falha e não paga sem conta', async () => {
      const tela = await abrir(fatura({ fechada: false }));
      const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
      tela['pagar']();
      http
        .expectOne('/api/lancamentos-financeiros/faturas/pagar')
        .flush(
          { message: 'Não há compras pendentes nesta fatura.' },
          { status: 400, statusText: 'x' },
        );
      expect(avisos).toHaveBeenCalledWith(
        expect.objectContaining({ detail: 'Não há compras pendentes nesta fatura.' }),
      );
      tela['contaPagamentoId'].set(null);
      tela['pagar']();
    });

    it('mostra erro ao carregar', async () => {
      fixture = TestBed.createComponent(FaturaDialogo);
      fixture.componentRef.setInput('cartao', CARTAO);
      TestBed.tick();
      http
        .expectOne('/api/lancamentos-financeiros/faturas?contaId=c3&ano=2026&mes=11')
        .flush(null, { status: 500, statusText: 'x' });
      await fixture.whenStable();
      expect(fixture.componentInstance['pendentes']()).toEqual([]);
    });
  });

  it('nomeia os tipos de conta', () => {
    expect(rotuloDoTipo('POUPANCA')).toBe('Poupança');
    expect(rotuloDoTipo('XYZ' as never)).toBe('XYZ');
  });
});
