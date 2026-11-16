import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-abas-orcamento',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="abas" aria-label="Visões do orçamento">
      <a
        routerLink="/orcamento"
        routerLinkActive="ativa"
        [routerLinkActiveOptions]="{ exact: true }"
        >Mês</a
      >
      <a routerLink="/orcamento/ano" routerLinkActive="ativa">Ano</a>
      <a routerLink="/orcamento/previsto" routerLinkActive="ativa">Editar previsto</a>
    </nav>
  `,
  styles: `
    .abas {
      display: flex;
      gap: var(--e1);
      border-bottom: 1px solid var(--p-content-border-color);
    }

    a {
      padding: var(--e2) var(--e3);
      border-bottom: 2px solid transparent;
      color: var(--p-text-muted-color);
      font-size: 14px;
      font-weight: 500;
      text-decoration: none;
    }

    a.ativa {
      border-bottom-color: var(--acao);
      color: var(--p-text-color);
    }

    a:focus-visible {
      outline: 2px solid var(--p-primary-color);
      outline-offset: 2px;
    }
  `,
})
export class AbasOrcamento {}
