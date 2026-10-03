"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency } from "@/lib/format";
import { PlusIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type Product = {
  id: string;
  name: string;
  description: string | null;
  price: number;
  ecFinalPrice?: number | null;
  ecDistributorPrice?: number | null;
  peFinalPrice?: number | null;
  peDistributorPrice?: number | null;
  platform: string | null;
  imageUrl: string | null;
  sku?: string | null;
  barcode?: string | null;
  brand?: string | null;
  physicalProduct?: boolean;
  stockQuantity?: number | null;
  reservedQuantity?: number | null;
};

type StockSummary = {
  softwareId: string;
  softwareName: string;
  available: number;
  assigned: number;
};

type Customer = {
  id: string;
  firstname: string;
  lastname: string;
  email: string;
  phone?: string | null;
};

type PriceListSelection = {
  includeEcFinalPrice: boolean;
  includeEcDistributorPrice: boolean;
  includePeFinalPrice: boolean;
  includePeDistributorPrice: boolean;
  includeStock: boolean;
};

type PriceListAction =
  | "download-pdf"
  | "download-xlsx"
  | "download-text"
  | "email"
  | "generate-whatsapp"
  | "send-whatsapp";

const defaultPriceListSelection: PriceListSelection = {
  includeEcFinalPrice: true,
  includeEcDistributorPrice: true,
  includePeFinalPrice: false,
  includePeDistributorPrice: false,
  includeStock: true,
};

export default function AdminProductsPage() {
  const router = useRouter();
  const { user, allowed } = useAdminGuard("ADMIN_MANAGER");
  const formPanelRef = useRef<HTMLDivElement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [selected, setSelected] = useState<Product | null>(null);
  const [stock, setStock] = useState<StockSummary[]>([]);
  const [search, setSearch] = useState("");
  const [isCreating, setIsCreating] = useState(false);
  const [priceListLoading, setPriceListLoading] = useState(false);
  const [priceListError, setPriceListError] = useState<string | null>(null);
  const [priceListNotice, setPriceListNotice] = useState<string | null>(null);
  const [whatsappText, setWhatsappText] = useState<string | null>(null);
  const [whatsappUrl, setWhatsappUrl] = useState<string | null>(null);
  const [priceListAction, setPriceListAction] = useState<PriceListAction | null>(null);
  const [priceListSelection, setPriceListSelection] = useState<PriceListSelection>(defaultPriceListSelection);
  const customerSearchRef = useRef<HTMLDivElement | null>(null);
  const [customerSearchTerm, setCustomerSearchTerm] = useState("");
  const [isCustomerMenuOpen, setIsCustomerMenuOpen] = useState(false);
  const [sendForm, setSendForm] = useState({
    clientName: "",
    toEmail: "",
    subject: "Lista completa de precios - Dismal",
    whatsappPhone: "",
    includePdf: true,
    includeXlsx: true,
  });

  const [form, setForm] = useState({
    name: "",
    description: "",
    ecFinalPrice: "",
    ecDistributorPrice: "",
    peFinalPrice: "",
    peDistributorPrice: "",
    platform: "",
    imageUrl: "",
    sku: "",
    barcode: "",
    brand: "",
    physicalProduct: true,
    stockQuantity: "0",
  });

  const loadProducts = useCallback(async () => {
    const data = await apiFetch<Product[]>("/api/backend/api/v1/store/products");
    setProducts(data);
    if (!selected && data.length > 0 && !isCreating) {
      setSelected(data[0]);
    }
  }, [selected, isCreating]);

  const loadStock = useCallback(async () => {
    const data = await apiFetch<StockSummary[]>("/api/backend/api/v1/inventory/stock");
    setStock(data);
  }, []);

  const loadCustomers = useCallback(async () => {
    try {
      const data = await apiFetch<Customer[]>("/api/backend/api/v1/customers");
      setCustomers(data);
    } catch {
      setCustomers([]);
    }
  }, []);

  useEffect(() => {
    const boot = async () => {
      if (!allowed || !user) return;
      setError(null);
      try {
        await Promise.all([loadProducts(), loadStock(), loadCustomers()]);
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar productos.");
      }
    };
    boot();
  }, [allowed, user, loadProducts, loadStock, loadCustomers, router]);

  useEffect(() => {
    const handlePointerDown = (event: MouseEvent) => {
      if (!customerSearchRef.current?.contains(event.target as Node)) {
        setIsCustomerMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  useEffect(() => {
    if (!selected) return;
    setIsCreating(false);
    setForm({
      name: selected.name || "",
      description: selected.description || "",
      ecFinalPrice: String(selected.ecFinalPrice ?? selected.price ?? ""),
      ecDistributorPrice: String(selected.ecDistributorPrice ?? ""),
      peFinalPrice: String(selected.peFinalPrice ?? ""),
      peDistributorPrice: String(selected.peDistributorPrice ?? ""),
      platform: selected.platform || "",
      imageUrl: selected.imageUrl || "",
      sku: selected.sku || "",
      barcode: selected.barcode || "",
      brand: selected.brand || "",
      physicalProduct: selected.physicalProduct === true,
      stockQuantity: String(selected.stockQuantity ?? 0),
    });
  }, [selected]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleCreate = async () => {
    setError(null);
    try {
      const ecFinalPrice = Number(form.ecFinalPrice);
      const ecDistributorPrice = Number(form.ecDistributorPrice);
      if (
        !form.name.trim()
        || [ecFinalPrice, ecDistributorPrice].some(
          (value) => Number.isNaN(value) || value <= 0
        )
      ) {
        setError("Completa el nombre y los precios de Ecuador para guardar.");
        return;
      }
      const payload = {
        name: form.name,
        description: form.description || null,
        price: ecFinalPrice,
        ecFinalPrice,
        ecDistributorPrice,
        peFinalPrice: ecFinalPrice,
        peDistributorPrice: ecDistributorPrice,
        platform: form.platform || null,
        imageUrl: form.imageUrl || null,
        sku: form.sku.trim() || null,
        barcode: form.barcode.trim() || null,
        brand: form.brand.trim() || null,
        physicalProduct: form.physicalProduct,
        stockQuantity: form.physicalProduct ? Math.max(0, Number(form.stockQuantity) || 0) : 0,
      };
      const created = await apiFetch<Product>("/api/backend/api/v1/store/products", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setNotice("Producto creado.");
      setSelected(created);
      setIsCreating(false);
      await loadProducts();
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo crear el producto.");
    }
  };

  const handleUpdate = async () => {
    if (!selected) return;
    setError(null);
    try {
      const ecFinalPrice = Number(form.ecFinalPrice);
      const ecDistributorPrice = Number(form.ecDistributorPrice);
      if (
        !form.name.trim()
        || [ecFinalPrice, ecDistributorPrice].some(
          (value) => Number.isNaN(value) || value <= 0
        )
      ) {
        setError("Completa el nombre y los precios de Ecuador para guardar.");
        return;
      }
      const payload = {
        name: form.name,
        description: form.description || null,
        price: ecFinalPrice,
        ecFinalPrice,
        ecDistributorPrice,
        peFinalPrice: ecFinalPrice,
        peDistributorPrice: ecDistributorPrice,
        platform: form.platform || null,
        imageUrl: form.imageUrl || null,
        sku: form.sku.trim() || null,
        barcode: form.barcode.trim() || null,
        brand: form.brand.trim() || null,
        physicalProduct: form.physicalProduct,
        stockQuantity: form.physicalProduct ? Math.max(0, Number(form.stockQuantity) || 0) : 0,
      };
      const updated = await apiFetch<Product>(`/api/backend/api/v1/store/products/${selected.id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      setSelected(updated);
      await loadProducts();
      setNotice("Producto actualizado.");
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar el producto.");
    }
  };

  const handleDelete = async () => {
    if (!selected) return;
    setError(null);
    try {
      await apiFetch(`/api/backend/api/v1/store/product/${selected.id}?force=true`, { method: "DELETE" });
      setSelected(null);
      setIsCreating(false);
      await loadProducts();
      setNotice("Producto eliminado.");
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo eliminar el producto.");
    }
  };

  const handleNew = () => {
    setSelected(null);
    setIsCreating(true);
    setForm({
      name: "",
      description: "",
      ecFinalPrice: "",
      ecDistributorPrice: "",
      peFinalPrice: "",
      peDistributorPrice: "",
      platform: "",
      imageUrl: "",
      sku: "",
      barcode: "",
      brand: "",
      physicalProduct: true,
      stockQuantity: "0",
    });
    window.requestAnimationFrame(() => {
      formPanelRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  };

  const handleCancelCreate = () => {
    setIsCreating(false);
    setError(null);
    setNotice(null);
    if (products.length > 0) {
      setSelected(products[0]);
    }
  };

  const handleRefresh = async () => {
    setError(null);
    try {
      await Promise.all([loadProducts(), loadStock(), loadCustomers()]);
    } catch (err: any) {
      setError(err?.message || "No se pudo recargar.");
    }
  };

  const customerOptions = useMemo(() => {
    const term = customerSearchTerm.trim().toLowerCase();
    const source = term
      ? customers.filter((customer) => {
          const haystack = `${customer.firstname} ${customer.lastname} ${customer.email} ${customer.phone || ""}`.toLowerCase();
          return haystack.includes(term);
        })
      : customers;
    return source.slice(0, 8);
  }, [customers, customerSearchTerm]);

  const selectCustomerForSend = (customer: Customer) => {
    const fullName = `${customer.firstname || ""} ${customer.lastname || ""}`.trim();
    setSendForm((prev) => ({
      ...prev,
      clientName: fullName || customer.email,
      toEmail: customer.email || prev.toEmail,
      whatsappPhone: customer.phone || prev.whatsappPhone,
    }));
    setCustomerSearchTerm(fullName || customer.email);
    setIsCustomerMenuOpen(false);
  };

  const normalizeEmail = (value: string) => value.replace(/[\u200B\uFEFF]/g, "").trim();
  const normalizePhone = (value: string) => value.replace(/\D/g, "");

  const handlePriceListAction = async (action: Exclude<PriceListAction, "download-pdf" | "download-xlsx" | "download-text">) => {
    setPriceListError(null);
    setPriceListNotice(null);
    setPriceListLoading(true);
    try {
      const payload = {
        toEmail: normalizeEmail(sendForm.toEmail),
        subject: sendForm.subject.trim(),
        clientName: (sendForm.clientName.trim() || customerSearchTerm.trim()),
        whatsappPhone: normalizePhone(sendForm.whatsappPhone),
        includePdf: true,
        includeXlsx: true,
        includeEcFinalPrice: priceListSelection.includeEcFinalPrice,
        includeEcDistributorPrice: priceListSelection.includeEcDistributorPrice,
        includePeFinalPrice: priceListSelection.includePeFinalPrice,
        includePeDistributorPrice: priceListSelection.includePeDistributorPrice,
        includeStock: priceListSelection.includeStock,
        sendWhatsapp: action === "send-whatsapp",
      };
      const data = await apiFetch<{
        whatsappText: string;
        whatsappUrl?: string | null;
        emailSent: boolean;
        whatsappSent: boolean;
        emailMessage?: string | null;
        whatsappMessage?: string | null;
      }>("/api/backend/api/v1/reports/price-list/send", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setWhatsappText(data.whatsappText || null);
      setWhatsappUrl(data.whatsappUrl || null);
      if (action === "email" && data.emailSent) {
        setPriceListNotice(data.emailMessage || "Lista enviada por email con PDF y Excel.");
      } else if (action === "email") {
        setPriceListError(data.emailMessage || "No se pudo enviar el email.");
      } else if (data.whatsappSent) {
        setPriceListNotice(data.whatsappMessage || "Lista enviada por WhatsApp mediante DismalCRM.");
      } else if (action === "send-whatsapp") {
        setPriceListError(data.whatsappMessage || "No se pudo enviar por WhatsApp.");
      } else {
        setPriceListNotice("Texto de WhatsApp generado. Ya puedes copiarlo.");
      }
      setTimeout(() => setPriceListNotice(null), 3500);
      setPriceListAction(null);
    } catch (err: any) {
      setPriceListError(err?.message || "No se pudo generar la lista.");
      setTimeout(() => setPriceListError(null), 4500);
    } finally {
      setPriceListLoading(false);
    }
  };

  const handleCopyWhatsapp = async () => {
    if (!whatsappText) return;
    try {
      await navigator.clipboard.writeText(whatsappText);
      setPriceListNotice("Texto de WhatsApp copiado.");
      setTimeout(() => setPriceListNotice(null), 2000);
    } catch {
      setPriceListError("No se pudo copiar el texto.");
    }
  };

  const openPriceListModal = (action: PriceListAction) => {
    setPriceListError(null);
    setPriceListNotice(null);
    setPriceListAction(action);
  };

  const closePriceListModal = () => {
    if (priceListLoading) return;
    setPriceListAction(null);
  };

  const buildPriceListQuery = (format: "pdf" | "xlsx" | "text") => {
    const params = new URLSearchParams({
      format,
      includeEcFinalPrice: String(priceListSelection.includeEcFinalPrice),
      includeEcDistributorPrice: String(priceListSelection.includeEcDistributorPrice),
      includePeFinalPrice: String(priceListSelection.includePeFinalPrice),
      includePeDistributorPrice: String(priceListSelection.includePeDistributorPrice),
      includeStock: String(priceListSelection.includeStock),
    });
    return `/api/backend/api/v1/reports/price-list?${params.toString()}`;
  };

  const handleConfirmPriceListAction = async () => {
    if (!priceListAction) return;
    if (priceListAction === "download-pdf") {
      window.open(buildPriceListQuery("pdf"), "_blank", "noopener,noreferrer");
      setPriceListAction(null);
      return;
    }
    if (priceListAction === "download-xlsx") {
      window.open(buildPriceListQuery("xlsx"), "_blank", "noopener,noreferrer");
      setPriceListAction(null);
      return;
    }
    if (priceListAction === "download-text") {
      window.open(buildPriceListQuery("text"), "_blank", "noopener,noreferrer");
      setPriceListAction(null);
      return;
    }
    await handlePriceListAction(priceListAction);
  };

  const filteredProducts = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return products;
    return products.filter((product) => {
      return (
        product.name.toLowerCase().includes(term) ||
        (product.platform || "").toLowerCase().includes(term)
      );
    });
  }, [products, search]);

  const stockByProduct = useMemo(() => {
    return stock.reduce<Record<string, StockSummary>>((acc, item) => {
      acc[item.softwareId] = item;
      return acc;
    }, {});
  }, [stock]);

  const shortId = (id: string) => id.replace(/-/g, "").slice(0, 5).toUpperCase();
  const getEcFinalPrice = (product: Product) => product.ecFinalPrice ?? product.price ?? 0;
  const ecFinalPriceValue = Number(form.ecFinalPrice);
  const ecDistributorPriceValue = Number(form.ecDistributorPrice);
  const canSave =
    form.name.trim().length > 0 &&
    [ecFinalPriceValue, ecDistributorPriceValue].every(
      (value) => !Number.isNaN(value) && value > 0
    );
  const missingPriceFields = [
    { label: "EC Final", value: ecFinalPriceValue },
    { label: "EC Distribuidor", value: ecDistributorPriceValue },
  ].filter((item) => Number.isNaN(item.value) || item.value <= 0);
  const emailReady = sendForm.toEmail.trim().length > 0 && sendForm.subject.trim().length > 0;
  const whatsappReady = sendForm.whatsappPhone.trim().length > 0;
  const selectionAllChecked = Object.values(priceListSelection).every(Boolean);
  const selectedColumnsCount = Object.values(priceListSelection).filter(Boolean).length;
  const priceListActionMeta: Record<PriceListAction, { title: string; description: string; confirmLabel: string }> = {
    "download-pdf": {
      title: "Descargar PDF",
      description: "Selecciona que precios y si deseas incluir stock en el PDF.",
      confirmLabel: "Descargar PDF",
    },
    "download-xlsx": {
      title: "Descargar Excel",
      description: "Selecciona que precios y si deseas incluir stock en el Excel.",
      confirmLabel: "Descargar Excel",
    },
    "download-text": {
      title: "Descargar texto",
      description: "Selecciona que precios y si deseas incluir stock en el texto.",
      confirmLabel: "Descargar texto",
    },
    email: {
      title: "Enviar lista por email",
      description: "Selecciona que informacion saldra en la lista antes de enviarla al cliente.",
      confirmLabel: "Enviar email",
    },
    "generate-whatsapp": {
      title: "Generar texto WhatsApp",
      description: "Selecciona que precios y si deseas incluir stock en el mensaje de WhatsApp.",
      confirmLabel: "Generar texto",
    },
    "send-whatsapp": {
      title: "Enviar por WhatsApp",
      description: "La lista se enviara por la API de DismalCRM usando la seleccion que marques.",
      confirmLabel: "Enviar WhatsApp",
    },
  };
  const activeActionMeta = priceListAction ? priceListActionMeta[priceListAction] : null;
  const floatingPriceListMessage = priceListNotice || priceListError;
  return (
    <div className="min-h-screen bg-paper text-slate">
      {floatingPriceListMessage && (
        <div
          className={`fixed right-6 top-6 z-[60] max-w-sm rounded-2xl border px-4 py-3 text-sm shadow-panel ${
            priceListError
              ? "border-rose-200 bg-rose-50 text-rose-700"
              : "border-emerald-200 bg-emerald-50 text-emerald-700"
          }`}
        >
          {floatingPriceListMessage}
        </div>
      )}
      <div className="flex">
        <main className="flex-1">
          <AdminHeader
            title="Productos"
            subtitle="Inventario"
            onLogout={handleLogout}
            userEmail={user?.email ?? ""}
          />

          <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-6 pb-16 pt-4">
            <div className="glass-panel hero-band rounded-[32px] px-6 py-6 fade-up">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-slate-400">Inventario</p>
                <h2 className="text-2xl font-semibold text-ink">Catalogo de productos</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-500">
                  Gestiona productos físicos y digitales, precios en USD e inventario para Ecuador.
                </p>
                {isCreating && (
                  <div className="mt-4 max-w-2xl rounded-2xl border border-emerald-300/50 bg-white/85 px-4 py-3 text-sm text-emerald-800 shadow-sm">
                    Estás creando un producto nuevo para Ecuador. Define su tipo, inventario y precios en USD.
                  </div>
                )}
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <AdminActionButton icon={<RefreshIcon />} variant="ghost" onClick={handleRefresh}>
                  Actualizar
                </AdminActionButton>
                <AdminActionButton icon={<PlusIcon />} variant="primary" onClick={handleNew}>
                  Nuevo producto
                </AdminActionButton>
              </div>
            </div>

            <div className="glass-panel grid gap-4 p-5 lg:grid-cols-[1.2fr_1fr]">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-slate-400">Lista de precios</p>
                <h3 className="text-lg font-semibold text-ink">Envio rapido</h3>
                <p className="mt-2 text-sm text-slate-500">
                  Genera PDF, Excel y texto para WhatsApp con precios de Ecuador y stock disponible.
                </p>
                {priceListError && (
                  <p className="mt-3 rounded-xl border border-rose-400/30 bg-rose-50 px-3 py-2 text-xs text-rose-700">
                    {priceListError}
                  </p>
                )}
                {priceListNotice && (
                  <p className="mt-3 rounded-xl border border-emerald-300/40 bg-emerald-50 px-3 py-2 text-xs text-emerald-700">
                    {priceListNotice}
                  </p>
                )}
                <div className="mt-4 flex flex-wrap gap-2">
                  <button
                    type="button"
                    className="btn btn-secondary text-xs"
                    onClick={() => openPriceListModal("download-pdf")}
                  >
                    Descargar PDF
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary text-xs"
                    onClick={() => openPriceListModal("download-xlsx")}
                  >
                    Descargar Excel
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary text-xs"
                    onClick={() => openPriceListModal("download-text")}
                  >
                    Descargar texto
                  </button>
                </div>
                <div className="mt-3 flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    className="btn btn-primary text-xs"
                    onClick={() => openPriceListModal("email")}
                    disabled={priceListLoading || !emailReady}
                  >
                    Enviar lista por email
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary text-xs"
                    onClick={() => openPriceListModal("generate-whatsapp")}
                    disabled={priceListLoading || !whatsappReady}
                  >
                    Generar WhatsApp
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary text-xs"
                    onClick={handleCopyWhatsapp}
                    disabled={!whatsappText}
                  >
                    Copiar texto WhatsApp
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary text-xs"
                    onClick={() => openPriceListModal("send-whatsapp")}
                    disabled={priceListLoading || !whatsappReady}
                  >
                    Enviar por WhatsApp
                  </button>
                </div>
              </div>
              <div className="grid gap-3">
                <div className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3 text-xs text-slate-600">
                  Los botones de email y WhatsApp se habilitan cuando completas los datos del cliente a la derecha.
                </div>
                <div className="text-sm font-semibold text-ink">Datos del cliente / envio</div>
                <div ref={customerSearchRef} className="relative">
                  <label className="label-pill">Nombre del cliente</label>
                  <input
                    className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                    placeholder="Busca por nombre, email o WhatsApp"
                    value={customerSearchTerm}
                    onFocus={() => setIsCustomerMenuOpen(true)}
                    onChange={(event) => {
                      const value = event.target.value;
                      setCustomerSearchTerm(value);
                      setSendForm((prev) => ({ ...prev, clientName: value }));
                      setIsCustomerMenuOpen(true);
                    }}
                  />
                  {isCustomerMenuOpen && customerOptions.length > 0 && (
                    <div className="absolute left-0 right-0 top-full z-20 mt-2 max-h-64 overflow-y-auto rounded-xl border border-slate-200 bg-white shadow-panel">
                      {customerOptions.map((customer) => {
                        const fullName = `${customer.firstname || ""} ${customer.lastname || ""}`.trim();
                        return (
                          <button
                            key={customer.id}
                            type="button"
                            className="w-full border-b border-slate-100 px-3 py-2 text-left text-sm last:border-b-0 hover:bg-slate-50"
                            onClick={() => selectCustomerForSend(customer)}
                          >
                            <span className="block font-medium text-ink">{fullName || customer.email}</span>
                            <span className="block text-xs text-slate-500">
                              {customer.email || "-"} {customer.phone ? `- ${customer.phone}` : ""}
                            </span>
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
                <div>
                  <label className="label-pill">Email destino</label>
                  <input
                    className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                    placeholder="cliente@correo.com"
                    value={sendForm.toEmail}
                    onChange={(event) => setSendForm((prev) => ({ ...prev, toEmail: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="label-pill">Asunto</label>
                  <input
                    className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                    value={sendForm.subject}
                    onChange={(event) => setSendForm((prev) => ({ ...prev, subject: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="label-pill">WhatsApp (opcional)</label>
                  <input
                    className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                    placeholder="593999999999"
                    value={sendForm.whatsappPhone}
                    onChange={(event) => setSendForm((prev) => ({ ...prev, whatsappPhone: event.target.value }))}
                  />
                  <p className="mt-2 text-xs text-slate-500">Incluye codigo pais sin +.</p>
                </div>
                <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600">
                  El email se envia con PDF y Excel adjuntos. WhatsApp se envia como texto.
                </div>
              </div>
            </div>

            {error && (
              <div className="glass-card border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-700">
                {error}
              </div>
            )}
            {notice && (
              <div className="glass-card border border-emerald-400/40 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-700">
                {notice}
              </div>
            )}

            <div className={`grid gap-6 ${isCreating ? "xl:grid-cols-1" : "lg:grid-cols-[1.6fr_1.4fr]"}`}>
              {!isCreating && (
              <div className="glass-panel p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-semibold text-ink">Productos</h3>
                    <p className="text-xs text-slate-500">{products.length} registros</p>
                  </div>
                  <input
                    className="w-full max-w-[220px] rounded-full border border-slate-200 px-4 py-2 text-sm text-ink shadow-sm focus:border-accent focus:outline-none"
                    placeholder="Buscar por nombre"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                  />
                </div>
                <div className="mt-3 overflow-hidden rounded-t-2xl border border-slate-200">
                  {filteredProducts.length === 0 && (
                    <p className="px-3 py-4 text-sm text-slate-500">No hay productos registrados.</p>
                  )}
                  {filteredProducts.length > 0 && (
                    <div className="grid items-center gap-2 bg-slate-50 px-2 py-2 text-[11px] uppercase tracking-[0.18em] text-slate-500 md:grid-cols-[0.5fr_1.4fr_0.8fr_0.8fr_0.6fr]">
                      <span>ID</span>
                      <span>Producto</span>
                      <span>Tipo</span>
                      <span className="md:text-right">Precio USD</span>
                      <span className="md:text-right">Stock</span>
                    </div>
                  )}
                  {filteredProducts.map((product) => (
                    <button
                      key={product.id}
                      className={`w-full border-b border-slate-200 px-2 py-2 text-left transition last:border-b-0 ${
                        selected?.id === product.id ? "bg-slate-100/70" : "bg-white"
                      }`}
                      onClick={() => setSelected(product)}
                    >
                      <div className="grid items-center gap-2 md:grid-cols-[0.5fr_1.4fr_0.8fr_0.8fr_0.6fr]">
                        <p className="text-xs font-semibold text-slate-500">{shortId(product.id)}</p>
                        <p className="text-sm font-semibold text-ink">{product.name}</p>
                        <p className="text-xs font-semibold text-slate-500">{product.physicalProduct ? "Físico" : "Digital"}</p>
                        <p className="text-sm text-slate-600 md:text-right">{formatCurrency(getEcFinalPrice(product))}</p>
                        <p className="text-sm text-slate-600 md:text-right">
                          {product.physicalProduct
                            ? Math.max(0, (product.stockQuantity ?? 0) - (product.reservedQuantity ?? 0))
                            : stockByProduct[product.id]?.available ?? 0}
                        </p>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
              )}

              <div
                ref={formPanelRef}
                className={`glass-panel p-4 ${isCreating ? "order-1 border border-emerald-300/50 bg-emerald-50/50 shadow-[0_18px_48px_rgba(16,185,129,0.12)]" : ""}`}
              >
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-lg font-semibold text-ink">
                      {isCreating ? "Nuevo producto" : "Detalle de producto"}
                    </h3>
                    <p className="text-xs text-slate-500">
                      {isCreating ? "Completa la informacion para guardar." : "Edita los datos del catalogo."}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {isCreating && (
                      <span className="rounded-full border border-emerald-300 bg-white px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-700">
                        Ecuador · USD
                      </span>
                    )}
                    {!isCreating && (
                      <AdminActionButton variant="ghost" onClick={handleNew}>
                        Nuevo
                      </AdminActionButton>
                    )}
                  </div>
                </div>

                <div className="mt-3 space-y-3">
                  {!isCreating && selected && (
                    <div>
                      <label className="label-pill">ID corto</label>
                      <input
                        className="mt-2 w-full rounded-lg border border-slate-200 bg-slate-100 px-3 py-2 text-sm text-slate-500"
                        value={shortId(selected.id)}
                        readOnly
                      />
                      <p className="mt-1 text-xs text-slate-500">
                        Identificador unico generado por el sistema para busquedas futuras.
                      </p>
                    </div>
                  )}
                  {isCreating && (
                    <div className="rounded-2xl border border-emerald-200 bg-white px-4 py-4">
                      <p className="text-sm font-semibold text-ink">Antes de guardar</p>
                      <p className="mt-1 text-xs text-slate-500">
                        Debes indicar si es un producto físico o digital y completar los dos precios para Ecuador.
                      </p>
                      {missingPriceFields.length > 0 ? (
                        <p className="mt-3 text-xs text-amber-700">
                          Faltan: {missingPriceFields.map((field) => field.label).join(", ")}.
                        </p>
                      ) : (
                        <p className="mt-3 text-xs text-emerald-700">
                          Todo listo. Ya puedes crear el producto.
                        </p>
                      )}
                    </div>
                  )}
                  <div className={`grid gap-3 ${isCreating ? "lg:grid-cols-3" : "md:grid-cols-2"}`}>
                    <div>
                      <label className="label-pill">Nombre</label>
                      <input
                        className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                        placeholder="Ej: Office 365, Canva Pro, Windows 11"
                        value={form.name}
                        onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
                      />
                    </div>
                    <div>
                      <label className="label-pill">Plataforma</label>
                      <input
                        className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                        placeholder="Windows, Web, Android, macOS"
                        value={form.platform}
                        onChange={(event) => setForm((prev) => ({ ...prev, platform: event.target.value }))}
                      />
                      <p className="mt-2 text-xs text-slate-500">
                        Ej: Windows, Web, iOS, Android o producto físico.
                      </p>
                    </div>
                    <div>
                      <label className="label-pill">Imagen</label>
                      <input
                        className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                        placeholder="https://..."
                        value={form.imageUrl}
                        onChange={(event) => setForm((prev) => ({ ...prev, imageUrl: event.target.value }))}
                      />
                    </div>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-white p-4">
                    <p className="text-sm font-semibold text-ink">Tipo e inventario</p>
                    <div className="mt-3 grid gap-3 md:grid-cols-2 lg:grid-cols-4">
                      <label className={`cursor-pointer rounded-xl border p-3 ${form.physicalProduct ? "border-accent bg-accent/5" : "border-slate-200"}`}>
                        <input type="radio" className="mr-2" checked={form.physicalProduct} onChange={() => setForm((prev) => ({ ...prev, physicalProduct: true }))} />
                        Producto físico
                      </label>
                      <label className={`cursor-pointer rounded-xl border p-3 ${!form.physicalProduct ? "border-accent bg-accent/5" : "border-slate-200"}`}>
                        <input type="radio" className="mr-2" checked={!form.physicalProduct} onChange={() => setForm((prev) => ({ ...prev, physicalProduct: false, stockQuantity: "0" }))} />
                        Licencia o producto digital
                      </label>
                      <div><label className="label-pill">SKU</label><input className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" value={form.sku} onChange={(event) => setForm((prev) => ({ ...prev, sku: event.target.value }))} /></div>
                      <div><label className="label-pill">Marca</label><input className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" value={form.brand} onChange={(event) => setForm((prev) => ({ ...prev, brand: event.target.value }))} /></div>
                      <div><label className="label-pill">Código de barras</label><input className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" value={form.barcode} onChange={(event) => setForm((prev) => ({ ...prev, barcode: event.target.value }))} /></div>
                      {form.physicalProduct ? <div><label className="label-pill">Existencias</label><input type="number" min={0} step={1} className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" value={form.stockQuantity} onChange={(event) => setForm((prev) => ({ ...prev, stockQuantity: event.target.value }))} /></div> : null}
                    </div>
                    <p className="mt-3 text-xs text-slate-500">Los productos físicos descuentan existencias; los digitales consumen activaciones de licencias.</p>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <p className="text-sm font-semibold text-ink">Precios para Ecuador</p>
                        <p className="text-xs text-slate-500">
                          Configura precios en dólares para cliente final y distribuidor.
                        </p>
                      </div>
                    </div>
                    <div className="mt-4 grid gap-3 md:grid-cols-2">
                      <div className="rounded-xl border border-slate-200 bg-white p-3">
                        <label className="label-pill">EC Final</label>
                        <input
                          type="number"
                          min={0}
                          step="0.01"
                          inputMode="decimal"
                          placeholder="0.00"
                          className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-ink focus:border-accent focus:outline-none"
                          value={form.ecFinalPrice}
                          onChange={(event) => setForm((prev) => ({ ...prev, ecFinalPrice: event.target.value }))}
                        />
                      </div>
                      <div className="rounded-xl border border-slate-200 bg-white p-3">
                        <label className="label-pill">EC Distribuidor</label>
                        <input
                          type="number"
                          min={0}
                          step="0.01"
                          inputMode="decimal"
                          placeholder="0.00"
                          className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-ink focus:border-accent focus:outline-none"
                          value={form.ecDistributorPrice}
                          onChange={(event) => setForm((prev) => ({ ...prev, ecDistributorPrice: event.target.value }))}
                        />
                      </div>
                    </div>
                  </div>
                  <div>
                    <label className="label-pill">Descripcion</label>
                    <textarea
                      className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                      rows={3}
                      value={form.description}
                      onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
                    />
                  </div>
                  <div className="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3">
                    <AdminActionButton
                      variant="primary"
                      onClick={isCreating ? handleCreate : handleUpdate}
                      disabled={!canSave}
                    >
                      {isCreating ? "Crear producto" : "Guardar cambios"}
                    </AdminActionButton>
                    {isCreating && (
                      <AdminActionButton variant="ghost" onClick={handleCancelCreate}>
                        Cancelar
                      </AdminActionButton>
                    )}
                    {!canSave && (
                      <p className="text-xs text-slate-500">
                        Completa el nombre y los dos precios de Ecuador para habilitar el guardado.
                      </p>
                    )}
                    {!isCreating && (
                      <AdminActionButton variant="ghost" onClick={handleDelete}>
                        Eliminar
                      </AdminActionButton>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>
      {priceListAction && activeActionMeta && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 px-4 py-8">
          <div className="w-full max-w-2xl rounded-[28px] border border-slate-200 bg-white shadow-2xl">
            <div className="border-b border-slate-200 px-6 py-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Lista de precios</p>
                  <h3 className="mt-1 text-xl font-semibold text-ink">{activeActionMeta.title}</h3>
                  <p className="mt-2 text-sm text-slate-500">{activeActionMeta.description}</p>
                </div>
                <button
                  type="button"
                  className="rounded-full border border-slate-200 px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500"
                  onClick={closePriceListModal}
                  disabled={priceListLoading}
                >
                  Cerrar
                </button>
              </div>
            </div>
            <div className="grid gap-6 px-6 py-6 md:grid-cols-[1.1fr_0.9fr]">
              <div>
                <div className="flex items-center justify-between">
                  <p className="text-sm font-semibold text-ink">Seleccion de columnas</p>
                  <label className="flex items-center gap-2 text-xs text-slate-500">
                    <input
                      type="checkbox"
                      checked={selectionAllChecked}
                      onChange={(event) =>
                        setPriceListSelection({
                          includeEcFinalPrice: event.target.checked,
                          includeEcDistributorPrice: event.target.checked,
                          includePeFinalPrice: false,
                          includePeDistributorPrice: false,
                          includeStock: event.target.checked,
                        })
                      }
                    />
                    Todos
                  </label>
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  {([
                    ["includeEcFinalPrice", "Precio 1 - EC Final"],
                    ["includeEcDistributorPrice", "Precio 2 - EC Distribuidor"],
                    ["includeStock", "Stock disponible"],
                  ] as Array<[keyof PriceListSelection, string]>).map(([key, label]) => (
                    <label
                      key={key}
                      className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700"
                    >
                      <input
                        type="checkbox"
                        checked={priceListSelection[key as keyof PriceListSelection]}
                        onChange={(event) =>
                          setPriceListSelection((prev) => ({
                            ...prev,
                            [key]: event.target.checked,
                          }))
                        }
                      />
                      {label}
                    </label>
                  ))}
                </div>
                <p className="mt-3 text-xs text-slate-500">
                  Seleccionados: {selectedColumnsCount} campos.
                </p>
              </div>
              <div className="rounded-3xl border border-slate-200 bg-slate-50/80 px-4 py-4">
                <p className="text-sm font-semibold text-ink">Destino actual</p>
                <div className="mt-4 space-y-3 text-sm text-slate-600">
                  <div>
                    <p className="text-[11px] uppercase tracking-[0.18em] text-slate-400">Cliente</p>
                    <p className="mt-1 font-medium text-ink">{sendForm.clientName.trim() || "No definido"}</p>
                  </div>
                  <div>
                    <p className="text-[11px] uppercase tracking-[0.18em] text-slate-400">Email</p>
                    <p className="mt-1 font-medium text-ink">{sendForm.toEmail.trim() || "No definido"}</p>
                  </div>
                  <div>
                    <p className="text-[11px] uppercase tracking-[0.18em] text-slate-400">Asunto</p>
                    <p className="mt-1 font-medium text-ink">{sendForm.subject.trim() || "No definido"}</p>
                  </div>
                  <div>
                    <p className="text-[11px] uppercase tracking-[0.18em] text-slate-400">WhatsApp</p>
                    <p className="mt-1 font-medium text-ink">{sendForm.whatsappPhone.trim() || "No definido"}</p>
                  </div>
                  {(priceListAction === "email" || priceListAction === "generate-whatsapp" || priceListAction === "send-whatsapp") && (
                    <p className="rounded-2xl border border-slate-200 bg-white px-3 py-3 text-xs text-slate-500">
                      {priceListAction === "email"
                        ? "El email solo se enviara si el correo y el asunto estan completos."
                        : "WhatsApp requiere numero con codigo pais para generar o enviar la lista."}
                    </p>
                  )}
                </div>
              </div>
            </div>
            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 px-6 py-4">
              <p className="text-xs text-slate-500">
                Debes seleccionar al menos un precio para continuar.
              </p>
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  className="rounded-full border border-slate-200 px-4 py-2 text-xs font-semibold uppercase tracking-[0.18em] text-slate-600"
                  onClick={closePriceListModal}
                  disabled={priceListLoading}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="rounded-full bg-ink px-4 py-2 text-xs font-semibold uppercase tracking-[0.18em] text-white disabled:opacity-60"
                  onClick={handleConfirmPriceListAction}
                  disabled={
                    priceListLoading
                    || selectedColumnsCount === 0
                    || (priceListAction === "email" && !emailReady)
                    || ((priceListAction === "generate-whatsapp" || priceListAction === "send-whatsapp") && !whatsappReady)
                  }
                >
                  {priceListLoading ? "Procesando..." : activeActionMeta.confirmLabel}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
