export type NivelDoOrcamento = 'ok' | 'amb' | 'lar' | 'des';

const STATUS: Record<NivelDoOrcamento, string> = {
  ok: 'No ritmo',
  amb: 'Atenção',
  lar: 'No limite',
  des: 'Acima do previsto',
};

export function nivelDoOrcamento(fracaoUsada: number): NivelDoOrcamento {
  if (fracaoUsada > 1) {
    return 'des';
  }
  if (fracaoUsada >= 0.9) {
    return 'lar';
  }
  if (fracaoUsada >= 0.7) {
    return 'amb';
  }
  return 'ok';
}

export function statusDoOrcamento(fracaoUsada: number): string {
  return STATUS[nivelDoOrcamento(fracaoUsada)];
}
