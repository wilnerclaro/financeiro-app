export interface Pagina<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export type TipoLancamento = 'RECEITA' | 'DESPESA';
export type StatusLancamento = 'PENDENTE' | 'PAGO' | 'ATRASADO' | 'CANCELADO';
export type TipoCategoria = 'RECEITA' | 'DESPESA';
export type FonteReceita = 'ATIVA' | 'PASSIVA';
export type TipoConta =
  'CONTA_CORRENTE' | 'POUPANCA' | 'DINHEIRO' | 'INVESTIMENTO' | 'CARTAO_CREDITO' | 'OUTRA';

export interface Lancamento {
  id: string;
  contaId: string;
  nomeConta: string;
  categoriaId: string;
  nomeCategoria: string;
  descricao: string;
  tipo: TipoLancamento;
  valor: number;
  dataCompetencia: string;
  dataVencimento: string | null;
  dataPagamento: string | null;
  dataCompra: string | null;
  status: StatusLancamento;
  observacao: string | null;
}

export interface LancamentoRequest {
  contaId: string;
  categoriaId: string;
  descricao: string;
  tipo: TipoLancamento;
  valor: number;
  dataCompetencia: string;
  dataVencimento: string | null;
  dataPagamento: string | null;
  observacao: string | null;
}

export interface FiltroLancamentos {
  tipo?: TipoLancamento;
  status?: StatusLancamento;
  contaId?: string;
  categoriaId?: string;
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
}

export interface Fatura {
  contaId: string;
  nomeConta: string;
  ano: number;
  mes: number;
  dataFechamento: string;
  dataVencimento: string;
  fechada: boolean;
  total: number;
  lancamentos: Lancamento[];
}

export interface PagarFaturaRequest {
  contaId: string;
  ano: number;
  mes: number;
  contaPagamentoId: string;
  dataPagamento: string;
}

export interface Conta {
  id: string;
  nome: string;
  tipo: TipoConta;
  saldoInicial: number;
  saldoAtual: number;
  variacaoDesdeSaldoInicial: number;
  ativa: boolean;
  diaFechamento: number | null;
  diaVencimento: number | null;
}

export interface ContaRequest {
  nome: string;
  tipo: TipoConta;
  diaFechamento: number | null;
  diaVencimento: number | null;
}

export interface CriarContaRequest extends ContaRequest {
  saldoInicial: number;
}

export interface ResumoContas {
  patrimonio: number;
  porTipo: { tipo: TipoConta; total: number; quantidadeDeContas: number }[];
}

export interface Categoria {
  id: string;
  categoriaPaiId: string | null;
  nomeCategoriaPai: string | null;
  nome: string;
  tipo: TipoCategoria;
  fonteReceita: FonteReceita | null;
  ativa: boolean;
}

export interface CategoriaRequest {
  categoriaPaiId: string | null;
  nome: string;
  tipo: TipoCategoria;
  fonteReceita: FonteReceita | null;
}

export interface Transferencia {
  id: string;
  contaOrigemId: string;
  nomeContaOrigem: string;
  contaDestinoId: string;
  nomeContaDestino: string;
  valor: number;
  dataTransferencia: string;
  descricao: string | null;
  cancelada: boolean;
}

export interface TransferenciaRequest {
  contaOrigemId: string;
  contaDestinoId: string;
  valor: number;
  dataTransferencia: string;
  descricao: string | null;
}

export interface Valores {
  previsto: number;
  realizado: number;
  desvio: number;
  percentualRealizado: number;
}

export interface ItemDoOrcamento {
  categoriaId: string;
  nome: string;
  fonteReceita: FonteReceita | null;
  ativa: boolean;
  valores: Valores;
}

export interface GrupoDoOrcamento extends ItemDoOrcamento {
  subitens: ItemDoOrcamento[];
}

export interface BlocoDoOrcamento {
  total: Valores;
  grupos: GrupoDoOrcamento[];
}

export interface PrevistoRealizado {
  ano: number;
  mes: number;
  receitas: BlocoDoOrcamento;
  receitasFonteAtiva: Valores;
  receitasFontePassiva: Valores;
  despesas: BlocoDoOrcamento;
  saldo: Valores;
}

export interface MesDaLinha {
  mes: number;
  previsto: number;
  realizado: number;
  percentualRealizado: number;
}

export interface LinhaDoAno {
  categoriaId: string;
  nome: string;
  categoriaPaiId: string | null;
  tipo: TipoCategoria;
  fonteReceita: FonteReceita | null;
  totalPrevisto: number;
  totalRealizado: number;
  mediaPrevisto: number;
  mediaRealizado: number;
  meses: MesDaLinha[];
}

export interface IndicadoresDoAno {
  entrou: number;
  saiu: number;
  guardou: number;
  percentualGuardado: number;
  mediaEntrou: number;
  mediaSaiu: number;
  previstoReceitas: number;
  previstoDespesas: number;
}

export interface TotaisDoMes {
  mes: number;
  receitas: Valores;
  receitasFonteAtiva: Valores;
  receitasFontePassiva: Valores;
  despesas: Valores;
  guardou: number;
  percentualGuardado: number;
  evolucaoDespesas: number | null;
  independenciometro: number;
}

export interface ResumoAnual {
  ano: number;
  linhas: LinhaDoAno[];
  grupos: LinhaDoAno[];
  indicadores: IndicadoresDoAno;
  meses: TotaisDoMes[];
}

export interface CopiaDePrevisto {
  meses: { competencia: string; gravadas: number; mantidas: number }[];
}

export interface CategoriaComPrevisto {
  categoriaId: string;
  nome: string;
  grupo: string | null;
  previsto: number;
  realizado: number;
  desvio: number;
  percentualRealizado: number;
}

export interface MesDaEvolucao {
  ano: number;
  mes: number;
  entrou: number;
  saiu: number;
  sobrou: number;
  percentualGuardado: number;
  atingiuMeta30: boolean;
  receitaPassiva: number;
  independenciometro: number;
  evolucaoReceitas: number;
  evolucaoDespesas: number;
}

export interface MetaDeGuardar {
  percentual: number;
  valor: number;
  atingiu: boolean;
  falta: number;
}

export interface Inicio {
  ano: number;
  mes: number;
  entrou: number;
  saiu: number;
  sobrou: number;
  percentualGuardado: number;
  meta30: MetaDeGuardar;
  meta50: MetaDeGuardar;
  comparativoMesAnterior: {
    diferencaReceitas: number;
    evolucaoReceitas: number;
    diferencaDespesas: number;
    evolucaoDespesas: number;
  };
  orcamento: {
    receitasPrevistas: number;
    despesasPrevistas: number;
    percentualDespesasUsado: number;
    restaDasDespesas: number;
    gruposMaisUsados: CategoriaComPrevisto[];
  };
  independenciometro: {
    indice: number;
    receitaPassiva: number;
    despesaTotal: number;
    indiceMesAnterior: number;
    variacao: number;
    faixa: number;
    proximoMarco: number | null;
    faltaParaProximoMarco: number;
  };
  evolucao: MesDaEvolucao[];
  paraPagar: { total: number; quantidade: number; itens: Lancamento[] };
  categoriasAcimaDoPrevisto: CategoriaComPrevisto[];
  receitasAcimaDoPrevisto: CategoriaComPrevisto[];
  ultimosLancamentos: Lancamento[];
  lancamentoRapido: {
    contaId: string | null;
    nomeConta: string | null;
    categoriasMaisUsadas: { categoriaId: string; nome: string; quantidade: number }[];
  };
  saldoEmContas: number;
}
