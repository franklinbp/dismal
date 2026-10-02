import { getFallbackProducts } from "../data/fallbackProducts";
import type { CountryCode, StorefrontProduct } from "../types";
import { apiRequest, getApiBaseUrl } from "./api";
import {
  defaultProductBenefits,
  defaultProductDescription,
  inferProductBrand,
  inferProductCategory,
  normalizeProductName,
  slugifyProduct
} from "./productPresentation";

type PublicProductResponse = {
  id?: string;
  name?: string;
  description?: string;
  platform?: string;
  imageUrl?: string;
  sku?: string;
  brand?: string;
  physicalProduct?: boolean;
  availableQuantity?: number;
  inStock?: boolean;
  category?: string;
  subcategory?: string;
  compatibility?: string[];
  requirements?: string[];
  tags?: string[];
  licenseType?: string;
  activationMode?: string;
  duration?: string;
  devices?: string;
  language?: string;
  region?: string;
  publicPrice?: number | string;
  effectivePrice?: number | string;
  finalPrice?: number | string;
  wholesalePrice?: number | string | null;
  priceType?: string;
};

function toOptionalNumber(value: number | string | null | undefined): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return undefined;
}

function sanitizeImageUrl(value?: string): string | undefined {
  if (!value?.trim()) return undefined;
  try {
    const normalizedValue = value.trim().replace(/\\/g, "/");
    const isAbsolute = /^https?:\/\//i.test(normalizedValue);
    const storefrontOrigin = window.location.origin;
    const relativePath = normalizedValue.startsWith("/")
      ? normalizedValue
      : `/${normalizedValue.replace(/^\.\//, "")}`;
    const url = isAbsolute
      ? new URL(normalizedValue)
      : new URL(relativePath, `${storefrontOrigin}/`);
    const localHttp = url.protocol === "http:" && ["localhost", "127.0.0.1"].includes(url.hostname);
    return url.protocol === "https:" || localHttp ? url.href : undefined;
  } catch {
    return undefined;
  }
}

function fromApiProduct(product: PublicProductResponse): StorefrontProduct | null {
  const id = product.id?.trim();
  const sourceName = product.name?.trim();
  const finalPrice = toOptionalNumber(product.finalPrice)
    ?? toOptionalNumber(product.effectivePrice)
    ?? toOptionalNumber(product.publicPrice);

  if (!id || !sourceName || finalPrice === undefined || finalPrice <= 0) return null;

  const name = normalizeProductName(sourceName);
  const category = inferProductCategory(name, product.platform, product.category);
  const wholesalePrice = toOptionalNumber(product.wholesalePrice);
  const publicPrice = toOptionalNumber(product.publicPrice);
  const sourceDescription = product.description?.trim() || "";
  const physicalProduct = product.physicalProduct !== false;
  const availableQuantity = Math.max(0, Math.trunc(toOptionalNumber(product.availableQuantity) ?? 0));
  const genericDescription = /licencia digital.*(entrega|soporte)|software original/i.test(sourceDescription);
  const description = !sourceDescription || genericDescription
    ? defaultProductDescription(category)
    : sourceDescription;

  return {
    id,
    slug: slugifyProduct(name),
    sku: product.sku?.trim() || undefined,
    name,
    shortName: name,
    brand: product.brand?.trim() || inferProductBrand(name),
    physicalProduct,
    availableQuantity,
    inStock: product.inStock === true && availableQuantity > 0,
    description,
    longDescription: description,
    platform: product.platform?.trim() || "Digital",
    imageUrl: sanitizeImageUrl(product.imageUrl),
    gallery: [],
    category,
    subcategory: product.subcategory?.trim() || undefined,
    badge: publicPrice && publicPrice > finalPrice ? "Oferta" : undefined,
    benefits: defaultProductBenefits(category),
    compatibility: Array.isArray(product.compatibility) ? product.compatibility.filter(Boolean) : [],
    requirements: Array.isArray(product.requirements) ? product.requirements.filter(Boolean) : [],
    commercialTags: Array.isArray(product.tags) ? product.tags.filter(Boolean) : [],
    licenseType: product.licenseType?.trim() || undefined,
    activationMode: product.activationMode?.trim() || undefined,
    duration: product.duration?.trim() || undefined,
    devices: product.devices?.trim() || undefined,
    language: product.language?.trim() || undefined,
    region: product.region?.trim() || undefined,
    delivery: physicalProduct
      ? "Despacho físico después de confirmar el pago y las existencias."
      : "Entrega digital después de confirmar el pago.",
    support: physicalProduct
      ? "Soporte postventa para cambios, garantía y seguimiento."
      : "Soporte para la activación y uso de tu producto digital.",
    finalPrice,
    wholesalePrice: wholesalePrice && wholesalePrice > 0 ? wholesalePrice : undefined,
    compareAtPrice: publicPrice && publicPrice > finalPrice ? publicPrice : undefined
  };
}

export async function fetchProducts(country: CountryCode, token?: string): Promise<StorefrontProduct[]> {
  if (!getApiBaseUrl()) return getFallbackProducts(country);

  const data = await apiRequest<PublicProductResponse[]>(
    `/api/public/products?country=${encodeURIComponent(country)}`,
    {
      headers: {
        Accept: "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      }
    },
    8_000
  );

  if (!Array.isArray(data)) return [];
  return data.map(fromApiProduct).filter((product): product is StorefrontProduct => product !== null);
}
