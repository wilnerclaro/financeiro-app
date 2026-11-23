import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';
import { categoria, provedoresDeTeste } from '../../../../../testing/dados-de-teste';
import { CategoriaDialogo, NovaCategoria } from './categoria-dialogo';

describe('CategoriaDialogo', () => {
  let fixture: ComponentFixture<CategoriaDialogo>;
  let http: HttpTestingController;
  const tela = () => fixture.componentInstance;
  const texto = () => document.body.textContent!.replace(/\s+/g, ' ');

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ...provedoresDeTeste()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CategoriaDialogo);
  });

  afterEach(() => http.verify());

  async function abrir(pedido: NovaCategoria): Promise<void> {
    fixture.componentRef.setInput('pedido', pedido);
    fixture.componentRef.setInput('grupos', [{ label: 'Alimentação', value: 'cat-alimentacao' }]);
    await fixture.whenStable();
  }

  it('cria subcategoria com o nome pesquisado e o grupo já escolhido', async () => {
    const salva = vi.fn();
    tela().salva.subscribe(salva);
    await abrir({ tipo: 'DESPESA', categoriaPaiId: 'cat-alimentacao', nome: ' Feira ' });

    expect(texto()).toContain('Nova subcategoria');
    expect(tela()['nome']()).toBe('Feira');
    tela()['salvar']();
    const pedido = http.expectOne('/api/categorias');
    expect(pedido.request.body).toEqual({
      nome: 'Feira',
      tipo: 'DESPESA',
      categoriaPaiId: 'cat-alimentacao',
      fonteReceita: null,
    });
    const criada = categoria({ id: 'cat-feira', nome: 'Feira' });
    pedido.flush(criada);
    expect(salva).toHaveBeenCalledWith(criada);
    expect(tela()['salvando']()).toBe(false);
  });

  it('grupo de receita pede a fonte e exige nome', async () => {
    await abrir({ tipo: 'RECEITA', categoriaPaiId: null, nome: '' });

    expect(texto()).toContain('Nova categoria de receita');
    expect(texto()).toContain('Fonte da receita');
    tela()['salvar']();
    await fixture.whenStable();
    http.expectNone('/api/categorias');
    expect(texto()).toContain('Informe o nome da categoria.');

    tela()['nome'].set('Aluguel do apê');
    tela()['fonte'].set('PASSIVA');
    tela()['salvar']();
    tela()['salvar']();
    const pedido = http.expectOne('/api/categorias');
    expect(pedido.request.body).toEqual(
      expect.objectContaining({ categoriaPaiId: null, fonteReceita: 'PASSIVA', tipo: 'RECEITA' }),
    );
    pedido.flush(categoria({ tipo: 'RECEITA' }));
  });

  it('mostra o erro da API e continua aberto', async () => {
    const avisos = vi.spyOn(TestBed.inject(MessageService), 'add');
    const salva = vi.fn();
    tela().salva.subscribe(salva);
    await abrir({ tipo: 'DESPESA', categoriaPaiId: null, nome: 'Moradia' });

    expect(texto()).toContain('Nova categoria de despesa');
    tela()['salvar']();
    http
      .expectOne('/api/categorias')
      .flush(
        { message: 'Já existe uma categoria com esse nome.' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(avisos).toHaveBeenCalledWith(
      expect.objectContaining({ detail: 'Já existe uma categoria com esse nome.' }),
    );
    expect(salva).not.toHaveBeenCalled();
    expect(tela()['salvando']()).toBe(false);
  });
});
