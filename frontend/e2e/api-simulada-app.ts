import { Page, Route } from '@playwright/test';
import { SENHA_CERTA, simularApiDeAutenticacao } from './api-simulada';

const pagina = <T>(content: T[]) => ({
  content,
  page: { size: content.length, number: 0, totalElements: content.length, totalPages: 1 },
});

const valores = (previsto: number, realizado: number) => ({
  previsto,
  realizado,
  desvio: realizado - previsto,
  percentualRealizado: previsto ? realizado / previsto : 0,
});

export const CONTAS = [
  {
    id: 'c1',
    nome: 'Banco Azul',
    tipo: 'CONTA_CORRENTE',
    saldoInicial: 1000,
    saldoAtual: 1500,
    variacaoDesdeSaldoInicial: 500,
    ativa: true,
    diaFechamento: null,
    diaVencimento: null,
  },
  {
    id: 'c3',
    nome: 'Cartão Roxo',
    tipo: 'CARTAO_CREDITO',
    saldoInicial: 0,
    saldoAtual: 0,
    variacaoDesdeSaldoInicial: 0,
    ativa: true,
    diaFechamento: 5,
    diaVencimento: 12,
  },
];

const CATEGORIAS = [
  {
    id: 'g1',
    categoriaPaiId: null,
    nomeCategoriaPai: null,
    nome: 'Alimentação',
    tipo: 'DESPESA',
    fonteReceita: null,
    ativa: true,
  },
  {
    id: 'mercado',
    categoriaPaiId: 'g1',
    nomeCategoriaPai: 'Alimentação',
    nome: 'Supermercado',
    tipo: 'DESPESA',
    fonteReceita: null,
    ativa: true,
  },
  {
    id: 'padaria',
    categoriaPaiId: 'g1',
    nomeCategoriaPai: 'Alimentação',
    nome: 'Padaria',
    tipo: 'DESPESA',
    fonteReceita: null,
    ativa: true,
  },
  {
    id: 'g2',
    categoriaPaiId: null,
    nomeCategoriaPai: null,
    nome: 'Receitas ativas',
    tipo: 'RECEITA',
    fonteReceita: 'ATIVA',
    ativa: true,
  },
  {
    id: 'salario',
    categoriaPaiId: 'g2',
    nomeCategoriaPai: 'Receitas ativas',
    nome: 'Salários',
    tipo: 'RECEITA',
    fonteReceita: 'ATIVA',
    ativa: true,
  },
];

function lancamento(dados: Record<string, unknown>) {
  return {
    id: 'l1',
    contaId: 'c1',
    nomeConta: 'Banco Azul',
    categoriaId: 'mercado',
    nomeCategoria: 'Supermercado',
    descricao: 'Mercado',
    tipo: 'DESPESA',
    valor: 250,
    dataCompetencia: '2026-11-10',
    dataVencimento: '2026-11-10',
    dataPagamento: '2026-11-10',
    dataCompra: null,
    status: 'PAGO',
    observacao: null,
    ...dados,
  };
}

const COMPRA = lancamento({
  id: 'compra',
  contaId: 'c3',
  nomeConta: 'Cartão Roxo',
  descricao: 'Cinema',
  valor: 90,
  dataCompra: '2026-11-02',
  dataCompetencia: '2026-11-12',
  dataVencimento: '2026-11-12',
  dataPagamento: null,
  status: 'ATRASADO',
});

const LANCAMENTOS = [
  lancamento({}),
  lancamento({
    id: 'sal',
    descricao: 'Salário',
    tipo: 'RECEITA',
    valor: 8000,
    categoriaId: 'salario',
    nomeCategoria: 'Salários',
    dataCompetencia: '2026-11-05',
  }),
  COMPRA,
];

function inicio() {
  return {
    ano: 2026,
    mes: 11,
    entrou: 8000,
    saiu: 340,
    sobrou: 7660,
    percentualGuardado: 0.9575,
    meta30: { percentual: 0.3, valor: 2400, atingiu: true, falta: 0 },
    meta50: { percentual: 0.5, valor: 4000, atingiu: true, falta: 0 },
    comparativoMesAnterior: {
      diferencaReceitas: 0,
      evolucaoReceitas: 0,
      diferencaDespesas: -10,
      evolucaoDespesas: -0.03,
    },
    orcamento: {
      receitasPrevistas: 8000,
      despesasPrevistas: 1000,
      percentualDespesasUsado: 0.34,
      restaDasDespesas: 660,
      gruposMaisUsados: [
        {
          categoriaId: 'g1',
          nome: 'Alimentação',
          grupo: null,
          previsto: 1000,
          realizado: 340,
          desvio: -660,
          percentualRealizado: 0.34,
        },
      ],
    },
    independenciometro: {
      indice: 0,
      receitaPassiva: 0,
      despesaTotal: 340,
      indiceMesAnterior: 0,
      variacao: 0,
      faixa: 0,
      proximoMarco: 0.1,
      faltaParaProximoMarco: 34,
    },
    evolucao: [10, 11].map((mes) => ({
      ano: 2026,
      mes,
      entrou: 8000,
      saiu: 340,
      sobrou: 7660,
      percentualGuardado: 0.9575,
      atingiuMeta30: true,
      receitaPassiva: 0,
      independenciometro: 0,
      evolucaoReceitas: 0,
      evolucaoDespesas: 0,
    })),
    paraPagar: { total: 90, quantidade: 1, itens: [COMPRA] },
    categoriasAcimaDoPrevisto: [],
    receitasAcimaDoPrevisto: [],
    ultimosLancamentos: LANCAMENTOS.slice(0, 2),
    lancamentoRapido: {
      contaId: 'c1',
      nomeConta: 'Banco Azul',
      categoriasMaisUsadas: [
        { categoriaId: 'mercado', nome: 'Supermercado', quantidade: 5 },
        { categoriaId: 'padaria', nome: 'Padaria', quantidade: 2 },
      ],
    },
    saldoEmContas: 1500,
  };
}

const ORCAMENTO = {
  ano: 2026,
  mes: 11,
  receitas: { total: valores(8000, 8000), grupos: [] },
  receitasFonteAtiva: valores(8000, 8000),
  receitasFontePassiva: valores(0, 0),
  despesas: {
    total: valores(1000, 340),
    grupos: [
      {
        categoriaId: 'g1',
        nome: 'Alimentação',
        fonteReceita: null,
        ativa: true,
        valores: valores(1000, 340),
        subitens: [
          {
            categoriaId: 'mercado',
            nome: 'Supermercado',
            fonteReceita: null,
            ativa: true,
            valores: valores(1000, 340),
          },
        ],
      },
    ],
  },
  saldo: valores(7000, 7660),
};

const meses = (previsto: number, realizado: number) =>
  Array.from({ length: 12 }, (_, i) => ({
    mes: i + 1,
    previsto,
    realizado: i < 11 ? realizado : 0,
    percentualRealizado: i < 11 ? realizado / previsto : 0,
  }));

const grupoDoAno = (categoriaId: string, nome: string, previsto: number, realizado: number) => ({
  categoriaId,
  nome,
  categoriaPaiId: null,
  tipo: categoriaId === 'renda' ? 'RECEITA' : 'DESPESA',
  fonteReceita: categoriaId === 'renda' ? 'ATIVA' : null,
  totalPrevisto: previsto * 12,
  totalRealizado: realizado * 11,
  mediaPrevisto: previsto,
  mediaRealizado: realizado,
  meses: meses(previsto, realizado),
});

const RESUMO_DO_ANO = {
  ano: 2026,
  linhas: [{ ...grupoDoAno('mercado', 'Supermercado', 1000, 940), categoriaPaiId: 'g1' }],
  grupos: [
    grupoDoAno('renda', 'Receitas ativas', 8000, 8000),
    grupoDoAno('g1', 'Alimentação', 1000, 940),
  ],
  indicadores: {
    entrou: 88000,
    saiu: 10340,
    guardou: 77660,
    percentualGuardado: 0.8825,
    mediaEntrou: 8000,
    mediaSaiu: 940,
    previstoReceitas: 96000,
    previstoDespesas: 12000,
  },
  meses: Array.from({ length: 12 }, (_, i) => ({
    mes: i + 1,
    receitas: valores(8000, i < 11 ? 8000 : 0),
    receitasFonteAtiva: valores(8000, i < 11 ? 8000 : 0),
    receitasFontePassiva: valores(0, 0),
    despesas: valores(1000, i < 11 ? 940 : 0),
    guardou: i < 11 ? 7060 : 0,
    percentualGuardado: i < 11 ? 0.8825 : 0,
    evolucaoDespesas: i > 0 && i < 11 ? 0 : null,
    independenciometro: 0,
  })),
};

export interface ApiDoApp {
  lancamentosCriados: unknown[];
  faturasPagas: unknown[];
}

export async function simularApiDoApp(page: Page): Promise<ApiDoApp> {
  const registro: ApiDoApp = { lancamentosCriados: [], faturasPagas: [] };
  let compraPaga = false;
  await simularApiDeAutenticacao(page);
  await page.clock.setFixedTime(new Date('2026-11-15T10:00:00-03:00'));

  const json = (rota: Route, corpo: unknown, status = 200) => rota.fulfill({ status, json: corpo });

  await page.route('**/api/dashboard-financeiro/inicio**', (r) => json(r, inicio()));
  await page.route('**/api/contas/resumo', (r) =>
    json(r, {
      patrimonio: 1500,
      porTipo: [{ tipo: 'CONTA_CORRENTE', total: 1500, quantidadeDeContas: 1 }],
    }),
  );
  await page.route('**/api/contas?**', (r) => json(r, pagina(CONTAS)));
  await page.route('**/api/categorias?**', (r) => json(r, pagina(CATEGORIAS)));
  await page.route('**/api/transferencias?**', (r) => json(r, pagina([])));
  await page.route('**/api/orcamentos/2026/11', (r) => json(r, ORCAMENTO));
  await page.route('**/api/orcamentos/2026', (r) => json(r, RESUMO_DO_ANO));
  await page.route('**/api/lancamentos-financeiros/faturas/pagar', async (r) => {
    registro.faturasPagas.push(r.request().postDataJSON());
    compraPaga = true;
    await json(r, faturaAtual(true));
  });
  const faturaAtual = (paga: boolean) => ({
    contaId: 'c3',
    nomeConta: 'Cartão Roxo',
    ano: 2026,
    mes: 11,
    dataFechamento: '2026-11-05',
    dataVencimento: '2026-11-12',
    fechada: true,
    total: 90,
    lancamentos: [
      { ...COMPRA, status: paga ? 'PAGO' : 'ATRASADO', dataPagamento: paga ? '2026-11-15' : null },
    ],
  });
  await page.route('**/api/lancamentos-financeiros/faturas?**', (r) =>
    json(r, faturaAtual(compraPaga)),
  );
  await page.route('**/api/lancamentos-financeiros?**', (r) => json(r, pagina(LANCAMENTOS)));
  await page.route('**/api/lancamentos-financeiros', async (r) => {
    const corpo = r.request().postDataJSON();
    registro.lancamentosCriados.push(corpo);
    await json(r, lancamento({ ...corpo, id: 'novo' }), 201);
  });
  return registro;
}

export async function entrar(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill('ana@exemplo.com');
  await page.getByLabel('Senha', { exact: true }).fill(SENHA_CERTA);
  await page.keyboard.press('Enter');
  await page.waitForURL(/\/$/);
}
