"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency, formatDate } from "@/lib/format";
import { DownloadIcon, RefreshIcon, SparkIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type ArSaleDetail = {
  accountsReceivableId: string;
  saleId: string;
  products: string;
  total: number;
  paid: number;
  balance: number;
  dueDate: string;
  status: "OPEN" | "OVERDUE" | "PAID";
  saleCreatedAt: string;
};

type ArClientSummary = {
  clientId: string;
  clientName: string;
  clientEmail: string;
  clientPhone: string | null;
  salesCount: number;
  total: number;
  paid: number;
  balance: number;
  overdueBalance: number;
  upcomingBalance: number;
  oldestDueDate: string | null;
  nextDueDate: string | null;
  maxDaysOverdue: number;
  sales: ArSaleDetail[];
};

type ReminderResponse = {
  outboxEventId: string;
  accountsReceivableId: string;
  clientId: string;
  dueDate: string | null;
  balance: number;
  daysOverdue: number;
  status: string;
  message: string;
  emailSent?: boolean;
  whatsappSent?: boolean;
};

type PaymentAccount = {
  id: string;
  name: string;
  type: "CASH" | "BANK" | "WALLET" | "OTHER";
  currency: string;
  active: boolean;
  defaultAccount: boolean;
};

type PaymentResponse = {
  success: boolean;
  message: string;
  summary: ArClientSummary | null;
};

const paymentMethods = ["CASH", "TRANSFER", "CARD"] as const;
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function parsePaymentAmount(value: string) {
  const normalized = value.trim().replace(",", ".");
  const amount = Number(normalized);
  return Number.isFinite(amount) ? amount : null;
}

function resolvePaymentAccountId(value: string) {
  return uuidPattern.test(value) ? value : null;
}

function resolvePaymentMethod(value: string): (typeof paymentMethods)[number] {
  return paymentMethods.includes(value as (typeof paymentMethods)[number])
    ? (value as (typeof paymentMethods)[number])
    : "CASH";
}

export default function AdminArPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [overdue, setOverdue] = useState<ArClientSummary[]>([]);
  const [upcoming, setUpcoming] = useState<ArClientSummary[]>([]);
  const [paymentAccounts, setPaymentAccounts] = useState<PaymentAccount[]>([]);
  const [paymentTarget, setPaymentTarget] = useState<{ item: ArClientSummary; status: "OVERDUE" | "OPEN" } | null>(null);
  const [paymentForm, setPaymentForm] = useState({
    amount: "",
    method: "CASH",
    paymentAccountId: "",
    reference: "",
    notifyClient: true,
  });
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [sendingAll, setSendingAll] = useState(false);
  const [sendingByClient, setSendingByClient] = useState<Record<string, boolean>>({});
  const [savingPayment, setSavingPayment] = useState(false);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const [overdueData, upcomingData, accounts] = await Promise.all([
        apiFetch<ArClientSummary[]>("/api/backend/api/v1/ar/grouped?status=OVERDUE"),
        apiFetch<ArClientSummary[]>("/api/backend/api/v1/ar/grouped?status=OPEN"),
        apiFetch<PaymentAccount[]>("/api/backend/api/v1/payment-accounts").catch(() => []),
      ]);
      setOverdue(overdueData);
      setUpcoming(upcomingData);
      setPaymentAccounts(accounts);
      const defaultAccount = accounts.find((account) => account.defaultAccount) ?? accounts[0];
      if (defaultAccount) {
        setPaymentForm((prev) => ({ ...prev, paymentAccountId: prev.paymentAccountId || defaultAccount.id }));
      }
    } catch (err: any) {
      if (err?.status === 401) {
        router.push("/login");
        return;
      }
      setError(err?.message || "No se pudo cargar cuentas por cobrar.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!allowed) return;
    load();
  }, [allowed]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const exportRows = useMemo(() => [...overdue, ...upcoming], [overdue, upcoming]);

  const handleExport = () => {
    const rows = [
      ["cliente", "email", "telefono", "ventas", "total", "pagado", "saldo", "vencido", "por_vencer"],
      ...exportRows.map((item) => [
        item.clientName || "-",
        item.clientEmail || "",
        item.clientPhone || "",
        String(item.salesCount),
        String(item.total ?? 0),
        String(item.paid ?? 0),
        String(item.balance ?? 0),
        String(item.overdueBalance ?? 0),
        String(item.upcomingBalance ?? 0),
      ]),
    ];
    const csv = rows
      .map((row) => row.map((value) => `"${String(value ?? "").replace(/"/g, '""')}"`).join(","))
      .join("\n");

    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "dismal-ar-clientes.csv";
    link.click();
    URL.revokeObjectURL(url);
    setNotice("Exportacion de AR generada.");
    setTimeout(() => setNotice(null), 2500);
  };

  const handleReminder = async (item: ArClientSummary, status: "OVERDUE" | "OPEN") => {
    setSendingByClient((prev) => ({ ...prev, [item.clientId]: true }));
    setError(null);
    try {
      const response = await apiFetch<ReminderResponse>(
        `/api/backend/api/v1/ar/client/${item.clientId}/reminder?status=${status}`,
        { method: "POST" }
      );
      if (response.whatsappSent || response.emailSent) {
        setNotice(`${item.clientName || item.clientEmail}: ${response.message}`);
      } else {
        setError(`${item.clientName || item.clientEmail}: ${response.message}`);
      }
      setTimeout(() => setNotice(null), 3500);
    } catch (err: any) {
      setError(err?.message || "No se pudo enviar el recordatorio.");
    } finally {
      setSendingByClient((prev) => ({ ...prev, [item.clientId]: false }));
    }
  };

  const handleSendAllReminders = async () => {
    if (overdue.length === 0) {
      setNotice("No hay clientes vencidos para recordar.");
      setTimeout(() => setNotice(null), 2500);
      return;
    }
    setSendingAll(true);
    setError(null);
    let success = 0;
    for (const item of overdue) {
      try {
        const response = await apiFetch<ReminderResponse>(
          `/api/backend/api/v1/ar/client/${item.clientId}/reminder?status=OVERDUE`,
          { method: "POST" }
        );
        if (response.whatsappSent || response.emailSent) success += 1;
      } catch (err: any) {
        setError(err?.message || "No se pudieron enviar todos los recordatorios.");
      }
    }
    setSendingAll(false);
    setNotice(`Recordatorios enviados: ${success}/${overdue.length}.`);
    setTimeout(() => setNotice(null), 3500);
  };

  const openPayment = (item: ArClientSummary, status: "OVERDUE" | "OPEN") => {
    const defaultAccount = paymentAccounts.find((account) => account.defaultAccount) ?? paymentAccounts[0];
    setPaymentTarget({ item, status });
    setPaymentForm({
      amount: String(item.balance),
      method: "CASH",
      paymentAccountId: defaultAccount?.id || "",
      reference: "",
      notifyClient: true,
    });
  };

  const handleRegisterPayment = async () => {
    if (!paymentTarget) return;
    const amount = parsePaymentAmount(paymentForm.amount);
    if (!amount || amount <= 0) {
      setError("Ingresa un monto valido para el cobro.");
      return;
    }
    setSavingPayment(true);
    setError(null);
    try {
      const response = await apiFetch<PaymentResponse>(`/api/backend/api/v1/ar/client/${paymentTarget.item.clientId}/payments`, {
        method: "POST",
        body: JSON.stringify({
          amount,
          method: resolvePaymentMethod(paymentForm.method),
          paymentAccountId: resolvePaymentAccountId(paymentForm.paymentAccountId),
          reference: paymentForm.reference || null,
          status: paymentTarget.status,
          notifyClient: paymentForm.notifyClient,
        }),
      });
      setNotice(response?.message || "Cobro registrado correctamente.");
      setPaymentTarget(null);
      await load();
      setTimeout(() => setNotice(null), 3000);
    } catch (err: any) {
      setError(err?.message || "No se pudo registrar el cobro.");
    } finally {
      setSavingPayment(false);
    }
  };

  if (authLoading || loading) {
    return (
      <div className="min-h-screen grid place-items-center">
        <div className="rounded-3xl bg-white/70 px-6 py-4 shadow-panel">Cargando cuentas...</div>
      </div>
    );
  }

  if (!user || !allowed) return null;

  return (
    <div className="min-h-screen flex">
      <main className="flex-1 px-6 py-8 lg:px-10">
        <div className="glass-panel hero-band rounded-[32px] px-6 py-6 fade-up">
          <AdminHeader
            title="Cuentas por cobrar"
            subtitle="AR por cliente"
            userEmail={user.email}
            onLogout={handleLogout}
            actions={
              <>
                <AdminActionButton
                  icon={<SparkIcon className="h-4 w-4" />}
                  label={sendingAll ? "Enviando..." : "Recordar vencidos"}
                  variant="primary"
                  onClick={handleSendAllReminders}
                />
                <AdminActionButton icon={<DownloadIcon className="h-4 w-4" />} label="Exportar" onClick={handleExport} />
                <AdminActionButton icon={<RefreshIcon className="h-4 w-4" />} label="Actualizar" onClick={load} />
              </>
            }
          />
        </div>

        {error && <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
        {notice && <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">{notice}</div>}

        <section className="mt-10 grid gap-6 xl:grid-cols-2">
          <ArPanel
            title="AR vencido"
            emptyText="Sin clientes vencidos."
            items={overdue}
            status="OVERDUE"
            expanded={expanded}
            sendingByClient={sendingByClient}
            onToggle={(clientId) => setExpanded((prev) => ({ ...prev, [clientId]: !prev[clientId] }))}
            onReminder={handleReminder}
            onPayment={openPayment}
          />
          <ArPanel
            title="AR por vencer"
            emptyText="Sin clientes por vencer."
            items={upcoming}
            status="OPEN"
            expanded={expanded}
            sendingByClient={sendingByClient}
            onToggle={(clientId) => setExpanded((prev) => ({ ...prev, [clientId]: !prev[clientId] }))}
            onReminder={handleReminder}
            onPayment={openPayment}
          />
        </section>

        {paymentTarget && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 px-4">
            <div className="w-full max-w-lg rounded-[28px] border border-smoke/60 bg-white p-6 shadow-[0_30px_80px_rgba(15,23,42,0.25)]">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs uppercase tracking-[0.3em] text-slate/45">Cobranza</p>
                  <h3 className="mt-2 font-display text-2xl text-slate">Registrar cobro</h3>
                  <p className="mt-2 text-sm text-slate/60">
                    {paymentTarget.item.clientName} - saldo {formatCurrency(paymentTarget.item.balance)}
                  </p>
                </div>
                <button
                  type="button"
                  className="rounded-full border border-smoke/60 px-3 py-1 text-[11px] uppercase tracking-[0.2em] text-slate"
                  onClick={() => setPaymentTarget(null)}
                >
                  Cerrar
                </button>
              </div>

              <div className="mt-6 grid gap-4">
                <label className="flex flex-col gap-2 text-sm">
                  Monto
                  <input
                    type="number"
                    min={0}
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    value={paymentForm.amount}
                    onChange={(event) => setPaymentForm((prev) => ({ ...prev, amount: event.target.value }))}
                  />
                </label>
                <label className="flex flex-col gap-2 text-sm">
                  Metodo
                  <select
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    value={paymentForm.method}
                    onChange={(event) => setPaymentForm((prev) => ({ ...prev, method: event.target.value }))}
                  >
                    {paymentMethods.map((method) => (
                      <option key={method} value={method}>
                        {method === "CASH" ? "Efectivo" : method === "TRANSFER" ? "Transferencia" : "Tarjeta"}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="flex flex-col gap-2 text-sm">
                  Cuenta destino
                  <select
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    value={paymentForm.paymentAccountId}
                    onChange={(event) => setPaymentForm((prev) => ({ ...prev, paymentAccountId: event.target.value }))}
                  >
                    {paymentAccounts.length === 0 && <option value="">Caja principal</option>}
                    {paymentAccounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.name} - {account.currency}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="flex flex-col gap-2 text-sm">
                  Referencia
                  <input
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    placeholder="Comprobante, transferencia u observacion"
                    value={paymentForm.reference}
                    onChange={(event) => setPaymentForm((prev) => ({ ...prev, reference: event.target.value }))}
                  />
                </label>
                <label className="flex items-center gap-2 rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm text-slate">
                  <input
                    type="checkbox"
                    checked={paymentForm.notifyClient}
                    onChange={(event) =>
                      setPaymentForm((prev) => ({ ...prev, notifyClient: event.target.checked }))
                    }
                  />
                  Notificar al cliente por email y WhatsApp
                </label>
              </div>

              <div className="mt-6 flex flex-wrap items-center gap-3">
                <button
                  type="button"
                  disabled={savingPayment}
                  className="rounded-full bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white disabled:opacity-60"
                  onClick={handleRegisterPayment}
                >
                  {savingPayment ? "Registrando..." : "Registrar cobro"}
                </button>
                <button
                  type="button"
                  className="rounded-full border border-smoke/60 px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate"
                  onClick={() => setPaymentTarget(null)}
                >
                  Cancelar
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

function ArPanel({
  title,
  emptyText,
  items,
  status,
  expanded,
  sendingByClient,
  onToggle,
  onReminder,
  onPayment,
}: {
  title: string;
  emptyText: string;
  items: ArClientSummary[];
  status: "OVERDUE" | "OPEN";
  expanded: Record<string, boolean>;
  sendingByClient: Record<string, boolean>;
  onToggle: (clientId: string) => void;
  onReminder: (item: ArClientSummary, status: "OVERDUE" | "OPEN") => void;
  onPayment: (item: ArClientSummary, status: "OVERDUE" | "OPEN") => void;
}) {
  return (
    <div className="glass-panel rounded-3xl p-6 fade-up delay-1">
      <div className="flex items-center justify-between">
        <h2 className="font-display text-xl text-slate">{title}</h2>
        <span className="rounded-full border border-smoke/60 bg-white px-3 py-1 text-xs text-slate">
          {items.length} clientes
        </span>
      </div>
      <div className="mt-4 space-y-3">
        {items.map((item) => {
          const isOpen = Boolean(expanded[item.clientId]);
          return (
            <div key={item.clientId} className="rounded-2xl bg-smoke/30 p-4 soft-shadow">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-medium text-slate">{item.clientName || "-"}</p>
                  <p className="text-xs text-slate/60">{item.clientEmail}</p>
                  <p className="text-xs text-slate/60">{item.clientPhone || "Sin telefono"}</p>
                </div>
                <div className="text-right text-sm text-slate">
                  {formatCurrency(item.balance)}
                  <div className="text-xs text-slate/50">{item.salesCount} ventas pendientes</div>
                  <div className="text-xs text-slate/50">
                    Vence {formatDate(item.nextDueDate || item.oldestDueDate || "")}
                  </div>
                </div>
              </div>
              <div className="mt-3 grid grid-cols-3 gap-2 text-xs">
                <div className="rounded-xl bg-white/80 p-2">
                  <span className="block text-slate/50">Total</span>
                  {formatCurrency(item.total)}
                </div>
                <div className="rounded-xl bg-white/80 p-2">
                  <span className="block text-slate/50">Pagado</span>
                  {formatCurrency(item.paid)}
                </div>
                <div className="rounded-xl bg-white/80 p-2">
                  <span className="block text-slate/50">Saldo</span>
                  {formatCurrency(item.balance)}
                </div>
              </div>
              <div className="mt-3 flex flex-wrap justify-end gap-2">
                <button
                  type="button"
                  onClick={() => onToggle(item.clientId)}
                  className="rounded-full border border-smoke/70 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-slate"
                >
                  {isOpen ? "Ocultar detalle" : "Ver detalle"}
                </button>
                <button
                  type="button"
                  onClick={() => onPayment(item, status)}
                  className="rounded-full border border-emerald-200 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-emerald-700"
                >
                  Registrar cobro
                </button>
                <button
                  type="button"
                  onClick={() => onReminder(item, status)}
                  disabled={Boolean(sendingByClient[item.clientId])}
                  className="rounded-full border border-sky-200 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-sky-700 disabled:opacity-60"
                >
                  {sendingByClient[item.clientId] ? "Enviando..." : status === "OVERDUE" ? "Recordar" : "Notificar"}
                </button>
              </div>
              {isOpen && (
                <div className="mt-3 divide-y divide-smoke/60 rounded-2xl border border-smoke/60 bg-white">
                  {item.sales.map((sale) => (
                    <div key={sale.accountsReceivableId} className="grid gap-2 p-3 text-xs md:grid-cols-[1fr_auto]">
                      <div>
                        <p className="font-medium text-slate">{sale.products || sale.saleId}</p>
                        <p className="text-slate/60">Venta {sale.saleId}</p>
                        <p className="text-slate/60">Vence {formatDate(sale.dueDate)}</p>
                      </div>
                      <div className="text-right">
                        <p>Saldo {formatCurrency(sale.balance)}</p>
                        <p className="text-slate/60">Total {formatCurrency(sale.total)}</p>
                        <p className="text-slate/60">Pagado {formatCurrency(sale.paid)}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
        {items.length === 0 && (
          <div className="rounded-2xl border border-dashed border-smoke/60 p-6 text-center text-sm text-slate/60">
            {emptyText}
          </div>
        )}
      </div>
    </div>
  );
}
