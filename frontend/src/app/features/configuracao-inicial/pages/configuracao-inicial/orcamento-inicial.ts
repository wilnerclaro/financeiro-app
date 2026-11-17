import { Categoria, ItemDoOrcamento, PrevistoRealizado } from '../../../../shared/api/api.models';

export interface LinhaInicial {
  id: string;
  nome: string;
  previsto: number;
  realizado: number;
  editavel: boolean;
  sub: boolean;
  filhos: string[];
}

interface CategoriaDoOrcamento {
  id: string;
  nome: string;
  paiId: string | null;
  nomePai: string | null;
  ativa: boolean;
  previsto: number;
  realizado: number;
}

export function linhasIniciais(
  categorias: Categoria[],
  relatorio: PrevistoRealizado | null,
  tipo: 'RECEITA' | 'DESPESA',
): LinhaInicial[] {
  const porId = new Map<string, CategoriaDoOrcamento>();
  for (const c of categorias.filter((categoria) => categoria.tipo === tipo)) {
    porId.set(c.id, {
      id: c.id,
      nome: c.nome,
      paiId: c.categoriaPaiId,
      nomePai: c.nomeCategoriaPai,
      ativa: true,
      previsto: 0,
      realizado: 0,
    });
  }

  const comValor = (item: ItemDoOrcamento, paiId: string | null) => {
    const existente = porId.get(item.categoriaId);
    porId.set(item.categoriaId, {
      id: item.categoriaId,
      nome: item.nome,
      paiId,
      nomePai: existente?.nomePai ?? null,
      ativa: existente?.ativa ?? false,
      previsto: item.valores.previsto,
      realizado: item.valores.realizado,
    });
  };
  const grupos = (tipo === 'RECEITA' ? relatorio?.receitas : relatorio?.despesas)?.grupos ?? [];
  for (const grupo of grupos) {
    comValor(grupo, null);
    grupo.subitens.forEach((sub) => comValor(sub, grupo.categoriaId));
  }

  const todas = [...porId.values()];
  const porNome = (a: CategoriaDoOrcamento, b: CategoriaDoOrcamento) =>
    a.nome.localeCompare(b.nome, 'pt-BR');
  const filhosDe = (id: string) => todas.filter((c) => c.paiId === id).sort(porNome);

  const linhas: LinhaInicial[] = [];
  for (const raiz of todas.filter((c) => !c.paiId || !porId.has(c.paiId)).sort(porNome)) {
    const filhos = filhosDe(raiz.id);
    linhas.push({
      id: raiz.id,
      nome: raiz.paiId && raiz.nomePai ? `${raiz.nomePai} › ${raiz.nome}` : raiz.nome,
      previsto: raiz.previsto,
      realizado: raiz.realizado,
      editavel: raiz.ativa && !filhos.length,
      sub: !!raiz.paiId,
      filhos: filhos.map((f) => f.id),
    });
    for (const filho of filhos) {
      linhas.push({
        id: filho.id,
        nome: filho.nome,
        previsto: filho.previsto,
        realizado: filho.realizado,
        editavel: filho.ativa,
        sub: true,
        filhos: [],
      });
    }
  }
  return linhas;
}
