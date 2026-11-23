import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { Menu } from 'primeng/menu';
import {
  CATEGORIAS,
  categoria,
  pagina,
  provedoresDeTeste,
} from '../../../../../testing/dados-de-teste';
import { Categorias } from './categorias';

const URL = '/api/categorias?size=500&sort=nome';

describe('Categorias', () => {
  let fixture: ComponentFixture<Categorias>;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ...provedoresDeTeste()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Categorias);
    const confirmacao = fixture.debugElement.injector.get(ConfirmationService);
    vi.spyOn(confirmacao, 'confirm').mockImplementation((c) => {
      c.accept?.();
      return confirmacao;
    });
    TestBed.tick();
    http.expectOne(URL).flush(pagina(CATEGORIAS));
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  const tela = () => fixture.componentInstance;
  const texto = () => (fixture.nativeElement as HTMLElement).textContent!.replace(/\s+/g, ' ');

  async function recarregar() {
    TestBed.tick();
    http.expectOne(URL).flush(pagina(CATEGORIAS));
    await fixture.whenStable();
  }

  function acoesDe(c = CATEGORIAS[0], grupo = true): MenuItem[] {
    tela()['abrirAcoes']({ toggle: vi.fn() } as unknown as Menu, new Event('x'), c, grupo);
    return tela()['acoes']();
  }

  it('agrupa por tipo e mostra inativas só quando pedido', async () => {
    expect(
      tela()
        ['grupos']()
        .map((g) => [g.categoria.nome, g.subcategorias.length]),
    ).toEqual([
      ['Alimentação', 2],
      ['Moradia', 1],
    ]);
    tela()['mostrarInativas'].set(true);
    expect(tela()['grupos']()).toHaveLength(3);
    tela()['escolherTipo']('RECEITA');
    await fixture.whenStable();
    expect(texto()).toContain('Renda ativa');
    expect(tela()['mostraFonte']()).toBe(true);
  });

  it('cria subcategoria pelo menu do grupo', () => {
    expect(acoesDe().map((a) => a.label)).toEqual(['Editar', 'Nova subcategoria', 'Inativar']);
    acoesDe()[1].command!({});
    expect(tela()['paiId']()).toBe('cat-alimentacao');
    expect(
      tela()
        ['opcoesDePai']()
        .map((o) => o.value),
    ).toEqual(['cat-alimentacao', 'cat-moradia']);
    tela()['salvar']();
    expect(tela()['erroDoNome']()).toBe('Informe o nome da categoria.');
    tela()['nome'].set(' Feira ');
    tela()['salvar']();
    const pedido = http.expectOne('/api/categorias');
    expect(pedido.request.body).toEqual({
      nome: 'Feira',
      tipo: 'DESPESA',
      categoriaPaiId: 'cat-alimentacao',
      fonteReceita: null,
    });
    pedido.flush(categoria({ nome: 'Feira' }));
    expect(tela()['dialogoAberto']()).toBe(false);
  });

  it('edita receita com fonte passiva e trata erro', async () => {
    tela()['escolherTipo']('RECEITA');
    tela()['editar'](CATEGORIAS[6]);
    expect(tela()['tituloDoDialogo']()).toBe('Editar categoria');
    tela()['fonte'].set('PASSIVA');
    tela()['salvar']();
    const pedido = http.expectOne('/api/categorias/cat-renda');
    expect(pedido.request.body.fonteReceita).toBe('PASSIVA');
    pedido.flush(null, { status: 409, statusText: 'x' });
    expect(tela()['dialogoAberto']()).toBe(true);
    tela()['fecharDialogo']();
    tela()['novaCategoria']();
    expect(tela()['tituloDoDialogo']()).toBe('Nova categoria de receita');
  });

  it('inativa e reativa', async () => {
    const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
    acoesDe(CATEGORIAS[2], false)[1].command!({});
    http.expectOne('/api/categorias/cat-padaria').flush(null);
    await recarregar();
    acoesDe(CATEGORIAS[5])[0].command!({});
    http.expectOne('/api/categorias/cat-pet/ativar').flush({});
    await recarregar();
    tela()['confirmarInativacao'](CATEGORIAS[0]);
    http.expectOne('/api/categorias/cat-alimentacao').flush(null, { status: 400, statusText: 'x' });
    tela()['reativar'](CATEGORIAS[5]);
    http.expectOne('/api/categorias/cat-pet/ativar').flush(null, { status: 400, statusText: 'x' });
    expect(avisos).toHaveBeenCalledTimes(4);
  });

  it('mostra erro ao carregar', async () => {
    tela()['categorias'].reload();
    TestBed.tick();
    http.expectOne(URL).flush(null, { status: 500, statusText: 'x' });
    await fixture.whenStable();
    expect(texto()).toContain('Não foi possível carregar as categorias.');
  });
});
