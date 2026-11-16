import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CopiaDePrevisto, PrevistoRealizado, ResumoAnual } from './api.models';

const BASE = '/api/orcamentos';

@Injectable({ providedIn: 'root' })
export class OrcamentoApi {
  private readonly http = inject(HttpClient);

  doMes(ano: number, mes: number): Observable<PrevistoRealizado> {
    return this.http.get<PrevistoRealizado>(`${BASE}/${ano}/${mes}`);
  }

  doAno(ano: number): Observable<ResumoAnual> {
    return this.http.get<ResumoAnual>(`${BASE}/${ano}`);
  }

  definir(
    ano: number,
    mes: number,
    itens: { categoriaId: string; valorPrevisto: number }[],
  ): Observable<PrevistoRealizado> {
    return this.http.put<PrevistoRealizado>(`${BASE}/${ano}/${mes}`, { itens });
  }

  repetir(
    categoriaId: string,
    valorPrevisto: number,
    meses: string[],
    substituirExistentes: boolean,
  ): Observable<CopiaDePrevisto> {
    return this.http.post<CopiaDePrevisto>(`${BASE}/repetir`, {
      categoriaId,
      valorPrevisto,
      meses,
      substituirExistentes,
    });
  }

  copiar(
    ano: number,
    mes: number,
    meses: string[],
    substituirExistentes: boolean,
  ): Observable<CopiaDePrevisto> {
    return this.http.post<CopiaDePrevisto>(`${BASE}/${ano}/${mes}/copiar`, {
      meses,
      substituirExistentes,
    });
  }
}
