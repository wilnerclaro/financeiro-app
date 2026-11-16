import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ChevronLeft, ChevronRight } from 'lucide';
import { Button } from 'primeng/button';
import { Skeleton } from 'primeng/skeleton';
import { LinhaDoAno, TipoCategoria } from '../../../../shared/api/api.models';
import { OrcamentoApi } from '../../../../shared/api/orcamento-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { Icone } from '../../../../shared/components/icone/icone';
import { AGORA } from '../../../../shared/util/agora';
import { brl } from '../../../../shared/util/formatacao';
import { AbasOrcamento } from '../../components/abas-orcamento/abas-orcamento';
import { valorDe } from '../../../../shared/util/recurso';

interface LinhaDaTabela extends LinhaDoAno {
  subcategoria: boolean;
}

@Component({
  selector: 'app-orcamento-ano',
  imports: [Button, Skeleton, EstadoErro, Icone, AbasOrcamento],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pagina surgir">
      <header class="cab">
        <div>
          <h1 class="titulo-pagina">Orçamento de {{ ano() }}</h1>
          <p class="aux">Total do ano e média por mês de cada categoria.</p>
        </div>
        <div class="seletor" role="group" aria-label="Ano de referência">
          <p-button
            [text]="true"
            severity="secondary"
            size="small"
            [ariaLabel]="'Ir para ' + (ano() - 1)"
            (onClick)="ano.set(ano() - 1)"
          >
            <app-icone [icone]="icones.ChevronLeft" />
          </p-button>
          <span class="medio" aria-live="polite">{{ ano() }}</span>
          <p-button
            [text]="true"
            severity="secondary"
            size="small"
            [ariaLabel]="'Ir para ' + (ano() + 1)"
            (onClick)="ano.set(ano() + 1)"
          >
            <app-icone [icone]="icones.ChevronRight" />
          </p-button>
        </div>
      </header>

      <app-abas-orcamento />

      @if (resumo.error()) {
        <app-estado-erro
          [titulo]="'Não foi possível carregar o orçamento de ' + ano() + '.'"
          mensagem="Verifique a conexão e tente de novo."
          (tentar)="resumo.reload()"
        />
      } @else if (resumo.isLoading()) {
        <p-skeleton height="18rem" />
      } @else {
        @for (bloco of blocos(); track bloco.tipo) {
          <section class="secao" [attr.aria-labelledby]="'bloco-' + bloco.tipo">
            <h2 [id]="'bloco-' + bloco.tipo" class="titulo-secao">{{ bloco.titulo }}</h2>
            @if (bloco.linhas.length) {
              <div class="superficie rolagem">
                <table class="tabela-simples">
                  <thead>
                    <tr>
                      <th scope="col">Categoria</th>
                      <th scope="col" class="n">Previsto</th>
                      <th scope="col" class="n">Realizado</th>
                      <th scope="col" class="n">Média prevista</th>
                      <th scope="col" class="n">Média realizada</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (l of bloco.linhas; track l.categoriaId) {
                      <tr [class.grupo]="!l.subcategoria">
                        <th scope="row" [class.sub]="l.subcategoria">{{ l.nome }}</th>
                        <td class="n num">{{ brl(l.totalPrevisto) }}</td>
                        <td class="n num">{{ brl(l.totalRealizado) }}</td>
                        <td class="n num">{{ brl(l.mediaPrevisto) }}</td>
                        <td class="n num">{{ brl(l.mediaRealizado) }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            } @else {
              <p class="aux">Nada previsto ou realizado em {{ ano() }}.</p>
            }
          </section>
        }
      }
    </div>
  `,
  styles: `
    .seletor {
      display: inline-flex;
      align-items: center;
      gap: var(--e1);
      border: 1px solid var(--p-content-border-color);
      border-radius: var(--r-ctl);
    }

    .rolagem {
      overflow-x: auto;
    }

    th[scope='row'] {
      font-weight: 600;
      text-align: left;
    }

    th.sub {
      padding-left: var(--e8);
      font-weight: 400;
    }

    tr.grupo td {
      font-weight: 600;
    }
  `,
})
export class OrcamentoAno {
  private readonly orcamentoApi = inject(OrcamentoApi);
  private readonly agora = inject(AGORA);

  protected readonly icones = { ChevronLeft, ChevronRight };
  protected readonly brl = brl;
  protected readonly ano = signal(this.agora().getFullYear());

  protected readonly resumo = rxResource({
    params: () => this.ano(),
    stream: ({ params }) => this.orcamentoApi.doAno(params),
  });

  protected readonly blocos = computed(() => {
    const linhas = valorDe(this.resumo)?.linhas ?? [];
    return [
      { tipo: 'DESPESA', titulo: 'Despesas', linhas: this.linhasDo(linhas, 'DESPESA') },
      { tipo: 'RECEITA', titulo: 'Receitas', linhas: this.linhasDo(linhas, 'RECEITA') },
    ];
  });

  private linhasDo(linhas: LinhaDoAno[], tipo: TipoCategoria): LinhaDaTabela[] {
    return linhas
      .filter((l) => l.tipo === tipo && (l.totalPrevisto !== 0 || l.totalRealizado !== 0))
      .map((l) => ({
        ...l,
        subcategoria: !!l.categoriaPaiId,
      }));
  }
}
