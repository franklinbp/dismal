"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { RefreshIcon, SparkIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type StrategyAction = {
  id: string;
  productId: string | null;
  productName: string | null;
  targetId: string | null;
  source: string;
  priority: string;
  status: string;
  recommendedChannel: string | null;
  title: string;
  description: string | null;
  assignedTo: string | null;
  assignedToName: string | null;
  dueDate: string | null;
  completedAt: string | null;
  resultNotes: string | null;
  createdAt: string;
  updatedAt: string;
};

const emptyPage: PageResponse<StrategyAction> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 20,
  number: 0,
};

const statuses = ["TODAS", "PENDIENTE", "EN_PROGRESO", "HECHA", "DESCARTADA"];

export default function StrategyActionsPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [status, setStatus] = useState("TODAS");
  const [page, setPage] = useState(0);
  const [actions, setActions] = useState<PageResponse<StrategyAction>>(emptyPage);

  const loadActions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.set("page", String(page));
      params.set("size", "20");
      if (status !== "TODAS") params.set("status", status);
      const data = await apiFetch<PageResponse<StrategyAction>>(
        `/api/backend/api/v1/marketing/strategy/actions?${params.toString()}`
      );
      setActions(data);
    } catch (err: any) {
      setError(err?.message || "No se pudo cargar acciones estrategicas.");
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    if (allowed) loadActions();
  }, [allowed, loadActions]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const updateStatus = async (id: string, nextStatus: string) => {
    setSavingId(id);
    setError(null);
    try {
      await apiFetch(`/api/backend/api/v1/marketing/strategy/actions/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: nextStatus }),
      });
      setNotice("Accion actualizada.");
      await loadActions();
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar la accion.");
    } finally {
      setSavingId(null);
    }
  };

  const summary = useMemo(() => {
    return actions.content.reduce(
      (acc, item) => {
        acc.total += 1;
        acc[item.status] = (acc[item.status] || 0) + 1;
        if (item.priority === "ALTA") acc.high += 1;
        return acc;
      },
      { total: 0, high: 0 } as Record<string, number>
    );
  }, [actions.content]);

  if (authLoading || (loading && actions.content.length === 0)) {
    return <div className="min-h-screen grid place-items-center">Cargando acciones...</div>;
  }

  if (!user || !allowed) return null;

  return (
    <div className="min-h-screen">
      <div className="px-6 py-6 border-b border-slate-200 bg-white/80">
        <AdminHeader
          title="Acciones estrategicas"
          subtitle="Seguimiento operativo de MIP, metas, cartera, stock y campanas."
          userEmail={user.email}
          onLogout={handleLogout}
          actions={
            <div className="flex flex-wrap gap-2">
              <AdminActionButton
                icon={<SparkIcon className="h-4 w-4" />}
                label="Ir a MIP"
                onClick={() => router.push("/admin/marketing/mip")}
              />
              <AdminActionButton
                icon={<RefreshIcon className="h-4 w-4" />}
                label="Actualizar"
                onClick={loadActions}
                disabled={loading}
                variant="primary"
              />
            </div>
          }
        />
      </div>

      <main className="px-6 py-8 space-y-6">
        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
        {notice && <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">{notice}</div>}

        <section className="grid gap-4 md:grid-cols-4">
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/60">Total</p>
            <p className="mt-2 font-display text-3xl text-slate">{actions.totalElements}</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/60">Pendientes</p>
            <p className="mt-2 font-display text-3xl text-slate">{summary.PENDIENTE || 0}</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/60">En progreso</p>
            <p className="mt-2 font-display text-3xl text-slate">{summary.EN_PROGRESO || 0}</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/60">Prioridad alta</p>
            <p className="mt-2 font-display text-3xl text-slate">{summary.high || 0}</p>
          </div>
        </section>

        <section className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
          <div className="flex flex-wrap items-center gap-3">
            <select
              className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
              value={status}
              onChange={(event) => {
                setStatus(event.target.value);
                setPage(0);
              }}
            >
              {statuses.map((item) => (
                <option key={item} value={item}>{item}</option>
              ))}
            </select>
            <span className="text-sm text-slate/60">{actions.totalElements} acciones encontradas</span>
          </div>
        </section>

        <section className="space-y-3">
          {actions.content.map((item) => (
            <article key={item.id} className="rounded-2xl border border-smoke/60 bg-white/90 p-5">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full border border-smoke/60 px-2 py-0.5 text-[10px] uppercase tracking-[0.18em] text-slate/60">{item.source}</span>
                    <span className="rounded-full border border-smoke/60 px-2 py-0.5 text-[10px] uppercase tracking-[0.18em] text-slate/60">{item.priority}</span>
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] uppercase tracking-[0.18em] text-slate">{item.status}</span>
                  </div>
                  <h2 className="mt-3 font-display text-xl text-slate">{item.title}</h2>
                  <p className="mt-1 text-sm text-slate/60">{item.productName || "Sin producto"} · {item.recommendedChannel || "Sin canal"}</p>
                  {item.description && <p className="mt-3 text-sm text-slate/70">{item.description}</p>}
                  <p className="mt-3 text-xs text-slate/50">
                    Responsable: {item.assignedToName || "Sin asignar"} · Vence: {item.dueDate || "Sin fecha"}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button
                    className="rounded-full border border-smoke/60 px-3 py-1 text-xs text-slate"
                    disabled={savingId === item.id || item.status === "EN_PROGRESO"}
                    onClick={() => updateStatus(item.id, "EN_PROGRESO")}
                  >
                    En progreso
                  </button>
                  <button
                    className="rounded-full bg-ink px-3 py-1 text-xs text-white disabled:opacity-60"
                    disabled={savingId === item.id || item.status === "HECHA"}
                    onClick={() => updateStatus(item.id, "HECHA")}
                  >
                    Hecha
                  </button>
                  <button
                    className="rounded-full border border-red-200 bg-red-50 px-3 py-1 text-xs text-red-700"
                    disabled={savingId === item.id || item.status === "DESCARTADA"}
                    onClick={() => updateStatus(item.id, "DESCARTADA")}
                  >
                    Descartar
                  </button>
                </div>
              </div>
            </article>
          ))}
          {actions.content.length === 0 && (
            <div className="rounded-2xl border border-dashed border-smoke/60 p-8 text-center text-sm text-slate/60">
              No hay acciones en este estado.
            </div>
          )}
        </section>

        <div className="flex items-center justify-between text-sm text-slate/60">
          <span>Pagina {actions.number + 1} de {Math.max(actions.totalPages, 1)}</span>
          <div className="flex gap-2">
            <button className="rounded-full border border-smoke/60 px-3 py-1" disabled={page <= 0} onClick={() => setPage((prev) => Math.max(0, prev - 1))}>Anterior</button>
            <button className="rounded-full border border-smoke/60 px-3 py-1" disabled={page + 1 >= actions.totalPages} onClick={() => setPage((prev) => prev + 1)}>Siguiente</button>
          </div>
        </div>
      </main>
    </div>
  );
}
