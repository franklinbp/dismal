"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import AdminActionButton from "@/components/admin/AdminActionButton";
import AdminHeader from "@/components/admin/AdminHeader";
import { InvoiceIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";
import { apiFetch, type ApiError } from "@/lib/api";
import { formatDateTime } from "@/lib/format";

type OrderStatus = "PENDING_PAYMENT" | "PAYMENT_REVIEW" | "PREPARING" | "READY_FOR_DISPATCH" | "SHIPPED" | "DELIVERED";

type StorefrontOrder = {
  orderId: string;
  orderNumber: string;
  status: string;
  country: "EC" | "PE";
  currency: string;
  customerType: "FINAL" | "DISTRIBUTOR";
  checkoutMethod: "BANK_TRANSFER" | "CUSTOMER_CREDIT";
  customerId: string;
  email: string;
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
  shippingCarrier?: string;
  trackingNumber?: string;
  total: number;
  paymentAccountId?: string;
  paymentClaimBank?: string;
  paymentClaimReference?: string;
  paymentClaimPayer?: string;
  paymentClaimAmount?: number;
  paymentClaimedAt?: string;
  paymentReviewNotes?: string;
  createdAt: string;
  items: Array<{ productId: string; productName: string; quantity: number; unitPrice: number; subtotal: number }>;
};

type PaymentAccount = {
  id: string;
  name: string;
  type: "CASH" | "BANK" | "WALLET" | "OTHER";
  currency: string;
  active: boolean;
  defaultAccount: boolean;
  countryCode?: "EC" | "PE";
  publicForStorefront: boolean;
  bankName?: string;
  accountHolder?: string;
  accountNumber?: string;
  accountType?: string;
  taxId?: string;
};

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

const emptyPage: PageResponse<StorefrontOrder> = {
  content: [], totalElements: 0, totalPages: 0, number: 0, size: 30,
};

const statusLabels: Record<OrderStatus, string> = {
  PAYMENT_REVIEW: "Por verificar",
  PENDING_PAYMENT: "Esperando pago",
  PREPARING: "En preparación",
  READY_FOR_DISPATCH: "Listos",
  SHIPPED: "Despachados",
  DELIVERED: "Entregados",
};

export default function StorefrontOrdersPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [status, setStatus] = useState<OrderStatus>("PAYMENT_REVIEW");
  const [page, setPage] = useState(0);
  const [orders, setOrders] = useState(emptyPage);
  const [selected, setSelected] = useState<StorefrontOrder | null>(null);
  const [paymentAccounts, setPaymentAccounts] = useState<PaymentAccount[]>([]);
  const [reviewAccountId, setReviewAccountId] = useState("");
  const [reviewNotes, setReviewNotes] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const publicBankAccounts = useMemo(
    () => paymentAccounts.filter((account) => account.type === "BANK" && account.publicForStorefront),
    [paymentAccounts]
  );

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [orderPage, accounts] = await Promise.all([
        apiFetch<PageResponse<StorefrontOrder>>(
          `/api/backend/api/v1/storefront/orders?status=${status}&page=${page}&size=30`
        ),
        apiFetch<PaymentAccount[]>("/api/backend/api/v1/payment-accounts?includeInactive=true"),
      ]);
      setOrders(orderPage);
      setPaymentAccounts(accounts);
      setSelected((current) => {
        const next = orderPage.content.find((order) => order.orderId === current?.orderId)
          || orderPage.content[0]
          || null;
        setReviewAccountId(next?.paymentAccountId || "");
        setReviewNotes(next?.paymentReviewNotes || "");
        return next;
      });
    } catch (err) {
      setError((err as ApiError)?.message || "No se pudieron cargar los pedidos web.");
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    if (allowed) void loadData();
  }, [allowed, loadData]);

  useEffect(() => {
    setPage(0);
    setSelected(null);
  }, [status]);

  const selectOrder = (order: StorefrontOrder) => {
    setSelected(order);
    setReviewAccountId(order.paymentAccountId || "");
    setReviewNotes(order.paymentReviewNotes || "");
    setError(null);
    setNotice(null);
  };

  const reviewPayment = async (approved: boolean) => {
    if (!selected) return;
    if (approved && !reviewAccountId) {
      setError("Selecciona la cuenta bancaria donde se verificó el ingreso.");
      return;
    }
    setSubmitting(true);
    setError(null);
    setNotice(null);
    try {
      await apiFetch(`/api/backend/api/v1/storefront/orders/${encodeURIComponent(selected.orderNumber)}/review-payment`, {
        method: "POST",
        body: JSON.stringify({
          approved,
          paymentAccountId: approved ? reviewAccountId : null,
          reviewNotes: reviewNotes.trim() || null,
          notifyClient: true,
        }),
      });
      setNotice(approved
        ? "Pago aprobado. La venta fue registrada y el pedido entró en preparación."
        : "Pago rechazado. La orden volvió a esperar un nuevo registro del cliente.");
      await loadData();
    } catch (err) {
      setError((err as ApiError)?.message || "No se pudo completar la revisión del pago.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  if (authLoading || !allowed || !user) {
    return <div className="p-8 text-sm text-slate/70">Validando acceso...</div>;
  }

  return (
    <main className="min-w-0 space-y-6 p-5 md:p-8">
      <AdminHeader
        title="Pedidos de la tienda"
        subtitle="Pagos, crédito y entrega"
        userEmail={user.email}
        onLogout={handleLogout}
        actions={<div className="flex gap-2"><AdminActionButton variant="ghost" label="Bancos y cajas" onClick={() => router.push("/admin/finance/accounts")} /><AdminActionButton icon={<RefreshIcon className="h-4 w-4" />} label="Actualizar" onClick={() => void loadData()} disabled={loading} /></div>}
      />

      <section className="border-y border-smoke/80 bg-white/70 py-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="inline-flex border border-smoke bg-white p-1" role="group" aria-label="Estado del pedido">
            {(Object.keys(statusLabels) as OrderStatus[]).map((item) => (
              <button key={item} type="button" onClick={() => setStatus(item)} className={`min-h-9 px-4 text-xs font-semibold ${status === item ? "bg-ink text-white" : "text-slate hover:bg-paper"}`}>{statusLabels[item]}</button>
            ))}
          </div>
          <p className="text-sm text-slate/70">{orders.totalElements} pedidos</p>
        </div>
      </section>

      {error ? <div className="border-l-4 border-red-500 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">{error}</div> : null}
      {notice ? <div className="border-l-4 border-emerald-600 bg-emerald-50 px-4 py-3 text-sm text-emerald-800" role="status">{notice}</div> : null}

      <div className="grid min-h-[520px] min-w-0 gap-6 2xl:grid-cols-[minmax(0,1.35fr)_minmax(380px,0.65fr)]">
        <section className="min-w-0 overflow-hidden border border-smoke bg-white">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] border-collapse text-left text-sm">
              <thead className="bg-paper text-[11px] uppercase text-slate/60"><tr><th className="px-4 py-3">Orden</th><th className="px-4 py-3">Cliente</th><th className="px-4 py-3">Pago</th><th className="px-4 py-3">Total</th><th className="px-4 py-3">Fecha</th></tr></thead>
              <tbody>
                {orders.content.map((order) => (
                  <tr key={order.orderId} onClick={() => selectOrder(order)} className={`cursor-pointer border-t border-smoke transition ${selected?.orderId === order.orderId ? "bg-cyan-50" : "hover:bg-paper/70"}`}>
                    <td className="px-4 py-3"><span className="block font-semibold text-ink">{order.orderNumber}</span><span className="text-xs text-slate/60">{order.country} · {order.customerType === "DISTRIBUTOR" ? "Distribuidor" : "Final"}</span></td>
                    <td className="px-4 py-3"><span className="block text-ink">{`${order.firstName || ""} ${order.lastName || ""}`.trim() || "Sin nombre"}</span><span className="text-xs text-slate/60">{order.email}</span></td>
                    <td className="px-4 py-3">{order.checkoutMethod === "CUSTOMER_CREDIT" ? "Crédito" : order.paymentClaimBank || "Transferencia"}</td>
                    <td className="px-4 py-3 font-semibold text-ink">{new Intl.NumberFormat("es", { style: "currency", currency: order.currency }).format(order.total)}</td>
                    <td className="px-4 py-3 text-xs text-slate/70">{formatDateTime(order.paymentClaimedAt || order.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!loading && orders.content.length === 0 ? <div className="grid min-h-72 place-items-center px-6 text-center text-sm text-slate/65"><div><InvoiceIcon className="mx-auto mb-3 h-8 w-8" /><p>No hay pedidos en este estado.</p></div></div> : null}
          {loading ? <div className="p-6 text-sm text-slate/65">Cargando pedidos...</div> : null}
          {orders.totalPages > 1 ? <div className="flex items-center justify-between border-t border-smoke px-4 py-3 text-sm"><button type="button" className="btn btn-secondary" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Anterior</button><span>Página {page + 1} de {orders.totalPages}</span><button type="button" className="btn btn-secondary" disabled={page + 1 >= orders.totalPages} onClick={() => setPage((value) => value + 1)}>Siguiente</button></div> : null}
        </section>

        <aside className="self-start border border-smoke bg-white p-5 2xl:sticky 2xl:top-24">
          {selected ? (
            <div className="space-y-5">
              <div><p className="text-[11px] uppercase text-slate/55">Pedido seleccionado</p><h2 className="mt-1 break-all font-display text-xl text-ink">{selected.orderNumber}</h2></div>
              <dl className="grid grid-cols-[110px_minmax(0,1fr)] gap-x-3 gap-y-3 text-sm">
                <dt className="text-slate/60">Cliente</dt><dd className="break-words text-ink">{`${selected.firstName || ""} ${selected.lastName || ""}`.trim() || selected.email}</dd>
                <dt className="text-slate/60">Correo</dt><dd className="break-all text-ink">{selected.email}</dd>
                <dt className="text-slate/60">Entrega</dt><dd className="text-ink">{[selected.shippingAddressLine1, selected.shippingAddressLine2, selected.shippingCity, selected.shippingRegion].filter(Boolean).join(", ") || "No informada"}</dd>
                <dt className="text-slate/60">Guía</dt><dd className="text-ink">{selected.trackingNumber ? `${selected.shippingCarrier || "Transportista"} · ${selected.trackingNumber}` : "Pendiente"}</dd>
                <dt className="text-slate/60">Banco origen</dt><dd className="text-ink">{selected.paymentClaimBank || "No informado"}</dd>
                <dt className="text-slate/60">Pagador</dt><dd className="text-ink">{selected.paymentClaimPayer || "No informado"}</dd>
                <dt className="text-slate/60">Referencia</dt><dd className="break-all font-mono text-ink">{selected.paymentClaimReference || "No informada"}</dd>
                <dt className="text-slate/60">Reportado</dt><dd className="font-semibold text-ink">{new Intl.NumberFormat("es", { style: "currency", currency: selected.currency }).format(selected.paymentClaimAmount || 0)}</dd>
                <dt className="text-slate/60">Esperado</dt><dd className="font-semibold text-ink">{new Intl.NumberFormat("es", { style: "currency", currency: selected.currency }).format(selected.total)}</dd>
              </dl>
              <div className="border-y border-smoke py-4"><p className="text-xs font-semibold text-slate/60">Productos</p>{selected.items.map((item) => <p className="mt-2 flex justify-between gap-3 text-sm" key={item.productId}><span>{item.quantity} × {item.productName}</span><strong>{new Intl.NumberFormat("es", { style: "currency", currency: selected.currency }).format(item.subtotal)}</strong></p>)}</div>
              {selected.status === "PAYMENT_REVIEW" ? (
                <div className="space-y-3">
                  <label className="block text-sm font-semibold text-slate/75">Cuenta donde ingresó<select className="mt-2 h-11 w-full border border-smoke bg-white px-3 text-sm" value={reviewAccountId} onChange={(event) => setReviewAccountId(event.target.value)}><option value="">Seleccionar cuenta</option>{publicBankAccounts.filter((account) => account.active && account.countryCode === selected.country && account.currency === selected.currency).map((account) => <option key={account.id} value={account.id}>{account.bankName || account.name} · {account.accountNumber}</option>)}</select></label>
                  <label className="block text-sm font-semibold text-slate/75">Observación<textarea className="mt-2 min-h-24 w-full border border-smoke p-3 text-sm outline-none focus:border-accent" maxLength={500} value={reviewNotes} onChange={(event) => setReviewNotes(event.target.value)} /></label>
                  <div className="grid grid-cols-2 gap-3"><button type="button" className="btn btn-primary" disabled={submitting} onClick={() => void reviewPayment(true)}>Aprobar y preparar</button><button type="button" className="btn btn-secondary border-red-200 text-red-700" disabled={submitting} onClick={() => void reviewPayment(false)}>Rechazar</button></div>
                </div>
              ) : <div className="border-l-4 border-accent bg-paper px-4 py-3 text-sm">{statusLabels[status]}</div>}
            </div>
          ) : <p className="text-sm text-slate/65">Selecciona un pedido para revisar sus datos.</p>}
        </aside>
      </div>

    </main>
  );
}
