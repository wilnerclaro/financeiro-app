import { expect, test } from '@playwright/test';
import {
  EMAIL_JA_USADO,
  SENHA_CERTA,
  simularApiDeAutenticacao,
  simularGoogle,
} from './api-simulada';

test.beforeEach(async ({ page }) => {
  await simularApiDeAutenticacao(page);
  await page.route('**/api/dashboard-financeiro/**', (rota) => rota.fulfill({ status: 503 }));
});

test('quem não entrou cai no login', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveURL(/\/login\?voltar=%2F$/);
  await expect(page.getByRole('heading', { name: 'Bem-vindo de volta' })).toBeVisible();
});

test('pede e-mail e senha antes de chamar a API', async ({ page }) => {
  await page.goto('/login');
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();

  await expect(page.getByText('Informe seu e-mail.')).toBeVisible();
  await expect(page.getByText('Informe sua senha.')).toBeVisible();
});

test('mostra a mensagem da API quando a senha está errada', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill('ana@exemplo.com');
  await page.getByLabel('Senha', { exact: true }).fill('errada');
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();

  await expect(page.getByRole('alert')).toContainText(
    'E-mail ou senha não conferem. Confira e tente de novo.',
  );
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
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page.getByRole('heading', { name: /^Resumo de / })).toBeVisible();

  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.goto('/');
  await expect(page).toHaveURL(/\/login\?voltar=%2F$/);
});

for (const [rota, titulo] of [
  ['/login', 'Bem-vindo de volta'],
  ['/criar-conta', 'Crie sua conta'],
]) {
  test(`nada rola para o lado em ${rota}`, async ({ page }) => {
    await page.goto(rota);
    await expect(page.getByRole('heading', { name: titulo })).toBeVisible();

    const larguras = await page.evaluate(() => ({
      conteudo: document.documentElement.scrollWidth,
      tela: document.documentElement.clientWidth,
    }));
    expect(larguras.conteudo).toBeLessThanOrEqual(larguras.tela);
  });
}

test('cria a conta com nome, e-mail e senha e cai no início', async ({ page }) => {
  await page.goto('/login');
  await page.getByRole('link', { name: 'Criar conta' }).click();
  await expect(page).toHaveURL(/\/criar-conta/);

  await page.getByLabel('Como podemos te chamar?').fill('Ana');
  await page.getByLabel('E-mail').fill('nova@exemplo.com');
  await page.getByLabel('Crie uma senha').fill('familia2026');
  await expect(page.getByRole('list', { name: 'Regras da senha' })).toContainText(
    /Um número\s*\(atendida\)/,
  );
  await page.getByRole('button', { name: 'Criar conta', exact: true }).click();

  await expect(page.getByRole('heading', { name: /^Resumo de / })).toBeVisible();
});

test('e-mail já cadastrado oferece entrar', async ({ page }) => {
  await page.goto('/criar-conta');
  await page.getByLabel('Como podemos te chamar?').fill('Ana');
  await page.getByLabel('E-mail').fill(EMAIL_JA_USADO);
  await page.getByLabel('Crie uma senha').fill('familia2026');
  await page.keyboard.press('Enter');

  const alerta = page.getByRole('alert');
  await expect(alerta).toContainText('Já existe uma conta com este e-mail.');
  await alerta.getByRole('link', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: 'Bem-vindo de volta' })).toBeVisible();
});

test('entra com o Google antes do formulário', async ({ page }) => {
  await simularGoogle(page);
  await page.goto('/login');

  const google = page.getByRole('button', { name: 'Google simulado: entrar' });
  await expect(google).toBeVisible();
  const caixaGoogle = await google.boundingBox();
  const campoEmail = await page.getByLabel('E-mail').boundingBox();
  expect(caixaGoogle!.y).toBeLessThan(campoEmail!.y);

  await google.click();
  await expect(page.getByText('Abrindo o Google…')).toBeVisible();
  await expect(page.getByRole('heading', { name: /^Resumo de / })).toBeVisible();
});
