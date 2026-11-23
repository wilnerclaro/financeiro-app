import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Fatura,
  FiltroLancamentos,
  Lancamento,
  LancamentoRequest,
  PagarFaturaRequest,
  Pagina,
} from './api.models';
import { parametros } from './parametros';

const BASE = '/api/lancamentos-financeiros';

@Injectable({ providedIn: 'root' })
export class LancamentosApi {
  private readonly http = inject(HttpClient);

  listar(filtro: FiltroLancamentos): Observable<Pagina<Lancamento>> {
    return this.http.get<Pagina<Lancamento>>(BASE, {
      params: parametros({ sort: 'dataCompetencia,desc', ...filtro }),
    });
  }

  buscar(id: string): Observable<Lancamento> {
    return this.http.get<Lancamento>(`${BASE}/${id}`);
  }

  criar(request: LancamentoRequest): Observable<Lancamento> {
    return this.http.post<Lancamento>(BASE, request);
  }

  atualizar(id: string, request: LancamentoRequest): Observable<Lancamento> {
    return this.http.put<Lancamento>(`${BASE}/${id}`, request);
  }

  pagar(id: string, dataPagamento: string): Observable<Lancamento> {
    return this.http.patch<Lancamento>(`${BASE}/${id}/pagar`, { dataPagamento });
  }

  cancelar(id: string): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  fatura(contaId: string, ano: number, mes: number): Observable<Fatura> {
    return this.http.get<Fatura>(`${BASE}/faturas`, {
      params: new HttpParams().set('contaId', contaId).set('ano', ano).set('mes', mes),
    });
  }

  pagarFatura(request: PagarFaturaRequest): Observable<Fatura> {
    return this.http.patch<Fatura>(`${BASE}/faturas/pagar`, request);
  }
}
