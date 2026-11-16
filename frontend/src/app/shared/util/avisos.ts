import { Injectable, inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { mensagemDoErro } from '../../core/error-handling/erro-api';

@Injectable({ providedIn: 'root' })
export class Avisos {
  private readonly mensagens = inject(MessageService);

  sucesso(titulo: string, detalhe: string): void {
    this.mensagens.add({ severity: 'success', summary: titulo, detail: detalhe });
  }

  erro(titulo: string, erro: unknown): void {
    this.mensagens.add({ severity: 'error', summary: titulo, detail: mensagemDoErro(erro) });
  }
}
