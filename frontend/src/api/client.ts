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

class RequestTimeoutError extends Error {}

export interface ApiRequestOptions extends RequestInit {
  auth?: boolean;
  timeoutMs?: number;
  retry?: number;
  retryDelayMs?: number;
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
        if (res.status === 401 && init?.auth !== false) {
          await session.clear();
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
