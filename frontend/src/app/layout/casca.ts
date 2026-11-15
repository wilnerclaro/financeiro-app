import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ChartLine, House, LogOut, Moon, Sun } from 'lucide';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { Sessao } from '../core/auth/sessao';
import { Tema } from '../core/config/tema';
import { Icone } from '../shared/components/icone/icone';

@Component({
  selector: 'app-casca',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Button, Tooltip, Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './casca.html',
  styleUrl: './casca.scss',
})
export class Casca {
  private readonly sessao = inject(Sessao);
  protected readonly tema = inject(Tema);
  protected readonly icones = { ChartLine, House, LogOut, Moon, Sun };
  protected readonly nomeDoUsuario = computed(() => this.sessao.usuario()?.nome ?? '');

  protected sair(): void {
    this.sessao.sair();
  }
}
