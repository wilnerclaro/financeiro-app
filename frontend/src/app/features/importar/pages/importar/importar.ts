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
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { finalize, forkJoin } from 'rxjs';
import { mensagemDoErro } from '../../../../core/error-handling/erro-api';
import { Categoria, Conta } from '../../../../shared/api/api.models';
import { CategoriasApi } from '../../../../shared/api/categorias-api';
import { ContasApi } from '../../../../shared/api/contas-api';
import { ImportacoesApi, ItemOfx, ResultadoOfx } from '../../../../shared/api/importacoes-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { brl, dataCurta } from '../../../../shared/util/formatacao';

interface LinhaOfx extends ItemOfx {
  selecionado: boolean;
  categoriaId: string | null;
}

@Component({
  selector: 'app-importar',
  imports: [FormsModule, RouterLink, Button, Select, Skeleton, EstadoErro],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './importar.html',
  styleUrl: './importar.scss',
})
export class Importar {
  private readonly api = inject(ImportacoesApi);
  private readonly contasApi = inject(ContasApi);
  private readonly categoriasApi = inject(CategoriasApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly brl = brl;
  protected readonly dataCurta = dataCurta;
  protected readonly contas = signal<Conta[]>([]);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly arquivo = signal<File | null>(null);
  protected readonly contaId = signal<string | null>(null);
  protected readonly linhas = signal<LinhaOfx[]>([]);
  protected readonly etapa = signal<'enviar' | 'revisar' | 'pronto'>('enviar');
  protected readonly carregando = signal(true);
  protected readonly erroInicial = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly resultado = signal<ResultadoOfx | null>(null);
  protected readonly opcoesContas = computed(() =>
    this.contas().map((c) => ({ label: c.nome, value: c.id })),
  );
  protected readonly nomeConta = computed(
    () => this.contas().find((c) => c.id === this.contaId())?.nome ?? '',
  );
  protected readonly categoriasPorTipo = computed(() => {
    const opcoes = (tipo: 'RECEITA' | 'DESPESA') =>
      this.categorias()
        .filter((c) => c.tipo === tipo)
        .map((c) => ({
          label: c.nomeCategoriaPai ? `${c.nomeCategoriaPai} › ${c.nome}` : c.nome,
          value: c.id,
        }));
    return { RECEITA: opcoes('RECEITA'), DESPESA: opcoes('DESPESA') };
  });
  protected readonly selecionadas = computed(() =>
    this.linhas().filter((l) => l.selecionado && l.importavel && !l.jaImportado),
  );
  protected readonly faltamCategorias = computed(() =>
    this.selecionadas().some((l) => !l.categoriaId),
  );
  protected readonly podeConfirmar = computed(
    () => this.selecionadas().length > 0 && !this.faltamCategorias() && !this.ocupado(),
  );
  protected readonly totalReceitas = computed(() => this.total('RECEITA'));
  protected readonly totalDespesas = computed(() => this.total('DESPESA'));

  constructor() {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erroInicial.set(false);
    forkJoin({ contas: this.contasApi.listar(true), categorias: this.categoriasApi.listar(true) })
      .pipe(
        finalize(() => this.carregando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ contas, categorias }) => {
          this.contas.set(
            contas.filter((c) => c.tipo === 'CONTA_CORRENTE' || c.tipo === 'POUPANCA'),
          );
          this.categorias.set(categorias);
          if (!this.contas().some((c) => c.id === this.contaId()))
            this.contaId.set(this.contas()[0]?.id ?? null);
        },
        error: () => this.erroInicial.set(true),
      });
  }

  protected escolherArquivo(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;
    this.arquivo.set(null);
    this.erro.set('');
    if (!arquivo) return;
    if (!/\.ofx$/i.test(arquivo.name)) {
      this.erro.set('Escolha um arquivo OFX. Os outros formatos ainda não estão disponíveis.');
      input.value = '';
      return;
    }
    if (arquivo.size === 0 || arquivo.size > 2 * 1024 * 1024) {
      this.erro.set('O arquivo deve ter conteúdo e no máximo 2 MB.');
      input.value = '';
      return;
    }
    this.arquivo.set(arquivo);
  }

  protected revisar(): void {
    if (this.ocupado() || !this.arquivo() || !this.contaId()) return;
    this.ocupado.set(true);
    this.erro.set('');
    this.api
      .revisar(this.arquivo()!, this.contaId()!)
      .pipe(
        finalize(() => this.ocupado.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (revisao) => {
          this.linhas.set(
            revisao.itens.map((item) => ({
              ...item,
              selecionado: item.importavel && !item.jaImportado,
              categoriaId: null,
            })),
          );
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

  protected categorizar(id: string, categoriaId: string | null): void {
    this.linhas.update((linhas) =>
      linhas.map((l) => (l.identificador === id ? { ...l, categoriaId } : l)),
    );
  }

  protected confirmar(): void {
    if (!this.podeConfirmar() || !this.arquivo() || !this.contaId()) return;
    this.ocupado.set(true);
    this.erro.set('');
    const selecoes = this.selecionadas().map((l) => ({
      identificador: l.identificador,
      categoriaId: l.categoriaId!,
    }));
    this.api
      .confirmar(this.arquivo()!, this.contaId()!, selecoes)
      .pipe(
        finalize(() => this.ocupado.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (resultado) => {
          this.resultado.set(resultado);
          this.etapa.set('pronto');
        },
        error: (erro: unknown) => this.erro.set(mensagemDoErro(erro)),
      });
  }

  protected recomecar(): void {
    if (this.ocupado()) return;
    this.arquivo.set(null);
    this.linhas.set([]);
    this.resultado.set(null);
    this.erro.set('');
    this.etapa.set('enviar');
  }

  private total(tipo: 'RECEITA' | 'DESPESA'): number {
    return (
      this.selecionadas()
        .filter((l) => l.tipo === tipo)
        .reduce((soma, l) => soma + Math.round(l.valor * 100), 0) / 100
    );
  }
}
