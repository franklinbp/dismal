import { apiRequest } from "./api";

export type AccountOrder = {
  order: {
    orderId: string;
    orderNumber: string;
    status: string;
    country: "EC" | "PE";
    currency: string;
    customerType: "FINAL" | "DISTRIBUTOR";
    checkoutMethod: "BANK_TRANSFER" | "CUSTOMER_CREDIT";
    paymentRequired: boolean;
    total: number;
    paymentAccountId?: string;
    paymentClaimBank?: string;
    paymentClaimReference?: string;
    paymentClaimPayer?: string;
    paymentClaimAmount?: number;
    paymentClaimedAt?: string;
    paymentReviewNotes?: string;
    shippingRecipient?: string;
    shippingAddressLine1?: string;
    shippingAddressLine2?: string;
    shippingCity?: string;
    shippingRegion?: string;
    shippingPostalCode?: string;
    shippingCountry?: string;
    shippingNotes?: string;
    shippingCarrier?: string;
    trackingNumber?: string;
    shippedAt?: string;
    deliveredAt?: string;
    createdAt: string;
    items: Array<{
      productId: string;
      productName: string;
      platform?: string;
      quantity: number;
      unitPrice: number;
      subtotal: number;
    }>;
  };
};

export type AccountOrderPage = {
  content: AccountOrder[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export async function fetchAccountOrders(
  token: string,
  country: "EC" | "PE",
  page = 0,
  size = 20
): Promise<AccountOrderPage> {
  const safePage = Math.max(0, Math.trunc(page));
  const safeSize = Math.min(50, Math.max(1, Math.trunc(size)));
  return apiRequest<AccountOrderPage>(
    `/api/v1/storefront/account/orders?country=${country}&page=${safePage}&size=${safeSize}`,
    { headers: { Accept: "application/json", Authorization: `Bearer ${token}` } }
  );
}

export async function cancelAccountOrder(
  token: string,
  orderNumber: string
): Promise<AccountOrder["order"]> {
  return apiRequest<AccountOrder["order"]>(
    `/api/v1/storefront/account/orders/${encodeURIComponent(orderNumber)}/cancel`,
    {
      method: "POST",
      headers: { Accept: "application/json", Authorization: `Bearer ${token}` }
    }
  );
}
