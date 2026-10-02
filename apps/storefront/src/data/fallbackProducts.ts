import type { CountryCode, StorefrontProduct } from "../types";

const ecProducts: StorefrontProduct[] = [
  {
    id: "office-365-personal",
    slug: "microsoft-365-personal",
    name: "Microsoft 365 Personal",
    shortName: "Microsoft 365 Personal",
    brand: "Microsoft",
    description: "Suscripción digital para productividad, documentos y almacenamiento en la nube.",
    longDescription: "Suscripción digital para productividad, documentos y almacenamiento en la nube.",
    platform: "Windows, macOS, Web",
    gallery: [],
    category: "Microsoft 365 y Office",
    badge: "Más vendido",
    benefits: ["Activación guiada", "Entrega digital", "Soporte post-compra"],
    compatibility: [],
    requirements: [],
    commercialTags: [],
    delivery: "Entrega digital después de confirmar el pago y la disponibilidad.",
    support: "Soporte postventa para orientación de activación.",
    finalPrice: 19.99,
    wholesalePrice: 15.5,
    compareAtPrice: 24.99
  },
  {
    id: "windows-11-pro",
    slug: "windows-11-pro",
    name: "Windows 11 Pro",
    shortName: "Windows 11 Pro",
    brand: "Microsoft",
    description: "Producto físico para uso profesional, con disponibilidad y entrega verificadas.",
    longDescription: "Producto físico para uso profesional, con disponibilidad y entrega verificadas.",
    platform: "Windows",
    gallery: [],
    category: "Windows",
    badge: "Oferta",
    benefits: ["Compatibilidad explicada", "Instrucciones claras", "Soporte postventa"],
    compatibility: [],
    requirements: [],
    commercialTags: [],
    delivery: "Entrega digital después de confirmar el pago y la disponibilidad.",
    support: "Soporte postventa para orientación de activación.",
    finalPrice: 16.99,
    wholesalePrice: 12.25,
    compareAtPrice: 29.99
  },
  {
    id: "kaspersky-standard",
    slug: "kaspersky-standard",
    name: "Kaspersky Standard",
    shortName: "Kaspersky Standard",
    brand: "Kaspersky",
    description: "Protección esencial para navegar, comprar y trabajar con mayor tranquilidad.",
    longDescription: "Protección esencial para navegar, comprar y trabajar con mayor tranquilidad.",
    platform: "Windows, Android",
    gallery: [],
    category: "Antivirus y seguridad",
    benefits: ["Protección anual", "Instalación asistida", "Renovación disponible"],
    compatibility: [],
    requirements: [],
    commercialTags: [],
    delivery: "Entrega digital después de confirmar el pago y la disponibilidad.",
    support: "Soporte postventa para orientación de activación.",
    finalPrice: 13.99,
    wholesalePrice: 10.75
  }
];

const peProducts: StorefrontProduct[] = ecProducts.map((product) => ({
  ...product,
  finalPrice: Number((product.finalPrice * 3.75).toFixed(2)),
  wholesalePrice: Number(((product.wholesalePrice ?? product.finalPrice) * 3.75).toFixed(2)),
  compareAtPrice: product.compareAtPrice ? Number((product.compareAtPrice * 3.75).toFixed(2)) : undefined
}));

export function getFallbackProducts(country: CountryCode): StorefrontProduct[] {
  return country === "PE" ? peProducts : ecProducts;
}
