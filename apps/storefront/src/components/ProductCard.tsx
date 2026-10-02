import { Check, ChevronRight, ShoppingCart } from "lucide-react";
import type { StorefrontProduct } from "../types";
import ProductArtwork from "./ProductArtwork";

type ProductCardProps = {
  product: StorefrontProduct;
  formattedPrice: string;
  formattedCompareAtPrice?: string;
  priceLabel: string;
  onBuyNow: () => void;
  onDetails: () => void;
};

export default function ProductCard({
  product,
  formattedPrice,
  formattedCompareAtPrice,
  priceLabel,
  onBuyNow,
  onDetails
}: ProductCardProps) {
  return (
    <article className="product-card">
      <button className="product-media" type="button" onClick={onDetails} aria-label={`Ver ${product.name}`}>
        {product.badge ? <span className="product-badge">{product.badge}</span> : null}
        <ProductArtwork product={product} />
      </button>
      <div className="product-body">
        <span className="product-kicker">{product.category} · {product.platform}</span>
        <span className="product-kicker">{product.physicalProduct ? "Producto físico" : "Producto digital"} · {product.inStock ? `${product.availableQuantity} disponibles` : "Agotado temporalmente"}</span>
        <button className="product-title" type="button" onClick={onDetails}>{product.name}</button>
        <p>{product.description}</p>
        <ul>
          {product.benefits.slice(0, 3).map((benefit) => (
            <li key={benefit}><Check size={14} aria-hidden="true" />{benefit}</li>
          ))}
        </ul>
      </div>
      <div className="product-purchase">
        <div className="price-block">
          {formattedCompareAtPrice ? <small>{formattedCompareAtPrice}</small> : null}
          <strong>{formattedPrice}</strong>
          <span>{priceLabel}</span>
        </div>
        <div className="product-actions">
          <button className="product-detail-button" type="button" onClick={onDetails} aria-label={`Ver detalles de ${product.name}`}>
            Ver detalles <ChevronRight size={16} aria-hidden="true" />
          </button>
          <button className="add-button" type="button" disabled={!product.inStock} onClick={onBuyNow} aria-label={`Comprar ahora ${product.name}`}>
            <ShoppingCart size={17} aria-hidden="true" /> {product.inStock ? "Comprar" : "Agotado"}
          </button>
        </div>
      </div>
    </article>
  );
}
