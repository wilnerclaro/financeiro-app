import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type FormatoExtrato = 'OFX' | 'CSV' | 'PDF';

export interface ItemExtrato {
  identificador: string;
  data: string;
  descricao: string;
  valor: number;
  tipo: 'RECEITA' | 'DESPESA';
  jaImportado: boolean;
  possivelDuplicado: boolean;
  importavel: boolean;
  motivo: string | null;
}

export interface RevisaoExtrato {
  formato: FormatoExtrato;
  banco: string | null;
  itens: ItemExtrato[];
  moeda: string;
}

export interface SelecaoItemExtrato {
  identificador: string;
  categoriaId: string;
}

export interface ResultadoImportacao {
  importados: number;
  ignorados: number;
}

@Injectable({ providedIn: 'root' })
export class ImportacoesApi {
  private readonly http = inject(HttpClient);

  revisar(arquivo: File, contaId: string): Observable<RevisaoExtrato> {
    return this.http.post<RevisaoExtrato>(
      '/api/importacoes/revisar',
      this.formulario(arquivo, contaId),
    );
  }

  confirmar(
    arquivo: File,
    contaId: string,
    selecoes: SelecaoItemExtrato[],
  ): Observable<ResultadoImportacao> {
    const formulario = this.formulario(arquivo, contaId);
    formulario.append('selecoes', JSON.stringify(selecoes));
    return this.http.post<ResultadoImportacao>('/api/importacoes/confirmar', formulario);
  }

  private formulario(arquivo: File, contaId: string): FormData {
    const formulario = new FormData();
    formulario.append('arquivo', arquivo);
    formulario.append('contaId', contaId);
    return formulario;
  }
}
