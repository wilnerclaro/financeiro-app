import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  ArrowDownRight,
  ArrowUpRight,
  ChevronRight,
  CircleAlert,
  Clock,
  IconNode,
  Plus,
  Receipt,
  TrendingUp,
  Upload,
} from 'lucide';
import { Button } from 'primeng/button';
import { ProgressBar } from 'primeng/progressbar';
import { SelectButton } from 'primeng/selectbutton';
import { Skeleton } from 'primeng/skeleton';
import {
  CategoriaComPrevisto,
  Inicio as ResumoInicio,
  Lancamento,
} from '../../../../shared/api/api.models';
import { ImportacaoDeExtrato } from '../../../importar/state/importacao-de-extrato';
import { InicioApi } from '../../../../shared/api/inicio-api';
import { ConfiguracaoInicialApi } from '../../../../shared/api/configuracao-inicial-api';
import { LancamentosApi } from '../../../../shared/api/lancamentos-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { EstadoVazio } from '../../../../shared/components/estado-vazio/estado-vazio';
import { Icone } from '../../../../shared/components/icone/icone';
import { SeletorMes } from '../../../../shared/components/seletor-mes/seletor-mes';
import { AGORA } from '../../../../shared/util/agora';
import { Avisos } from '../../../../shared/util/avisos';
import { MesDoAno, hojeIso, mesAtual, somarMeses } from '../../../../shared/util/datas';
import {
  MESES_CURTOS,
  MESES_LONGOS,
  brl,
  brlComSinal,
  dataCurta,
  percentual,
} from '../../../../shared/util/formatacao';
import { nivelDoOrcamento, statusDoOrcamento } from '../../../../shared/util/orcamento';
import { valorDe } from '../../../../shared/util/recurso';

interface Indicador {
  rotulo: string;
  valor: string;
  cor: string;
  delta: string;
  corDelta: string;
  seta?: IconNode;
  barra?: { valor: number; nivel: string };
}

interface ItemDeAtencao {
  id: string;
  icone: IconNode;
  cor: string;
  titulo: string;
  detalhe: string;
  lancamento?: Lancamento;
  rota?: string;
  acao?: string;
}

interface BarraDaSobra {
  rotulo: string;
  rotuloLongo: string;
  entrou: number;
  saiu: number;
  sobrou: number;
  percentualGuardado: number;
  atingiuMeta: boolean;
  altura: number;
  alturaMeta: number;
}

@Component({
  selector: 'app-inicio',
  imports: [
    FormsModule,
    RouterLink,
    Button,
    ProgressBar,
    SelectButton,
    Skeleton,
    EstadoErro,
    EstadoVazio,
    Icone,
    SeletorMes,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  private readonly inicioApi = inject(InicioApi);
  private readonly lancamentosApi = inject(LancamentosApi);
  private readonly avisos = inject(Avisos);
  private readonly agora = inject(AGORA);
  private readonly router = inject(Router);
  private readonly configuracaoApi = inject(ConfiguracaoInicialApi);
  protected readonly importacao = inject(ImportacaoDeExtrato);

  protected readonly configuracao = rxResource({ stream: () => this.configuracaoApi.buscar() });
  protected readonly primeirosPassos = computed(() => valorDe(this.configuracao));

  constructor() {
    effect(() => {
      const configuracao = this.primeirosPassos();
      if (configuracao && configuracao.etapa !== 'CONCLUIDO' && !configuracao.pausado) {
        void this.router.navigateByUrl('/configuracao-inicial', { replaceUrl: true });
      }
    });
  }

  protected readonly icones = { ChevronRight, Plus, Receipt, Upload };
  protected readonly brl = brl;
  protected readonly brlComSinal = brlComSinal;
  protected readonly dataCurta = dataCurta;
  protected readonly percentual = percentual;
  protected readonly nivel = nivelDoOrcamento;
  protected readonly statusDoOrcamento = statusDoOrcamento;
  protected readonly vistas = ['Gráfico', 'Tabela'];

  protected readonly mes = signal<MesDoAno>(mesAtual(this.agora()));
  protected readonly vista = signal('Gráfico');
  protected readonly pagando = signal<string | null>(null);

  protected readonly resumo = rxResource({
    params: () => ({ mes: this.mes(), importacoes: this.importacao.concluidas() }),
    stream: ({ params: { mes } }) => this.inicioApi.buscar(mes.ano, mes.mes),
  });

  protected readonly dados = computed(() => valorDe(this.resumo));
  protected readonly nomeDoMes = computed(() => MESES_LONGOS[this.mes().mes - 1]);
  private readonly nomeDoMesAnterior = computed(
    () => MESES_LONGOS[somarMeses(this.mes(), -1).mes - 1],
  );

  protected readonly vazio = computed(() => {
    const dados = this.dados();
    return (
      !!dados &&
      dados.entrou === 0 &&
      dados.saiu === 0 &&
      dados.ultimosLancamentos.length === 0 &&
      dados.paraPagar.quantidade === 0
    );
  });

  protected readonly indicadores = computed<Indicador[]>(() => {
    const dados = this.dados();
    return dados ? this.montarIndicadores(dados) : [];
  });

  protected readonly atencao = computed<ItemDeAtencao[]>(() => {
    const dados = this.dados();
    if (!dados) {
      return [];
    }
    const contas = dados.paraPagar.itens.slice(0, 3).map((l) => this.itemDeConta(l));
    const categorias = dados.categoriasAcimaDoPrevisto
      .slice(0, 2)
      .map((c) => this.itemDeCategoria(c));
    return [...contas, ...categorias];
  });

  protected readonly barras = computed<BarraDaSobra[]>(() => {
    const evolucao = this.dados()?.evolucao ?? [];
    const maior = Math.max(1, ...evolucao.map((m) => Math.max(Math.abs(m.sobrou), m.entrou * 0.3)));
    return evolucao.map((m) => ({
      rotulo: MESES_CURTOS[m.mes - 1],
      rotuloLongo: `${MESES_LONGOS[m.mes - 1]} de ${m.ano}`,
      entrou: m.entrou,
      saiu: m.saiu,
      sobrou: m.sobrou,
      percentualGuardado: m.percentualGuardado,
      atingiuMeta: m.atingiuMeta30,
      altura: Math.round((Math.abs(m.sobrou) / maior) * 100),
      alturaMeta: Math.round(((m.entrou * 0.3) / maior) * 100),
    }));
  });

  protected readonly resumoDoGrafico = computed(() => {
    const abaixo = this.barras().filter((b) => !b.atingiuMeta);
    const meses = this.barras()
      .map((b) => `${b.rotuloLongo}: ${brl(b.sobrou)} (${percentual(b.percentualGuardado)})`)
      .join('; ');
    const meta = abaixo.length
      ? `${abaixo.length} ${abaixo.length === 1 ? 'mês ficou' : 'meses ficaram'} abaixo da meta de 30%.`
      : 'Todos os meses atingiram a meta de 30%.';
    return `Sobra por mês. ${meses}. ${meta}`;
  });

  protected readonly independencia = computed(() => {
    const ind = this.dados()?.independenciometro;
    if (!ind) {
      return null;
    }
    const pontos = Math.abs(ind.variacao * 100).toLocaleString('pt-BR', {
      maximumFractionDigits: 1,
    });
    const comparacao =
      ind.variacao === 0
        ? `Igual a ${this.nomeDoMesAnterior()}`
        : `${pontos} ${pontos === '1' ? 'ponto' : 'pontos'} a ${ind.variacao > 0 ? 'mais' : 'menos'} que ${this.nomeDoMesAnterior()}`;
    return {
      ...ind,
      progresso: ind.proximoMarco ? Math.min(100, (ind.indice / ind.proximoMarco) * 100) : 100,
      comparacao,
      subiu: ind.variacao > 0,
    };
  });

  protected tentarDeNovo(): void {
    this.resumo.reload();
  }

  protected pagar(lancamento: Lancamento): void {
    this.pagando.set(lancamento.id);
    this.lancamentosApi.pagar(lancamento.id, hojeIso(this.agora())).subscribe({
      next: () => {
        this.pagando.set(null);
        this.avisos.sucesso('Pago', `${lancamento.descricao} foi marcado como pago hoje.`);
        this.resumo.reload();
      },
      error: (erro: unknown) => {
        this.pagando.set(null);
        this.avisos.erro('Não foi possível pagar', erro);
      },
    });
  }

  private montarIndicadores(dados: ResumoInicio): Indicador[] {
    const anterior = this.nomeDoMesAnterior();
    const comparativo = dados.comparativoMesAnterior;
    const orcamento = dados.orcamento;
    const temPrevisto = orcamento.despesasPrevistas > 0;
    const nivelOrcamento = nivelDoOrcamento(orcamento.percentualDespesasUsado);

    return [
      {
        rotulo: 'Sobra do mês',
        valor: brl(dados.sobrou),
        cor: dados.sobrou < 0 ? 'des' : '',
        delta: `${percentual(dados.percentualGuardado)} do que entrou · meta 30%`,
        corDelta: dados.meta30.atingiu ? 'ok' : 'lar',
        seta: dados.meta30.atingiu ? ArrowUpRight : undefined,
      },
      {
        rotulo: 'Receitas',
        valor: brl(dados.entrou),
        cor: 'rec',
        delta: this.comparar(comparativo.diferencaReceitas, comparativo.evolucaoReceitas, anterior),
        corDelta: 'aux',
        seta: this.seta(comparativo.diferencaReceitas),
      },
      {
        rotulo: 'Despesas',
        valor: brl(dados.saiu),
        cor: 'des',
        delta: this.comparar(comparativo.diferencaDespesas, comparativo.evolucaoDespesas, anterior),
        corDelta: comparativo.diferencaDespesas < 0 ? 'ok' : 'aux',
        seta: this.seta(comparativo.diferencaDespesas),
      },
      {
        rotulo: 'Orçamento usado',
        valor: temPrevisto ? percentual(orcamento.percentualDespesasUsado) : 'Sem previsto',
        cor: temPrevisto ? nivelOrcamento : 'aux',
        delta: !temPrevisto
          ? 'Defina o previsto para acompanhar'
          : orcamento.restaDasDespesas >= 0
            ? `Restam ${brl(orcamento.restaDasDespesas)} do previsto`
            : `${brl(-orcamento.restaDasDespesas)} acima do previsto`,
        corDelta: 'aux',
        barra: temPrevisto
          ? {
              valor: Math.min(100, orcamento.percentualDespesasUsado * 100),
              nivel: nivelOrcamento,
            }
          : undefined,
      },
    ];
  }

  private comparar(diferenca: number, evolucao: number, anterior: string): string {
    if (diferenca === 0) {
      return `Igual a ${anterior}`;
    }
    if (evolucao === 0) {
      return `${brl(Math.abs(diferenca))} a ${diferenca > 0 ? 'mais' : 'menos'} que ${anterior}`;
    }
    return `${percentual(Math.abs(evolucao))} a ${diferenca > 0 ? 'mais' : 'menos'} que ${anterior}`;
  }

  private seta(diferenca: number): IconNode | undefined {
    if (diferenca === 0) {
      return undefined;
    }
    return diferenca > 0 ? ArrowUpRight : ArrowDownRight;
  }

  private itemDeConta(lancamento: Lancamento): ItemDeAtencao {
    const atrasado = lancamento.status === 'ATRASADO';
    const vencimento = lancamento.dataVencimento ?? lancamento.dataCompetencia;
    return {
      id: lancamento.id,
      icone: atrasado ? CircleAlert : Clock,
      cor: atrasado ? 'des' : 'lar',
      titulo: `${lancamento.descricao} ${atrasado ? 'está atrasado' : 'vence em breve'}`,
      detalhe: `${brl(lancamento.valor)} · vencimento ${dataCurta(vencimento)} · ${lancamento.nomeConta}`,
      ...(lancamento.dataCompra ? { rota: '/contas', acao: 'Ver fatura' } : { lancamento }),
    };
  }

  private itemDeCategoria(categoria: CategoriaComPrevisto): ItemDeAtencao {
    const passou = categoria.percentualRealizado > 1;
    return {
      id: categoria.categoriaId,
      icone: TrendingUp,
      cor: passou ? 'des' : 'lar',
      titulo: `${categoria.nome} ${passou ? 'passou do previsto' : 'está no limite do previsto'}`,
      detalhe: `${brl(categoria.realizado)} de ${brl(categoria.previsto)} · ${percentual(categoria.percentualRealizado)} do previsto`,
      rota: '/orcamento',
      acao: 'Ver orçamento',
    };
  }
}
