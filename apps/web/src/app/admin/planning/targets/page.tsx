"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency, formatDate } from "@/lib/format";
import { PlusIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type SalesTarget = {
  id: string;
  softwareId: string;
  softwareName: string;
  status: "ACTIVE" | "CLOSED";
  periodStart: string | null;
  periodEnd: string | null;
  closedAt: string | null;
  metaUnits: number;
  salePrice: number;
  variableCost: number;
  fixedCostProduct: number | null;
  unitsSoldCurrent: number;
  notes: string | null;
  marginUnit: number;
  expectedProfit: number;
  breakEvenUnits: number | null;
  deadline: string | null;
  profitable: boolean;
  targetAchieved: boolean;
};

type Software = {
  id: string;
  name: string;
  price: number;
};

type Summary = {
  totalTargets: number;
  achievedTargets: number;
  totalTargetRevenue: number;
  totalExpectedProfit: number;
};

type RealActivationCost = {
  softwareId: string;
  averageActivationCost: number | null;
};

export default function PlanningTargetsPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [targets, setTargets] = useState<SalesTarget[]>([]);
  const [closedTargets, setClosedTargets] = useState<SalesTarget[]>([]);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [products, setProducts] = useState<Software[]>([]);
  const [realCostMap, setRealCostMap] = useState<Record<string, number | null>>({});
  const [fixedCostGlobal, setFixedCostGlobal] = useState<string>("");
  const [savingFixedCost, setSavingFixedCost] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [form, setForm] = useState({
    softwareId: "",
    metaUnits: "0",
    salePrice: "",
    variableCost: "",
    fixedCostProduct: "",
    periodStart: "",
    periodEnd: "",
    deadline: "",
    notes: "",
  });

  useEffect(() => {
    if (!allowed) {
      return;
    }
    let active = true;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const [targetsData, closedTargetsData, summaryData, productData, realCostData, settingsData] = await Promise.all([
          apiFetch<SalesTarget[]>("/api/backend/api/v1/sales-targets?status=ACTIVE"),
          apiFetch<SalesTarget[]>("/api/backend/api/v1/sales-targets?status=CLOSED"),
          apiFetch<Summary>("/api/backend/api/v1/sales-targets/summary"),
          apiFetch<Software[]>("/api/backend/api/v1/store/products"),
          apiFetch<RealActivationCost[]>("/api/backend/api/v1/sales-targets/real-costs"),
          apiFetch<{ fixedCostGlobal: number }>("/api/backend/api/v1/sales-targets/settings"),
        ]);
        if (!active) return;
        setTargets(targetsData);
        setClosedTargets(closedTargetsData);
        setSummary(summaryData);
        setProducts(productData);
        const costMap = realCostData.reduce<Record<string, number | null>>((acc, item) => {
          acc[item.softwareId] = item.averageActivationCost;
          return acc;
        }, {});
        setRealCostMap(costMap);
        setFixedCostGlobal(
          settingsData?.fixedCostGlobal !== undefined ? settingsData.fixedCostGlobal.toString() : ""
        );
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

  const reloadPlanningData = async () => {
    const [targetsData, closedTargetsData, summaryData, productData, realCostData, settingsData] = await Promise.all([
      apiFetch<SalesTarget[]>("/api/backend/api/v1/sales-targets?status=ACTIVE"),
      apiFetch<SalesTarget[]>("/api/backend/api/v1/sales-targets?status=CLOSED"),
      apiFetch<Summary>("/api/backend/api/v1/sales-targets/summary"),
      apiFetch<Software[]>("/api/backend/api/v1/store/products"),
      apiFetch<RealActivationCost[]>("/api/backend/api/v1/sales-targets/real-costs"),
      apiFetch<{ fixedCostGlobal: number }>("/api/backend/api/v1/sales-targets/settings"),
    ]);
    setTargets(targetsData);
    setClosedTargets(closedTargetsData);
    setSummary(summaryData);
    setProducts(productData);
    const costMap = realCostData.reduce<Record<string, number | null>>((acc, item) => {
      acc[item.softwareId] = item.averageActivationCost;
      return acc;
    }, {});
    setRealCostMap(costMap);
    setFixedCostGlobal(
      settingsData?.fixedCostGlobal !== undefined ? settingsData.fixedCostGlobal.toString() : ""
    );
  };

  const resetForm = () => {
    setEditingId(null);
    setForm({
      softwareId: "",
      metaUnits: "0",
      salePrice: "",
      variableCost: "",
      fixedCostProduct: "",
      periodStart: "",
      periodEnd: "",
      deadline: "",
      notes: "",
    });
  };

  const handleEdit = (target: SalesTarget) => {
    setEditingId(target.id);
    setForm({
      softwareId: target.softwareId,
      metaUnits: String(target.metaUnits ?? 0),
      salePrice: String(target.salePrice ?? 0),
      variableCost: String(target.variableCost ?? 0),
      fixedCostProduct: target.fixedCostProduct != null ? String(target.fixedCostProduct) : "",
      periodStart: target.periodStart || "",
      periodEnd: target.periodEnd || target.deadline || "",
      deadline: target.deadline || "",
      notes: target.notes || "",
    });
  };

  const resolveRealCost = (softwareId: string) => {
    const cost = realCostMap[softwareId];
    return typeof cost === "number" ? cost : null;
  };

  const toDateInput = (date: Date) => date.toISOString().slice(0, 10);

  const quarterBoundsFrom = (value?: string | null) => {
    const base = value ? new Date(`${value}T00:00:00`) : new Date();
    const quarterStartMonth = Math.floor(base.getMonth() / 3) * 3;
    const start = new Date(base.getFullYear(), quarterStartMonth, 1);
    const end = new Date(base.getFullYear(), quarterStartMonth + 3, 0);
    return {
      start: toDateInput(start),
      end: toDateInput(end),
    };
  };

  const nextQuarterBounds = (target: SalesTarget) => {
    const base = target.periodEnd || target.deadline || toDateInput(new Date());
    const date = new Date(`${base}T00:00:00`);
    const next = new Date(date.getFullYear(), date.getMonth() + 1, 1);
    return quarterBoundsFrom(toDateInput(next));
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const metaUnits = Number(form.metaUnits || 0);
      const salePrice = Number(form.salePrice || 0);
      const variableCost = Number(form.variableCost || 0);
      const fixedCostProduct = form.fixedCostProduct ? Number(form.fixedCostProduct) : null;
      if (!form.softwareId) {
        setError("Selecciona un producto para guardar la meta.");
        return;
      }
      if (Number.isNaN(metaUnits) || metaUnits <= 0) {
        setError("La meta debe tener al menos 1 unidad.");
        return;
      }
      if (Number.isNaN(salePrice) || salePrice <= 0) {
        setError("El precio de venta debe ser mayor que 0.");
        return;
      }
      if (Number.isNaN(variableCost) || variableCost < 0) {
        setError("El costo variable no puede ser negativo.");
        return;
      }
      if (fixedCostProduct != null && (Number.isNaN(fixedCostProduct) || fixedCostProduct < 0)) {
        setError("El costo fijo del producto no puede ser negativo.");
        return;
      }
      if (form.periodStart && form.periodEnd && form.periodEnd < form.periodStart) {
        setError("La fecha final del periodo no puede ser menor que la fecha inicial.");
        return;
      }
      const currentUnitsSold = editingId
        ? targets.find((target) => target.id === editingId)?.unitsSoldCurrent ?? 0
        : 0;
      const periodEnd = form.periodEnd || form.deadline || null;
      const payload = {
        softwareId: form.softwareId || null,
        metaUnits,
        salePrice,
        variableCost,
        fixedCostProduct,
        unitsSoldCurrent: currentUnitsSold,
        periodStart: form.periodStart || null,
        periodEnd,
        deadline: form.deadline || periodEnd,
        notes: form.notes || "",
      };
      if (editingId) {
        await apiFetch(`/api/backend/api/v1/sales-targets/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      } else {
        await apiFetch("/api/backend/api/v1/sales-targets", {
          method: "POST",
          body: JSON.stringify(payload),
        });
      }
      await reloadPlanningData();
      resetForm();
      setNotice(editingId ? "Meta actualizada." : "Meta creada.");
    } catch (err: any) {
      setError(err?.message || "No se pudo guardar la meta.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    const ok = window.confirm("Eliminar esta meta? Esta accion no se puede deshacer.");
    if (!ok) return;
    setError(null);
    setNotice(null);
    try {
      await apiFetch(`/api/backend/api/v1/sales-targets/${id}`, { method: "DELETE" });
      await reloadPlanningData();
      if (editingId === id) {
        resetForm();
      }
      setNotice("Meta eliminada.");
    } catch (err: any) {
      setError(err?.message || "No se pudo eliminar la meta.");
    }
  };

  const handleCloseTarget = async (target: SalesTarget) => {
    const ok = window.confirm(
      `Cerrar la meta de ${target.softwareName}? Quedara como historial y las ventas nuevas ya no sumaran a este ciclo.`
    );
    if (!ok) return false;
    setError(null);
    setNotice(null);
    try {
      await apiFetch(`/api/backend/api/v1/sales-targets/${target.id}/close`, { method: "POST" });
      await reloadPlanningData();
      if (editingId === target.id) {
        resetForm();
      }
      setNotice("Meta cerrada. Ahora puedes crear una nueva meta para el siguiente trimestre.");
      return true;
    } catch (err: any) {
      setError(err?.message || "No se pudo cerrar la meta.");
      return false;
    }
  };

  const handleRenewTarget = async (target: SalesTarget) => {
    if (target.status === "ACTIVE") {
      const closed = await handleCloseTarget(target);
      if (!closed) {
        return;
      }
    }
    const nextPeriod = nextQuarterBounds(target);
    setEditingId(null);
    setForm({
      softwareId: target.softwareId,
      metaUnits: String(target.metaUnits ?? 0),
      salePrice: String(target.salePrice ?? 0),
      variableCost: String(target.variableCost ?? 0),
      fixedCostProduct: target.fixedCostProduct != null ? String(target.fixedCostProduct) : "",
      periodStart: nextPeriod.start,
      periodEnd: nextPeriod.end,
      deadline: nextPeriod.end,
      notes: `Renovada desde meta cerrada ${target.periodStart || ""} - ${target.periodEnd || target.deadline || ""}`.trim(),
    });
    setNotice("Formulario preparado para el nuevo trimestre. Revisa unidades, precios y guarda la nueva meta.");
  };

  const handleSaveFixedCost = async () => {
    const value = Number(fixedCostGlobal);
    if (Number.isNaN(value) || value < 0) {
      setError("Costo fijo global invalido.");
      return;
    }
    setSavingFixedCost(true);
    setError(null);
    setNotice(null);
    try {
      await apiFetch("/api/backend/api/v1/sales-targets/settings", {
        method: "PUT",
        body: JSON.stringify({ fixedCostGlobal: value }),
      });
      setNotice("Costo fijo global actualizado.");
    } catch (err: any) {
      setError(err?.message || "No se pudo guardar el costo fijo global.");
    } finally {
      setSavingFixedCost(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    setError(null);
    setNotice(null);
    try {
      await reloadPlanningData();
      setNotice("Planeacion actualizada.");
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar planeacion.");
    } finally {
      setRefreshing(false);
    }
  };

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

  const saveDisabled =
    saving
    || !form.softwareId
    || Number.isNaN(Number(form.metaUnits || 0))
    || Number(form.metaUnits || 0) <= 0
    || Number.isNaN(Number(form.salePrice || 0))
    || Number(form.salePrice || 0) <= 0
    || Number.isNaN(Number(form.variableCost || 0))
    || Number(form.variableCost || 0) < 0
    || Boolean(form.periodStart && form.periodEnd && form.periodEnd < form.periodStart);

  return (
    <div className="min-h-screen min-w-0">

      <div className="min-w-0 px-4 py-6 sm:px-6 lg:px-8">
        <div className="glass-panel hero-band rounded-[32px] px-6 py-6 fade-up">
          <AdminHeader
            title="Metas y rentabilidad"
            subtitle="Planeacion"
            userEmail={user.email}
            onLogout={handleLogout}
            actions={
              <>
                <AdminActionButton
                  icon={<PlusIcon className="h-4 w-4" />}
                  label={editingId ? "Nueva meta" : "Limpiar"}
                  variant="primary"
                  onClick={resetForm}
                />
                <AdminActionButton
                  icon={<RefreshIcon className="h-4 w-4" />}
                  label={refreshing ? "Actualizando" : "Actualizar"}
                  onClick={handleRefresh}
                />
                <AdminActionButton
                  label="Ver metas"
                  onClick={() => router.push("/admin/targets")}
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

        <section className="mt-6 rounded-2xl border border-indigo-200 bg-indigo-50 px-4 py-3 text-sm text-indigo-800">
          Planeacion es la pantalla operativa para crear, editar y recalcular metas por producto.
          Si solo quieres revisar avance consolidado, usa{" "}
          <button
            type="button"
            className="font-semibold underline"
            onClick={() => router.push("/admin/targets")}
          >
            Metas
          </button>
          .
        </section>

        <section className="mt-6 glass-panel rounded-3xl p-6 fade-up delay-1">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <h2 className="font-display text-lg text-slate">Costo fijo global</h2>
              <p className="text-xs text-slate/60">Se descuenta de la ganancia estimada si no hay costo fijo por producto.</p>
            </div>
            <div className="flex items-center gap-2">
              <input
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
                value={fixedCostGlobal}
                onChange={(e) => setFixedCostGlobal(e.target.value)}
                placeholder="0.00"
              />
              <button
                onClick={handleSaveFixedCost}
                disabled={savingFixedCost}
                className="rounded-full border border-smoke/70 bg-white px-4 py-2 text-[10px] uppercase tracking-[0.18em] text-slate disabled:opacity-60"
              >
                {savingFixedCost ? "Guardando" : "Guardar"}
              </button>
            </div>
          </div>
        </section>

        <section className="mt-8 grid min-w-0 gap-5 2xl:grid-cols-[minmax(0,1.45fr)_minmax(380px,0.55fr)]">
          <div className="min-w-0 space-y-4">
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <div className="rounded-xl border border-smoke/60 bg-white/90 p-3">
                <p className="text-[10px] uppercase tracking-[0.18em] text-slate/60">Metas</p>
                <p className="mt-1 text-lg font-semibold text-ink">{summary?.totalTargets ?? 0}</p>
              </div>
              <div className="rounded-xl border border-smoke/60 bg-white/90 p-3">
                <p className="text-[10px] uppercase tracking-[0.18em] text-slate/60">Revenue objetivo</p>
                <p className="mt-1 text-lg font-semibold text-ink">
                  {formatCurrency(summary?.totalTargetRevenue ?? 0)}
                </p>
              </div>
              <div className="rounded-xl border border-smoke/60 bg-white/90 p-3">
                <p className="text-[10px] uppercase tracking-[0.18em] text-slate/60">Ganancia esperada</p>
                <p className="mt-1 text-lg font-semibold text-ink">
                  {formatCurrency(summary?.totalExpectedProfit ?? 0)}
                </p>
              </div>
              <div className="rounded-xl border border-smoke/60 bg-white/90 p-3">
                <p className="text-[10px] uppercase tracking-[0.18em] text-slate/60">No rentables</p>
                <p className="mt-1 text-lg font-semibold text-ink">
                  {targets.filter((target) => !target.profitable).length}
                </p>
              </div>
            </div>

            <div className="glass-panel min-w-0 rounded-3xl p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="font-display text-lg text-slate">Metas activas</h2>
                <span className="text-xs uppercase tracking-[0.2em] text-slate/50">
                  Cumplidas {summary?.achievedTargets ?? 0}
                </span>
              </div>
              <div className="mt-3 overflow-hidden rounded-2xl border border-smoke/60 bg-white/90 text-slate/70">
                {targets.map((target) => (
                  <article
                    key={target.id}
                    className="min-w-0 border-b border-smoke/60 p-4 last:border-b-0"
                  >
                    <div className="grid min-w-0 gap-4 sm:grid-cols-[minmax(0,1fr)_minmax(170px,auto)] sm:items-start">
                      <div className="min-w-0">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Producto</span>
                        <p className="mt-1 break-words text-sm font-semibold leading-5 text-ink">
                          {target.softwareName}
                        </p>
                        <p className="mt-1 text-[11px] text-slate/50">
                          {target.profitable ? "Rentable" : "No rentable"} -{" "}
                          {target.deadline ? formatDate(target.deadline) : "Sin fecha"}
                        </p>
                      </div>
                      <div className="sm:text-right">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Periodo</span>
                        <p className="mt-1 whitespace-nowrap text-xs font-medium text-ink">
                          {target.periodStart ? formatDate(target.periodStart) : "-"} -{" "}
                          {target.periodEnd ? formatDate(target.periodEnd) : "-"}
                        </p>
                      </div>
                    </div>

                    <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-5">
                      <div className="min-w-0 rounded-xl bg-slate-50 px-3 py-2">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Meta</span>
                        <p className="mt-1 text-sm font-semibold text-ink">{target.metaUnits}</p>
                      </div>
                      <div className="min-w-0 rounded-xl bg-slate-50 px-3 py-2">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Avance</span>
                        <p className="mt-1 text-sm font-semibold text-ink">{target.unitsSoldCurrent}</p>
                      </div>
                      <div className="min-w-0 rounded-xl bg-slate-50 px-3 py-2">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Margen</span>
                        <p className="mt-1 break-words text-sm font-semibold text-ink">
                          {formatCurrency(target.marginUnit)}
                        </p>
                      </div>
                      <div className="min-w-0 rounded-xl bg-slate-50 px-3 py-2">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Costo real</span>
                        <p className="mt-1 break-words text-sm font-semibold text-ink">
                          {resolveRealCost(target.softwareId) != null
                            ? formatCurrency(resolveRealCost(target.softwareId) || 0)
                            : "-"}
                        </p>
                      </div>
                      <div className="min-w-0 rounded-xl bg-slate-50 px-3 py-2">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Ganancia</span>
                        <p className="mt-1 break-words text-sm font-semibold text-ink">
                          {formatCurrency(target.expectedProfit)}
                        </p>
                      </div>
                    </div>

                    <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-smoke/50 pt-3 sm:justify-end">
                        <button
                          onClick={() => handleEdit(target)}
                          className="rounded-full border border-smoke/70 bg-white px-3 py-1.5 text-[11px] font-semibold tracking-normal text-ink"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => handleCloseTarget(target)}
                          className="rounded-full border border-amber-200 bg-amber-50 px-3 py-1.5 text-[11px] font-semibold tracking-normal text-amber-800"
                        >
                          Cerrar
                        </button>
                        <button
                          onClick={() => handleRenewTarget(target)}
                          className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-[11px] font-semibold tracking-normal text-emerald-700"
                        >
                          Renovar
                        </button>
                        <button
                          onClick={() => handleDelete(target.id)}
                          className="rounded-full border border-red-200 bg-red-50 px-3 py-1.5 text-[11px] font-semibold tracking-normal text-red-700"
                        >
                          Eliminar
                        </button>
                    </div>
                  </article>
                ))}
                {targets.length === 0 && (
                  <div className="rounded-2xl border border-dashed border-smoke/60 p-4 text-center text-sm text-slate/60">
                    Aun no hay metas registradas.
                  </div>
                )}
              </div>
            </div>

            <div className="glass-panel min-w-0 rounded-3xl p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="font-display text-lg text-slate">Metas cerradas</h2>
                <span className="text-xs uppercase tracking-[0.2em] text-slate/50">
                  Historial {closedTargets.length}
                </span>
              </div>
              <div className="mt-3 overflow-hidden rounded-2xl border border-smoke/60 bg-white/80 text-slate/70">
                {closedTargets.slice(0, 8).map((target) => (
                  <article
                    key={target.id}
                    className="min-w-0 border-b border-smoke/60 p-4 last:border-b-0"
                  >
                    <div className="grid min-w-0 gap-4 sm:grid-cols-[minmax(0,1fr)_minmax(170px,auto)] sm:items-start">
                      <div className="min-w-0">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Producto</span>
                        <p className="mt-1 break-words text-sm font-semibold leading-5 text-ink">
                          {target.softwareName}
                        </p>
                        <p className="mt-1 text-[11px] text-slate/50">
                          Cerrada {target.closedAt ? formatDate(target.closedAt) : "-"}
                        </p>
                      </div>
                      <div className="sm:text-right">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Periodo</span>
                        <p className="mt-1 whitespace-nowrap text-xs font-medium text-ink">
                          {target.periodStart ? formatDate(target.periodStart) : "-"} -{" "}
                          {target.periodEnd ? formatDate(target.periodEnd) : "-"}
                        </p>
                      </div>
                    </div>
                    <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
                      <div className="min-w-0 rounded-xl bg-slate-50 px-3 py-2">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Meta</span>
                        <p className="mt-1 text-sm font-semibold text-ink">{target.metaUnits}</p>
                      </div>
                      <div className="min-w-0 rounded-xl bg-slate-50 px-3 py-2">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Vendido</span>
                        <p className="mt-1 text-sm font-semibold text-ink">{target.unitsSoldCurrent}</p>
                      </div>
                      <div className="min-w-0 rounded-xl bg-slate-50 px-3 py-2">
                        <span className="text-[10px] font-semibold uppercase tracking-normal text-slate/50">Ganancia</span>
                        <p className="mt-1 break-words text-sm font-semibold text-ink">
                          {formatCurrency(target.expectedProfit)}
                        </p>
                      </div>
                    </div>
                    <div className="mt-4 flex justify-end border-t border-smoke/50 pt-3">
                        <button
                          onClick={() => handleRenewTarget(target)}
                          className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-[11px] font-semibold tracking-normal text-emerald-700"
                        >
                          Renovar
                        </button>
                    </div>
                  </article>
                ))}
                {closedTargets.length === 0 && (
                  <div className="rounded-2xl border border-dashed border-smoke/60 p-4 text-center text-sm text-slate/60">
                    Aun no hay metas cerradas.
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="glass-panel min-w-0 self-start rounded-3xl p-4 2xl:sticky 2xl:top-20">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-lg text-slate">
                {editingId ? "Editar" : "Nueva"}
              </h2>
              <button
                onClick={resetForm}
                className="rounded-full border border-smoke/70 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-slate"
              >
                Limpiar
              </button>
            </div>
            <div className="mt-3 grid gap-2 text-[12px] text-slate/80">
              <label className="flex flex-col gap-1">
                Producto
                <span className="text-[10px] text-slate/50">Producto al que aplica la meta.</span>
                <select
                  className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px]"
                  value={form.softwareId}
                  onChange={(event) => {
                    const product = products.find((item) => item.id === event.target.value);
                    const realCost = event.target.value ? resolveRealCost(event.target.value) : null;
                    const currentQuarter = quarterBoundsFrom();
                    setForm((prev) => ({
                      ...prev,
                      softwareId: event.target.value,
                      salePrice: product?.price != null ? String(product.price) : "",
                      periodStart: prev.periodStart || currentQuarter.start,
                      periodEnd: prev.periodEnd || currentQuarter.end,
                      deadline: prev.deadline || currentQuarter.end,
                      variableCost:
                        prev.variableCost && prev.variableCost !== "0"
                          ? prev.variableCost
                          : realCost != null
                            ? String(realCost)
                            : prev.variableCost,
                    }));
                  }}
                >
                  <option value="">Selecciona producto</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.name}
                    </option>
                  ))}
                </select>
              </label>
              <div className="rounded-2xl border border-sky-100 bg-sky-50/70 p-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <p className="text-[10px] uppercase tracking-[0.18em] text-sky-800">Periodo de meta</p>
                    <p className="text-[10px] text-sky-700">
                      Cierra el trimestre anterior y crea uno nuevo para mantener historial limpio.
                    </p>
                  </div>
                  <button
                    type="button"
                    className="rounded-full border border-sky-200 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.16em] text-sky-800"
                    onClick={() => {
                      const currentQuarter = quarterBoundsFrom();
                      setForm((prev) => ({
                        ...prev,
                        periodStart: currentQuarter.start,
                        periodEnd: currentQuarter.end,
                        deadline: currentQuarter.end,
                      }));
                    }}
                  >
                    Trimestre actual
                  </button>
                </div>
                <div className="mt-3 grid gap-2 sm:grid-cols-2">
                  <label className="flex flex-col gap-1">
                    Inicio periodo
                    <input
                      type="date"
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px]"
                      value={form.periodStart}
                      onChange={(event) => setForm((prev) => ({ ...prev, periodStart: event.target.value }))}
                    />
                  </label>
                  <label className="flex flex-col gap-1">
                    Fin periodo
                    <input
                      type="date"
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px]"
                      value={form.periodEnd}
                      onChange={(event) =>
                        setForm((prev) => ({
                          ...prev,
                          periodEnd: event.target.value,
                          deadline: prev.deadline || event.target.value,
                        }))
                      }
                    />
                  </label>
                </div>
              </div>
              <div className="grid gap-2 sm:grid-cols-2">
                <label className="flex flex-col gap-1">
                  Meta unidades
                  <span className="text-[10px] text-slate/50">Cantidad objetivo a vender.</span>
                  <input
                    type="number"
                    min={0}
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px]"
                    value={form.metaUnits}
                    onChange={(event) => setForm((prev) => ({ ...prev, metaUnits: event.target.value }))}
                  />
                </label>
                <label className="flex flex-col gap-1">
                  Precio venta
                  <span className="text-[10px] text-slate/50">
                    Precio base del producto para el escenario financiero. Puedes ajustarlo si hace falta.
                  </span>
                  <input
                    type="number"
                    min={0}
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px]"
                    value={form.salePrice}
                    onChange={(event) => setForm((prev) => ({ ...prev, salePrice: event.target.value }))}
                  />
                </label>
                <label className="flex flex-col gap-1">
                  Costo activacion (inventario)
                  <span className="text-[10px] text-slate/50">
                    Se calcula con costo de compra por activaciones por serial. Este valor alimenta metas y margen.
                  </span>
                  <input
                    type="text"
                    readOnly
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px] text-slate/60"
                    value={
                      form.softwareId && resolveRealCost(form.softwareId) != null
                        ? formatCurrency(resolveRealCost(form.softwareId) || 0)
                        : "Sin datos"
                    }
                  />
                </label>
                <label className="flex flex-col gap-1">
                  Costo variable
                  <span className="text-[10px] text-slate/50">Costo por unidad vendida.</span>
                  <input
                    type="number"
                    min={0}
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px]"
                    value={form.variableCost}
                    onChange={(event) =>
                      setForm((prev) => ({ ...prev, variableCost: event.target.value }))
                    }
                  />
                  <span className="text-[10px] text-slate/50">
                    Costo real por activacion:{" "}
                    {form.softwareId && resolveRealCost(form.softwareId) != null
                      ? formatCurrency(resolveRealCost(form.softwareId) || 0)
                      : "Sin datos"}
                  </span>
                </label>
                <label className="flex flex-col gap-1">
                  Costo fijo producto
                  <span className="text-[10px] text-slate/50">Gasto fijo exclusivo del producto.</span>
                  <input
                    type="number"
                    min={0}
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px]"
                    value={form.fixedCostProduct}
                    onChange={(event) =>
                      setForm((prev) => ({ ...prev, fixedCostProduct: event.target.value }))
                    }
                  />
                </label>
                <label className="flex flex-col gap-1">
                  Deadline
                  <span className="text-[10px] text-slate/50">Fecha limite de la meta.</span>
                  <input
                    type="date"
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px]"
                    value={form.deadline}
                    onChange={(event) =>
                      setForm((prev) => ({
                        ...prev,
                        deadline: event.target.value,
                        periodEnd: prev.periodEnd || event.target.value,
                      }))
                    }
                  />
                </label>
              </div>
              <label className="flex flex-col gap-1">
                Notas
                <span className="text-[10px] text-slate/50">Contexto interno para el equipo.</span>
                <textarea
                  rows={2}
                  className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[12px]"
                  value={form.notes}
                  onChange={(event) => setForm((prev) => ({ ...prev, notes: event.target.value }))}
                />
              </label>
              <button
                onClick={handleSave}
                disabled={saveDisabled}
                className="rounded-full bg-ink px-4 py-2 text-[10px] uppercase tracking-[0.18em] text-white disabled:opacity-60"
              >
                {saving ? "Guardando" : editingId ? "Actualizar" : "Crear"}
              </button>
            </div>
          </div>
        </section>

        <section className="mt-6 rounded-2xl border border-smoke/60 bg-white/80 p-4 text-[12px] text-slate/70">
          <h3 className="text-xs uppercase tracking-[0.2em] text-slate/50">Guia rapida</h3>
          <p className="mt-2">
            Meta unidades: objetivo de ventas del producto. Precio venta y costo variable definen el margen por unidad.
          </p>
          <p className="mt-1">
            Costo fijo producto: gasto fijo asociado solo a ese producto (si no aplica, deja en blanco).
          </p>
          <p className="mt-1">
            Periodo de meta: define el trimestre comercial. Al terminar el trimestre, cierra la meta y renuevala para crear el nuevo ciclo.
          </p>
          <p className="mt-1">
            Deadline: fecha limite para cumplir la meta. Notas: contexto interno para el equipo.
          </p>
          <p className="mt-1">
            Costo por activacion: se calcula por serial (costo de compra por activaciones) y se usa en metas y margen.
          </p>
        </section>
      </div>
    </div>
  );
}
