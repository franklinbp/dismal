"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import ReportChart from "@/components/admin/ReportChart";
import { formatCurrency, formatDate } from "@/lib/format";
import { RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type ReportPoint = {
  period: string;
  total: number;
  count: number;
};

type SalesReport = {
  from: string;
  to: string;
  period: string;
  totalAmount: number;
  totalCount: number;
  points: ReportPoint[];
};

export default function SalesWeeklyReportPage() {
  const router = useRouter();
  const { user, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [report, setReport] = useState<SalesReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState({
    from: "",
    to: "",
  });

  const chartPoints = useMemo(() => {
    if (!report) return [];
    return report.points.map((point) => ({
      label: formatDate(point.period),
      value: point.total,
    }));
  }, [report]);

  const loadReport = useCallback(async () => {
    const params = new URLSearchParams();
    if (filters.from) params.set("from", filters.from);
    if (filters.to) params.set("to", filters.to);
    const data = await apiFetch<SalesReport>(
      `/api/backend/api/v1/reports/sales/weekly?${params.toString()}`
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
            title="Ventas semanales"
            subtitle="Reportes"
            onLogout={handleLogout}
            userEmail={user?.email ?? ""}
          />

          <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-6 pb-16 pt-4">
            <div className="hero-band">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-cyan-200/70">Reportes</p>
                <h2 className="text-2xl font-semibold text-white">Rendimiento semanal</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-200/80">
                  Agregados por semana para detectar picos y tendencias.
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
              <ReportChart points={chartPoints} valueLabel="Ventas" />
              <div className="glass-panel p-5">
                <h3 className="text-lg font-semibold text-white">KPIs</h3>
                <div className="mt-4 grid gap-3">
                  <div className="glass-card px-4 py-3">
                    <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Total ventas</p>
                    <p className="text-lg text-slate-100">
                      {formatCurrency(report?.totalAmount || 0)}
                    </p>
                  </div>
                  <div className="glass-card px-4 py-3">
                    <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Ordenes</p>
                    <p className="text-lg text-slate-100">{report?.totalCount || 0}</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="glass-panel p-5">
              <h3 className="text-lg font-semibold text-white">Detalle semanal</h3>
              {report?.points.length ? (
                <div className="mt-4 overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead className="text-xs uppercase tracking-[0.3em] text-slate-400">
                      <tr>
                        <th className="px-3 py-2 text-left">Semana</th>
                        <th className="px-3 py-2 text-right">Total</th>
                        <th className="px-3 py-2 text-right">Ventas</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {report.points.map((point) => (
                        <tr key={point.period}>
                          <td className="px-3 py-2">{formatDate(point.period)}</td>
                          <td className="px-3 py-2 text-right">{formatCurrency(point.total)}</td>
                          <td className="px-3 py-2 text-right">{point.count}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="mt-3 text-sm text-slate-400">No hay datos en el rango.</p>
              )}
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
