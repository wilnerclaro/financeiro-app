import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ArrowRight, ChartLine, Check, Circle, CircleAlert, Moon, ShieldCheck, Sun } from 'lucide';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';
import { Observable, finalize } from 'rxjs';
import { AutenticacaoApi } from '../../../../core/auth/autenticacao-api';
import { UsuarioResponse } from '../../../../core/auth/autenticacao.models';
import { destinoSeguro } from '../../../../core/auth/destino-seguro';
import { GoogleIdentidade } from '../../../../core/auth/google-identidade';
import { Sessao } from '../../../../core/auth/sessao';
import { Tema } from '../../../../core/config/tema';
import { ApiErrorResponse, mensagemDoErro } from '../../../../core/error-handling/erro-api';
import { Icone } from '../../../../shared/components/icone/icone';
import { PainelAcesso } from '../../components/painel-acesso/painel-acesso';

export type ModoDeAcesso = 'entrar' | 'criar';
type Envio = 'parado' | 'enviando' | 'google';
type Campo = 'nome' | 'email' | 'senha';
type OrigemDoPedido = 'formulario' | 'google';
type ErroDeAcesso =
  { tipo: 'credenciais' } | { tipo: 'emailUsado' } | { tipo: 'mensagem'; texto: string };

const SENHA_COM_LETRA_E_NUMERO = /^(?=.*\p{L})(?=.*\d).*$/u;
const ESPERA_PARA_LIBERAR_GOOGLE_MS = 800;

@Component({
  selector: 'app-acesso',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    Icone,
    InputText,
    Message,
    PainelAcesso,
    Password,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './acesso.html',
  styleUrl: './acesso.scss',
  host: { '(window:focus)': 'voltouDoGoogle()' },
})
export class Acesso {
  private readonly sessao = inject(Sessao);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly google = inject(GoogleIdentidade);
  protected readonly tema = inject(Tema);
  protected readonly icones = {
    ArrowRight,
    ChartLine,
    Check,
    Circle,
    CircleAlert,
    Moon,
    ShieldCheck,
    Sun,
  };

  readonly modo = input<ModoDeAcesso>('entrar');
  readonly voltar = input<string>();

  protected readonly criar = computed(() => this.modo() === 'criar');
  protected readonly envio = signal<Envio>('parado');
  protected readonly ocupado = computed(() => this.envio() !== 'parado');
  protected readonly erro = signal<ErroDeAcesso | null>(null);
  protected readonly errosDoServidor = signal<Partial<Record<Campo, string>>>({});
  protected readonly googleClientId = signal<string | null | undefined>(undefined);
  protected readonly googleDesenhado = signal(false);
  private readonly caixaGoogle = viewChild<ElementRef<HTMLElement>>('caixaGoogle');

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nome: [''],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });

  private readonly senha = toSignal(this.formulario.controls.senha.valueChanges, {
    initialValue: '',
  });
  protected readonly regras = computed(() => [
    { texto: '8 caracteres ou mais', ok: this.senha().length >= 8 },
    { texto: 'Uma letra', ok: /\p{L}/u.test(this.senha()) },
    { texto: 'Um número', ok: /\d/.test(this.senha()) },
  ]);

  constructor() {
    inject(AutenticacaoApi)
      .configuracao()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (configuracao) => this.googleClientId.set(configuracao.googleClientId),
        error: () => this.googleClientId.set(null),
      });

    this.formulario.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.errosDoServidor.set({}));

    effect(() => this.ajustarValidacoes(this.criar()));
    effect(() => {
      if (this.ocupado()) {
        this.formulario.disable({ emitEvent: false });
      } else {
        this.formulario.enable({ emitEvent: false });
      }
    });
    effect(() => this.desenharGoogle());
  }

  protected enviar(): void {
    if (this.ocupado()) {
      return;
    }
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { nome, email, senha } = this.formulario.getRawValue();
    this.acompanhar(
      this.criar()
        ? this.sessao.cadastrar({ nome, email, senha })
        : this.sessao.entrar({ email, senha }),
      'formulario',
    );
  }

  protected googleIndisponivel(): void {
    this.erro.set({
      tipo: 'mensagem',
      texto: 'Não foi possível abrir o Google agora. Tente de novo ou use seu e-mail.',
    });
  }

  protected voltouDoGoogle(): void {
    if (this.envio() !== 'google') {
      return;
    }
    setTimeout(() => {
      if (this.envio() === 'google') {
        this.envio.set('parado');
      }
    }, ESPERA_PARA_LIBERAR_GOOGLE_MS);
  }

  protected campoInvalido(campo: Campo): boolean {
    const controle = this.formulario.controls[campo];
    const erro = this.erro();
    return (
      (controle.invalid && controle.touched) ||
      !!this.errosDoServidor()[campo] ||
      (erro?.tipo === 'credenciais' && campo !== 'nome') ||
      (erro?.tipo === 'emailUsado' && campo === 'email')
    );
  }

  protected mensagemDoCampo(campo: Campo): string | null {
    const doServidor = this.errosDoServidor()[campo];
    if (doServidor) {
      return doServidor;
    }
    const controle = this.formulario.controls[campo];
    if (!controle.invalid || !controle.touched) {
      return null;
    }
    if (campo === 'nome') {
      return 'Informe seu nome.';
    }
    if (campo === 'email') {
      return controle.hasError('required') ? 'Informe seu e-mail.' : 'Informe um e-mail válido.';
    }
    if (!this.criar()) {
      return 'Informe sua senha.';
    }
    return controle.hasError('required')
      ? 'Crie uma senha.'
      : 'A senha precisa seguir as regras abaixo.';
  }

  private acompanhar(pedido: Observable<UsuarioResponse>, origem: OrigemDoPedido): void {
    this.envio.set('enviando');
    this.erro.set(null);
    this.errosDoServidor.set({});
    pedido
      .pipe(
        finalize(() => this.envio.set('parado')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => void this.router.navigateByUrl(destinoSeguro(this.voltar())),
        error: (erro: unknown) => this.mostrarErro(erro, origem),
      });
  }

  private mostrarErro(erro: unknown, origem: OrigemDoPedido): void {
    const status = erro instanceof HttpErrorResponse ? erro.status : null;
    if (status === 401 && origem === 'formulario' && !this.criar()) {
      this.erro.set({ tipo: 'credenciais' });
      return;
    }
    if (status === 409 && this.criar()) {
      this.erro.set({ tipo: 'emailUsado' });
      return;
    }
    const corpo =
      erro instanceof HttpErrorResponse ? (erro.error as Partial<ApiErrorResponse>) : null;
    const porCampo = Object.fromEntries(
      (corpo?.fieldErrors ?? []).map((campo) => [campo.field, campo.message]),
    );
    this.errosDoServidor.set(porCampo);
    this.erro.set({ tipo: 'mensagem', texto: mensagemDoErro(erro) });
  }

  private ajustarValidacoes(criar: boolean): void {
    const { nome, senha } = this.formulario.controls;
    nome.setValidators(criar ? [Validators.required] : []);
    senha.setValidators(
      criar
        ? [
            Validators.required,
            Validators.minLength(8),
            Validators.pattern(SENHA_COM_LETRA_E_NUMERO),
          ]
        : [Validators.required],
    );
    nome.updateValueAndValidity({ emitEvent: false });
    senha.updateValueAndValidity({ emitEvent: false });
  }

  private desenharGoogle(): void {
    const clientId = this.googleClientId();
    const caixa = this.caixaGoogle()?.nativeElement;
    const criarConta = this.criar();
    const escuro = this.tema.escuro();
    if (!clientId || !caixa) {
      return;
    }
    this.google
      .desenharBotao(caixa, {
        clientId,
        criarConta,
        escuro,
        aoClicar: () => {
          this.erro.set(null);
          this.envio.set('google');
        },
        aoReceberToken: (idToken) =>
          this.acompanhar(this.sessao.entrarComGoogle(idToken), 'google'),
      })
      .then(
        () => this.googleDesenhado.set(true),
        () => this.googleDesenhado.set(false),
      );
  }
}
