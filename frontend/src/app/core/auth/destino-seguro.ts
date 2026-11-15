export function destinoSeguro(url: string | null | undefined): string {
  if (!url || !url.startsWith('/') || url.startsWith('//') || url.startsWith('/login')) {
    return '/';
  }
  return url;
}
