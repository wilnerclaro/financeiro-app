import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { Skeleton } from 'primeng/skeleton';
import { Observable, finalize, forkJoin, of, switchMap, tap } from 'rxjs';
import { mensagemDoErro } from '../../../../core/error-handling/erro-api';
import { Categoria, Conta, PrevistoRealizado } from '../../../../shared/api/api.models';
import { CategoriasApi } from '../../../../shared/api/categorias-api';
import {
  ConfiguracaoInicial,
  ConfiguracaoInicialApi,
  EtapaConfiguracaoInicial,
} from '../../../../shared/api/configuracao-inicial-api';
import { ContasApi } from '../../../../shared/api/contas-api';
import { OrcamentoApi } from '../../../../shared/api/orcamento-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { SeletorMes } from '../../../../shared/components/seletor-mes/seletor-mes';
import { Avisos } from '../../../../shared/util/avisos';
import { MesDoAno } from '../../../../shared/util/datas';
import { brl, rotuloDoMes } from '../../../../shared/util/formatacao';
import {
  ContaDialogo,
  ModoDoDialogo,
} from '../../../contas/components/conta-dialogo/conta-dialogo';
import { rotuloDoTipo } from '../../../contas/tipos-de-conta';
import { LinhaInicial, linhasIniciais } from './orcamento-inicial';

const ETAPAS: EtapaConfiguracaoInicial[] = [
  'BOAS_VINDAS',
  'OBJETIVO',
  'ORCAMENTO',
  'CONTAS',
  'LANCAMENTO',
  'CONCLUIDO',
];

@Component({
  selector: 'app-configuracao-inicial',
  imports: [FormsModule, Button, InputNumber, Skeleton, EstadoErro, SeletorMes, ContaDialogo],
  templateUrl: './configuracao-inicial.html',
  styleUrl: './configuracao-inicial.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfiguracaoInicialPagina {
  private readonly api = inject(ConfiguracaoInicialApi);
  private readonly categoriasApi = inject(CategoriasApi);
  private readonly contasApi = inject(ContasApi);
  private readonly orcamentoApi = inject(OrcamentoApi);
  private readonly router = inject(Router);
  private readonly avisos = inject(Avisos);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly brl = brl;
  protected readonly rotuloDoTipo = rotuloDoTipo;
  protected readonly passos = [
    { etapa: 'OBJETIVO' as const, nome: 'Seu objetivo' },
    { etapa: 'ORCAMENTO' as const, nome: 'Seu orçamento' },
    { etapa: 'CONTAS' as const, nome: 'Suas contas' },
    { etapa: 'LANCAMENTO' as const, nome: 'Primeiro lançamento' },
  ];
  protected readonly configuracao = signal<ConfiguracaoInicial | null>(null);
  protected readonly objetivo = signal<number | null>(null);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly contas = signal<Conta[]>([]);
  protected readonly relatorio = signal<PrevistoRealizado | null>(null);
  protected readonly editados = signal<ReadonlyMap<string, number>>(new Map());
  protected readonly carregando = signal(true);
  protected readonly erroInicial = signal(false);
  protected readonly erro = signal('');
  protected readonly salvando = signal(false);
  protected readonly modoConta = signal<ModoDoDialogo | null>(null);
  protected readonly contaEmEdicao = signal<Conta | null>(null);
  protected readonly etapa = computed(() => this.configuracao()?.etapa ?? 'BOAS_VINDAS');
  protected readonly indice = computed(() => ETAPAS.indexOf(this.etapa()));
  protected readonly mes = computed<MesDoAno>(() => ({
    ano: this.configuracao()!.ano,
    mes: this.configuracao()!.mes,
  }));
  protected readonly nomeMes = computed(() =>
    this.configuracao() ? rotuloDoMes(this.mes().ano, this.mes().mes) : '',
  );
  protected readonly blocos = computed(() => [
    {
      titulo: 'Receitas',
      tipo: 'RECEITA',
      linhas: linhasIniciais(this.categorias(), this.relatorio(), 'RECEITA'),
    },
    {
      titulo: 'Despesas',
      tipo: 'DESPESA',
      linhas: linhasIniciais(this.categorias(), this.relatorio(), 'DESPESA'),
    },
  ]);
  protected readonly sobra = computed(() => {
    let centavos = Math.round((this.relatorio()?.saldo.previsto ?? 0) * 100);
    for (const bloco of this.blocos()) {
      for (const linha of bloco.linhas.filter((l) => l.editavel)) {
        const delta = Math.round(this.valor(linha) * 100) - Math.round(linha.previsto * 100);
        centavos += bloco.tipo === 'RECEITA' ? delta : -delta;
      }
    }
    return centavos / 100;
  });
  protected readonly rotuloContinuar = computed(
    () =>
      ({
        BOAS_VINDAS: 'Começar configuração',
        OBJETIVO: 'Planejar meu orçamento',
        ORCAMENTO: 'Preparar minhas contas',
        CONTAS: 'Ir para o primeiro lançamento',
        LANCAMENTO: 'Registrar agora',
        CONCLUIDO: 'Ir para o início',
      })[this.etapa()],
  );

  constructor() {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erroInicial.set(false);
    this.api
      .buscar()
      .pipe(
        switchMap((configuracao) =>
          forkJoin({
            configuracao: of(configuracao),
            contas: this.contasApi.listar(true),
            categorias: this.categoriasApi.listar(true),
            relatorio: this.orcamentoApi.doMes(configuracao.ano, configuracao.mes),
          }),
        ),
        finalize(() => this.carregando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (dados) => {
          this.configuracao.set(dados.configuracao);
          this.objetivo.set(dados.configuracao.rendaPassivaDesejada);
          this.contas.set(dados.contas);
          this.categorias.set(dados.categorias);
          this.relatorio.set(dados.relatorio);
        },
        error: () => this.erroInicial.set(true),
      });
  }

  protected valor(linha: LinhaInicial): number {
    return this.editados().get(linha.id) ?? linha.previsto;
  }

  protected totalGrupo(linha: LinhaInicial): number {
    const linhas = this.blocos().flatMap((b) => b.linhas);
    const delta = linhas
      .filter((l) => linha.filhos.includes(l.id))
      .reduce(
        (soma, l) => soma + Math.round(this.valor(l) * 100) - Math.round(l.previsto * 100),
        0,
      );
    return (Math.round(linha.previsto * 100) + delta) / 100;
  }

  protected editar(id: string, valor: number | null): void {
    this.editados.update((anteriores) => new Map(anteriores).set(id, valor ?? 0));
  }

  protected selecionarValor(evento: Event): void {
    if (evento.target instanceof HTMLInputElement) evento.target.select();
  }

  protected continuar(): void {
    this.erro.set('');
    if (this.etapa() === 'OBJETIVO' && !(this.objetivo()! > 0)) {
      this.erro.set('Informe uma renda passiva mensal desejada maior que zero.');
      return;
    }
    if (this.etapa() === 'ORCAMENTO' && !this.editados().size && !this.temPrevisto()) {
      this.erro.set('Preencha o previsto de pelo menos uma categoria para continuar.');
      return;
    }
    if ((this.etapa() === 'CONTAS' || this.etapa() === 'LANCAMENTO') && !this.contas().length) {
      this.erro.set('Cadastre sua primeira conta para continuar.');
      return;
    }
    if (this.etapa() === 'CONCLUIDO') {
      void this.router.navigateByUrl('/');
      return;
    }
    if (this.etapa() === 'LANCAMENTO') {
      this.persistir(
        'LANCAMENTO',
        false,
        () =>
          void this.router.navigate(['/lancar'], { queryParams: { configuracaoInicial: true } }),
      );
      return;
    }
    this.persistir(ETAPAS[this.indice() + 1], false);
  }

  protected voltar(): void {
    this.irPara(ETAPAS[Math.max(0, this.indice() - 1)]);
  }
  protected irPara(etapa: EtapaConfiguracaoInicial): void {
    this.persistir(etapa, false);
  }
  protected pausar(): void {
    this.persistir(this.etapa(), true, () => void this.router.navigateByUrl('/'));
  }
  protected concluir(): void {
    this.persistir('CONCLUIDO', false);
  }

  protected trocarMes(mes: MesDoAno): void {
    if (this.salvando() || mes.ano < 2000 || mes.ano > 2100) return;
    this.executar(
      this.salvarPrevisto().pipe(
        switchMap(() => this.orcamentoApi.doMes(mes.ano, mes.mes)),
        switchMap((relatorio) =>
          this.api
            .salvar({
              ...this.configuracao()!,
              ...mes,
              rendaPassivaDesejada: this.objetivo(),
              pausado: false,
            })
            .pipe(
              tap((configuracao) => {
                this.configuracao.set(configuracao);
                this.relatorio.set(relatorio);
              }),
            ),
        ),
      ),
    );
  }

  protected abrirConta(conta: Conta | null = null, modo: ModoDoDialogo = 'criar'): void {
    this.contaEmEdicao.set(conta);
    this.modoConta.set(modo);
  }

  protected contaSalva(conta: Conta): void {
    this.contas.update((contas) => [...contas.filter((c) => c.id !== conta.id), conta]);
    this.modoConta.set(null);
    this.contaEmEdicao.set(null);
  }

  private persistir(etapa: EtapaConfiguracaoInicial, pausado: boolean, depois?: () => void): void {
    if (this.salvando()) return;
    const configuracao = this.configuracao()!;
    const objetivo = this.objetivo();
    const rendaPassivaDesejada =
      pausado && !(objetivo! > 0) ? configuracao.rendaPassivaDesejada : objetivo;
    this.executar(
      this.salvarPrevisto().pipe(
        switchMap(() => this.api.salvar({ ...configuracao, rendaPassivaDesejada, etapa, pausado })),
        tap((salva) => {
          this.configuracao.set(salva);
          this.avisos.sucesso(
            pausado
              ? 'Progresso salvo'
              : etapa === 'CONCLUIDO'
                ? 'Configuração concluída'
                : 'Etapa salva',
            pausado ? 'Você pode continuar de onde parou.' : 'Suas escolhas foram guardadas.',
          );
          depois?.();
        }),
      ),
    );
  }

  private executar(pedido: Observable<unknown>): void {
    this.salvando.set(true);
    this.erro.set('');
    pedido
      .pipe(
        finalize(() => this.salvando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: (erro: unknown) => this.erro.set(mensagemDoErro(erro)),
      });
  }

  private salvarPrevisto(): Observable<unknown> {
    if (!this.editados().size) return of(null);
    const itens = [...this.editados()].map(([categoriaId, valorPrevisto]) => ({
      categoriaId,
      valorPrevisto,
    }));
    const { ano, mes } = this.mes();
    return this.orcamentoApi.definir(ano, mes, itens).pipe(
      tap((relatorio) => {
        this.relatorio.set(relatorio);
        this.editados.set(new Map());
      }),
    );
  }

  private temPrevisto(): boolean {
    const relatorio = this.relatorio();
    return [relatorio?.receitas.total.previsto, relatorio?.despesas.total.previsto].some(
      (v) => (v ?? 0) > 0,
    );
  }
}
