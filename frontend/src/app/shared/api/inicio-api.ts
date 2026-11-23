import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Inicio } from './api.models';
import { parametros } from './parametros';

@Injectable({ providedIn: 'root' })
export class InicioApi {
  private readonly http = inject(HttpClient);

  buscar(ano?: number, mes?: number): Observable<Inicio> {
    return this.http.get<Inicio>('/api/dashboard-financeiro/inicio', {
      params: parametros({ ano, mes }),
    });
  }
}
