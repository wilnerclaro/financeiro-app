import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Sessao } from './sessao';

export const autenticadoGuard: CanActivateFn = (_rota, estado) =>
  inject(Sessao).autenticado() ||
  inject(Router).createUrlTree(['/login'], { queryParams: { voltar: estado.url } });

export const visitanteGuard: CanActivateFn = () =>
  !inject(Sessao).autenticado() || inject(Router).createUrlTree(['/']);
