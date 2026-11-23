import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CHAVE_REFRESH_TOKEN, tokenDeTeste } from '../../../testing/sessao-de-teste';
import { Sessao } from './sessao';

describe('Sessao', () => {
  let sessao: Sessao;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    sessao = TestBed.inject(Sessao);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  it('entra com e-mail e senha, guarda o access token só em memória e o refresh na sessão da aba', async () => {
    const usuario = firstValueFrom(
      sessao.entrar({ email: 'ana@exemplo.com', senha: 'segredo123' }),
    );

    const requisicao = http.expectOne('/api/auth/login');
    expect(requisicao.request.method).toBe('POST');
    expect(requisicao.request.body).toEqual({ email: 'ana@exemplo.com', senha: 'segredo123' });
    requisicao.flush(tokenDeTeste());

    expect((await usuario).nome).toBe('Ana Souza');
    expect(sessao.autenticado()).toBe(true);
    expect(sessao.accessToken()).toBe('acesso-1');
    expect(sessionStorage.getItem(CHAVE_REFRESH_TOKEN)).toBe('refresh-1');
    expect(JSON.stringify({ ...sessionStorage, ...localStorage })).not.toContain('acesso-1');
  });

  it('cadastra e já abre a sessão com os tokens devolvidos', async () => {
    const usuario = firstValueFrom(
      sessao.cadastrar({ nome: 'Ana', email: 'ana@exemplo.com', senha: 'familia2026' }),
    );

    const requisicao = http.expectOne('/api/auth/cadastro');
    expect(requisicao.request.body).toEqual({
      nome: 'Ana',
      email: 'ana@exemplo.com',
      senha: 'familia2026',
    });
    requisicao.flush(tokenDeTeste());

    expect((await usuario).email).toBe('ana@exemplo.com');
    expect(sessao.accessToken()).toBe('acesso-1');
  });

  it('entra com o ID token do Google', async () => {
    const usuario = firstValueFrom(sessao.entrarComGoogle('token-do-google'));

    const requisicao = http.expectOne('/api/auth/google');
    expect(requisicao.request.body).toEqual({ idToken: 'token-do-google' });
    requisicao.flush(tokenDeTeste(3));

    expect((await usuario).nome).toBe('Ana Souza');
    expect(sessionStorage.getItem(CHAVE_REFRESH_TOKEN)).toBe('refresh-3');
  });

  it('não chama a API para renovar quando não há refresh token', async () => {
    await expect(firstValueFrom(sessao.renovar())).rejects.toThrow('Não há sessão para renovar.');
  });

  it('faz um único refresh para chamadas simultâneas', async () => {
    sessionStorage.setItem(CHAVE_REFRESH_TOKEN, 'refresh-0');

    const primeira = firstValueFrom(sessao.renovar());
    const segunda = firstValueFrom(sessao.renovar());

    const requisicao = http.expectOne('/api/auth/refresh');
    expect(requisicao.request.body).toEqual({ refreshToken: 'refresh-0' });
    requisicao.flush(tokenDeTeste(2));

    expect(await primeira).toBe('acesso-2');
    expect(await segunda).toBe('acesso-2');
    expect(sessionStorage.getItem(CHAVE_REFRESH_TOKEN)).toBe('refresh-2');
  });

  it('pede um refresh novo depois que o anterior terminou', async () => {
    sessionStorage.setItem(CHAVE_REFRESH_TOKEN, 'refresh-0');

    const primeira = firstValueFrom(sessao.renovar());
    http.expectOne('/api/auth/refresh').flush(tokenDeTeste(2));
    await primeira;

    const segunda = firstValueFrom(sessao.renovar());
    const requisicao = http.expectOne('/api/auth/refresh');
    expect(requisicao.request.body).toEqual({ refreshToken: 'refresh-2' });
    requisicao.flush(tokenDeTeste(3));
    expect(await segunda).toBe('acesso-3');
  });

  it('não restaura nada quando a aba não tem refresh token', async () => {
    await sessao.restaurar();

    http.expectNone('/api/auth/refresh');
    expect(sessao.autenticado()).toBe(false);
  });

  it('restaura a sessão ao abrir o app quando há refresh token', async () => {
    sessionStorage.setItem(CHAVE_REFRESH_TOKEN, 'refresh-0');

    const restauracao = sessao.restaurar();
    http.expectOne('/api/auth/refresh').flush(tokenDeTeste());
    await restauracao;

    expect(sessao.autenticado()).toBe(true);
    expect(sessao.usuario()?.email).toBe('ana@exemplo.com');
  });

  it('descarta o refresh token quando a restauração falha', async () => {
    sessionStorage.setItem(CHAVE_REFRESH_TOKEN, 'refresh-vencido');

    const restauracao = sessao.restaurar();
    http
      .expectOne('/api/auth/refresh')
      .flush(
        { status: 401, message: 'Refresh token inválido ou expirado.' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await restauracao;

    expect(sessao.autenticado()).toBe(false);
    expect(sessionStorage.getItem(CHAVE_REFRESH_TOKEN)).toBeNull();
  });

  it('ao sair, descarta os tokens e volta para o login', async () => {
    await entrar();

    sessao.sair();

    expect(sessao.autenticado()).toBe(false);
    expect(sessao.usuario()).toBeNull();
    expect(sessionStorage.getItem(CHAVE_REFRESH_TOKEN)).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('quando a sessão expira, volta para o login guardando a rota de volta', async () => {
    await entrar();

    sessao.expirar('/contas');

    expect(sessao.autenticado()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { voltar: '/contas' },
    });
  });

  async function entrar(): Promise<void> {
    const login = firstValueFrom(sessao.entrar({ email: 'ana@exemplo.com', senha: 'segredo123' }));
    http.expectOne('/api/auth/login').flush(tokenDeTeste());
    await login;
  }
});
