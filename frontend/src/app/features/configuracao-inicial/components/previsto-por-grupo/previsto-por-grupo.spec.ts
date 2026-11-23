import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LinhaInicial } from '../../pages/configuracao-inicial/orcamento-inicial';
import { BlocoDoPrevisto, PrevistoPorGrupo } from './previsto-por-grupo';

const linha = (dados: Partial<LinhaInicial>): LinhaInicial => ({
  id: 'x',
  nome: 'X',
  previsto: 0,
  realizado: 0,
  editavel: true,
  sub: true,
  raiz: false,
  filhos: [],
  ...dados,
});

const BLOCOS: BlocoDoPrevisto[] = [
  {
    titulo: 'Receitas',
    tipo: 'RECEITA',
    linhas: [
      linha({ id: 'renda', nome: 'Receitas ativas', sub: false, raiz: true, editavel: false }),
      linha({ id: 'decimo', nome: '13º Salário' }),
      linha({ id: 'salario', nome: 'Salários', previsto: 10000, realizado: 12130 }),
      linha({ id: 'avulsa', nome: 'Renda extra', sub: false, raiz: true }),
    ],
  },
  {
    titulo: 'Despesas',
    tipo: 'DESPESA',
    linhas: [
      linha({
        id: 'alimentacao',
        nome: 'Alimentação',
        sub: false,
        raiz: true,
        editavel: false,
        previsto: 1200,
        filhos: ['mercado', 'padaria'],
      }),
      linha({ id: 'mercado', nome: 'Supermercado', previsto: 1200 }),
      linha({ id: 'padaria', nome: 'Padaria' }),
      linha({ id: 'moradia', nome: 'Moradia', sub: false, raiz: true, editavel: false }),
      linha({ id: 'luz', nome: 'Conta de luz' }),
      linha({ id: 'antiga', nome: 'Inativa', editavel: false, previsto: 50 }),
    ],
  },
];

describe('PrevistoPorGrupo', () => {
  let fixture: ComponentFixture<PrevistoPorGrupo>;
  const tela = () => fixture.componentInstance;
  const el = () => fixture.nativeElement as HTMLElement;
  const texto = () => el().textContent!.replace(/\s+/g, ' ');
  const cabecalhos = () => Array.from(el().querySelectorAll<HTMLButtonElement>('.cab-grupo'));

  beforeEach(async () => {
    fixture = TestBed.createComponent(PrevistoPorGrupo);
    fixture.componentRef.setInput('blocos', BLOCOS);
    await fixture.whenStable();
  });

  it('começa em Receitas com os grupos fechados e o total de cada um', () => {
    expect(cabecalhos().map((b) => b.getAttribute('aria-expanded'))).toEqual(['false']);
    expect(cabecalhos()[0].textContent!.replace(/\s+/g, '')).toContain('Receitasativas2');
    expect(texto()).toContain('R$ 0,00');
    expect(el().querySelector('#previsto-salario')).toBeNull();
    expect(el().querySelector('#previsto-avulsa')).not.toBeNull();
  });

  it('abre e fecha um grupo e todos de uma vez', async () => {
    cabecalhos()[0].click();
    await fixture.whenStable();
    expect(el().querySelector('#previsto-salario')).not.toBeNull();
    expect(texto()).toContain('Fechar todos os grupos');

    cabecalhos()[0].click();
    await fixture.whenStable();
    expect(el().querySelector('#previsto-salario')).toBeNull();

    tela()['trocarTipo']('DESPESA');
    await fixture.whenStable();
    tela()['alternarTodos']();
    await fixture.whenStable();
    expect(cabecalhos().map((b) => b.getAttribute('aria-expanded'))).toEqual(['true', 'true']);
    tela()['alternarTodos']();
    await fixture.whenStable();
    expect(cabecalhos().map((b) => b.getAttribute('aria-expanded'))).toEqual(['false', 'false']);
  });

  it('soma no total do grupo o que foi editado nos filhos e avisa a edição', async () => {
    const editar = vi.fn();
    tela().editar.subscribe(editar);
    tela()['trocarTipo']('DESPESA');
    fixture.componentRef.setInput('editados', new Map([['padaria', 300]]));
    await fixture.whenStable();

    expect(cabecalhos()[0].textContent!.replace(/\s+/g, ' ')).toContain('R$ 1.500,00');
    tela()['alternar'](tela()['grupos']()[0]);
    tela()['alternar'](tela()['grupos']()[1]);
    await fixture.whenStable();
    expect(texto()).toContain('R$ 50,00');
    expect(el().querySelector('#previsto-antiga')).toBeNull();
  });

  it('busca sem acento e abre os grupos com resultado', async () => {
    tela()['trocarTipo']('DESPESA');
    tela()['busca'].set('LUZ');
    await fixture.whenStable();

    expect(
      tela()
        ['grupos']()
        .map((g) => [g.raiz.nome, g.filhos.map((f) => f.nome)]),
    ).toEqual([['Moradia', ['Conta de luz']]]);
    expect(cabecalhos()[0].getAttribute('aria-expanded')).toBe('true');
    expect(texto()).not.toContain('Abrir todos os grupos');
    tela()['alternar'](tela()['grupos']()[0]);
    expect(tela()['abertos']().size).toBe(0);

    tela()['busca'].set('aliment');
    expect(tela()['grupos']()[0].filhos).toHaveLength(2);
  });

  it('sem resultado no tipo aberto, oferece o outro tipo', async () => {
    tela()['trocarTipo']('DESPESA');
    tela()['busca'].set('salario');
    await fixture.whenStable();

    expect(texto()).toContain('Nenhuma categoria com “salario” em Despesas.');
    const ver = Array.from(el().querySelectorAll<HTMLButtonElement>('button')).find((b) =>
      b.textContent?.includes('Ver 2 em Receitas'),
    )!;
    ver.click();
    await fixture.whenStable();
    expect(tela()['tipo']()).toBe('RECEITA');
    expect(el().querySelector('#previsto-salario')).not.toBeNull();

    tela()['busca'].set('nada disso');
    await fixture.whenStable();
    expect(tela()['resultadoNoOutroTipo']()).toBeNull();
  });

  it('mostra aviso quando não há categorias', async () => {
    fixture.componentRef.setInput('blocos', [{ titulo: 'Receitas', tipo: 'RECEITA', linhas: [] }]);
    await fixture.whenStable();
    expect(texto()).toContain('Nenhuma categoria disponível.');
    expect(tela()['resultadoNoOutroTipo']()).toBeNull();
  });
});
