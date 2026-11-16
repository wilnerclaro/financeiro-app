import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { tokenDeTeste } from '../../../../../testing/sessao-de-teste';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function preencher(email: string, senha: string): void {
    const campoEmail = elemento().querySelector<HTMLInputElement>('#email')!;
    const campoSenha = elemento().querySelector<HTMLInputElement>('#senha')!;
    campoEmail.value = email;
    campoEmail.dispatchEvent(new Event('input'));
    campoSenha.value = senha;
    campoSenha.dispatchEvent(new Event('input'));
  }

  async function enviar(): Promise<void> {
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit');
    await fixture.whenStable();
  }

  function botaoEntrar(): HTMLButtonElement {
    return elemento().querySelector<HTMLButtonElement>('button[type="submit"]')!;
  }

  it('tem rótulo em cada campo', () => {
    expect(elemento().querySelector('label[for="email"]')?.textContent).toContain('E-mail');
    expect(elemento().querySelector('label[for="senha"]')?.textContent).toContain('Senha');
  });

  it('mostra o erro de cada campo e não chama a API quando o formulário está vazio', async () => {
    await enviar();

    expect(elemento().textContent).toContain('Informe seu e-mail.');
    expect(elemento().textContent).toContain('Informe sua senha.');
    http.expectNone('/api/auth/login');
  });

  it('avisa quando o e-mail não é válido', async () => {
    preencher('ana@', 'segredo123');
    await enviar();

    expect(elemento().textContent).toContain('Informe um e-mail válido.');
    http.expectNone('/api/auth/login');
  });

  it('entra e vai para o início', async () => {
    preencher('ana@exemplo.com', 'segredo123');
    await enviar();

    const requisicao = http.expectOne('/api/auth/login');
    expect(requisicao.request.body).toEqual({ email: 'ana@exemplo.com', senha: 'segredo123' });
    expect(botaoEntrar().disabled).toBe(true);

    requisicao.flush(tokenDeTeste());
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
    expect(botaoEntrar().disabled).toBe(false);
  });

  it('volta para a rota que pediu o login', async () => {
    fixture.componentRef.setInput('voltar', '/orcamento/ano');
    preencher('ana@exemplo.com', 'segredo123');
    await enviar();

    http.expectOne('/api/auth/login').flush(tokenDeTeste());
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/orcamento/ano');
  });

  it('não envia de novo enquanto o primeiro envio não terminou', async () => {
    preencher('ana@exemplo.com', 'segredo123');
    await enviar();
    await enviar();

    http.expectOne('/api/auth/login').flush(tokenDeTeste());
  });

  it('mostra a mensagem da API, mantém o que foi digitado e libera o botão', async () => {
    preencher('ana@exemplo.com', 'errada');
    await enviar();

    http
      .expectOne('/api/auth/login')
      .flush(
        { status: 401, message: 'E-mail ou senha inválidos.', fieldErrors: null },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(elemento().querySelector('[role="alert"]')?.textContent).toContain(
      'E-mail ou senha inválidos.',
    );
    expect(elemento().querySelector<HTMLInputElement>('#email')!.value).toBe('ana@exemplo.com');
    expect(botaoEntrar().disabled).toBe(false);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
