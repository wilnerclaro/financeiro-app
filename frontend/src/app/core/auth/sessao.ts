import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, finalize, firstValueFrom, map, shareReplay, tap, throwError } from 'rxjs';
import { AutenticacaoApi } from './autenticacao-api';
import { LoginRequest, TokenResponse, UsuarioResponse } from './autenticacao.models';

const CHAVE_REFRESH = 'financeiro.refreshToken';

@Injectable({ providedIn: 'root' })
export class Sessao {
  private readonly api = inject(AutenticacaoApi);
  private readonly router = inject(Router);

  private readonly token = signal<string | null>(null);
  private readonly usuarioLogado = signal<UsuarioResponse | null>(null);
  private renovacaoEmAndamento: Observable<string> | null = null;

  readonly accessToken = this.token.asReadonly();
  readonly usuario = this.usuarioLogado.asReadonly();
  readonly autenticado = computed(() => this.token() !== null);

  entrar(credenciais: LoginRequest): Observable<UsuarioResponse> {
    return this.api.login(credenciais).pipe(
      tap((resposta) => this.iniciar(resposta)),
      map((resposta) => resposta.usuario),
    );
  }

  renovar(): Observable<string> {
    const refreshToken = lerRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('Não há sessão para renovar.'));
    }
    this.renovacaoEmAndamento ??= this.api.renovar(refreshToken).pipe(
      tap((resposta) => this.iniciar(resposta)),
      map((resposta) => resposta.accessToken),
      finalize(() => (this.renovacaoEmAndamento = null)),
      shareReplay(1),
    );
    return this.renovacaoEmAndamento;
  }

  async restaurar(): Promise<void> {
    if (!lerRefreshToken()) {
      return;
    }
    try {
      await firstValueFrom(this.renovar());
    } catch {
      this.limpar();
    }
  }

  sair(): void {
    this.limpar();
    void this.router.navigate(['/login']);
  }

  expirar(urlDeVolta: string): void {
    this.limpar();
    void this.router.navigate(['/login'], { queryParams: { voltar: urlDeVolta } });
  }

  private iniciar(resposta: TokenResponse): void {
    this.token.set(resposta.accessToken);
    this.usuarioLogado.set(resposta.usuario);
    guardarRefreshToken(resposta.refreshToken);
  }

  private limpar(): void {
    this.token.set(null);
    this.usuarioLogado.set(null);
    guardarRefreshToken(null);
  }
}

function lerRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(CHAVE_REFRESH);
  } catch {
    return null;
  }
}

function guardarRefreshToken(refreshToken: string | null): void {
  try {
    if (refreshToken) {
      sessionStorage.setItem(CHAVE_REFRESH, refreshToken);
    } else {
      sessionStorage.removeItem(CHAVE_REFRESH);
    }
  } catch {
    return;
  }
}
