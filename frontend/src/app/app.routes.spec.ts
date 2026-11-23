import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { MessageService } from 'primeng/api';
import { firstValueFrom } from 'rxjs';
import { tokenDeTeste } from '../testing/sessao-de-teste';
import { routes } from './app.routes';
import { Sessao } from './core/auth/sessao';
import { Acesso } from './features/autenticacao/pages/acesso/acesso';
import { Inicio } from './features/inicio/pages/inicio/inicio';
import { Casca } from './layout/casca';

describe('rotas do app', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
        MessageService,
      ],
    });
  });

  it('sem sessão, qualquer rota leva ao login', async () => {
    const harness = await RouterTestingHarness.create();
    const tela = await harness.navigateByUrl('/qualquer-coisa', Acesso);

    expect(tela).toBeInstanceOf(Acesso);
    expect(TestBed.inject(Router).url).toBe('/login?voltar=%2F');
    TestBed.inject(HttpTestingController)
      .expectOne('/api/auth/configuracao')
      .flush({ googleClientId: null });
  });

  it('criar conta abre a mesma tela no modo de cadastro', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/criar-conta', Acesso);
    TestBed.inject(HttpTestingController)
      .expectOne('/api/auth/configuracao')
      .flush({ googleClientId: null });
    harness.detectChanges();

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain(
      'Crie sua conta',
    );
  });

  it('com sessão, abre o início dentro da casca', async () => {
    const sessao = TestBed.inject(Sessao);
    const login = firstValueFrom(sessao.entrar({ email: 'ana@exemplo.com', senha: 'segredo123' }));
    TestBed.inject(HttpTestingController).expectOne('/api/auth/login').flush(tokenDeTeste());
    await login;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/', Casca);
    TestBed.inject(HttpTestingController).expectOne('/api/configuracao-inicial').flush({
      rendaPassivaDesejada: 6000,
      etapa: 'CONCLUIDO',
      pausado: false,
      ano: 2026,
      mes: 11,
    });

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Resumo de');
    expect(
      harness.fixture.debugElement.query((d) => d.componentInstance instanceof Inicio),
    ).toBeTruthy();
  });
});
