import type { CartItem } from "../types";

const cartStorageKey = "dismal.storefront.cart.v1";

export function readStoredCart(): CartItem[] {
  try {
    const rawValue = window.localStorage.getItem(cartStorageKey);
    if (!rawValue) {
      return [];
    }
    const parsed = JSON.parse(rawValue);
    if (!Array.isArray(parsed)) {
      return [];
    }
    const merged = new Map<string, number>();
    parsed
      .filter((item) => typeof item.productId === "string" && Number.isInteger(item.quantity))
      .forEach((item) => {
        const productId = item.productId.trim();
        if (!productId) return;
        const quantity = Math.min(Math.max(item.quantity, 1), 50);
        merged.set(productId, Math.min((merged.get(productId) || 0) + quantity, 50));
      });
    return Array.from(merged, ([productId, quantity]) => ({ productId, quantity }));
  } catch {
    return [];
  }
}

export function storeCart(items: CartItem[]): void {
  try {
    window.localStorage.setItem(cartStorageKey, JSON.stringify(items));
  } catch {
    // A blocked storage API must not interrupt the purchase session in memory.
  }
}

export function upsertCartItem(items: CartItem[], productId: string, quantity: number): CartItem[] {
  const safeQuantity = Math.min(Math.max(quantity, 1), 50);
  const existing = items.find((item) => item.productId === productId);
  if (!existing) {
    return [...items, { productId, quantity: safeQuantity }];
  }
  return items.map((item) =>
    item.productId === productId
      ? { ...item, quantity: Math.min(item.quantity + safeQuantity, 50) }
      : item
  );
}
