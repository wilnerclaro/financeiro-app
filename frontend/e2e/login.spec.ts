import { expect, test } from '@playwright/test';
import { SENHA_CERTA, simularApiDeAutenticacao } from './api-simulada';

test.beforeEach(async ({ page }) => {
  await simularApiDeAutenticacao(page);
  await page.route('**/api/dashboard-financeiro/**', (rota) => rota.fulfill({ status: 503 }));
});

test('quem não entrou cai no login', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveURL(/\/login\?voltar=%2F$/);
  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();
});

test('pede e-mail e senha antes de chamar a API', async ({ page }) => {
  await page.goto('/login');
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page.getByText('Informe seu e-mail.')).toBeVisible();
  await expect(page.getByText('Informe sua senha.')).toBeVisible();
});

test('mostra a mensagem da API quando a senha está errada', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill('ana@exemplo.com');
  await page.getByLabel('Senha', { exact: true }).fill('errada');
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page.getByRole('alert')).toContainText('E-mail ou senha inválidos.');
  await expect(page.getByLabel('E-mail')).toHaveValue('ana@exemplo.com');
});

test('entra só com o teclado e continua logado depois de recarregar', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill('ana@exemplo.com');
  await page.getByLabel('Senha', { exact: true }).fill(SENHA_CERTA);
  await page.keyboard.press('Enter');

  await expect(page.getByRole('heading', { name: /^Resumo de / })).toBeVisible();
  await expect(page).toHaveURL(/\/$/);

  await page.reload();
  await expect(page.getByRole('heading', { name: /^Resumo de / })).toBeVisible();

  const guardado = await page.evaluate(() =>
    JSON.stringify({ ...sessionStorage, ...localStorage }),
  );
  expect(guardado).not.toContain('acesso-');
});

test('sair volta para o login e não restaura a sessão', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill('ana@exemplo.com');
  await page.getByLabel('Senha', { exact: true }).fill(SENHA_CERTA);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: /^Resumo de / })).toBeVisible();

  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.goto('/');
  await expect(page).toHaveURL(/\/login\?voltar=%2F$/);
});

test('nada rola para o lado no login', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();

  const larguras = await page.evaluate(() => ({
    conteudo: document.documentElement.scrollWidth,
    tela: document.documentElement.clientWidth,
  }));
  expect(larguras.conteudo).toBeLessThanOrEqual(larguras.tela);
});
