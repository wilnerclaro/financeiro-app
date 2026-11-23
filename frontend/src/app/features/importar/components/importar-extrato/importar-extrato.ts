import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Check, ChevronDown, FileText, Plus, Upload } from 'lucide';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { finalize, forkJoin } from 'rxjs';
import { mensagemDoErro } from '../../../../core/error-handling/erro-api';
import { Categoria } from '../../../../shared/api/api.models';
import { CategoriasApi } from '../../../../shared/api/categorias-api';
import { ContasApi } from '../../../../shared/api/contas-api';
import {
  ImportacoesApi,
  ItemExtrato,
  ResultadoImportacao,
} from '../../../../shared/api/importacoes-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { Icone } from '../../../../shared/components/icone/icone';
import { brl, dataCurta } from '../../../../shared/util/formatacao';
import { valorDe } from '../../../../shared/util/recurso';
import {
  CategoriaDialogo,
  NovaCategoria,
} from '../../../categorias/components/categoria-dialogo/categoria-dialogo';
import { ImportacaoDeExtrato } from '../../state/importacao-de-extrato';

interface LinhaDoExtrato extends ItemExtrato {
  selecionado: boolean;
  categoriaId: string | null;
}

type Etapa = 'enviar' | 'revisar' | 'pronto';

type Tipo = ItemExtrato['tipo'];

interface SecaoDoExtrato {
  tipo: Tipo;
  titulo: string;
  sinal: string;
  linhas: LinhaDoExtrato[];
  total: number;
  semCategoria: number;
}

const SECOES: { tipo: Tipo; titulo: string; sinal: string }[] = [
  { tipo: 'RECEITA', titulo: 'Receitas', sinal: '+' },
  { tipo: 'DESPESA', titulo: 'Despesas', sinal: '−' },
];

const ETAPAS: { valor: Etapa; rotulo: string }[] = [
  { valor: 'enviar', rotulo: 'Enviar arquivo' },
  { valor: 'revisar', rotulo: 'Revisar' },
  { valor: 'pronto', rotulo: 'Pronto' },
];

const LIMITE_EM_BYTES = 2 * 1024 * 1024;

@Component({
  selector: 'app-importar-extrato',
  imports: [FormsModule, Button, Dialog, Select, Skeleton, EstadoErro, Icone, CategoriaDialogo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './importar-extrato.html',
  styleUrl: './importar-extrato.scss',
})
export class ImportarExtrato {
  private readonly api = inject(ImportacoesApi);
  private readonly contasApi = inject(ContasApi);
  private readonly categoriasApi = inject(CategoriasApi);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly importacao = inject(ImportacaoDeExtrato);

  protected readonly icones = { Check, ChevronDown, FileText, Plus, Upload };
  protected readonly etapas = ETAPAS;
  protected readonly brl = brl;
  protected readonly dataCurta = dataCurta;

  protected readonly arquivo = signal<File | null>(null);
  protected readonly arrastando = signal(false);
  protected readonly linhas = signal<LinhaDoExtrato[]>([]);
  protected readonly etapa = signal<Etapa>('enviar');
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly banco = signal<string | null>(null);
  protected readonly resultado = signal<ResultadoImportacao | null>(null);
  protected readonly recolhidas = signal<ReadonlySet<Tipo>>(new Set());
  protected readonly novaCategoria = signal<NovaCategoria | null>(null);
  protected readonly filtroDaCategoria = signal('');
  private readonly linhaDaNovaCategoria = signal<string | null>(null);
  private readonly categoriasCriadas = signal<Categoria[]>([]);

  protected readonly dados = rxResource({
    params: () => this.importacao.aberta() || undefined,
    stream: () =>
      forkJoin({
        contas: this.contasApi.listar(true),
        categorias: this.categoriasApi.listar(true),
      }),
  });

  protected readonly contas = computed(() =>
    (valorDe(this.dados)?.contas ?? []).filter(
      (c) => c.tipo === 'CONTA_CORRENTE' || c.tipo === 'POUPANCA',
    ),
  );
  protected readonly contaId = linkedSignal<string | null>(() => {
    const contas = this.contas();
    const sugerida = this.importacao.contaSugerida();
    return contas.some((c) => c.id === sugerida) ? sugerida : (contas[0]?.id ?? null);
  });
  protected readonly opcoesContas = computed(() =>
    this.contas().map((c) => ({ label: c.nome, value: c.id })),
  );
  protected readonly nomeConta = computed(
    () => this.contas().find((c) => c.id === this.contaId())?.nome ?? '',
  );
  private readonly categorias = computed(() => {
    const todas = [...(valorDe(this.dados)?.categorias ?? []), ...this.categoriasCriadas()];
    return [...new Map(todas.map((c) => [c.id, c])).values()];
  });
  protected readonly categoriasPorTipo = computed(() => {
    const categorias = this.categorias();
    const opcoes = (tipo: 'RECEITA' | 'DESPESA') =>
      categorias
        .filter((c) => c.tipo === tipo)
        .map((c) => ({
          label: c.nomeCategoriaPai ? `${c.nomeCategoriaPai} › ${c.nome}` : c.nome,
          value: c.id,
        }));
    return { RECEITA: opcoes('RECEITA'), DESPESA: opcoes('DESPESA') };
  });
  protected readonly gruposParaNovaCategoria = computed(() =>
    this.categorias()
      .filter((c) => c.tipo === this.novaCategoria()?.tipo && !c.categoriaPaiId)
      .map((c) => ({ label: c.nome, value: c.id }))
      .sort((a, b) => a.label.localeCompare(b.label, 'pt-BR')),
  );
  protected readonly selecionadas = computed(() =>
    this.linhas().filter((l) => l.selecionado && l.importavel && !l.jaImportado),
  );
  protected readonly faltamCategorias = computed(
    () => this.selecionadas().filter((l) => !l.categoriaId).length,
  );
  protected readonly podeConfirmar = computed(
    () => this.selecionadas().length > 0 && !this.faltamCategorias() && !this.ocupado(),
  );
  protected readonly secoes = computed<SecaoDoExtrato[]>(() =>
    SECOES.map((secao) => ({
      ...secao,
      linhas: this.linhas().filter((l) => l.tipo === secao.tipo),
      total: this.total(secao.tipo),
      semCategoria: this.selecionadas().filter((l) => l.tipo === secao.tipo && !l.categoriaId)
        .length,
    })).filter((secao) => secao.linhas.length),
  );
  protected readonly totalReceitas = computed(() => this.total('RECEITA'));
  protected readonly totalDespesas = computed(() => this.total('DESPESA'));
  protected readonly largura = computed(() =>
    this.etapa() === 'pronto' ? 'min(520px, 96vw)' : 'min(920px, 96vw)',
  );

  protected escolherArquivo(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    if (!this.receberArquivo(input.files?.[0] ?? null)) {
      input.value = '';
    }
  }

  protected arrastar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastando.set(true);
  }

  protected soltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastando.set(false);
    if (!this.ocupado()) {
      this.receberArquivo(evento.dataTransfer?.files?.[0] ?? null);
    }
  }

  protected trocarArquivo(): void {
    this.arquivo.set(null);
    this.erro.set('');
  }

  protected revisar(): void {
    const arquivo = this.arquivo();
    const contaId = this.contaId();
    if (this.ocupado() || !arquivo || !contaId) return;
    this.ocupado.set(true);
    this.erro.set('');
    this.api
      .revisar(arquivo, contaId)
      .pipe(
        finalize(() => this.ocupado.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (revisao) => {
          this.linhas.set(
            revisao.itens.map((item) => ({
              ...item,
              selecionado: item.importavel && !item.jaImportado && !item.possivelDuplicado,
              categoriaId: null,
            })),
          );
          this.banco.set(revisao.banco);
          this.etapa.set('revisar');
        },
        error: (erro: unknown) => this.erro.set(mensagemDoErro(erro)),
      });
  }

  protected selecionar(id: string, selecionado: boolean): void {
    this.linhas.update((linhas) =>
      linhas.map((l) =>
        l.identificador === id && l.importavel && !l.jaImportado ? { ...l, selecionado } : l,
      ),
    );
  }

  protected alternarSecao(tipo: Tipo): void {
    this.recolhidas.update((recolhidas) => {
      const proximas = new Set(recolhidas);
      if (!proximas.delete(tipo)) {
        proximas.add(tipo);
      }
      return proximas;
    });
  }

  protected categorizar(id: string, categoriaId: string | null): void {
    this.linhas.update((linhas) =>
      linhas.map((l) => (l.identificador === id ? { ...l, categoriaId } : l)),
    );
  }

  protected cadastrarCategoria(linha: LinhaDoExtrato, seletor: Select): void {
    const nome = this.filtroDaCategoria();
    seletor.hide();
    this.linhaDaNovaCategoria.set(linha.identificador);
    this.novaCategoria.set({ tipo: linha.tipo, categoriaPaiId: null, nome });
  }

  protected categoriaCriada(categoria: Categoria): void {
    this.categoriasCriadas.update((criadas) => [...criadas, categoria]);
    const linha = this.linhaDaNovaCategoria();
    if (linha) {
      this.categorizar(linha, categoria.id);
    }
    this.fecharNovaCategoria();
  }

  protected fecharNovaCategoria(): void {
    this.novaCategoria.set(null);
    this.linhaDaNovaCategoria.set(null);
  }

  protected confirmar(): void {
    const arquivo = this.arquivo();
    const contaId = this.contaId();
    if (!this.podeConfirmar() || !arquivo || !contaId) return;
    this.ocupado.set(true);
    this.erro.set('');
    const selecoes = this.selecionadas().map((l) => ({
      identificador: l.identificador,
      categoriaId: l.categoriaId ?? '',
    }));
    this.api
      .confirmar(arquivo, contaId, selecoes)
      .pipe(
        finalize(() => this.ocupado.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (resultado) => {
          this.resultado.set(resultado);
          this.etapa.set('pronto');
          this.importacao.registrarConclusao();
        },
        error: (erro: unknown) => this.erro.set(mensagemDoErro(erro)),
      });
  }

  protected recomecar(): void {
    if (this.ocupado()) return;
    this.limpar();
  }

  protected fechar(): void {
    if (this.ocupado()) return;
    this.importacao.fechar();
  }

  protected aoFechar(): void {
    this.limpar();
  }

  protected irPara(rota: string): void {
    this.importacao.fechar();
    void this.router.navigateByUrl(rota);
  }

  private receberArquivo(arquivo: File | null): boolean {
    this.arquivo.set(null);
    this.erro.set('');
    if (!arquivo) return false;
    if (!/\.(pdf|csv|ofx)$/i.test(arquivo.name)) {
      this.erro.set('Escolha o extrato em PDF, CSV ou OFX.');
      return false;
    }
    if (arquivo.size === 0 || arquivo.size > LIMITE_EM_BYTES) {
      this.erro.set('O arquivo deve ter conteúdo e no máximo 2 MB.');
      return false;
    }
    this.arquivo.set(arquivo);
    return true;
  }

  private limpar(): void {
    this.arquivo.set(null);
    this.arrastando.set(false);
    this.linhas.set([]);
    this.banco.set(null);
    this.resultado.set(null);
    this.recolhidas.set(new Set());
    this.categoriasCriadas.set([]);
    this.erro.set('');
    this.etapa.set('enviar');
  }

  private total(tipo: Tipo): number {
    return (
      this.selecionadas()
        .filter((l) => l.tipo === tipo)
        .reduce((soma, l) => soma + Math.round(l.valor * 100), 0) / 100
    );
  }
}
