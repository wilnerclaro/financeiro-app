import { ChangeDetectionStrategy, Component, computed, model } from '@angular/core';
import { ChevronLeft, ChevronRight } from 'lucide';
import { Button } from 'primeng/button';
import { MesDoAno, somarMeses } from '../../util/datas';
import { rotuloDoMes } from '../../util/formatacao';
import { Icone } from '../icone/icone';

@Component({
  selector: 'app-seletor-mes',
  imports: [Button, Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="seletor" role="group" aria-label="Mês de referência">
      <p-button
        [text]="true"
        severity="secondary"
        size="small"
        [ariaLabel]="'Ir para ' + rotuloAnterior()"
        (onClick)="mover(-1)"
      >
        <app-icone [icone]="icones.ChevronLeft" />
      </p-button>
      <span class="mes medio" aria-live="polite">{{ rotulo() }}</span>
      <p-button
        [text]="true"
        severity="secondary"
        size="small"
        [ariaLabel]="'Ir para ' + rotuloSeguinte()"
        (onClick)="mover(1)"
      >
        <app-icone [icone]="icones.ChevronRight" />
      </p-button>
    </div>
  `,
  styles: `
    .seletor {
      display: inline-flex;
      align-items: center;
      gap: var(--e1);
      border: 1px solid var(--p-content-border-color);
      border-radius: var(--r-ctl);
      background: var(--p-content-background);
    }

    .mes {
      min-width: 136px;
      font-size: 14px;
      text-align: center;
    }
  `,
})
export class SeletorMes {
  readonly mes = model.required<MesDoAno>();
  protected readonly icones = { ChevronLeft, ChevronRight };
  protected readonly rotulo = computed(() => rotuloDoMes(this.mes().ano, this.mes().mes));
  protected readonly rotuloAnterior = computed(() => this.rotuloDe(-1));
  protected readonly rotuloSeguinte = computed(() => this.rotuloDe(1));

  protected mover(quantidade: number): void {
    this.mes.set(somarMeses(this.mes(), quantidade));
  }

  private rotuloDe(quantidade: number): string {
    const alvo = somarMeses(this.mes(), quantidade);
    return rotuloDoMes(alvo.ano, alvo.mes).toLowerCase();
  }
}
