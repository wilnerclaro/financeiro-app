import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ChevronDown, ChevronRight } from 'lucide';
import { ProgressBar } from 'primeng/progressbar';
import { Valores } from '../../../../shared/api/api.models';
import { Icone } from '../../../../shared/components/icone/icone';
import { brl, percentual } from '../../../../shared/util/formatacao';
import { NivelDoOrcamento } from '../../../../shared/util/orcamento';

export interface LinhaDoOrcamento {
  id: string;
  nome: string;
  valores: Valores;
  nivel: NivelDoOrcamento | null;
  status: string;
  largura: number;
  subitens: LinhaDoOrcamento[];
}

@Component({
  selector: 'app-linha-orcamento',
  imports: [ProgressBar, Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': 'tipo()' },
  template: `
    @let l = linha();
    <div class="c-nome">
      @if (expansivel()) {
        <button
          type="button"
          class="alternar"
          [attr.aria-expanded]="aberto()"
          (click)="alternar.emit()"
        >
          <app-icone [icone]="aberto() ? icones.ChevronDown : icones.ChevronRight" [tamanho]="14" />
          <span>{{ l.nome }}</span>
        </button>
      } @else {
        <span class="nome">{{ l.nome }}</span>
      }
      @if (tipo() !== 'subitem') {
        <span class="selo estreito" [class]="l.nivel ?? ''">{{ l.status }}</span>
      }
    </div>
    <p class="c-prev num"><span class="rot">Previsto</span> {{ brl(l.valores.previsto) }}</p>
    <p class="c-real num"><span class="rot">Realizado</span> {{ brl(l.valores.realizado) }}</p>
    <p class="c-desv num">
      <span class="rot">Desvio</span>
      @if (l.valores.desvio > 0) {
        <span class="des"
          ><span aria-hidden="true">▲ </span>{{ brl(l.valores.desvio)
          }}<span class="sr-only"> acima do previsto</span></span
        >
      } @else if (l.valores.desvio < 0) {
        <span class="ok"
          ><span aria-hidden="true">▼ </span>{{ brl(-l.valores.desvio)
          }}<span class="sr-only"> abaixo do previsto</span></span
        >
      } @else {
        <span class="aux">—</span>
      }
    </p>
    <div class="c-uso" [class.sem-previsto]="!l.nivel">
      @if (l.nivel) {
        <p-progressbar
          [value]="l.largura"
          [showValue]="false"
          [class]="'barra ' + l.nivel"
          [attr.aria-label]="
            l.nome + ': ' + percentual(l.valores.percentualRealizado) + ' do previsto'
          "
        />
        <span class="pct num forte" [class]="l.nivel"
          >{{ percentual(l.valores.percentualRealizado) }}
          @if (tipo() === 'subitem') {
            <span class="sr-only">{{ l.status }}</span>
          }
        </span>
      }
      @if (tipo() !== 'subitem' || !l.nivel) {
        <span class="selo largo" [class]="l.nivel ?? ''">{{ l.status }}</span>
      }
    </div>
    @if (tipo() === 'subitem') {
      <p class="c-resumo num" aria-hidden="true">
        {{ brl(l.valores.realizado) }}
        <span class="aux">{{ l.nivel ? 'de ' + brl(l.valores.previsto) : 'sem previsto' }}</span>
      </p>
    }
  `,
  styleUrl: './linha-orcamento.scss',
})
export class LinhaOrcamento {
  readonly linha = input.required<LinhaDoOrcamento>();
  readonly tipo = input<'grupo' | 'subitem' | 'total'>('grupo');
  readonly aberto = input(false);
  readonly alternar = output();

  protected readonly icones = { ChevronDown, ChevronRight };
  protected readonly brl = brl;
  protected readonly percentual = percentual;
  protected readonly expansivel = computed(
    () => this.tipo() === 'grupo' && this.linha().subitens.length > 0,
  );
}
