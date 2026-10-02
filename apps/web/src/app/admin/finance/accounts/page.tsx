"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import AdminActionButton from "@/components/admin/AdminActionButton";
import AdminHeader from "@/components/admin/AdminHeader";
import { PlusIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";
import { apiFetch, type ApiError } from "@/lib/api";
import { formatDateTime } from "@/lib/format";

type AccountType = "CASH" | "BANK" | "WALLET" | "OTHER";
type PaymentAccount = {
  id: string; name: string; type: AccountType; currency: string; active: boolean;
  defaultAccount: boolean; countryCode?: "EC" | "PE"; publicForStorefront: boolean;
  bankName?: string; accountHolder?: string; accountNumber?: string; accountType?: string; taxId?: string;
};
type AccountUsage = { account: PaymentAccount; recordedIncome: number; paymentCount: number; lastMovementAt?: string };
type Movement = {
  id: string; paymentAccountId: string; paymentAccountName: string; saleId: string;
  clientName: string; clientEmail: string; amount: number; method: string; reference?: string; createdAt: string;
};
type MovementPage = { content: Movement[]; page: number; size: number; totalElements: number; totalPages: number };

const labels: Record<AccountType, string> = { BANK: "Banco", CASH: "Caja", WALLET: "Billetera", OTHER: "Otra" };
const newAccount = {
  id: "", name: "", type: "BANK" as AccountType, countryCode: "EC" as "EC" | "PE", currency: "USD",
  active: true, defaultAccount: false, publicForStorefront: false, bankName: "", accountHolder: "",
  accountNumber: "", accountType: "Ahorros", taxId: "",
};
const emptyMovements: MovementPage = { content: [], page: 0, size: 30, totalElements: 0, totalPages: 0 };

function money(value: number, currency = "USD") {
  return new Intl.NumberFormat("es", { style: "currency", currency }).format(value || 0);
}

export default function PaymentAccountsPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [accounts, setAccounts] = useState<AccountUsage[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [filter, setFilter] = useState<"ALL" | AccountType>("ALL");
  const [form, setForm] = useState(newAccount);
  const [movements, setMovements] = useState<MovementPage>(emptyMovements);
  const [movementPage, setMovementPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const visibleAccounts = useMemo(
    () => accounts.filter(({ account }) => filter === "ALL" || account.type === filter),
    [accounts, filter]
  );
  const activeCount = accounts.filter(({ account }) => account.active).length;
  const storefrontCount = accounts.filter(({ account }) => account.active && account.publicForStorefront).length;
  const movementCount = accounts.reduce((total, item) => total + Number(item.paymentCount || 0), 0);

  const loadAccounts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<AccountUsage[]>("/api/backend/api/v1/payment-accounts/usage?includeInactive=true");
      setAccounts(data);
      setSelectedId((current) => current || data[0]?.account.id || "");
    } catch (err) {
      setError((err as ApiError)?.message || "No se pudieron cargar las cuentas.");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadMovements = useCallback(async () => {
    const query = new URLSearchParams({ page: movementPage.toString(), size: "30" });
    if (selectedId) query.set("accountId", selectedId);
    try {
      setMovements(await apiFetch<MovementPage>(`/api/backend/api/v1/payment-accounts/movements?${query}`));
    } catch (err) {
      setError((err as ApiError)?.message || "No se pudieron cargar los movimientos.");
    }
  }, [movementPage, selectedId]);

  useEffect(() => { if (allowed) void loadAccounts(); }, [allowed, loadAccounts]);
  useEffect(() => { if (allowed) void loadMovements(); }, [allowed, loadMovements]);

  const edit = (account: PaymentAccount) => {
    setSelectedId(account.id);
    setMovementPage(0);
    setForm({
      id: account.id, name: account.name, type: account.type, countryCode: account.countryCode || "EC",
      currency: account.currency, active: account.active, defaultAccount: account.defaultAccount,
      publicForStorefront: account.publicForStorefront, bankName: account.bankName || "",
      accountHolder: account.accountHolder || "", accountNumber: account.accountNumber || "",
      accountType: account.accountType || "Ahorros", taxId: account.taxId || "",
    });
    setError(null);
    setNotice(null);
  };

  const reset = () => { setForm(newAccount); setError(null); setNotice(null); };

  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setNotice(null);
    const bank = form.type === "BANK";
    try {
      const saved = await apiFetch<PaymentAccount>(
        form.id ? `/api/backend/api/v1/payment-accounts/${form.id}` : "/api/backend/api/v1/payment-accounts",
        {
          method: form.id ? "PUT" : "POST",
          body: JSON.stringify({
            name: form.name.trim(), type: form.type, currency: form.currency, active: form.active,
            defaultAccount: form.defaultAccount, countryCode: form.countryCode,
            publicForStorefront: bank && form.publicForStorefront,
            bankName: bank ? form.bankName.trim() || null : null,
            accountHolder: bank ? form.accountHolder.trim() || null : null,
            accountNumber: bank ? form.accountNumber.trim() || null : null,
            accountType: bank ? form.accountType.trim() || null : null,
            taxId: bank ? form.taxId.trim() || null : null,
          }),
        }
      );
      setSelectedId(saved.id);
      setNotice(form.id ? "Cuenta actualizada." : "Cuenta registrada.");
      setForm(newAccount);
      await loadAccounts();
    } catch (err) {
      setError((err as ApiError)?.message || "No se pudo guardar la cuenta.");
    } finally {
      setSaving(false);
    }
  };

  const logout = async () => { await apiFetch("/api/auth/logout", { method: "POST" }); router.push("/login"); };
  if (authLoading || !allowed || !user) return <div className="p-8 text-sm text-slate/70">Validando acceso...</div>;

  return (
    <main className="min-w-0 space-y-6 p-5 md:p-8">
      <AdminHeader
        title="Bancos y cajas"
        subtitle="Cuentas de cobro y movimientos registrados"
        userEmail={user.email}
        onLogout={logout}
        actions={<div className="flex gap-2"><AdminActionButton icon={<PlusIcon className="h-4 w-4" />} label="Nueva cuenta" onClick={reset} /><AdminActionButton icon={<RefreshIcon className="h-4 w-4" />} variant="ghost" label="Actualizar" onClick={() => void loadAccounts()} disabled={loading} /></div>}
      />

      <section className="grid border-y border-smoke bg-white sm:grid-cols-2 xl:grid-cols-4">
        {[["Cuentas", accounts.length], ["Activas", activeCount], ["Publicadas en tiendas", storefrontCount], ["Movimientos", movementCount]].map(([label, value], index) => (
          <div key={label} className={`px-5 py-4 ${index < 3 ? "border-b border-smoke xl:border-b-0 xl:border-r" : ""}`}><span className="text-xs text-slate/60">{label}</span><strong className="mt-1 block text-2xl text-ink">{value}</strong></div>
        ))}
      </section>
      {error ? <div className="border-l-4 border-red-500 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">{error}</div> : null}
      {notice ? <div className="border-l-4 border-emerald-600 bg-emerald-50 px-4 py-3 text-sm text-emerald-800" role="status">{notice}</div> : null}

      <div className="grid min-w-0 gap-6 2xl:grid-cols-[minmax(0,1.35fr)_minmax(390px,0.65fr)]">
        <section className="min-w-0 overflow-hidden border border-smoke bg-white">
          <div className="flex items-center justify-between gap-3 border-b border-smoke px-4 py-3"><h2 className="font-display text-xl text-ink">Cuentas registradas</h2><select className="h-10 border border-smoke bg-white px-3 text-sm" value={filter} onChange={(event) => setFilter(event.target.value as "ALL" | AccountType)} aria-label="Filtrar cuentas"><option value="ALL">Todos los tipos</option>{Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
          <div className="overflow-x-auto"><table className="w-full min-w-[720px] border-collapse text-left text-sm"><thead className="bg-paper text-[11px] uppercase text-slate/60"><tr><th className="px-4 py-3">Cuenta</th><th className="px-4 py-3">Pais</th><th className="px-4 py-3">Uso</th><th className="px-4 py-3 text-right">Ingresos</th><th className="px-4 py-3">Estado</th></tr></thead><tbody>{visibleAccounts.map(({ account, recordedIncome, paymentCount }) => <tr key={account.id} onClick={() => edit(account)} className={`cursor-pointer border-t border-smoke ${selectedId === account.id ? "bg-cyan-50" : "hover:bg-paper/70"}`}><td className="px-4 py-3"><strong className="block text-ink">{account.name}</strong><span className="text-xs text-slate/60">{labels[account.type]}{account.defaultAccount ? " · Principal" : ""}</span></td><td className="px-4 py-3">{account.countryCode || "General"} · {account.currency}</td><td className="px-4 py-3">{paymentCount} movimientos</td><td className="px-4 py-3 text-right font-semibold text-ink">{money(recordedIncome, account.currency)}</td><td className="px-4 py-3">{account.active ? "Activa" : "Inactiva"}</td></tr>)}</tbody></table></div>
          {!loading && visibleAccounts.length === 0 ? <p className="p-6 text-sm text-slate/65">No hay cuentas para este filtro.</p> : null}
        </section>

        <form className="self-start border border-smoke bg-white p-5 2xl:sticky 2xl:top-24" onSubmit={save}>
          <div className="mb-5 flex items-center justify-between"><div><p className="text-[11px] uppercase text-slate/55">Configuracion</p><h2 className="font-display text-xl text-ink">{form.id ? "Editar cuenta" : "Nueva cuenta"}</h2></div>{form.id ? <button type="button" className="text-xs font-semibold text-accent" onClick={reset}>Nueva</button> : null}</div>
          <div className="space-y-4">
            <label className="block text-xs font-semibold text-slate/70">Tipo<select className="mt-1 h-11 w-full border border-smoke bg-white px-3 text-sm" value={form.type} onChange={(event) => { const type = event.target.value as AccountType; setForm((current) => ({ ...current, type, publicForStorefront: type === "BANK" && current.publicForStorefront })); }}>{Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
            <label className="block text-xs font-semibold text-slate/70">Nombre interno<input className="mt-1 h-11 w-full border border-smoke px-3 text-sm" required maxLength={120} value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} /></label>
            <div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-semibold text-slate/70">Pais<select className="mt-1 h-11 w-full border border-smoke bg-white px-3 text-sm" value={form.countryCode} onChange={(event) => { const countryCode = event.target.value as "EC" | "PE"; setForm((current) => ({ ...current, countryCode, currency: countryCode === "PE" ? "PEN" : "USD" })); }}><option value="EC">Ecuador</option><option value="PE">Peru</option></select></label><label className="text-xs font-semibold text-slate/70">Moneda<input className="mt-1 h-11 w-full border border-smoke bg-paper px-3 text-sm" value={form.currency} readOnly /></label></div>
            {form.type === "BANK" ? <><label className="block text-xs font-semibold text-slate/70">Banco<input className="mt-1 h-11 w-full border border-smoke px-3 text-sm" required={form.publicForStorefront} value={form.bankName} onChange={(event) => setForm((current) => ({ ...current, bankName: event.target.value }))} /></label><label className="block text-xs font-semibold text-slate/70">Titular<input className="mt-1 h-11 w-full border border-smoke px-3 text-sm" required={form.publicForStorefront} value={form.accountHolder} onChange={(event) => setForm((current) => ({ ...current, accountHolder: event.target.value }))} /></label><div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-semibold text-slate/70">Numero<input className="mt-1 h-11 w-full border border-smoke px-3 text-sm" required={form.publicForStorefront} value={form.accountNumber} onChange={(event) => setForm((current) => ({ ...current, accountNumber: event.target.value }))} /></label><label className="text-xs font-semibold text-slate/70">Clase<input className="mt-1 h-11 w-full border border-smoke px-3 text-sm" value={form.accountType} onChange={(event) => setForm((current) => ({ ...current, accountType: event.target.value }))} /></label></div><label className="block text-xs font-semibold text-slate/70">RUC / identificacion<input className="mt-1 h-11 w-full border border-smoke px-3 text-sm" value={form.taxId} onChange={(event) => setForm((current) => ({ ...current, taxId: event.target.value }))} /></label></> : null}
            <div className="grid gap-3 border-y border-smoke py-4 sm:grid-cols-2"><label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.active} onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))} /> Activa</label><label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.defaultAccount} onChange={(event) => setForm((current) => ({ ...current, defaultAccount: event.target.checked }))} /> Principal</label>{form.type === "BANK" ? <label className="flex items-center gap-2 text-sm sm:col-span-2"><input type="checkbox" checked={form.publicForStorefront} onChange={(event) => setForm((current) => ({ ...current, publicForStorefront: event.target.checked }))} /> Publicar en la tienda del pais</label> : null}</div>
            <button className="btn btn-primary w-full" type="submit" disabled={saving}>{saving ? "Guardando..." : form.id ? "Actualizar cuenta" : "Registrar cuenta"}</button>
          </div>
        </form>
      </div>

      <section className="overflow-hidden border border-smoke bg-white">
        <div className="flex items-center justify-between border-b border-smoke px-4 py-3"><div><p className="text-[11px] uppercase text-slate/55">Trazabilidad</p><h2 className="font-display text-xl text-ink">Movimientos registrados</h2></div><span className="text-sm text-slate/60">{movements.totalElements} ingresos</span></div>
        <div className="overflow-x-auto"><table className="w-full min-w-[800px] border-collapse text-left text-sm"><thead className="bg-paper text-[11px] uppercase text-slate/60"><tr><th className="px-4 py-3">Fecha</th><th className="px-4 py-3">Cuenta</th><th className="px-4 py-3">Cliente</th><th className="px-4 py-3">Referencia</th><th className="px-4 py-3 text-right">Ingreso</th></tr></thead><tbody>{movements.content.map((item) => <tr key={item.id} className="border-t border-smoke"><td className="px-4 py-3 text-xs">{formatDateTime(item.createdAt)}</td><td className="px-4 py-3">{item.paymentAccountName}</td><td className="px-4 py-3"><strong className="block font-medium">{item.clientName || item.clientEmail}</strong><span className="text-xs text-slate/60">{item.clientEmail}</span></td><td className="px-4 py-3 font-mono text-xs">{item.reference || item.method}</td><td className="px-4 py-3 text-right font-semibold text-emerald-700">{money(item.amount, accounts.find(({ account }) => account.id === item.paymentAccountId)?.account.currency)}</td></tr>)}</tbody></table></div>
        {!loading && movements.content.length === 0 ? <p className="p-6 text-sm text-slate/65">Esta cuenta todavia no tiene movimientos.</p> : null}
        {movements.totalPages > 1 ? <div className="flex items-center justify-between border-t border-smoke px-4 py-3 text-sm"><button type="button" className="btn btn-secondary" disabled={movementPage === 0} onClick={() => setMovementPage((value) => Math.max(0, value - 1))}>Anterior</button><span>Pagina {movementPage + 1} de {movements.totalPages}</span><button type="button" className="btn btn-secondary" disabled={movementPage + 1 >= movements.totalPages} onClick={() => setMovementPage((value) => value + 1)}>Siguiente</button></div> : null}
      </section>
    </main>
  );
}
