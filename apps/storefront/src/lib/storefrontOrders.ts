import type { CartItem, CheckoutCustomer, CountryCode, CustomerType, ShippingAddress } from "../types";
import { apiRequest, getApiBaseUrl } from "./api";

export type StorefrontOrderRequest = {
  country: CountryCode;
  customerType: CustomerType;
  checkoutMethod: "BANK_TRANSFER" | "CUSTOMER_CREDIT";
  customer: CheckoutCustomer;
  shipping: ShippingAddress;
  items: CartItem[];
};

export type StorefrontOrderResponse = {
  orderId: string;
  orderNumber: string;
  status: "PENDING_PAYMENT" | "PAYMENT_REVIEW" | "PAID" | "PREPARING" | "READY_FOR_DISPATCH" | "SHIPPED" | "DELIVERED" | "CANCELLED" | "FAILED" | "REFUNDED";
  country: CountryCode;
  currency: string;
  customerType: "FINAL" | "DISTRIBUTOR";
  checkoutMethod: "BANK_TRANSFER" | "CUSTOMER_CREDIT";
  paymentRequired: boolean;
  email?: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  taxId?: string;
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
  subtotal: number;
  discountTotal: number;
  total: number;
  paymentAccountId?: string;
  paymentClaimBank?: string;
  paymentClaimReference?: string;
  paymentClaimPayer?: string;
  paymentClaimAmount?: number;
  paymentClaimedAt?: string;
  paymentReviewNotes?: string;
  createdAt: string;
  items: Array<{
    productId: string;
    productName: string;
    platform?: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
  }>;
  paymentUrl?: string;
  demo?: boolean;
};

export async function createStorefrontOrder(
  request: StorefrontOrderRequest,
  token?: string
): Promise<StorefrontOrderResponse> {
  if (!getApiBaseUrl()) {
    return {
      orderId: `local-${Date.now()}`,
      orderNumber: "DEMO",
      status: "PENDING_PAYMENT",
      country: request.country,
      currency: "USD",
      customerType: request.customerType === "wholesale" ? "DISTRIBUTOR" : "FINAL",
      checkoutMethod: request.checkoutMethod,
      paymentRequired: true,
      subtotal: 0,
      discountTotal: 0,
      total: 0,
      createdAt: new Date().toISOString(),
      items: [],
      demo: true
    };
  }

  return apiRequest<StorefrontOrderResponse>("/api/public/storefront/orders", {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(request)
  }, 15_000);
}

export type StorefrontPaymentAccount = {
  id: string;
  name: string;
  country: CountryCode;
  currency: string;
  bankName: string;
  accountHolder: string;
  accountNumber: string;
  accountType?: string;
  taxId?: string;
};

export async function fetchStorefrontPaymentAccounts(
  country: CountryCode
): Promise<StorefrontPaymentAccount[]> {
  if (!getApiBaseUrl()) return [];
  return apiRequest<StorefrontPaymentAccount[]>(
    `/api/public/storefront/payment-accounts?country=${encodeURIComponent(country)}`,
    { headers: { Accept: "application/json" } }
  );
}

export async function submitStorefrontPaymentClaim(
  orderNumber: string,
  token: string,
  claim: {
    paymentAccountId: string;
    payerName: string;
    sourceBank: string;
    reference: string;
    amount: number;
  }
): Promise<StorefrontOrderResponse> {
  return apiRequest<StorefrontOrderResponse>(
    `/api/v1/storefront/account/orders/${encodeURIComponent(orderNumber)}/payment-claim`,
    {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify(claim)
    },
    15_000
  );
}
