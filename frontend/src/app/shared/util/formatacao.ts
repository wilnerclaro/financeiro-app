const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const PERCENTUAL = new Intl.NumberFormat('pt-BR', {
  style: 'percent',
  minimumFractionDigits: 0,
  maximumFractionDigits: 1,
});

export const MESES_CURTOS = [
  'jan',
  'fev',
  'mar',
  'abr',
  'mai',
  'jun',
  'jul',
  'ago',
  'set',
  'out',
  'nov',
  'dez',
];

export const MESES_LONGOS = [
  'janeiro',
  'fevereiro',
  'março',
  'abril',
  'maio',
  'junho',
  'julho',
  'agosto',
  'setembro',
  'outubro',
  'novembro',
  'dezembro',
];

export function brl(valor: number): string {
  return MOEDA.format(valor);
}

export function brlComSinal(valor: number, tipo: 'RECEITA' | 'DESPESA'): string {
  return `${tipo === 'RECEITA' ? '+' : '−'} ${brl(valor)}`;
}

export function percentual(fracao: number): string {
  return PERCENTUAL.format(fracao);
}

export function dataCurta(iso: string): string {
  const [, mes, dia] = iso.split('-');
  return `${dia}/${mes}`;
}

export function dataCompleta(iso: string): string {
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}

export function rotuloDoMes(ano: number, mes: number): string {
  const nome = MESES_LONGOS[mes - 1];
  return `${nome.charAt(0).toUpperCase()}${nome.slice(1)} de ${ano}`;
}

export function competenciaCurta(ano: number, mes: number): string {
  return `${MESES_CURTOS[mes - 1]}/${ano}`;
}

export function variacao(valor: number): string {
  return `${valor < 0 ? '−' : '+'} ${brl(Math.abs(valor))}`;
}
