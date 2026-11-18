import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EsqueletoGrafico, FormaDoGrafico, mascaraDoGrafico } from './esqueleto/esqueleto-grafico';
import { EsqueletoLista } from './esqueleto/esqueleto-lista';

const desenho = (mascara: string) =>
  decodeURIComponent(mascara.replace(/^url\("data:image\/svg\+xml,/, '').replace(/"\)$/, ''));

const contar = (svg: string, marca: string) => svg.split(`<${marca}`).length - 1;

describe('esqueleto de gráfico', () => {
  it('desenha uma barra e um rótulo por coluna', () => {
    const svg = desenho(mascaraDoGrafico('barras', 6, 200));

    expect(svg).toContain('viewBox="0 0 600 200"');
    expect(svg).toContain('preserveAspectRatio="none"');
    expect(contar(svg, 'rect')).toBe(12);
  });

  it('desenha realizado, previsto tracejado, área, grade e rótulos dos meses', () => {
    const svg = desenho(mascaraDoGrafico('linhas', 12, 220));

    expect(contar(svg, 'polyline')).toBe(2);
    expect(svg).toContain('stroke-dasharray="6 6"');
    expect(contar(svg, 'polygon')).toBe(1);
    expect(contar(svg, 'line ')).toBe(4);
    expect(contar(svg, 'rect')).toBe(12);
  });

  it('desenha o anel sem distorcer', () => {
    const svg = desenho(mascaraDoGrafico('anel', 6, 200));

    expect(svg).toContain('viewBox="0 0 120 120"');
    expect(svg).not.toContain('preserveAspectRatio');
    expect(contar(svg, 'circle')).toBe(1);
  });

  it('fica fora do leitor de tela e ocupa a altura pedida', async () => {
    @Component({
      imports: [EsqueletoGrafico],
      template: `<app-esqueleto-grafico [forma]="forma()" [altura]="180" />`,
    })
    class Anfitriao {
      forma = signal<FormaDoGrafico>('barras');
    }

    const fixture = TestBed.createComponent(Anfitriao);
    await fixture.whenStable();
    const grafico = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>(
      'app-esqueleto-grafico',
    )!;

    expect(grafico.getAttribute('aria-hidden')).toBe('true');
    expect(grafico.style.height).toBe('180px');

    fixture.componentInstance.forma.set('anel');
    await fixture.whenStable();
    expect(grafico.style.height).toBe('100%');
    expect(grafico.style.width).toBe('100%');
  });
});

describe('esqueleto de lista', () => {
  it('imita as linhas, o cabeçalho, o selo e as colunas de valor', async () => {
    @Component({
      imports: [EsqueletoLista],
      template: `
        <app-esqueleto-lista
          [linhas]="4"
          [icone]="true"
          [selo]="true"
          [colunas]="3"
          [cabecalho]="true"
          [recuadas]="[1, 2]"
        />
      `,
    })
    class Anfitriao {}

    const fixture = TestBed.createComponent(Anfitriao);
    await fixture.whenStable();
    const lista = (fixture.nativeElement as HTMLElement).querySelector('app-esqueleto-lista')!;
    const linhas = lista.querySelectorAll('li:not(.cab)');

    expect(lista.getAttribute('aria-hidden')).toBe('true');
    expect(lista.querySelectorAll('li.cab')).toHaveLength(1);
    expect(linhas).toHaveLength(4);
    expect(lista.querySelectorAll('li.recuada')).toHaveLength(2);
    expect(linhas[0].querySelectorAll('.p-skeleton-circle')).toHaveLength(1);
    expect(linhas[0].querySelectorAll('.coluna')).toHaveLength(3);
    expect(linhas[0].querySelectorAll('.selo')).toHaveLength(1);
  });

  it('mostra só o título quando não há detalhe', async () => {
    @Component({
      imports: [EsqueletoLista],
      template: `<app-esqueleto-lista [linhas]="2" [detalhe]="false" />`,
    })
    class Anfitriao {}

    const fixture = TestBed.createComponent(Anfitriao);
    await fixture.whenStable();
    const linha = (fixture.nativeElement as HTMLElement).querySelector('li')!;

    expect(linha.querySelectorAll('.txt p-skeleton')).toHaveLength(1);
    expect(linha.querySelectorAll('.coluna')).toHaveLength(1);
  });
});
