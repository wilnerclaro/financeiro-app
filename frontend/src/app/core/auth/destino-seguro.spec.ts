import { destinoSeguro } from './destino-seguro';

describe('destinoSeguro', () => {
  it('aceita rota interna', () => {
    expect(destinoSeguro('/orcamento/ano')).toBe('/orcamento/ano');
  });

  it.each([undefined, null, '', 'https://site.com', '//site.com', 'contas', '/login?voltar=/'])(
    'volta para o início quando o destino é %s',
    (url) => {
      expect(destinoSeguro(url)).toBe('/');
    },
  );
});
