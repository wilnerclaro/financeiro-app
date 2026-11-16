import { Categoria, PrevistoRealizado } from '../../../../shared/api/api.models';

export interface LinhaInicial {
  id: string;
  nome: string;
  previsto: number;
  realizado: number;
  editavel: boolean;
  sub: boolean;
  filhos: string[];
}

export function linhasIniciais(
  categorias: Categoria[],
  relatorio: PrevistoRealizado | null,
  tipo: 'RECEITA' | 'DESPESA',
): LinhaInicial[] {
  const ativas = categorias.filter((c) => c.tipo === tipo);
  const grupos = (tipo === 'RECEITA' ? relatorio?.receitas : relatorio?.despesas)?.grupos ?? [];
  const linhas: LinhaInicial[] = [];
  for (const grupo of grupos) {
    linhas.push({
      id: grupo.categoriaId,
      nome: grupo.nome,
      previsto: grupo.valores.previsto,
      realizado: grupo.valores.realizado,
      editavel: !grupo.subitens.length && ativas.some((c) => c.id === grupo.categoriaId),
      sub: false,
      filhos: grupo.subitens.map((f) => f.categoriaId),
    });
    for (const filho of grupo.subitens)
      linhas.push({
        id: filho.categoriaId,
        nome: filho.nome,
        previsto: filho.valores.previsto,
        realizado: filho.valores.realizado,
        editavel: ativas.some((c) => c.id === filho.categoriaId),
        sub: true,
        filhos: [],
      });
  }
  for (const categoria of ativas) {
    if (!linhas.some((l) => l.id === categoria.id))
      linhas.push({
        id: categoria.id,
        nome: categoria.nomeCategoriaPai
          ? `${categoria.nomeCategoriaPai} › ${categoria.nome}`
          : categoria.nome,
        previsto: 0,
        realizado: 0,
        editavel: !ativas.some((c) => c.categoriaPaiId === categoria.id),
        sub: !!categoria.categoriaPaiId,
        filhos: ativas.filter((c) => c.categoriaPaiId === categoria.id).map((c) => c.id),
      });
  }
  return linhas.map((linha) => {
    const filhos = [
      ...new Set([
        ...linha.filhos,
        ...ativas.filter((c) => c.categoriaPaiId === linha.id).map((c) => c.id),
      ]),
    ];
    return { ...linha, filhos, editavel: linha.editavel && !filhos.length };
  });
}
