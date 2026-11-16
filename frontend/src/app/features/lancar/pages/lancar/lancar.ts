import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ChevronDown, ChevronUp, CreditCard } from 'lucide';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { Skeleton } from 'primeng/skeleton';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import {
  Categoria,
  Conta,
  Inicio,
  Lancamento,
  LancamentoRequest,
  TipoLancamento,
  TransferenciaRequest,
} from '../../../../shared/api/api.models';
import { CategoriasApi } from '../../../../shared/api/categorias-api';
import { ContasApi } from '../../../../shared/api/contas-api';
import { InicioApi } from '../../../../shared/api/inicio-api';
import { LancamentosApi } from '../../../../shared/api/lancamentos-api';
import { TransferenciasApi } from '../../../../shared/api/transferencias-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { Icone } from '../../../../shared/components/icone/icone';
import { AGORA } from '../../../../shared/util/agora';
import { Avisos } from '../../../../shared/util/avisos';
import { deDate, hojeIso, paraDate } from '../../../../shared/util/datas';
import { brl } from '../../../../shared/util/formatacao';
import { valorDe } from '../../../../shared/util/recurso';

type TipoDoFormulario = TipoLancamento | 'TRANSFERENCIA';

interface DadosDoFormulario {
  contas: Conta[];
  categorias: Categoria[];
  rapido: Inicio['lancamentoRapido'] | null;
  lancamento: Lancamento | null;
}

interface Opcao {
  label: string;
  value: string;
}

const TIPOS: { label: string; value: TipoDoFormulario }[] = [
  { label: 'Despesa', value: 'DESPESA' },
  { label: 'Receita', value: 'RECEITA' },
  { label: 'Transferência', value: 'TRANSFERENCIA' },
];

@Component({
  selector: 'app-lancar',
  imports: [
    FormsModule,
    RouterLink,
    Button,
    DatePicker,
    InputNumber,
    InputText,
    Select,
    SelectButton,
    Skeleton,
    Textarea,
    ToggleSwitch,
    EstadoErro,
    Icone,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lancar.html',
  styleUrl: './lancar.scss',
})
export class Lancar {
  private readonly lancamentosApi = inject(LancamentosApi);
  private readonly transferenciasApi = inject(TransferenciasApi);
  private readonly contasApi = inject(ContasApi);
  private readonly categoriasApi = inject(CategoriasApi);
  private readonly inicioApi = inject(InicioApi);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);
  private readonly agora = inject(AGORA);

  readonly id = input<string>();

  protected readonly icones = { ChevronDown, ChevronUp, CreditCard };
  protected readonly tipos = TIPOS;

  protected readonly tipo = signal<TipoDoFormulario>('DESPESA');
  protected readonly valor = signal<number | null>(null);
  protected readonly categoriaId = signal<string | null>(null);
  protected readonly contaId = signal<string | null>(null);
  protected readonly contaDestinoId = signal<string | null>(null);
  protected readonly data = signal<Date>(paraDate(hojeIso(this.agora())));
  protected readonly pago = signal(true);
  protected readonly descricao = signal('');
  protected readonly vencimento = signal<Date | null>(null);
  protected readonly observacao = signal('');
  protected readonly maisDetalhes = signal(false);
  protected readonly salvando = signal(false);
  protected readonly tentouSalvar = signal(false);

  protected readonly base = rxResource({
    params: () => ({ id: this.id() }),
    stream: ({ params }) => this.carregar(params.id),
  });

  protected readonly editando = computed(() => !!this.id());
  protected readonly ehTransferencia = computed(() => this.tipo() === 'TRANSFERENCIA');
  protected readonly contas = computed(() => valorDe(this.base)?.contas ?? []);
  protected readonly opcoesDeConta = computed<Opcao[]>(() =>
    this.contas().map((c) => ({ label: c.nome, value: c.id })),
  );
  protected readonly opcoesDeDestino = computed<Opcao[]>(() =>
    this.opcoesDeConta().filter((o) => o.value !== this.contaId()),
  );
  protected readonly contaSelecionada = computed(() =>
    this.contas().find((c) => c.id === this.contaId()),
  );
  protected readonly ehCartao = computed(
    () => !this.ehTransferencia() && this.contaSelecionada()?.tipo === 'CARTAO_CREDITO',
  );
  protected readonly dataFutura = computed(() => deDate(this.data()) > hojeIso(this.agora()));
  protected readonly podeMarcarPago = computed(() => !this.ehCartao() && !this.dataFutura());
  protected readonly ficaPago = computed(() => this.podeMarcarPago() && this.pago());

  protected readonly opcoesDeCategoria = computed<Opcao[]>(() => {
    const tipo = this.tipo();
    return (valorDe(this.base)?.categorias ?? [])
      .filter((c) => c.tipo === tipo)
      .map((c) => ({
        label: c.nomeCategoriaPai ? `${c.nomeCategoriaPai} › ${c.nome}` : c.nome,
        value: c.id,
      }))
      .sort((a, b) => a.label.localeCompare(b.label, 'pt-BR'));
  });

  protected readonly sugestoes = computed<Opcao[]>(() => {
    const disponiveis = new Set(this.opcoesDeCategoria().map((o) => o.value));
    return (valorDe(this.base)?.rapido?.categoriasMaisUsadas ?? [])
      .filter((c) => disponiveis.has(c.categoriaId))
      .map((c) => ({ label: c.nome, value: c.categoriaId }));
  });

  protected readonly nomeDaCategoria = computed(() => {
    const categoria = valorDe(this.base)?.categorias.find((c) => c.id === this.categoriaId());
    return categoria?.nome ?? '';
  });

  protected readonly erros = computed(() => ({
    valor: !this.valor() || this.valor()! <= 0 ? 'Informe um valor maior que zero.' : '',
    categoria: !this.ehTransferencia() && !this.categoriaId() ? 'Escolha uma categoria.' : '',
    conta: !this.contaId() ? 'Escolha uma conta.' : '',
    destino: this.ehTransferencia() && !this.contaDestinoId() ? 'Escolha a conta de destino.' : '',
  }));

  protected readonly valido = computed(() => Object.values(this.erros()).every((e) => !e));

  protected readonly rotuloDoPago = computed(() =>
    this.tipo() === 'RECEITA' ? 'Já recebi' : 'Já paguei',
  );

  constructor() {
    effect(() => {
      const dados = valorDe(this.base);
      if (dados) {
        untracked(() => this.preencher(dados));
      }
    });
  }

  protected escolherTipo(tipo: TipoDoFormulario): void {
    if (tipo === this.tipo()) {
      return;
    }
    this.tipo.set(tipo);
    this.categoriaId.set(null);
    this.contaDestinoId.set(null);
  }

  protected escolherConta(contaId: string | null): void {
    this.contaId.set(contaId);
    if (this.contaDestinoId() === contaId) {
      this.contaDestinoId.set(null);
    }
  }

  protected tentarDeNovo(): void {
    this.base.reload();
  }

  protected salvar(fazerOutro: boolean): void {
    this.tentouSalvar.set(true);
    if (!this.valido() || this.salvando()) {
      return;
    }
    this.salvando.set(true);
    const descricao = this.descricaoFinal();
    const valor = this.valor()!;
    const pedido: Observable<unknown> = this.ehTransferencia()
      ? this.transferenciasApi.criar(this.pedidoDeTransferencia())
      : this.editando()
        ? this.lancamentosApi.atualizar(this.id()!, this.pedidoDeLancamento())
        : this.lancamentosApi.criar(this.pedidoDeLancamento());

    pedido.subscribe({
      next: () => {
        this.salvando.set(false);
        this.avisos.sucesso(
          this.editando() ? 'Lançamento atualizado' : 'Lançamento salvo',
          `${descricao} · ${brl(valor)}`,
        );
        if (fazerOutro) {
          this.limparParaOutro();
        } else {
          void this.router.navigateByUrl('/lancamentos');
        }
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.avisos.erro('Não foi possível salvar', erro);
      },
    });
  }

  private carregar(id: string | undefined): Observable<DadosDoFormulario> {
    return forkJoin({
      contas: this.contasApi.listar(true),
      categorias: this.categoriasApi.listar(true),
      rapido: id
        ? of(null)
        : this.inicioApi.buscar().pipe(
            catchError(() => of(null)),
            map((inicio) => inicio?.lancamentoRapido ?? null),
          ),
      lancamento: id ? this.lancamentosApi.buscar(id) : of(null),
    });
  }

  private preencher(dados: DadosDoFormulario): void {
    const lancamento = dados.lancamento;
    if (lancamento) {
      this.tipo.set(lancamento.tipo);
      this.valor.set(lancamento.valor);
      this.categoriaId.set(lancamento.categoriaId);
      this.contaId.set(lancamento.contaId);
      this.data.set(paraDate(lancamento.dataCompra ?? lancamento.dataCompetencia));
      this.pago.set(lancamento.status === 'PAGO');
      this.descricao.set(lancamento.descricao);
      this.vencimento.set(lancamento.dataVencimento ? paraDate(lancamento.dataVencimento) : null);
      this.observacao.set(lancamento.observacao ?? '');
      this.maisDetalhes.set(true);
      return;
    }
    if (!this.contaId()) {
      const sugerida = dados.contas.find((c) => c.id === dados.rapido?.contaId);
      this.contaId.set((sugerida ?? dados.contas[0])?.id ?? null);
    }
  }

  private descricaoFinal(): string {
    const digitada = this.descricao().trim();
    if (digitada) {
      return digitada;
    }
    if (this.ehTransferencia()) {
      const destino = this.contas().find((c) => c.id === this.contaDestinoId());
      return `Transferência para ${destino?.nome ?? 'outra conta'}`;
    }
    return this.nomeDaCategoria();
  }

  private pedidoDeLancamento(): LancamentoRequest {
    const data = deDate(this.data());
    const vencimento = this.vencimento();
    return {
      contaId: this.contaId()!,
      categoriaId: this.categoriaId()!,
      descricao: this.descricaoFinal(),
      tipo: this.tipo() as TipoLancamento,
      valor: this.valor()!,
      dataCompetencia: data,
      dataVencimento: vencimento ? deDate(vencimento) : data,
      dataPagamento: this.ficaPago() ? data : null,
      observacao: this.observacao().trim() || null,
    };
  }

  private pedidoDeTransferencia(): TransferenciaRequest {
    return {
      contaOrigemId: this.contaId()!,
      contaDestinoId: this.contaDestinoId()!,
      valor: this.valor()!,
      dataTransferencia: deDate(this.data()),
      descricao: this.descricaoFinal(),
    };
  }

  private limparParaOutro(): void {
    this.valor.set(null);
    this.categoriaId.set(null);
    this.descricao.set('');
    this.vencimento.set(null);
    this.observacao.set('');
    this.tentouSalvar.set(false);
  }
}
