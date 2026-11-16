import { TokenResponse } from '../app/core/auth/autenticacao.models';

export function tokenDeTeste(versao = 1): TokenResponse {
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

export const CHAVE_REFRESH_TOKEN = 'financeiro.refreshToken';
