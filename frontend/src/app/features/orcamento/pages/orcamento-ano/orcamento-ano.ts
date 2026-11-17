import { DOCUMENT, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ChartData, ChartOptions, TooltipItem } from 'chart.js';
import { ChevronDown, ChevronLeft, ChevronRight } from 'lucide';
import { Button } from 'primeng/button';
import { UIChart } from 'primeng/chart';
import { Skeleton } from 'primeng/skeleton';
import { Tema } from '../../../../core/config/tema';
import {
  LinhaDoAno,
  MesDaLinha,
  ResumoAnual,
  TotaisDoMes,
  Valores,
} from '../../../../shared/api/api.models';
import { OrcamentoApi } from '../../../../shared/api/orcamento-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { Icone } from '../../../../shared/components/icone/icone';
import { AGORA } from '../../../../shared/util/agora';
import { MESES_CURTOS, MESES_LONGOS, brl, percentual } from '../../../../shared/util/formatacao';
import { valorDe } from '../../../../shared/util/recurso';
import { AbasOrcamento } from '../../components/abas-orcamento/abas-orcamento';

const META_DE_GUARDAR = 0.3;
const INTEIRO = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });
const COMPACTO = new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 });

export interface CelulaDoAno {
  texto: string;
  classe: string;
  dica: string;
}

export interface LinhaDaTabela {
  id: string;
  nome: string;
  classe: string;
  celulas: CelulaDoAno[];
  total: string;
  media: string;
  previsto: string;
  subitens: LinhaDaTabela[];
}

interface Grafico {
  titulo: string;
  resumo: string;
  dados: ChartData<'line', (number | null)[], string>;
}

@Component({
  selector: 'app-orcamento-ano',
  imports: [NgTemplateOutlet, Button, UIChart, Skeleton, EstadoErro, Icone, AbasOrcamento],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './orcamento-ano.html',
  styleUrl: './orcamento-ano.scss',
})
export class OrcamentoAno {
  private readonly orcamentoApi = inject(OrcamentoApi);
  private readonly agora = inject(AGORA);
  private readonly tema = inject(Tema);
  private readonly documento = inject(DOCUMENT);

  protected readonly icones = { ChevronDown, ChevronLeft, ChevronRight };
  protected readonly brl = brl;
  protected readonly percentual = percentual;
  protected readonly meses = MESES_CURTOS;
  protected readonly ano = signal(this.agora().getFullYear());
  protected readonly abertos = signal<ReadonlySet<string>>(new Set());

  protected readonly resumo = rxResource({
    params: () => this.ano(),
    stream: ({ params }) => this.orcamentoApi.doAno(params),
  });

  protected readonly mesAtual = computed(() => {
    const hoje = this.agora();
    if (this.ano() < hoje.getFullYear()) {
      return 12;
    }
    return this.ano() === hoje.getFullYear() ? hoje.getMonth() + 1 : 0;
  });

  protected readonly subtitulo = computed(() => {
    const mes = this.mesAtual();
    if (mes === 0) {
      return 'O que você já planejou para o ano.';
    }
    return mes === 12
      ? 'Previsto x realizado mês a mês, com total e média do ano.'
      : `Até ${MESES_LONGOS[mes - 1]}: previsto x realizado mês a mês, com total e média.`;
  });

  protected readonly vazio = computed(() => {
    const r = valorDe(this.resumo);
    return !!r && !r.grupos.length;
  });

  protected readonly indicadores = computed(() => {
    const r = valorDe(this.resumo);
    if (!r) {
      return [];
    }
    const i = r.indicadores;
    const comDespesa = r.meses.filter((m) => m.despesas.realizado > 0);
    const primeiro = comDespesa.at(0);
    const ultimo = comDespesa.at(-1);
    return [
      {
        rotulo: 'Entrou no ano',
        valor: brl(i.entrou),
        cor: 'rec',
        detalhe: `média ${brl(i.mediaEntrou)} por mês`,
        corDetalhe: '',
      },
      {
        rotulo: 'Saiu no ano',
        valor: brl(i.saiu),
        cor: 'des',
        detalhe: `média ${brl(i.mediaSaiu)} por mês`,
        corDetalhe: '',
      },
      {
        rotulo: 'Guardou no ano',
        valor: brl(i.guardou),
        cor: i.guardou < 0 ? 'des' : 'ok',
        detalhe: i.entrou
          ? `${percentual(i.percentualGuardado)} do que entrou · meta 30%`
          : 'Sem receita no ano',
        corDetalhe: !i.entrou ? '' : i.percentualGuardado >= META_DE_GUARDAR ? 'ok' : 'amb',
      },
      {
        rotulo: 'Independenciômetro',
        valor: ultimo ? percentual(ultimo.independenciometro) : '—',
        cor: ultimo ? 'ok' : '',
        detalhe: this.evolucaoDoIndependenciometro(primeiro, ultimo),
        corDetalhe: '',
      },
    ];
  });

  protected readonly graficos = computed<Grafico[]>(() => {
    const r = valorDe(this.resumo);
    if (!r) {
      return [];
    }
    const cores = this.cores();
    return [
      this.grafico('Receitas', r.meses, (m) => m.receitas, cores.rec),
      this.grafico('Despesas', r.meses, (m) => m.despesas, cores.des),
    ];
  });

  protected readonly opcoesDoGrafico = computed<ChartOptions<'line'>>(() => {
    const cores = this.cores();
    return {
      maintainAspectRatio: false,
      animation: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: {
          position: 'bottom',
          align: 'start',
          labels: { color: cores.texto, usePointStyle: true, boxWidth: 8, boxHeight: 8 },
        },
        tooltip: {
          callbacks: {
            label: (item: TooltipItem<'line'>) =>
              `${item.dataset.label}: ${brl(item.parsed.y ?? 0)}`,
          },
        },
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: cores.texto, maxRotation: 0, autoSkipPadding: 8 },
          border: { display: false },
        },
        y: {
          beginAtZero: true,
          grid: { color: cores.grade },
          border: { display: false },
          ticks: { color: cores.texto, maxTicksLimit: 5, callback: (v) => COMPACTO.format(+v) },
        },
      },
    };
  });

  protected readonly receitas = computed<LinhaDaTabela[]>(() => {
    const r = valorDe(this.resumo);
    if (!r) {
      return [];
    }
    const grupos = r.grupos.filter((g) => g.tipo === 'RECEITA').map((g) => this.linhaDeReceita(g));
    return [
      ...grupos,
      {
        id: 'total-receitas',
        nome: 'Total de receitas',
        classe: 'total rec',
        celulas: r.meses.map((m) => this.celulaDeReceita(m.receitas, m.mes)),
        total: curto(r.indicadores.entrou),
        media: curto(r.indicadores.mediaEntrou),
        previsto: curto(r.indicadores.previstoReceitas),
        subitens: [],
      },
    ];
  });

  protected readonly despesas = computed<LinhaDaTabela[]>(() => {
    const r = valorDe(this.resumo);
    if (!r) {
      return [];
    }
    const grupos = r.grupos
      .filter((g) => g.tipo === 'DESPESA')
      .map((g) => this.linhaDeDespesa(g, this.subcategoriasDe(r, g.categoriaId)));
    return [
      ...grupos,
      {
        id: 'total-despesas',
        nome: 'Total de despesas',
        classe: 'total des',
        celulas: r.meses.map((m) => this.celulaDeDespesa(m.despesas, m.mes)),
        total: curto(r.indicadores.saiu),
        media: curto(r.indicadores.mediaSaiu),
        previsto: curto(r.indicadores.previstoDespesas),
        subitens: [],
      },
    ];
  });

  protected readonly resultado = computed<LinhaDaTabela[]>(() => {
    const r = valorDe(this.resumo);
    if (!r) {
      return [];
    }
    return [
      this.linhaDeResultado('Evolução dos gastos', r.meses, (m) => this.celulaDeEvolucao(m)),
      this.linhaDeResultado('Guardou', r.meses, (m) => this.celulaDeGuardou(m)),
      this.linhaDeResultado('Independenciômetro', r.meses, (m) =>
        this.mesFuturo(m.mes) || !m.despesas.realizado
          ? vazia()
          : { texto: percentual(m.independenciometro), classe: 'ok', dica: '' },
      ),
    ];
  });

  protected mudarAno(passo: number): void {
    this.ano.set(this.ano() + passo);
    this.abertos.set(new Set());
  }

  protected alternar(id: string): void {
    const abertos = new Set(this.abertos());
    if (!abertos.delete(id)) {
      abertos.add(id);
    }
    this.abertos.set(abertos);
  }

  private cores() {
    this.tema.escuro();
    return {
      rec: this.corDoToken('--rec', '#7cb4ff'),
      des: this.corDoToken('--des', '#ff6b6b'),
      texto: this.corDoToken('--p-text-muted-color', '#93a39c'),
      grade: this.corDoToken('--p-content-border-color', '#222b28'),
    };
  }

  private corDoToken(token: string, padrao: string): string {
    const amostra = this.documento.createElement('span');
    amostra.style.color = `var(${token}, ${padrao})`;
    this.documento.body.appendChild(amostra);
    const cor = this.documento.defaultView?.getComputedStyle(amostra).color;
    amostra.remove();
    return cor || padrao;
  }

  private grafico(
    titulo: string,
    meses: TotaisDoMes[],
    valores: (m: TotaisDoMes) => Valores,
    cor: string,
  ): Grafico {
    const realizado = meses.map((m) => (this.mesFuturo(m.mes) ? null : valores(m).realizado));
    const previsto = meses.map((m) => valores(m).previsto);
    const ultimo = [...meses].reverse().find((m) => !this.mesFuturo(m.mes));
    return {
      titulo,
      resumo: ultimo
        ? `${titulo} de ${this.ano()}: em ${MESES_LONGOS[ultimo.mes - 1]}, ${brl(valores(ultimo).realizado)} realizados de ${brl(valores(ultimo).previsto)} previstos. Os valores de cada mês estão na tabela abaixo.`
        : `${titulo} previstas para ${this.ano()}. Os valores estão na tabela abaixo.`,
      dados: {
        labels: MESES_CURTOS,
        datasets: [
          {
            label: 'Realizado',
            data: realizado,
            borderColor: cor,
            backgroundColor: comTransparencia(cor, 0.14),
            pointBackgroundColor: cor,
            fill: true,
            tension: 0.3,
            borderWidth: 2,
            pointRadius: 3,
            pointHoverRadius: 5,
          },
          {
            label: 'Previsto',
            data: previsto,
            borderColor: this.cores().texto,
            pointBackgroundColor: this.cores().texto,
            borderDash: [6, 5],
            fill: false,
            tension: 0.3,
            borderWidth: 2,
            pointRadius: 0,
            pointHoverRadius: 4,
          },
        ],
      },
    };
  }

  private linhaDeReceita(linha: LinhaDoAno): LinhaDaTabela {
    return {
      ...this.colunasDoAno(linha),
      classe: 'grupo',
      celulas: linha.meses.map((m) => this.celulaDeReceita(m, m.mes)),
      subitens: [],
    };
  }

  private linhaDeDespesa(linha: LinhaDoAno, subcategorias: LinhaDoAno[]): LinhaDaTabela {
    return {
      ...this.colunasDoAno(linha),
      classe: 'grupo',
      celulas: linha.meses.map((m) => this.celulaDeDespesa(m, m.mes)),
      subitens: subcategorias.map((s) => ({
        ...this.colunasDoAno(s),
        classe: 'sub',
        celulas: s.meses.map((m) => this.celulaDeDespesa(m, m.mes)),
        subitens: [],
      })),
    };
  }

  private colunasDoAno(linha: LinhaDoAno) {
    return {
      id: linha.categoriaId,
      nome: linha.nome,
      total: curto(linha.totalRealizado),
      media: curto(linha.mediaRealizado),
      previsto: curto(linha.totalPrevisto),
    };
  }

  private linhaDeResultado(
    nome: string,
    meses: TotaisDoMes[],
    celula: (m: TotaisDoMes) => CelulaDoAno,
  ): LinhaDaTabela {
    return {
      id: nome,
      nome,
      classe: 'resultado',
      celulas: meses.map(celula),
      total: '',
      media: '',
      previsto: '',
      subitens: [],
    };
  }

  private subcategoriasDe(resumo: ResumoAnual, grupoId: string): LinhaDoAno[] {
    return resumo.linhas.filter(
      (l) => l.categoriaPaiId === grupoId && (l.totalPrevisto !== 0 || l.totalRealizado !== 0),
    );
  }

  private celulaDeReceita(valores: Pick<MesDaLinha, 'previsto' | 'realizado'>, mes: number) {
    if (this.mesFuturo(mes)) {
      return this.celulaDePrevisto(valores.previsto);
    }
    return valores.realizado
      ? {
          texto: curto(valores.realizado),
          classe: '',
          dica: `${brl(valores.realizado)} de ${brl(valores.previsto)} previstos`,
        }
      : vazia();
  }

  private celulaDeDespesa(
    valores: Pick<MesDaLinha, 'previsto' | 'realizado' | 'percentualRealizado'>,
    mes: number,
  ): CelulaDoAno {
    if (this.mesFuturo(mes)) {
      return this.celulaDePrevisto(valores.previsto);
    }
    if (!valores.realizado) {
      return vazia();
    }
    if (!valores.previsto) {
      return {
        texto: curto(valores.realizado),
        classe: 'estourou',
        dica: `${brl(valores.realizado)} sem previsto`,
      };
    }
    const uso = valores.percentualRealizado;
    return {
      texto: curto(valores.realizado),
      classe: uso > 1 ? 'estourou' : uso >= 0.9 ? 'limite' : '',
      dica: `${brl(valores.realizado)}: ${percentual(uso)} do previsto de ${brl(valores.previsto)}`,
    };
  }

  private celulaDePrevisto(previsto: number): CelulaDoAno {
    return previsto
      ? { texto: curto(previsto), classe: 'futuro', dica: `${brl(previsto)} previstos` }
      : vazia();
  }

  private celulaDeEvolucao(m: TotaisDoMes): CelulaDoAno {
    const evolucao = m.evolucaoDespesas;
    if (evolucao === null || this.mesFuturo(m.mes)) {
      return vazia();
    }
    const anterior = MESES_LONGOS[m.mes - 2];
    if (evolucao === 0) {
      return { texto: '0%', classe: '', dica: `Igual a ${anterior}` };
    }
    return {
      texto: `${evolucao > 0 ? '▲' : '▼'} ${percentual(Math.abs(evolucao))}`,
      classe: evolucao > 0 ? 'des' : 'ok',
      dica: `${percentual(Math.abs(evolucao))} ${evolucao > 0 ? 'a mais' : 'a menos'} que ${anterior}`,
    };
  }

  private celulaDeGuardou(m: TotaisDoMes): CelulaDoAno {
    if (this.mesFuturo(m.mes) || (!m.receitas.realizado && !m.despesas.realizado)) {
      return vazia();
    }
    return {
      texto: curto(m.guardou),
      classe: m.guardou < 0 ? 'des' : m.percentualGuardado >= META_DE_GUARDAR ? 'ok' : 'amb',
      dica: m.receitas.realizado
        ? `${brl(m.guardou)}: ${percentual(m.percentualGuardado)} do que entrou`
        : brl(m.guardou),
    };
  }

  private evolucaoDoIndependenciometro(
    primeiro: TotaisDoMes | undefined,
    ultimo: TotaisDoMes | undefined,
  ): string {
    if (!primeiro || !ultimo) {
      return 'Sem despesas no ano';
    }
    const fim = `${percentual(ultimo.independenciometro)} em ${MESES_CURTOS[ultimo.mes - 1]}`;
    return primeiro === ultimo
      ? fim
      : `de ${percentual(primeiro.independenciometro)} em ${MESES_CURTOS[primeiro.mes - 1]} para ${fim}`;
  }

  private mesFuturo(mes: number): boolean {
    return mes > this.mesAtual();
  }
}

function comTransparencia(cor: string, alfa: number): string {
  const canais = cor.match(/\d+(\.\d+)?/g);
  return cor.startsWith('rgb') && canais && canais.length >= 3
    ? `rgba(${canais.slice(0, 3).join(', ')}, ${alfa})`
    : cor;
}

function curto(valor: number): string {
  return INTEIRO.format(valor);
}

function vazia(): CelulaDoAno {
  return { texto: '–', classe: 'vazio', dica: '' };
}
