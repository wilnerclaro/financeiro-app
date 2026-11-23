import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChevronDown, Search } from 'lucide';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { SelectButton } from 'primeng/selectbutton';
import { TipoCategoria } from '../../../../shared/api/api.models';
import { Icone } from '../../../../shared/components/icone/icone';
import { brl } from '../../../../shared/util/formatacao';
import { LinhaInicial } from '../../pages/configuracao-inicial/orcamento-inicial';

export interface BlocoDoPrevisto {
  titulo: string;
  tipo: TipoCategoria;
  linhas: LinhaInicial[];
}

export interface EdicaoDoPrevisto {
  id: string;
  valor: number | null;
}

interface GrupoDoPrevisto {
  raiz: LinhaInicial;
  filhos: LinhaInicial[];
}

const normalizar = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .trim();

function agrupar(linhas: LinhaInicial[]): GrupoDoPrevisto[] {
  const grupos: GrupoDoPrevisto[] = [];
  for (const linha of linhas) {
    if (linha.raiz || !grupos.length) {
      grupos.push({ raiz: linha, filhos: [] });
    } else {
      grupos[grupos.length - 1].filhos.push(linha);
    }
  }
  return grupos;
}

function filtrar(grupos: GrupoDoPrevisto[], termo: string): GrupoDoPrevisto[] {
  if (!termo) {
    return grupos;
  }
  const encontra = (linha: LinhaInicial) => normalizar(linha.nome).includes(termo);
  return grupos
    .map((grupo) =>
      encontra(grupo.raiz) ? grupo : { ...grupo, filhos: grupo.filhos.filter(encontra) },
    )
    .filter((grupo) => encontra(grupo.raiz) || grupo.filhos.length);
}

@Component({
  selector: 'app-previsto-por-grupo',
  imports: [
    FormsModule,
    NgTemplateOutlet,
    IconField,
    InputIcon,
    InputNumber,
    InputText,
    SelectButton,
    Icone,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './previsto-por-grupo.html',
  styleUrl: './previsto-por-grupo.scss',
})
export class PrevistoPorGrupo {
  readonly blocos = input.required<BlocoDoPrevisto[]>();
  readonly editados = input<ReadonlyMap<string, number>>(new Map());
  readonly desabilitado = input(false);
  readonly editar = output<EdicaoDoPrevisto>();

  protected readonly icones = { ChevronDown, Search };
  protected readonly brl = brl;
  protected readonly tipo = signal<TipoCategoria>('RECEITA');
  protected readonly busca = signal('');
  protected readonly abertos = signal<ReadonlySet<string>>(new Set());

  protected readonly opcoesDeTipo = computed(() =>
    this.blocos().map((b) => ({ label: b.titulo, value: b.tipo })),
  );
  private readonly termo = computed(() => normalizar(this.busca()));
  protected readonly buscando = computed(() => !!this.termo());
  protected readonly bloco = computed(() => this.blocos().find((b) => b.tipo === this.tipo()));
  protected readonly grupos = computed(() =>
    filtrar(agrupar(this.bloco()?.linhas ?? []), this.termo()),
  );
  protected readonly gruposComFilhos = computed(() => this.grupos().filter((g) => g.filhos.length));
  protected readonly todosAbertos = computed(
    () =>
      this.gruposComFilhos().length > 0 &&
      this.gruposComFilhos().every((g) => this.abertos().has(g.raiz.id)),
  );
  protected readonly resultadoNoOutroTipo = computed(() => {
    if (!this.buscando()) {
      return null;
    }
    const outro = this.blocos().find((b) => b.tipo !== this.tipo());
    const quantidade =
      outro?.linhas.filter((l) => normalizar(l.nome).includes(this.termo())).length ?? 0;
    return outro && quantidade ? { titulo: outro.titulo, tipo: outro.tipo, quantidade } : null;
  });

  protected aberto(grupo: GrupoDoPrevisto): boolean {
    return this.buscando() || this.abertos().has(grupo.raiz.id);
  }

  protected alternar(grupo: GrupoDoPrevisto): void {
    if (this.buscando()) {
      return;
    }
    this.abertos.update((abertos) => {
      const proximos = new Set(abertos);
      if (!proximos.delete(grupo.raiz.id)) {
        proximos.add(grupo.raiz.id);
      }
      return proximos;
    });
  }

  protected alternarTodos(): void {
    const ids = this.gruposComFilhos().map((g) => g.raiz.id);
    const abrir = !this.todosAbertos();
    this.abertos.update((abertos) => {
      const proximos = new Set(abertos);
      ids.forEach((id) => (abrir ? proximos.add(id) : proximos.delete(id)));
      return proximos;
    });
  }

  protected trocarTipo(tipo: TipoCategoria): void {
    this.tipo.set(tipo);
  }

  protected valor(linha: LinhaInicial): number {
    return this.editados().get(linha.id) ?? linha.previsto;
  }

  protected totalDoGrupo(grupo: GrupoDoPrevisto): number {
    const todos = agrupar(this.bloco()?.linhas ?? []).find((g) => g.raiz.id === grupo.raiz.id);
    const delta = (todos?.filhos ?? []).reduce(
      (soma, l) => soma + Math.round(this.valor(l) * 100) - Math.round(l.previsto * 100),
      0,
    );
    return (Math.round(grupo.raiz.previsto * 100) + delta) / 100;
  }

  protected selecionarValor(evento: Event): void {
    if (evento.target instanceof HTMLInputElement) evento.target.select();
  }
}
