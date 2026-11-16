import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RefreshCw } from 'lucide';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Icone } from '../icone/icone';

@Component({
  selector: 'app-estado-erro',
  imports: [Button, Message, Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p-message severity="error" role="alert">
      <div class="erro">
        <span
          ><strong>{{ titulo() }}</strong> {{ mensagem() }}</span
        >
        <p-button size="small" severity="danger" [outlined]="true" (onClick)="tentar.emit()">
          <app-icone [icone]="iconeTentar" />
          <span>Tentar de novo</span>
        </p-button>
      </div>
    </p-message>
  `,
  styles: `
    .erro {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: var(--e4);
      width: 100%;
    }
  `,
})
export class EstadoErro {
  readonly titulo = input.required<string>();
  readonly mensagem = input('');
  readonly tentar = output();
  protected readonly iconeTentar = RefreshCw;
}
