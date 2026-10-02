"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import ReportChart from "@/components/admin/ReportChart";
import { formatCurrency, formatDate, formatNumber } from "@/lib/format";
import { RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type VsTargetItem = {
  targetId: string;
  softwareId: string;
  softwareName: string;
  metaUnits: number;
  actualUnits: number;
  varianceUnits: number;
  achieved: boolean;
  targetSalePrice: number;
  deadline: string | null;
};

type VsTargetReport = {
  from: string;
  to: string;
  totalTargets: number;
  achievedTargets: number;
  totalActualUnits: number;
  items: VsTargetItem[];
};

export default function SalesVsTargetsReportPage() {
  const router = useRouter();
  const { user, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [report, setReport] = useState<VsTargetReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState({
    from: "",
    to: "",
  });

  const chartPoints = useMemo(() => {
    if (!report) return [];
    return report.items.slice(0, 8).map((item) => ({
      label: item.softwareName,
      value: item.actualUnits,
    }));
  }, [report]);

  const loadReport = useCallback(async () => {
    const params = new URLSearchParams();
    if (filters.from) params.set("from", filters.from);
    if (filters.to) params.set("to", filters.to);
    const data = await apiFetch<VsTargetReport>(
      `/api/backend/api/v1/reports/sales/vs-targets?${params.toString()}`
    );
    setReport(data);
  }, [filters.from, filters.to]);

  useEffect(() => {
    const boot = async () => {
      if (!allowed || !user) return;
      setError(null);
      try {
        await loadReport();
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar el reporte.");
      }
    };
    boot();
  }, [allowed, user, loadReport, router]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleRefresh = async () => {
    setError(null);
    try {
      await loadReport();
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="flex">
        <main className="flex-1">
          <AdminHeader
            title="Ventas vs metas"
            subtitle="Reportes"
            onLogout={handleLogout}
            userEmail={user?.email ?? ""}
          />

          <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-6 pb-16 pt-4">
            <div className="hero-band">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-cyan-200/70">Reportes</p>
                <h2 className="text-2xl font-semibold text-white">Comparativo contra metas</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-200/80">
                  Progreso de ventas reales frente a objetivos definidos.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <AdminActionButton icon={<RefreshIcon />} variant="ghost" onClick={handleRefresh}>
                  Actualizar
                </AdminActionButton>
              </div>
            </div>

            {error && (
              <div className="glass-card border border-rose-500/40 bg-rose-500/10 px-4 py-3 text-sm text-rose-100">
                {error}
              </div>
            )}

            <div className="glass-panel p-5">
              <div className="flex flex-wrap items-center gap-3">
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
            </div>

            <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
              <ReportChart points={chartPoints} valueLabel="Unidades vendidas" />
              <div className="glass-panel p-5">
                <h3 className="text-lg font-semibold text-white">KPIs</h3>
                <div className="mt-4 grid gap-3">
                  <div className="glass-card px-4 py-3">
                    <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Metas</p>
                    <p className="text-lg text-slate-100">{report?.totalTargets || 0}</p>
                  </div>
                  <div className="glass-card px-4 py-3">
                    <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Cumplidas</p>
                    <p className="text-lg text-slate-100">{report?.achievedTargets || 0}</p>
                  </div>
                  <div className="glass-card px-4 py-3">
                    <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Unidades</p>
                    <p className="text-lg text-slate-100">{report?.totalActualUnits || 0}</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="glass-panel p-5">
              <h3 className="text-lg font-semibold text-white">Detalle por producto</h3>
              {report?.items.length ? (
                <div className="mt-4 overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead className="text-xs uppercase tracking-[0.3em] text-slate-400">
                      <tr>
                        <th className="px-3 py-2 text-left">Producto</th>
                        <th className="px-3 py-2 text-right">Meta</th>
                        <th className="px-3 py-2 text-right">Real</th>
                        <th className="px-3 py-2 text-right">Var</th>
                        <th className="px-3 py-2 text-right">Precio</th>
                        <th className="px-3 py-2 text-right">Deadline</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {report.items.map((item) => (
                        <tr key={item.targetId}>
                          <td className="px-3 py-2">{item.softwareName}</td>
                          <td className="px-3 py-2 text-right">{formatNumber(item.metaUnits)}</td>
                          <td className="px-3 py-2 text-right">{formatNumber(item.actualUnits)}</td>
                          <td className="px-3 py-2 text-right">
                            <span className={item.varianceUnits >= 0 ? "text-emerald-300" : "text-rose-300"}>
                              {formatNumber(item.varianceUnits)}
                            </span>
                          </td>
                          <td className="px-3 py-2 text-right">
                            {formatCurrency(item.targetSalePrice)}
                          </td>
                          <td className="px-3 py-2 text-right">
                            {item.deadline ? formatDate(item.deadline) : "-"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="mt-3 text-sm text-slate-400">No hay metas en el rango.</p>
              )}
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
