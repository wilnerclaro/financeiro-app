import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CATEGORIAS, CONTAS, pagina } from '../../../../../testing/dados-de-teste';
import { ItemOfx } from '../../../../shared/api/importacoes-api';
import { Importar } from './importar';

const ITEM: ItemOfx = {
  identificador: 'fit1',
  data: '2026-11-10',
  descricao: 'Mercado',
  valor: 100,
  tipo: 'DESPESA',
  jaImportado: false,
  importavel: true,
  motivo: null,
};

describe('Importar OFX', () => {
  let fixture: ComponentFixture<Importar>;
  let http: HttpTestingController;
  const tela = () => fixture.componentInstance;
  const elemento = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Importar);
  });
  afterEach(() => http.verify());

  async function carregar(contas = CONTAS) {
    http.expectOne('/api/contas?ativa=true&size=200&sort=nome').flush(pagina(contas));
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome').flush(pagina(CATEGORIAS));
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

  function responderRevisao(itens = [ITEM]) {
    const req = http.expectOne('/api/importacoes/ofx/revisar');
    expect(req.request.body).toBeInstanceOf(FormData);
    expect(req.request.body.get('contaId')).toBe('c1');
    expect(req.request.headers.has('Content-Type')).toBe(false);
    req.flush({ moeda: 'BRL', itens });
  }

  it('oferece apenas contas bancárias e informa os formatos futuros', async () => {
    await carregar();
    expect(
      tela()
        ['contas']()
        .map((c) => c.id),
    ).toEqual(['c1']);
    expect(elemento().textContent).toContain('Excel');
    expect(elemento().textContent).toContain('em breve');
  });

  it('rejeita outros formatos, arquivos vazios e maiores que 2 MB', async () => {
    await carregar();
    arquivo('planilha.xlsx');
    expect(tela()['arquivo']()).toBeNull();
    expect(tela()['erro']()).toContain('OFX');
    arquivo('vazio.ofx', 0);
    expect(tela()['arquivo']()).toBeNull();
    arquivo('grande.ofx', 2 * 1024 * 1024 + 1);
    expect(tela()['arquivo']()).toBeNull();
    arquivo('extrato.OFX');
    expect(tela()['arquivo']()?.name).toBe('extrato.OFX');
    expect(tela()['erro']()).toBe('');
  });

  it('revisa sem criar lançamentos e só confirma linhas válidas com categoria', async () => {
    await carregar();
    const file = arquivo();
    tela()['revisar']();
    responderRevisao([
      ITEM,
      { ...ITEM, identificador: 'fit2', jaImportado: true },
      { ...ITEM, identificador: 'fit3', importavel: false, motivo: 'Transferência' },
    ]);
    expect(tela()['selecionadas']()).toHaveLength(1);
    expect(tela()['podeConfirmar']()).toBe(false);
    tela()['selecionar']('fit2', true);
    tela()['selecionar']('fit3', true);
    expect(tela()['selecionadas']()).toHaveLength(1);
    tela()['confirmar']();
    http.expectNone('/api/importacoes/ofx/confirmar');
    tela()['categorizar']('fit1', 'cat-mercado');
    expect(tela()['podeConfirmar']()).toBe(true);
    expect(tela()['totalDespesas']()).toBe(100);
    tela()['confirmar']();
    const confirmar = http.expectOne('/api/importacoes/ofx/confirmar');
    expect(confirmar.request.body.get('arquivo')).toEqual(file);
    expect(JSON.parse(confirmar.request.body.get('selecoes'))).toEqual([
      { identificador: 'fit1', categoriaId: 'cat-mercado' },
    ]);
    confirmar.flush({ importados: 1, ignorados: 2 });
    await fixture.whenStable();
    expect(elemento().textContent).toContain('Importação concluída');
    http.expectNone('/api/lancamentos-financeiros');
    tela()['recomecar']();
    expect(tela()['arquivo']()).toBeNull();
    expect(tela()['linhas']()).toEqual([]);
    expect(tela()['resultado']()).toBeNull();
  });

  it('mantém a revisão para nova tentativa quando a confirmação falha', async () => {
    await carregar();
    arquivo();
    tela()['revisar']();
    responderRevisao();
    tela()['categorizar']('fit1', 'cat-mercado');
    tela()['confirmar']();
    http
      .expectOne('/api/importacoes/ofx/confirmar')
      .flush({ message: 'Tente novamente.' }, { status: 503, statusText: 'Unavailable' });
    expect(tela()['etapa']()).toBe('revisar');
    expect(tela()['linhas']()[0].categoriaId).toBe('cat-mercado');
    expect(tela()['erro']()).toContain('Tente novamente');
    expect(tela()['podeConfirmar']()).toBe(true);
    tela()['selecionar']('fit1', false);
    expect(tela()['podeConfirmar']()).toBe(false);
  });

  it('permite corrigir arquivo inválido após falha de leitura', async () => {
    await carregar();
    arquivo();
    tela()['revisar']();
    http
      .expectOne('/api/importacoes/ofx/revisar')
      .flush({ message: 'Arquivo inválido.' }, { status: 400, statusText: 'Bad Request' });
    expect(tela()['etapa']()).toBe('enviar');
    expect(tela()['erro']()).toContain('Arquivo inválido');
    expect(tela()['ocupado']()).toBe(false);
  });

  it('oferece cadastro quando não há conta bancária', async () => {
    await carregar([]);
    expect(elemento().textContent).toContain('Cadastre uma conta corrente ou poupança');
    expect(elemento().querySelector('input[type=file]')).toBeNull();
  });

  it('repete o carregamento após falha de conexão', async () => {
    http
      .expectOne('/api/contas?ativa=true&size=200&sort=nome')
      .flush({}, { status: 503, statusText: 'Unavailable' });
    http.expectOne('/api/categorias?ativa=true&size=500&sort=nome');
    expect(tela()['erroInicial']()).toBe(true);
    tela()['carregar']();
    await carregar();
    expect(tela()['erroInicial']()).toBe(false);
  });
});
