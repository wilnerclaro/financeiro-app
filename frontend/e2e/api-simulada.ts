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

export async function simularApiDeAutenticacao(page: Page): Promise<void> {
  let versao = 0;

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
