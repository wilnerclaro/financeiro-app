export interface MesDoAno {
  ano: number;
  mes: number;
}

export function hojeIso(agora = new Date()): string {
  const ano = agora.getFullYear();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

export function mesAtual(agora = new Date()): MesDoAno {
  return { ano: agora.getFullYear(), mes: agora.getMonth() + 1 };
}

export function somarMeses({ ano, mes }: MesDoAno, quantidade: number): MesDoAno {
  const indice = ano * 12 + (mes - 1) + quantidade;
  return { ano: Math.floor(indice / 12), mes: (indice % 12) + 1 };
}

export function primeiroDia({ ano, mes }: MesDoAno): string {
  return `${ano}-${String(mes).padStart(2, '0')}-01`;
}

export function ultimoDia({ ano, mes }: MesDoAno): string {
  const dia = new Date(ano, mes, 0).getDate();
  return `${ano}-${String(mes).padStart(2, '0')}-${String(dia).padStart(2, '0')}`;
}

export function competenciaIso({ ano, mes }: MesDoAno): string {
  return `${ano}-${String(mes).padStart(2, '0')}`;
}

export function deDate(data: Date): string {
  return hojeIso(data);
}

export function paraDate(iso: string): Date {
  const [ano, mes, dia] = iso.split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}
