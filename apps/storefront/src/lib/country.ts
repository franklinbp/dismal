import type { CountryCode } from "../types";

export type CountryConfig = {
  code: CountryCode;
  name: string;
  domain: string;
  currency: string;
  locale: string;
  contactEmail: string;
  supportPhone: string;
  whatsappNumber: string;
  telegramHandle: string;
  headline: string;
  paymentMethods: string[];
};

function normalizeTelegramHandle(value?: string): string {
  return (value || "")
    .trim()
    .replace(/^https?:\/\/(?:www\.)?t\.me\//i, "")
    .replace(/^@/, "")
    .split(/[/?#]/, 1)[0]
    .replace(/[^a-zA-Z0-9_]/g, "");
}

const countries: Record<CountryCode, CountryConfig> = {
  EC: {
    code: "EC",
    name: "Ecuador",
    domain: "dismalec.com",
    currency: "USD",
    locale: "es-EC",
    contactEmail: "admin@dismal.vip",
    supportPhone: "Soporte por WhatsApp",
    whatsappNumber: (import.meta.env.VITE_WHATSAPP_EC || "").replace(/\D/g, ""),
    telegramHandle: normalizeTelegramHandle(import.meta.env.VITE_TELEGRAM_EC),
    headline: "Productos físicos y digitales para Ecuador",
    paymentMethods: ["Pago sujeto a confirmación", "Entrega física o digital"]
  }
};

export function resolveCountry(_hostname = window.location.hostname): CountryConfig {
  return countries.EC;
}

export function getCountry(_code: string): CountryConfig {
  return countries.EC;
}

export function formatMoney(value: number, country: CountryConfig): string {
  return new Intl.NumberFormat(country.locale, {
    style: "currency",
    currency: country.currency,
    maximumFractionDigits: 2
  }).format(value);
}

export function whatsappUrl(country: CountryConfig, message: string): string | null {
  if (!country.whatsappNumber) return null;
  return `https://wa.me/${country.whatsappNumber}?text=${encodeURIComponent(message)}`;
}

export function telegramUrl(country: CountryConfig): string | null {
  return country.telegramHandle ? `https://t.me/${country.telegramHandle}` : null;
}

export function formatContactPhone(country: CountryConfig): string {
  const digits = country.whatsappNumber;
  if (country.code === "EC" && /^593\d{9}$/.test(digits)) {
    return `+593 ${digits.slice(3, 5)} ${digits.slice(5, 8)} ${digits.slice(8)}`;
  }
  return digits ? `+${digits}` : "WhatsApp";
}
