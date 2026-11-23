import { Resource } from '@angular/core';

export function valorDe<T>(recurso: Resource<T>): T | undefined {
  return recurso.hasValue() ? recurso.value() : undefined;
}
