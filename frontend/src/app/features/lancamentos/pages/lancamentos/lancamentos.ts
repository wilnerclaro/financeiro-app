import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ArrowLeftRight, EllipsisVertical, List, Plus, Search, Upload } from 'lucide';
import { ConfirmationService, MenuItem } from 'primeng/api';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Menu } from 'primeng/menu';
import { SelectButton } from 'primeng/selectbutton';
import { Skeleton } from 'primeng/skeleton';
import { forkJoin } from 'rxjs';
import { ImportacaoDeExtrato } from '../../../importar/state/importacao-de-extrato';
import { Lancamento, StatusLancamento, Transferencia } from '../../../../shared/api/api.models';
import { LancamentosApi } from '../../../../shared/api/lancamentos-api';
import { TransferenciasApi } from '../../../../shared/api/transferencias-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { EstadoVazio } from '../../../../shared/components/estado-vazio/estado-vazio';
import { Icone } from '../../../../shared/components/icone/icone';
import { SeletorMes } from '../../../../shared/components/seletor-mes/seletor-mes';
import { AGORA } from '../../../../shared/util/agora';
import { Avisos } from '../../../../shared/util/avisos';
import {
  MesDoAno,
  deDate,
  hojeIso,
  mesAtual,
  paraDate,
  primeiroDia,
  ultimoDia,
} from '../../../../shared/util/datas';
import {
  MESES_LONGOS,
  brl,
  brlComSinal,
  dataCompleta,
  dataCurta,
} from '../../../../shared/util/formatacao';
import { valorDe } from '../../../../shared/util/recurso';

export type FiltroDeLancamentos = 'todos' | 'apagar' | 'atrasados' | 'receitas' | 'despesas';

const FILTROS: { label: string; value: FiltroDeLancamentos }[] = [
  { label: 'Todos', value: 'todos' },
  { label: 'A pagar', value: 'apagar' },
  { label: 'Atrasados', value: 'atrasados' },
  { label: 'Receitas', value: 'receitas' },
  { label: 'Despesas', value: 'despesas' },
];

const STATUS: Record<StatusLancamento, { rotulo: string; cor: string }> = {
  PAGO: { rotulo: 'Pago', cor: 'ok' },
  PENDENTE: { rotulo: 'Pendente', cor: 'amb' },
  ATRASADO: { rotulo: 'Atrasado', cor: 'des' },
  CANCELADO: { rotulo: 'Cancelado', cor: '' },
};

interface DiaDeLancamentos {
  data: string;
  rotulo: string;
  itens: Lancamento[];
}

@Component({
  selector: 'app-lancamentos',
  imports: [
    FormsModule,
    RouterLink,
    Button,
    ConfirmDialog,
    DatePicker,
    Dialog,
    IconField,
    InputIcon,
    InputText,
    Menu,
    SelectButton,
    Skeleton,
    EstadoErro,
    EstadoVazio,
    Icone,
    SeletorMes,
  ],
  providers: [ConfirmationService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lancamentos.html',
  styleUrl: './lancamentos.scss',
})
export class Lancamentos {
  private readonly lancamentosApi = inject(LancamentosApi);
  private readonly transferenciasApi = inject(TransferenciasApi);
  private readonly confirmacao = inject(ConfirmationService);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);
  private readonly agora = inject(AGORA);
  protected readonly importacao = inject(ImportacaoDeExtrato);

  readonly filtro = input<FiltroDeLancamentos>();

  protected readonly icones = { ArrowLeftRight, EllipsisVertical, List, Plus, Search, Upload };
  protected readonly filtros = FILTROS;
  protected readonly status = STATUS;
  protected readonly brl = brl;
  protected readonly brlComSinal = brlComSinal;
  protected readonly dataCurta = dataCurta;

  protected readonly mes = signal<MesDoAno>(mesAtual(this.agora()));
  protected readonly filtroEscolhido = signal<FiltroDeLancamentos | null>(null);
  protected readonly busca = signal('');
  protected readonly acoes = signal<MenuItem[]>([]);
  protected readonly aPagar = signal<Lancamento | null>(null);
  protected readonly dataDoPagamento = signal<Date>(paraDate(hojeIso(this.agora())));
  protected readonly pagando = signal(false);
  protected readonly hoje = paraDate(hojeIso(this.agora()));

  protected readonly filtroAtual = computed(
    () => this.filtroEscolhido() ?? this.filtro() ?? 'todos',
  );
  protected readonly nomeDoMes = computed(() => MESES_LONGOS[this.mes().mes - 1]);

  protected readonly movimentos = rxResource({
    params: () => ({ mes: this.mes(), importacoes: this.importacao.concluidas() }),
    stream: ({ params: { mes } }) =>
      forkJoin({
        lancamentos: this.lancamentosApi.listar({
          dataInicio: primeiroDia(mes),
          dataFim: ultimoDia(mes),
          size: 500,
        }),
        transferencias: this.transferenciasApi.listar(primeiroDia(mes), ultimoDia(mes)),
      }),
  });

  protected readonly lancamentos = computed(
    () => valorDe(this.movimentos)?.lancamentos.content ?? [],
  );
  protected readonly transferencias = computed<Transferencia[]>(
    () => valorDe(this.movimentos)?.transferencias ?? [],
  );

  protected readonly filtrados = computed(() => {
    const termo = this.normalizar(this.busca());
    const filtro = this.filtroAtual();
    return this.lancamentos().filter(
      (l) =>
        this.passaNoFiltro(l, filtro) &&
        (!termo ||
          this.normalizar(`${l.descricao} ${l.nomeCategoria} ${l.nomeConta}`).includes(termo)),
    );
  });

  protected readonly dias = computed<DiaDeLancamentos[]>(() => {
    const porDia = new Map<string, Lancamento[]>();
    for (const l of this.filtrados()) {
      porDia.set(l.dataCompetencia, [...(porDia.get(l.dataCompetencia) ?? []), l]);
    }
    return [...porDia.entries()]
      .sort(([a], [b]) => b.localeCompare(a))
      .map(([data, itens]) => ({ data, rotulo: this.rotuloDoDia(data), itens }));
  });

  protected readonly totais = computed(() => {
    const validos = this.filtrados().filter((l) => l.status !== 'CANCELADO');
    const somar = (tipo: string) =>
      validos.filter((l) => l.tipo === tipo).reduce((total, l) => total + l.valor, 0);
    const receitas = somar('RECEITA');
    const despesas = somar('DESPESA');
    return { receitas, despesas, saldo: receitas - despesas };
  });

  protected escolherFiltro(filtro: FiltroDeLancamentos): void {
    this.filtroEscolhido.set(filtro);
  }

  protected tentarDeNovo(): void {
    this.movimentos.reload();
  }

  protected abrirAcoes(menu: Menu, evento: Event, lancamento: Lancamento): void {
    const aberto = lancamento.status === 'PENDENTE' || lancamento.status === 'ATRASADO';
    const itens: MenuItem[] = [];
    if (aberto) {
      itens.push({ label: 'Pagar', command: () => this.abrirPagamento(lancamento) });
    }
    if (lancamento.status !== 'CANCELADO') {
      itens.push(
        {
          label: 'Editar',
          command: () => void this.router.navigate(['/lancar', lancamento.id]),
        },
        { label: 'Cancelar lançamento', command: () => this.confirmarCancelamento(lancamento) },
      );
    }
    this.acoes.set(itens);
    menu.toggle(evento);
  }

  protected abrirPagamento(lancamento: Lancamento): void {
    this.dataDoPagamento.set(paraDate(hojeIso(this.agora())));
    this.aPagar.set(lancamento);
  }

  protected fecharPagamento(): void {
    this.aPagar.set(null);
  }

  protected confirmarPagamento(): void {
    const lancamento = this.aPagar();
    if (!lancamento || this.pagando()) {
      return;
    }
    this.pagando.set(true);
    this.lancamentosApi.pagar(lancamento.id, deDate(this.dataDoPagamento())).subscribe({
      next: () => {
        this.pagando.set(false);
        this.aPagar.set(null);
        this.avisos.sucesso('Pago', `${lancamento.descricao} · ${brl(lancamento.valor)}`);
        this.movimentos.reload();
      },
      error: (erro: unknown) => {
        this.pagando.set(false);
        this.avisos.erro('Não foi possível pagar', erro);
      },
    });
  }

  protected confirmarCancelamento(lancamento: Lancamento): void {
    this.confirmacao.confirm({
      header: 'Cancelar lançamento?',
      message: `${lancamento.descricao} · ${brl(lancamento.valor)}. ${
        lancamento.status === 'PAGO' ? 'O valor volta para o saldo da conta.' : ''
      }`,
      acceptLabel: 'Cancelar lançamento',
      rejectLabel: 'Manter',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.lancamentosApi.cancelar(lancamento.id).subscribe({
          next: () => {
            this.avisos.sucesso('Lançamento cancelado', lancamento.descricao);
            this.movimentos.reload();
          },
          error: (erro: unknown) => this.avisos.erro('Não foi possível cancelar', erro),
        }),
    });
  }

  protected confirmarCancelamentoDaTransferencia(transferencia: Transferencia): void {
    this.confirmacao.confirm({
      header: 'Cancelar transferência?',
      message: `${brl(transferencia.valor)} de ${transferencia.nomeContaOrigem} para ${transferencia.nomeContaDestino}. Os saldos das duas contas voltam ao que eram.`,
      acceptLabel: 'Cancelar transferência',
      rejectLabel: 'Manter',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.transferenciasApi.cancelar(transferencia.id).subscribe({
          next: () => {
            this.avisos.sucesso('Transferência cancelada', brl(transferencia.valor));
            this.movimentos.reload();
          },
          error: (erro: unknown) => this.avisos.erro('Não foi possível cancelar', erro),
        }),
    });
  }

  protected dataDaTransferencia(transferencia: Transferencia): string {
    return dataCompleta(transferencia.dataTransferencia);
  }

  private passaNoFiltro(lancamento: Lancamento, filtro: FiltroDeLancamentos): boolean {
    switch (filtro) {
      case 'apagar':
        return lancamento.status === 'PENDENTE' || lancamento.status === 'ATRASADO';
      case 'atrasados':
        return lancamento.status === 'ATRASADO';
      case 'receitas':
        return lancamento.tipo === 'RECEITA';
      case 'despesas':
        return lancamento.tipo === 'DESPESA';
      default:
        return true;
    }
  }

  private rotuloDoDia(data: string): string {
    const hoje = hojeIso(this.agora());
    if (data === hoje) {
      return 'Hoje';
    }
    const dia = paraDate(data);
    const semana = dia.toLocaleDateString('pt-BR', { weekday: 'long' });
    return `${semana.charAt(0).toUpperCase()}${semana.slice(1)}, ${dataCompleta(data)}`;
  }

  private normalizar(texto: string): string {
    return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
  }
}
