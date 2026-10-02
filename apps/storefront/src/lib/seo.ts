import type { CountryConfig } from "./country";
import { routeForView, type StoreView } from "../storefrontNavigation";
import type { StorefrontProduct } from "../types";

type PageMetadata = { title: string; description: string };

const metadata: Record<StoreView, PageMetadata> = {
  home: {
    title: "Productos físicos para hogares y negocios",
    description: "Compra productos físicos con precios claros, existencias verificadas, seguimiento y soporte Dismal."
  },
  catalog: {
    title: "Catálogo de productos",
    description: "Compara productos, presentaciones, precios y condiciones de entrega antes de crear tu pedido."
  },
  offers: {
    title: "Ofertas y productos destacados",
    description: "Consulta promociones vigentes de software con validación de precio, país y disponibilidad."
  },
  wholesale: {
    title: "Programa para distribuidores de software",
    description: "Acceso para distribuidores aprobados con precios privados, historial y compras por volumen."
  },
  about: {
    title: "Quiénes somos",
    description: "Conoce cómo Dismal conecta tienda, administración, pagos, inventario y entregas físicas."
  },
  help: {
    title: "Centro de ayuda",
    description: "Respuestas sobre productos, entrega, garantía y pedidos de Dismal."
  },
  terms: {
    title: "Términos de compra",
    description: "Condiciones generales aplicables a órdenes, pagos y entrega digital en la tienda Dismal."
  },
  privacy: {
    title: "Privacidad y seguridad",
    description: "Información sobre los datos utilizados para procesar compras, coordinar entregas y brindar soporte."
  },
  refunds: {
    title: "Garantía y devoluciones",
    description: "Consulta el proceso de soporte, revisión, reemplazo y devoluciones para productos digitales."
  }
};

function setNamedMeta(name: string, content: string): void {
  let element = document.head.querySelector<HTMLMetaElement>(`meta[name="${name}"]`);
  if (!element) {
    element = document.createElement("meta");
    element.name = name;
    document.head.appendChild(element);
  }
  element.content = content;
}

function setPropertyMeta(property: string, content: string): void {
  let element = document.head.querySelector<HTMLMetaElement>(`meta[property="${property}"]`);
  if (!element) {
    element = document.createElement("meta");
    element.setAttribute("property", property);
    document.head.appendChild(element);
  }
  element.content = content;
}

function setCanonical(url: string): void {
  let canonical = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
  if (!canonical) {
    canonical = document.createElement("link");
    canonical.rel = "canonical";
    document.head.appendChild(canonical);
  }
  canonical.href = url;
}

function setStructuredData(value: Record<string, unknown> | null): void {
  const id = "dismal-structured-data";
  document.getElementById(id)?.remove();
  if (!value) return;
  const script = document.createElement("script");
  script.id = id;
  script.type = "application/ld+json";
  script.text = JSON.stringify(value);
  document.head.appendChild(script);
}

export function updatePageMetadata(view: StoreView, country: CountryConfig): void {
  const page = metadata[view];
  const siteName = `Dismal ${country.name}`;
  const url = `https://${country.domain}${routeForView(view)}`;
  document.title = `${page.title} | ${siteName}`;
  document.documentElement.lang = country.locale;
  setNamedMeta("description", page.description);
  setPropertyMeta("og:title", `${page.title} | ${siteName}`);
  setPropertyMeta("og:description", page.description);
  setPropertyMeta("og:url", url);
  setPropertyMeta("og:locale", country.locale.replace("-", "_"));

  setCanonical(url);
  setPropertyMeta("og:type", "website");
  setStructuredData(null);
}

export function updateProductMetadata(product: StorefrontProduct, country: CountryConfig): void {
  const siteName = `Dismal ${country.name}`;
  const title = `${product.name} | ${siteName}`;
  const description = product.description.slice(0, 160);
  const url = `https://${country.domain}/productos/${encodeURIComponent(product.slug)}`;
  document.title = title;
  document.documentElement.lang = country.locale;
  setNamedMeta("description", description);
  setPropertyMeta("og:title", title);
  setPropertyMeta("og:description", description);
  setPropertyMeta("og:url", url);
  setPropertyMeta("og:type", "product");
  setPropertyMeta("og:locale", country.locale.replace("-", "_"));
  if (product.imageUrl) setPropertyMeta("og:image", product.imageUrl);
  setCanonical(url);
  setStructuredData({
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    description,
    ...(product.sku ? { sku: product.sku } : {}),
    ...(product.brand ? { brand: { "@type": "Brand", name: product.brand } } : {}),
    ...(product.imageUrl ? { image: [product.imageUrl] } : {}),
    offers: {
      "@type": "Offer",
      url,
      priceCurrency: country.currency,
      price: product.finalPrice
    }
  });
}
