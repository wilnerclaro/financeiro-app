import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { ImportacaoDeExtrato, abrirImportacaoGuard } from './importacao-de-extrato';

describe('ImportacaoDeExtrato', () => {
  let importacao: ImportacaoDeExtrato;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    importacao = TestBed.inject(ImportacaoDeExtrato);
  });

  it('abre com a conta sugerida e fecha', () => {
    importacao.abrir('c1');
    expect(importacao.aberta()).toBe(true);
    expect(importacao.contaSugerida()).toBe('c1');
    importacao.fechar();
    expect(importacao.aberta()).toBe(false);
    importacao.abrir();
    expect(importacao.contaSugerida()).toBeNull();
  });

  it('conta as importações concluídas para as telas recarregarem', () => {
    importacao.registrarConclusao();
    importacao.registrarConclusao();
    expect(importacao.concluidas()).toBe(2);
  });

  it('abre o modal sobre Lançamentos quando alguém acessa /importar', () => {
    const destino = TestBed.runInInjectionContext(() =>
      abrirImportacaoGuard({} as never, {} as never),
    ) as UrlTree;
    expect(importacao.aberta()).toBe(true);
    expect(TestBed.inject(Router).serializeUrl(destino)).toBe('/lancamentos');
  });
});
