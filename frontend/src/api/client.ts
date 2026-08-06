import { router } from 'expo-router';
import { session } from './session';
import { ApiError } from './types';

const BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';
const DEFAULT_TIMEOUT_MS = 12_000;
const DEFAULT_RETRY_DELAY_MS = 250;

export class ApiException extends Error {
  constructor(
    public readonly status: number,
    public readonly error: ApiError,
  ) {
    super(error.message);
  }
}

export type ApiNetworkErrorCode = 'NETWORK_ERROR' | 'TIMEOUT' | 'REQUEST_ABORTED';

export class ApiNetworkException extends Error {
  constructor(
    public readonly code: ApiNetworkErrorCode,
    message: string,
    public readonly cause?: unknown,
  ) {
    super(message);
  }
}

// Códigos de erro que a API pode devolver — ver docs/catalogo-erros-api.md
// pra origem de cada um (qual exceção do backend gera qual código) e a razão
// da mensagem escolhida.
export type ApiErrorCode =
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'AUTH_INVALID'
  | 'CADASTRO_INCOMPLETO'
  | 'INVALID_TRANSITION'
  | 'INVALID_INPUT'
  | 'INVALID_STATE'
  | 'VALIDATION_ERROR';

const ERROR_MESSAGES: Record<ApiErrorCode, string> = {
  NOT_FOUND: 'Não encontramos o que você está procurando. Pode ter sido removido, ou você não tem acesso a ele.',
  CONFLICT: 'Já existe um registro com esses dados, ou a ação não é mais possível nesse contexto. Confira as informações e tente novamente.',
  AUTH_INVALID: 'Não foi possível confirmar suas credenciais. Verifique login e senha, ou faça login novamente.',
  CADASTRO_INCOMPLETO: 'Complete seu cadastro (CPF/CNPJ) no seu perfil antes de continuar.',
  INVALID_TRANSITION: 'Essa ação não é permitida no momento — o item pode já ter mudado de status. Atualize a página e tente novamente.',
  INVALID_INPUT: 'Alguns dados enviados são inválidos. Revise as informações e tente novamente.',
  INVALID_STATE: 'Essa ação não pode ser concluída agora. Verifique se as etapas anteriores foram concluídas e tente novamente.',
  VALIDATION_ERROR: 'Verifique os campos preenchidos e tente novamente.',
};

const NETWORK_ERROR_MESSAGES: Record<ApiNetworkErrorCode, string> = {
  NETWORK_ERROR: 'Não foi possível conectar. Verifique sua internet e tente novamente.',
  TIMEOUT: 'A requisição demorou demais para responder. Tente novamente.',
  REQUEST_ABORTED: 'A requisição foi cancelada.',
};

const DEFAULT_ERROR_MESSAGE = 'Não foi possível completar a ação. Tente novamente.';

// Ponto único pra transformar qualquer erro de apiFetch numa mensagem
// orientada a ação pro usuário — use no catch de toda tela em vez de
// `e.message ?? 'mensagem genérica'`. `error.error.code` (ApiException) e
// `error.code` (ApiNetworkException) continuam disponíveis pra quem
// precisar de lógica condicional além da mensagem (ex.: redirecionar pro
// login em AUTH_INVALID).
export function getErrorMessage(error: unknown, fallback: string = DEFAULT_ERROR_MESSAGE): string {
  if (error instanceof ApiException) {
    return ERROR_MESSAGES[error.error.code as ApiErrorCode] ?? fallback;
  }
  if (error instanceof ApiNetworkException) {
    return NETWORK_ERROR_MESSAGES[error.code] ?? fallback;
  }
  return fallback;
}

class RequestTimeoutError extends Error {}

export interface ApiRequestOptions extends RequestInit {
  auth?: boolean;
  timeoutMs?: number;
  retry?: number;
  retryDelayMs?: number;
  // Por padrão, um 401 numa chamada autenticada é tratado como "a sessão
  // morreu": limpa o token local e redireciona pro login — rank 8
  // (docs/jornadas-e-backlog-tecnico.md), pra garantir que todo ponto onde
  // a sessão pode terminar se comporte igual, em vez de cada tela decidir
  // isso ad-hoc (só uma tela fazia isso antes desta mudança). Uma minoria
  // de chamadas devolve 401 (AUTH_INVALID) por um motivo que não tem nada a
  // ver com a validade da sessão — ex.: POST /auth/senha/trocar com a senha
  // atual errada, onde a sessão continua perfeitamente válida. Passe
  // `preserveSessionOn401: true` nesses casos pra deixar a própria tela
  // tratar o erro (getErrorMessage) sem derrubar quem está logado.
  preserveSessionOn401?: boolean;
}

export async function apiFetch<T>(
  path: string,
  init?: ApiRequestOptions,
): Promise<T> {
  const isFormData = typeof FormData !== 'undefined' && init?.body instanceof FormData;
  const headers: Record<string, string> = {
    // multipart/form-data needs its own boundary, which fetch only sets
    // correctly when it stays unset here — never force it for FormData bodies.
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(init?.headers as Record<string, string>),
  };

  if (init?.auth !== false) {
    const token = await session.get();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  const method = (init?.method ?? 'GET').toUpperCase();
  const maxRetries = init?.retry ?? (method === 'GET' || method === 'HEAD' ? 1 : 0);
  const retryDelayMs = init?.retryDelayMs ?? DEFAULT_RETRY_DELAY_MS;

  for (let attempt = 0; ; attempt += 1) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}${path}`, {
        ...init,
        headers,
      });

      if (!res.ok) {
        let error: ApiError = { code: 'UNKNOWN', message: res.statusText };
        try {
          error = await res.json();
        } catch {}
        if (res.status === 401 && init?.auth !== false && !init?.preserveSessionOn401) {
          await session.clear();
          router.replace('/login');
        }
        throw new ApiException(res.status, error);
      }

      if (res.status === 204) return undefined as T;
      return res.json();
    } catch (e) {
      const networkError = toNetworkException(e);
      if (!networkError) {
        throw e;
      }
      if (attempt >= maxRetries) {
        throw networkError;
      }
      await sleep(retryDelayMs * (attempt + 1));
    }
  }
}

function toNetworkException(error: unknown): ApiNetworkException | null {
  if (error instanceof ApiException) return null;
  if (error instanceof ApiNetworkException) return error;

  if (error instanceof RequestTimeoutError) {
    return new ApiNetworkException('TIMEOUT', 'Tempo limite da requisição excedido', error);
  }

  if (error instanceof DOMException && error.name === 'AbortError') {
    return new ApiNetworkException('REQUEST_ABORTED', 'Requisição cancelada', error);
  }

  if (error instanceof TypeError) {
    return new ApiNetworkException('NETWORK_ERROR', 'Falha de rede ao conectar na API', error);
  }

  return null;
}

async function fetchWithTimeout(url: string, init: ApiRequestOptions): Promise<Response> {
  const timeoutMs = init.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  const parentSignal = init.signal;

  const onParentAbort = () => controller.abort();
  if (parentSignal) {
    parentSignal.addEventListener('abort', onParentAbort, { once: true });
  }

  try {
    return await fetch(url, {
      ...init,
      signal: controller.signal,
    });
  } catch (error) {
    if (controller.signal.aborted && !parentSignal?.aborted) {
      throw new RequestTimeoutError();
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
    if (parentSignal) {
      parentSignal.removeEventListener('abort', onParentAbort);
    }
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
