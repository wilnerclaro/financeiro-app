import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { tokenDeTeste } from '../../../../../testing/sessao-de-teste';
import { BotaoGoogle, GoogleIdentidade } from '../../../../core/auth/google-identidade';
import { Tema } from '../../../../core/config/tema';
import { MENSAGEM_SEM_CONEXAO } from '../../../../core/error-handling/erro-api';
import { Acesso, ModoDeAcesso } from './acesso';

const CLIENT_ID = 'cliente.apps.googleusercontent.com';

describe('Acesso', () => {
  let fixture: ComponentFixture<Acesso>;
  let http: HttpTestingController;
  let router: Router;
  let desenharBotao: ReturnType<typeof vi.fn>;

  async function abrir(modo: ModoDeAcesso, googleClientId: string | null = null): Promise<void> {
    fixture = TestBed.createComponent(Acesso);
    fixture.componentRef.setInput('modo', modo);
    TestBed.tick();
    http.expectOne('/api/auth/configuracao').flush({ googleClientId });
    await fixture.whenStable();
  }

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function digitar(seletor: string, valor: string): void {
    const campo = elemento().querySelector<HTMLInputElement>(seletor)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  }

  async function enviar(): Promise<void> {
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit');
    await fixture.whenStable();
  }

  function botaoPrincipal(): HTMLButtonElement {
    return elemento().querySelector<HTMLButtonElement>('button[type="submit"]')!;
  }

  function alerta(): string {
    return elemento().querySelector('[role="alert"]')?.textContent ?? '';
  }

  function ultimoBotaoGoogle(): BotaoGoogle {
    return desenharBotao.mock.lastCall![1] as BotaoGoogle;
  }

  function erroDaApi(status: number, message: string, fieldErrors: unknown = null) {
    return [
      { status, message, fieldErrors },
      { status, statusText: 'Erro' },
    ] as const;
  }

  beforeEach(() => {
    sessionStorage.clear();
    desenharBotao = vi.fn().mockResolvedValue(undefined);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: GoogleIdentidade, useValue: { desenharBotao } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  describe('entrar', () => {
    beforeEach(() => abrir('entrar'));

    it('mostra título, rótulos e o link para criar conta', () => {
      expect(elemento().querySelector('h1')?.textContent).toContain('Bem-vindo de volta');
      expect(elemento().querySelector('label[for="email"]')?.textContent).toContain('E-mail');
      expect(elemento().querySelector('label[for="senha"]')?.textContent).toContain('Senha');
      expect(elemento().querySelector('#nome')).toBeNull();
      expect(elemento().querySelector('.trocar a')?.getAttribute('href')).toBe('/criar-conta');
    });

    it('esconde o Google quando a API não tem Client ID', () => {
      expect(elemento().querySelector('.google')).toBeNull();
      expect(elemento().textContent).not.toContain('ou com e-mail');
      expect(desenharBotao).not.toHaveBeenCalled();
    });

    it('pede e-mail e senha antes de chamar a API', async () => {
      await enviar();

      expect(elemento().textContent).toContain('Informe seu e-mail.');
      expect(elemento().textContent).toContain('Informe sua senha.');
      http.expectNone('/api/auth/login');
    });

    it('avisa quando o e-mail não é válido', async () => {
      digitar('#email', 'ana@');
      digitar('#senha', 'segredo123');
      await enviar();

      expect(elemento().textContent).toContain('Informe um e-mail válido.');
      http.expectNone('/api/auth/login');
    });

    it('entra, trava o formulário enquanto envia e vai para o destino guardado', async () => {
      fixture.componentRef.setInput('voltar', '/orcamento/ano');
      digitar('#email', 'ana@exemplo.com');
      digitar('#senha', 'segredo123');
      await enviar();
      await enviar();

      const requisicao = http.expectOne('/api/auth/login');
      expect(requisicao.request.body).toEqual({ email: 'ana@exemplo.com', senha: 'segredo123' });
      expect(botaoPrincipal().disabled).toBe(true);
      expect(botaoPrincipal().textContent).toContain('Entrando…');
      expect(elemento().querySelector<HTMLInputElement>('#email')!.disabled).toBe(true);

      requisicao.flush(tokenDeTeste());
      await fixture.whenStable();

      expect(router.navigateByUrl).toHaveBeenCalledWith('/orcamento/ano');
      expect(botaoPrincipal().disabled).toBe(false);
    });

    it('senha errada não diz qual campo errou e marca os dois', async () => {
      digitar('#email', 'ana@exemplo.com');
      digitar('#senha', 'errada');
      await enviar();

      http.expectOne('/api/auth/login').flush(...erroDaApi(401, 'E-mail ou senha inválidos.'));
      await fixture.whenStable();

      expect(alerta()).toContain('E-mail ou senha não conferem. Confira e tente de novo.');
      expect(
        elemento().querySelectorAll('.p-invalid, [aria-invalid="true"]').length,
      ).toBeGreaterThan(0);
      expect(elemento().querySelector<HTMLInputElement>('#email')!.value).toBe('ana@exemplo.com');
      expect(botaoPrincipal().disabled).toBe(false);
      expect(router.navigateByUrl).not.toHaveBeenCalled();
    });

    it('mostra o aviso de conexão quando o servidor não responde', async () => {
      digitar('#email', 'ana@exemplo.com');
      digitar('#senha', 'segredo123');
      await enviar();

      http.expectOne('/api/auth/login').error(new ProgressEvent('error'), { status: 0 });
      await fixture.whenStable();

      expect(alerta()).toContain(MENSAGEM_SEM_CONEXAO);
    });

    it('troca o tema', () => {
      const tema = TestBed.inject(Tema);
      const antes = tema.escuro();

      elemento().querySelector<HTMLButtonElement>('.barra-topo button')!.click();

      expect(tema.escuro()).toBe(!antes);
    });
  });

  describe('criar conta', () => {
    beforeEach(() => abrir('criar'));

    it('pede só nome, e-mail e senha, com o link para entrar', () => {
      expect(elemento().querySelector('h1')?.textContent).toContain('Crie sua conta');
      expect(elemento().querySelector('label[for="nome"]')?.textContent).toContain(
        'Como podemos te chamar?',
      );
      expect(elemento().querySelector('label[for="senha"]')?.textContent).toContain(
        'Crie uma senha',
      );
      expect(elemento().querySelectorAll('input').length).toBe(3);
      expect(elemento().querySelector('.trocar a')?.getAttribute('href')).toBe('/login');
    });

    it('marca as regras da senha conforme a pessoa digita', async () => {
      const atendidas = () =>
        Array.from(elemento().querySelectorAll('.regras li.ok')).map((li) => li.textContent);

      expect(atendidas()).toEqual([]);
      digitar('#senha', 'abc');
      await fixture.whenStable();
      expect(atendidas()).toEqual([expect.stringContaining('Uma letra')]);

      digitar('#senha', 'familia2026');
      await fixture.whenStable();
      expect(atendidas()).toHaveLength(3);
      expect(elemento().querySelector('.regras')?.textContent).toContain('(atendida)');
    });

    it('valida os campos antes de enviar', async () => {
      await enviar();
      expect(elemento().textContent).toContain('Informe seu nome.');
      expect(elemento().textContent).toContain('Crie uma senha.');

      digitar('#senha', 'semnumero');
      await fixture.whenStable();
      expect(elemento().textContent).toContain('A senha precisa seguir as regras abaixo.');
      http.expectNone('/api/auth/cadastro');
    });

    it('cria a conta e entra direto', async () => {
      digitar('#nome', 'Ana');
      digitar('#email', 'ana@exemplo.com');
      digitar('#senha', 'familia2026');
      await enviar();

      const requisicao = http.expectOne('/api/auth/cadastro');
      expect(requisicao.request.body).toEqual({
        nome: 'Ana',
        email: 'ana@exemplo.com',
        senha: 'familia2026',
      });
      expect(botaoPrincipal().textContent).toContain('Criando sua conta…');
      requisicao.flush(tokenDeTeste());
      await fixture.whenStable();

      expect(router.navigateByUrl).toHaveBeenCalledWith('/');
    });

    it('e-mail já usado oferece entrar', async () => {
      digitar('#nome', 'Ana');
      digitar('#email', 'ana@exemplo.com');
      digitar('#senha', 'familia2026');
      await enviar();

      http
        .expectOne('/api/auth/cadastro')
        .flush(...erroDaApi(409, 'Já existe uma conta com este e-mail.'));
      await fixture.whenStable();

      expect(alerta()).toContain('Já existe uma conta com este e-mail.');
      expect(alerta()).toContain('ou use outro e-mail.');
      expect(elemento().querySelector('[role="alert"] a')?.getAttribute('href')).toBe('/login');
    });

    it('mostra o erro de campo da API e o tira quando a pessoa corrige', async () => {
      digitar('#nome', 'Ana');
      digitar('#email', 'ana@exemplo.com');
      digitar('#senha', 'familia2026');
      await enviar();

      http
        .expectOne('/api/auth/cadastro')
        .flush(
          ...erroDaApi(400, 'Existem campos inválidos na requisição.', [
            { field: 'email', message: 'O e-mail deve ter no máximo 180 caracteres.' },
          ]),
        );
      await fixture.whenStable();

      expect(alerta()).toContain('Existem campos inválidos na requisição.');
      expect(elemento().querySelector('#email-erro')?.textContent).toContain('no máximo 180');

      digitar('#email', 'ana@curto.com');
      await fixture.whenStable();
      expect(elemento().querySelector('#email-erro')).toBeNull();
    });
  });

  describe('Google', () => {
    it('desenha o botão do Google com o Client ID, o modo e o tema', async () => {
      await abrir('criar', CLIENT_ID);

      expect(elemento().querySelector('.google')?.textContent).toContain('Criar conta com Google');
      expect(elemento().textContent).toContain('ou com e-mail');
      expect(desenharBotao).toHaveBeenCalledWith(
        elemento().querySelector('.google-oficial'),
        expect.objectContaining({
          clientId: CLIENT_ID,
          criarConta: true,
          escuro: TestBed.inject(Tema).escuro(),
        }),
      );
      expect(elemento().querySelector('.google-oficial.visivel')).not.toBeNull();
    });

    it('mostra "Abrindo o Google…", envia o token e entra', async () => {
      await abrir('entrar', CLIENT_ID);

      ultimoBotaoGoogle().aoClicar();
      await fixture.whenStable();
      expect(elemento().querySelector('.google')?.textContent).toContain('Abrindo o Google…');
      expect(elemento().querySelector<HTMLButtonElement>('.google')!.disabled).toBe(true);
      expect(botaoPrincipal().disabled).toBe(true);

      ultimoBotaoGoogle().aoReceberToken('id-token');
      const requisicao = http.expectOne('/api/auth/google');
      expect(requisicao.request.body).toEqual({ idToken: 'id-token' });
      requisicao.flush(tokenDeTeste());
      await fixture.whenStable();

      expect(router.navigateByUrl).toHaveBeenCalledWith('/');
    });

    it('recusa do Google mostra a mensagem da API, não a de senha errada', async () => {
      await abrir('entrar', CLIENT_ID);

      ultimoBotaoGoogle().aoReceberToken('id-token');
      http
        .expectOne('/api/auth/google')
        .flush(...erroDaApi(401, 'Sua conta Google ainda não tem o e-mail confirmado.'));
      await fixture.whenStable();

      expect(alerta()).toContain('Sua conta Google ainda não tem o e-mail confirmado.');
    });

    it('libera a tela quando a janela do Google é fechada sem escolher conta', async () => {
      await abrir('entrar', CLIENT_ID);
      vi.useFakeTimers();
      try {
        window.dispatchEvent(new Event('focus'));
        ultimoBotaoGoogle().aoClicar();
        TestBed.tick();

        window.dispatchEvent(new Event('focus'));
        vi.advanceTimersByTime(800);
        TestBed.tick();

        expect(botaoPrincipal().disabled).toBe(false);
        expect(elemento().querySelector('.google')?.textContent).toContain('Entrar com Google');
      } finally {
        vi.useRealTimers();
      }
    });

    it('quando o Google não carrega, o botão próprio explica e sugere o e-mail', async () => {
      desenharBotao.mockRejectedValue(new Error('sem rede'));
      await abrir('entrar', CLIENT_ID);

      expect(elemento().querySelector('.google-oficial.visivel')).toBeNull();
      elemento().querySelector<HTMLButtonElement>('.google')!.click();
      await fixture.whenStable();

      expect(alerta()).toContain('Não foi possível abrir o Google agora.');
    });

    it('sem resposta da configuração, a tela segue só com e-mail', async () => {
      fixture = TestBed.createComponent(Acesso);
      TestBed.tick();
      http.expectOne('/api/auth/configuracao').error(new ProgressEvent('error'), { status: 0 });
      await fixture.whenStable();

      expect(elemento().querySelector('.google')).toBeNull();
      expect(elemento().querySelector('#email')).not.toBeNull();
    });
  });
});
