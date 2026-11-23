import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { EllipsisVertical, Plus, Tags } from 'lucide';
import { ConfirmationService, MenuItem } from 'primeng/api';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Menu } from 'primeng/menu';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { Skeleton } from 'primeng/skeleton';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Observable } from 'rxjs';
import { Categoria, FonteReceita, TipoCategoria } from '../../../../shared/api/api.models';
import { CategoriasApi } from '../../../../shared/api/categorias-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { EstadoVazio } from '../../../../shared/components/estado-vazio/estado-vazio';
import { Icone } from '../../../../shared/components/icone/icone';
import { Avisos } from '../../../../shared/util/avisos';
import { valorDe } from '../../../../shared/util/recurso';

interface GrupoDeCategorias {
  categoria: Categoria;
  subcategorias: Categoria[];
}

const TIPOS: { label: string; value: TipoCategoria }[] = [
  { label: 'Despesas', value: 'DESPESA' },
  { label: 'Receitas', value: 'RECEITA' },
];

const FONTES: { label: string; value: FonteReceita }[] = [
  { label: 'Ativa (trabalho)', value: 'ATIVA' },
  { label: 'Passiva (renda de patrimônio)', value: 'PASSIVA' },
];

@Component({
  selector: 'app-categorias',
  imports: [
    FormsModule,
    Button,
    ConfirmDialog,
    Dialog,
    InputText,
    Menu,
    Select,
    SelectButton,
    Skeleton,
    ToggleSwitch,
    EstadoErro,
    EstadoVazio,
    Icone,
  ],
  providers: [ConfirmationService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './categorias.html',
  styleUrl: './categorias.scss',
})
export class Categorias {
  private readonly categoriasApi = inject(CategoriasApi);
  private readonly confirmacao = inject(ConfirmationService);
  private readonly avisos = inject(Avisos);

  protected readonly icones = { EllipsisVertical, Plus, Tags };
  protected readonly tipos = TIPOS;
  protected readonly fontes = FONTES;
  protected readonly esqueletoDosGrupos = [
    { titulo: '9rem', subcategorias: ['7rem', '5rem', '8rem'] },
    { titulo: '7rem', subcategorias: ['6rem', '7rem'] },
    { titulo: '10rem', subcategorias: ['5rem', '8rem', '6rem', '7rem'] },
  ];

  protected readonly tipo = signal<TipoCategoria>('DESPESA');
  protected readonly mostrarInativas = signal(false);
  protected readonly acoes = signal<MenuItem[]>([]);

  protected readonly dialogoAberto = signal(false);
  protected readonly emEdicao = signal<Categoria | null>(null);
  protected readonly nome = signal('');
  protected readonly paiId = signal<string | null>(null);
  protected readonly fonte = signal<FonteReceita>('ATIVA');
  protected readonly salvando = signal(false);
  protected readonly tentouSalvar = signal(false);

  protected readonly categorias = rxResource({ stream: () => this.categoriasApi.listar() });

  protected readonly todas = computed(() => valorDe(this.categorias) ?? []);
  protected readonly temInativas = computed(() =>
    this.todas().some((c) => c.tipo === this.tipo() && !c.ativa),
  );

  protected readonly grupos = computed<GrupoDeCategorias[]>(() => {
    const doTipo = this.todas().filter(
      (c) => c.tipo === this.tipo() && (c.ativa || this.mostrarInativas()),
    );
    const porNome = (a: Categoria, b: Categoria) => a.nome.localeCompare(b.nome, 'pt-BR');
    return doTipo
      .filter((c) => !c.categoriaPaiId)
      .sort(porNome)
      .map((categoria) => ({
        categoria,
        subcategorias: doTipo.filter((c) => c.categoriaPaiId === categoria.id).sort(porNome),
      }));
  });

  protected readonly opcoesDePai = computed(() => {
    const editando = this.emEdicao();
    return this.todas()
      .filter(
        (c) => c.tipo === this.tipo() && c.ativa && !c.categoriaPaiId && c.id !== editando?.id,
      )
      .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
      .map((c) => ({ label: c.nome, value: c.id }));
  });

  protected readonly tituloDoDialogo = computed(() =>
    this.emEdicao()
      ? 'Editar categoria'
      : `Nova categoria de ${this.tipo() === 'DESPESA' ? 'despesa' : 'receita'}`,
  );
  protected readonly mostraFonte = computed(() => this.tipo() === 'RECEITA' && !this.paiId());
  protected readonly erroDoNome = computed(() =>
    this.nome().trim() ? '' : 'Informe o nome da categoria.',
  );

  protected escolherTipo(tipo: TipoCategoria): void {
    this.tipo.set(tipo);
  }

  protected novaCategoria(paiId: string | null = null): void {
    this.emEdicao.set(null);
    this.nome.set('');
    this.paiId.set(paiId);
    this.fonte.set('ATIVA');
    this.tentouSalvar.set(false);
    this.dialogoAberto.set(true);
  }

  protected editar(categoria: Categoria): void {
    this.emEdicao.set(categoria);
    this.nome.set(categoria.nome);
    this.paiId.set(categoria.categoriaPaiId);
    this.fonte.set(categoria.fonteReceita ?? 'ATIVA');
    this.tentouSalvar.set(false);
    this.dialogoAberto.set(true);
  }

  protected fecharDialogo(): void {
    this.dialogoAberto.set(false);
  }

  protected abrirAcoes(menu: Menu, evento: Event, categoria: Categoria, ehGrupo: boolean): void {
    const itens: MenuItem[] = categoria.ativa
      ? [
          { label: 'Editar', command: () => this.editar(categoria) },
          ...(ehGrupo
            ? [{ label: 'Nova subcategoria', command: () => this.novaCategoria(categoria.id) }]
            : []),
          { label: 'Inativar', command: () => this.confirmarInativacao(categoria) },
        ]
      : [{ label: 'Reativar', command: () => this.reativar(categoria) }];
    this.acoes.set(itens);
    menu.toggle(evento);
  }

  protected salvar(): void {
    this.tentouSalvar.set(true);
    if (this.erroDoNome() || this.salvando()) {
      return;
    }
    const editando = this.emEdicao();
    const request = {
      nome: this.nome().trim(),
      tipo: editando?.tipo ?? this.tipo(),
      categoriaPaiId: this.paiId(),
      fonteReceita: this.mostraFonte() ? this.fonte() : null,
    };
    const pedido: Observable<Categoria> = editando
      ? this.categoriasApi.atualizar(editando.id, request)
      : this.categoriasApi.criar(request);
    this.salvando.set(true);
    pedido.subscribe({
      next: (categoria) => {
        this.salvando.set(false);
        this.dialogoAberto.set(false);
        this.avisos.sucesso(editando ? 'Categoria atualizada' : 'Categoria criada', categoria.nome);
        this.categorias.reload();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.avisos.erro('Não foi possível salvar a categoria', erro);
      },
    });
  }

  protected confirmarInativacao(categoria: Categoria): void {
    this.confirmacao.confirm({
      header: `Inativar ${categoria.nome}?`,
      message:
        'A categoria some das listas de lançamento. O histórico continua guardado e você pode reativar depois.',
      acceptLabel: 'Inativar',
      rejectLabel: 'Manter',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.categoriasApi.inativar(categoria.id).subscribe({
          next: () => {
            this.avisos.sucesso('Categoria inativada', categoria.nome);
            this.categorias.reload();
          },
          error: (erro: unknown) => this.avisos.erro('Não foi possível inativar', erro),
        }),
    });
  }

  protected reativar(categoria: Categoria): void {
    this.categoriasApi.ativar(categoria.id).subscribe({
      next: () => {
        this.avisos.sucesso('Categoria reativada', categoria.nome);
        this.categorias.reload();
      },
      error: (erro: unknown) => this.avisos.erro('Não foi possível reativar', erro),
    });
  }
}
