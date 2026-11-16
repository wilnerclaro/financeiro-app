import {
  competenciaIso,
  deDate,
  hojeIso,
  mesAtual,
  paraDate,
  primeiroDia,
  somarMeses,
  ultimoDia,
} from './datas';

describe('datas', () => {
  const dia = new Date(2026, 0, 31);

  it('converte a data local em ISO e volta', () => {
    expect(hojeIso(dia)).toBe('2026-01-31');
    expect(deDate(paraDate('2026-02-03'))).toBe('2026-02-03');
  });

  it('acha o mês atual e soma meses na virada do ano', () => {
    expect(mesAtual(dia)).toEqual({ ano: 2026, mes: 1 });
    expect(somarMeses({ ano: 2026, mes: 1 }, -1)).toEqual({ ano: 2025, mes: 12 });
    expect(somarMeses({ ano: 2026, mes: 12 }, 1)).toEqual({ ano: 2027, mes: 1 });
  });

  it('dá o primeiro e o último dia do mês', () => {
    expect(primeiroDia({ ano: 2028, mes: 2 })).toBe('2028-02-01');
    expect(ultimoDia({ ano: 2028, mes: 2 })).toBe('2028-02-29');
    expect(competenciaIso({ ano: 2026, mes: 7 })).toBe('2026-07');
  });
});
