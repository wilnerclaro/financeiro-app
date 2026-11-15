import { Routes } from '@angular/router';
import { autenticadoGuard, visitanteGuard } from './core/auth/autenticacao.guards';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [visitanteGuard],
    loadComponent: () => import('./features/autenticacao/pages/login/login').then((m) => m.Login),
    title: 'Entrar · financeiro',
  },
  {
    path: '',
    canActivate: [autenticadoGuard],
    loadComponent: () => import('./layout/casca').then((m) => m.Casca),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/inicio/pages/inicio/inicio').then((m) => m.Inicio),
        title: 'Início · financeiro',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
