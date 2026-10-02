import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const routes = [
  "/",
  "/catalogo",
  "/ofertas",
  "/distribuidores",
  "/nosotros",
  "/ayuda",
  "/terminos",
  "/privacidad",
  "/garantia"
];

const sites = [
  { build: "ecuador", domain: "https://www.dismal.net", language: "es-EC" },
  { build: "peru", domain: "https://www.dismal.net.pe", language: "es-PE" }
];

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

for (const site of sites) {
  const root = resolve("dist", site.build);
  const requiredFiles = [
    ".htaccess",
    "index.html",
    "robots.txt",
    "sitemap.xml",
    "assets/dismal-logo.png",
    "assets/dismal-store-hero-v2.webp"
  ];

  for (const file of requiredFiles) {
    assert(existsSync(resolve(root, file)), `[${site.build}] Missing ${file}`);
  }

  const html = readFileSync(resolve(root, "index.html"), "utf8");
  const sitemap = readFileSync(resolve(root, "sitemap.xml"), "utf8");
  const htaccess = readFileSync(resolve(root, ".htaccess"), "utf8");

  assert(html.includes(`<html lang="${site.language}">`), `[${site.build}] Invalid language`);
  assert(html.includes(`rel="canonical" href="${site.domain}/"`), `[${site.build}] Invalid canonical URL`);
  assert(html.includes(`${site.domain}/assets/dismal-store-hero-v2.webp`), `[${site.build}] Missing social image`);
  assert(html.includes('hreflang="es-EC"'), `[${site.build}] Missing Ecuador alternate`);
  assert(html.includes('hreflang="es-PE"'), `[${site.build}] Missing Peru alternate`);
  assert(htaccess.includes("RewriteRule ^ index.html [L]"), `[${site.build}] Missing SPA rewrite`);
  assert(htaccess.includes("^sample-page/?$"), `[${site.build}] Missing legacy page redirect`);
  assert(htaccess.includes("^mayoristas/?$"), `[${site.build}] Missing distributor legacy redirect`);
  assert(htaccess.includes("Content-Security-Policy"), `[${site.build}] Missing CSP`);

  for (const route of routes) {
    assert(sitemap.includes(`<loc>${site.domain}${route}</loc>`), `[${site.build}] Missing route ${route}`);
  }
}

console.log("Storefront production builds verified for Ecuador and Peru.");
