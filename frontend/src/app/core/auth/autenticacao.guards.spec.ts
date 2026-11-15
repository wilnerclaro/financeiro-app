import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { autenticadoGuard, visitanteGuard } from './autenticacao.guards';
import { Sessao } from './sessao';

describe('guards de autenticação', () => {
  const autenticado = signal(false);
  const rota = {} as ActivatedRouteSnapshot;
  const estado = { url: '/orcamento?ano=2026' } as RouterStateSnapshot;

  beforeEach(() => {
    autenticado.set(false);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Sessao, useValue: { autenticado } }],
    });
  });

  function executar(guard: typeof autenticadoGuard): ReturnType<typeof autenticadoGuard> {
    return TestBed.runInInjectionContext(() => guard(rota, estado));
  }

  it('deixa entrar na área logada quem tem sessão', () => {
    autenticado.set(true);
    expect(executar(autenticadoGuard)).toBe(true);
  });

  it('manda para o login quem não tem sessão, guardando a rota de volta', () => {
    const destino = executar(autenticadoGuard) as UrlTree;
    expect(destino.toString()).toBe('/login?voltar=%2Forcamento%3Fano%3D2026');
  });

  it('mostra o login para quem não entrou', () => {
    expect(executar(visitanteGuard)).toBe(true);
  });

  it('manda para o início quem já entrou e abre o login', () => {
    autenticado.set(true);
    expect((executar(visitanteGuard) as UrlTree).toString()).toBe('/');
  });
});
