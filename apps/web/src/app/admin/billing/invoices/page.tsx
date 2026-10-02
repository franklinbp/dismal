"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency, formatDate } from "@/lib/format";
import { PlusIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type InvoiceSummary = {
  id: string;
  invoiceNumber: string;
  status: string;
  issueDate: string;
  dueDate: string;
  totalAmount: number;
  saleId: string | null;
  clientName: string;
  clientEmail: string;
};

type InvoiceDetail = InvoiceSummary & {
  clientId: string;
};

const defaultPage: PageResponse<unknown> = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

export default function AdminInvoicesPage() {
  const router = useRouter();
  const { user, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [invoices, setInvoices] = useState<PageResponse<InvoiceSummary>>(defaultPage as PageResponse<InvoiceSummary>);
  const [selectedInvoice, setSelectedInvoice] = useState<InvoiceDetail | null>(null);

  const [filters, setFilters] = useState({
    q: "",
    from: "",
    to: "",
  });

  const [createForm, setCreateForm] = useState({
    saleId: "",
    dueDate: "",
  });

  const loadInvoices = useCallback(async () => {
    const params = new URLSearchParams();
    if (filters.q) params.set("q", filters.q);
    if (filters.from) params.set("from", filters.from);
    if (filters.to) params.set("to", filters.to);
    params.set("page", "0");
    params.set("size", "25");
    const data = await apiFetch<PageResponse<InvoiceSummary>>(`/api/backend/api/v1/invoices?${params.toString()}`);
    setInvoices(data);
    if (!selectedInvoice && data.content.length > 0) {
      const detail = await apiFetch<InvoiceDetail>(`/api/backend/api/v1/invoices/${data.content[0].id}`);
      setSelectedInvoice(detail);
    }
  }, [filters, selectedInvoice]);

  useEffect(() => {
    const boot = async () => {
      if (!allowed || !user) return;
      setError(null);
      try {
        await loadInvoices();
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar facturas.");
      }
    };
    boot();
  }, [allowed, user, loadInvoices, router]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleCreateInvoice = async () => {
    setError(null);
    try {
      const payload = {
        saleId: createForm.saleId,
        dueDate: createForm.dueDate || null,
      };
      const created = await apiFetch<InvoiceDetail>("/api/backend/api/v1/invoices", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setSelectedInvoice(created);
      setCreateForm({ saleId: "", dueDate: "" });
      await loadInvoices();
      setNotice("Factura creada.");
      setTimeout(() => setNotice(null), 2200);
    } catch (err: any) {
      setError(err?.message || "No se pudo crear factura.");
    }
  };

  const handleSelectInvoice = async (invoiceId: string) => {
    setError(null);
    try {
      const detail = await apiFetch<InvoiceDetail>(`/api/backend/api/v1/invoices/${invoiceId}`);
      setSelectedInvoice(detail);
    } catch (err: any) {
      setError(err?.message || "No se pudo cargar el detalle.");
    }
  };

  const handleVoidInvoice = async () => {
    if (!selectedInvoice) return;
    setError(null);
    try {
      const updated = await apiFetch<InvoiceDetail>(`/api/backend/api/v1/invoices/${selectedInvoice.id}/void`, {
        method: "POST",
      });
      setSelectedInvoice(updated);
      await loadInvoices();
      setNotice("Factura anulada.");
      setTimeout(() => setNotice(null), 2200);
    } catch (err: any) {
      setError(err?.message || "No se pudo anular la factura.");
    }
  };

  const handleRefresh = async () => {
    setError(null);
    try {
      await loadInvoices();
    } catch (err: any) {
      setError(err?.message || "No se pudo recargar.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="flex">
        <main className="flex-1">
          <AdminHeader
            title="Facturas"
            subtitle="Facturacion"
            onLogout={handleLogout}
            userEmail={user?.email ?? ""}
          />

          <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-6 pb-16 pt-4">
            <div className="hero-band">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-cyan-200/70">Facturacion</p>
                <h2 className="text-2xl font-semibold text-white">Control de facturas</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-200/80">
                  Genera facturas desde ventas confirmadas y gestiona estados.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <AdminActionButton icon={<RefreshIcon />} variant="ghost" onClick={handleRefresh}>
                  Actualizar
                </AdminActionButton>
                <AdminActionButton icon={<PlusIcon />} onClick={handleCreateInvoice}>
                  Crear factura
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
                  <input
                    className="flex-1 rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                    placeholder="Buscar por numero o cliente"
                    value={filters.q}
                    onChange={(event) => setFilters((prev) => ({ ...prev, q: event.target.value }))}
                  />
                  <input
                    type="date"
                    className="rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                    value={filters.from}
                    onChange={(event) => setFilters((prev) => ({ ...prev, from: event.target.value }))}
                  />
                  <input
                    type="date"
                    className="rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                    value={filters.to}
                    onChange={(event) => setFilters((prev) => ({ ...prev, to: event.target.value }))}
                  />
                  <AdminActionButton variant="ghost" onClick={handleRefresh}>
                    Aplicar
                  </AdminActionButton>
                </div>

                <div className="mt-4 space-y-3">
                  {invoices.content.length === 0 && (
                    <p className="text-sm text-slate-400">No hay facturas registradas.</p>
                  )}
                  {invoices.content.map((invoice) => (
                    <button
                      key={invoice.id}
                      className={`glass-card w-full px-4 py-3 text-left transition ${
                        selectedInvoice?.id === invoice.id ? "border-cyan-400/60" : "border-white/10"
                      }`}
                      onClick={() => handleSelectInvoice(invoice.id)}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm font-semibold text-slate-100">{invoice.invoiceNumber}</p>
                          <p className="text-xs text-slate-400">{invoice.clientName}</p>
                        </div>
                        <span className="status-pill text-xs">{invoice.status}</span>
                      </div>
                      <div className="mt-2 flex items-center justify-between text-xs text-slate-400">
                        <span>{formatDate(invoice.issueDate)}</span>
                        <span className="text-slate-200">{formatCurrency(invoice.totalAmount)}</span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>

              <div className="glass-panel p-5">
                <h3 className="text-lg font-semibold text-white">Detalle de factura</h3>
                {!selectedInvoice ? (
                  <p className="mt-4 text-sm text-slate-400">Selecciona una factura para ver el detalle.</p>
                ) : (
                  <div className="mt-4 space-y-4">
                    <div className="grid gap-3 md:grid-cols-2">
                      <div className="glass-card px-4 py-3">
                        <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Factura</p>
                        <p className="text-sm font-semibold text-slate-100">{selectedInvoice.invoiceNumber}</p>
                        <p className="text-xs text-slate-400">{selectedInvoice.status}</p>
                      </div>
                      <div className="glass-card px-4 py-3">
                        <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Cliente</p>
                        <p className="text-sm font-semibold text-slate-100">{selectedInvoice.clientName}</p>
                        <p className="text-xs text-slate-400">{selectedInvoice.clientEmail}</p>
                      </div>
                    </div>

                    <div className="grid gap-3 md:grid-cols-3">
                      <div className="glass-card px-4 py-3">
                        <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Emision</p>
                        <p className="text-sm text-slate-100">{formatDate(selectedInvoice.issueDate)}</p>
                      </div>
                      <div className="glass-card px-4 py-3">
                        <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Vence</p>
                        <p className="text-sm text-slate-100">{formatDate(selectedInvoice.dueDate)}</p>
                      </div>
                      <div className="glass-card px-4 py-3">
                        <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Total</p>
                        <p className="text-sm text-slate-100">{formatCurrency(selectedInvoice.totalAmount)}</p>
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-3">
                      <AdminActionButton
                        variant="ghost"
                        onClick={handleVoidInvoice}
                        disabled={selectedInvoice.status === "PAID" || selectedInvoice.status === "CANCELLED"}
                      >
                        Anular factura
                      </AdminActionButton>
                      {selectedInvoice.saleId && (
                        <p className="text-xs text-slate-400">Sale ID: {selectedInvoice.saleId}</p>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="glass-panel p-5">
              <h3 className="text-lg font-semibold text-white">Nueva factura</h3>
              <div className="mt-4 grid gap-4 md:grid-cols-2">
                <div>
                  <label className="label-pill">Sale ID</label>
                  <input
                    className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                    placeholder="UUID de la venta confirmada"
                    value={createForm.saleId}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, saleId: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="label-pill">Vencimiento (opcional)</label>
                  <input
                    type="date"
                    className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                    value={createForm.dueDate}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, dueDate: event.target.value }))}
                  />
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
