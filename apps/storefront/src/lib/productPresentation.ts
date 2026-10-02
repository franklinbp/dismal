import type { StorefrontProduct } from "../types";

const CATEGORY_WINDOWS = "Windows";
const CATEGORY_OFFICE = "Microsoft 365 y Office";
const CATEGORY_SECURITY = "Antivirus y seguridad";
const CATEGORY_DESIGN = "Diseño y productividad";
const CATEGORY_SERVERS = "Servidores";
const CATEGORY_COMBOS = "Combos";

export const storefrontCategories = [
  CATEGORY_WINDOWS,
  CATEGORY_OFFICE,
  CATEGORY_SECURITY,
  CATEGORY_DESIGN,
  CATEGORY_SERVERS,
  CATEGORY_COMBOS
] as const;

export function slugifyProduct(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 90) || "producto";
}

export function normalizeProductName(value: string): string {
  const source = value.replace(/[_-]+/g, " ").replace(/\s+/g, " ").trim();
  const base = source === source.toUpperCase() || source === source.toLowerCase()
    ? source.toLowerCase().replace(/\b\p{L}/gu, (letter) => letter.toUpperCase())
    : source;
  return base
    .replace(/\bmicrosoft\b/gi, "Microsoft")
    .replace(/\bwindows\b/gi, "Windows")
    .replace(/\bserver\b/gi, "Server")
    .replace(/\boffice\b/gi, "Office")
    .replace(/\bkaspersky\b/gi, "Kaspersky")
    .replace(/\beset\b/gi, "ESET")
    .replace(/\bcanva\b/gi, "Canva")
    .replace(/\bautodesk\b/gi, "Autodesk")
    .replace(/\bmalwarebytes\b/gi, "Malwarebytes")
    .replace(/\bpanda\b/gi, "Panda")
    .replace(/\bprofessional\b/gi, "Professional")
    .replace(/\bprofesional\b/gi, "Professional")
    .replace(/\bestandar\b/gi, "Standard")
    .replace(/\bstandard\b/gi, "Standard")
    .replace(/\bplus\b/gi, "Plus")
    .replace(/\bhome\b/gi, "Home")
    .replace(/\bbusiness\b/gi, "Business")
    .replace(/\bpersonal\b/gi, "Personal")
    .replace(/\bfamilia\b/gi, "Familia")
    .replace(/\bpremium\b/gi, "Premium")
    .replace(/\bultimate\b/gi, "Ultimate")
    .replace(/\bpro\b/gi, "Pro")
    .replace(/\bY\b/g, "y")
    .replace(/\s+/g, " ")
    .trim();
}

export function inferProductBrand(value: string): string | undefined {
  const text = value.toLowerCase();
  const brands = ["Microsoft", "Kaspersky", "ESET", "Canva", "Autodesk", "Malwarebytes", "Panda"];
  return brands.find((brand) => text.includes(brand.toLowerCase()));
}

export function inferProductCategory(name: string, platform = "", declaredCategory = ""): string {
  const text = `${name} ${platform} ${declaredCategory}`.toLowerCase();
  if (/(combo|pack|paquete)/.test(text)) return CATEGORY_COMBOS;
  if (/(server|servidor|sql server|windows server)/.test(text)) return CATEGORY_SERVERS;
  if (/(office|microsoft 365|word|excel|powerpoint|outlook)/.test(text)) return CATEGORY_OFFICE;
  if (/(kaspersky|antivirus|seguridad|security|defender|eset|malwarebytes|panda)/.test(text)) return CATEGORY_SECURITY;
  if (/(autodesk|canva|adobe|corel|diseño|diseno|productividad)/.test(text)) return CATEGORY_DESIGN;
  if (/(windows|sistema operativo)/.test(text)) return CATEGORY_WINDOWS;
  return declaredCategory.trim() || CATEGORY_DESIGN;
}

export function defaultProductDescription(category: string): string {
  if (category === CATEGORY_WINDOWS) {
    return "Producto físico en la presentación indicada. Confirma medidas, cantidad y cobertura de entrega antes de comprar.";
  }
  if (category === CATEGORY_OFFICE) {
    return "Solución de productividad digital. Revisa la edición, la modalidad y los equipos compatibles antes de comprar.";
  }
  if (category === CATEGORY_SECURITY) {
    return "Solución de seguridad digital. Verifica la duración, la cantidad de dispositivos y el sistema compatible.";
  }
  if (category === CATEGORY_SERVERS) {
    return "Producto especializado para negocios. Confirma presentación, disponibilidad y requisitos de instalación antes de comprar.";
  }
  if (category === CATEGORY_COMBOS) {
    return "Paquete digital con productos relacionados. Revisa su contenido y compatibilidad antes de crear la orden.";
  }
  return "Software digital para productividad. Confirma la modalidad y compatibilidad antes de comprar.";
}

export function defaultProductBenefits(category: string): string[] {
  const compatibility = category === CATEGORY_SECURITY
    ? "Dispositivos compatibles identificados"
    : "Compatibilidad explicada";
  return [compatibility, "Entrega digital", "Soporte postventa"];
}

export function searchableProductText(product: StorefrontProduct): string {
  return [
    product.name,
    product.shortName,
    product.brand,
    product.description,
    product.category,
    product.subcategory,
    product.platform,
    ...product.benefits,
    ...product.commercialTags
  ].filter(Boolean).join(" ").toLowerCase();
}
