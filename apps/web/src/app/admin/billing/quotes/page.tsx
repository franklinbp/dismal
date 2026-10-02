"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { PlusIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

const quoteStatuses = ["DRAFT", "SENT", "APPROVED", "EXPIRED", "CANCELLED"] as const;

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type Customer = {
  id: string;
  firstname: string;
  lastname: string;
  email: string;
  customerType?: "FINAL" | "DISTRIBUTOR";
};

type Software = {
  id: string;
  name: string;
  price: number;
  ecFinalPrice?: number | null;
  ecDistributorPrice?: number | null;
  peFinalPrice?: number | null;
  peDistributorPrice?: number | null;
};

type QuoteItem = {
  id: string;
  softwareId: string;
  softwareName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
};

type QuoteSummary = {
  id: string;
  clientId: string;
  clientName: string;
  clientEmail: string;
  status: string;
  total: number;
  createdAt: string;
  updatedAt: string;
};

type QuoteDetail = QuoteSummary & {
  notes: string | null;
  saleId: string | null;
  items: QuoteItem[];
};

type CreateItem = {
  softwareId: string;
  quantity: number;
  unitPrice: number;
};

const defaultPage: PageResponse<unknown> = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

function resolveSegmentUnitPrice(
  product: Software | undefined,
  marketCode: "EC" | "PE",
  customerType: "FINAL" | "DISTRIBUTOR"
) {
  if (!product) return 0;
  if (marketCode === "EC" && customerType === "FINAL") {
    return Number(product.ecFinalPrice ?? product.price ?? 0);
  }
  if (marketCode === "EC" && customerType === "DISTRIBUTOR") {
    return Number(product.ecDistributorPrice ?? product.ecFinalPrice ?? product.price ?? 0);
  }
  if (marketCode === "PE" && customerType === "FINAL") {
    return Number(product.peFinalPrice ?? product.ecFinalPrice ?? product.price ?? 0);
  }
  return Number(
    product.peDistributorPrice
    ?? product.peFinalPrice
    ?? product.ecDistributorPrice
    ?? product.ecFinalPrice
    ?? product.price
    ?? 0
  );
}

export default function AdminQuotesPage() {
  const router = useRouter();
  const { user, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [quotes, setQuotes] = useState<PageResponse<QuoteSummary>>(defaultPage as PageResponse<QuoteSummary>);
  const [selectedQuote, setSelectedQuote] = useState<QuoteDetail | null>(null);

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Software[]>([]);

  const [filters, setFilters] = useState({
    status: "",
  });

  const [newQuote, setNewQuote] = useState({
    clientId: "",
    marketCode: "EC" as "EC" | "PE",
    notes: "",
    items: [{ softwareId: "", quantity: 1, unitPrice: 0 }] as CreateItem[],
  });

  const totalPreview = useMemo(() => {
    return newQuote.items.reduce((acc, item) => acc + item.quantity * item.unitPrice, 0);
  }, [newQuote.items]);
  const selectedCustomer = useMemo(
    () => customers.find((customer) => customer.id === newQuote.clientId) ?? null,
    [customers, newQuote.clientId]
  );
  const selectedCustomerType = selectedCustomer?.customerType ?? "FINAL";

  const loadQuotes = useCallback(async () => {
    const params = new URLSearchParams();
    if (filters.status) params.set("status", filters.status);
    params.set("page", "0");
    params.set("size", "25");
    const data = await apiFetch<PageResponse<QuoteSummary>>(`/api/backend/api/v1/quotes?${params.toString()}`);
    setQuotes(data);
    if (!selectedQuote && data.content.length > 0) {
      const detail = await apiFetch<QuoteDetail>(`/api/backend/api/v1/quotes/${data.content[0].id}`);
      setSelectedQuote(detail);
    }
  }, [filters, selectedQuote]);

  const loadAux = useCallback(async () => {
    const [customerData, productData] = await Promise.all([
      apiFetch<Customer[]>("/api/backend/api/v1/customers"),
      apiFetch<Software[]>("/api/backend/api/v1/store/products"),
    ]);
    setCustomers(customerData);
    setProducts(productData);
  }, []);

  useEffect(() => {
    const boot = async () => {
      if (!allowed || !user) return;
      setError(null);
      try {
        await Promise.all([loadQuotes(), loadAux()]);
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar cotizaciones.");
      }
    };
    boot();
  }, [allowed, user, loadAux, loadQuotes, router]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const updateItem = (index: number, patch: Partial<CreateItem>) => {
    setNewQuote((prev) => {
      const items = prev.items.map((item, idx) => (idx === index ? { ...item, ...patch } : item));
      return { ...prev, items };
    });
  };

  const applySegmentPricesToItems = useCallback(
    (
      items: CreateItem[],
      marketCode: "EC" | "PE",
      customerType: "FINAL" | "DISTRIBUTOR"
    ) =>
      items.map((item) => {
        const product = products.find((candidate) => candidate.id === item.softwareId);
        if (!product) {
          return item;
        }
        return {
          ...item,
          unitPrice: resolveSegmentUnitPrice(product, marketCode, customerType),
        };
      }),
    [products]
  );

  const addItem = () => {
    setNewQuote((prev) => ({
      ...prev,
      items: [...prev.items, { softwareId: "", quantity: 1, unitPrice: 0 }],
    }));
  };

  const removeItem = (index: number) => {
    setNewQuote((prev) => ({
      ...prev,
      items: prev.items.filter((_, idx) => idx !== index),
    }));
  };

  const handleCreateQuote = async () => {
    setError(null);
    try {
      const payload = {
        clientId: newQuote.clientId,
        notes: newQuote.notes || null,
        items: newQuote.items
          .filter((item) => item.softwareId && item.quantity > 0)
          .map((item) => ({
            softwareId: item.softwareId,
            quantity: item.quantity,
            unitPrice: item.unitPrice,
          })),
      };
      const created = await apiFetch<QuoteDetail>("/api/backend/api/v1/quotes", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setNotice("Cotizacion creada.");
      setSelectedQuote(created);
      setNewQuote({
        clientId: "",
        marketCode: "EC",
        notes: "",
        items: [{ softwareId: "", quantity: 1, unitPrice: 0 }],
      });
      await loadQuotes();
      setTimeout(() => setNotice(null), 2200);
    } catch (err: any) {
      setError(err?.message || "No se pudo crear cotizacion.");
    }
  };

  const handleSelectQuote = async (quoteId: string) => {
    setError(null);
    try {
      const detail = await apiFetch<QuoteDetail>(`/api/backend/api/v1/quotes/${quoteId}`);
      setSelectedQuote(detail);
    } catch (err: any) {
      setError(err?.message || "No se pudo cargar el detalle.");
    }
  };

  const handleConvert = async () => {
    if (!selectedQuote) return;
    setError(null);
    try {
      await apiFetch(`/api/backend/api/v1/quotes/${selectedQuote.id}/convert-to-sale`, {
        method: "POST",
      });
      const refreshed = await apiFetch<QuoteDetail>(`/api/backend/api/v1/quotes/${selectedQuote.id}`);
      setSelectedQuote(refreshed);
      setNotice("Cotizacion convertida en venta.");
      setTimeout(() => setNotice(null), 2200);
    } catch (err: any) {
      setError(err?.message || "No se pudo convertir la cotizacion.");
    }
  };

  const handleRefresh = async () => {
    setError(null);
    try {
      await loadQuotes();
    } catch (err: any) {
      setError(err?.message || "No se pudo recargar.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="flex">
        <main className="flex-1">
          <AdminHeader
            title="Cotizaciones"
            subtitle="Facturacion"
            onLogout={handleLogout}
            userEmail={user?.email ?? ""}
          />

          <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-6 pb-16 pt-4">
            <div className="hero-band">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-cyan-200/70">Facturacion</p>
                <h2 className="text-2xl font-semibold text-white">Cotizaciones profesionales</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-200/80">
                  Crea propuestas, confirma precios y convierte en ventas en un clic.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <AdminActionButton icon={<RefreshIcon />} variant="ghost" onClick={handleRefresh}>
                  Actualizar
                </AdminActionButton>
                <AdminActionButton icon={<PlusIcon />} onClick={handleCreateQuote}>
                  Crear cotizacion
                </AdminActionButton>
              </div>
            </div>

            {error && (
              <div className="glass-card border border-rose-500/40 bg-rose-500/10 px-4 py-3 text-sm text-rose-100">
                {error}
              </div>
            )}
            {notice && (
              <div className="glass-card border border-emerald-400/40 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-100">
                {notice}
              </div>
            )}

            <div className="grid gap-6 lg:grid-cols-[1.2fr_1.8fr]">
              <div className="glass-panel p-5">
                <div className="flex flex-wrap items-center gap-3">
                  <div className="flex flex-1 items-center gap-2">
                    <label className="label-pill">Estado</label>
                    <select
                      className="min-w-[160px] rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                      value={filters.status}
                      onChange={(event) => setFilters({ status: event.target.value })}
                    >
                      <option value="">Todos</option>
                      {quoteStatuses.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>
                  <AdminActionButton variant="ghost" onClick={handleRefresh}>
                    Aplicar
                  </AdminActionButton>
                </div>

                <div className="mt-4 space-y-3">
                  {quotes.content.length === 0 && (
                    <p className="text-sm text-slate-400">No hay cotizaciones registradas.</p>
                  )}
                  {quotes.content.map((quote) => (
                    <button
                      key={quote.id}
                      className={`glass-card w-full px-4 py-3 text-left transition ${
                        selectedQuote?.id === quote.id ? "border-cyan-400/60" : "border-white/10"
                      }`}
                      onClick={() => handleSelectQuote(quote.id)}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm font-semibold text-slate-100">{quote.clientName}</p>
                          <p className="text-xs text-slate-400">{quote.clientEmail}</p>
                        </div>
                        <span className="status-pill text-xs">{quote.status}</span>
                      </div>
                      <div className="mt-2 flex items-center justify-between text-xs text-slate-400">
                        <span>{formatDateTime(quote.createdAt)}</span>
                        <span className="text-slate-200">{formatCurrency(quote.total)}</span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>

              <div className="glass-panel p-5">
                <h3 className="text-lg font-semibold text-white">Detalle de cotizacion</h3>
                {!selectedQuote ? (
                  <p className="mt-4 text-sm text-slate-400">Selecciona una cotizacion para ver el detalle.</p>
                ) : (
                  <div className="mt-4 space-y-4">
                    <div className="grid gap-3 md:grid-cols-2">
                      <div className="glass-card px-4 py-3">
                        <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Cliente</p>
                        <p className="text-sm font-semibold text-slate-100">{selectedQuote.clientName}</p>
                        <p className="text-xs text-slate-400">{selectedQuote.clientEmail}</p>
                      </div>
                      <div className="glass-card px-4 py-3">
                        <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Estado</p>
                        <p className="text-sm text-slate-100">{selectedQuote.status}</p>
                        <p className="text-xs text-slate-400">
                          {formatDateTime(selectedQuote.updatedAt)}
                        </p>
                      </div>
                    </div>

                    <div className="glass-card px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Notas</p>
                      <p className="text-sm text-slate-100">{selectedQuote.notes || "Sin notas"}</p>
                    </div>

                    <div className="glass-card px-4 py-3">
                      <div className="flex items-center justify-between">
                        <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Items</p>
                        <span className="text-sm text-slate-200">
                          Total {formatCurrency(selectedQuote.total)}
                        </span>
                      </div>
                      <div className="mt-3 space-y-2">
                        {selectedQuote.items.map((item) => (
                          <div key={item.id} className="flex items-center justify-between text-sm text-slate-200">
                            <div>
                              <p className="font-medium">{item.softwareName}</p>
                              <p className="text-xs text-slate-400">
                                {item.quantity} x {formatCurrency(item.unitPrice)}
                              </p>
                            </div>
                            <span className="text-sm">{formatCurrency(item.subtotal)}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-3">
                      <AdminActionButton
                        onClick={handleConvert}
                        disabled={!!selectedQuote.saleId}
                      >
                        {selectedQuote.saleId ? "Venta creada" : "Convertir en venta"}
                      </AdminActionButton>
                      {selectedQuote.saleId && (
                        <p className="text-xs text-slate-400">Sale ID: {selectedQuote.saleId}</p>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="glass-panel p-5">
              <h3 className="text-lg font-semibold text-white">Nueva cotizacion</h3>
              <div className="mt-4 grid gap-4 md:grid-cols-2">
                <div>
                  <label className="label-pill">Cliente</label>
                  <select
                    className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                    value={newQuote.clientId}
                    onChange={(event) => {
                      const customer = customers.find((item) => item.id === event.target.value);
                      const nextCustomerType = customer?.customerType ?? "FINAL";
                      setNewQuote((prev) => ({
                        ...prev,
                        clientId: event.target.value,
                        items: applySegmentPricesToItems(prev.items, prev.marketCode, nextCustomerType),
                      }));
                    }}
                  >
                    <option value="">Selecciona un cliente</option>
                    {customers.map((customer) => (
                      <option key={customer.id} value={customer.id}>
                        {customer.firstname} {customer.lastname} - {customer.email}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label-pill">Mercado</label>
                  <select
                    className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                    value={newQuote.marketCode}
                    onChange={(event) => {
                      const nextMarketCode = event.target.value as "EC" | "PE";
                      setNewQuote((prev) => ({
                        ...prev,
                        marketCode: nextMarketCode,
                        items: applySegmentPricesToItems(prev.items, nextMarketCode, selectedCustomerType),
                      }));
                    }}
                  >
                    <option value="EC">Ecuador</option>
                    <option value="PE">Peru</option>
                  </select>
                </div>
                <div>
                  <label className="label-pill">Notas</label>
                  <input
                    className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                    placeholder="Observaciones internas"
                    value={newQuote.notes}
                    onChange={(event) =>
                      setNewQuote((prev) => ({ ...prev, notes: event.target.value }))
                    }
                  />
                </div>
              </div>

              <div className="mt-4 space-y-3">
                <div className="rounded-2xl border border-white/10 bg-slate-900/50 px-4 py-3 text-xs text-slate-300">
                  Tipo de cliente aplicado: {selectedCustomerType === "DISTRIBUTOR" ? "Distribuidor" : "Cliente final"}.
                </div>
                {newQuote.items.map((item, index) => (
                  <div key={`${item.softwareId}-${index}`} className="grid gap-3 md:grid-cols-[2fr_1fr_1fr_auto]">
                    <select
                      className="rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                      value={item.softwareId}
                      onChange={(event) => {
                        const product = products.find((candidate) => candidate.id === event.target.value);
                        updateItem(index, {
                          softwareId: event.target.value,
                          unitPrice: resolveSegmentUnitPrice(
                            product,
                            newQuote.marketCode,
                            selectedCustomerType
                          ),
                        });
                      }}
                    >
                      <option value="">Selecciona software</option>
                      {products.map((product) => (
                        <option key={product.id} value={product.id}>
                          {product.name}
                        </option>
                      ))}
                    </select>
                    <input
                      type="number"
                      className="rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                      min={1}
                      value={item.quantity}
                      onChange={(event) => updateItem(index, { quantity: Number(event.target.value) })}
                    />
                    <input
                      type="number"
                      className="rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                      min={0}
                      value={item.unitPrice}
                      onChange={(event) => updateItem(index, { unitPrice: Number(event.target.value) })}
                    />
                    <button
                      className="rounded-lg border border-white/10 px-3 py-2 text-xs text-slate-300 hover:border-rose-400 hover:text-rose-200"
                      onClick={() => removeItem(index)}
                    >
                      Quitar
                    </button>
                  </div>
                ))}
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <button
                    className="rounded-lg border border-white/10 px-3 py-2 text-xs text-slate-300 hover:border-cyan-300 hover:text-white"
                    onClick={addItem}
                  >
                    Agregar item
                  </button>
                  <span className="text-sm text-slate-200">
                    Total estimado {formatCurrency(totalPreview)}
                  </span>
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
