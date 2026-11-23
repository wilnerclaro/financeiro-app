import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ChevronDown, ChevronUp, CreditCard, Plus } from 'lucide';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { Skeleton } from 'primeng/skeleton';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import {
  Categoria,
  Conta,
  Inicio,
  Lancamento,
  LancamentoRequest,
  TipoCategoria,
  TipoLancamento,
  TransferenciaRequest,
} from '../../../../shared/api/api.models';
import { CategoriasApi } from '../../../../shared/api/categorias-api';
import { ContasApi } from '../../../../shared/api/contas-api';
import { ConfiguracaoInicialApi } from '../../../../shared/api/configuracao-inicial-api';
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
import {
  CategoriaDialogo,
  NovaCategoria,
} from '../../../categorias/components/categoria-dialogo/categoria-dialogo';
import { ContaDialogo } from '../../../contas/components/conta-dialogo/conta-dialogo';

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

interface GrupoDeCategoria extends Opcao {
  subcategorias: Opcao[];
}

const porRotulo = (a: Opcao, b: Opcao) => a.label.localeCompare(b.label, 'pt-BR');

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
    CategoriaDialogo,
    ContaDialogo,
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
  private readonly configuracaoApi = inject(ConfiguracaoInicialApi);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input<string>();
  readonly configuracaoInicial = input<string>();
  protected readonly primeirosPassos = computed(
    () => this.configuracaoInicial() === 'true' && !this.id(),
  );
  protected readonly lancamentoRegistrado = signal(false);
  protected readonly concluindoConfiguracao = signal(false);
  protected readonly erroNaConclusao = signal(false);

  protected readonly icones = { ChevronDown, ChevronUp, CreditCard, Plus };
  protected readonly tipos = TIPOS;

  protected readonly tipo = signal<TipoDoFormulario>('DESPESA');
  protected readonly valor = signal<number | null>(null);
  protected readonly grupoId = signal<string | null>(null);
  protected readonly subcategoriaId = signal<string | null>(null);
  protected readonly categoriaId = computed(() => this.subcategoriaId() ?? this.grupoId());
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
  protected readonly cadastrandoConta = signal(false);
  protected readonly novaCategoria = signal<NovaCategoria | null>(null);
  protected readonly filtroDoGrupo = signal('');
  protected readonly filtroDaSubcategoria = signal('');
  private readonly categoriasCriadas = signal<Categoria[]>([]);

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

  private readonly categorias = computed(() => {
    const todas = [...(valorDe(this.base)?.categorias ?? []), ...this.categoriasCriadas()];
    return [...new Map(todas.map((c) => [c.id, c])).values()];
  });

  protected readonly grupos = computed<GrupoDeCategoria[]>(() => {
    const tipo = this.tipo();
    const doTipo = this.categorias().filter((c) => c.tipo === tipo);
    const ids = new Set(doTipo.map((c) => c.id));
    return doTipo
      .filter((c) => !c.categoriaPaiId || !ids.has(c.categoriaPaiId))
      .map((raiz) => ({
        label: raiz.nomeCategoriaPai ? `${raiz.nomeCategoriaPai} › ${raiz.nome}` : raiz.nome,
        value: raiz.id,
        subcategorias: doTipo
          .filter((c) => c.categoriaPaiId === raiz.id)
          .map((c) => ({ label: c.nome, value: c.id }))
          .sort(porRotulo),
      }))
      .sort(porRotulo);
  });

  protected readonly grupoEscolhido = computed(() =>
    this.grupos().find((g) => g.value === this.grupoId()),
  );

  protected readonly subcategorias = computed(() => this.grupoEscolhido()?.subcategorias ?? []);

  protected readonly placeholderDaSubcategoria = computed(() => {
    if (!this.grupoEscolhido()) {
      return 'Escolha a categoria antes';
    }
    return this.subcategorias().length ? 'Escolha a subcategoria' : 'Nenhuma ainda';
  });

  protected readonly gruposParaNovaCategoria = computed(() =>
    this.categorias()
      .filter((c) => c.tipo === this.tipo() && !c.categoriaPaiId)
      .map((c) => ({ label: c.nome, value: c.id }))
      .sort(porRotulo),
  );

  protected readonly nomeDaCategoria = computed(() => {
    const categoria = this.categorias().find((c) => c.id === this.categoriaId());
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
    this.escolherGrupo(null);
    this.contaDestinoId.set(null);
  }

  protected escolherGrupo(grupoId: string | null): void {
    this.grupoId.set(grupoId);
    this.subcategoriaId.set(null);
  }

  protected cadastrarGrupo(seletor: Select): void {
    this.abrirNovaCategoria(seletor, null, this.filtroDoGrupo());
  }

  protected cadastrarSubcategoria(seletor: Select): void {
    this.abrirNovaCategoria(seletor, this.grupoId(), this.filtroDaSubcategoria());
  }

  protected categoriaCriada(categoria: Categoria): void {
    this.categoriasCriadas.update((criadas) => [...criadas, categoria]);
    this.escolherCategoria(this.categorias(), categoria.id);
    this.novaCategoria.set(null);
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

  protected contaCriada(conta: Conta): void {
    this.base.update((dados) => dados && { ...dados, contas: [...dados.contas, conta] });
    if (this.ehTransferencia() && this.contaId()) {
      this.contaDestinoId.set(conta.id);
    } else {
      this.escolherConta(conta.id);
    }
    this.cadastrandoConta.set(false);
  }

  protected salvar(fazerOutro: boolean): void {
    this.tentouSalvar.set(true);
    if (!this.valido() || this.salvando() || this.lancamentoRegistrado()) {
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
        if (this.primeirosPassos()) {
          this.lancamentoRegistrado.set(true);
          this.concluirConfiguracao();
        } else if (fazerOutro) {
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

  protected concluirConfiguracao(): void {
    if (!this.lancamentoRegistrado() || this.concluindoConfiguracao()) {
      return;
    }
    this.concluindoConfiguracao.set(true);
    this.erroNaConclusao.set(false);
    this.configuracaoApi
      .buscar()
      .pipe(
        switchMap((configuracao) =>
          this.configuracaoApi.salvar({
            ...configuracao,
            etapa: 'CONCLUIDO',
            pausado: false,
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.concluindoConfiguracao.set(false);
          void this.router.navigateByUrl('/configuracao-inicial', { replaceUrl: true });
        },
        error: () => {
          this.concluindoConfiguracao.set(false);
          this.erroNaConclusao.set(true);
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
      this.escolherCategoria(dados.categorias, lancamento.categoriaId);
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

  private abrirNovaCategoria(seletor: Select, categoriaPaiId: string | null, nome: string): void {
    seletor.hide();
    this.novaCategoria.set({ tipo: this.tipo() as TipoCategoria, categoriaPaiId, nome });
  }

  private escolherCategoria(categorias: Categoria[], categoriaId: string): void {
    const categoria = categorias.find((c) => c.id === categoriaId);
    const temGrupo = categorias.some((c) => c.id === categoria?.categoriaPaiId);
    this.grupoId.set(temGrupo ? categoria!.categoriaPaiId : categoriaId);
    this.subcategoriaId.set(temGrupo ? categoriaId : null);
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
    this.escolherGrupo(null);
    this.descricao.set('');
    this.vencimento.set(null);
    this.observacao.set('');
    this.tentouSalvar.set(false);
  }
}
