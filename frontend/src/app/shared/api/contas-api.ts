import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Conta, ContaRequest, CriarContaRequest, Pagina, ResumoContas } from './api.models';
import { parametros } from './parametros';

const BASE = '/api/contas';

@Injectable({ providedIn: 'root' })
export class ContasApi {
  private readonly http = inject(HttpClient);

  listar(ativa?: boolean): Observable<Conta[]> {
    return this.http
      .get<Pagina<Conta>>(BASE, { params: parametros({ ativa, size: 200, sort: 'nome' }) })
      .pipe(map((pagina) => pagina.content));
  }

  resumo(): Observable<ResumoContas> {
    return this.http.get<ResumoContas>(`${BASE}/resumo`);
  }

  criar(request: CriarContaRequest): Observable<Conta> {
    return this.http.post<Conta>(BASE, request);
  }

  atualizar(id: string, request: ContaRequest): Observable<Conta> {
    return this.http.put<Conta>(`${BASE}/${id}`, request);
  }

  corrigirSaldoInicial(id: string, saldoInicial: number): Observable<Conta> {
    return this.http.patch<Conta>(`${BASE}/${id}/corrigir-saldo-inicial`, { saldoInicial });
  }

  inativar(id: string): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  ativar(id: string): Observable<Conta> {
    return this.http.patch<Conta>(`${BASE}/${id}/ativar`, {});
  }
}
