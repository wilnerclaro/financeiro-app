import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { InputNumber } from 'primeng/inputnumber';
import { Skeleton } from 'primeng/skeleton';
import { forkJoin, of, switchMap } from 'rxjs';
import { Categoria, PrevistoRealizado, TipoCategoria } from '../../../../shared/api/api.models';
import { CategoriasApi } from '../../../../shared/api/categorias-api';
import { OrcamentoApi } from '../../../../shared/api/orcamento-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { SeletorMes } from '../../../../shared/components/seletor-mes/seletor-mes';
import { AGORA } from '../../../../shared/util/agora';
import { Avisos } from '../../../../shared/util/avisos';
import { MesDoAno, competenciaIso, mesAtual, somarMeses } from '../../../../shared/util/datas';
import { MESES_LONGOS, brl } from '../../../../shared/util/formatacao';
import { AbasOrcamento } from '../../components/abas-orcamento/abas-orcamento';
import { valorDe } from '../../../../shared/util/recurso';

interface LinhaDoPrevisto {
  categoriaId: string;
  nome: string;
  grupo: boolean;
  sub: boolean;
  editavel: boolean;
  filhos: string[];
}

@Component({
  selector: 'app-orcamento-previsto',
  imports: [
    FormsModule,
    Button,
    Checkbox,
    InputNumber,
    Skeleton,
    EstadoErro,
    SeletorMes,
    AbasOrcamento,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './orcamento-previsto.html',
  styleUrl: './orcamento-previsto.scss',
})
export class OrcamentoPrevisto {
  private readonly orcamentoApi = inject(OrcamentoApi);
  private readonly categoriasApi = inject(CategoriasApi);
  private readonly avisos = inject(Avisos);
  private readonly agora = inject(AGORA);

  protected readonly brl = brl;
  protected readonly mes = signal<MesDoAno>(mesAtual(this.agora()));
  protected readonly editados = signal<ReadonlyMap<string, number>>(new Map());
  protected readonly repetirAteDezembro = signal(false);
  protected readonly salvando = signal(false);
  protected readonly nomeDoMes = computed(() => MESES_LONGOS[this.mes().mes - 1]);
  protected readonly podeRepetir = computed(() => this.mes().mes < 12);

  protected readonly base = rxResource({
    params: () => this.mes(),
    stream: ({ params }) =>
      forkJoin({
        categorias: this.categoriasApi.listar(true),
        relatorio: this.orcamentoApi.doMes(params.ano, params.mes),
      }),
  });

  private readonly originais = computed(() => this.previstosDe(valorDe(this.base)?.relatorio));

  protected readonly blocos = computed(() => {
    const categorias = valorDe(this.base)?.categorias ?? [];
    return [
      { tipo: 'DESPESA', titulo: 'Despesas', linhas: this.linhasDo(categorias, 'DESPESA') },
      { tipo: 'RECEITA', titulo: 'Receitas', linhas: this.linhasDo(categorias, 'RECEITA') },
    ];
  });

  protected readonly alterados = computed(() => {
    const originais = this.originais();
    return [...this.editados().entries()].filter(
      ([id, valor]) => (originais.get(id) ?? 0) !== valor,
    );
  });

  protected valorDe(categoriaId: string): number {
    return this.editados().get(categoriaId) ?? this.originais().get(categoriaId) ?? 0;
  }

  protected somaDe(linha: LinhaDoPrevisto): number {
    return linha.filhos.reduce((total, id) => total + this.valorDe(id), 0);
  }

  protected editar(categoriaId: string, valor: number | null): void {
    const editados = new Map(this.editados());
    editados.set(categoriaId, valor ?? 0);
    this.editados.set(editados);
  }

  protected trocarMes(mes: MesDoAno): void {
    this.mes.set(mes);
    this.editados.set(new Map());
  }

  protected descartar(): void {
    this.editados.set(new Map());
  }

  protected salvar(): void {
    const alterados = this.alterados();
    if (!alterados.length || this.salvando()) {
      return;
    }
    const { ano, mes } = this.mes();
    const itens = alterados.map(([categoriaId, valorPrevisto]) => ({ categoriaId, valorPrevisto }));
    const proximos = this.repetirAteDezembro() && this.podeRepetir() ? this.mesesAteDezembro() : [];
    this.salvando.set(true);
    this.orcamentoApi
      .definir(ano, mes, itens)
      .pipe(
        switchMap(() =>
          proximos.length
            ? forkJoin(
                itens.map((i) =>
                  this.orcamentoApi.repetir(i.categoriaId, i.valorPrevisto, proximos, true),
                ),
              )
            : of([]),
        ),
      )
      .subscribe({
        next: () => {
          this.salvando.set(false);
          this.editados.set(new Map());
          this.avisos.sucesso(
            'Previsto salvo',
            `${itens.length} ${itens.length === 1 ? 'categoria atualizada' : 'categorias atualizadas'}${
              proximos.length ? ' até dezembro' : ` em ${this.nomeDoMes()}`
            }.`,
          );
          this.base.reload();
        },
        error: (erro: unknown) => {
          this.salvando.set(false);
          this.avisos.erro('Não foi possível salvar o previsto', erro);
        },
      });
  }

  private mesesAteDezembro(): string[] {
    const meses: string[] = [];
    for (
      let alvo = somarMeses(this.mes(), 1);
      alvo.ano === this.mes().ano;
      alvo = somarMeses(alvo, 1)
    ) {
      meses.push(competenciaIso(alvo));
    }
    return meses;
  }

  private previstosDe(relatorio: PrevistoRealizado | undefined): Map<string, number> {
    const previstos = new Map<string, number>();
    for (const bloco of [relatorio?.despesas, relatorio?.receitas]) {
      for (const grupo of bloco?.grupos ?? []) {
        previstos.set(grupo.categoriaId, grupo.valores.previsto);
        for (const sub of grupo.subitens) {
          previstos.set(sub.categoriaId, sub.valores.previsto);
        }
      }
    }
    return previstos;
  }

  private linhasDo(categorias: Categoria[], tipo: TipoCategoria): LinhaDoPrevisto[] {
    const doTipo = categorias.filter((c) => c.tipo === tipo && c.ativa);
    const porNome = (a: Categoria, b: Categoria) => a.nome.localeCompare(b.nome, 'pt-BR');
    const linhas: LinhaDoPrevisto[] = [];
    for (const grupo of doTipo.filter((c) => !c.categoriaPaiId).sort(porNome)) {
      const filhos = doTipo.filter((c) => c.categoriaPaiId === grupo.id).sort(porNome);
      linhas.push({
        categoriaId: grupo.id,
        nome: grupo.nome,
        grupo: filhos.length > 0,
        sub: false,
        editavel: filhos.length === 0,
        filhos: filhos.map((f) => f.id),
      });
      for (const filho of filhos) {
        linhas.push({
          categoriaId: filho.id,
          nome: filho.nome,
          grupo: false,
          sub: true,
          editavel: true,
          filhos: [],
        });
      }
    }
    return linhas;
  }
}
