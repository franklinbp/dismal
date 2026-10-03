import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

type SiteMetadata = {
  domain: string;
  language: string;
  title: string;
  description: string;
};

const site: SiteMetadata = {
  domain: "https://dismalec.com",
  language: "es-EC",
  title: "Dismal Ecuador | Tecnología y licencias digitales",
  description: "Compra computadoras, impresoras, telefonía móvil, discos SSD y licencias digitales en Ecuador."
};

const publicRoutes = ["/", "/catalogo", "/ofertas", "/distribuidores", "/nosotros", "/ayuda", "/terminos", "/privacidad", "/garantia"];

function metadataPlugin(site: SiteMetadata) {
  return {
    name: "dismal-storefront-metadata",
    transformIndexHtml(html: string) {
      return html
        .replace('<html lang="es">', `<html lang="${site.language}">`)
        .replace("__DISMAL_STOREFRONT_DESCRIPTION__", site.description)
        .replace("<title>Dismal Store</title>", `<title>${site.title}</title>\n    <link rel="canonical" href="${site.domain}/" />\n    <link rel="alternate" hreflang="es-EC" href="${site.domain}/" />\n    <link rel="alternate" hreflang="x-default" href="${site.domain}/" />\n    <meta property="og:title" content="${site.title}" />\n    <meta property="og:description" content="${site.description}" />\n    <meta property="og:type" content="website" />\n    <meta property="og:site_name" content="Dismal" />\n    <meta property="og:locale" content="${site.language.replace("-", "_")}" />\n    <meta property="og:url" content="${site.domain}/" />\n    <meta property="og:image" content="${site.domain}/assets/dismal-store-hero-v2.webp" />\n    <meta name="twitter:card" content="summary_large_image" />`);
    },
    generateBundle(this: { emitFile: (asset: { type: "asset"; fileName: string; source: string }) => void }) {
      this.emitFile({
        type: "asset",
        fileName: "robots.txt",
        source: `User-agent: *\nAllow: /\nSitemap: ${site.domain}/sitemap.xml\n`
      });
      this.emitFile({
        type: "asset",
        fileName: "sitemap.xml",
        source: `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${publicRoutes.map((route) => `  <url><loc>${site.domain}${route}</loc><changefreq>${route === "/" || route === "/catalogo" ? "weekly" : "monthly"}</changefreq><priority>${route === "/" ? "1.0" : route === "/catalogo" ? "0.9" : "0.6"}</priority></url>`).join("\n")}\n</urlset>\n`
      });
    }
  };
}

export default defineConfig(({ mode }) => {
  const countryMode = "ecuador";
  const outDir = mode === "peru" || mode === "ecuador" ? `dist/${countryMode}` : "dist";
  return {
    plugins: [react(), metadataPlugin(site)],
    build: {
      outDir,
      emptyOutDir: true,
      sourcemap: false
    }
  };
});
