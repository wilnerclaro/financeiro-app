import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Observable } from 'rxjs';
import { Conta, TipoConta } from '../../../../shared/api/api.models';
import { ContasApi } from '../../../../shared/api/contas-api';
import { Avisos } from '../../../../shared/util/avisos';
import { TIPOS_DE_CONTA } from '../../tipos-de-conta';

export type ModoDoDialogo = 'criar' | 'editar' | 'saldo';

@Component({
  selector: 'app-conta-dialogo',
  imports: [FormsModule, Button, Dialog, InputNumber, InputText, Select],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './conta-dialogo.html',
  styles: `
    .corpo {
      display: flex;
      flex-direction: column;
      gap: var(--e4);
    }

    .duas {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--e3);
    }

    .erro {
      color: var(--des);
    }
  `,
})
export class ContaDialogo {
  private readonly contasApi = inject(ContasApi);
  private readonly avisos = inject(Avisos);

  readonly modo = input<ModoDoDialogo | null>(null);
  readonly conta = input<Conta | null>(null);
  readonly fechar = output();
  readonly salvo = output<Conta>();

  protected readonly tipos = TIPOS_DE_CONTA;
  protected readonly nome = signal('');
  protected readonly tipo = signal<TipoConta>('CONTA_CORRENTE');
  protected readonly saldoInicial = signal<number | null>(0);
  protected readonly diaFechamento = signal<number | null>(null);
  protected readonly diaVencimento = signal<number | null>(null);
  protected readonly salvando = signal(false);
  protected readonly tentouSalvar = signal(false);

  protected readonly titulo = computed(() => {
    switch (this.modo()) {
      case 'editar':
        return 'Editar conta';
      case 'saldo':
        return 'Corrigir saldo inicial';
      default:
        return 'Nova conta';
    }
  });
  protected readonly ehCartao = computed(() => this.tipo() === 'CARTAO_CREDITO');
  protected readonly erros = computed(() => ({
    nome: this.modo() !== 'saldo' && !this.nome().trim() ? 'Informe o nome da conta.' : '',
    saldo:
      this.modo() !== 'editar' && this.saldoInicial() === null ? 'Informe o saldo inicial.' : '',
    dias:
      this.modo() !== 'saldo' && this.ehCartao() && (!this.diaFechamento() || !this.diaVencimento())
        ? 'Cartão de crédito precisa do dia de fechamento e do dia de vencimento.'
        : '',
  }));

  constructor() {
    effect(() => {
      const modo = this.modo();
      const conta = this.conta();
      untracked(() => this.preencher(modo, conta));
    });
  }

  protected salvar(): void {
    this.tentouSalvar.set(true);
    if (Object.values(this.erros()).some(Boolean) || this.salvando()) {
      return;
    }
    this.salvando.set(true);
    this.pedido().subscribe({
      next: (conta) => {
        this.salvando.set(false);
        this.avisos.sucesso(
          this.modo() === 'criar' ? 'Conta criada' : 'Conta atualizada',
          conta.nome,
        );
        this.salvo.emit(conta);
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.avisos.erro('Não foi possível salvar a conta', erro);
      },
    });
  }

  private pedido(): Observable<Conta> {
    const conta = this.conta();
    const dados = {
      nome: this.nome().trim(),
      tipo: this.tipo(),
      diaFechamento: this.ehCartao() ? this.diaFechamento() : null,
      diaVencimento: this.ehCartao() ? this.diaVencimento() : null,
    };
    if (this.modo() === 'saldo' && conta) {
      return this.contasApi.corrigirSaldoInicial(conta.id, this.saldoInicial() ?? 0);
    }
    if (this.modo() === 'editar' && conta) {
      return this.contasApi.atualizar(conta.id, dados);
    }
    return this.contasApi.criar({ ...dados, saldoInicial: this.saldoInicial() ?? 0 });
  }

  private preencher(modo: ModoDoDialogo | null, conta: Conta | null): void {
    this.tentouSalvar.set(false);
    this.nome.set(conta?.nome ?? '');
    this.tipo.set(conta?.tipo ?? 'CONTA_CORRENTE');
    this.saldoInicial.set(conta?.saldoInicial ?? 0);
    this.diaFechamento.set(conta?.diaFechamento ?? null);
    this.diaVencimento.set(conta?.diaVencimento ?? null);
    if (modo === 'criar') {
      this.nome.set('');
    }
  }
}
