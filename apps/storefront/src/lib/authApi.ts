import { apiRequest, getApiBaseUrl } from "./api";

const SESSION_TOKEN_KEY = "dismal-storefront-token";

export type StorefrontUser = {
  id: string;
  email: string;
  role: string;
  customerType: "FINAL" | "DISTRIBUTOR";
  country: "EC" | "PE";
  currency: "USD" | "PEN";
  marketActive: boolean;
  firstName?: string;
  lastName?: string;
  phone?: string;
  taxId?: string;
  billingEmail?: string;
  emailVerified: boolean;
  hasCredit: boolean;
  creditLimit: number;
  creditUsed: number;
  creditAvailable: number;
  creditDays: number;
};

export type WholesaleApplication = {
  id: string;
  country: "EC" | "PE";
  businessName: string;
  taxId: string;
  phone: string;
  website?: string;
  notes?: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  reviewNotes?: string;
  reviewedAt?: string;
  createdAt: string;
};

export function readSessionToken(): string {
  return window.sessionStorage.getItem(SESSION_TOKEN_KEY) || "";
}

export function storeSessionToken(token: string): void {
  window.sessionStorage.setItem(SESSION_TOKEN_KEY, token);
}

export function clearSessionToken(): void {
  window.sessionStorage.removeItem(SESSION_TOKEN_KEY);
}

export async function authenticate(email: string, password: string): Promise<string> {
  if (!getApiBaseUrl()) {
    throw new Error("El inicio de sesión requiere conexión con la API de Dismal.");
  }

  const body = await apiRequest<{ token?: string }>("/api/v1/auth/authenticate", {
    method: "POST",
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: JSON.stringify({ email: email.trim().toLowerCase(), password })
  });

  if (!body.token) throw new Error("Dismal no devolvió una sesión válida.");
  return body.token;
}

export async function fetchCurrentUser(
  token: string,
  country: "EC" | "PE"
): Promise<StorefrontUser> {
  return apiRequest<StorefrontUser>(`/api/v1/users/me?country=${country}`, {
    headers: { Accept: "application/json", Authorization: `Bearer ${token}` }
  });
}

export async function requestPasswordReset(email: string, country: "EC" | "PE"): Promise<string> {
  const result = await apiRequest<{ message: string }>("/api/v1/auth/account/forgot-password", {
    method: "POST",
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: JSON.stringify({ email: email.trim().toLowerCase(), country })
  });
  return result.message;
}

export async function resetPassword(token: string, newPassword: string): Promise<string> {
  const result = await apiRequest<{ message: string }>("/api/v1/auth/account/reset-password", {
    method: "POST",
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: JSON.stringify({ token, newPassword })
  });
  return result.message;
}

export async function verifyEmail(token: string): Promise<string> {
  const result = await apiRequest<{ message: string }>("/api/v1/auth/account/verify-email", {
    method: "POST",
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: JSON.stringify({ token })
  });
  return result.message;
}

export async function resendVerification(token: string, country: "EC" | "PE"): Promise<string> {
  const result = await apiRequest<{ message: string }>(
    `/api/v1/account/resend-verification?country=${country}`,
    {
      method: "POST",
      headers: { Accept: "application/json", Authorization: `Bearer ${token}` }
    }
  );
  return result.message;
}

export async function updateProfile(
  token: string,
  country: "EC" | "PE",
  profile: Pick<StorefrontUser, "firstName" | "lastName" | "phone" | "taxId" | "billingEmail">
): Promise<StorefrontUser> {
  return apiRequest<StorefrontUser>("/api/v1/account/profile", {
    method: "PUT",
    headers: { Accept: "application/json", "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ ...profile, country })
  });
}

export async function changePassword(
  token: string,
  currentPassword: string,
  newPassword: string
): Promise<string> {
  const result = await apiRequest<{ message: string }>("/api/v1/account/password", {
    method: "PUT",
    headers: { Accept: "application/json", "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ currentPassword, newPassword })
  });
  return result.message;
}

export async function fetchLatestWholesaleApplication(
  token: string,
  country: "EC" | "PE"
): Promise<WholesaleApplication | null> {
  return apiRequest<WholesaleApplication | null>(
    `/api/v1/account/wholesale-applications/latest?country=${country}`,
    {
    headers: { Accept: "application/json", Authorization: `Bearer ${token}` }
    }
  );
}

export async function submitWholesaleApplication(
  token: string,
  application: {
    country: "EC" | "PE";
    businessName: string;
    taxId: string;
    phone: string;
    website?: string;
    notes?: string;
  }
): Promise<WholesaleApplication> {
  return apiRequest<WholesaleApplication>("/api/v1/account/wholesale-applications", {
    method: "POST",
    headers: { Accept: "application/json", "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify(application)
  });
}
