import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type FormaDoGrafico = 'barras' | 'linhas' | 'anel';

const LARGURA = 600;
const ALTURA_DO_ROTULO = 8;
const RESPIRO_DO_ROTULO = 16;
const BARRAS = [0.55, 0.8, 0.42, 0.95, 0.66, 0.74, 0.5, 0.86, 0.6, 0.9, 0.46, 0.7];
const REALIZADO = [0.5, 0.58, 0.54, 0.66, 0.62, 0.74, 0.68, 0.78, 0.72, 0.84, 0.8, 0.88];
const PREVISTO = [0.36, 0.32, 0.4, 0.38, 0.46, 0.42, 0.48, 0.46, 0.52, 0.5, 0.54, 0.56];

function rotulos(colunas: number, altura: number): string {
  const faixa = LARGURA / colunas;
  return Array.from({ length: colunas }, (_, i) => {
    const largura = faixa * 0.34;
    const x = faixa * i + (faixa - largura) / 2;
    return `<rect x="${x}" y="${altura - ALTURA_DO_ROTULO}" width="${largura}" height="${ALTURA_DO_ROTULO}" rx="4"/>`;
  }).join('');
}

function barras(colunas: number, altura: number): string {
  const faixa = LARGURA / colunas;
  const teto = altura - RESPIRO_DO_ROTULO - 8;
  const chao = altura - RESPIRO_DO_ROTULO;
  const colunasDesenhadas = Array.from({ length: colunas }, (_, i) => {
    const largura = faixa * 0.36;
    const alturaDaBarra = teto * BARRAS[i % BARRAS.length];
    const x = faixa * i + (faixa - largura) / 2;
    return `<rect x="${x}" y="${chao - alturaDaBarra}" width="${largura}" height="${alturaDaBarra}" rx="4"/>`;
  }).join('');
  return colunasDesenhadas + rotulos(colunas, altura);
}

function linhas(colunas: number, altura: number): string {
  const topo = 8;
  const chao = altura - RESPIRO_DO_ROTULO - 4;
  const faixa = LARGURA / colunas;
  const ponto = (serie: number[], i: number) =>
    `${faixa * i + faixa / 2},${chao - (chao - topo) * serie[i % serie.length]}`;
  const pontos = (serie: number[]) =>
    Array.from({ length: colunas }, (_, i) => ponto(serie, i)).join(' ');
  const traco = 'fill="none" stroke="#fff" vector-effect="non-scaling-stroke"';
  const grade = [0, 1 / 3, 2 / 3, 1]
    .map((f) => {
      const y = topo + (chao - topo) * f;
      return `<line x1="0" x2="${LARGURA}" y1="${y}" y2="${y}" ${traco} stroke-opacity="0.35"/>`;
    })
    .join('');
  const inicio = `${faixa / 2},${chao}`;
  const fim = `${LARGURA - faixa / 2},${chao}`;
  return (
    grade +
    `<polygon points="${inicio} ${pontos(REALIZADO)} ${fim}" fill-opacity="0.3"/>` +
    `<polyline points="${pontos(PREVISTO)}" ${traco} stroke-width="2" stroke-dasharray="6 6"/>` +
    `<polyline points="${pontos(REALIZADO)}" ${traco} stroke-width="3" stroke-linejoin="round"/>` +
    rotulos(colunas, altura)
  );
}

function anel(): string {
  return '<circle cx="60" cy="60" r="52" fill="none" stroke="#fff" stroke-width="12"/>';
}

export function mascaraDoGrafico(forma: FormaDoGrafico, colunas: number, altura: number): string {
  const svg =
    forma === 'anel'
      ? `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">${anel()}</svg>`
      : `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${LARGURA} ${altura}" preserveAspectRatio="none" fill="#fff">${
          forma === 'barras' ? barras(colunas, altura) : linhas(colunas, altura)
        }</svg>`;
  return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`;
}

@Component({
  selector: 'app-esqueleto-grafico',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'aria-hidden': 'true',
    '[style.height]': "forma() === 'anel' ? '100%' : altura() + 'px'",
    '[style.width]': "forma() === 'anel' ? '100%' : null",
    '[style.mask-image]': 'mascara()',
    '[style.-webkit-mask-image]': 'mascara()',
  },
  template: '',
  styles: `
    :host {
      position: relative;
      display: block;
      flex-shrink: 0;
      overflow: hidden;
      background: var(--p-skeleton-background);
      mask-size: 100% 100%;
      mask-repeat: no-repeat;
    }

    :host::after {
      content: '';
      position: absolute;
      inset: 0;
      transform: translateX(-100%);
      background: linear-gradient(
        90deg,
        transparent,
        var(--p-skeleton-animation-background),
        transparent
      );
      animation: brilho-do-esqueleto 1.4s ease-in-out infinite;
    }

    @keyframes brilho-do-esqueleto {
      to {
        transform: translateX(100%);
      }
    }

    @media (prefers-reduced-motion: reduce) {
      :host::after {
        animation: none;
      }
    }
  `,
})
export class EsqueletoGrafico {
  readonly forma = input<FormaDoGrafico>('barras');
  readonly colunas = input(6);
  readonly altura = input(200);

  protected readonly mascara = computed(() =>
    mascaraDoGrafico(this.forma(), this.colunas(), this.altura()),
  );
}
