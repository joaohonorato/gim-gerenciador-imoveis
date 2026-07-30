import { apiPost } from './api';

export async function loginViaApi(email: string, nome: string, cpfCnpj: string): Promise<string> {
  try {
    const { sessionToken } = await apiPost<{ sessionToken: string }>('/auth/login', {
      email,
      senha: 'Senha1234',
    });
    return sessionToken;
  } catch {
    const { sessionToken } = await apiPost<{ sessionToken: string }>('/auth/register/proprietario', {
      email,
      senha: 'Senha1234',
      username: nome,
      cpfCnpj,
    });
    return sessionToken;
  }
}
