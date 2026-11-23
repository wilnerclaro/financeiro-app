import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ChartPie, CircleAlert, CircleCheck, Copy, IconNode, TriangleAlert } from 'lucide';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ProgressBar } from 'primeng/progressbar';
import { Skeleton } from 'primeng/skeleton';
import { FonteReceita, ItemDoOrcamento, Valores } from '../../../../shared/api/api.models';
import { OrcamentoApi } from '../../../../shared/api/orcamento-api';
import { EsqueletoGrafico } from '../../../../shared/components/esqueleto/esqueleto-grafico';
import { EsqueletoLista } from '../../../../shared/components/esqueleto/esqueleto-lista';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { EstadoVazio } from '../../../../shared/components/estado-vazio/estado-vazio';
import { Icone } from '../../../../shared/components/icone/icone';
import { SeletorMes } from '../../../../shared/components/seletor-mes/seletor-mes';
import { AGORA } from '../../../../shared/util/agora';
import { Avisos } from '../../../../shared/util/avisos';
import { MesDoAno, competenciaIso, mesAtual, somarMeses } from '../../../../shared/util/datas';
import { MESES_LONGOS, brl, percentual } from '../../../../shared/util/formatacao';
import { nivelDoOrcamento, statusDoOrcamento } from '../../../../shared/util/orcamento';
import { AbasOrcamento } from '../../components/abas-orcamento/abas-orcamento';
import { LinhaDoOrcamento, LinhaOrcamento } from '../../components/linha-orcamento/linha-orcamento';
import { valorDe } from '../../../../shared/util/recurso';

interface CartaoDeReceita {
  id: string;
  nome: string;
  fonte: string;
  valores: Valores;
  largura: number;
  subitens: ItemDoOrcamento[];
}

interface Alerta {
  id: string;
  cor: 'des' | 'lar' | '';
  icone: IconNode;
  titulo: string;
  detalhe: string;
  acao: string;
  rota: string;
}

const RAIO_DO_ANEL = 52;
const VOLTA_DO_ANEL = 2 * Math.PI * RAIO_DO_ANEL;
const FONTES: Record<FonteReceita, string> = { ATIVA: 'Ativa', PASSIVA: 'Passiva' };
const PESO_DO_ALERTA = { des: 0, lar: 1, '': 2 };

@Component({
  selector: 'app-orcamento-mes',
  imports: [
    RouterLink,
    Button,
    ConfirmDialog,
    ProgressBar,
    Skeleton,
    EsqueletoGrafico,
    EsqueletoLista,
    EstadoErro,
    EstadoVazio,
    Icone,
    SeletorMes,
    AbasOrcamento,
    LinhaOrcamento,
  ],
  providers: [ConfirmationService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './orcamento-mes.html',
  styleUrl: './orcamento-mes.scss',
})
export class OrcamentoMes {
  private readonly orcamentoApi = inject(OrcamentoApi);
  private readonly confirmacao = inject(ConfirmationService);
  private readonly avisos = inject(Avisos);
  private readonly agora = inject(AGORA);

  protected readonly icones = { ChartPie, CircleCheck, Copy };
  protected readonly brl = brl;
  protected readonly percentual = percentual;
  protected readonly voltaDoAnel = VOLTA_DO_ANEL;
  protected readonly raioDoAnel = RAIO_DO_ANEL;

  protected readonly mes = signal<MesDoAno>(mesAtual(this.agora()));
  protected readonly abertos = signal<ReadonlySet<string>>(new Set());
  protected readonly copiando = signal(false);
  protected readonly nomeDoMes = computed(() => MESES_LONGOS[this.mes().mes - 1]);
  protected readonly nomeDoMesAnterior = computed(
    () => MESES_LONGOS[somarMeses(this.mes(), -1).mes - 1],
  );

  protected readonly relatorio = rxResource({
    params: () => this.mes(),
    stream: ({ params }) => this.orcamentoApi.doMes(params.ano, params.mes),
  });

  protected readonly vazio = computed(() => {
    const r = valorDe(this.relatorio);
    return (
      !!r &&
      [r.despesas.total, r.receitas.total].every((v) => v.previsto === 0 && v.realizado === 0)
    );
  });

  protected readonly totalDeDespesas = computed(() => {
    const total = valorDe(this.relatorio)?.despesas.total;
    return total ? this.linha({ categoriaId: 'total', nome: 'Total de despesas' }, total) : null;
  });

  protected readonly anel = computed(() => {
    const total = this.totalDeDespesas();
    return VOLTA_DO_ANEL * (1 - (total?.largura ?? 0) / 100);
  });

  protected readonly despesas = computed(() =>
    comMovimento(valorDe(this.relatorio)?.despesas.grupos).map((g) =>
      this.linha(
        g,
        g.valores,
        comMovimento(g.subitens).map((s) => this.linha(s, s.valores)),
      ),
    ),
  );

  protected readonly receitas = computed<CartaoDeReceita[]>(() =>
    comMovimento(valorDe(this.relatorio)?.receitas.grupos).map((g) => ({
      id: g.categoriaId,
      nome: g.nome,
      fonte: g.fonteReceita ? FONTES[g.fonteReceita] : '',
      valores: g.valores,
      largura: largura(g.valores),
      subitens: comMovimento(g.subitens),
    })),
  );

  protected readonly expansiveis = computed(() =>
    this.despesas()
      .filter((g) => g.subitens.length)
      .map((g) => g.id),
  );

  protected readonly tudoAberto = computed(
    () => this.expansiveis().length > 0 && this.expansiveis().every((id) => this.abertos().has(id)),
  );

  protected readonly atencao = computed<Alerta[]>(() =>
    this.despesas()
      .flatMap((g) => this.alertasDo(g))
      .sort((a, b) => PESO_DO_ALERTA[a.cor] - PESO_DO_ALERTA[b.cor]),
  );

  protected alternar(id: string): void {
    const abertos = new Set(this.abertos());
    if (!abertos.delete(id)) {
      abertos.add(id);
    }
    this.abertos.set(abertos);
  }

  protected alternarTodos(): void {
    this.abertos.set(new Set(this.tudoAberto() ? [] : this.expansiveis()));
  }

  protected tentarDeNovo(): void {
    this.relatorio.reload();
  }

  protected confirmarCopia(): void {
    this.confirmacao.confirm({
      header: `Copiar o previsto de ${this.nomeDoMesAnterior()}?`,
      message: `Os valores previstos de ${this.nomeDoMesAnterior()} entram em ${this.nomeDoMes()}. Categorias que já têm previsto neste mês ficam como estão.`,
      acceptLabel: 'Copiar',
      rejectLabel: 'Voltar',
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () => this.copiarMesAnterior(),
    });
  }

  private copiarMesAnterior(): void {
    const origem = somarMeses(this.mes(), -1);
    this.copiando.set(true);
    this.orcamentoApi
      .copiar(origem.ano, origem.mes, [competenciaIso(this.mes())], false)
      .subscribe({
        next: (copia) => {
          this.copiando.set(false);
          const gravadas = copia.meses.reduce((total, m) => total + m.gravadas, 0);
          this.avisos.sucesso(
            'Previsto copiado',
            gravadas
              ? `${gravadas} ${gravadas === 1 ? 'categoria recebeu' : 'categorias receberam'} o previsto de ${this.nomeDoMesAnterior()}.`
              : 'Nenhuma categoria nova para copiar.',
          );
          this.relatorio.reload();
        },
        error: (erro: unknown) => {
          this.copiando.set(false);
          this.avisos.erro('Não foi possível copiar', erro);
        },
      });
  }

  private linha(
    item: Pick<ItemDoOrcamento, 'categoriaId' | 'nome'>,
    valores: Valores,
    subitens: LinhaDoOrcamento[] = [],
  ): LinhaDoOrcamento {
    const previsto = valores.previsto > 0;
    return {
      id: item.categoriaId,
      nome: item.nome,
      valores,
      nivel: previsto ? nivelDoOrcamento(valores.percentualRealizado) : null,
      status: previsto ? statusDoOrcamento(valores.percentualRealizado) : 'Sem previsto',
      largura: largura(valores),
      subitens,
    };
  }

  private alertasDo(grupo: LinhaDoOrcamento): Alerta[] {
    const { previsto, realizado, desvio, percentualRealizado } = grupo.valores;
    const usado = `${percentual(percentualRealizado)} de ${brl(previsto)}`;
    const passaram = grupo.subitens
      .filter((s) => s.nivel === 'des')
      .sort((a, b) => b.valores.desvio - a.valores.desvio);

    if (grupo.nivel === null) {
      return [
        {
          id: grupo.id,
          cor: '',
          icone: CircleAlert,
          titulo: `${grupo.nome} não tem previsto`,
          detalhe: `${brl(realizado)} gastos sem um valor planejado para o mês.`,
          acao: 'Definir previsto',
          rota: '/orcamento/previsto',
        },
      ];
    }
    if (grupo.nivel === 'des') {
      const nomes = passaram.slice(0, 2).map((s) => s.nome);
      const motivo = nomes.length
        ? ` ${nomes.join(' e ')} ${nomes.length === 1 ? 'puxou' : 'puxaram'} a alta.`
        : '';
      return [
        {
          id: grupo.id,
          cor: 'des',
          icone: TriangleAlert,
          titulo: `${grupo.nome} passou do previsto`,
          detalhe: `${brl(desvio)} acima (${usado}).${motivo}`,
          acao: 'Ver lançamentos',
          rota: '/lancamentos',
        },
      ];
    }
    const alertas: Alerta[] = passaram.map((s) => ({
      id: s.id,
      cor: 'des',
      icone: TriangleAlert,
      titulo: `${s.nome} passou do previsto`,
      detalhe: `${brl(s.valores.desvio)} acima (${percentual(s.valores.percentualRealizado)} de ${brl(s.valores.previsto)}), em ${grupo.nome}.`,
      acao: 'Ver lançamentos',
      rota: '/lancamentos',
    }));
    if (grupo.nivel === 'lar') {
      alertas.push({
        id: grupo.id,
        cor: 'lar',
        icone: TriangleAlert,
        titulo: `${grupo.nome} está no limite`,
        detalhe: desvio
          ? `Restam ${brl(-desvio)} (${usado} usados).`
          : `Usou todo o previsto de ${brl(previsto)}.`,
        acao: 'Ver lançamentos',
        rota: '/lancamentos',
      });
    }
    return alertas;
  }
}

function largura(valores: Valores): number {
  return Math.min(100, Math.max(0, valores.percentualRealizado * 100));
}

function comMovimento<T extends ItemDoOrcamento>(itens: T[] | undefined): T[] {
  return (itens ?? []).filter((i) => i.valores.previsto !== 0 || i.valores.realizado !== 0);
}
