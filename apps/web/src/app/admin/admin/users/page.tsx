"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency, formatNumber } from "@/lib/format";
import { PlusIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type ArSummary = {
  saldoActual: number;
  diasAtraso: number;
  estado: "AL_DIA" | "VENCIDO" | "BLOQUEADO";
};

type AdminUser = {
  id: string;
  firstname: string;
  lastname: string;
  email: string;
  phone: string | null;
  role: string;
  customerType?: "FINAL" | "DISTRIBUTOR" | null;
  country?: "EC" | "PE" | null;
  currency?: "USD" | "PEN" | null;
  primaryMarket?: boolean;
  enabled: boolean;
  emailVerified: boolean;
  taxId: string | null;
  billingEmail: string | null;
  hasCredit: boolean;
  creditLimit: number;
  creditUsed: number;
  creditDays: number;
  arSummary: ArSummary;
};

const defaultPage: PageResponse<AdminUser> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 20,
  number: 0,
};

type TabType = "internal" | "client";

export default function AdminUsersPage() {
  const router = useRouter();
  const { user, allowed } = useAdminGuard("ADMIN_ONLY");
  const [activeTab, setActiveTab] = useState<TabType>("internal");
  const [selectedCountry, setSelectedCountry] = useState<"EC" | "PE">("EC");
  const [users, setUsers] = useState<PageResponse<AdminUser>>(defaultPage);
  const [selected, setSelected] = useState<AdminUser | null>(null);
  const [mode, setMode] = useState<"create" | "edit">("edit");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [tempPassword, setTempPassword] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [importFormat, setImportFormat] = useState<"csv" | "xlsx">("csv");
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importResult, setImportResult] = useState<string | null>(null);

  const isAdmin = user?.role === "ADMIN";
  const canEditRole = isAdmin;

  const [form, setForm] = useState({
    firstname: "",
    lastname: "",
    email: "",
    phone: "",
    role: "USER",
    customerType: "FINAL" as "FINAL" | "DISTRIBUTOR",
    enabled: true,
    taxId: "",
    billingEmail: "",
    hasCredit: false,
    creditLimit: "",
    creditDays: "",
    password: "",
  });

  const generateTempPassword = () => {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    const bytes = new Uint32Array(12);
    crypto.getRandomValues(bytes);
    return Array.from(bytes, (value) => chars[value % chars.length]).join("");
  };

  const normalizeEmail = (value: string) => value.trim().toLowerCase();

  const hasClientsTab = isAdmin || user?.role === "MANAGER";

  const tabOptions = useMemo(() => {
    if (isAdmin) {
      return [
        { key: "internal", label: "Usuarios internos" },
        { key: "client", label: "Clientes" },
      ] as const;
    }
    return [{ key: "client", label: "Clientes" }] as const;
  }, [isAdmin]);

  const loadUsers = useCallback(async () => {
    const params = new URLSearchParams();
    params.set("type", activeTab === "internal" ? "INTERNAL" : "CLIENT");
    params.set("page", page.toString());
    params.set("size", "10");
    if (activeTab === "client") params.set("country", selectedCountry);
    if (search) params.set("q", search);
    const data = await apiFetch<PageResponse<AdminUser>>(
      `/api/backend/api/v1/admin/users?${params.toString()}`
    );
    setUsers(data);
    if (data.content.length > 0 && !selected && mode === "edit") {
      setSelected(data.content[0]);
      setMode("edit");
    }
  }, [activeTab, page, search, selected, mode, selectedCountry]);

  const findUserByEmail = async (email: string) => {
    const params = new URLSearchParams();
    params.set("page", "0");
    params.set("size", "10");
    params.set("q", email);

    const internal = await apiFetch<PageResponse<AdminUser>>(
      `/api/backend/api/v1/admin/users?type=INTERNAL&${params.toString()}`
    );
    const internalMatch = internal.content.find(
      (item) => item.email?.toLowerCase() === email.toLowerCase()
    );
    if (internalMatch) {
      return { user: internalMatch, tab: "internal" as TabType };
    }

    const client = await apiFetch<PageResponse<AdminUser>>(
      `/api/backend/api/v1/admin/users?type=CLIENT&country=${selectedCountry}&${params.toString()}`
    );
    const clientMatch = client.content.find(
      (item) => item.email?.toLowerCase() === email.toLowerCase()
    );
    if (clientMatch) {
      return { user: clientMatch, tab: "client" as TabType };
    }

    return null;
  };

  useEffect(() => {
    if (!allowed || !user) return;
    if (!isAdmin) {
      setActiveTab("client");
    }
  }, [allowed, user, isAdmin]);

  useEffect(() => {
    const boot = async () => {
      if (!allowed || !user || !hasClientsTab) return;
      setError(null);
      try {
        await loadUsers();
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar usuarios.");
      }
    };
    boot();
  }, [allowed, user, activeTab, page, hasClientsTab, loadUsers, router]);

  useEffect(() => {
    if (!selected) {
      setForm({
        firstname: "",
        lastname: "",
        email: "",
        phone: "",
        role: activeTab === "internal" ? "MANAGER" : "CUSTOMER",
        customerType: "FINAL",
        enabled: true,
        taxId: "",
        billingEmail: "",
        hasCredit: false,
        creditLimit: "",
        creditDays: "",
        password: "",
      });
      return;
    }
    setForm({
      firstname: selected.firstname || "",
      lastname: selected.lastname || "",
      email: selected.email || "",
      phone: selected.phone || "",
      role: activeTab === "internal" ? selected.role || "MANAGER" : selected.role || "CUSTOMER",
      customerType: selected.customerType || "FINAL",
      enabled: selected.enabled,
      taxId: selected.taxId || "",
      billingEmail: selected.billingEmail || "",
      hasCredit: selected.hasCredit,
      creditLimit: selected.creditLimit?.toString() || "",
      creditDays: selected.creditDays?.toString() || "",
      password: "",
    });
  }, [selected, activeTab, selectedCountry]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleRefresh = async () => {
    setError(null);
    try {
      await loadUsers();
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar.");
    }
  };

  const handleExport = async () => {
    setError(null);
    setImportResult(null);
    try {
      const response = await fetch(
        `/api/backend/api/v1/admin/users/export?type=CLIENT&format=${importFormat}&country=${selectedCountry}`
      );
      if (!response.ok) {
        throw new Error("No se pudo exportar.");
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      const date = new Date().toISOString().slice(0, 10);
      link.href = url;
      link.download = `clientes-${date}.${importFormat}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err: any) {
      setError(err?.message || "No se pudo exportar.");
    }
  };

  const handleDownloadTemplate = async () => {
    setError(null);
    try {
      const response = await fetch(
        `/api/backend/api/v1/admin/users/template?type=CLIENT&format=${importFormat}&country=${selectedCountry}`
      );
      if (!response.ok) {
        throw new Error("No se pudo descargar la plantilla.");
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `plantilla-clientes.${importFormat}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err: any) {
      setError(err?.message || "No se pudo descargar la plantilla.");
    }
  };

  const handleImport = async () => {
    setError(null);
    setImportResult(null);
    if (!importFile) {
      setError("Selecciona un archivo antes de importar.");
      return;
    }
    try {
      const formData = new FormData();
      formData.append("file", importFile);
      const response = await fetch(
        `/api/backend/api/v1/admin/users/import?type=CLIENT&format=${importFormat}&country=${selectedCountry}`,
        {
          method: "POST",
          body: formData,
        }
      );
      if (!response.ok) {
        const data = await response.json().catch(() => null);
        throw new Error(data?.message || "No se pudo importar.");
      }
      const data = await response.json();
      setImportResult(
        `Importados: ${data.imported} · Omitidos: ${data.skipped}${
          data.errors?.length ? ` · Errores: ${data.errors.length}` : ""
        }`
      );
      setImportFile(null);
      await loadUsers();
    } catch (err: any) {
      setError(err?.message || "No se pudo importar.");
    }
  };

  const resetForm = (tab: TabType, keepPassword: boolean) => {
    setForm({
      firstname: "",
      lastname: "",
      email: "",
      phone: "",
      role: tab === "internal" ? "MANAGER" : "USER",
      customerType: "FINAL",
      enabled: true,
      taxId: "",
      billingEmail: "",
      hasCredit: false,
      creditLimit: "",
      creditDays: "",
      password: keepPassword ? generateTempPassword() : "",
    });
  };

  const handleCreate = (tab?: TabType) => {
    const nextTab = tab || activeTab;
    if (tab) {
      setActiveTab(tab);
    }
    setSelected(null);
    setMode("create");
    setFieldErrors({});
    resetForm(nextTab, true);
  };

  const handleClear = () => {
    handleCreate(activeTab);
  };

  const validateForm = () => {
    const errors: Record<string, string> = {};
    if (!form.firstname.trim()) errors.firstname = "Requerido";
    if (!form.lastname.trim()) errors.lastname = "Requerido";
    if (!form.email.trim()) errors.email = "Requerido";
    if (mode === "create" && !form.password.trim()) errors.password = "Requerido";
    const creditLimit = Number(form.creditLimit || 0);
    const creditDays = Number(form.creditDays || 0);
    if (creditLimit < 0) errors.creditLimit = "No negativo";
    if (creditDays < 0) errors.creditDays = "No negativo";
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = async () => {
    setError(null);
    setNotice(null);
    setTempPassword(null);
    if (!validateForm()) {
      setError("Completa los campos obligatorios.");
      return;
    }
    const isCreate = mode === "create" || !selected;
    const normalizedEmail = normalizeEmail(form.email);
    if (isCreate) {
      try {
        const result = await findUserByEmail(normalizedEmail);
        if (result) {
          setActiveTab(result.tab);
          setSearch(normalizedEmail);
          setPage(0);
          setSelected(result.user);
          setMode("edit");
          setNotice("Email ya existe. Edita el usuario existente.");
          return;
        }
      } catch {
        // If lookup fails, continue with creation and rely on backend validation.
      }
    }
    const payload = {
      firstname: form.firstname,
      lastname: form.lastname,
      email: normalizedEmail,
      phone: form.phone || null,
      type: activeTab === "internal" ? "INTERNAL" : "CLIENT",
      role: form.role,
      customerType: activeTab === "client" ? form.customerType : undefined,
      country: activeTab === "client" ? selectedCountry : undefined,
      enabled: form.enabled,
      taxId: form.taxId || null,
      billingEmail: form.billingEmail || null,
      hasCredit: form.hasCredit,
      creditLimit: form.creditLimit ? Number(form.creditLimit) : 0,
      creditDays: form.creditDays ? Number(form.creditDays) : 0,
      password: isCreate ? form.password : undefined,
    };
    try {
      if (isCreate) {
        const created = await apiFetch<AdminUser>("/api/backend/api/v1/admin/users", {
          method: "POST",
          body: JSON.stringify(payload),
        });
        setSelected(null);
        setMode("create");
        setTempPassword(form.password);
        resetForm(activeTab, true);
        await loadUsers();
        setNotice("Usuario creado.");
      } else if (selected) {
        const updated = await apiFetch<AdminUser>(
          `/api/backend/api/v1/admin/users/${selected.id}`,
          {
            method: "PUT",
            body: JSON.stringify(payload),
          }
        );
        setSelected(updated);
        await loadUsers();
        setNotice("Usuario actualizado.");
      }
    } catch (err: any) {
      const message = err?.message || "No se pudo guardar.";
      if (message.toLowerCase().includes("email already in use")) {
        try {
          const result = await findUserByEmail(normalizedEmail);
          if (result) {
            setActiveTab(result.tab);
            setSearch(normalizedEmail);
            setPage(0);
            setSelected(result.user);
            setMode("edit");
            setNotice("Email ya existe. Edita el usuario existente.");
            return;
          }
        } catch {
          // fall through to show error
        }
      }
      setError(message);
    }
  };

  const handleToggleStatus = async () => {
    if (!selected) return;
    setError(null);
    try {
      const updated = await apiFetch<AdminUser>(
        `/api/backend/api/v1/admin/users/${selected.id}/status${
          activeTab === "client" ? `?country=${selectedCountry}` : ""
        }`,
        {
          method: "PATCH",
          body: JSON.stringify({ enabled: !selected.enabled }),
        }
      );
      setSelected(updated);
      await loadUsers();
      setNotice(updated.enabled ? "Usuario activado." : "Usuario desactivado.");
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar estado.");
    }
  };

  const handleResetPassword = async () => {
    if (!selected) return;
    setError(null);
    try {
      const data = await apiFetch<{ tempPassword: string }>(
        `/api/backend/api/v1/admin/users/${selected.id}/reset-password`,
        { method: "POST" }
      );
      setTempPassword(data.tempPassword);
      setNotice("Password reiniciado.");
    } catch (err: any) {
      setError(err?.message || "No se pudo reiniciar password.");
    }
  };

  const handleViewAr = (userId: string) => {
    router.push(`/admin/ar?clientId=${userId}`);
  };

  const changeTab = (tab: TabType) => {
    setActiveTab(tab);
    setPage(0);
    setSelected(null);
    setMode("edit");
    setTempPassword(null);
    setFieldErrors({});
  };

  const changeCountry = (country: "EC" | "PE") => {
    setSelectedCountry(country);
    setPage(0);
    setSelected(null);
    setMode("edit");
    setTempPassword(null);
    setFieldErrors({});
  };

  const badgeClass = (status: string) => {
    if (status === "BLOQUEADO") return "bg-rose-500/20 text-rose-200 border-rose-500/40";
    if (status === "VENCIDO") return "bg-amber-500/20 text-amber-200 border-amber-500/40";
    return "bg-emerald-500/20 text-emerald-200 border-emerald-500/40";
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="flex">
        <main className="flex-1">
          <AdminHeader
            title="Administracion"
            subtitle="Usuarios y clientes"
            onLogout={handleLogout}
            userEmail={user?.email ?? ""}
            actions={<AdminActionButton icon={<PlusIcon />} onClick={() => handleCreate(activeTab)} label={activeTab === "client" ? "Nuevo cliente" : "Nuevo usuario"} />}
          />

          <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-6 pb-16 pt-4">
            <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-[0_16px_40px_rgba(0,0,0,0.35)]">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-ink/70">Administracion</p>
                <h2 className="text-2xl font-semibold text-ink">Usuarios y clientes</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-400">
                  Gestion centralizada de roles, acceso y creditos con resumen financiero.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <AdminActionButton
                  icon={<RefreshIcon />}
                  variant="ghost"
                  onClick={handleRefresh}
                  label="Actualizar"
                />
              </div>
            </div>

            {error && (
              <div className="rounded-2xl border border-rose-500/40 bg-rose-500/10 px-4 py-3 text-sm text-rose-100">
                {error}
              </div>
            )}
            {notice && (
              <div className="rounded-2xl border border-emerald-400/40 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-100">
                {notice}
              </div>
            )}
            {tempPassword && (
              <div className="rounded-2xl border border-cyan-400/40 bg-cyan-400/10 px-4 py-3 text-sm text-cyan-100">
                Password temporal: {tempPassword}
              </div>
            )}

            <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5 shadow-[0_12px_32px_rgba(0,0,0,0.35)]">
              <div className="flex flex-wrap items-center gap-3">
                <div className="flex gap-2 rounded-full bg-white/10 p-1">
                  {tabOptions.map((tab) => (
                    <button
                      key={tab.key}
                      onClick={() => changeTab(tab.key)}
                      className={`rounded-full px-4 py-2 text-xs uppercase tracking-[0.2em] ${
                        activeTab === tab.key
                          ? "bg-accent text-ink"
                          : "text-ink/70 hover:text-ink"
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>
                {activeTab === "client" && (
                  <div
                    className="flex overflow-hidden rounded-lg border border-white/10 bg-slate-950/70"
                    aria-label="Mercado comercial"
                  >
                    {([
                      { code: "EC", label: "Ecuador", currency: "USD" },
                      { code: "PE", label: "Peru", currency: "PEN" },
                    ] as const).map((market) => (
                      <button
                        key={market.code}
                        type="button"
                        onClick={() => changeCountry(market.code)}
                        className={`min-w-24 px-3 py-2 text-xs font-semibold ${
                          selectedCountry === market.code
                            ? "bg-cyan-500 text-slate-950"
                            : "text-slate-300 hover:bg-white/5 hover:text-white"
                        }`}
                        aria-pressed={selectedCountry === market.code}
                      >
                        {market.label} · {market.currency}
                      </button>
                    ))}
                  </div>
                )}
                <input
                  className="flex-1 rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                  placeholder="Buscar por email, nombre, tax_id o telefono"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      setPage(0);
                      handleRefresh();
                    }
                  }}
                />
                <AdminActionButton variant="ghost" onClick={handleRefresh} label="Buscar" />
              </div>
            </div>

            {activeTab === "client" && (
              <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4 shadow-[0_12px_28px_rgba(0,0,0,0.35)]">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-xs uppercase tracking-[0.3em] text-slate-400">
                      Importar / Exportar clientes
                    </p>
                    <h3 className="text-base font-semibold text-white">Carga masiva y descarga</h3>
                    <p className="mt-1 text-xs text-slate-300">
                      Formato CSV o XLSX. Duplicados por email se omiten.
                    </p>
                    <p className="mt-1 text-[11px] text-slate-400">
                      Columnas: country, firstname, lastname, email, phone, tax_id, billing_email, has_credit,
                      credit_limit, credit_days, enabled, role.
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <select
                      className="rounded-full border border-white/10 bg-slate-900/70 px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-ink"
                      value={importFormat}
                      onChange={(event) =>
                        setImportFormat(event.target.value as "csv" | "xlsx")
                      }
                    >
                      <option value="csv">CSV</option>
                      <option value="xlsx">XLSX</option>
                    </select>
                    <button
                      className="rounded-full border border-white/10 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-ink"
                      onClick={handleDownloadTemplate}
                    >
                      Descargar plantilla
                    </button>
                    <button
                      className="rounded-full bg-white px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-ink shadow-panel"
                      onClick={handleExport}
                    >
                      Exportar
                    </button>
                  </div>
                </div>
                <div className="mt-3 flex flex-wrap items-center gap-2">
                  <input
                    type="file"
                    accept={importFormat === "csv" ? ".csv" : ".xlsx"}
                    className="rounded-full border border-white/10 bg-slate-900/70 px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-ink"
                    onChange={(event) => setImportFile(event.target.files?.[0] || null)}
                  />
                  <button
                    className="rounded-full bg-white px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-ink shadow-panel disabled:opacity-40"
                    onClick={handleImport}
                    disabled={!importFile}
                  >
                    Importar
                  </button>
                </div>
                {importResult && (
                  <div className="mt-3 rounded-2xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-2 text-sm text-emerald-100">
                    {importResult}
                  </div>
                )}
              </div>
            )}

            <div className="grid gap-6 lg:grid-cols-[1.6fr_1.2fr]">
              <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5 shadow-[0_12px_32px_rgba(0,0,0,0.35)]">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-white">Listado</h3>
                  <p className="text-xs text-slate-400">
                    {users.totalElements} resultados
                  </p>
                </div>
                <div className="mt-4 max-h-[32rem] overflow-auto rounded-2xl border border-white/5 bg-slate-950/60">
                  <table className="min-w-full text-sm">
                    <thead className="text-[10px] uppercase tracking-[0.28em] text-slate-400">
                      <tr className="border-b border-white/5">
                        <th className="px-4 py-3 text-left">Usuario</th>
                        <th className="px-4 py-3 text-left">Contacto</th>
                        {activeTab === "internal" ? (
                          <>
                            <th className="px-4 py-3 text-left">Rol</th>
                            <th className="px-4 py-3 text-left">Estado</th>
                          </>
                        ) : (
                          <>
                            <th className="px-4 py-3 text-right">Saldo</th>
                            <th className="px-4 py-3 text-left">Estado</th>
                          </>
                        )}
                        <th className="px-4 py-3 text-right">Accion</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {users.content.map((item) => (
                        <tr key={item.id} className="hover:bg-white/[0.04]">
                          <td className="px-4 py-3">
                            <button
                              className="text-left text-slate-100"
                              onClick={() => {
                                setSelected(item);
                                setMode("edit");
                              }}
                            >
                              <div className="text-sm font-medium text-white">
                                {item.firstname} {item.lastname}
                              </div>
                              <div className="text-xs text-slate-400">{item.email}</div>
                              {activeTab === "client" && (
                                <div
                                  className={`mt-1 text-[11px] ${
                                    item.emailVerified ? "text-emerald-300" : "text-amber-300"
                                  }`}
                                >
                                  {item.emailVerified ? "Correo verificado" : "Correo pendiente"}
                                </div>
                              )}
                            </button>
                          </td>
                          <td className="px-4 py-3 text-xs text-slate-300">
                            <div>{item.phone || "-"}</div>
                            {activeTab === "client" && (
                              <div className="text-[11px] text-slate-500">{item.taxId || "-"}</div>
                            )}
                          </td>
                          {activeTab === "internal" ? (
                            <>
                              <td className="px-4 py-3 text-xs text-slate-200">{item.role}</td>
                              <td className="px-4 py-3">
                                <span
                                  className={`rounded-full border px-2.5 py-1 text-[11px] ${
                                    item.enabled
                                      ? "border-emerald-400/40 bg-emerald-400/10 text-emerald-100"
                                      : "border-rose-400/40 bg-rose-400/10 text-rose-100"
                                  }`}
                                >
                                  {item.enabled ? "Activo" : "Inactivo"}
                                </span>
                              </td>
                            </>
                          ) : (
                            <>
                              <td className="px-4 py-3 text-right">
                                <div className="text-sm text-slate-100">
                                  {formatCurrency(item.arSummary?.saldoActual || 0)}
                                </div>
                                <div className="text-[11px] text-slate-500">
                                  {formatNumber(item.arSummary?.diasAtraso || 0)} dias
                                </div>
                              </td>
                              <td className="px-4 py-3">
                                <span
                                  className={`rounded-full border px-2.5 py-1 text-[11px] ${badgeClass(
                                    item.arSummary?.estado || "AL_DIA"
                                  )}`}
                                >
                                  {item.arSummary?.estado || "AL_DIA"}
                                </span>
                              </td>
                            </>
                          )}
                          <td className="px-4 py-3 text-right">
                            {activeTab === "client" && (
                              <button
                                className="text-xs text-cyan-200 hover:text-cyan-100"
                                onClick={() => handleViewAr(item.id)}
                              >
                                Ver detalle
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                      {users.content.length === 0 && (
                        <tr>
                          <td colSpan={5} className="px-4 py-6 text-center text-sm text-slate-400">
                            No hay registros. Usa la accion Nuevo para crear el primero.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
                <div className="mt-4 flex items-center justify-between text-xs text-slate-400">
                  <span>
                    Pagina {users.number + 1} de {Math.max(users.totalPages, 1)}
                  </span>
                  <div className="flex gap-2">
                    <button
                      className="rounded-full border border-white/10 px-3 py-1 hover:border-white/30"
                      disabled={page <= 0}
                      onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                    >
                      Anterior
                    </button>
                    <button
                      className="rounded-full border border-white/10 px-3 py-1 hover:border-white/30"
                      disabled={page + 1 >= users.totalPages}
                      onClick={() => setPage((prev) => prev + 1)}
                    >
                      Siguiente
                    </button>
                  </div>
                </div>
              </div>

              <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5 shadow-[0_12px_32px_rgba(0,0,0,0.35)]">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-white">
                    {mode === "create"
                      ? activeTab === "client"
                        ? "Nuevo cliente"
                        : "Nuevo usuario interno"
                      : "Editar usuario"}
                  </h3>
                  {mode === "create" && (
                    <span className="rounded-full border border-cyan-400/40 bg-cyan-400/10 px-3 py-1 text-xs text-cyan-100">
                      Formulario listo
                    </span>
                  )}
                </div>
                {mode === "create" && (
                  <div className="mt-3 rounded-2xl border border-amber-400/30 bg-amber-400/10 px-4 py-3 text-sm text-amber-100">
                    Completa los datos y guarda. Este es el unico formulario de usuarios y clientes.
                  </div>
                )}
                <div className="mt-4 space-y-4">
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="label-pill">Nombre</label>
                      <input
                        className={`mt-2 w-full rounded-lg border bg-slate-900/70 px-3 py-2 text-sm ${
                          fieldErrors.firstname ? "border-rose-400/70" : "border-white/10"
                        }`}
                        value={form.firstname}
                        onChange={(event) => setForm((prev) => ({ ...prev, firstname: event.target.value }))}
                      />
                      {fieldErrors.firstname && (
                        <p className="mt-1 text-xs text-rose-200">{fieldErrors.firstname}</p>
                      )}
                    </div>
                    <div>
                      <label className="label-pill">Apellido</label>
                      <input
                        className={`mt-2 w-full rounded-lg border bg-slate-900/70 px-3 py-2 text-sm ${
                          fieldErrors.lastname ? "border-rose-400/70" : "border-white/10"
                        }`}
                        value={form.lastname}
                        onChange={(event) => setForm((prev) => ({ ...prev, lastname: event.target.value }))}
                      />
                      {fieldErrors.lastname && (
                        <p className="mt-1 text-xs text-rose-200">{fieldErrors.lastname}</p>
                      )}
                    </div>
                    <div>
                      <label className="label-pill">Email</label>
                      <input
                        className={`mt-2 w-full rounded-lg border bg-slate-900/70 px-3 py-2 text-sm ${
                          fieldErrors.email ? "border-rose-400/70" : "border-white/10"
                        }`}
                        value={form.email}
                        onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
                      />
                      {fieldErrors.email && (
                        <p className="mt-1 text-xs text-rose-200">{fieldErrors.email}</p>
                      )}
                    </div>
                    <div>
                      <label className="label-pill">Telefono</label>
                      <input
                        className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                        value={form.phone}
                        onChange={(event) => setForm((prev) => ({ ...prev, phone: event.target.value }))}
                      />
                    </div>
                    <div>
                      <label className="label-pill">Rol</label>
                      <select
                        className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                        value={form.role}
                        onChange={(event) => setForm((prev) => ({ ...prev, role: event.target.value }))}
                        disabled={!canEditRole || (mode === "edit" && activeTab === "internal" && !isAdmin)}
                      >
                        {activeTab === "internal" ? (
                          <>
                            <option value="ADMIN">ADMIN</option>
                            <option value="MANAGER">MANAGER</option>
                            <option value="OPERATOR">OPERATOR</option>
                          </>
                        ) : (
                          <>
                            <option value="CUSTOMER">CLIENTE</option>
                            <option value="USER">USUARIO</option>
                          </>
                        )}
                      </select>
                    </div>
                    <div className="flex items-center gap-3 pt-6">
                      <input
                        type="checkbox"
                        checked={form.enabled}
                        onChange={(event) => setForm((prev) => ({ ...prev, enabled: event.target.checked }))}
                      />
                      <span className="text-sm text-slate-200">
                        {activeTab === "client"
                          ? `Cuenta activa en ${selectedCountry === "EC" ? "Ecuador" : "Peru"}`
                          : "Usuario activo"}
                      </span>
                    </div>
                  </div>

                  {activeTab === "client" && (
                    <>
                      {mode === "create" && (
                        <div className="rounded-2xl border border-slate-800/60 bg-slate-950/70 px-4 py-3">
                          <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Datos del cliente</p>
                          <p className="text-sm text-slate-200">
                            Completa tax ID, correo de facturacion y politica de credito si aplica.
                          </p>
                        </div>
                      )}
                      <div className="grid gap-3 md:grid-cols-2">
                        <div>
                          <label className="label-pill">Mercado</label>
                          <div className="mt-2 flex h-10 items-center rounded-lg border border-cyan-400/30 bg-cyan-400/10 px-3 text-sm font-semibold text-cyan-100">
                            {selectedCountry === "EC" ? "Ecuador · USD" : "Peru · PEN"}
                          </div>
                        </div>
                        <div>
                          <label className="label-pill">Tax ID</label>
                          <input
                            className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                            value={form.taxId}
                            onChange={(event) => setForm((prev) => ({ ...prev, taxId: event.target.value }))}
                          />
                        </div>
                        <div>
                          <label className="label-pill">Billing email</label>
                          <input
                            className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                            value={form.billingEmail}
                            onChange={(event) =>
                              setForm((prev) => ({ ...prev, billingEmail: event.target.value }))
                            }
                          />
                        </div>
                        <div>
                          <label className="label-pill">Tipo de cliente</label>
                          <select
                            className="mt-2 w-full rounded-lg border border-white/10 bg-slate-900/70 px-3 py-2 text-sm"
                            value={form.customerType}
                            onChange={(event) =>
                              setForm((prev) => ({
                                ...prev,
                                customerType: event.target.value as "FINAL" | "DISTRIBUTOR",
                              }))
                            }
                          >
                            <option value="FINAL">Cliente final</option>
                            <option value="DISTRIBUTOR">Distribuidor</option>
                          </select>
                        </div>
                      </div>
                      <div className="rounded-2xl border border-slate-800/60 bg-slate-950/70 px-4 py-3">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Credito</p>
                            <p className="text-sm text-slate-200">Configura cupo y dias</p>
                          </div>
                          <div className="flex items-center gap-2">
                            <input
                              type="checkbox"
                              checked={form.hasCredit}
                              onChange={(event) =>
                                setForm((prev) => ({ ...prev, hasCredit: event.target.checked }))
                              }
                            />
                            <span className="text-sm text-slate-200">Tiene credito</span>
                          </div>
                        </div>
                        <div className="mt-3 grid gap-3 md:grid-cols-2">
                          <div>
                          <label className="label-pill">Limite</label>
                          <input
                            type="number"
                            min={0}
                            className={`mt-2 w-full rounded-lg border bg-slate-900/70 px-3 py-2 text-sm ${
                              fieldErrors.creditLimit ? "border-rose-400/70" : "border-white/10"
                            }`}
                            value={form.creditLimit}
                            onChange={(event) =>
                              setForm((prev) => ({ ...prev, creditLimit: event.target.value }))
                            }
                          />
                          {fieldErrors.creditLimit && (
                            <p className="mt-1 text-xs text-rose-200">{fieldErrors.creditLimit}</p>
                          )}
                        </div>
                        <div>
                          <label className="label-pill">Dias credito</label>
                          <input
                            type="number"
                            min={0}
                            className={`mt-2 w-full rounded-lg border bg-slate-900/70 px-3 py-2 text-sm ${
                              fieldErrors.creditDays ? "border-rose-400/70" : "border-white/10"
                            }`}
                            value={form.creditDays}
                            onChange={(event) =>
                              setForm((prev) => ({ ...prev, creditDays: event.target.value }))
                            }
                          />
                          {fieldErrors.creditDays && (
                            <p className="mt-1 text-xs text-rose-200">{fieldErrors.creditDays}</p>
                          )}
                        </div>
                      </div>
                    </div>
                  </>
                  )}

                  {mode === "create" && (
                    <div>
                      <label className="label-pill">Password temporal</label>
                      <div className="mt-2 flex gap-2">
                        <input
                          readOnly
                          className={`w-full rounded-lg border bg-slate-900/70 px-3 py-2 text-sm ${
                            fieldErrors.password ? "border-rose-400/70" : "border-white/10"
                          }`}
                          value={form.password}
                        />
                        <button
                          className="rounded-lg border border-white/10 px-3 text-xs text-slate-300 hover:border-white/30"
                          onClick={() =>
                            setForm((prev) => ({ ...prev, password: generateTempPassword() }))
                          }
                        >
                          Regenerar
                        </button>
                      </div>
                      {fieldErrors.password && (
                        <p className="mt-1 text-xs text-rose-200">{fieldErrors.password}</p>
                      )}
                    </div>
                  )}

                  <div className="flex flex-wrap items-center gap-3">
                    <AdminActionButton onClick={handleSave} label="Guardar" />
                    {mode === "create" && (
                      <AdminActionButton variant="ghost" onClick={handleClear} label="Limpiar" />
                    )}
                    {mode === "edit" && selected && (
                      <>
                        <AdminActionButton
                          variant="ghost"
                          onClick={handleToggleStatus}
                          label={selected.enabled ? "Desactivar" : "Activar"}
                        />
                        {isAdmin && (
                          <AdminActionButton
                            variant="ghost"
                            onClick={handleResetPassword}
                            label="Reset password"
                          />
                        )}
                      </>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>

    </div>
  );
}
