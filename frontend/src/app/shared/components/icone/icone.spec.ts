import { TestBed } from '@angular/core/testing';
import { House, Sun } from 'lucide';
import { Icone } from './icone';

describe('Icone', () => {
  it('desenha o ícone Lucide com 16px e traço 1,75, escondido do leitor de tela', async () => {
    const fixture = TestBed.createComponent(Icone);
    fixture.componentRef.setInput('icone', House);
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    const svg = host.querySelector('svg')!;
    expect(host.getAttribute('aria-hidden')).toBe('true');
    expect(svg.getAttribute('width')).toBe('16');
    expect(svg.getAttribute('stroke-width')).toBe('1.75');
    expect(svg.querySelectorAll('path')).toHaveLength(House.length);
  });

  it('troca o desenho quando o ícone ou o tamanho mudam', async () => {
    const fixture = TestBed.createComponent(Icone);
    fixture.componentRef.setInput('icone', House);
    await fixture.whenStable();

    fixture.componentRef.setInput('icone', Sun);
    fixture.componentRef.setInput('tamanho', 24);
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelectorAll('svg')).toHaveLength(1);
    expect(host.querySelector('svg')!.getAttribute('height')).toBe('24');
    expect(host.querySelector('svg circle')).not.toBeNull();
  });
});
