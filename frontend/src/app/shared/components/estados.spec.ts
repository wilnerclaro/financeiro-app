import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Receipt } from 'lucide';
import { MesDoAno } from '../util/datas';
import { EstadoErro } from './estado-erro/estado-erro';
import { EstadoVazio } from './estado-vazio/estado-vazio';
import { SeletorMes } from './seletor-mes/seletor-mes';

@Component({
  imports: [EstadoErro, EstadoVazio, SeletorMes],
  template: `
    <app-estado-erro
      titulo="Falhou."
      mensagem="Tente de novo."
      (tentar)="tentativas = tentativas + 1"
    />
    <app-estado-vazio [icone]="icone" titulo="Nada aqui" texto="Lance algo." />
    <app-seletor-mes [(mes)]="mes" />
  `,
})
class Anfitriao {
  icone = Receipt;
  tentativas = 0;
  mes = signal<MesDoAno>({ ano: 2026, mes: 1 });
}

describe('componentes de estado e seletor de mês', () => {
  it('mostram o conteúdo e reagem aos cliques', async () => {
    const fixture = TestBed.createComponent(Anfitriao);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Falhou.');
    expect(el.textContent).toContain('Nada aqui');
    expect(el.textContent).toContain('Janeiro de 2026');

    el.querySelector<HTMLButtonElement>('app-estado-erro button')!.click();
    expect(fixture.componentInstance.tentativas).toBe(1);

    el.querySelector<HTMLButtonElement>('[aria-label="Ir para dezembro de 2025"]')!.click();
    await fixture.whenStable();
    expect(fixture.componentInstance.mes()).toEqual({ ano: 2025, mes: 12 });

    el.querySelector<HTMLButtonElement>('[aria-label="Ir para janeiro de 2026"]')!.click();
    await fixture.whenStable();
    expect(el.textContent).toContain('Janeiro de 2026');
  });
});
