"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { RefreshIcon } from "@/components/admin/icons";
import { formatCurrency } from "@/lib/format";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type Expense = {
  id: string;
  name: string;
  category: string;
  type: string;
  totalAmount: number;
  months: number;
  monthlyAmount: number;
  dueDay: number;
  priority: string;
  active: boolean;
};

type ExpenseSummary = {
  monthPayments: number;
  monthExpenses: number;
  balance: number;
  status: string;
  recommendation: string;
};

export default function ExpensesPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [summary, setSummary] = useState<ExpenseSummary | null>(null);
  const [form, setForm] = useState({
    name: "",
    category: "",
    type: "MENSUAL",
    totalAmount: "",
    months: "12",
    dueDay: "5",
    priority: "MEDIA",
  });
  const [editingId, setEditingId] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const [list, summaryData] = await Promise.all([
        apiFetch<Expense[]>("/api/backend/api/v1/expenses"),
        apiFetch<ExpenseSummary>("/api/backend/api/v1/expenses/summary"),
      ]);
      setExpenses(list);
      setSummary(summaryData);
    } catch (err: any) {
      if (err?.status === 401) {
        router.push("/login");
        return;
      }
      setError(err?.message || "No se pudo cargar gastos.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (allowed) load();
  }, [allowed]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleSave = async () => {
    const payload = {
      name: form.name,
      category: form.category,
      type: form.type,
      totalAmount: Number(form.totalAmount || 0),
      months: form.type === "MENSUAL" ? 1 : Number(form.months || 1),
      dueDay: Number(form.dueDay || 1),
      priority: form.priority,
      active: true,
    };
    try {
      if (editingId) {
        await apiFetch(`/api/backend/api/v1/expenses/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      } else {
        await apiFetch("/api/backend/api/v1/expenses", {
          method: "POST",
          body: JSON.stringify(payload),
        });
      }
      setEditingId(null);
      setForm({ name: "", category: "", type: "MENSUAL", totalAmount: "", months: "12", dueDay: "5", priority: "MEDIA" });
      await load();
    } catch (err: any) {
      setError(err?.message || "No se pudo guardar el gasto.");
    }
  };

  const handleEdit = (expense: Expense) => {
    setEditingId(expense.id);
    setForm({
      name: expense.name,
      category: expense.category,
      type: expense.type || "MENSUAL",
      totalAmount: String(expense.totalAmount),
      months: String(expense.months),
      dueDay: String(expense.dueDay),
      priority: expense.priority,
    });
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Eliminar este gasto?")) return;
    try {
      await apiFetch(`/api/backend/api/v1/expenses/${id}`, { method: "DELETE" });
      await load();
    } catch (err: any) {
      setError(err?.message || "No se pudo eliminar el gasto.");
    }
  };

  if (authLoading || loading) {
    return (
      <div className="min-h-screen grid place-items-center">
        <div className="rounded-3xl bg-white/70 px-6 py-4 shadow-panel">Cargando gastos...</div>
      </div>
    );
  }

  if (!user || !allowed) return null;

  return (
    <main className="px-6 py-8 lg:px-10">
      <div className="glass-panel hero-band rounded-[32px] px-6 py-6 fade-up">
        <AdminHeader
          title="Gastos y pagos"
          subtitle="Control mensual de gastos fijos y capacidad de pago."
          userEmail={user.email}
          onLogout={handleLogout}
          actions={
            <AdminActionButton
              icon={<RefreshIcon className="h-4 w-4" />}
              label="Actualizar"
              onClick={load}
              variant="primary"
            />
          }
        />
      </div>

      {error && (
        <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      <section className="mt-8 grid gap-4 md:grid-cols-3">
        <div className="glass-panel rounded-3xl p-4">
          <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Pagos del mes</p>
          <p className="mt-2 text-2xl font-semibold text-slate">{formatCurrency(summary?.monthPayments ?? 0)}</p>
        </div>
        <div className="glass-panel rounded-3xl p-4">
          <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Gastos del mes</p>
          <p className="mt-2 text-2xl font-semibold text-slate">{formatCurrency(summary?.monthExpenses ?? 0)}</p>
        </div>
        <div className="glass-panel rounded-3xl p-4">
          <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Saldo</p>
          <p className="mt-2 text-2xl font-semibold text-slate">{formatCurrency(summary?.balance ?? 0)}</p>
          <p className="mt-2 text-xs text-slate/60">{summary?.recommendation}</p>
        </div>
      </section>

      <section className="mt-8 grid gap-6 xl:grid-cols-[1.4fr_0.6fr]">
        <div className="glass-panel rounded-3xl p-4">
          <h2 className="font-display text-lg text-slate">Gastos fijos</h2>
          <div className="mt-3 divide-y divide-smoke/60">
            {expenses.map((item) => (
              <div key={item.id} className="flex items-center justify-between py-3 text-sm">
                <div>
                  <p className="font-medium text-slate">{item.name}</p>
                  <p className="text-xs text-slate/50">
                    {item.category} · {item.type} · {item.priority} · Vence día {item.dueDay}
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-medium text-slate">{formatCurrency(item.monthlyAmount)}</p>
                  <p className="text-xs text-slate/50">Total {formatCurrency(item.totalAmount)}</p>
                  <div className="mt-1 flex gap-2 justify-end">
                    <button className="text-xs text-slate/60" onClick={() => handleEdit(item)}>Editar</button>
                    <button className="text-xs text-red-600" onClick={() => handleDelete(item.id)}>Eliminar</button>
                  </div>
                </div>
              </div>
            ))}
            {expenses.length === 0 && (
              <p className="py-6 text-center text-sm text-slate/60">Sin gastos registrados.</p>
            )}
          </div>
        </div>

        <div className="glass-panel rounded-3xl p-4">
          <h2 className="font-display text-lg text-slate">{editingId ? "Editar" : "Nuevo"} gasto</h2>
          <div className="mt-4 grid gap-2 text-sm">
            <input className="rounded-xl border border-smoke/60 bg-white px-3 py-2" placeholder="Nombre"
              value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} />
            <input className="rounded-xl border border-smoke/60 bg-white px-3 py-2" placeholder="Categoría"
              value={form.category} onChange={(e) => setForm((p) => ({ ...p, category: e.target.value }))} />
            <select
              className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
              value={form.type}
              onChange={(e) => setForm((p) => ({ ...p, type: e.target.value }))}
            >
              <option value="MENSUAL">Mensual</option>
              <option value="DIFERIDO">Diferido</option>
            </select>
            <input className="rounded-xl border border-smoke/60 bg-white px-3 py-2" placeholder="Total"
              value={form.totalAmount} onChange={(e) => setForm((p) => ({ ...p, totalAmount: e.target.value }))} />
            <input className="rounded-xl border border-smoke/60 bg-white px-3 py-2" placeholder="Meses"
              disabled={form.type === "MENSUAL"}
              value={form.months} onChange={(e) => setForm((p) => ({ ...p, months: e.target.value }))} />
            <input className="rounded-xl border border-smoke/60 bg-white px-3 py-2" placeholder="Día de vencimiento"
              value={form.dueDay} onChange={(e) => setForm((p) => ({ ...p, dueDay: e.target.value }))} />
            <input className="rounded-xl border border-smoke/60 bg-white px-3 py-2" placeholder="Prioridad"
              value={form.priority} onChange={(e) => setForm((p) => ({ ...p, priority: e.target.value }))} />
            <button
              onClick={handleSave}
              className="rounded-full bg-ink px-4 py-2 text-[10px] uppercase tracking-[0.18em] text-white"
            >
              {editingId ? "Actualizar" : "Crear"}
            </button>
          </div>
        </div>
      </section>
    </main>
  );
}
