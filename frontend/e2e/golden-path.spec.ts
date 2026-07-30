import { test, expect } from '@playwright/test';
import { loginViaApi } from './helpers/magic-link';
import { apiGet, apiPost } from './helpers/api';

test('golden path — proprietário cadastra imóvel, inquilino assina, pagamentos gerados', async ({ page }) => {
  const runId = Date.now();
  const OWNER_EMAIL = `e2e-owner-${runId}@test.com`;
  const OWNER_CPF = generateValidCpf(runId);
  const OWNER_PASSWORD = 'Senha1234';
  const TENANT_EMAIL = `e2e-inquilino-${runId}@test.com`;
  const TENANT_CPF = generateValidCpf(runId + 1);

  // 1. Registro + login via UI
  await page.goto('http://localhost:19006/register');
  await page.getByTestId('input-register-name').fill('E2E Owner');
  await page.getByTestId('input-register-cpf').fill(OWNER_CPF);
  await page.getByTestId('input-register-email').fill(OWNER_EMAIL);
  await page.getByTestId('input-register-password').fill(OWNER_PASSWORD);
  await page.getByTestId('btn-register-owner').click();

  await page.waitForURL(/imoveis/, { timeout: 10_000 });
  await expect(page).toHaveURL(/imoveis/, { timeout: 10_000 });

  await page.goto('http://localhost:19006/login');
  await page.getByTestId('input-email').fill(OWNER_EMAIL);
  await page.getByTestId('input-password').fill(OWNER_PASSWORD);
  await page.getByTestId('btn-login').click();

  await expect(page).toHaveURL(/imoveis/, { timeout: 10_000 });

  // ownerToken para chamadas diretas à API (sessão separada)
  const ownerToken = await loginViaApi(OWNER_EMAIL, 'E2E Owner', OWNER_CPF);

  // 2. Cadastra imóvel via UI
  await page.getByTestId('btn-novo-imovel').click();
  await page.getByTestId('input-endereco').fill('Rua E2E, 42');
  await page.getByTestId('input-cidade').fill('São Paulo');
  await page.getByTestId('input-matricula').fill('E2E-001');
  await page.getByTestId('btn-salvar-imovel').click();

  await expect(page).toHaveURL(/imoveis$/, { timeout: 10_000 });

  // 3. Pega o id do imóvel via API
  const imoveis = await apiGet<Array<{ id: string }>>('/imoveis', ownerToken);
  const imovelId = imoveis[0].id;

  // 4. Convite e candidatura via API direta
  const convite = await apiPost<{ token: string }>(`/imoveis/${imovelId}/convites`, {
    tipoContrato: 'RESIDENCIAL',
    valorAluguel: 2000,
    dataInicio: '2026-08-01',
    dataFim: '2027-08-01',
    garantiaAceita: 'CAUCAO',
  }, ownerToken);

  const candidatura = await apiPost<{ id: string }>(`/convites/${convite.token}/cadastro`, {
    username: 'inquilino-e2e', cpf: TENANT_CPF, email: TENANT_EMAIL, senha: 'Senha1234',
  });

  await apiPost(`/convites/${convite.token}/garantia`, {
    tipo: 'CAUCAO', dadosEspecificos: '{"valor":4000}',
  });

  // 5. Proprietário aprova → contrato criado
  const contrato = await apiPost<{ id: string }>(`/candidaturas/${candidatura.id}/aprovar`, {}, ownerToken);

  // 6. Proprietário assina via UI
  await page.goto(`http://localhost:19006/${contrato.id}/revisar`);
  await expect(page.getByTestId('btn-assinar')).toBeVisible({ timeout: 10_000 });
  await page.getByTestId('btn-assinar').click();
  await expect(page.getByTestId('btn-assinar')).not.toBeVisible({ timeout: 10_000 });

  // 7. Inquilino assina via API
  await apiPost(`/contratos/${contrato.id}/assinar`, { parte: 'INQUILINO' }, ownerToken);

  // 8. Verifica pagamentos gerados
  const pagamentos = await apiGet<Array<{ status: string }>>(`/contratos/${contrato.id}/pagamentos`, ownerToken);
  expect(pagamentos.length).toBe(12);
  expect(pagamentos[0].status).toBe('PENDENTE');
});

function generateValidCpf(seed: number): string {
  const baseDigits = String(seed).replace(/\D/g, '').padStart(9, '0').slice(-9).split('').map(Number);

  const firstVerifier = calculateVerifier(baseDigits, 10);
  const secondVerifier = calculateVerifier([...baseDigits, firstVerifier], 11);

  return [...baseDigits, firstVerifier, secondVerifier].join('');
}

function calculateVerifier(digits: number[], weightStart: number): number {
  const sum = digits.reduce((acc, digit, index) => acc + digit * (weightStart - index), 0);
  const remainder = (sum * 10) % 11;
  return remainder === 10 ? 0 : remainder;
}
