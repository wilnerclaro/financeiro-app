import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { BotaoGoogle, GoogleIdentidade } from './google-identidade';

interface JanelaDeTeste {
  google?: unknown;
}

describe('GoogleIdentidade', () => {
  const initialize = vi.fn();
  const renderButton = vi.fn();
  let documento: Document;
  let servico: GoogleIdentidade;

  function janela(): JanelaDeTeste {
    return documento.defaultView as unknown as JanelaDeTeste;
  }

  function botao(mudancas: Partial<BotaoGoogle> = {}): BotaoGoogle {
    return {
      clientId: 'cliente.apps.googleusercontent.com',
      criarConta: false,
      escuro: true,
      aoClicar: vi.fn(),
      aoReceberToken: vi.fn(),
      ...mudancas,
    };
  }

  function scriptDoGoogle(): HTMLScriptElement | null {
    return documento.head.querySelector('script[src^="https://accounts.google.com/gsi/client"]');
  }

  beforeEach(() => {
    initialize.mockReset();
    renderButton.mockReset();
    documento = TestBed.inject(DOCUMENT);
    servico = TestBed.inject(GoogleIdentidade);
  });

  afterEach(() => {
    delete janela().google;
    scriptDoGoogle()?.remove();
  });

  it('carrega o script do Google uma vez, inicia com o Client ID e desenha o botão', async () => {
    const elemento = documento.createElement('div');
    const primeiro = botao();

    const desenho = servico.desenharBotao(elemento, primeiro);
    const script = scriptDoGoogle()!;
    expect(script.src).toContain('hl=pt-BR');
    janela().google = { accounts: { id: { initialize, renderButton } } };
    script.onload?.(new Event('load'));
    await desenho;

    expect(initialize).toHaveBeenCalledWith(
      expect.objectContaining({ client_id: primeiro.clientId, ux_mode: 'popup' }),
    );
    expect(renderButton).toHaveBeenCalledWith(
      elemento,
      expect.objectContaining({
        theme: 'filled_black',
        text: 'signin_with',
        width: 200,
        click_listener: primeiro.aoClicar,
      }),
    );

    const segundo = botao({ criarConta: true, escuro: false });
    await servico.desenharBotao(elemento, segundo);
    expect(initialize).toHaveBeenCalledTimes(1);
    expect(renderButton).toHaveBeenLastCalledWith(
      elemento,
      expect.objectContaining({ theme: 'outline', text: 'signup_with' }),
    );

    const callback = initialize.mock.calls[0][0].callback as (r: { credential: string }) => void;
    callback({ credential: 'id-token' });
    expect(segundo.aoReceberToken).toHaveBeenCalledWith('id-token');
    expect(primeiro.aoReceberToken).not.toHaveBeenCalled();
  });

  it('usa a biblioteca já carregada e inicia de novo quando o Client ID muda', async () => {
    janela().google = { accounts: { id: { initialize, renderButton } } };
    const elemento = documento.createElement('div');

    await servico.desenharBotao(elemento, botao());
    await servico.desenharBotao(elemento, botao({ clientId: 'outro' }));

    expect(scriptDoGoogle()).toBeNull();
    expect(initialize).toHaveBeenCalledTimes(2);
  });

  it('falha quando o script não carrega e tenta de novo na próxima vez', async () => {
    const elemento = documento.createElement('div');

    const primeira = servico.desenharBotao(elemento, botao());
    scriptDoGoogle()!.onerror?.(new Event('error'));
    await expect(primeira).rejects.toThrow('Não foi possível carregar o Google.');
    scriptDoGoogle()!.remove();

    const segunda = servico.desenharBotao(elemento, botao());
    scriptDoGoogle()!.onload?.(new Event('load'));
    await expect(segunda).rejects.toThrow('Não foi possível carregar o Google.');
  });
});
