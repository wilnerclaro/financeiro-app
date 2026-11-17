import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CATEGORIAS, CONTAS, conta, pagina } from '../../../../../testing/dados-de-teste';
import { ItemExtrato } from '../../../../shared/api/importacoes-api';
import { ImportacaoDeExtrato } from '../../state/importacao-de-extrato';
import { ImportarExtrato } from './importar-extrato';

const ITEM: ItemExtrato = {
  identificador: 'fit1',
  data: '2026-11-10',
  descricao: 'Mercado',
  valor: 100,
  tipo: 'DESPESA',
  jaImportado: false,
  possivelDuplicado: false,
  importavel: true,
  motivo: null,
};

const URL_CONTAS = '/api/contas?ativa=true&size=200&sort=nome';
const URL_CATEGORIAS = '/api/categorias?ativa=true&size=500&sort=nome';

describe('Importar extrato', () => {
  let fixture: ComponentFixture<ImportarExtrato>;
  let http: HttpTestingController;
  let importacao: ImportacaoDeExtrato;
  const tela = () => fixture.componentInstance;
  const elemento = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    importacao = TestBed.inject(ImportacaoDeExtrato);
    fixture = TestBed.createComponent(ImportarExtrato);
  });
  afterEach(() => http.verify());

  async function abrir(contas = CONTAS, contaId: string | null = null) {
    importacao.abrir(contaId);
    TestBed.tick();
    http.expectOne(URL_CONTAS).flush(pagina(contas));
    http.expectOne(URL_CATEGORIAS).flush(pagina(CATEGORIAS));
    await fixture.whenStable();
  }

  function arquivo(nome = 'extrato.ofx', tamanho = 20) {
    const input = document.createElement('input');
    input.type = 'file';
    const file = new File([new Uint8Array(tamanho)], nome);
    Object.defineProperty(input, 'files', { value: [file] });
    tela()['escolherArquivo']({ target: input } as unknown as Event);
    return file;
  }

  function responderRevisao(itens = [ITEM], contaId = 'c1') {
    const req = http.expectOne('/api/importacoes/revisar');
    expect(req.request.body).toBeInstanceOf(FormData);
    expect(req.request.body.get('contaId')).toBe(contaId);
    expect(req.request.headers.has('Content-Type')).toBe(false);
    req.flush({ formato: 'OFX', banco: null, moeda: 'BRL', itens });
  }

  it('só busca contas e categorias quando o modal abre', async () => {
    await fixture.whenStable();
    http.expectNone(URL_CONTAS);
    await abrir();
    expect(
      tela()
        ['contas']()
        .map((c) => c.id),
    ).toEqual(['c1']);
    expect(elemento().textContent).toContain('Arraste o extrato aqui');
    expect(elemento().textContent).toContain('Itaú, Santander e Inter');
    expect(elemento().textContent).toContain('PDF, CSV ou OFX de até 2 MB');
  });

  it('já vem com a conta escolhida em Contas', async () => {
    const poupanca = conta({ id: 'c9', nome: 'Poupança', tipo: 'POUPANCA' });
    await abrir([...CONTAS, poupanca], 'c9');
    expect(tela()['contaId']()).toBe('c9');
    arquivo();
    tela()['revisar']();
    responderRevisao([ITEM], 'c9');
  });

  it('rejeita outros formatos, arquivos vazios e maiores que 2 MB', async () => {
    await abrir();
    arquivo('planilha.xlsx');
    expect(tela()['arquivo']()).toBeNull();
    expect(tela()['erro']()).toBe('Escolha o extrato em PDF, CSV ou OFX.');
    arquivo('vazio.ofx', 0);
    expect(tela()['arquivo']()).toBeNull();
    arquivo('grande.ofx', 2 * 1024 * 1024 + 1);
    expect(tela()['arquivo']()).toBeNull();
    arquivo('extrato.csv');
    expect(tela()['arquivo']()?.name).toBe('extrato.csv');
    arquivo('extrato.OFX');
    expect(tela()['arquivo']()?.name).toBe('extrato.OFX');
    expect(tela()['erro']()).toBe('');
    tela()['trocarArquivo']();
    expect(tela()['arquivo']()).toBeNull();
  });

  it('aceita o arquivo arrastado para a área de envio', async () => {
    await abrir();
    const preventDefault = vi.fn();
    tela()['arrastar']({ preventDefault } as unknown as DragEvent);
    expect(tela()['arrastando']()).toBe(true);
    const file = new File([new Uint8Array(10)], 'extrato.ofx');
    tela()['soltar']({ preventDefault, dataTransfer: { files: [file] } } as unknown as DragEvent);
    expect(preventDefault).toHaveBeenCalledTimes(2);
    expect(tela()['arrastando']()).toBe(false);
    expect(tela()['arquivo']()).toBe(file);
    await fixture.whenStable();
    expect(elemento().textContent).toContain('extrato.ofx');
  });

  it('revisa sem criar lançamentos e só confirma linhas válidas com categoria', async () => {
    await abrir();
    const file = arquivo();
    tela()['revisar']();
    responderRevisao([
      ITEM,
      { ...ITEM, identificador: 'fit2', jaImportado: true },
      { ...ITEM, identificador: 'fit3', importavel: false, motivo: 'Transferência' },
    ]);
    await fixture.whenStable();
    expect(elemento().textContent).toContain('Revise antes de importar');
    expect(tela()['selecionadas']()).toHaveLength(1);
    expect(tela()['faltamCategorias']()).toBe(1);
    expect(tela()['podeConfirmar']()).toBe(false);
    tela()['selecionar']('fit2', true);
    tela()['selecionar']('fit3', true);
    expect(tela()['selecionadas']()).toHaveLength(1);
    tela()['confirmar']();
    http.expectNone('/api/importacoes/confirmar');
    tela()['categorizar']('fit1', 'cat-mercado');
    expect(tela()['podeConfirmar']()).toBe(true);
    expect(tela()['totalDespesas']()).toBe(100);
    tela()['confirmar']();
    const confirmar = http.expectOne('/api/importacoes/confirmar');
    expect(confirmar.request.body.get('arquivo')).toEqual(file);
    expect(JSON.parse(confirmar.request.body.get('selecoes'))).toEqual([
      { identificador: 'fit1', categoriaId: 'cat-mercado' },
    ]);
    confirmar.flush({ importados: 1, ignorados: 2 });
    await fixture.whenStable();
    expect(elemento().textContent).toContain('Importação concluída');
    expect(importacao.concluidas()).toBe(1);
    http.expectNone('/api/lancamentos-financeiros');
    tela()['recomecar']();
    expect(tela()['arquivo']()).toBeNull();
    expect(tela()['linhas']()).toEqual([]);
    expect(tela()['resultado']()).toBeNull();
    expect(tela()['etapa']()).toBe('enviar');
  });

  it('deixa desmarcado o que parece já lançado e mostra o banco do arquivo', async () => {
    await abrir();
    arquivo('extrato.PDF');
    expect(tela()['erro']()).toBe('');
    tela()['revisar']();
    http.expectOne('/api/importacoes/revisar').flush({
      formato: 'PDF',
      banco: 'Itaú',
      moeda: 'BRL',
      itens: [ITEM, { ...ITEM, identificador: 'fit2', possivelDuplicado: true }],
    });
    await fixture.whenStable();
    expect(
      tela()
        ['selecionadas']()
        .map((l) => l.identificador),
    ).toEqual(['fit1']);
    expect(elemento().textContent).toContain('Parece já lançada');
    expect(elemento().textContent).toContain('Itaú');
    tela()['selecionar']('fit2', true);
    expect(tela()['selecionadas']()).toHaveLength(2);
    tela()['recomecar']();
    expect(tela()['banco']()).toBeNull();
  });

  it('mantém a revisão para nova tentativa quando a confirmação falha', async () => {
    await abrir();
    arquivo();
    tela()['revisar']();
    responderRevisao();
    tela()['categorizar']('fit1', 'cat-mercado');
    tela()['confirmar']();
    expect(tela()['ocupado']()).toBe(true);
    tela()['fechar']();
    expect(importacao.aberta()).toBe(true);
    http
      .expectOne('/api/importacoes/confirmar')
      .flush({ message: 'Tente novamente.' }, { status: 503, statusText: 'Unavailable' });
    expect(tela()['etapa']()).toBe('revisar');
    expect(tela()['linhas']()[0].categoriaId).toBe('cat-mercado');
    expect(tela()['erro']()).toContain('Tente novamente');
    expect(tela()['podeConfirmar']()).toBe(true);
    expect(importacao.concluidas()).toBe(0);
    tela()['selecionar']('fit1', false);
    expect(tela()['podeConfirmar']()).toBe(false);
  });

  it('permite corrigir arquivo inválido após falha de leitura', async () => {
    await abrir();
    arquivo();
    tela()['revisar']();
    http
      .expectOne('/api/importacoes/revisar')
      .flush({ message: 'Arquivo inválido.' }, { status: 400, statusText: 'Bad Request' });
    expect(tela()['etapa']()).toBe('enviar');
    expect(tela()['erro']()).toContain('Arquivo inválido');
    expect(tela()['ocupado']()).toBe(false);
  });

  it('oferece cadastro de conta quando não há conta bancária', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await abrir([]);
    expect(elemento().textContent).toContain('Cadastre uma conta corrente ou poupança');
    expect(elemento().querySelector('input[type=file]')).toBeNull();
    tela()['irPara']('/contas');
    expect(importacao.aberta()).toBe(false);
    expect(navegar).toHaveBeenCalledWith('/contas');
  });

  it('repete o carregamento após falha de conexão', async () => {
    importacao.abrir();
    TestBed.tick();
    http.expectOne(URL_CONTAS).flush({}, { status: 503, statusText: 'Unavailable' });
    http.expectOne(URL_CATEGORIAS);
    await fixture.whenStable();
    expect(elemento().textContent).toContain('Não foi possível carregar suas contas');
    tela()['dados'].reload();
    TestBed.tick();
    http.expectOne(URL_CONTAS).flush(pagina(CONTAS));
    http.expectOne(URL_CATEGORIAS).flush(pagina(CATEGORIAS));
    await fixture.whenStable();
    expect(tela()['contas']()).toHaveLength(1);
  });

  it('fecha e recomeça do zero na próxima abertura', async () => {
    await abrir();
    arquivo();
    tela()['fechar']();
    expect(importacao.aberta()).toBe(false);
    tela()['aoFechar']();
    expect(tela()['arquivo']()).toBeNull();
    expect(tela()['etapa']()).toBe('enviar');
  });
});
