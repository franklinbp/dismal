import { NextResponse } from "next/server";
import { getBaseApiUrl } from "@/lib/env";

const BASE_API_URL = getBaseApiUrl();

export async function POST(request: Request) {
  if (!BASE_API_URL) {
    return NextResponse.json(
      { message: "BASE_API_URL no esta configurado." },
      { status: 500 }
    );
  }

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { message: "JSON invalido." },
      { status: 400 }
    );
  }
  const response = await fetch(`${BASE_API_URL}/api/v1/auth/authenticate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const payload = await response.json().catch(() => ({}));
    return NextResponse.json(
      { message: payload?.message || "Credenciales invalidas." },
      { status: response.status }
    );
  }

  const data = await response.json().catch(() => null);
  if (!data) {
    return NextResponse.json(
      { message: "Respuesta invalida del servidor de autenticacion." },
      { status: 502 }
    );
  }
  const token = data?.token as string | undefined;

  if (!token) {
    return NextResponse.json(
      { message: "No se recibio token." },
      { status: 500 }
    );
  }

  const meResponse = await fetch(`${BASE_API_URL}/api/v1/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!meResponse.ok) {
    return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
  }

  const meData = await meResponse.json().catch(() => null);
  const role = meData?.role as string | undefined;
  const allowedRoles = new Set(["ADMIN", "MANAGER", "OPERATOR"]);
  if (!role || !allowedRoles.has(role)) {
    return NextResponse.json(
      { message: "Tu usuario no tiene permisos para el dashboard." },
      { status: 403 }
    );
  }
  
  // Create a safe user payload WITHOUT the token
  const userPayload = {
    id: meData.id,
    email: meData.email,
    role: meData.role,
  };

  const nextResponse = NextResponse.json(userPayload);
  const forwardedProto = request.headers.get("x-forwarded-proto");
  const requestProto = forwardedProto || new URL(request.url).protocol.replace(":", "");
  const isSecure = requestProto === "https";
  nextResponse.cookies.set("dismal_token", token, {
    httpOnly: true,
    sameSite: "lax",
    secure: isSecure,
    path: "/",
  });

  return nextResponse;
}
