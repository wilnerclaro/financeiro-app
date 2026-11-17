import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class ImportacaoDeExtrato {
  private readonly abertaAgora = signal(false);
  private readonly contaEscolhida = signal<string | null>(null);
  private readonly totalConcluidas = signal(0);

  readonly aberta = this.abertaAgora.asReadonly();
  readonly contaSugerida = this.contaEscolhida.asReadonly();
  readonly concluidas = this.totalConcluidas.asReadonly();

  abrir(contaId: string | null = null): void {
    this.contaEscolhida.set(contaId);
    this.abertaAgora.set(true);
  }

  fechar(): void {
    this.abertaAgora.set(false);
  }

  registrarConclusao(): void {
    this.totalConcluidas.update((total) => total + 1);
  }
}

export const abrirImportacaoGuard: CanActivateFn = () => {
  inject(ImportacaoDeExtrato).abrir();
  return inject(Router).parseUrl('/lancamentos');
};
