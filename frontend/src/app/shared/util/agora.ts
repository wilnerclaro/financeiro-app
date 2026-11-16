import { InjectionToken } from '@angular/core';

export const AGORA = new InjectionToken<() => Date>('agora', {
  providedIn: 'root',
  factory: () => () => new Date(),
});
