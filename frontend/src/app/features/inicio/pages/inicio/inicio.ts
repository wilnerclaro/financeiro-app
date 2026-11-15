import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { LayoutDashboard } from 'lucide';
import { Sessao } from '../../../../core/auth/sessao';
import { Icone } from '../../../../shared/components/icone/icone';

@Component({
  selector: 'app-inicio',
  imports: [Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pagina surgir">
      <header>
        <h1 class="titulo-pagina">Olá, {{ primeiroNome() }}</h1>
        <p class="aux">Seu controle financeiro, mês a mês.</p>
      </header>
      <section class="vazio" aria-labelledby="vazio-titulo">
        <span class="vazio-icone"><app-icone [icone]="iconeVazio" [tamanho]="24" /></span>
        <h2 id="vazio-titulo" class="titulo-secao">O resumo do mês chega na próxima versão</h2>
        <p class="aux">Indicadores, pendências e orçamento vão aparecer aqui.</p>
      </section>
    </div>
  `,
  styles: `
    .pagina {
      display: flex;
      flex-direction: column;
      gap: var(--e8);
    }

    .vazio {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--e3);
      padding: var(--e12) var(--e4);
      border: 1px dashed var(--p-content-border-color);
      border-radius: var(--r-sup);
      text-align: center;
    }

    .vazio-icone {
      display: grid;
      place-items: center;
      width: 48px;
      height: 48px;
      border-radius: 99px;
      background: var(--p-content-hover-background);
      color: var(--p-text-muted-color);
    }
  `,
})
export class Inicio {
  private readonly sessao = inject(Sessao);
  protected readonly iconeVazio = LayoutDashboard;
  protected readonly primeiroNome = computed(() => this.sessao.usuario()?.nome.split(' ')[0] ?? '');
}
