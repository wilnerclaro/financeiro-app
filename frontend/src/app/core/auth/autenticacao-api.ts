import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LoginRequest, TokenResponse } from './autenticacao.models';

const BASE = '/api/auth';

export const ROTAS_DA_API_SEM_TOKEN = [`${BASE}/login`, `${BASE}/cadastro`, `${BASE}/refresh`];

@Injectable({ providedIn: 'root' })
export class AutenticacaoApi {
  private readonly http = inject(HttpClient);

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${BASE}/login`, request);
  }

  renovar(refreshToken: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${BASE}/refresh`, { refreshToken });
  }
}
