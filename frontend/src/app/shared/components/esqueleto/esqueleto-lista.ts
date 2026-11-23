import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Skeleton } from 'primeng/skeleton';

const TITULOS = ['42%', '30%', '52%', '36%', '46%', '28%'];
const DETALHES = ['58%', '44%', '38%', '62%', '50%', '40%'];

@Component({
  selector: 'app-esqueleto-lista',
  imports: [Skeleton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true' },
  template: `
    <ul class="lista superficie">
      @if (cabecalho()) {
        <li class="cab">
          <div class="txt"><p-skeleton width="5rem" height=".7rem" /></div>
          @if (selo()) {
            <span class="selo"></span>
          }
          @for (c of colunasDesenhadas(); track c) {
            <p-skeleton class="coluna" [width]="larguraDaColuna()" height=".7rem" />
          }
        </li>
      }
      @for (i of linhasDesenhadas(); track i) {
        <li [class.recuada]="recuadas().includes(i)">
          @if (icone()) {
            <p-skeleton shape="circle" size="2rem" />
          }
          <div class="txt">
            <p-skeleton [width]="titulo(i)" height=".9rem" />
            @if (detalhe()) {
              <p-skeleton [width]="detalheDa(i)" height=".75rem" />
            }
          </div>
          @if (selo()) {
            <p-skeleton class="selo" width="3.5rem" height="1.25rem" />
          }
          @for (c of colunasDesenhadas(); track c) {
            <p-skeleton class="coluna" [width]="larguraDaColuna()" height=".9rem" />
          }
        </li>
      }
    </ul>
  `,
  styles: `
    :host {
      display: block;
      min-width: 0;
    }

    .lista {
      overflow: hidden;
    }

    .lista .txt {
      gap: var(--e2);
    }

    .coluna,
    .selo {
      flex-shrink: 1;
      min-width: 1.5rem;
    }

    span.selo {
      width: 3.5rem;
    }

    .lista > .cab {
      min-height: 40px;
    }

    .lista > .recuada {
      padding-left: var(--e8);
    }

    @media (max-width: 600px) {
      .lista > .cab,
      .coluna ~ .coluna {
        display: none;
      }

      .lista > .recuada {
        padding-left: var(--e6);
      }
    }
  `,
})
export class EsqueletoLista {
  readonly linhas = input(3);
  readonly icone = input(false);
  readonly detalhe = input(true);
  readonly selo = input(false);
  readonly colunas = input(1);
  readonly larguraDaColuna = input('5rem');
  readonly cabecalho = input(false);
  readonly recuadas = input<number[]>([]);

  protected readonly linhasDesenhadas = computed(() =>
    Array.from({ length: this.linhas() }, (_, i) => i),
  );
  protected readonly colunasDesenhadas = computed(() =>
    Array.from({ length: this.colunas() }, (_, i) => i),
  );

  protected titulo(i: number): string {
    return TITULOS[i % TITULOS.length];
  }

  protected detalheDa(i: number): string {
    return DETALHES[i % DETALHES.length];
  }
}
