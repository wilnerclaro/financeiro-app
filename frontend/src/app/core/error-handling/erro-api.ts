import { HttpErrorResponse } from '@angular/common/http';

export interface ErroDeCampo {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: ErroDeCampo[] | null;
}

export const MENSAGEM_SEM_CONEXAO =
  'Não foi possível falar com o servidor. Confira sua conexão e tente de novo.';
export const MENSAGEM_ERRO_INESPERADO = 'Algo deu errado. Tente de novo em instantes.';

export function mensagemDoErro(erro: unknown): string {
  if (!(erro instanceof HttpErrorResponse)) {
    return MENSAGEM_ERRO_INESPERADO;
  }
  if (erro.status === 0) {
    return MENSAGEM_SEM_CONEXAO;
  }
  const corpo = erro.error as Partial<ApiErrorResponse> | null;
  return corpo?.message || MENSAGEM_ERRO_INESPERADO;
}
