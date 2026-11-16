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
      {
        path: 'lancar',
        loadComponent: () => import('./features/lancar/pages/lancar/lancar').then((m) => m.Lancar),
        title: 'Novo lançamento · financeiro',
      },
      {
        path: 'lancar/:id',
        loadComponent: () => import('./features/lancar/pages/lancar/lancar').then((m) => m.Lancar),
        title: 'Editar lançamento · financeiro',
      },
      {
        path: 'lancamentos',
        loadComponent: () =>
          import('./features/lancamentos/pages/lancamentos/lancamentos').then((m) => m.Lancamentos),
        title: 'Lançamentos · financeiro',
      },
      {
        path: 'orcamento',
        loadComponent: () =>
          import('./features/orcamento/pages/orcamento-mes/orcamento-mes').then(
            (m) => m.OrcamentoMes,
          ),
        title: 'Orçamento · financeiro',
      },
      {
        path: 'orcamento/ano',
        loadComponent: () =>
          import('./features/orcamento/pages/orcamento-ano/orcamento-ano').then(
            (m) => m.OrcamentoAno,
          ),
        title: 'Orçamento do ano · financeiro',
      },
      {
        path: 'orcamento/previsto',
        loadComponent: () =>
          import('./features/orcamento/pages/orcamento-previsto/orcamento-previsto').then(
            (m) => m.OrcamentoPrevisto,
          ),
        title: 'Editar previsto · financeiro',
      },
      {
        path: 'contas',
        loadComponent: () => import('./features/contas/pages/contas/contas').then((m) => m.Contas),
        title: 'Contas · financeiro',
      },
      {
        path: 'categorias',
        loadComponent: () =>
          import('./features/categorias/pages/categorias/categorias').then((m) => m.Categorias),
        title: 'Categorias · financeiro',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
