import {
  Boxes,
  FileSpreadsheet,
  MonitorCog,
  Palette,
  ServerCog,
  ShieldCheck,
  type LucideIcon
} from "lucide-react";
import { useState } from "react";
import type { StorefrontProduct } from "../types";

const categoryIcons: Array<[RegExp, LucideIcon, string]> = [
  [/windows/i, MonitorCog, "windows"],
  [/(office|microsoft 365)/i, FileSpreadsheet, "office"],
  [/(antivirus|seguridad)/i, ShieldCheck, "security"],
  [/(diseño|productividad)/i, Palette, "design"],
  [/servidor/i, ServerCog, "server"],
  [/combo/i, Boxes, "combo"]
];

type ProductArtworkProps = {
  product: StorefrontProduct;
  large?: boolean;
};

export default function ProductArtwork({ product, large = false }: ProductArtworkProps) {
  const [imageFailed, setImageFailed] = useState(false);

  if (product.imageUrl && !imageFailed) {
    return (
      <img
        src={product.imageUrl}
        alt={`Presentación de ${product.name}`}
        loading={large ? "eager" : "lazy"}
        decoding="async"
        referrerPolicy="no-referrer"
        onError={() => setImageFailed(true)}
      />
    );
  }

  const [, Icon, style] = categoryIcons.find(([pattern]) => pattern.test(product.category))
    || [/.*/, Boxes, "default"];

  return (
    <div className={`product-artwork product-artwork-${style}${large ? " large" : ""}`} aria-hidden="true">
      <div className="artwork-symbol"><Icon /></div>
      <div className="artwork-lines"><span /><span /><span /></div>
      <small>{product.brand || product.category}</small>
    </div>
  );
}
