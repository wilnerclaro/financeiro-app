import { expect, test } from '@playwright/test';
import { entrar, simularApiDoApp } from './api-simulada-app';

test('lança uma despesa comum em três toques', async ({ page, isMobile }) => {
  const api = await simularApiDoApp(page);
  await entrar(page);
  await expect(page.getByRole('heading', { name: 'Resumo de novembro' })).toBeVisible();

  const abrir = isMobile
    ? page
        .getByRole('navigation', { name: 'Atalhos do celular' })
        .getByRole('link', { name: 'Novo lançamento' })
    : page.getByRole('banner').getByRole('button', { name: 'Novo lançamento' });
  await abrir.click();
  await expect(page.getByLabel('Valor')).toBeFocused();
  await page.keyboard.type('45,90');
  await page.getByRole('button', { name: 'Supermercado' }).click();
  await page.getByRole('button', { name: 'Lançar', exact: true }).click();

  await expect(page.getByText('Lançamento salvo')).toBeVisible();
  await expect(page).toHaveURL(/\/lancamentos$/);
  expect(api.lancamentosCriados).toEqual([
    expect.objectContaining({
      contaId: 'c1',
      categoriaId: 'mercado',
      descricao: 'Supermercado',
      valor: 45.9,
      dataPagamento: '2026-11-15',
    }),
  ]);
});

test('paga a fatura do cartão pela tela de contas', async ({ page }) => {
  const api = await simularApiDoApp(page);
  await entrar(page);
  await page.goto('/contas');
  await page.getByRole('button', { name: 'Ver fatura' }).click();
  await page.getByRole('button', { name: 'Pagar fatura' }).click();
  await page.getByRole('button', { name: 'Confirmar pagamento' }).click();

  await expect(page.getByText('Fatura paga')).toBeVisible();
  await expect(page.getByRole('dialog').getByText('Paga', { exact: true })).toBeVisible();
  expect(api.faturasPagas).toEqual([
    { contaId: 'c3', ano: 2026, mes: 11, contaPagamentoId: 'c1', dataPagamento: '2026-11-15' },
  ]);
});

for (const [rota, titulo] of [
  ['/', 'Resumo de novembro'],
  ['/lancar', 'Novo lançamento'],
  ['/lancamentos', 'Lançamentos'],
  ['/orcamento', 'Orçamento'],
  ['/orcamento/ano', 'Orçamento de 2026'],
  ['/orcamento/previsto', 'Editar previsto'],
  ['/contas', 'Contas'],
  ['/categorias', 'Categorias'],
]) {
  test(`${rota} abre sem rolagem lateral`, async ({ page }) => {
    await simularApiDoApp(page);
    await entrar(page);
    await page.goto(rota);

    await expect(page.getByRole('heading', { level: 1, name: titulo })).toBeVisible();
    const larguras = await page.evaluate(() => ({
      pagina: document.documentElement.scrollWidth,
      janela: window.innerWidth,
    }));
    expect(larguras.pagina).toBeLessThanOrEqual(larguras.janela);
  });
}

for (const rota of ['/', '/orcamento', '/orcamento/ano', '/lancamentos', '/contas']) {
  test(`${rota} mostra o esqueleto animado enquanto carrega, sem rolagem lateral`, async ({
    page,
  }) => {
    await simularApiDoApp(page);
    await entrar(page);
    await page.route(/\/api\/(?!auth)/, () => undefined);
    await page.goto(rota);

    const esqueleto = page.locator('.p-skeleton').first();
    await expect(esqueleto).toBeVisible();
    const brilho = () => esqueleto.evaluate((el) => getComputedStyle(el, '::after').animationName);
    expect(await brilho()).not.toBe('none');
    const larguras = await page.evaluate(() => ({
      pagina: document.documentElement.scrollWidth,
      janela: window.innerWidth,
    }));
    expect(larguras.pagina).toBeLessThanOrEqual(larguras.janela);

    await page.emulateMedia({ reducedMotion: 'reduce' });
    expect(await brilho()).toBe('none');
  });
}
