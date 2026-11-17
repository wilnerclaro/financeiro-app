import { expect, test } from '@playwright/test';
import { entrar, simularApiDoApp } from './api-simulada-app';

test('abre a importação por Lançamentos também no celular', async ({ page }) => {
  await simularApiDoApp(page);
  await entrar(page);
  await page.goto('/lancamentos');
  await page
    .getByRole('main')
    .getByRole('button', { name: 'Importar extrato', exact: true })
    .click();
  await expect(page).toHaveURL(/\/importar$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Importar extrato' })).toBeVisible();
  await expect(page.getByText(/em breve/i).first()).toBeVisible();
});

test('revisa o OFX, exige categoria e importa sem duplicar itens conhecidos', async ({ page }) => {
  await simularApiDoApp(page);
  let confirmacoes = 0;
  await page.route('**/api/importacoes/ofx/revisar', async (rota) => {
    expect(rota.request().headers()['content-type']).toContain('multipart/form-data; boundary=');
    expect(rota.request().postData()).toContain('extrato.ofx');
    await rota.fulfill({
      json: {
        moeda: 'BRL',
        itens: [
          {
            identificador: 'novo',
            data: '2026-11-10',
            descricao: 'Compra OFX',
            valor: 100,
            tipo: 'DESPESA',
            jaImportado: false,
            importavel: true,
            motivo: null,
          },
          {
            identificador: 'duplicado',
            data: '2026-11-10',
            descricao: 'Compra anterior',
            valor: 50,
            tipo: 'DESPESA',
            jaImportado: true,
            importavel: true,
            motivo: null,
          },
        ],
      },
    });
  });
  await page.route('**/api/importacoes/ofx/confirmar', async (rota) => {
    confirmacoes++;
    const corpo = rota.request().postData() ?? '';
    expect(corpo).toContain('name="selecoes"');
    expect(corpo).toContain('[{"identificador":"novo","categoriaId":"mercado"}]');
    await rota.fulfill({ json: { importados: 1, ignorados: 1 } });
  });
  await entrar(page);
  await page.goto('/importar');
  await page.getByLabel('Arquivo OFX').setInputFiles({
    name: 'extrato.ofx',
    mimeType: 'application/x-ofx',
    buffer: Buffer.from('<OFX></OFX>'),
  });
  await page.getByRole('combobox', { name: 'Conta de destino' }).click();
  await page.getByRole('option', { name: 'Banco Azul' }).click();
  await page.getByRole('button', { name: 'Ler extrato e revisar' }).click();
  await expect(page.getByRole('heading', { name: 'Revise antes de importar' })).toBeVisible();
  expect(confirmacoes).toBe(0);
  await expect(
    page.getByRole('checkbox', { name: 'Importar Compra anterior', exact: true }),
  ).toBeDisabled();
  await expect(
    page.getByRole('checkbox', { name: 'Importar Compra anterior', exact: true }),
  ).not.toBeChecked();
  const confirmar = page.getByRole('button', { name: 'Confirmar importação' });
  await expect(confirmar).toBeDisabled();
  await page.getByRole('combobox', { name: 'Categoria de Compra OFX' }).click();
  await expect(page.getByRole('option', { name: /Salários/ })).toHaveCount(0);
  await page.getByRole('option', { name: /Supermercado/ }).click();
  await expect(confirmar).toBeEnabled();
  await confirmar.click();
  await expect(page.getByRole('heading', { name: 'Importação concluída' })).toBeVisible();
  await expect(page.getByText('1 lançamento(s) importado(s) em Banco Azul.')).toBeVisible();
  expect(confirmacoes).toBe(1);
  await page.getByRole('button', { name: 'Ver lançamentos' }).click();
  await expect(page).toHaveURL(/\/lancamentos$/);
});
