"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { PlusIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

const licenseStatuses = ["ACTIVE", "INACTIVE", "EXPIRED", "DAMAGED"] as const;

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type Software = {
  id: string;
  name: string;
  price: number;
};

type License = {
  id: string;
  licenseKey: string;
  status: string;
  available: boolean;
  usedActivations: number;
  maxActivations: number;
  purchasePrice: number;
  softwareId: string;
  softwareName: string;
  ownerId: string | null;
  ownerEmail: string | null;
};

type LicenseAssignment = {
  saleId: string;
  clientId: string;
  clientName: string;
  clientEmail: string;
  assignedAt: string;
};

type ProductSummary = {
  softwareId: string;
  softwareName: string;
  availableSerials: number;
  totalSerials: number;
  usedActivations: number;
  maxActivations: number;
  avgCost: number;
  totalCost: number;
  costPerActivation: number;
  status: string;
};

const defaultPage: PageResponse<unknown> = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

export default function AdminLicensesPage() {
  const router = useRouter();
  const { user, allowed } = useAdminGuard("ADMIN_MANAGER");
  const createSectionRef = useRef<HTMLDivElement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [licenses, setLicenses] = useState<PageResponse<License>>(defaultPage as PageResponse<License>);
  const [selected, setSelected] = useState<ProductSummary | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [products, setProducts] = useState<Software[]>([]);
  const [search, setSearch] = useState("");
  const [assignments, setAssignments] = useState<LicenseAssignment[]>([]);
  const [assignmentsError, setAssignmentsError] = useState<string | null>(null);
  const [productLicenses, setProductLicenses] = useState<License[]>([]);
  const [productLicensesError, setProductLicensesError] = useState<string | null>(null);

  const [filters, setFilters] = useState({
    softwareId: "",
    status: "",
  });

  const [createForm, setCreateForm] = useState({
    softwareId: "",
    licenseKey: "",
    purchasePrice: "",
    maxActivations: "1",
  });

  const [bulkForm, setBulkForm] = useState({
    softwareId: "",
    purchasePrice: "",
    maxActivations: "1",
    keys: "",
  });

  const bulkCount = useMemo(() => {
    return bulkForm.keys
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean).length;
  }, [bulkForm.keys]);

  const filteredLicenses = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return licenses.content;
    return licenses.content.filter((license) => {
      return (
        license.softwareName.toLowerCase().includes(term)
      );
    });
  }, [licenses.content, search]);

  const productRows = useMemo<ProductSummary[]>(() => {
    const grouped = new Map<string, License[]>();
    filteredLicenses.forEach((license) => {
      const list = grouped.get(license.softwareId) || [];
      list.push(license);
      grouped.set(license.softwareId, list);
    });

    return Array.from(grouped.entries()).map(([softwareId, items]) => {
      const softwareName = items[0]?.softwareName || "-";
      const availableSerials = items.filter(
        (license) => license.status === "ACTIVE" && license.usedActivations < license.maxActivations
      ).length;
      const totalSerials = items.length;
      const usedActivations = items.reduce((sum, license) => sum + (license.usedActivations || 0), 0);
      const maxActivations = items.reduce((sum, license) => sum + (license.maxActivations || 0), 0);
      const totalCost = items.reduce((sum, license) => sum + (license.purchasePrice || 0), 0);
      const avgCost = totalSerials > 0 ? totalCost / totalSerials : 0;
      const costPerActivation = maxActivations > 0 ? totalCost / maxActivations : 0;
      const statusSet = new Set(items.map((license) => license.status));
      const status = statusSet.size === 1 ? items[0].status : "MIXED";
      return {
        softwareId,
        softwareName,
        availableSerials,
        totalSerials,
        usedActivations,
        maxActivations,
        avgCost,
        totalCost,
        costPerActivation,
        status,
      };
    });
  }, [filteredLicenses]);

  const loadLicenses = useCallback(async () => {
    const params = new URLSearchParams();
    if (filters.softwareId) params.set("softwareId", filters.softwareId);
    if (filters.status) params.set("status", filters.status);
    params.set("page", "0");
    params.set("size", "500");
    const data = await apiFetch<PageResponse<License>>(`/api/backend/api/v1/inventory/licenses?${params.toString()}`);
    setLicenses(data);
  }, [filters.softwareId, filters.status]);

  const loadProducts = useCallback(async () => {
    const data = await apiFetch<Software[]>("/api/backend/api/v1/store/products");
    setProducts(data);
  }, []);

  useEffect(() => {
    const boot = async () => {
      if (!allowed || !user) return;
      setError(null);
      try {
        await Promise.all([loadLicenses(), loadProducts()]);
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar licencias.");
      }
    };
    boot();
  }, [allowed, user, loadLicenses, loadProducts, router]);

  useEffect(() => {
    const loadProductLicenses = async () => {
      if (!selected) {
        setProductLicenses([]);
        setProductLicensesError(null);
        return;
      }
      setProductLicensesError(null);
      try {
        const data = licenses.content.filter((license) => license.softwareId === selected.softwareId);
        setProductLicenses(data);
      } catch (err: any) {
        setProductLicensesError("No se pudo cargar seriales del producto.");
      }
    };
    loadProductLicenses();
  }, [selected, licenses.content]);

  const availableSerials = useMemo(() => {
    return productLicenses
      .filter((license) => license.usedActivations < license.maxActivations && license.status === "ACTIVE")
      .sort((a, b) => a.id.localeCompare(b.id));
  }, [productLicenses]);

  const currentSerial = availableSerials[0] || null;

  useEffect(() => {
    const loadAssignments = async () => {
      if (!currentSerial) {
        setAssignments([]);
        setAssignmentsError(null);
        return;
      }
      setAssignmentsError(null);
      try {
        const data = await apiFetch<LicenseAssignment[]>(
          `/api/backend/api/v1/inventory/licenses/${currentSerial.id}/assignments`
        );
        setAssignments(data);
      } catch (err: any) {
        if (err?.status === 404) {
          setAssignments([]);
          return;
        }
        setAssignmentsError("No se pudo cargar clientes activados.");
      }
    };
    loadAssignments();
  }, [currentSerial]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleRefresh = async () => {
    setError(null);
    try {
      await loadLicenses();
    } catch (err: any) {
      setError(err?.message || "No se pudo recargar.");
    }
  };

  const handleCreate = async () => {
    setError(null);
    try {
      if (!createForm.softwareId || !createForm.licenseKey.trim()) {
        setError("Selecciona un producto y escribe la licencia.");
        return;
      }
      const payload = {
        softwareId: createForm.softwareId,
        licenseKey: createForm.licenseKey,
        purchasePrice: Number(createForm.purchasePrice),
        maxActivations: Number(createForm.maxActivations || 1),
      };
      const created = await apiFetch<License>("/api/backend/api/v1/inventory/licenses", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setNotice("Licencia creada.");
      setSelected(null);
      setCreateForm({
        softwareId: "",
        licenseKey: "",
        purchasePrice: "",
        maxActivations: "1",
      });
      await loadLicenses();
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo crear licencia.");
    }
  };

  const handleBulk = async () => {
    setError(null);
    try {
      if (!bulkForm.softwareId || bulkCount === 0) {
        setError("Selecciona producto y agrega licencias para cargar.");
        return;
      }
      const keys = bulkForm.keys
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);
      const licenses = keys.map((key) => ({
        softwareId: bulkForm.softwareId,
        licenseKey: key,
        purchasePrice: Number(bulkForm.purchasePrice),
        maxActivations: Number(bulkForm.maxActivations || 1),
      }));
      await apiFetch("/api/backend/api/v1/inventory/licenses/bulk", {
        method: "POST",
        body: JSON.stringify({ licenses }),
      });
      setNotice(`Carga masiva: ${keys.length} licencias.`);
      setBulkForm({
        softwareId: "",
        purchasePrice: "",
        maxActivations: "1",
        keys: "",
      });
      await loadLicenses();
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo cargar licencias.");
    }
  };

  const canCreateLicense =
    !!createForm.softwareId && createForm.licenseKey.trim().length > 0 && createForm.purchasePrice !== "";
  const canBulkUpload =
    !!bulkForm.softwareId && bulkCount > 0 && bulkForm.purchasePrice !== "";

  const updateLicense = async (payload: { status?: string; available?: boolean }) => {
    if (!currentSerial) return;
    setError(null);
    try {
      const updated = await apiFetch<License>(`/api/backend/api/v1/inventory/licenses/${currentSerial.id}`, {
        method: "PATCH",
        body: JSON.stringify(payload),
      });
      await loadLicenses();
      setNotice("Licencia actualizada.");
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar la licencia.");
    }
  };

  const handleDeleteLicense = async () => {
    if (!currentSerial) return;
    const ok = window.confirm("Eliminar esta licencia? Esta accion no se puede deshacer.");
    if (!ok) return;
    setError(null);
    try {
      await apiFetch(`/api/backend/api/v1/inventory/licenses/${currentSerial.id}`, { method: "DELETE" });
      await loadLicenses();
      setNotice("Licencia eliminada.");
      setTimeout(() => setNotice(null), 2000);
      setDrawerOpen(false);
    } catch (err: any) {
      setError(err?.message || "No se pudo eliminar la licencia.");
    }
  };

  const openDrawer = (row: ProductSummary) => {
    setSelected(row);
    setDrawerOpen(true);
  };

  const closeDrawer = () => {
    setDrawerOpen(false);
  };

  return (
    <div className="min-h-screen bg-paper text-slate">
      <div className="flex">
        <main className="flex-1">
          <AdminHeader
            title="Licencias"
            subtitle="Inventario"
            onLogout={handleLogout}
            userEmail={user?.email ?? ""}
          />

          <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-6 pb-16 pt-4">
            <div className="glass-panel hero-band rounded-[32px] px-6 py-6 fade-up">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-slate-400">Inventario</p>
                <h2 className="text-2xl font-semibold text-ink">Licencias y stock</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-500">
                  Controla claves, disponibilidad y asignaciones.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <AdminActionButton icon={<RefreshIcon />} variant="ghost" onClick={handleRefresh}>
                  Actualizar
                </AdminActionButton>
                <AdminActionButton
                  icon={<PlusIcon />}
                  variant="primary"
                  onClick={() => createSectionRef.current?.scrollIntoView({ behavior: "smooth" })}
                >
                  Nueva licencia
                </AdminActionButton>
              </div>
            </div>

            {error && (
              <div className="glass-card border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-700">
                {error}
              </div>
            )}
            {notice && (
              <div className="glass-card border border-emerald-400/40 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-700">
                {notice}
              </div>
            )}

            <div className="grid gap-6">
              <div className="glass-panel p-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-semibold text-ink">Licencias</h3>
                    <p className="text-xs text-slate-500">{productRows.length} productos</p>
                  </div>
                  <input
                    className="w-full max-w-[220px] rounded-full border border-slate-200 px-4 py-2 text-sm text-ink shadow-sm focus:border-accent focus:outline-none"
                    placeholder="Buscar producto"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                  />
                </div>

                <div className="mt-4 flex flex-wrap items-center gap-3">
                  <select
                    className="min-w-[180px] rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-ink"
                    value={filters.softwareId}
                    onChange={(event) => setFilters((prev) => ({ ...prev, softwareId: event.target.value }))}
                  >
                    <option value="">Todos los productos</option>
                    {products.map((product) => (
                      <option key={product.id} value={product.id}>
                        {product.name}
                      </option>
                    ))}
                  </select>
                  <select
                    className="min-w-[160px] rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-ink"
                    value={filters.status}
                    onChange={(event) => setFilters((prev) => ({ ...prev, status: event.target.value }))}
                  >
                    <option value="">Todos los estados</option>
                    {licenseStatuses.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                  <AdminActionButton variant="ghost" onClick={handleRefresh}>
                    Aplicar
                  </AdminActionButton>
                </div>
                <p className="mt-3 text-xs text-slate-500">
                  Costo por activación = costo de compra del serial ÷ número de activaciones de ese serial.
                </p>

                <div className="mt-3 overflow-hidden rounded-t-2xl border border-slate-200">
                  {productRows.length === 0 && (
                    <p className="px-3 py-4 text-sm text-slate-500">No hay productos con licencias.</p>
                  )}
                  {productRows.length > 0 && (
                    <div className="grid items-center gap-2 bg-slate-50 px-3 py-2 text-[11px] uppercase tracking-[0.14em] text-slate-500 md:grid-cols-[1.2fr_0.6fr_0.8fr_0.7fr_0.7fr_0.6fr_auto]">
                      <span>Producto</span>
                      <span className="md:text-right">Seriales</span>
                      <span className="md:text-right">Costo/activación</span>
                      <span className="md:text-right">Usadas</span>
                      <span className="md:text-right">Totales</span>
                      <span className="md:text-right">Estado</span>
                      <span className="md:text-right">Detalle</span>
                    </div>
                  )}
                  {productRows.map((row) => (
                    <div
                      key={row.softwareId}
                      className={`w-full border-b border-slate-200 px-3 py-2 transition last:border-b-0 ${
                        selected?.softwareId === row.softwareId
                          ? "border-l-4 border-l-accent bg-slate-50"
                          : "border-l-4 border-l-transparent bg-white"
                      }`}
                      onClick={() => setSelected(row)}
                      role="button"
                      tabIndex={0}
                    >
                      <div className="grid items-center gap-2 md:grid-cols-[1.2fr_0.6fr_0.8fr_0.7fr_0.7fr_0.6fr_auto]">
                        <div>
                          <p
                            className={`text-sm font-semibold ${
                              selected?.softwareId === row.softwareId ? "text-ink" : "text-slate-700"
                            }`}
                          >
                            {row.softwareName}
                          </p>
                        </div>
                        <p className="text-xs text-slate-500 md:text-right">{row.availableSerials}/{row.totalSerials}</p>
                        <p className="text-xs text-slate-500 md:text-right">{formatCurrency(row.costPerActivation)}</p>
                        <p className="text-xs text-slate-500 md:text-right">{row.usedActivations}</p>
                        <p className="text-xs text-slate-500 md:text-right">{row.maxActivations}</p>
                        <p className="text-xs text-slate-500 md:text-right">{row.status}</p>
                        <div className="flex justify-end">
                          <button
                            type="button"
                            className="text-[10px] uppercase tracking-[0.04em] text-accent"
                            onClick={(event) => {
                              event.stopPropagation();
                              openDrawer(row);
                            }}
                          >
                            Ver detalle
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="grid gap-4 lg:grid-cols-2">
              <div ref={createSectionRef} className="glass-panel p-4">
                <h3 className="text-base font-semibold text-ink">Nueva licencia</h3>
                <p className="mt-1 text-[11px] text-slate-500">Registra licencias una por una.</p>
                <div className="mt-3 grid gap-2">
                  <select
                    className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-ink"
                    value={createForm.softwareId}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, softwareId: event.target.value }))}
                  >
                    <option value="">Selecciona producto</option>
                    {products.map((product) => (
                      <option key={product.id} value={product.id}>
                        {product.name}
                      </option>
                    ))}
                  </select>
                  <input
                    className="rounded-lg border border-slate-200 px-3 py-2 text-xs text-ink focus:border-accent focus:outline-none"
                    placeholder="License key"
                    value={createForm.licenseKey}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, licenseKey: event.target.value }))}
                  />
                  <input
                    type="number"
                    min={0}
                    className="rounded-lg border border-slate-200 px-3 py-2 text-xs text-ink focus:border-accent focus:outline-none"
                    placeholder="Costo de compra"
                    value={createForm.purchasePrice}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, purchasePrice: event.target.value }))}
                  />
                  <input
                    type="number"
                    min={1}
                    className="rounded-lg border border-slate-200 px-3 py-2 text-xs text-ink focus:border-accent focus:outline-none"
                    placeholder="Max activaciones"
                    value={createForm.maxActivations}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, maxActivations: event.target.value }))}
                  />
                  {createForm.purchasePrice && createForm.maxActivations && (
                    <p className="text-[11px] text-slate-500">
                      Costo por activacion:{" "}
                      {formatCurrency(
                        Number(createForm.purchasePrice) / Number(createForm.maxActivations || 1)
                      )}
                    </p>
                  )}
                  <div className="flex justify-end">
                    <AdminActionButton variant="primary" onClick={handleCreate} disabled={!canCreateLicense}>
                      Guardar licencia
                    </AdminActionButton>
                  </div>
                </div>
              </div>

              <div className="glass-panel p-4">
                <h3 className="text-base font-semibold text-ink">Carga masiva</h3>
                <p className="mt-1 text-[11px] text-slate-500">
                  Pega una licencia por linea. Total: {bulkCount}
                </p>
                <div className="mt-3 grid gap-2">
                  <select
                    className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-ink"
                    value={bulkForm.softwareId}
                    onChange={(event) => setBulkForm((prev) => ({ ...prev, softwareId: event.target.value }))}
                  >
                    <option value="">Selecciona producto</option>
                    {products.map((product) => (
                      <option key={product.id} value={product.id}>
                        {product.name}
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    min={0}
                    className="rounded-lg border border-slate-200 px-3 py-2 text-xs text-ink focus:border-accent focus:outline-none"
                    placeholder="Costo de compra"
                    value={bulkForm.purchasePrice}
                    onChange={(event) => setBulkForm((prev) => ({ ...prev, purchasePrice: event.target.value }))}
                  />
                  <input
                    type="number"
                    min={1}
                    className="rounded-lg border border-slate-200 px-3 py-2 text-xs text-ink focus:border-accent focus:outline-none"
                    placeholder="Max activaciones"
                    value={bulkForm.maxActivations}
                    onChange={(event) => setBulkForm((prev) => ({ ...prev, maxActivations: event.target.value }))}
                  />
                  {bulkForm.purchasePrice && bulkForm.maxActivations && (
                    <p className="text-[11px] text-slate-500">
                      Costo por activacion:{" "}
                      {formatCurrency(
                        Number(bulkForm.purchasePrice) / Number(bulkForm.maxActivations || 1)
                      )}
                    </p>
                  )}
                  <textarea
                    className="min-h-[100px] rounded-lg border border-slate-200 px-3 py-2 text-xs text-ink focus:border-accent focus:outline-none"
                    placeholder="LIC-0001\nLIC-0002\nLIC-0003"
                    value={bulkForm.keys}
                    onChange={(event) => setBulkForm((prev) => ({ ...prev, keys: event.target.value }))}
                  />
                  <div className="flex justify-end">
                    <AdminActionButton variant="primary" onClick={handleBulk} disabled={!canBulkUpload}>
                      Cargar licencias
                    </AdminActionButton>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>

      {drawerOpen && (
        <div className="fixed inset-0 z-40">
          <button
            type="button"
            className="absolute inset-0 bg-black/30"
            onClick={closeDrawer}
            aria-label="Cerrar detalle"
          />
          <aside className="absolute right-0 top-0 h-full w-full max-w-xl overflow-y-auto bg-white shadow-2xl">
            <div className="border-b border-slate-200 px-6 py-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Detalle</p>
                  <h3 className="text-lg font-semibold text-ink">
                    {selected?.softwareName || "Producto"}
                  </h3>
                </div>
                <button
                  type="button"
                  className="rounded-full border border-slate-200 px-3 py-1 text-xs uppercase tracking-[0.18em] text-slate-500"
                  onClick={closeDrawer}
                >
                  Cerrar
                </button>
              </div>
            </div>
            <div className="px-6 py-5">
              {!selected ? (
                <p className="text-sm text-slate-500">Selecciona una licencia para ver detalle.</p>
              ) : (
                <div className="space-y-4">
                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="glass-card px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Producto</p>
                      <p className="text-xs text-slate-500">ID: {selected.softwareId}</p>
                    </div>
                    <div className="glass-card px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Estado</p>
                      <p className="text-sm text-ink">{selected.status}</p>
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-3">
                    <div className="glass-card px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Costo por activación</p>
                      <p className="text-sm text-ink">{formatCurrency(selected.costPerActivation)}</p>
                    </div>
                    <div className="glass-card px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Activaciones totales</p>
                      <p className="text-sm text-ink">{selected.maxActivations}</p>
                    </div>
                    <div className="glass-card px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Activaciones usadas</p>
                      <p className="text-sm text-ink">{selected.usedActivations}</p>
                    </div>
                  </div>
                  <div className="glass-card px-4 py-3">
                    <div className="flex items-center justify-between">
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Serial en uso</p>
                      <p className="text-xs text-slate-400">
                        {currentSerial ? "Disponible" : "Sin serial disponible"}
                      </p>
                    </div>
                    {productLicensesError && (
                      <p className="mt-2 text-xs text-rose-600">{productLicensesError}</p>
                    )}
                    {!productLicensesError && (
                      <p className="mt-2 text-sm font-semibold text-ink">
                        {currentSerial?.licenseKey || "No disponible"}
                      </p>
                    )}
                    <p className="mt-1 text-xs text-slate-500">
                      Se usa en orden. Cuando este serial alcanza su limite, se pasa al siguiente.
                    </p>
                  </div>
                  <div className="glass-card px-4 py-3">
                    <div className="flex items-center justify-between">
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Seriales disponibles</p>
                      <p className="text-xs text-slate-400">{availableSerials.length} disponibles</p>
                    </div>
                    {productLicensesError && (
                      <p className="mt-2 text-xs text-rose-600">{productLicensesError}</p>
                    )}
                    {!productLicensesError && availableSerials.length === 0 && (
                      <p className="mt-2 text-xs text-slate-500">No hay seriales disponibles.</p>
                    )}
                    {!productLicensesError && availableSerials.length > 0 && (
                      <div className="mt-3 flex flex-wrap gap-2">
                        {availableSerials.map((license) => (
                          <span
                            key={license.id}
                            className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs text-slate-600"
                          >
                            {license.licenseKey}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="glass-card px-4 py-3">
                    <div className="flex items-center justify-between">
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Clientes activados</p>
                      <p className="text-xs text-slate-400">{assignments.length} registros</p>
                    </div>
                    {assignmentsError && (
                      <p className="mt-2 text-xs text-rose-600">{assignmentsError}</p>
                    )}
                    {!assignmentsError && assignments.length === 0 && (
                      <p className="mt-2 text-xs text-slate-500">Sin activaciones registradas.</p>
                    )}
                    {!assignmentsError && assignments.length > 0 && (
                      <div className="mt-3 space-y-2">
                        {assignments.map((assignment) => (
                          <div
                            key={`${assignment.saleId}-${assignment.clientId}-${assignment.assignedAt}`}
                            className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-xs text-slate-600"
                          >
                            <div>
                              <p className="text-sm font-semibold text-ink">{assignment.clientName}</p>
                              <p className="text-xs text-slate-500">{assignment.clientEmail}</p>
                            </div>
                            <span className="text-xs text-slate-400">
                              {formatDateTime(assignment.assignedAt)}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="flex flex-wrap items-center gap-3">
                    <select
                      className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-ink"
                      value={currentSerial?.status || "ACTIVE"}
                      onChange={(event) => updateLicense({ status: event.target.value })}
                      disabled={!currentSerial}
                    >
                      {licenseStatuses.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                    <AdminActionButton
                      variant="ghost"
                      onClick={() => updateLicense({ available: true })}
                      disabled={!currentSerial}
                    >
                      Marcar disponible
                    </AdminActionButton>
                    <AdminActionButton
                      variant="ghost"
                      onClick={() => updateLicense({ available: false })}
                      disabled={!currentSerial}
                    >
                      Marcar inactiva
                    </AdminActionButton>
                    <AdminActionButton
                      variant="ghost"
                      onClick={handleDeleteLicense}
                      disabled={!currentSerial}
                    >
                      Eliminar licencia
                    </AdminActionButton>
                  </div>
                </div>
              )}
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
