import { expect, test } from '@playwright/test';
import { entrar, simularApiDoApp } from './api-simulada-app';

test('abre a importação em modal por Lançamentos também no celular', async ({ page }) => {
  await simularApiDoApp(page);
  await entrar(page);
  await page.goto('/lancamentos');
  await page
    .getByRole('main')
    .getByRole('button', { name: 'Importar extrato', exact: true })
    .click();
  const modal = page.getByRole('dialog', { name: 'Importar extrato' });
  await expect(modal).toBeVisible();
  await expect(page).toHaveURL(/\/lancamentos$/);
  await expect(modal.getByText('Arraste o extrato aqui')).toBeVisible();
  await expect(modal.getByText('Itaú, Santander e Inter')).toBeVisible();
  await modal.getByRole('button', { name: 'Cancelar' }).click();
  await expect(modal).toBeHidden();
});

test('abre a importação pelo topo, pela tecla I e pelo endereço antigo', async ({ page }) => {
  await simularApiDoApp(page);
  await entrar(page);
  const modal = page.getByRole('dialog', { name: 'Importar extrato' });
  await page.getByRole('banner').getByRole('button', { name: 'Importar extrato' }).click();
  await expect(modal).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(modal).toBeHidden();
  if (!test.info().project.name.includes('celular')) {
    await page.keyboard.press('i');
    await expect(modal).toBeVisible();
    await page.keyboard.press('Escape');
  }
  await page.goto('/importar');
  await expect(page).toHaveURL(/\/lancamentos$/);
  await expect(modal).toBeVisible();
});

test('revisa o PDF do banco, exige categoria e importa sem duplicar itens conhecidos', async ({
  page,
}) => {
  await simularApiDoApp(page);
  let confirmacoes = 0;
  await page.route('**/api/importacoes/revisar', async (rota) => {
    expect(rota.request().headers()['content-type']).toContain('multipart/form-data; boundary=');
    expect(rota.request().postData()).toContain('extrato-itau.pdf');
    await rota.fulfill({
      json: {
        formato: 'PDF',
        banco: 'Itaú',
        moeda: 'BRL',
        itens: [
          {
            identificador: 'novo',
            data: '2026-11-10',
            descricao: 'COMPRA DÉBITO PADARIA',
            valor: 100,
            tipo: 'DESPESA',
            jaImportado: false,
            possivelDuplicado: false,
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
            possivelDuplicado: false,
            importavel: true,
            motivo: null,
          },
          {
            identificador: 'parecido',
            data: '2026-11-09',
            descricao: 'PIX ENVIADO ALUGUEL',
            valor: 1200,
            tipo: 'DESPESA',
            jaImportado: false,
            possivelDuplicado: true,
            importavel: true,
            motivo: null,
          },
        ],
      },
    });
  });
  await page.route('**/api/importacoes/confirmar', async (rota) => {
    confirmacoes++;
    const corpo = rota.request().postData() ?? '';
    expect(corpo).toContain('name="selecoes"');
    expect(corpo).toContain('[{"identificador":"novo","categoriaId":"mercado"}]');
    await rota.fulfill({ json: { importados: 1, ignorados: 1 } });
  });
  await entrar(page);
  await page.goto('/lancamentos');
  await page
    .getByRole('main')
    .getByRole('button', { name: 'Importar extrato', exact: true })
    .click();
  await page.getByLabel('Arquivo do extrato').setInputFiles({
    name: 'extrato-itau.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.7'),
  });
  await page.getByRole('combobox', { name: 'Conta de destino' }).click();
  await page.getByRole('option', { name: 'Banco Azul' }).click();
  await page.getByRole('button', { name: 'Ler extrato e revisar' }).click();
  await expect(page.getByRole('heading', { name: 'Revise antes de importar' })).toBeVisible();
  expect(confirmacoes).toBe(0);
  await expect(page.getByText(/extrato-itau\.pdf · Itaú/)).toBeVisible();
  const parecido = page.getByRole('checkbox', { name: 'Importar PIX ENVIADO ALUGUEL' });
  await expect(parecido).toBeEnabled();
  await expect(parecido).not.toBeChecked();
  await expect(page.getByText('Parece já lançada')).toBeVisible();
  await expect(
    page.getByRole('checkbox', { name: 'Importar Compra anterior', exact: true }),
  ).toBeDisabled();
  await expect(
    page.getByRole('checkbox', { name: 'Importar Compra anterior', exact: true }),
  ).not.toBeChecked();
  const confirmar = page.getByRole('button', { name: 'Confirmar importação' });
  await expect(confirmar).toBeDisabled();
  await page.getByRole('combobox', { name: 'Categoria de COMPRA DÉBITO PADARIA' }).click();
  await expect(page.getByRole('option', { name: /Salários/ })).toHaveCount(0);
  await page.getByRole('option', { name: /Supermercado/ }).click();
  await expect(confirmar).toBeEnabled();
  await confirmar.click();
  await expect(page.getByRole('heading', { name: 'Importação concluída' })).toBeVisible();
  await expect(page.getByText('1 lançamento(s) importado(s) em Banco Azul.')).toBeVisible();
  expect(confirmacoes).toBe(1);
  await page.getByRole('button', { name: 'Ver lançamentos' }).click();
  await expect(page.getByRole('dialog', { name: 'Importar extrato' })).toBeHidden();
  await expect(page).toHaveURL(/\/lancamentos$/);
});

test('importa o extrato de uma conta escolhida em Contas', async ({ page }) => {
  await simularApiDoApp(page);
  await entrar(page);
  await page.goto('/contas');
  await page.getByRole('button', { name: /Ações de Banco Azul/ }).click();
  await page.getByRole('menuitem', { name: 'Importar extrato' }).click();
  const modal = page.getByRole('dialog', { name: 'Importar extrato' });
  await expect(modal).toBeVisible();
  await expect(modal.getByRole('combobox', { name: 'Conta de destino' })).toHaveText('Banco Azul');
});
