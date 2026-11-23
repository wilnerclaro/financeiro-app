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
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { of } from 'rxjs';
import { Conta } from '../../../../shared/api/api.models';
import { LancamentosApi } from '../../../../shared/api/lancamentos-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { SeletorMes } from '../../../../shared/components/seletor-mes/seletor-mes';
import { AGORA } from '../../../../shared/util/agora';
import { Avisos } from '../../../../shared/util/avisos';
import { MesDoAno, deDate, hojeIso, mesAtual, paraDate } from '../../../../shared/util/datas';
import { brl, brlComSinal, dataCompleta, dataCurta } from '../../../../shared/util/formatacao';
import { valorDe } from '../../../../shared/util/recurso';

@Component({
  selector: 'app-fatura-dialogo',
  imports: [FormsModule, Button, DatePicker, Dialog, Select, Skeleton, EstadoErro, SeletorMes],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './fatura-dialogo.html',
  styleUrl: './fatura-dialogo.scss',
})
export class FaturaDialogo {
  private readonly lancamentosApi = inject(LancamentosApi);
  private readonly avisos = inject(Avisos);
  private readonly agora = inject(AGORA);

  readonly cartao = input<Conta | null>(null);
  readonly contas = input<Conta[]>([]);
  readonly fechar = output();
  readonly paga = output();

  protected readonly brl = brl;
  protected readonly brlComSinal = brlComSinal;
  protected readonly dataCompleta = dataCompleta;
  protected readonly dataCurta = dataCurta;
  protected readonly hoje = paraDate(hojeIso(this.agora()));

  protected readonly mes = signal<MesDoAno>(mesAtual(this.agora()), {
    equal: (a, b) => a.ano === b.ano && a.mes === b.mes,
  });
  protected readonly contaPagamentoId = signal<string | null>(null);
  protected readonly dataPagamento = signal<Date>(this.hoje);
  protected readonly pagando = signal(false);
  protected readonly confirmando = signal(false);

  protected readonly fatura = rxResource({
    params: () => ({ cartao: this.cartao(), mes: this.mes() }),
    stream: ({ params }) =>
      params.cartao
        ? this.lancamentosApi.fatura(params.cartao.id, params.mes.ano, params.mes.mes)
        : of(null),
  });

  protected readonly opcoesDePagamento = computed(() =>
    this.contas()
      .filter((c) => c.ativa && c.tipo !== 'CARTAO_CREDITO')
      .map((c) => ({ label: `${c.nome} · ${brl(c.saldoAtual)}`, value: c.id })),
  );

  protected readonly pendentes = computed(() =>
    (valorDe(this.fatura)?.lancamentos ?? []).filter(
      (l) => l.status === 'PENDENTE' || l.status === 'ATRASADO',
    ),
  );

  protected readonly quitada = computed(() => {
    const fatura = valorDe(this.fatura);
    return !!fatura && fatura.lancamentos.length > 0 && this.pendentes().length === 0;
  });

  constructor() {
    effect(() => {
      const cartao = this.cartao();
      if (cartao) {
        untracked(() => this.abrir());
      }
    });
  }

  protected iniciarPagamento(): void {
    this.confirmando.set(true);
  }

  protected pagar(): void {
    const cartao = this.cartao();
    const contaPagamentoId = this.contaPagamentoId();
    if (!cartao || !contaPagamentoId || this.pagando()) {
      return;
    }
    this.pagando.set(true);
    this.lancamentosApi
      .pagarFatura({
        contaId: cartao.id,
        ano: this.mes().ano,
        mes: this.mes().mes,
        contaPagamentoId,
        dataPagamento: deDate(this.dataPagamento()),
      })
      .subscribe({
        next: (fatura) => {
          this.pagando.set(false);
          this.confirmando.set(false);
          this.avisos.sucesso('Fatura paga', `${fatura.nomeConta} · ${brl(fatura.total)}`);
          this.fatura.reload();
          this.paga.emit();
        },
        error: (erro: unknown) => {
          this.pagando.set(false);
          this.avisos.erro('Não foi possível pagar a fatura', erro);
        },
      });
  }

  private abrir(): void {
    this.mes.set(mesAtual(this.agora()));
    this.confirmando.set(false);
    this.dataPagamento.set(this.hoje);
    this.contaPagamentoId.set(this.opcoesDePagamento()[0]?.value ?? null);
  }
}
