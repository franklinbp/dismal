import type { CountryCode, CustomerType, StorefrontProduct } from "../types";

type StorefrontEvent = {
  event: string;
  country: CountryCode;
  currency: string;
  customerType: CustomerType;
  productId?: string;
  productName?: string;
  productCategory?: string;
  value?: number;
  orderNumber?: string;
};

declare global {
  interface Window {
    dataLayer?: Array<Record<string, unknown>>;
  }
}

const attributionKey = "dismal.storefront.attribution.v1";

export function captureAttribution(): void {
  const params = new URLSearchParams(window.location.search);
  const attribution = Object.fromEntries(
    ["utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term"]
      .map((key) => [key, params.get(key)?.slice(0, 160) || ""])
      .filter(([, value]) => Boolean(value))
  );
  if (Object.keys(attribution).length === 0) return;
  try {
    window.sessionStorage.setItem(attributionKey, JSON.stringify(attribution));
  } catch {
    // Attribution is optional and must never interrupt a shopping session.
  }
}

function readAttribution(): Record<string, string> {
  try {
    return JSON.parse(window.sessionStorage.getItem(attributionKey) || "{}") as Record<string, string>;
  } catch {
    return {};
  }
}

export function trackStorefrontEvent(event: StorefrontEvent): void {
  const payload = { ...readAttribution(), ...event, timestamp: new Date().toISOString() };
  window.dataLayer?.push(payload);
  window.dispatchEvent(new CustomEvent("dismal:storefront", { detail: payload }));
}

export function productEventFields(product: StorefrontProduct) {
  return {
    productId: product.id,
    productName: product.name,
    productCategory: product.category
  };
}
