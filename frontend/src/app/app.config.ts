import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localePt from '@angular/common/locales/pt';
import {
  ApplicationConfig,
  DEFAULT_CURRENCY_CODE,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { environment } from '../environments/environment';
import { routes } from './app.routes';
import { autenticacaoInterceptor } from './core/auth/autenticacao.interceptor';
import { Sessao } from './core/auth/sessao';
import { Tema } from './core/config/tema';
import { temaFinanceiro } from './core/config/tema-preset';

registerLocaleData(localePt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding(), withViewTransitions()),
    provideHttpClient(withFetch(), withInterceptors([autenticacaoInterceptor])),
    providePrimeNG({
      license: environment.primeuiLicense,
      ripple: true,
      theme: { preset: temaFinanceiro, options: { darkModeSelector: '.app-dark' } },
    }),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'BRL' },
    MessageService,
    provideAppInitializer(() => {
      inject(Tema);
      return inject(Sessao).restaurar();
    }),
  ],
};
