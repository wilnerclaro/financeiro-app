import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { EllipsisVertical, Plus, Wallet } from 'lucide';
import { ConfirmationService, MenuItem } from 'primeng/api';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Menu } from 'primeng/menu';
import { Skeleton } from 'primeng/skeleton';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { forkJoin } from 'rxjs';
import { Conta } from '../../../../shared/api/api.models';
import { ContasApi } from '../../../../shared/api/contas-api';
import { EstadoErro } from '../../../../shared/components/estado-erro/estado-erro';
import { EstadoVazio } from '../../../../shared/components/estado-vazio/estado-vazio';
import { Icone } from '../../../../shared/components/icone/icone';
import { Avisos } from '../../../../shared/util/avisos';
import { brl, variacao } from '../../../../shared/util/formatacao';
import { ContaDialogo, ModoDoDialogo } from '../../components/conta-dialogo/conta-dialogo';
import { FaturaDialogo } from '../../components/fatura-dialogo/fatura-dialogo';
import { rotuloDoTipo } from '../../tipos-de-conta';
import { valorDe } from '../../../../shared/util/recurso';

@Component({
  selector: 'app-contas',
  imports: [
    FormsModule,
    Button,
    ConfirmDialog,
    Menu,
    Skeleton,
    ToggleSwitch,
    EstadoErro,
    EstadoVazio,
    Icone,
    ContaDialogo,
    FaturaDialogo,
  ],
  providers: [ConfirmationService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './contas.html',
  styleUrl: './contas.scss',
})
export class Contas {
  private readonly contasApi = inject(ContasApi);
  private readonly confirmacao = inject(ConfirmationService);
  private readonly avisos = inject(Avisos);

  protected readonly icones = { EllipsisVertical, Plus, Wallet };
  protected readonly brl = brl;
  protected readonly variacao = variacao;
  protected readonly rotuloDoTipo = rotuloDoTipo;

  protected readonly mostrarInativas = signal(false);
  protected readonly acoes = signal<MenuItem[]>([]);
  protected readonly modo = signal<ModoDoDialogo | null>(null);
  protected readonly emEdicao = signal<Conta | null>(null);
  protected readonly cartaoDaFatura = signal<Conta | null>(null);

  protected readonly dados = rxResource({
    stream: () => forkJoin({ contas: this.contasApi.listar(), resumo: this.contasApi.resumo() }),
  });

  protected readonly todas = computed(() => valorDe(this.dados)?.contas ?? []);
  protected readonly visiveis = computed(() =>
    this.todas().filter((c) => c.ativa || this.mostrarInativas()),
  );
  protected readonly temInativas = computed(() => this.todas().some((c) => !c.ativa));
  protected readonly resumo = computed(() => valorDe(this.dados)?.resumo);

  protected novaConta(): void {
    this.emEdicao.set(null);
    this.modo.set('criar');
  }

  protected abrirAcoes(menu: Menu, evento: Event, conta: Conta): void {
    const itens: MenuItem[] = conta.ativa
      ? [
          { label: 'Editar', command: () => this.abrirDialogo('editar', conta) },
          { label: 'Corrigir saldo inicial', command: () => this.abrirDialogo('saldo', conta) },
          { label: 'Inativar', command: () => this.confirmarInativacao(conta) },
        ]
      : [{ label: 'Reativar', command: () => this.reativar(conta) }];
    this.acoes.set(itens);
    menu.toggle(evento);
  }

  protected abrirDialogo(modo: ModoDoDialogo, conta: Conta): void {
    this.emEdicao.set(conta);
    this.modo.set(modo);
  }

  protected fecharDialogo(): void {
    this.modo.set(null);
    this.emEdicao.set(null);
  }

  protected aoSalvar(): void {
    this.fecharDialogo();
    this.dados.reload();
  }

  protected confirmarInativacao(conta: Conta): void {
    this.confirmacao.confirm({
      header: `Inativar ${conta.nome}?`,
      message:
        'A conta some das listas e não recebe novos lançamentos. O histórico continua guardado e você pode reativar depois.',
      acceptLabel: 'Inativar',
      rejectLabel: 'Manter',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.contasApi.inativar(conta.id).subscribe({
          next: () => {
            this.avisos.sucesso('Conta inativada', conta.nome);
            this.dados.reload();
          },
          error: (erro: unknown) => this.avisos.erro('Não foi possível inativar', erro),
        }),
    });
  }

  protected reativar(conta: Conta): void {
    this.contasApi.ativar(conta.id).subscribe({
      next: () => {
        this.avisos.sucesso('Conta reativada', conta.nome);
        this.dados.reload();
      },
      error: (erro: unknown) => this.avisos.erro('Não foi possível reativar', erro),
    });
  }
}
