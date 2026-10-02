export type CountryCode = "EC" | "PE";

export type CustomerType = "final" | "wholesale";

export type StorefrontProduct = {
  id: string;
  slug: string;
  sku?: string;
  name: string;
  shortName: string;
  brand?: string;
  description: string;
  longDescription: string;
  platform: string;
  imageUrl?: string;
  gallery: string[];
  category: string;
  subcategory?: string;
  badge?: string;
  benefits: string[];
  compatibility: string[];
  requirements: string[];
  commercialTags: string[];
  licenseType?: string;
  activationMode?: string;
  duration?: string;
  devices?: string;
  language?: string;
  region?: string;
  delivery: string;
  support: string;
  finalPrice: number;
  wholesalePrice?: number;
  compareAtPrice?: number;
};

export type CartItem = {
  productId: string;
  quantity: number;
};

export type CheckoutCustomer = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  taxId: string;
  password?: string;
};

export type ShippingAddress = {
  recipient: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  region: string;
  postalCode: string;
  notes: string;
};
