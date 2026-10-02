import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { getBaseApiUrl } from "@/lib/env";

const BASE_API_URL = getBaseApiUrl();
const allowedRoles = new Set(["ADMIN", "MANAGER", "OPERATOR"]);

const loadRole = async (token: string) => {
  const response = await fetch(`${BASE_API_URL}/api/v1/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    return null;
  }
  const data = await response.json().catch(() => null);
  return (data?.role as string | undefined) || null;
};

async function handler(request: Request) {
  if (!BASE_API_URL) {
    return NextResponse.json(
      { message: "BASE_API_URL no esta configurado." },
      { status: 500 }
    );
  }

  const token = cookies().get("dismal_token")?.value;
  if (!token) {
    return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
  }

  const role = await loadRole(token);
  if (!role || !allowedRoles.has(role)) {
    return NextResponse.json({ message: "Forbidden" }, { status: 403 });
  }

  const url = new URL(request.url);
  const path = url.pathname.replace("/api/backend", "");
  const allowedPrefixes =
    role === "OPERATOR" ? ["/api/v1/integrations/"] : ["/api/v1/"];
  if (!allowedPrefixes.some((prefix) => path.startsWith(prefix))) {
    return NextResponse.json({ message: "Forbidden" }, { status: 403 });
  }

  const targetUrl = `${BASE_API_URL}${path}${url.search}`;

  const headers = new Headers(request.headers);
  headers.set("Authorization", `Bearer ${token}`);

  const method = request.method.toUpperCase();
  let body: BodyInit | undefined;
  if (method !== "GET" && method !== "HEAD") {
    const buffer = await request.arrayBuffer();
    if (buffer.byteLength > 0) {
      body = buffer;
    }
  }

  let response: Response;
  try {
    response = await fetch(targetUrl, {
      method: request.method,
      headers,
      body,
    });
  } catch (error) {
    return NextResponse.json(
      {
        message:
          error instanceof Error
            ? `No se pudo conectar con el backend de Dismal: ${error.message}`
            : "No se pudo conectar con el backend de Dismal.",
      },
      { status: 502 }
    );
  }

  const responseHeaders = new Headers(response.headers);
  if (response.status === 204) {
    return new NextResponse(null, { status: 204, headers: responseHeaders });
  }

  const contentType = responseHeaders.get("content-type") || "";
  const text = await response.text();

  if (!response.ok) {
    let message = `Backend respondio con estado ${response.status}`;
    if (text) {
      if (contentType.includes("application/json")) {
        try {
          const data = JSON.parse(text);
          message = data?.message || data?.error || message;
        } catch {
          message = text;
        }
      } else {
        message = text;
      }
    }
    return NextResponse.json({ message }, { status: response.status });
  }

  if (!text) {
    return new NextResponse(null, { status: response.status });
  }

  if (contentType.includes("application/json")) {
    try {
      return NextResponse.json(JSON.parse(text), { status: response.status });
    } catch {
      return NextResponse.json(
        { message: "El backend respondio con JSON invalido.", raw: text },
        { status: 502 }
      );
    }
  }

  return new NextResponse(text, {
    status: response.status,
    headers: responseHeaders,
  });
}

export { handler as GET, handler as POST, handler as PUT, handler as DELETE, handler as PATCH };
