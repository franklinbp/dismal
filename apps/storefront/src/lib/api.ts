const configuredApiUrl = import.meta.env.VITE_DISMAL_API_URL?.trim().replace(/\/$/, "") || "";

export type ApiErrorKind = "http" | "network" | "timeout" | "invalid-response";

export class StorefrontApiError extends Error {
  constructor(
    message: string,
    readonly kind: ApiErrorKind,
    readonly status?: number
  ) {
    super(message);
    this.name = "StorefrontApiError";
  }
}

function isLocalHostname(hostname: string): boolean {
  return hostname === "localhost" || hostname === "127.0.0.1";
}

function normalizeApiUrl(value: string): string {
  if (!value) return "";
  try {
    const url = new URL(value);
    const localHttp = url.protocol === "http:" && isLocalHostname(url.hostname);
    if (url.protocol !== "https:" && !localHttp) return "";
    return url.href.replace(/\/$/, "");
  } catch {
    return "";
  }
}

export function getApiBaseUrl(): string {
  const normalizedConfiguredUrl = normalizeApiUrl(configuredApiUrl);
  if (normalizedConfiguredUrl) return normalizedConfiguredUrl;

  const hostname = window.location.hostname.toLowerCase();
  if (isLocalHostname(hostname)) return "";
  return "https://api.dismal.vip";
}

export function hasApiConnection(): boolean {
  return Boolean(getApiBaseUrl());
}

export async function readApiError(response: Response, fallback: string): Promise<string> {
  const rawBody = await response.text().catch(() => "");
  if (!rawBody) return fallback;

  try {
    const body = JSON.parse(rawBody) as { message?: string; detail?: string; error?: string };
    return body.message || body.detail || body.error || fallback;
  } catch {
    return rawBody.length <= 240 ? rawBody : fallback;
  }
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  timeoutMs = 10_000
): Promise<T> {
  const apiBaseUrl = getApiBaseUrl();
  if (!apiBaseUrl) {
    throw new StorefrontApiError(
      "Esta función requiere conexión con la API segura de Dismal.",
      "network"
    );
  }

  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      cache: init.cache || "no-store",
      credentials: "omit",
      signal: controller.signal
    });

    if (!response.ok) {
      const fallback = response.status >= 500
        ? "El servicio de Dismal está temporalmente ocupado. Intenta nuevamente."
        : "No se pudo completar la solicitud.";
      throw new StorefrontApiError(
        await readApiError(response, fallback),
        "http",
        response.status
      );
    }

    if (response.status === 204) return undefined as T;
    try {
      return (await response.json()) as T;
    } catch {
      throw new StorefrontApiError(
        "La API devolvió una respuesta que no se pudo validar.",
        "invalid-response",
        response.status
      );
    }
  } catch (error) {
    if (error instanceof StorefrontApiError) throw error;
    if (controller.signal.aborted) {
      throw new StorefrontApiError(
        "La conexión está tardando más de lo esperado. Revisa tu internet e intenta nuevamente.",
        "timeout"
      );
    }
    throw new StorefrontApiError(
      "No fue posible conectar con Dismal. Revisa tu conexión e intenta nuevamente.",
      "network"
    );
  } finally {
    window.clearTimeout(timeout);
  }
}
