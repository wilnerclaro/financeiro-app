import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { ROTAS_DA_API_SEM_TOKEN } from './autenticacao-api';
import { Sessao } from './sessao';

export const autenticacaoInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  if (!precisaDeToken(requisicao.url)) {
    return proximo(requisicao);
  }
  const sessao = inject(Sessao);
  const router = inject(Router);

  return proximo(comToken(requisicao, sessao.accessToken())).pipe(
    catchError((erro: unknown) => {
      if (!(erro instanceof HttpErrorResponse) || erro.status !== HttpStatusCode.Unauthorized) {
        return throwError(() => erro);
      }
      return sessao.renovar().pipe(
        catchError(() => {
          sessao.expirar(router.url);
          return throwError(() => erro);
        }),
        switchMap((novoToken) => proximo(comToken(requisicao, novoToken))),
      );
    }),
  );
};

function precisaDeToken(url: string): boolean {
  return url.startsWith('/api/') && !ROTAS_DA_API_SEM_TOKEN.some((rota) => url.startsWith(rota));
}

function comToken<T>(requisicao: HttpRequest<T>, token: string | null): HttpRequest<T> {
  return token
    ? requisicao.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : requisicao;
}
