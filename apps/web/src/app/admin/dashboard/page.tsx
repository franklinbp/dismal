"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency, formatDate, formatDateTime, formatNumber } from "@/lib/format";
import { DownloadIcon, PlusIcon, RefreshIcon, SparkIcon, UsersIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type DashboardSummary = {
  todaySalesCount: number;
  todaySalesAmount: number;
  todayProfitAmount: number;
  monthSalesCount: number;
  monthSalesAmount: number;
  monthProfitAmount: number;
  monthExpectedProfit: number;
  overdueArCount: number;
  overdueArBalance: number;
  openArCount: number;
  openArBalance: number;
  nonProfitableProductsCount: number;
  outboxFailedCount: number;
};

type RecentSale = {
  id: string;
  date: string;
  clientName: string;
  clientEmail: string;
  saleType: string;
  status: string;
  total: number;
  paid: number;
  balance: number;
};

type ArSaleDetail = {
  accountsReceivableId: string;
  saleId: string;
  products: string;
  total: number;
  paid: number;
  balance: number;
  dueDate: string;
  status: string;
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

type MarketingAnalysisResult = {
  productId: string;
  productName: string;
  state: string;
  diagnosis: string;
  priority: string;
  suggestedActions: string[];
  explanation: string;
};

const defaultPage: PageResponse<unknown> = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

export default function AdminDashboardPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [loading, setLoading] = useState(true);
  const [secondaryLoading, setSecondaryLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [recentSales, setRecentSales] = useState<PageResponse<RecentSale>>(defaultPage as PageResponse<RecentSale>);
  const [arOverdue, setArOverdue] = useState<ArClientSummary[]>([]);
  const [arOpen, setArOpen] = useState<ArClientSummary[]>([]);
  const [targets, setTargets] = useState<PageResponse<SalesTarget>>(defaultPage as PageResponse<SalesTarget>);
  const [mipAlerts, setMipAlerts] = useState<MarketingAnalysisResult[]>([]);
  const [monthlySales, setMonthlySales] = useState<Record<string, number>>({});
  const [expandedAr, setExpandedAr] = useState<Record<string, boolean>>({});
  const [refreshing, setRefreshing] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const loadSecondaryData = useCallback(async () => {
    setSecondaryLoading(true);
    const [overdueResult, openResult, mipResult] = await Promise.allSettled([
      apiFetch<ArClientSummary[]>("/api/backend/api/v1/ar/grouped?status=OVERDUE"),
      apiFetch<ArClientSummary[]>("/api/backend/api/v1/ar/grouped?status=OPEN"),
      apiFetch<MarketingAnalysisResult[]>("/api/backend/api/v1/marketing/intelligence/analysis"),
    ]);

    if (overdueResult.status === "fulfilled") {
      setArOverdue(overdueResult.value);
    }
    if (openResult.status === "fulfilled") {
      setArOpen(openResult.value);
    }
    if (mipResult.status === "fulfilled") {
      setMipAlerts(mipResult.value.filter((a) => a.priority === "ALTA" || a.state === "ALERTA_COMERCIAL"));
    }
    setSecondaryLoading(false);
  }, []);

  const loadDashboard = useCallback(async (showLoader = false) => {
    if (showLoader) {
      setLoading(true);
    } else {
      setRefreshing(true);
    }
    setError(null);
    try {
      const [summaryData, salesData, targetData, monthlySalesData] = await Promise.all([
        apiFetch<DashboardSummary>("/api/backend/api/v1/dashboard/admin/summary"),
        apiFetch<PageResponse<RecentSale>>("/api/backend/api/v1/dashboard/admin/recent-sales?page=0&size=10"),
        apiFetch<PageResponse<SalesTarget>>("/api/backend/api/v1/dashboard/admin/sales-targets?sort=profit&page=0&size=10"),
        apiFetch<Record<string, number>>(
          `/api/backend/api/v1/dashboard/admin/monthly-sales?year=${new Date().getFullYear()}`
        ),
      ]);
      setSummary(summaryData);
      setRecentSales(salesData);
      setTargets(targetData);
      setMonthlySales(monthlySalesData);
      setLoading(false);
      setRefreshing(false);
      void loadSecondaryData();
    } catch (err: any) {
      if (err?.status === 401) {
        router.push("/login");
        return;
      }
      setError(err?.message || "No se pudo cargar el dashboard.");
      setLoading(false);
      setRefreshing(false);
      setSecondaryLoading(false);
    }
  }, [loadSecondaryData, router]);

  useEffect(() => {
    if (!allowed) {
      return;
    }
    loadDashboard(true).catch(() => null);
  }, [allowed, loadDashboard]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleOpenSales = () => {
    router.push("/admin/sales");
  };

  const handleCreateCustomerSale = () => {
    router.push("/admin/sales?intent=create-customer");
  };

  const handleRefresh = async () => {
    await loadDashboard(false);
    setNotice("Dashboard actualizado.");
    setTimeout(() => setNotice(null), 2200);
  };

  const handleExport = () => {
    const summaryRows = summary
      ? [
          ["Ventas hoy", String(summary.todaySalesCount), formatCurrency(summary.todaySalesAmount)],
          ["Utilidad hoy", formatCurrency(summary.todayProfitAmount), ""],
          ["Ventas mes", String(summary.monthSalesCount), formatCurrency(summary.monthSalesAmount)],
          ["Utilidad mes", formatCurrency(summary.monthProfitAmount), ""],
          ["Ganancia esperada", formatCurrency(summary.monthExpectedProfit), ""],
          ["AR vencido", String(summary.overdueArCount), formatCurrency(summary.overdueArBalance)],
          ["AR abierto", String(summary.openArCount), formatCurrency(summary.openArBalance)],
          ["No rentables", String(summary.nonProfitableProductsCount), ""],
        ]
      : [];

    const lines = [
      ["Seccion", "Campo 1", "Campo 2", "Campo 3"].join(","),
      ...summaryRows.map((row) => ["Resumen", ...row].map((value) => `"${String(value ?? "").replace(/"/g, '""')}"`).join(",")),
      ...recentSales.content.map((sale) =>
        [
          "Ventas recientes",
          sale.id,
          sale.clientName,
          `Total ${formatCurrency(sale.total)} | Pagado ${formatCurrency(sale.paid)} | Pendiente ${formatCurrency(sale.balance)} | ${sale.status}`,
        ]
          .map((value) => `"${String(value ?? "").replace(/"/g, '""')}"`)
          .join(",")
      ),
      ...targets.content.map((target) =>
        ["Metas", target.productName, `${target.unitsSoldCurrent}/${target.metaUnits}`, formatCurrency(target.expectedProfit)]
          .map((value) => `"${String(value ?? "").replace(/"/g, '""')}"`)
          .join(",")
      ),
    ];

    const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `dashboard-dismal-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    setNotice("Exportacion generada.");
    setTimeout(() => setNotice(null), 2200);
  };

  const monthlyEntries = useMemo(
    () =>
      Object.entries(monthlySales).map(([month, total]) => ({
        month,
        label: month.slice(5),
        value: Number(total || 0),
      })),
    [monthlySales]
  );
  const maxMonthlySales = Math.max(...monthlyEntries.map((item) => item.value), 1);
  const currentMonthKey = `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, "0")}`;
  const currentMonthValue = monthlyEntries.find((item) => item.month === currentMonthKey)?.value ?? 0;
  const previousMonthValue =
    monthlyEntries[Math.max(0, monthlyEntries.findIndex((item) => item.month === currentMonthKey) - 1)]?.value ?? 0;
  const monthlyTrend = currentMonthValue - previousMonthValue;
  const targetStats = useMemo(() => {
    const total = targets.content.length;
    const achieved = targets.content.filter((item) => item.targetAchieved).length;
    const sold = targets.content.reduce((sum, item) => sum + Number(item.unitsSoldCurrent || 0), 0);
    const goal = targets.content.reduce((sum, item) => sum + Number(item.metaUnits || 0), 0);
    const expectedProfit = targets.content.reduce((sum, item) => sum + Number(item.expectedProfit || 0), 0);
    const progress = goal > 0 ? Math.min(100, Math.round((sold / goal) * 100)) : 0;
    return { total, achieved, sold, goal, expectedProfit, progress };
  }, [targets.content]);

  if (authLoading || loading) {
    return (
      <div className="min-h-screen grid place-items-center">
        <div className="rounded-3xl bg-white/70 px-6 py-4 shadow-panel">Cargando dashboard...</div>
      </div>
    );
  }

  if (!user || !allowed) {
    return null;
  }

  return (
    <>
      <div className="glass-panel hero-band rounded-[28px] px-5 py-5 fade-up">
        <AdminHeader
          title="Panel de Metas y Rentabilidad"
          subtitle="Dashboard"
          userEmail={user.email}
          onLogout={handleLogout}
          actions={
            <>
              <AdminActionButton
                icon={<PlusIcon className="h-4 w-4" />}
                label="Nueva venta"
                variant="primary"
                onClick={handleOpenSales}
              />
              <AdminActionButton
                icon={<UsersIcon className="h-4 w-4" />}
                label="Crear nuevo cliente"
                onClick={handleCreateCustomerSale}
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
        <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
      {notice && (
        <div className="mt-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
          {notice}
        </div>
      )}

      <section className="mt-6 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {[
          {
            label: "Ventas hoy",
            value: summary ? formatNumber(summary.todaySalesCount) : "-",
            sub: summary ? formatCurrency(summary.todaySalesAmount) : "-",
            icon: <SparkIcon className="h-4 w-4" />,
          },
          {
            label: "Ventas mes",
            value: summary ? formatNumber(summary.monthSalesCount) : "-",
            sub: summary ? formatCurrency(summary.monthSalesAmount) : "-",
            icon: <SparkIcon className="h-4 w-4" />,
          },
          {
            label: "Utilidad hoy",
            value: summary ? formatCurrency(Math.abs(summary.todayProfitAmount)) : "-",
            sub: "Mes actual",
            icon: <SparkIcon className="h-4 w-4" />,
          },
          {
            label: "Utilidad mes",
            value: summary ? formatCurrency(Math.abs(summary.monthProfitAmount)) : "-",
            sub: "Mes actual",
            icon: <SparkIcon className="h-4 w-4" />,
          },
          {
            label: "Ganancia esperada",
            value: summary ? formatCurrency(summary.monthExpectedProfit) : "-",
            sub: "Mes actual",
            icon: <SparkIcon className="h-4 w-4" />,
          },
          {
            label: "AR vencido",
            value: summary ? formatNumber(arOverdue.length || summary.overdueArCount) : "-",
            sub: summary ? formatCurrency(summary.overdueArBalance) : "-",
            icon: <SparkIcon className="h-4 w-4" />,
          },
          {
            label: "AR por vencer",
            value: summary ? formatNumber(arOpen.length || summary.openArCount) : "-",
            sub: summary ? formatCurrency(summary.openArBalance) : "-",
            icon: <SparkIcon className="h-4 w-4" />,
          },
          {
            label: "Productos no rentables",
            value: summary ? formatNumber(summary.nonProfitableProductsCount) : "-",
            sub: "Metas activas",
            icon: <SparkIcon className="h-4 w-4" />,
          },
        ].map((card) => (
          <div
            key={card.label}
            className="glass-card fade-up delay-1 rounded-[26px] px-5 py-4"
          >
            <div className="flex items-center justify-between text-xs uppercase tracking-[0.2em] text-slate/50">
              <span>{card.label}</span>
              <span className="text-accentDeep">{card.icon}</span>
            </div>
            <div className="mt-2.5 flex items-end justify-between gap-3">
              <span className="font-display text-[2.2rem] leading-none text-slate">{card.value}</span>
              <span className="text-xs text-slate/60">{card.sub}</span>
            </div>
          </div>
        ))}
      </section>

      <section className="mt-6 glass-panel rounded-[28px] p-5 fade-up delay-1">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <h2 className="font-display text-xl text-slate">Ventas mensuales (año fiscal)</h2>
          <span className="text-xs uppercase tracking-[0.2em] text-slate/50">
            {new Date().getFullYear()}
          </span>
        </div>
        <div className="mt-3 rounded-2xl border border-smoke/60 bg-white/70 p-4">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-3 text-sm text-slate/70">
            <span>Mes actual {formatCurrency(currentMonthValue)}</span>
            <span className={monthlyTrend >= 0 ? "text-emerald-600" : "text-red-600"}>
              {monthlyTrend >= 0 ? "Avance" : "Caida"} vs mes anterior: {formatCurrency(Math.abs(monthlyTrend))}
            </span>
          </div>
          <div className="grid h-44 grid-cols-12 items-end gap-2 border-b border-smoke/60 pb-3">
          {monthlyEntries.map(({ month, label, value }) => {
            const height = Math.max(8, Math.round((value / maxMonthlySales) * 160));
            const isCurrent = month === currentMonthKey;
            return (
              <div key={month} className="group flex h-full flex-col items-center justify-end gap-2">
                <span className="rounded-lg bg-slate px-2 py-1 text-[10px] text-white opacity-0 shadow-sm transition group-hover:opacity-100">
                  {formatCurrency(value)}
                </span>
                <div
                  className={`w-full max-w-[28px] rounded-t-2xl transition ${
                    isCurrent ? "bg-accent" : value > 0 ? "bg-slate/80" : "bg-smoke"
                  }`}
                  style={{ height }}
                  title={`${month}: ${formatCurrency(value)}`}
                />
                <span className="text-[10px] text-slate/50">{label}</span>
              </div>
            );
          })}
          </div>
        </div>
      </section>

      {(secondaryLoading || mipAlerts.length > 0) && (
        <section className="mt-8">
          <div className="glass-panel border-amber-100 bg-amber-50/30 rounded-[28px] p-5 fade-up">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-xl text-slate flex items-center gap-2">
                <SparkIcon className="h-5 w-5 text-amber-500" />
                Alertas Comerciales Inteligentes (MIP)
              </h2>
              <button
                onClick={() => router.push("/admin/marketing/mip")}
                className="text-xs uppercase tracking-[0.2em] text-accentDeep"
              >
                Ver Analisis Completo
              </button>
            </div>
            {secondaryLoading && mipAlerts.length === 0 ? (
              <div className="mt-4 rounded-2xl border border-amber-100 bg-white/80 px-4 py-3 text-sm text-slate/60">
                Analizando alertas comerciales...
              </div>
            ) : (
            <div className="mt-4 grid gap-3 md:grid-cols-2">
              {mipAlerts.slice(0, 4).map((alert) => (
                <div key={alert.productId} className="rounded-2xl border border-amber-100 bg-white p-4 shadow-sm">
                  <div className="flex justify-between items-start">
                    <span className="font-bold text-slate-900">{alert.productName}</span>
                    <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700 border border-red-200 font-bold uppercase">
                      {alert.priority}
                    </span>
                  </div>
                  <p className="mt-2 text-xs text-slate-600 line-clamp-2 italic">
                    &quot;{alert.explanation}&quot;
                  </p>
                  <div className="mt-3 flex gap-2">
                    <span className="text-[10px] font-bold text-amber-600 uppercase tracking-tight">
                      {alert.diagnosis.replace("PROBLEMA_", "PROBLEMA DE ").replace("_", " ")}
                    </span>
                  </div>
                </div>
              ))}
            </div>
            )}
          </div>
        </section>
      )}

      <section className="mt-8 grid gap-5 xl:grid-cols-2">
        <div className="glass-panel fade-up delay-2 rounded-[28px] p-5">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-xl text-slate">Ventas recientes</h2>
            <button
              onClick={() => router.push("/admin/sales")}
              className="text-xs uppercase tracking-[0.2em] text-accentDeep"
            >
              Detalle
            </button>
          </div>
          <div className="mt-4 overflow-auto">
            <table className="min-w-full text-sm">
              <thead className="text-xs uppercase text-slate/60">
                <tr>
                  <th className="py-2 text-left">Cliente</th>
                  <th className="py-2 text-left">Tipo</th>
                  <th className="py-2 text-left">Estado</th>
                  <th className="py-2 text-right">Comprado</th>
                  <th className="py-2 text-right">Pagado</th>
                  <th className="py-2 text-right">Pendiente</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-smoke/60">
                {recentSales.content.map((sale) => (
                  <tr key={sale.id} className="text-slate/80 hover:bg-paper/60">
                    <td className="py-2.5">
                      <div className="font-medium text-slate">{sale.clientName || "-"}</div>
                      <div className="text-xs text-slate/60">{sale.clientEmail}</div>
                      <div className="text-xs text-slate/50">{formatDateTime(sale.date)}</div>
                    </td>
                    <td className="py-2.5 text-xs font-mono text-slate/70">{sale.saleType}</td>
                    <td className="py-2.5">
                      <span className="status-pill rounded-full px-2 py-1 text-xs text-slate/70">
                        {sale.status}
                      </span>
                    </td>
                    <td className="py-2.5 text-right font-medium text-slate">{formatCurrency(sale.total)}</td>
                    <td className="py-2.5 text-right text-emerald-700">{formatCurrency(sale.paid)}</td>
                    <td className="py-2.5 text-right text-slate">
                      <span className={sale.balance > 0 ? "font-medium text-amber-700" : "text-slate/50"}>
                        {formatCurrency(sale.balance)}
                      </span>
                    </td>
                  </tr>
                ))}
                {recentSales.content.length === 0 && (
                  <tr>
                    <td className="py-6 text-center text-sm text-slate/60" colSpan={6}>
                      Sin ventas recientes.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="glass-panel fade-up delay-2 rounded-[28px] p-5">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-xl text-slate">Avance de metas</h2>
            <button
              onClick={() => router.push("/admin/planning/targets")}
              className="text-xs uppercase tracking-[0.2em] text-accentDeep"
            >
              Detalle
            </button>
          </div>
          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            <div className="rounded-2xl bg-white/80 p-3.5">
              <span className="text-xs uppercase tracking-[0.18em] text-slate/45">Progreso</span>
              <p className="mt-1.5 font-display text-[2rem] text-slate">{targetStats.progress}%</p>
            </div>
            <div className="rounded-2xl bg-white/80 p-3.5">
              <span className="text-xs uppercase tracking-[0.18em] text-slate/45">Vendidas</span>
              <p className="mt-1.5 font-display text-[2rem] text-slate">{formatNumber(targetStats.sold)}</p>
            </div>
            <div className="rounded-2xl bg-white/80 p-3.5">
              <span className="text-xs uppercase tracking-[0.18em] text-slate/45">Ganancia</span>
              <p className="mt-1.5 font-display text-[1.7rem] text-slate">{formatCurrency(targetStats.expectedProfit)}</p>
            </div>
          </div>
          <div className="mt-5">
            <div className="h-4 overflow-hidden rounded-full bg-smoke">
              <div className="h-full rounded-full bg-accent" style={{ width: `${targetStats.progress}%` }} />
            </div>
            <div className="mt-2 flex justify-between text-xs text-slate/60">
              <span>{formatNumber(targetStats.sold)} vendidas</span>
              <span>{formatNumber(targetStats.goal)} meta total</span>
            </div>
          </div>
          <p className="mt-4 text-sm text-slate/60">
            {targetStats.achieved} de {targetStats.total} metas cumplidas. Usa este bloque para saber si el mes va
            avanzando o si hay productos que necesitan impulso comercial.
          </p>
        </div>
      </section>

      <section className="mt-8 grid gap-5 xl:grid-cols-2">
        <div className="glass-panel fade-up delay-3 rounded-[28px] p-5">
          <h2 className="font-display text-xl text-slate">AR vencido</h2>
          <div className="mt-3 space-y-3">
            {secondaryLoading && arOverdue.length === 0 && (
              <div className="rounded-2xl border border-dashed border-smoke/60 p-5 text-center text-sm text-slate/60">
                Cargando cuentas vencidas...
              </div>
            )}
            {arOverdue.slice(0, 8).map((item) => {
              const isOpen = Boolean(expandedAr[item.clientId]);
              return (
              <div key={item.clientId} className="rounded-2xl bg-smoke/30 p-3.5 soft-shadow">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-medium text-slate">{item.clientName || "-"}</p>
                    <p className="text-xs text-slate/60">{item.clientEmail}</p>
                    <p className="text-xs text-slate/50">{item.salesCount} ventas pendientes</p>
                  </div>
                  <div className="text-right text-sm text-slate">
                    {formatCurrency(item.balance)}
                    <div className="text-xs text-slate/50">Vence {formatDate(item.oldestDueDate || item.nextDueDate || "")}</div>
                    <button
                      type="button"
                      className="mt-2 rounded-full border border-smoke/70 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.16em] text-slate"
                      onClick={() => setExpandedAr((prev) => ({ ...prev, [item.clientId]: !prev[item.clientId] }))}
                    >
                      {isOpen ? "Ocultar" : "Detalle"}
                    </button>
                  </div>
                </div>
                {isOpen && (
                  <div className="mt-3 divide-y divide-smoke/60 rounded-2xl border border-smoke/60 bg-white">
                    {item.sales.map((sale) => (
                      <div key={sale.accountsReceivableId} className="grid gap-2 p-2.5 text-xs md:grid-cols-[1fr_auto]">
                        <div>
                          <p className="font-medium text-slate">{sale.products || `Venta ${sale.saleId}`}</p>
                          <p className="text-slate/60">Vence {formatDate(sale.dueDate)}</p>
                        </div>
                        <div className="text-right">
                          <p>Saldo {formatCurrency(sale.balance)}</p>
                          <p className="text-slate/60">Pagado {formatCurrency(sale.paid)}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
              );
            })}
            {!secondaryLoading && arOverdue.length === 0 && (
              <div className="rounded-2xl border border-dashed border-smoke/60 p-6 text-center text-sm text-slate/60">
                Sin cuentas vencidas.
              </div>
            )}
          </div>
        </div>

        <div className="glass-panel fade-up delay-3 rounded-[28px] p-5">
          <h2 className="font-display text-xl text-slate">AR por vencer</h2>
          <div className="mt-3 space-y-3">
            {secondaryLoading && arOpen.length === 0 && (
              <div className="rounded-2xl border border-dashed border-smoke/60 p-5 text-center text-sm text-slate/60">
                Cargando cuentas por vencer...
              </div>
            )}
            {arOpen.slice(0, 8).map((item) => {
              const isOpen = Boolean(expandedAr[item.clientId]);
              return (
              <div key={item.clientId} className="rounded-2xl bg-smoke/30 p-3.5 soft-shadow">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-medium text-slate">{item.clientName || "-"}</p>
                    <p className="text-xs text-slate/60">{item.clientEmail}</p>
                    <p className="text-xs text-slate/50">{item.salesCount} ventas pendientes</p>
                  </div>
                  <div className="text-right text-sm text-slate">
                    {formatCurrency(item.balance)}
                    <div className="text-xs text-slate/50">Vence {formatDate(item.nextDueDate || item.oldestDueDate || "")}</div>
                    <button
                      type="button"
                      className="mt-2 rounded-full border border-smoke/70 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.16em] text-slate"
                      onClick={() => setExpandedAr((prev) => ({ ...prev, [item.clientId]: !prev[item.clientId] }))}
                    >
                      {isOpen ? "Ocultar" : "Detalle"}
                    </button>
                  </div>
                </div>
                {isOpen && (
                  <div className="mt-3 divide-y divide-smoke/60 rounded-2xl border border-smoke/60 bg-white">
                    {item.sales.map((sale) => (
                      <div key={sale.accountsReceivableId} className="grid gap-2 p-2.5 text-xs md:grid-cols-[1fr_auto]">
                        <div>
                          <p className="font-medium text-slate">{sale.products || `Venta ${sale.saleId}`}</p>
                          <p className="text-slate/60">Vence {formatDate(sale.dueDate)}</p>
                        </div>
                        <div className="text-right">
                          <p>Saldo {formatCurrency(sale.balance)}</p>
                          <p className="text-slate/60">Pagado {formatCurrency(sale.paid)}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
              );
            })}
            {!secondaryLoading && arOpen.length === 0 && (
              <div className="rounded-2xl border border-dashed border-smoke/60 p-6 text-center text-sm text-slate/60">
                Sin cuentas por vencer.
              </div>
            )}
          </div>
        </div>
      </section>

      <section className="mt-8 rounded-[28px] glass-panel fade-up delay-3 p-5">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-xl text-slate">Metas de ventas</h2>
          <button
            onClick={() => router.push("/admin/planning/targets")}
            className="text-xs uppercase tracking-[0.2em] text-accentDeep"
          >
            Detalle
          </button>
        </div>
        <div className="mt-4 grid gap-4 lg:grid-cols-[300px_1fr]">
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/45">Avance global</p>
            <div className="mt-3 flex items-end justify-between">
              <span className="font-display text-[2.3rem] text-slate">{targetStats.progress}%</span>
              <span className="text-sm text-slate/60">
                {formatNumber(targetStats.sold)} / {formatNumber(targetStats.goal)}
              </span>
            </div>
            <div className="mt-3 h-3.5 overflow-hidden rounded-full bg-smoke">
              <div className="h-full rounded-full bg-accent" style={{ width: `${targetStats.progress}%` }} />
            </div>
            <p className="mt-2.5 text-xs text-slate/60">
              {targetStats.achieved} metas cumplidas de {targetStats.total}. Ganancia proyectada:{" "}
              {formatCurrency(targetStats.expectedProfit)}.
            </p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/45">Productos con mayor impacto</p>
            <div className="mt-3 space-y-2.5">
              {targets.content.slice(0, 5).map((item) => {
                const progress = item.metaUnits > 0 ? Math.min(100, Math.round((item.unitsSoldCurrent / item.metaUnits) * 100)) : 0;
                return (
                  <div key={item.id}>
                    <div className="mb-1 flex justify-between gap-3 text-xs">
                      <span className="truncate text-slate">{item.productName}</span>
                      <span className={item.expectedProfit >= 0 ? "text-emerald-700" : "text-red-600"}>
                        {formatCurrency(item.expectedProfit)}
                      </span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-smoke">
                      <div className={item.targetAchieved ? "h-full bg-emerald-500" : "h-full bg-slate/80"} style={{ width: `${progress}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
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
                <tr key={item.id} className="text-slate/80">
                  <td className="py-2.5">
                    <div className="font-medium text-slate">{item.productName}</div>
                    <div className="mt-1 text-xs text-slate/50">
                      Punto de equilibrio: {item.breakEvenUnits ?? "-"} unidades · avance{" "}
                      {item.metaUnits > 0 ? Math.min(100, Math.round((item.unitsSoldCurrent / item.metaUnits) * 100)) : 0}%
                    </div>
                    <div className="mt-2 h-2 max-w-md overflow-hidden rounded-full bg-smoke">
                      <div
                        className={item.targetAchieved ? "h-full bg-emerald-500" : "h-full bg-accent"}
                        style={{
                          width: `${item.metaUnits > 0 ? Math.min(100, Math.round((item.unitsSoldCurrent / item.metaUnits) * 100)) : 0}%`,
                        }}
                      />
                    </div>
                  </td>
                  <td className="py-2.5 text-right font-medium">{formatNumber(item.metaUnits)}</td>
                  <td className="py-2.5 text-right">{formatNumber(item.unitsSoldCurrent)}</td>
                  <td className="py-2.5 text-right">{formatCurrency(item.marginUnit)}</td>
                  <td className="py-2.5 text-right">{formatCurrency(item.expectedProfit)}</td>
                  <td className="py-2.5 text-left">{formatDate(item.deadline)}</td>
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
    </>
  );
}
