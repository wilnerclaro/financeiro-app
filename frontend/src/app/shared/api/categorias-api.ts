import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Categoria, CategoriaRequest, Pagina } from './api.models';
import { parametros } from './parametros';

const BASE = '/api/categorias';

@Injectable({ providedIn: 'root' })
export class CategoriasApi {
  private readonly http = inject(HttpClient);

  listar(ativa?: boolean): Observable<Categoria[]> {
    return this.http
      .get<Pagina<Categoria>>(BASE, { params: parametros({ ativa, size: 500, sort: 'nome' }) })
      .pipe(map((pagina) => pagina.content));
  }

  criar(request: CategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(BASE, request);
  }

  atualizar(id: string, request: CategoriaRequest): Observable<Categoria> {
    return this.http.put<Categoria>(`${BASE}/${id}`, request);
  }

  inativar(id: string): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  ativar(id: string): Observable<Categoria> {
    return this.http.patch<Categoria>(`${BASE}/${id}/ativar`, {});
  }
}
