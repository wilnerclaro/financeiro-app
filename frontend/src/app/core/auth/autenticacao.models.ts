export interface UsuarioResponse {
  id: string;
  nome: string;
  email: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tipo: 'Bearer';
  expiraEm: number;
  usuario: UsuarioResponse;
}
