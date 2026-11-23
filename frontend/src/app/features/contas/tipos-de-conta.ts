import { TipoConta } from '../../shared/api/api.models';

export const TIPOS_DE_CONTA: { label: string; value: TipoConta }[] = [
  { label: 'Conta corrente', value: 'CONTA_CORRENTE' },
  { label: 'Poupança', value: 'POUPANCA' },
  { label: 'Dinheiro', value: 'DINHEIRO' },
  { label: 'Investimento', value: 'INVESTIMENTO' },
  { label: 'Cartão de crédito', value: 'CARTAO_CREDITO' },
  { label: 'Outra', value: 'OUTRA' },
];

export function rotuloDoTipo(tipo: TipoConta): string {
  return TIPOS_DE_CONTA.find((t) => t.value === tipo)?.label ?? tipo;
}
