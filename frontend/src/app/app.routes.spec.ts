import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { firstValueFrom } from 'rxjs';
import { tokenDeTeste } from '../testing/sessao-de-teste';
import { routes } from './app.routes';
import { Sessao } from './core/auth/sessao';
import { Login } from './features/autenticacao/pages/login/login';
import { Inicio } from './features/inicio/pages/inicio/inicio';
import { Casca } from './layout/casca';

describe('rotas do app', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('sem sessão, qualquer rota leva ao login', async () => {
    const harness = await RouterTestingHarness.create();
    const tela = await harness.navigateByUrl('/qualquer-coisa', Login);

    expect(tela).toBeInstanceOf(Login);
    expect(TestBed.inject(Router).url).toBe('/login?voltar=%2F');
  });

  it('com sessão, abre o início dentro da casca', async () => {
    const sessao = TestBed.inject(Sessao);
    const login = firstValueFrom(sessao.entrar({ email: 'ana@exemplo.com', senha: 'segredo123' }));
    TestBed.inject(HttpTestingController).expectOne('/api/auth/login').flush(tokenDeTeste());
    await login;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/', Casca);

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Olá, Ana');
    expect(
      harness.fixture.debugElement.query((d) => d.componentInstance instanceof Inicio),
    ).toBeTruthy();
  });
});
