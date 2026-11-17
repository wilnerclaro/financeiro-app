import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  ChartLine,
  ChartPie,
  House,
  IconNode,
  List,
  LogOut,
  Moon,
  Plus,
  Sun,
  Tags,
  Upload,
  Wallet,
} from 'lucide';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { Sessao } from '../core/auth/sessao';
import { Tema } from '../core/config/tema';
import { Icone } from '../shared/components/icone/icone';

interface ItemDoMenu {
  rota: string;
  rotulo: string;
  icone: IconNode;
}

const MENU: ItemDoMenu[] = [
  { rota: '/', rotulo: 'Início', icone: House },
  { rota: '/lancamentos', rotulo: 'Lançamentos', icone: List },
  { rota: '/orcamento', rotulo: 'Orçamento', icone: ChartPie },
  { rota: '/categorias', rotulo: 'Categorias', icone: Tags },
  { rota: '/contas', rotulo: 'Contas', icone: Wallet },
];

@Component({
  selector: 'app-casca',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Button, Tooltip, Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './casca.html',
  styleUrl: './casca.scss',
  host: { '(document:keydown)': 'atalho($event)' },
})
export class Casca {
  private readonly sessao = inject(Sessao);
  private readonly router = inject(Router);
  protected readonly tema = inject(Tema);
  protected readonly icones = { ChartLine, LogOut, Moon, Plus, Sun, Upload };
  protected readonly menu = MENU;
  protected readonly menuDoCelular: ItemDoMenu[] = [
    MENU[0],
    MENU[1],
    { rota: '/lancar', rotulo: 'Lançar', icone: Plus },
    MENU[2],
    MENU[4],
  ];
  protected readonly nomeDoUsuario = computed(() => this.sessao.usuario()?.nome ?? '');

  protected sair(): void {
    this.sessao.sair();
  }

  protected atalho(evento: KeyboardEvent): void {
    if (evento.key.toLowerCase() !== 'n' || evento.ctrlKey || evento.metaKey || evento.altKey) {
      return;
    }
    const alvo = evento.target;
    if (
      alvo instanceof Element &&
      alvo.closest('input, textarea, select, [contenteditable="true"], [role="dialog"]')
    ) {
      return;
    }
    evento.preventDefault();
    void this.router.navigateByUrl('/lancar');
  }
}
