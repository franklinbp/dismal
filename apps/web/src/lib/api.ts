export type ApiError = {
  status: number;
  message: string;
};

/**
 * A wrapper for the fetch API that proxies requests through the Next.js backend
 * to securely attach the httpOnly authentication cookie.
 *
 * @param path The API path to request (e.g., "/api/v1/users"). It will be proxied.
 * @param options Standard fetch options.
 */
export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const isInternalApiRoute = path.startsWith("/api/");
  const url = isInternalApiRoute ? path : `/api/proxy${path.startsWith("/") ? "" : "/"}${path}`;

  const headers = new Headers(options.headers || {});
  
  const hasContentType = headers.has("Content-Type");
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
  if (!hasContentType && options.body && !isFormData) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  const contentType = response.headers.get("content-type") || "";
  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    try {
      const text = await response.text();
      if (text) {
        if (contentType.includes("application/json")) {
          try {
            const data = JSON.parse(text);
            message = data?.message || message;
          } catch {
            message = text;
          }
        } else {
          message = text;
        }
      }
    } catch {
      // ignore response parsing errors
    }
    const error: ApiError = { status: response.status, message };
    throw error;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (contentType.includes("application/json")) {
    const text = await response.text();
    if (!text) {
      return undefined as T;
    }
    try {
      return JSON.parse(text) as T;
    } catch {
      const error: ApiError = {
        status: response.status,
        message: "Respuesta JSON invalida del servidor.",
      };
      throw error;
    }
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return text as T;
}

