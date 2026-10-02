import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { getBaseApiUrl } from "./src/lib/env";

const allowedRoles = new Set(["ADMIN", "MANAGER", "OPERATOR"]);

export async function middleware(request: NextRequest) {
  if (request.nextUrl.pathname.startsWith("/admin")) {
    const token = request.cookies.get("dismal_token")?.value;
    if (!token) {
      const url = request.nextUrl.clone();
      url.pathname = "/login";
      return NextResponse.redirect(url);
    }

    const baseApiUrl = getBaseApiUrl();
    if (!baseApiUrl) {
      const url = request.nextUrl.clone();
      url.pathname = "/login";
      return NextResponse.redirect(url);
    }

    try {
      const response = await fetch(`${baseApiUrl}/api/v1/users/me`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) {
        const url = request.nextUrl.clone();
        url.pathname = "/login";
        return NextResponse.redirect(url);
      }
      const data = await response.json().catch(() => null);
      const role = data?.role as string | undefined;
      if (!role || !allowedRoles.has(role)) {
        const url = request.nextUrl.clone();
        url.pathname = "/login";
        return NextResponse.redirect(url);
      }
      if (role === "OPERATOR" && !request.nextUrl.pathname.startsWith("/admin/integrations")) {
        const url = request.nextUrl.clone();
        url.pathname = "/admin/integrations";
        return NextResponse.redirect(url);
      }
      return NextResponse.next();
    } catch {
      const url = request.nextUrl.clone();
      url.pathname = "/login";
      return NextResponse.redirect(url);
    }
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"]
};
