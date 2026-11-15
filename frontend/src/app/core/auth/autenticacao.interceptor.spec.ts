import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CHAVE_REFRESH_TOKEN, tokenDeTeste } from '../../../testing/sessao-de-teste';
import { autenticacaoInterceptor } from './autenticacao.interceptor';
import { Sessao } from './sessao';

describe('autenticacaoInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let sessao: Sessao;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([autenticacaoInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(Sessao);
  });

  afterEach(() => backend.verify());

  async function entrar(): Promise<void> {
    const login = firstValueFrom(sessao.entrar({ email: 'ana@exemplo.com', senha: 'segredo123' }));
    const requisicao = backend.expectOne('/api/auth/login');
    expect(requisicao.request.headers.has('Authorization')).toBe(false);
    requisicao.flush(tokenDeTeste());
    await login;
  }

  it('envia o Bearer nas chamadas da API', async () => {
    await entrar();

    const resposta = firstValueFrom(http.get('/api/contas'));
    const requisicao = backend.expectOne('/api/contas');
    expect(requisicao.request.headers.get('Authorization')).toBe('Bearer acesso-1');
    requisicao.flush({ content: [] });

    expect(await resposta).toEqual({ content: [] });
  });

  it('não envia o token para fora da API', async () => {
    await entrar();

    void firstValueFrom(http.get('/assets/config.json'));
    const requisicao = backend.expectOne('/assets/config.json');
    expect(requisicao.request.headers.has('Authorization')).toBe(false);
    requisicao.flush({});
  });

  it('chama a API sem Bearer quando ninguém entrou e manda para o login no 401', async () => {
    const expirar = vi.spyOn(sessao, 'expirar').mockImplementation(() => undefined);

    const resposta = firstValueFrom(http.get('/api/contas'));
    const requisicao = backend.expectOne('/api/contas');
    expect(requisicao.request.headers.has('Authorization')).toBe(false);
    requisicao.flush({}, { status: 401, statusText: 'Unauthorized' });

    await expect(resposta).rejects.toBeInstanceOf(HttpErrorResponse);
    backend.expectNone('/api/auth/refresh');
    expect(expirar).toHaveBeenCalledTimes(1);
  });

  it('renova o token em 401 e repete a chamada uma vez', async () => {
    await entrar();

    const resposta = firstValueFrom(http.get('/api/contas'));
    backend.expectOne('/api/contas').flush({}, { status: 401, statusText: 'Unauthorized' });
    const refresh = backend.expectOne('/api/auth/refresh');
    expect(refresh.request.body).toEqual({ refreshToken: 'refresh-1' });
    refresh.flush(tokenDeTeste(2));

    const repetida = backend.expectOne('/api/contas');
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer acesso-2');
    repetida.flush({ content: ['conta'] });

    expect(await resposta).toEqual({ content: ['conta'] });
  });

  it('expira a sessão e devolve o erro original quando o refresh falha', async () => {
    await entrar();
    const expirar = vi.spyOn(sessao, 'expirar').mockImplementation(() => undefined);

    const resposta = firstValueFrom(http.get('/api/contas'));
    backend.expectOne('/api/contas').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    const erro = await resposta.catch((e: unknown) => e);
    expect(erro).toBeInstanceOf(HttpErrorResponse);
    expect((erro as HttpErrorResponse).url).toBe('/api/contas');
    expect(expirar).toHaveBeenCalledTimes(1);
  });

  it('não entra em laço quando a chamada repetida também recebe 401', async () => {
    await entrar();

    const resposta = firstValueFrom(http.get('/api/contas'));
    backend.expectOne('/api/contas').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/auth/refresh').flush(tokenDeTeste(2));
    backend.expectOne('/api/contas').flush({}, { status: 401, statusText: 'Unauthorized' });

    const erro = (await resposta.catch((e: unknown) => e)) as HttpErrorResponse;
    expect(erro.status).toBe(401);
    backend.expectNone('/api/auth/refresh');
  });

  it('repassa outros erros sem tentar renovar', async () => {
    await entrar();

    const resposta = firstValueFrom(http.get('/api/contas'));
    backend.expectOne('/api/contas').flush({}, { status: 500, statusText: 'Server Error' });

    const erro = (await resposta.catch((e: unknown) => e)) as HttpErrorResponse;
    expect(erro.status).toBe(500);
    backend.expectNone('/api/auth/refresh');
  });

  it('não tenta renovar quando o próprio login responde 401', async () => {
    const resposta = firstValueFrom(sessao.entrar({ email: 'ana@exemplo.com', senha: 'errada' }));
    backend
      .expectOne('/api/auth/login')
      .flush(
        { message: 'E-mail ou senha inválidos.' },
        { status: 401, statusText: 'Unauthorized' },
      );

    await expect(resposta).rejects.toBeInstanceOf(HttpErrorResponse);
    backend.expectNone('/api/auth/refresh');
    expect(sessionStorage.getItem(CHAVE_REFRESH_TOKEN)).toBeNull();
  });
});
