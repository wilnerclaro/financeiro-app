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
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Categoria, FonteReceita, TipoCategoria } from '../../../../shared/api/api.models';
import { CategoriasApi } from '../../../../shared/api/categorias-api';
import { Avisos } from '../../../../shared/util/avisos';

export interface NovaCategoria {
  tipo: TipoCategoria;
  categoriaPaiId: string | null;
  nome: string;
}

export interface OpcaoDeGrupo {
  label: string;
  value: string;
}

const FONTES: { label: string; value: FonteReceita }[] = [
  { label: 'Ativa (trabalho)', value: 'ATIVA' },
  { label: 'Passiva (renda de patrimônio)', value: 'PASSIVA' },
];

@Component({
  selector: 'app-categoria-dialogo',
  imports: [FormsModule, Button, Dialog, InputText, Select],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './categoria-dialogo.html',
  styles: `
    .corpo {
      display: flex;
      flex-direction: column;
      gap: var(--e4);
    }

    .erro {
      color: var(--des);
    }
  `,
})
export class CategoriaDialogo {
  private readonly categoriasApi = inject(CategoriasApi);
  private readonly avisos = inject(Avisos);

  readonly pedido = input<NovaCategoria | null>(null);
  readonly grupos = input<OpcaoDeGrupo[]>([]);
  readonly fechar = output();
  readonly salva = output<Categoria>();

  protected readonly fontes = FONTES;
  protected readonly nome = signal('');
  protected readonly paiId = signal<string | null>(null);
  protected readonly fonte = signal<FonteReceita>('ATIVA');
  protected readonly salvando = signal(false);
  protected readonly tentouSalvar = signal(false);

  protected readonly tipo = computed(() => this.pedido()?.tipo ?? 'DESPESA');
  protected readonly titulo = computed(() =>
    this.paiId()
      ? 'Nova subcategoria'
      : `Nova categoria de ${this.tipo() === 'DESPESA' ? 'despesa' : 'receita'}`,
  );
  protected readonly mostraFonte = computed(() => this.tipo() === 'RECEITA' && !this.paiId());
  protected readonly erroDoNome = computed(() =>
    this.nome().trim() ? '' : 'Informe o nome da categoria.',
  );

  constructor() {
    effect(() => {
      const pedido = this.pedido();
      untracked(() => {
        this.nome.set(pedido?.nome.trim() ?? '');
        this.paiId.set(pedido?.categoriaPaiId ?? null);
        this.fonte.set('ATIVA');
        this.tentouSalvar.set(false);
      });
    });
  }

  protected salvar(): void {
    this.tentouSalvar.set(true);
    if (this.erroDoNome() || this.salvando()) {
      return;
    }
    this.salvando.set(true);
    this.categoriasApi
      .criar({
        nome: this.nome().trim(),
        tipo: this.tipo(),
        categoriaPaiId: this.paiId(),
        fonteReceita: this.mostraFonte() ? this.fonte() : null,
      })
      .subscribe({
        next: (categoria) => {
          this.salvando.set(false);
          this.avisos.sucesso('Categoria criada', categoria.nome);
          this.salva.emit(categoria);
        },
        error: (erro: unknown) => {
          this.salvando.set(false);
          this.avisos.erro('Não foi possível salvar a categoria', erro);
        },
      });
  }
}
