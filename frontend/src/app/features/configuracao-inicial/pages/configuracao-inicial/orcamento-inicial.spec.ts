import { categoria, previstoRealizado, valores } from '../../../../../testing/dados-de-teste';
import { linhasIniciais } from './orcamento-inicial';

describe('linhasIniciais', () => {
  const alimentacao = categoria({
    id: 'cat-alimentacao',
    categoriaPaiId: null,
    nomeCategoriaPai: null,
    nome: 'Alimentação',
  });

  it('põe a subcategoria sem valor sob o grupo, em ordem de nome', () => {
    const relatorio = previstoRealizado();
    const linhas = linhasIniciais(
      [
        categoria({ id: 'cat-padaria', nome: 'Padaria' }),
        categoria(),
        alimentacao,
        categoria({ id: 'cat-acougue', nome: 'Açougue' }),
      ],
      relatorio,
      'DESPESA',
    );

    expect(linhas.slice(0, 4).map((l) => [l.nome, l.sub, l.editavel])).toEqual([
      ['Alimentação', false, false],
      ['Açougue', true, true],
      ['Padaria', true, true],
      ['Supermercado', true, true],
    ]);
    expect(linhas[0].filhos).toEqual(['cat-acougue', 'cat-padaria', 'cat-mercado']);
    expect(linhas[3].previsto).toBe(1000);
    expect(linhas[3].realizado).toBe(1100);
  });

  it('mantém categoria inativa com valor sem permitir edição', () => {
    const relatorio = previstoRealizado();
    const linhas = linhasIniciais([], relatorio, 'DESPESA');

    expect(linhas.find((l) => l.id === 'cat-mercado')).toMatchObject({
      sub: true,
      editavel: false,
    });
    expect(linhas.find((l) => l.id === 'cat-moradia')!.editavel).toBe(false);
  });

  it('mostra o caminho da subcategoria cujo grupo não está na lista', () => {
    const linhas = linhasIniciais(
      [
        categoria({
          id: 'cat-bonus',
          nome: 'Bônus',
          tipo: 'RECEITA',
          categoriaPaiId: 'x',
          nomeCategoriaPai: 'Renda',
        }),
      ],
      null,
      'RECEITA',
    );

    expect(linhas).toEqual([
      {
        id: 'cat-bonus',
        nome: 'Renda › Bônus',
        previsto: 0,
        realizado: 0,
        editavel: true,
        sub: true,
        raiz: true,
        filhos: [],
      },
    ]);
  });

  it('usa os valores do relatório para receita', () => {
    const relatorio = previstoRealizado();
    relatorio.receitas.grupos[0].subitens[0].valores = valores(8000, 0);
    const linhas = linhasIniciais([], relatorio, 'RECEITA');

    expect(linhas.map((l) => [l.nome, l.previsto])).toEqual([
      ['Receitas ativas', 8000],
      ['Salários', 8000],
    ]);
  });
});
