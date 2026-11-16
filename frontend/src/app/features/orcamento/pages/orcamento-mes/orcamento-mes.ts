import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ChartPie, ChevronDown, ChevronRight, Copy } from 'lucide';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ProgressBar } from 'primeng/progressbar';
import { Skeleton } from 'primeng/skeleton';
import { GrupoDoOrcamento, Valores } from '../../../../shared/api/api.models';
import { OrcamentoApi } from '../../../../shared/api/orcamento-api';
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
import { valorDe } from '../../../../shared/util/recurso';

@Component({
  selector: 'app-orcamento-mes',
  imports: [
    RouterLink,
    Button,
    ConfirmDialog,
    ProgressBar,
    Skeleton,
    EstadoErro,
    EstadoVazio,
    Icone,
    SeletorMes,
    AbasOrcamento,
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

  protected readonly icones = { ChartPie, ChevronDown, ChevronRight, Copy };
  protected readonly brl = brl;
  protected readonly percentual = percentual;
  protected readonly nivel = nivelDoOrcamento;
  protected readonly statusDoOrcamento = statusDoOrcamento;

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

  protected readonly despesas = computed(() =>
    this.comMovimento(valorDe(this.relatorio)?.despesas.grupos),
  );
  protected readonly receitas = computed(() =>
    this.comMovimento(valorDe(this.relatorio)?.receitas.grupos),
  );

  protected alternar(id: string): void {
    const abertos = new Set(this.abertos());
    if (!abertos.delete(id)) {
      abertos.add(id);
    }
    this.abertos.set(abertos);
  }

  protected largura(valores: Valores): number {
    return Math.min(100, valores.percentualRealizado * 100);
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

  private comMovimento(grupos: GrupoDoOrcamento[] | undefined): GrupoDoOrcamento[] {
    return (grupos ?? []).filter((g) => g.valores.previsto !== 0 || g.valores.realizado !== 0);
  }
}
