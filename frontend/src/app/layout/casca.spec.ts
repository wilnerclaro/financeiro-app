import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { UsuarioResponse } from '../core/auth/autenticacao.models';
import { Sessao } from '../core/auth/sessao';
import { Tema } from '../core/config/tema';
import { Casca } from './casca';

describe('Casca', () => {
  let fixture: ComponentFixture<Casca>;
  const sair = vi.fn();
  const alternar = vi.fn();
  const usuario = signal<UsuarioResponse | null>({
    id: '1',
    nome: 'Ana Souza',
    email: 'ana@exemplo.com',
  });

  beforeEach(async () => {
    sair.mockReset();
    alternar.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: Sessao, useValue: { usuario, sair } },
        { provide: Tema, useValue: { escuro: signal(true), alternar } },
      ],
    });
    fixture = TestBed.createComponent(Casca);
    await fixture.whenStable();
  });

  function botao(rotulo: string): HTMLButtonElement {
    const botoes = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'),
    );
    return botoes.find(
      (b) => b.textContent?.includes(rotulo) || b.getAttribute('aria-label') === rotulo,
    )!;
  }

  it('mostra o nome de quem entrou', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ana Souza');
  });

  it('sai da conta pelo botão Sair', () => {
    botao('Sair').click();
    expect(sair).toHaveBeenCalledTimes(1);
  });

  it('troca o tema pelo botão de sol e lua', () => {
    botao('Usar tema claro').click();
    expect(alternar).toHaveBeenCalledTimes(1);
  });

  it('abre o novo lançamento pela tecla N fora de campos de texto', () => {
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'n' }));
    expect(navegar).toHaveBeenCalledWith('/lancar');

    navegar.mockClear();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'n', ctrlKey: true }));
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'x' }));
    const campo = document.createElement('input');
    document.body.appendChild(campo);
    campo.dispatchEvent(new KeyboardEvent('keydown', { key: 'N', bubbles: true }));
    campo.remove();
    expect(navegar).not.toHaveBeenCalled();
  });
});
