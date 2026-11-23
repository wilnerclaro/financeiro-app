import { Page, expect, test } from '@playwright/test';
import { SENHA_CERTA } from './api-simulada';
import { simularApiDoApp } from './api-simulada-app';

interface Configuracao {
  rendaPassivaDesejada: number | null;
  etapa: 'BOAS_VINDAS' | 'OBJETIVO' | 'ORCAMENTO' | 'CONTAS' | 'LANCAMENTO' | 'CONCLUIDO';
  pausado: boolean;
  ano: number;
  mes: number;
}

async function preparar(page: Page, inicial: Partial<Configuracao> = {}) {
  const app = await simularApiDoApp(page);
  let estado: Configuracao = {
    rendaPassivaDesejada: null,
    etapa: 'BOAS_VINDAS',
    pausado: false,
    ano: 2026,
    mes: 11,
    ...inicial,
  };
  const salvos: Configuracao[] = [];
  let falharConclusao = false;
  let tentativasDeConclusao = 0;
  await page.route('**/api/configuracao-inicial', async (rota) => {
    if (rota.request().method() === 'PUT') {
      const pedido = rota.request().postDataJSON() as Configuracao;
      if (pedido.etapa === 'CONCLUIDO') {
        tentativasDeConclusao++;
        if (falharConclusao) {
          falharConclusao = false;
          await rota.fulfill({ status: 503, json: { message: 'Tente novamente.' } });
          return;
        }
      }
      estado = { ...pedido };
      salvos.push({ ...estado });
    }
    await rota.fulfill({ json: estado });
  });
  return {
    app,
    salvos,
    estado: () => estado,
    falharProximaConclusao: () => (falharConclusao = true),
    tentativasDeConclusao: () => tentativasDeConclusao,
  };
}

async function entrarNoFluxo(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill('ana@exemplo.com');
  await page.getByLabel('Senha', { exact: true }).fill(SENHA_CERTA);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page).toHaveURL(/\/configuracao-inicial$/);
}

test('percorre os primeiros passos com contas existentes sem criar lançamento fictício', async ({
  page,
}, testInfo) => {
  const api = await preparar(page);
  await entrarNoFluxo(page);
  await page.getByRole('button', { name: 'Começar configuração' }).click();
  await page.getByLabel('Renda passiva mensal desejada').fill('6000');
  await page.getByRole('button', { name: 'Planejar meu orçamento' }).click();
  await expect.poll(() => api.estado().etapa).toBe('ORCAMENTO');
  await expect(page.getByRole('button', { name: 'Preparar minhas contas' })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('orcamento-inicial.png'), fullPage: true });
  await page.getByRole('button', { name: 'Preparar minhas contas' }).click();
  await expect(page.getByText('Banco Azul', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Ir para o primeiro lançamento' }).click();
  await page.getByRole('button', { name: 'Concluir sem lançar' }).click();
  await expect.poll(() => api.estado().etapa).toBe('CONCLUIDO');
  expect(api.estado().rendaPassivaDesejada).toBe(6000);
  expect(api.app.lancamentosCriados).toHaveLength(0);
  await page.getByRole('button', { name: 'Ir para o início' }).click();
  await expect(page.getByRole('heading', { name: 'Resumo de novembro' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Resumo de novembro' })).toBeVisible();
});

test('pausa e retoma o objetivo após recarregar sem perder o valor', async ({ page }) => {
  const api = await preparar(page);
  await entrarNoFluxo(page);
  await page.getByRole('button', { name: 'Começar configuração' }).click();
  await page.getByLabel('Renda passiva mensal desejada').fill('7500');
  await page.getByRole('button', { name: 'Continuar depois' }).click();
  await expect(page).toHaveURL(/\/$/);
  expect(api.estado().pausado).toBe(true);
  expect(api.estado().rendaPassivaDesejada).toBe(7500);
  await page.reload();
  await page.getByRole('button', { name: 'Retomar configuração' }).click();
  await expect(page.getByLabel('Renda passiva mensal desejada')).toHaveValue(/7.*500/);
  expect(api.app.lancamentosCriados).toHaveLength(0);
  const largura = await page.evaluate(() => [document.documentElement.scrollWidth, innerWidth]);
  expect(largura[0]).toBeLessThanOrEqual(largura[1]);
});

test('retenta somente a conclusão quando o primeiro lançamento já foi registrado', async ({
  page,
}) => {
  const api = await preparar(page, {
    rendaPassivaDesejada: 6000,
    etapa: 'LANCAMENTO',
  });
  await entrarNoFluxo(page);
  await page.goto('/lancar?configuracaoInicial=true');
  await expect(page.getByLabel('Valor')).toBeFocused();
  await page.keyboard.type('45,90');
  await page.getByRole('combobox', { name: 'Categoria', exact: true }).click();
  await page.getByRole('option', { name: /Alimentação/ }).click();
  await page.getByRole('combobox', { name: 'Subcategoria', exact: true }).click();
  await page.getByRole('option', { name: 'Supermercado' }).click();
  api.falharProximaConclusao();
  await page.getByRole('button', { name: 'Registrar e concluir' }).click();

  await expect(page.getByText('Seu lançamento foi registrado.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Registrar e concluir' })).toHaveCount(0);
  expect(api.app.lancamentosCriados).toHaveLength(1);
  await page.getByRole('button', { name: 'Tentar concluir novamente' }).click();

  await expect(page).toHaveURL(/\/configuracao-inicial$/);
  await expect.poll(() => api.estado().etapa).toBe('CONCLUIDO');
  expect(api.tentativasDeConclusao()).toBe(2);
  expect(api.app.lancamentosCriados).toHaveLength(1);
});

test('acha a categoria do orçamento pela busca e grava o previsto', async ({ page }) => {
  await preparar(page, { rendaPassivaDesejada: 6000, etapa: 'ORCAMENTO' });
  const gravados: unknown[] = [];
  await page.route('**/api/orcamentos/2026/11', async (rota) => {
    if (rota.request().method() === 'PUT') {
      gravados.push(rota.request().postDataJSON());
    }
    await rota.fallback();
  });
  await entrarNoFluxo(page);
  await expect(page.getByRole('button', { name: /^Receitas ativas/ })).toHaveAttribute(
    'aria-expanded',
    'false',
  );
  await page.getByRole('button', { name: 'Despesas', exact: true }).click();
  await page.getByRole('searchbox', { name: 'Buscar categoria' }).fill('padaria');
  const padaria = page.getByLabel('Padaria, previsto');
  await padaria.fill('150');
  await padaria.blur();
  await expect(page.getByText('Despesas R$ 1.150,00')).toBeVisible();
  await page.getByRole('button', { name: 'Preparar minhas contas' }).click();

  await expect
    .poll(() => gravados)
    .toEqual([{ itens: [{ categoriaId: 'padaria', valorPrevisto: 150 }] }]);
});
