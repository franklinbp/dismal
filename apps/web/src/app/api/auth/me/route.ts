import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { getBaseApiUrl } from "@/lib/env";

const BASE_API_URL = getBaseApiUrl();

export async function GET() {
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

  const response = await fetch(`${BASE_API_URL}/api/v1/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) {
    return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
  }

  const data = await response.json().catch(() => null);
  if (!data) {
    return NextResponse.json(
      { message: "Respuesta invalida del servidor de usuarios." },
      { status: 502 }
    );
  }
  return NextResponse.json(data);
}
