import { HttpParams } from '@angular/common/http';

export function parametros(
  valores: Record<string, string | number | boolean | undefined>,
): HttpParams {
  let params = new HttpParams();
  for (const [chave, valor] of Object.entries(valores)) {
    if (valor !== undefined && valor !== '') {
      params = params.set(chave, valor);
    }
  }
  return params;
}
