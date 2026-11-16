import { Provider } from '@angular/core';
import { MessageService } from 'primeng/api';
import {
  Categoria,
  Conta,
  Fatura,
  Inicio,
  Lancamento,
  PrevistoRealizado,
  ResumoAnual,
  Transferencia,
  Valores,
} from '../app/shared/api/api.models';
import { AGORA } from '../app/shared/util/agora';

export const HOJE = new Date(2026, 10, 15, 10, 0, 0);

export function provedoresDeTeste(): Provider[] {
  return [{ provide: AGORA, useValue: () => HOJE }, MessageService];
}

export function pagina<T>(content: T[]) {
  return {
    content,
    page: { size: content.length, number: 0, totalElements: content.length, totalPages: 1 },
  };
}

export function lancamento(dados: Partial<Lancamento> = {}): Lancamento {
  return {
    id: 'l1',
    contaId: 'c1',
    nomeConta: 'Banco Azul',
    categoriaId: 'cat-mercado',
    nomeCategoria: 'Supermercado',
    descricao: 'Mercado',
    tipo: 'DESPESA',
    valor: 100,
    dataCompetencia: '2026-11-10',
    dataVencimento: '2026-11-10',
    dataPagamento: '2026-11-10',
    dataCompra: null,
    status: 'PAGO',
    observacao: null,
    ...dados,
  };
}

export function conta(dados: Partial<Conta> = {}): Conta {
  return {
    id: 'c1',
    nome: 'Banco Azul',
    tipo: 'CONTA_CORRENTE',
    saldoInicial: 1000,
    saldoAtual: 1500,
    variacaoDesdeSaldoInicial: 500,
    ativa: true,
    diaFechamento: null,
    diaVencimento: null,
    ...dados,
  };
}

export const CARTAO = conta({
  id: 'c3',
  nome: 'Cartão Roxo',
  tipo: 'CARTAO_CREDITO',
  saldoInicial: 0,
  saldoAtual: 0,
  variacaoDesdeSaldoInicial: 0,
  diaFechamento: 5,
  diaVencimento: 12,
});

export const CONTAS: Conta[] = [
  conta(),
  conta({
    id: 'c2',
    nome: 'Carteira',
    tipo: 'DINHEIRO',
    saldoAtual: 80,
    variacaoDesdeSaldoInicial: -20,
  }),
  CARTAO,
];

export function categoria(dados: Partial<Categoria> = {}): Categoria {
  return {
    id: 'cat-mercado',
    categoriaPaiId: 'cat-alimentacao',
    nomeCategoriaPai: 'Alimentação',
    nome: 'Supermercado',
    tipo: 'DESPESA',
    fonteReceita: null,
    ativa: true,
    ...dados,
  };
}

export const CATEGORIAS: Categoria[] = [
  categoria({
    id: 'cat-alimentacao',
    categoriaPaiId: null,
    nomeCategoriaPai: null,
    nome: 'Alimentação',
  }),
  categoria(),
  categoria({ id: 'cat-padaria', nome: 'Padaria' }),
  categoria({
    id: 'cat-luz',
    categoriaPaiId: 'cat-moradia',
    nomeCategoriaPai: 'Moradia',
    nome: 'Conta de luz',
  }),
  categoria({ id: 'cat-moradia', categoriaPaiId: null, nomeCategoriaPai: null, nome: 'Moradia' }),
  categoria({
    id: 'cat-pet',
    categoriaPaiId: null,
    nomeCategoriaPai: null,
    nome: 'Pet',
    ativa: false,
  }),
  categoria({
    id: 'cat-renda',
    categoriaPaiId: null,
    nomeCategoriaPai: null,
    nome: 'Receitas ativas',
    tipo: 'RECEITA',
    fonteReceita: 'ATIVA',
  }),
  categoria({
    id: 'cat-salario',
    categoriaPaiId: 'cat-renda',
    nomeCategoriaPai: 'Receitas ativas',
    nome: 'Salários',
    tipo: 'RECEITA',
    fonteReceita: 'ATIVA',
  }),
];

export function valores(previsto: number, realizado: number): Valores {
  return {
    previsto,
    realizado,
    desvio: realizado - previsto,
    percentualRealizado: previsto ? Math.round((realizado / previsto) * 10000) / 10000 : 0,
  };
}

export function inicio(dados: Partial<Inicio> = {}): Inicio {
  return {
    ano: 2026,
    mes: 11,
    entrou: 8000,
    saiu: 5000,
    sobrou: 3000,
    percentualGuardado: 0.375,
    meta30: { percentual: 0.3, valor: 2400, atingiu: true, falta: 0 },
    meta50: { percentual: 0.5, valor: 4000, atingiu: false, falta: 1000 },
    comparativoMesAnterior: {
      diferencaReceitas: 200,
      evolucaoReceitas: 0.0256,
      diferencaDespesas: -300,
      evolucaoDespesas: -0.0566,
    },
    orcamento: {
      receitasPrevistas: 8000,
      despesasPrevistas: 6000,
      percentualDespesasUsado: 0.8333,
      restaDasDespesas: 1000,
      gruposMaisUsados: [
        {
          categoriaId: 'cat-alimentacao',
          nome: 'Alimentação',
          grupo: null,
          previsto: 1000,
          realizado: 1100,
          desvio: 100,
          percentualRealizado: 1.1,
        },
      ],
    },
    independenciometro: {
      indice: 0.212,
      receitaPassiva: 1060,
      despesaTotal: 5000,
      indiceMesAnterior: 0.2,
      variacao: 0.012,
      faixa: 0.2,
      proximoMarco: 0.3,
      faltaParaProximoMarco: 440,
    },
    evolucao: [
      {
        ano: 2026,
        mes: 10,
        entrou: 7800,
        saiu: 6000,
        sobrou: 1800,
        percentualGuardado: 0.2308,
        atingiuMeta30: false,
        receitaPassiva: 1000,
        independenciometro: 0.2,
        evolucaoReceitas: 0,
        evolucaoDespesas: 0,
      },
      {
        ano: 2026,
        mes: 11,
        entrou: 8000,
        saiu: 5000,
        sobrou: 3000,
        percentualGuardado: 0.375,
        atingiuMeta30: true,
        receitaPassiva: 1060,
        independenciometro: 0.212,
        evolucaoReceitas: 0.0256,
        evolucaoDespesas: -0.0566,
      },
    ],
    paraPagar: {
      total: 290,
      quantidade: 2,
      itens: [
        lancamento({
          id: 'luz',
          descricao: 'Conta de luz',
          valor: 200,
          status: 'ATRASADO',
          dataVencimento: '2026-11-10',
          dataPagamento: null,
        }),
        lancamento({
          id: 'cinema',
          descricao: 'Cinema',
          valor: 90,
          status: 'PENDENTE',
          nomeConta: 'Cartão Roxo',
          dataCompra: '2026-11-02',
          dataVencimento: '2026-12-12',
          dataPagamento: null,
        }),
      ],
    },
    categoriasAcimaDoPrevisto: [
      {
        categoriaId: 'cat-mercado',
        nome: 'Supermercado',
        grupo: 'Alimentação',
        previsto: 1000,
        realizado: 1100,
        desvio: 100,
        percentualRealizado: 1.1,
      },
      {
        categoriaId: 'cat-luz',
        nome: 'Conta de luz',
        grupo: 'Moradia',
        previsto: 200,
        realizado: 190,
        desvio: -10,
        percentualRealizado: 0.95,
      },
    ],
    receitasAcimaDoPrevisto: [],
    ultimosLancamentos: [
      lancamento(),
      lancamento({ id: 'l2', descricao: 'Salário', tipo: 'RECEITA', valor: 8000 }),
    ],
    lancamentoRapido: {
      contaId: 'c2',
      nomeConta: 'Carteira',
      categoriasMaisUsadas: [
        { categoriaId: 'cat-mercado', nome: 'Supermercado', quantidade: 9 },
        { categoriaId: 'cat-padaria', nome: 'Padaria', quantidade: 4 },
        { categoriaId: 'cat-sumiu', nome: 'Inativa', quantidade: 1 },
      ],
    },
    saldoEmContas: 1580,
    ...dados,
  };
}

export const INICIO_VAZIO = inicio({
  entrou: 0,
  saiu: 0,
  sobrou: 0,
  ultimosLancamentos: [],
  paraPagar: { total: 0, quantidade: 0, itens: [] },
});

export function previstoRealizado(): PrevistoRealizado {
  return {
    ano: 2026,
    mes: 11,
    receitas: {
      total: valores(8000, 8200),
      grupos: [
        {
          categoriaId: 'cat-renda',
          nome: 'Receitas ativas',
          fonteReceita: 'ATIVA',
          ativa: true,
          valores: valores(8000, 8200),
          subitens: [
            {
              categoriaId: 'cat-salario',
              nome: 'Salários',
              fonteReceita: 'ATIVA',
              ativa: true,
              valores: valores(8000, 8200),
            },
          ],
        },
      ],
    },
    receitasFonteAtiva: valores(8000, 8200),
    receitasFontePassiva: valores(0, 0),
    despesas: {
      total: valores(1200, 1100),
      grupos: [
        {
          categoriaId: 'cat-alimentacao',
          nome: 'Alimentação',
          fonteReceita: null,
          ativa: true,
          valores: valores(1000, 1100),
          subitens: [
            {
              categoriaId: 'cat-mercado',
              nome: 'Supermercado',
              fonteReceita: null,
              ativa: true,
              valores: valores(1000, 1100),
            },
          ],
        },
        {
          categoriaId: 'cat-moradia',
          nome: 'Moradia',
          fonteReceita: null,
          ativa: true,
          valores: valores(200, 0),
          subitens: [],
        },
        {
          categoriaId: 'cat-pet',
          nome: 'Pet',
          fonteReceita: null,
          ativa: true,
          valores: valores(0, 0),
          subitens: [],
        },
      ],
    },
    saldo: valores(6800, 7100),
  };
}

export const RESUMO_ANUAL: ResumoAnual = {
  ano: 2026,
  linhas: [
    {
      categoriaId: 'cat-alimentacao',
      nome: 'Alimentação',
      categoriaPaiId: null,
      tipo: 'DESPESA',
      totalPrevisto: 12000,
      totalRealizado: 11000,
      mediaPrevisto: 1000,
      mediaRealizado: 916.67,
      meses: [],
    },
    {
      categoriaId: 'cat-mercado',
      nome: 'Supermercado',
      categoriaPaiId: 'cat-alimentacao',
      tipo: 'DESPESA',
      totalPrevisto: 12000,
      totalRealizado: 11000,
      mediaPrevisto: 1000,
      mediaRealizado: 916.67,
      meses: [],
    },
    {
      categoriaId: 'cat-pet',
      nome: 'Pet',
      categoriaPaiId: null,
      tipo: 'DESPESA',
      totalPrevisto: 0,
      totalRealizado: 0,
      mediaPrevisto: 0,
      mediaRealizado: 0,
      meses: [],
    },
  ],
};

export const TRANSFERENCIA: Transferencia = {
  id: 't1',
  contaOrigemId: 'c1',
  nomeContaOrigem: 'Banco Azul',
  contaDestinoId: 'c2',
  nomeContaDestino: 'Carteira',
  valor: 300,
  dataTransferencia: '2026-11-09',
  descricao: 'Saque',
  cancelada: false,
};

export function fatura(dados: Partial<Fatura> = {}): Fatura {
  return {
    contaId: 'c3',
    nomeConta: 'Cartão Roxo',
    ano: 2026,
    mes: 11,
    dataFechamento: '2026-11-05',
    dataVencimento: '2026-11-12',
    fechada: true,
    total: 440,
    lancamentos: [
      lancamento({
        id: 'f1',
        descricao: 'Cinema',
        valor: 90,
        status: 'ATRASADO',
        contaId: 'c3',
        nomeConta: 'Cartão Roxo',
        dataCompra: '2026-11-02',
        dataPagamento: null,
      }),
      lancamento({
        id: 'f2',
        descricao: 'Loja',
        valor: 350,
        status: 'ATRASADO',
        contaId: 'c3',
        nomeConta: 'Cartão Roxo',
        dataCompra: '2026-10-28',
        dataPagamento: null,
      }),
    ],
    ...dados,
  };
}
