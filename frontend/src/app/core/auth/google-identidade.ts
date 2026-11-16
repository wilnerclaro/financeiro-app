import { DOCUMENT, Injectable, inject } from '@angular/core';

const SCRIPT_DO_GOOGLE = 'https://accounts.google.com/gsi/client?hl=pt-BR';
const LARGURA_MINIMA = 200;
const LARGURA_MAXIMA = 400;

interface RespostaDoGoogle {
  credential: string;
}

interface ApiDoGoogle {
  initialize(configuracao: {
    client_id: string;
    callback: (resposta: RespostaDoGoogle) => void;
    ux_mode: 'popup';
  }): void;
  renderButton(elemento: HTMLElement, opcoes: Record<string, unknown>): void;
}

interface JanelaComGoogle {
  google?: { accounts?: { id?: ApiDoGoogle } };
}

export interface BotaoGoogle {
  clientId: string;
  criarConta: boolean;
  escuro: boolean;
  aoClicar: () => void;
  aoReceberToken: (idToken: string) => void;
}

@Injectable({ providedIn: 'root' })
export class GoogleIdentidade {
  private readonly documento = inject(DOCUMENT);
  private carregamento: Promise<ApiDoGoogle> | null = null;
  private clientIdIniciado: string | null = null;
  private aoReceberToken: (idToken: string) => void = () => undefined;

  async desenharBotao(elemento: HTMLElement, botao: BotaoGoogle): Promise<void> {
    const api = await this.carregar();
    this.aoReceberToken = botao.aoReceberToken;
    if (this.clientIdIniciado !== botao.clientId) {
      api.initialize({
        client_id: botao.clientId,
        callback: (resposta) => this.aoReceberToken(resposta.credential),
        ux_mode: 'popup',
      });
      this.clientIdIniciado = botao.clientId;
    }
    elemento.replaceChildren();
    api.renderButton(elemento, {
      type: 'standard',
      theme: botao.escuro ? 'filled_black' : 'outline',
      size: 'large',
      text: botao.criarConta ? 'signup_with' : 'signin_with',
      shape: 'rectangular',
      logo_alignment: 'center',
      locale: 'pt-BR',
      width: Math.min(LARGURA_MAXIMA, Math.max(LARGURA_MINIMA, elemento.clientWidth)),
      click_listener: botao.aoClicar,
    });
  }

  private carregar(): Promise<ApiDoGoogle> {
    this.carregamento ??= new Promise<ApiDoGoogle>((resolver, rejeitar) => {
      const janela = this.documento.defaultView as JanelaComGoogle | null;
      const jaCarregada = janela?.google?.accounts?.id;
      if (jaCarregada) {
        resolver(jaCarregada);
        return;
      }
      const script = this.documento.createElement('script');
      script.src = SCRIPT_DO_GOOGLE;
      script.async = true;
      script.onload = () => {
        const api = janela?.google?.accounts?.id;
        if (api) {
          resolver(api);
        } else {
          this.falhar(rejeitar);
        }
      };
      script.onerror = () => this.falhar(rejeitar);
      this.documento.head.appendChild(script);
    });
    return this.carregamento;
  }

  private falhar(rejeitar: (erro: Error) => void): void {
    this.carregamento = null;
    rejeitar(new Error('Não foi possível carregar o Google.'));
  }
}
