import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Tema } from './tema';

describe('Tema', () => {
  let documento: Document;

  beforeEach(() => {
    localStorage.clear();
    documento = TestBed.inject(DOCUMENT);
    documento.documentElement.classList.remove('app-dark');
  });

  afterEach(() => vi.unstubAllGlobals());

  function criar(): Tema {
    const tema = TestBed.inject(Tema);
    TestBed.tick();
    return tema;
  }

  it('começa no tema escuro quando não há escolha salva', () => {
    const tema = criar();
    expect(tema.escuro()).toBe(true);
    expect(documento.documentElement.classList.contains('app-dark')).toBe(true);
  });

  it('usa o tema claro quando ele foi escolhido antes', () => {
    localStorage.setItem('financeiro.tema', 'claro');
    const tema = criar();
    expect(tema.escuro()).toBe(false);
    expect(documento.documentElement.classList.contains('app-dark')).toBe(false);
  });

  it('alterna o tema, aplica a classe e guarda a escolha', () => {
    localStorage.setItem('financeiro.tema', 'claro');
    const tema = criar();

    tema.alternar();
    TestBed.tick();

    expect(tema.escuro()).toBe(true);
    expect(documento.documentElement.classList.contains('app-dark')).toBe(true);
    expect(localStorage.getItem('financeiro.tema')).toBe('escuro');
  });

  it('funciona mesmo quando o navegador bloqueia o armazenamento', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => {
        throw new Error('bloqueado');
      },
      setItem: () => {
        throw new Error('bloqueado');
      },
    });
    const tema = criar();

    expect(tema.escuro()).toBe(true);
    expect(() => tema.alternar()).not.toThrow();
    expect(tema.escuro()).toBe(false);
  });
});
