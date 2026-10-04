import { API_BASE } from "../config";

export type ApiError = Error & {
  readonly status: number;
  readonly code: string;
  readonly details: Record<string, unknown>;
};

const apiError = (status: number, code: string, message: string, details: Record<string, unknown> = {}): ApiError =>
  Object.assign(new Error(message), { status, code, details });

export const isApiError = (error: unknown): error is ApiError =>
  error instanceof Error && "status" in error && "code" in error;

const parseJson = (text: string): unknown => {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
};

// 토큰과 재발급 함수는 세션이 채워 준다. 401이면 한 번만 다시 식별한 뒤 같은 요청을 재시도한다.
let accessToken: string | null = null;
let refresh: (() => Promise<void>) | null = null;

export const setAccessToken = (token: string | null): void => {
  accessToken = token;
};
export const setTokenRefresher = (fn: (() => Promise<void>) | null): void => {
  refresh = fn;
};

type Json = Record<string, unknown> | ReadonlyArray<unknown>;

export async function request<T>(path: string, init: { method?: string; body?: Json } = {}, canRetry = true): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: init.method ?? "GET",
    headers: {
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    body: init.body ? JSON.stringify(init.body) : undefined,
  }).catch(() => {
    throw apiError(0, "NETWORK_ERROR", "서버에 연결하지 못했어요. 잠시 뒤 다시 시도해 주세요.");
  });

  if (response.status === 204) return undefined as T;

  const text = await response.text();
  const body = (text ? parseJson(text) : undefined) as
    | { code?: string; message?: string; details?: Record<string, unknown> }
    | undefined;

  if (response.ok) return body as T;

  if (response.status === 401 && canRetry && refresh) {
    await refresh();
    return request<T>(path, init, false);
  }

  throw apiError(
    response.status,
    body?.code ?? "UNKNOWN",
    body?.message ?? "요청을 처리하지 못했어요.",
    body?.details ?? {},
  );
}
