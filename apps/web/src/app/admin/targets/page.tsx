"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency, formatDate, formatNumber } from "@/lib/format";
import { DownloadIcon, PlusIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type SalesTarget = {
  id: string;
  productName: string;
  metaUnits: number;
  unitsSoldCurrent: number;
  marginUnit: number;
  expectedProfit: number;
  breakEvenUnits: number | null;
  deadline: string | null;
  profitable: boolean;
  targetAchieved: boolean;
};

const defaultPage: PageResponse<unknown> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 20,
  number: 0,
};

export default function AdminTargetsPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [targets, setTargets] = useState<PageResponse<SalesTarget>>(defaultPage as PageResponse<SalesTarget>);

  const loadTargets = useCallback(async () => {
    const targetData = await apiFetch<PageResponse<SalesTarget>>(
      "/api/backend/api/v1/dashboard/admin/sales-targets?sort=profit&page=0&size=20"
    );
    setTargets(targetData);
  }, []);

  useEffect(() => {
    if (!allowed) {
      return;
    }
    let active = true;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const targetData = await apiFetch<PageResponse<SalesTarget>>(
          "/api/backend/api/v1/dashboard/admin/sales-targets?sort=profit&page=0&size=20"
        );
        if (!active) return;
        setTargets(targetData);
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar metas.");
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    load();
    return () => {
      active = false;
    };
  }, [allowed, router]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    setError(null);
    try {
      await loadTargets();
      setNotice("Metas actualizadas.");
      setTimeout(() => setNotice(null), 2200);
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar metas.");
    } finally {
      setRefreshing(false);
    }
  };

  const handleExport = () => {
    const header = [
      "Producto",
      "MetaUnidades",
      "Vendidas",
      "MargenUnitario",
      "GananciaEsperada",
      "BreakEven",
      "Deadline",
    ];
    const rows = targets.content.map((item) => [
      item.productName,
      item.metaUnits,
      item.unitsSoldCurrent,
      item.marginUnit,
      item.expectedProfit,
      item.breakEvenUnits ?? "",
      item.deadline ?? "",
    ]);
    const csv = [header, ...rows]
      .map((row) => row.map((value) => `"${String(value ?? "").replaceAll('"', '""')}"`).join(","))
      .join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `metas_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const achievedCount = useMemo(
    () => targets.content.filter((item) => item.targetAchieved).length,
    [targets.content]
  );
  const riskyCount = useMemo(
    () => targets.content.filter((item) => !item.profitable).length,
    [targets.content]
  );
  const expectedProfitTotal = useMemo(
    () => targets.content.reduce((acc, item) => acc + Number(item.expectedProfit || 0), 0),
    [targets.content]
  );

  if (authLoading || loading) {
    return (
      <div className="min-h-screen grid place-items-center">
        <div className="rounded-3xl bg-white/70 px-6 py-4 shadow-panel">Cargando metas...</div>
      </div>
    );
  }

  if (!user || !allowed) {
    return null;
  }

  return (
    <div className="min-h-screen flex">
      <main className="flex-1 px-6 py-8 lg:px-10">
        <div className="glass-panel hero-band rounded-[32px] px-6 py-6 fade-up">
          <AdminHeader
            title="Metas de ventas"
            subtitle="Seguimiento"
            userEmail={user.email}
            onLogout={handleLogout}
            actions={
              <>
                <AdminActionButton
                  icon={<PlusIcon className="h-4 w-4" />}
                  label="Nueva meta"
                  variant="primary"
                  onClick={() => router.push("/admin/planning/targets")}
                />
                <AdminActionButton
                  icon={<DownloadIcon className="h-4 w-4" />}
                  label="Exportar"
                  onClick={handleExport}
                />
                <AdminActionButton
                  icon={<RefreshIcon className="h-4 w-4" />}
                  label={refreshing ? "Actualizando" : "Actualizar"}
                  onClick={handleRefresh}
                />
              </>
            }
          />
        </div>

        {error && (
          <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {error}
          </div>
        )}
        {notice && (
          <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">
            {notice}
          </div>
        )}

        <section className="mt-6 grid gap-4 md:grid-cols-4">
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-[10px] uppercase tracking-[0.18em] text-slate/50">Metas activas</p>
            <p className="mt-2 text-2xl font-semibold text-ink">{targets.totalElements}</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-[10px] uppercase tracking-[0.18em] text-slate/50">Cumplidas</p>
            <p className="mt-2 text-2xl font-semibold text-ink">{achievedCount}</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-[10px] uppercase tracking-[0.18em] text-slate/50">No rentables</p>
            <p className="mt-2 text-2xl font-semibold text-ink">{riskyCount}</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-[10px] uppercase tracking-[0.18em] text-slate/50">Ganancia esperada</p>
            <p className="mt-2 text-xl font-semibold text-ink">{formatCurrency(expectedProfitTotal)}</p>
          </div>
        </section>

        <section className="mt-6 rounded-2xl border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-800">
          Esta pantalla resume el avance de metas. Para crear, editar precios, costos o rentabilidad usa{" "}
          <button
            type="button"
            className="font-semibold underline"
            onClick={() => router.push("/admin/planning/targets")}
          >
            Planeacion
          </button>
          .
        </section>

        <section className="mt-10 glass-panel rounded-3xl p-6 fade-up delay-1">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-xl text-slate">Objetivos activos</h2>
            <span className="text-xs uppercase tracking-[0.2em] text-slate/50">
              Total {targets.totalElements}
            </span>
          </div>
          <div className="mt-4 overflow-auto">
            <table className="min-w-full text-sm">
              <thead className="text-xs uppercase text-slate/60">
                <tr>
                  <th className="py-2 text-left">Producto</th>
                  <th className="py-2 text-right">Meta</th>
                  <th className="py-2 text-right">Vendidas</th>
                  <th className="py-2 text-right">Margen</th>
                  <th className="py-2 text-right">Ganancia</th>
                  <th className="py-2 text-left">Deadline</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-smoke/60">
                {targets.content.map((item) => (
                  <tr key={item.id} className="text-slate/80 hover:bg-paper/60">
                    <td className="py-3">
                      <div className="font-medium text-slate">{item.productName}</div>
                      <div className="text-xs text-slate/50">
                        Punto de equilibrio: {item.breakEvenUnits ?? "-"} unidades
                      </div>
                    </td>
                    <td className="py-3 text-right font-medium">{formatNumber(item.metaUnits)}</td>
                    <td className="py-3 text-right">{formatNumber(item.unitsSoldCurrent)}</td>
                    <td className="py-3 text-right">{formatCurrency(item.marginUnit)}</td>
                    <td className="py-3 text-right">{formatCurrency(item.expectedProfit)}</td>
                    <td className="py-3 text-left">{formatDate(item.deadline)}</td>
                  </tr>
                ))}
                {targets.content.length === 0 && (
                  <tr>
                    <td className="py-6 text-center text-sm text-slate/60" colSpan={6}>
                      Sin metas registradas.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}
