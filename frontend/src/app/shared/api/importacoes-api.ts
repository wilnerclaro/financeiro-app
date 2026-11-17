import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ItemOfx {
  identificador: string;
  data: string;
  descricao: string;
  valor: number;
  tipo: 'RECEITA' | 'DESPESA';
  jaImportado: boolean;
  importavel: boolean;
  motivo: string | null;
}

export interface RevisaoOfx {
  itens: ItemOfx[];
  moeda: string;
}

export interface SelecaoOfx {
  identificador: string;
  categoriaId: string;
}

export interface ResultadoOfx {
  importados: number;
  ignorados: number;
}

@Injectable({ providedIn: 'root' })
export class ImportacoesApi {
  private readonly http = inject(HttpClient);

  revisar(arquivo: File, contaId: string): Observable<RevisaoOfx> {
    return this.http.post<RevisaoOfx>(
      '/api/importacoes/ofx/revisar',
      this.formulario(arquivo, contaId),
    );
  }

  confirmar(arquivo: File, contaId: string, selecoes: SelecaoOfx[]): Observable<ResultadoOfx> {
    const formulario = this.formulario(arquivo, contaId);
    formulario.append('selecoes', JSON.stringify(selecoes));
    return this.http.post<ResultadoOfx>('/api/importacoes/ofx/confirmar', formulario);
  }

  private formulario(arquivo: File, contaId: string): FormData {
    const formulario = new FormData();
    formulario.append('arquivo', arquivo);
    formulario.append('contaId', contaId);
    return formulario;
  }
}
