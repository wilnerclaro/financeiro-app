import { Page } from '@playwright/test';

export const SENHA_CERTA = 'segredo123';

function token(versao: number) {
  return {
    accessToken: `acesso-${versao}`,
    refreshToken: `refresh-${versao}`,
    tipo: 'Bearer',
    expiraEm: 900,
    usuario: {
      id: 'b3c1f0a2-0000-4000-8000-000000000001',
      nome: 'Ana Souza',
      email: 'ana@exemplo.com',
    },
  };
}

function naoAutenticado(message: string, path: string) {
  return {
    status: 401,
    json: {
      timestamp: '2026-11-14T10:00:00-03:00',
      status: 401,
      error: 'Unauthorized',
      message,
      path,
      fieldErrors: null,
    },
  };
}

export const EMAIL_JA_USADO = 'usada@exemplo.com';
export const TOKEN_DO_GOOGLE = 'token-do-google';

const GOOGLE_SIMULADO = `
window.google = { accounts: { id: {
  initialize(configuracao) { window.__google = configuracao; },
  renderButton(elemento, opcoes) {
    const botao = document.createElement('button');
    botao.type = 'button';
    botao.textContent = opcoes.text === 'signup_with' ? 'Google simulado: cadastrar' : 'Google simulado: entrar';
    botao.style.cssText = 'width:100%;height:40px';
    botao.onclick = () => {
      opcoes.click_listener();
      setTimeout(() => window.__google.callback({ credential: '${TOKEN_DO_GOOGLE}' }), 300);
    };
    elemento.appendChild(botao);
  },
} } };
`;

export async function simularGoogle(page: Page): Promise<void> {
  await page.route('**/api/auth/configuracao', (rota) =>
    rota.fulfill({ json: { googleClientId: 'cliente.apps.googleusercontent.com' } }),
  );
  await page.route('https://accounts.google.com/gsi/client**', (rota) =>
    rota.fulfill({ contentType: 'application/javascript', body: GOOGLE_SIMULADO }),
  );
}

export async function simularApiDeAutenticacao(page: Page): Promise<void> {
  let versao = 0;

  await page.route('**/api/configuracao-inicial', (rota) =>
    rota.fulfill({
      json: {
        rendaPassivaDesejada: 6000,
        etapa: 'CONCLUIDO',
        pausado: false,
        ano: 2026,
        mes: 11,
      },
    }),
  );

  await page.route('**/api/auth/configuracao', (rota) =>
    rota.fulfill({ json: { googleClientId: null } }),
  );

  await page.route('**/api/auth/cadastro', async (rota) => {
    const { email } = rota.request().postDataJSON() as { email: string };
    if (email === EMAIL_JA_USADO) {
      await rota.fulfill({
        status: 409,
        json: {
          timestamp: '2026-11-14T10:00:00-03:00',
          status: 409,
          error: 'Conflict',
          message: 'Já existe uma conta com este e-mail.',
          path: '/api/auth/cadastro',
          fieldErrors: [],
        },
      });
    } else {
      await rota.fulfill({ status: 201, json: token(++versao) });
    }
  });

  await page.route('**/api/auth/google', async (rota) => {
    const { idToken } = rota.request().postDataJSON() as { idToken: string };
    if (idToken === TOKEN_DO_GOOGLE) {
      await rota.fulfill({ json: token(++versao) });
    } else {
      await rota.fulfill(naoAutenticado('Token do Google recusado.', '/api/auth/google'));
    }
  });

  await page.route('**/api/auth/login', async (rota) => {
    const { senha } = rota.request().postDataJSON() as { senha: string };
    if (senha === SENHA_CERTA) {
      await rota.fulfill({ json: token(++versao) });
    } else {
      await rota.fulfill(naoAutenticado('E-mail ou senha inválidos.', '/api/auth/login'));
    }
  });

  await page.route('**/api/auth/refresh', async (rota) => {
    const { refreshToken } = rota.request().postDataJSON() as { refreshToken: string };
    if (refreshToken === `refresh-${versao}`) {
      await rota.fulfill({ json: token(++versao) });
    } else {
      await rota.fulfill(
        naoAutenticado('Refresh token inválido ou expirado.', '/api/auth/refresh'),
      );
    }
  });
}
