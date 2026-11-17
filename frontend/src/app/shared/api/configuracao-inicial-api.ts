import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type EtapaConfiguracaoInicial =
  'BOAS_VINDAS' | 'OBJETIVO' | 'ORCAMENTO' | 'CONTAS' | 'LANCAMENTO' | 'CONCLUIDO';

export interface ConfiguracaoInicial {
  rendaPassivaDesejada: number | null;
  etapa: EtapaConfiguracaoInicial;
  pausado: boolean;
  ano: number;
  mes: number;
}

@Injectable({ providedIn: 'root' })
export class ConfiguracaoInicialApi {
  private readonly http = inject(HttpClient);

  buscar(): Observable<ConfiguracaoInicial> {
    return this.http.get<ConfiguracaoInicial>('/api/configuracao-inicial');
  }

  salvar(configuracao: ConfiguracaoInicial): Observable<ConfiguracaoInicial> {
    return this.http.put<ConfiguracaoInicial>('/api/configuracao-inicial', configuracao);
  }
}
