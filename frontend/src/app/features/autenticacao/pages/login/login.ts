import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ChartLine, CircleAlert, LogIn, Moon, Sun } from 'lucide';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';
import { finalize } from 'rxjs';
import { destinoSeguro } from '../../../../core/auth/destino-seguro';
import { Sessao } from '../../../../core/auth/sessao';
import { Tema } from '../../../../core/config/tema';
import { mensagemDoErro } from '../../../../core/error-handling/erro-api';
import { Icone } from '../../../../shared/components/icone/icone';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, Button, Icone, InputText, Message, Password],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly sessao = inject(Sessao);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly tema = inject(Tema);
  protected readonly icones = { ChartLine, CircleAlert, LogIn, Moon, Sun };

  readonly voltar = input<string>();

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });

  protected entrar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    if (this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);

    this.sessao
      .entrar(this.formulario.getRawValue())
      .pipe(
        finalize(() => this.enviando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => void this.router.navigateByUrl(destinoSeguro(this.voltar())),
        error: (erro: unknown) => this.erro.set(mensagemDoErro(erro)),
      });
  }

  protected campoInvalido(campo: 'email' | 'senha'): boolean {
    const controle = this.formulario.controls[campo];
    return controle.invalid && controle.touched;
  }
}
