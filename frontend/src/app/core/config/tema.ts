import { DOCUMENT, Injectable, effect, inject, signal } from '@angular/core';

const CHAVE_TEMA = 'financeiro.tema';
const CLASSE_ESCURO = 'app-dark';

@Injectable({ providedIn: 'root' })
export class Tema {
  private readonly documento = inject(DOCUMENT);

  readonly escuro = signal(this.preferenciaInicial());

  constructor() {
    effect(() => this.documento.documentElement.classList.toggle(CLASSE_ESCURO, this.escuro()));
  }

  alternar(): void {
    const escuro = !this.escuro();
    this.escuro.set(escuro);
    try {
      localStorage.setItem(CHAVE_TEMA, escuro ? 'escuro' : 'claro');
    } catch {
      return;
    }
  }

  private preferenciaInicial(): boolean {
    try {
      return localStorage.getItem(CHAVE_TEMA) === 'escuro';
    } catch {
      return false;
    }
  }
}
