"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { RefreshIcon, SparkIcon, ChevronIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type MarketingAnalysisResult = {
  productId: string;
  productName: string;
  state: string;
  diagnosis: string;
  priority: string;
  suggestedActions: string[];
  explanation: string;
  progressPercent?: number;
  expectedPacePercent?: number;
  behindSchedule?: boolean;
  recommendedAction?: string;
  reason?: string;
  metaUnits?: number;
  unitsSoldCurrent?: number;
  daysRemaining?: number;
};

type StrategyOverview = {
  activeTargets: number;
  achievedTargets: number;
  targetsBehindSchedule: number;
  highPriorityProducts: number;
  commercialAlerts: number;
  pausedProducts: number;
  scheduledCampaigns: number;
  failedCampaigns: number;
  openStrategyActions?: number;
  activeTargetRevenue: number;
  activeExpectedProfit: number;
  priorityItems: MarketingAnalysisResult[];
};

export default function MarketingMIPPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [analysis, setAnalysis] = useState<MarketingAnalysisResult[]>([]);
  const [selected, setSelected] = useState<MarketingAnalysisResult | null>(null);
  const [targetsCount, setTargetsCount] = useState(0);
  const [strategyOverview, setStrategyOverview] = useState<StrategyOverview | null>(null);
  const [creatingAction, setCreatingAction] = useState(false);

  const loadAnalysis = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [analysisData, targetsData] = await Promise.all([
        apiFetch<MarketingAnalysisResult[]>("/api/backend/api/v1/marketing/intelligence/analysis"),
        apiFetch<StrategyOverview>("/api/backend/api/v1/marketing/intelligence/strategy-overview"),
      ]);

      const activeTargetIds = new Set(targetsData.priorityItems.map((item) => item.productId));
      const detailsByProduct = new Map(analysisData.map((item) => [item.productId, item]));
      const filtered = targetsData.priorityItems.map((item) => {
        const detail = detailsByProduct.get(item.productId);
        return {
          ...(detail || {}),
          ...item,
          suggestedActions: detail?.suggestedActions || item.suggestedActions || [],
          explanation: item.reason || detail?.explanation || "",
          diagnosis: detail?.diagnosis || item.diagnosis || "OPTIMO",
        };
      });
      setTargetsCount(targetsData.activeTargets);
      setStrategyOverview(targetsData);
      setAnalysis(filtered);
      if (filtered.length > 0) {
        if (!selected || !activeTargetIds.has(selected.productId)) {
          setSelected(filtered[0]);
        }
      } else {
        setSelected(null);
      }
    } catch (err: any) {
      setError(err?.message || "No se pudo cargar el analisis MIP.");
    } finally {
      setLoading(false);
    }
  }, [selected]);

  useEffect(() => {
    if (allowed) {
      loadAnalysis();
    }
  }, [allowed, loadAnalysis]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleCreateStrategyAction = async () => {
    if (!selected) return;
    setCreatingAction(true);
    setError(null);
    try {
      await apiFetch("/api/backend/api/v1/marketing/strategy/actions", {
        method: "POST",
        body: JSON.stringify({
          productId: selected.productId,
          source: selected.behindSchedule ? "META_ATRASADA" : "MIP",
          priority: selected.priority || "MEDIA",
          recommendedChannel: "CAMPANA",
          title: `Impulsar ${selected.productName}`,
          description: selected.recommendedAction || selected.explanation || "Accion comercial creada desde MIP.",
          dueDate: new Date().toISOString().slice(0, 10),
        }),
      });
      await loadAnalysis();
    } catch (err: any) {
      setError(err?.message || "No se pudo crear la accion estrategica.");
    } finally {
      setCreatingAction(false);
    }
  };

  if (authLoading || (loading && analysis.length === 0)) {
    return (
      <div className="min-h-screen grid place-items-center">
        <div className="rounded-3xl bg-white/70 px-6 py-4 shadow-panel">Cargando Inteligencia MIP...</div>
      </div>
    );
  }

  if (!user || !allowed) {
    return null;
  }

  const getStateColor = (state: string) => {
    switch (state) {
      case "RENTABLE": return "text-emerald-600 bg-emerald-50 border-emerald-100";
      case "BAJA_TRACCION": return "text-amber-600 bg-amber-50 border-amber-100";
      case "ALERTA_COMERCIAL": return "text-red-600 bg-red-50 border-red-100";
      case "PAUSADO": return "text-slate-600 bg-slate-50 border-slate-100";
      default: return "text-slate-400 bg-slate-50 border-slate-100";
    }
  };

  const getStateLabel = (state: string) => {
    switch (state) {
      case "RENTABLE": return "Rentable";
      case "BAJA_TRACCION": return "Baja tracción";
      case "ALERTA_COMERCIAL": return "Alerta comercial";
      case "PAUSADO": return "Pausado";
      default: return "Sin señal clara";
    }
  };

  const getDiagnosisLabel = (diagnosis: string) => {
    switch (diagnosis) {
      case "PROBLEMA_VISIBILIDAD": return "Falta visibilidad";
      case "PROBLEMA_CONFIANZA": return "Falta confianza";
      case "PROBLEMA_PERCEPCION": return "Percepción del valor";
      case "PROBLEMA_COMPLEJIDAD": return "Demasiada complejidad";
      default: return "Óptimo";
    }
  };

  const getPriorityLabel = (priority: string) => {
    switch (priority) {
      case "ALTA": return "Alta";
      case "MEDIA": return "Media";
      case "BAJA": return "Baja";
      default: return "Sin urgencia";
    }
  };

  const getNextStep = (item: MarketingAnalysisResult) => {
    if (item.recommendedAction) return item.recommendedAction;
    if (item.priority === "ALTA") return "Actúa hoy: crea una campaña enfocada en este producto.";
    if (item.priority === "MEDIA") return "Planifica una acción esta semana para mejorar conversión.";
    if (item.state === "PAUSADO") return "Evalúa relanzamiento o ajuste de propuesta.";
    return "Mantén el seguimiento y revisa metas mensuales.";
  };

  const formatPercent = (value?: number) => `${Number(value || 0).toFixed(1)}%`;

  const summary = analysis.reduce(
    (acc, item) => {
      acc.total += 1;
      acc[item.state] = (acc[item.state] || 0) + 1;
      if (item.priority === "ALTA") acc.high += 1;
      if (item.state === "ALERTA_COMERCIAL") acc.alerts += 1;
      return acc;
    },
    { total: 0, high: 0, alerts: 0 } as Record<string, number>
  );

  const priorityList = analysis.filter((item) => item.priority === "ALTA" || item.state === "ALERTA_COMERCIAL");

  return (
    <div className="min-h-screen">
      <div className="px-6 py-6 border-b border-slate-200 bg-white/80">
        <AdminHeader
          title="Inteligencia MIP"
          subtitle="Alertas claras para impulsar ventas y alinear metas."
          userEmail={user.email}
          onLogout={handleLogout}
          actions={
            <div className="flex flex-wrap gap-2">
              <AdminActionButton
                icon={<SparkIcon className="h-4 w-4" />}
                label="Ir a Metas"
                onClick={() => router.push("/admin/planning/targets")}
              />
              <AdminActionButton
                icon={<RefreshIcon className="h-4 w-4" />}
                label="Recalcular"
                onClick={loadAnalysis}
                disabled={loading}
                variant="primary"
              />
            </div>
          }
        />
      </div>

      <main className="px-6 py-8 space-y-8">
        {error && (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {error}
          </div>
        )}

        <section className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
          <div className="glass-panel p-6 space-y-4">
            <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Resumen ejecutivo</p>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                <p className="text-xs uppercase tracking-[0.2em] text-slate/60">Productos analizados</p>
                <p className="mt-2 font-display text-3xl text-slate">{summary.total}</p>
              </div>
              <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                <p className="text-xs uppercase tracking-[0.2em] text-slate/60">Alertas activas</p>
                <p className="mt-2 font-display text-3xl text-slate">{summary.alerts}</p>
                <p className="mt-1 text-xs text-slate/60">Prioridad alta: {summary.high}</p>
              </div>
              <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                <p className="text-xs uppercase tracking-[0.2em] text-slate/60">Metas atrasadas</p>
                <p className="mt-2 font-display text-3xl text-slate">{strategyOverview?.targetsBehindSchedule ?? 0}</p>
                <p className="mt-1 text-xs text-slate/60">Activas: {strategyOverview?.activeTargets ?? targetsCount}</p>
              </div>
              <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                <p className="text-xs uppercase tracking-[0.2em] text-slate/60">Acciones abiertas</p>
                <p className="mt-2 font-display text-3xl text-slate">{strategyOverview?.openStrategyActions ?? 0}</p>
                <p className="mt-1 text-xs text-slate/60">Pendientes o en progreso</p>
              </div>
            </div>
            <div className="flex flex-wrap gap-2 text-xs text-slate/60">
              <span className="label-pill rounded-full px-3 py-1">Rentable: {summary.RENTABLE || 0}</span>
              <span className="label-pill rounded-full px-3 py-1">Baja tracción: {summary.BAJA_TRACCION || 0}</span>
              <span className="label-pill rounded-full px-3 py-1">Alerta comercial: {summary.ALERTA_COMERCIAL || 0}</span>
              <span className="label-pill rounded-full px-3 py-1">Pausado: {summary.PAUSADO || 0}</span>
            </div>
            <p className="text-sm text-slate/70">
              MIP muestra solo productos con metas activas en Planeación ({targetsCount}). Usa estas alertas para
              decidir campañas y recalibrar metas.
            </p>
          </div>

          <div className="glass-panel p-6 space-y-4">
            <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Cómo usar MIP</p>
            <div className="space-y-3 text-sm text-slate/70">
              <div className="flex items-start gap-3">
                <span className="status-pill rounded-full px-2 py-0.5 text-[11px] text-slate/70">1</span>
                <p>Prioriza los productos en “Alerta comercial” o “Prioridad alta”.</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="status-pill rounded-full px-2 py-0.5 text-[11px] text-slate/70">2</span>
                <p>Ejecuta la acción sugerida y registra el aprendizaje.</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="status-pill rounded-full px-2 py-0.5 text-[11px] text-slate/70">3</span>
                <p>Vincula el hallazgo con tus metas en Planeación.</p>
              </div>
            </div>
            <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4 text-xs text-slate/60">
              MIP no reemplaza tu criterio: es una guía rápida para tomar decisiones comerciales más claras.
            </div>
          </div>
        </section>

        <section className="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-xl text-slate">Alertas prioritarias</h2>
              <span className="text-xs text-slate/60">{priorityList.length} detectadas</span>
            </div>
            <div className="grid gap-3">
              {priorityList.length === 0 && targetsCount > 0 && (
                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4 text-sm text-slate/60">
                  No hay alertas críticas. Revisa el listado completo para oportunidades.
                </div>
              )}
              {targetsCount === 0 && (
                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4 text-sm text-slate/60">
                  No hay metas activas. Agrega metas en Planeación para habilitar Inteligencia MIP.
                </div>
              )}
              {priorityList.slice(0, 4).map((item) => (
                <button
                  key={item.productId}
                  onClick={() => setSelected(item)}
                  className={`w-full text-left rounded-2xl border p-4 transition-all ${
                    selected?.productId === item.productId
                      ? "border-accent bg-accent/5 shadow-panel"
                      : "border-smoke/60 bg-white/80 hover:border-slate-300"
                  }`}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <h3 className="font-display text-lg text-slate">{item.productName}</h3>
                      <p className="mt-1 text-sm text-slate/60">{getNextStep(item)}</p>
                      <div className="mt-3 flex flex-wrap gap-2">
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase border ${getStateColor(item.state)}`}>
                          {getStateLabel(item.state)}
                        </span>
                        <span className="text-xs text-slate/60">
                          Prioridad {getPriorityLabel(item.priority)}
                        </span>
                        {item.behindSchedule && (
                          <span className="text-xs text-red-600">
                            Atrasada: {formatPercent(item.progressPercent)} vs {formatPercent(item.expectedPacePercent)}
                          </span>
                        )}
                      </div>
                    </div>
                    <ChevronIcon className={`h-5 w-5 text-slate-300 transition-transform ${selected?.productId === item.productId ? "rotate-90 text-accent" : ""}`} />
                  </div>
                </button>
              ))}
            </div>

            <div className="pt-2">
              <h2 className="font-display text-xl text-slate">Listado completo</h2>
            </div>
            <div className="space-y-2">
              {analysis.map((item) => (
                <button
                  key={item.productId}
                  onClick={() => setSelected(item)}
                  className={`w-full text-left rounded-2xl border p-4 transition-all ${
                    selected?.productId === item.productId
                      ? "border-accent bg-accent/5 shadow-panel"
                      : "border-smoke/60 bg-white/80 hover:border-slate-300"
                  }`}
                >
                  <div className="flex justify-between items-start gap-4">
                    <div>
                      <h3 className="font-display text-lg text-slate">{item.productName}</h3>
                      <div className="mt-2 flex flex-wrap gap-2">
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase border ${getStateColor(item.state)}`}>
                          {getStateLabel(item.state)}
                        </span>
                        <span className="text-xs text-slate/60 flex items-center gap-1">
                          Prioridad {getPriorityLabel(item.priority)}
                        </span>
                      </div>
                      <p className="mt-2 text-xs text-slate/50">
                        Avance {item.unitsSoldCurrent ?? 0}/{item.metaUnits ?? 0} unidades · ritmo {formatPercent(item.expectedPacePercent)}
                      </p>
                    </div>
                    <ChevronIcon className={`h-5 w-5 text-slate-300 transition-transform ${selected?.productId === item.productId ? "rotate-90 text-accent" : ""}`} />
                  </div>
                </button>
              ))}

              {analysis.length === 0 && !loading && targetsCount === 0 && (
                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4 text-sm text-slate/60">
                  No hay metas activas. Agrega metas en Planeación para ver diagnóstico MIP.
                </div>
              )}
              {analysis.length === 0 && !loading && targetsCount > 0 && (
                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4 text-sm text-slate/60">
                  No hay productos analizados. Sincroniza las ventas primero.
                </div>
              )}
            </div>
          </div>

          <div className="glass-panel p-6 space-y-6">
            {selected ? (
              <>
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-xs uppercase tracking-[0.3em] text-slate/50">Diagnóstico MIP</p>
                    <h2 className="mt-2 font-display text-2xl text-slate">{selected.productName}</h2>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase border ${getStateColor(selected.state)}`}>
                      {getStateLabel(selected.state)}
                    </span>
                    <span className="text-xs text-slate/60">
                      {getPriorityLabel(selected.priority)}
                    </span>
                  </div>
                </div>

                <div className="grid gap-3 sm:grid-cols-3">
                  <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                    <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Avance</p>
                    <p className="mt-2 text-xl font-semibold text-slate">{formatPercent(selected.progressPercent)}</p>
                    <p className="text-xs text-slate/50">{selected.unitsSoldCurrent ?? 0}/{selected.metaUnits ?? 0} unidades</p>
                  </div>
                  <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                    <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Ritmo esperado</p>
                    <p className="mt-2 text-xl font-semibold text-slate">{formatPercent(selected.expectedPacePercent)}</p>
                    <p className="text-xs text-slate/50">{selected.behindSchedule ? "Requiere accion" : "Dentro del ritmo"}</p>
                  </div>
                  <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                    <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Dias restantes</p>
                    <p className="mt-2 text-xl font-semibold text-slate">{selected.daysRemaining ?? 0}</p>
                    <p className="text-xs text-slate/50">Segun deadline activo</p>
                  </div>
                </div>

                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-5 text-sm text-slate/70">
                  “{selected.explanation}”
                </div>

                <div>
                  <h4 className="text-sm font-semibold text-slate">Causa principal</h4>
                  <div className="mt-2 inline-flex items-center gap-2 rounded-2xl bg-amber-50 text-amber-700 border border-amber-100 px-4 py-2 text-sm font-medium">
                    <SparkIcon className="h-4 w-4" />
                    {getDiagnosisLabel(selected.diagnosis)}
                  </div>
                </div>

                <div>
                  <h4 className="text-sm font-semibold text-slate">Acciones recomendadas</h4>
                  <div className="mt-3 grid gap-3">
                    {selected.suggestedActions.map((action, i) => (
                      <div key={i} className="flex items-center gap-4 rounded-2xl border border-smoke/60 bg-white/80 p-4 text-sm text-slate/70">
                        <div className="h-8 w-8 rounded-full border border-smoke/60 bg-white grid place-items-center text-xs font-semibold text-slate/60">
                          {i + 1}
                        </div>
                        <span className="font-medium">{action}</span>
                      </div>
                    ))}
                    {selected.suggestedActions.length === 0 && (
                      <p className="text-sm text-slate/60">No se requieren acciones inmediatas.</p>
                    )}
                  </div>
                </div>

                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4 text-sm text-slate/70">
                  <p className="font-semibold text-slate">Conexión con metas</p>
                  <p className="mt-2">
                    Usa este diagnóstico para ajustar metas de venta, revisa márgenes esperados y prioriza campañas.
                  </p>
                  <div className="mt-4 grid gap-2 sm:grid-cols-2">
                    <button
                      onClick={handleCreateStrategyAction}
                      disabled={creatingAction}
                      className="btn btn-secondary w-full"
                    >
                      {creatingAction ? "Creando accion" : "Crear accion estrategica"}
                    </button>
                    <button
                      onClick={() => router.push("/admin/planning/targets")}
                      className="btn btn-secondary w-full"
                    >
                      Ir a Planeación y Metas
                    </button>
                    <button
                      onClick={() => router.push("/admin/marketing/campaigns")}
                      className="btn btn-primary w-full sm:col-span-2"
                    >
                      Crear campaña ahora
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="grid place-items-center text-slate/50 py-16 text-center">
                <div>
                  <SparkIcon className="h-16 w-16 mx-auto mb-4 opacity-20" />
                  <p className="text-lg">Selecciona un producto para ver el diagnóstico y las acciones.</p>
                </div>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
