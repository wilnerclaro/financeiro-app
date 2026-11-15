import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Sessao } from '../../../../core/auth/sessao';
import { Inicio } from './inicio';

describe('Inicio', () => {
  it('cumprimenta pelo primeiro nome', async () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: Sessao,
          useValue: { usuario: signal({ id: '1', nome: 'Ana Souza', email: 'ana@exemplo.com' }) },
        },
      ],
    });
    const fixture = TestBed.createComponent(Inicio);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain(
      'Olá, Ana',
    );
  });
});
