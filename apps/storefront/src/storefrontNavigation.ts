export type StoreView =
  | "home"
  | "catalog"
  | "offers"
  | "wholesale"
  | "about"
  | "help"
  | "terms"
  | "privacy"
  | "refunds";

const routeByView: Record<StoreView, string> = {
  home: "/",
  catalog: "/catalogo",
  offers: "/ofertas",
  wholesale: "/distribuidores",
  about: "/nosotros",
  help: "/ayuda",
  terms: "/terminos",
  privacy: "/privacidad",
  refunds: "/garantia"
};

const viewByRoute = Object.entries(routeByView).reduce<Record<string, StoreView>>(
  (routes, [view, route]) => ({ ...routes, [route]: view as StoreView }),
  { "/mayoristas": "wholesale" }
);

function normalizePath(pathname: string): string {
  if (!pathname || pathname === "/index.html") return "/";
  const path = pathname.replace(/\/+$/, "") || "/";
  return path.toLowerCase();
}

export function routeForView(view: StoreView): string {
  return routeByView[view];
}

export function storefrontHref(view: StoreView): string {
  return routeForView(view);
}

export function productHref(slug: string): string {
  return `/productos/${encodeURIComponent(slug)}`;
}

export function resolveProductSlug(pathname = window.location.pathname): string | null {
  const match = normalizePath(pathname).match(/^\/productos\/([^/]+)$/);
  if (!match) return null;
  try {
    return decodeURIComponent(match[1]);
  } catch {
    return null;
  }
}

export function resolveStoreView(
  pathname = window.location.pathname,
  hash = window.location.hash
): StoreView {
  const legacyHashRoute = hash.startsWith("#/") ? normalizePath(hash.slice(1)) : "";
  return viewByRoute[legacyHashRoute || normalizePath(pathname)] || "home";
}
