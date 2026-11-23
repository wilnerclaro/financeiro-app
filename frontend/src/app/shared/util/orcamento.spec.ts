import { nivelDoOrcamento, statusDoOrcamento } from './orcamento';

describe('escala do orçamento', () => {
  it.each([
    [0.69, 'ok', 'No ritmo'],
    [0.7, 'amb', 'Atenção'],
    [0.9, 'lar', 'No limite'],
    [1, 'lar', 'No limite'],
    [1.01, 'des', 'Acima do previsto'],
  ])('%s do previsto é %s', (fracao, nivel, status) => {
    expect(nivelDoOrcamento(fracao)).toBe(nivel);
    expect(statusDoOrcamento(fracao)).toBe(status);
  });
});
