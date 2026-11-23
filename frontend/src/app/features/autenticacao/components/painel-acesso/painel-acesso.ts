import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Check } from 'lucide';
import { Icone } from '../../../../shared/components/icone/icone';

@Component({
  selector: 'app-painel-acesso',
  imports: [Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './painel-acesso.html',
  styleUrl: './painel-acesso.scss',
  host: { 'aria-hidden': 'true' },
})
export class PainelAcesso {
  protected readonly check = Check;
  protected readonly barras = [
    { altura: 52 },
    { altura: 70 },
    { altura: 58, baixa: true },
    { altura: 92 },
    { altura: 74, baixa: true },
    { altura: 90 },
  ];
  protected readonly meses = ['jun', 'jul', 'ago', 'set', 'out', 'nov'];
}
