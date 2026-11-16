import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Pagina, Transferencia, TransferenciaRequest } from './api.models';
import { parametros } from './parametros';

const BASE = '/api/transferencias';

@Injectable({ providedIn: 'root' })
export class TransferenciasApi {
  private readonly http = inject(HttpClient);

  listar(dataInicio: string, dataFim: string): Observable<Transferencia[]> {
    return this.http
      .get<Pagina<Transferencia>>(BASE, {
        params: parametros({ dataInicio, dataFim, size: 200, sort: 'dataTransferencia,desc' }),
      })
      .pipe(map((pagina) => pagina.content));
  }

  criar(request: TransferenciaRequest): Observable<Transferencia> {
    return this.http.post<Transferencia>(BASE, request);
  }

  cancelar(id: string): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }
}
