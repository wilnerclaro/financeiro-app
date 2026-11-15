import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  effect,
  inject,
  input,
} from '@angular/core';
import { IconNode, createElement } from 'lucide';

@Component({
  selector: 'app-icone',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '',
  host: { 'aria-hidden': 'true' },
  styles: `
    :host {
      display: inline-flex;
      flex: none;
      line-height: 0;
    }
  `,
})
export class Icone {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly icone = input.required<IconNode>();
  readonly tamanho = input(16);
  readonly traco = input(1.75);

  constructor() {
    effect(() => {
      const svg = createElement(this.icone(), {
        width: this.tamanho(),
        height: this.tamanho(),
        'stroke-width': this.traco(),
        focusable: 'false',
      });
      this.host.nativeElement.replaceChildren(svg);
    });
  }
}
