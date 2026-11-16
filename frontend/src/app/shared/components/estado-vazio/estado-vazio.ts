import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IconNode } from 'lucide';
import { Icone } from '../icone/icone';

@Component({
  selector: 'app-estado-vazio',
  imports: [Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="vazio" [attr.aria-label]="titulo()">
      <span class="vazio-icone"><app-icone [icone]="icone()" [tamanho]="24" /></span>
      <h2 class="titulo-secao">{{ titulo() }}</h2>
      @if (texto()) {
        <p class="aux">{{ texto() }}</p>
      }
      <div class="acoes"><ng-content /></div>
    </section>
  `,
  styles: `
    .vazio {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--e3);
      padding: var(--e12) var(--e4);
      border: 1px dashed var(--p-content-border-color);
      border-radius: var(--r-sup);
      text-align: center;
    }

    .aux {
      max-width: 420px;
    }

    .acoes {
      justify-content: center;
    }

    .acoes:empty {
      display: none;
    }

    .vazio-icone {
      display: grid;
      place-items: center;
      width: 48px;
      height: 48px;
      border-radius: 99px;
      background: var(--p-content-hover-background);
      color: var(--p-text-muted-color);
    }
  `,
})
export class EstadoVazio {
  readonly icone = input.required<IconNode>();
  readonly titulo = input.required<string>();
  readonly texto = input('');
}
