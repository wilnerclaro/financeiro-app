import {
  brl,
  brlComSinal,
  competenciaCurta,
  dataCompleta,
  dataCurta,
  percentual,
  rotuloDoMes,
  variacao,
} from './formatacao';

const espacos = (texto: string) => texto.replace(/\s/g, ' ');

describe('formatação', () => {
  it('formata dinheiro em reais', () => {
    expect(espacos(brl(1234.5))).toBe('R$ 1.234,50');
  });

  it('põe sinal de receita e despesa', () => {
    expect(espacos(brlComSinal(10, 'RECEITA'))).toBe('+ R$ 10,00');
    expect(espacos(brlComSinal(10, 'DESPESA'))).toBe('− R$ 10,00');
  });

  it('põe sinal na variação pelo valor', () => {
    expect(espacos(variacao(-20))).toBe('− R$ 20,00');
    expect(espacos(variacao(0))).toBe('+ R$ 0,00');
  });

  it('formata a fração como percentual sem recalcular', () => {
    expect(espacos(percentual(0.2534))).toBe('25,3%');
    expect(espacos(percentual(1))).toBe('100%');
  });

  it('mostra datas no formato brasileiro', () => {
    expect(dataCurta('2026-11-05')).toBe('05/11');
    expect(dataCompleta('2026-11-05')).toBe('05/11/2026');
  });

  it('nomeia o mês', () => {
    expect(rotuloDoMes(2026, 3)).toBe('Março de 2026');
    expect(competenciaCurta(2026, 12)).toBe('dez/2026');
  });
});
