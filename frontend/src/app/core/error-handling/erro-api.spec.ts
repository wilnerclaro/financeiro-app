import { HttpErrorResponse } from '@angular/common/http';
import { MENSAGEM_ERRO_INESPERADO, MENSAGEM_SEM_CONEXAO, mensagemDoErro } from './erro-api';

describe('mensagemDoErro', () => {
  it('usa a mensagem do envelope da API', () => {
    const erro = new HttpErrorResponse({
      status: 401,
      error: { status: 401, message: 'E-mail ou senha inválidos.', fieldErrors: null },
    });
    expect(mensagemDoErro(erro)).toBe('E-mail ou senha inválidos.');
  });

  it('avisa quando o servidor não respondeu', () => {
    expect(mensagemDoErro(new HttpErrorResponse({ status: 0 }))).toBe(MENSAGEM_SEM_CONEXAO);
  });

  it('usa a mensagem genérica quando a resposta não tem envelope', () => {
    expect(mensagemDoErro(new HttpErrorResponse({ status: 502, error: '<html>' }))).toBe(
      MENSAGEM_ERRO_INESPERADO,
    );
    expect(mensagemDoErro(new HttpErrorResponse({ status: 500, error: null }))).toBe(
      MENSAGEM_ERRO_INESPERADO,
    );
  });

  it('usa a mensagem genérica para erro que não veio do HTTP', () => {
    expect(mensagemDoErro(new Error('falhou'))).toBe(MENSAGEM_ERRO_INESPERADO);
  });
});
