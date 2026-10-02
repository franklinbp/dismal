"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatDateTime } from "@/lib/format";
import { PlusIcon, RefreshIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

const campaignStatuses = ["DRAFT", "SCHEDULED", "SENT", "FAILED", "CANCELLED"] as const;
const channels = ["EMAIL", "WHATSAPP"] as const;
const roles = ["CUSTOMER", "USER"] as const;

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type Product = {
  id: string;
  name: string;
};

type Customer = {
  id: string;
  firstname: string;
  lastname: string;
  email: string;
};

type Campaign = {
  id: string;
  title: string;
  messageBody: string;
  imageUrl: string | null;
  targetRole: string;
  channel: string;
  productId: string | null;
  productName: string | null;
  scheduledAt: string;
  status: string;
  createdAt: string;
  updatedAt: string;
};

const defaultPage: PageResponse<unknown> = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

export default function AdminCampaignsPage() {
  const router = useRouter();
  const { user, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [campaigns, setCampaigns] = useState<PageResponse<Campaign>>(defaultPage as PageResponse<Campaign>);
  const [selected, setSelected] = useState<Campaign | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);

  const [filters, setFilters] = useState({ status: "" });

  const [form, setForm] = useState({
    title: "",
    messageBody: "",
    imageUrl: "",
    targetRole: "CUSTOMER",
    channel: "EMAIL",
    productId: "",
    scheduledAt: "",
  });

  const [testForm, setTestForm] = useState({
    clientId: "",
    channel: "EMAIL",
  });

  const canSchedule = useMemo(() => selected && selected.status !== "SENT", [selected]);

  const loadCampaigns = useCallback(async () => {
    const params = new URLSearchParams();
    if (filters.status) params.set("status", filters.status);
    params.set("page", "0");
    params.set("size", "25");
    const data = await apiFetch<PageResponse<Campaign>>(
      `/api/backend/api/v1/marketing/campaigns?${params.toString()}`
    );
    setCampaigns(data);
    if (!selected && data.content.length > 0) {
      setSelected(data.content[0]);
    }
  }, [filters.status, selected]);

  const loadAux = useCallback(async () => {
    const [productData, customerData] = await Promise.all([
      apiFetch<Product[]>("/api/backend/api/v1/store/products"),
      apiFetch<Customer[]>("/api/backend/api/v1/customers"),
    ]);
    setProducts(productData);
    setCustomers(customerData);
  }, []);

  useEffect(() => {
    const boot = async () => {
      if (!allowed || !user) return;
      setError(null);
      try {
        await Promise.all([loadCampaigns(), loadAux()]);
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar campanas.");
      }
    };
    boot();
  }, [allowed, user, loadCampaigns, loadAux, router]);

  useEffect(() => {
    if (!selected) return;
    setForm({
      title: selected.title || "",
      messageBody: selected.messageBody || "",
      imageUrl: selected.imageUrl || "",
      targetRole: selected.targetRole || "CUSTOMER",
      channel: selected.channel || "EMAIL",
      productId: selected.productId || "",
      scheduledAt: selected.scheduledAt ? selected.scheduledAt.slice(0, 16) : "",
    });
  }, [selected]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleCreate = async () => {
    setError(null);
    try {
      const payload = {
        title: form.title,
        messageBody: form.messageBody,
        imageUrl: form.imageUrl || null,
        targetRole: form.targetRole,
        channel: form.channel,
        productId: form.productId || null,
        scheduledAt: form.scheduledAt ? new Date(form.scheduledAt).toISOString() : null,
      };
      const created = await apiFetch<Campaign>("/api/backend/api/v1/marketing/campaigns", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setSelected(created);
      await loadCampaigns();
      setNotice("Campana creada.");
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo crear campana.");
    }
  };

  const handleUpdate = async () => {
    if (!selected) return;
    setError(null);
    try {
      const payload = {
        title: form.title,
        messageBody: form.messageBody,
        imageUrl: form.imageUrl || null,
        targetRole: form.targetRole,
        channel: form.channel,
        productId: form.productId || null,
        scheduledAt: form.scheduledAt ? new Date(form.scheduledAt).toISOString() : selected.scheduledAt,
        status: selected.status,
      };
      const updated = await apiFetch<Campaign>(
        `/api/backend/api/v1/marketing/campaigns/${selected.id}`,
        {
          method: "PUT",
          body: JSON.stringify(payload),
        }
      );
      setSelected(updated);
      await loadCampaigns();
      setNotice("Campana actualizada.");
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar campana.");
    }
  };

  const handleSchedule = async () => {
    if (!selected) return;
    setError(null);
    try {
      const payload = {
        scheduledAt: form.scheduledAt ? new Date(form.scheduledAt).toISOString() : null,
      };
      const updated = await apiFetch<Campaign>(
        `/api/backend/api/v1/marketing/campaigns/${selected.id}/schedule`,
        { method: "POST", body: JSON.stringify(payload) }
      );
      setSelected(updated);
      await loadCampaigns();
      setNotice("Campana programada.");
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo programar campana.");
    }
  };

  const handleSendTest = async () => {
    if (!selected) return;
    setError(null);
    try {
      const payload = {
        clientId: testForm.clientId,
        channel: testForm.channel,
      };
      await apiFetch(`/api/backend/api/v1/marketing/campaigns/${selected.id}/send-test`, {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setNotice("Test enviado.");
      setTimeout(() => setNotice(null), 2000);
    } catch (err: any) {
      setError(err?.message || "No se pudo enviar test.");
    }
  };

  const handleRefresh = async () => {
    setError(null);
    try {
      await loadCampaigns();
    } catch (err: any) {
      setError(err?.message || "No se pudo recargar.");
    }
  };

  return (
    <div className="min-h-screen bg-paper text-slate">
      <div className="flex">
        <main className="flex-1">
          <AdminHeader
            title="Campañas"
            subtitle="Convierte alertas MIP en ventas rápidas"
            onLogout={handleLogout}
            userEmail={user?.email ?? ""}
          />

          <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-6 pb-16 pt-4">
            <div className="glass-panel hero-band rounded-[32px] px-6 py-6 fade-up">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-slate-500">Marketing</p>
                <h2 className="text-2xl font-semibold text-slate">Campañas integradas</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate/70">
                  Crea campañas rápidas en Email y WhatsApp. Para Facebook/TikTok te damos la guía de acción.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <AdminActionButton icon={<RefreshIcon />} variant="ghost" onClick={handleRefresh}>
                  Actualizar
                </AdminActionButton>
                <AdminActionButton icon={<PlusIcon />} onClick={handleCreate}>
                  Crear campaña
                </AdminActionButton>
              </div>
            </div>

            {error && (
              <div className="glass-card border border-rose-500/40 bg-rose-500/10 px-4 py-3 text-sm text-rose-700">
                {error}
              </div>
            )}
            {notice && (
              <div className="glass-card border border-emerald-400/40 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-700">
                {notice}
              </div>
            )}

            <div className="grid gap-4 lg:grid-cols-[1.2fr_1fr]">
              <div className="glass-panel p-5">
                <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Guía rápida</p>
                <div className="mt-3 grid gap-3 text-sm text-slate/70">
                  <div className="flex items-start gap-3">
                    <span className="status-pill rounded-full px-2 py-0.5 text-[11px] text-slate/70">1</span>
                    <p>Elige el producto con alerta MIP (baja tracción o pausado).</p>
                  </div>
                  <div className="flex items-start gap-3">
                    <span className="status-pill rounded-full px-2 py-0.5 text-[11px] text-slate/70">2</span>
                    <p>Redacta un mensaje corto con beneficio + llamada a la acción.</p>
                  </div>
                  <div className="flex items-start gap-3">
                    <span className="status-pill rounded-full px-2 py-0.5 text-[11px] text-slate/70">3</span>
                    <p>Envía test, programa, y monitorea el estado.</p>
                  </div>
                </div>
              </div>
              <div className="glass-panel p-5">
                <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Canales recomendados</p>
                <div className="mt-4 grid gap-3 md:grid-cols-3 text-xs text-slate/70">
                  <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                    <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Facebook Ads</p>
                    <p className="mt-2 text-sm text-slate/70">Anuncio pago para tráfico o mensajes.</p>
                    <p className="mt-2 text-[11px] text-slate/50">Acción: crear campaña con objetivo “mensajes”.</p>
                  </div>
                  <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                    <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Grupos</p>
                    <p className="mt-2 text-sm text-slate/70">Publicación orgánica en grupos relevantes.</p>
                    <p className="mt-2 text-[11px] text-slate/50">Acción: lista de grupos + calendario.</p>
                  </div>
                  <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                    <p className="text-xs uppercase tracking-[0.2em] text-slate/50">TikTok Ads</p>
                    <p className="mt-2 text-sm text-slate/70">Video corto + presupuesto diario.</p>
                    <p className="mt-2 text-[11px] text-slate/50">Acción: anuncio con CTA a WhatsApp.</p>
                  </div>
                </div>
                <p className="mt-3 text-xs text-slate/50">
                  Nota: Facebook/TikTok son externos y no se automatizan aquí. Email/WhatsApp sí.
                </p>
              </div>
            </div>

            <div className="glass-panel p-5">
              <p className="text-xs uppercase tracking-[0.3em] text-slate-500">¿Cuándo aplicar campañas?</p>
              <div className="mt-4 grid gap-3 md:grid-cols-2 text-sm text-slate/70">
                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Baja tracción</p>
                  <p className="mt-2">Campaña de empuje: más visibilidad y urgencia.</p>
                </div>
                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Alerta comercial</p>
                  <p className="mt-2">Campaña de confianza: testimonios, garantía, demo.</p>
                </div>
                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Pausado</p>
                  <p className="mt-2">Campaña de relanzamiento: nueva oferta o mensaje.</p>
                </div>
                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Rentable</p>
                  <p className="mt-2">Campaña de expansión: escalar ventas con el stock disponible.</p>
                </div>
              </div>
            </div>

            <div className="grid gap-6 lg:grid-cols-[1.2fr_1.8fr]">
              <div className="glass-panel p-5">
                <div className="flex flex-wrap items-center gap-3">
                  <select
                    className="min-w-[180px] rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                    value={filters.status}
                    onChange={(event) => setFilters({ status: event.target.value })}
                  >
                    <option value="">Todos los estados</option>
                    {campaignStatuses.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                  <AdminActionButton variant="ghost" onClick={handleRefresh}>
                    Aplicar
                  </AdminActionButton>
                </div>

                <div className="mt-4 space-y-3">
                  {campaigns.content.length === 0 && (
                    <p className="text-sm text-slate/60">No hay campañas registradas.</p>
                  )}
                  {campaigns.content.map((campaign) => (
                    <button
                      key={campaign.id}
                      className={`glass-card w-full px-4 py-3 text-left transition ${
                        selected?.id === campaign.id ? "border-accent/60" : "border-smoke/60"
                      }`}
                      onClick={() => setSelected(campaign)}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm font-semibold text-slate">{campaign.title}</p>
                          <p className="text-xs text-slate/60">{campaign.channel}</p>
                        </div>
                        <span className="status-pill text-xs">{campaign.status}</span>
                      </div>
                      <div className="mt-2 flex items-center justify-between text-xs text-slate/60">
                        <span>{formatDateTime(campaign.scheduledAt)}</span>
                        <span>{campaign.productName || "Sin producto"}</span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>

              <div className="glass-panel p-5">
                <h3 className="text-lg font-semibold text-slate">Detalle de campaña</h3>
                {!selected ? (
                  <p className="mt-4 text-sm text-slate/60">Selecciona una campaña para editar.</p>
                ) : (
                  <div className="mt-4 space-y-4">
                    <div className="grid gap-4 md:grid-cols-2">
                      <div>
                        <label className="label-pill">Titulo</label>
                        <input
                          className="mt-2 w-full rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                          value={form.title}
                          onChange={(event) => setForm((prev) => ({ ...prev, title: event.target.value }))}
                        />
                      </div>
                      <div>
                        <label className="label-pill">Canal</label>
                        <select
                          className="mt-2 w-full rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                          value={form.channel}
                          onChange={(event) => setForm((prev) => ({ ...prev, channel: event.target.value }))}
                        >
                          {channels.map((channel) => (
                            <option key={channel} value={channel}>
                              {channel}
                            </option>
                          ))}
                        </select>
                        <p className="mt-2 text-[11px] text-slate/60">
                          Email y WhatsApp se envían desde aquí. Facebook/TikTok se ejecutan fuera del sistema.
                        </p>
                      </div>
                      <div>
                        <label className="label-pill">Segmento</label>
                        <select
                          className="mt-2 w-full rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                          value={form.targetRole}
                          onChange={(event) => setForm((prev) => ({ ...prev, targetRole: event.target.value }))}
                        >
                          {roles.map((role) => (
                            <option key={role} value={role}>
                              {role}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="label-pill">Producto</label>
                        <select
                          className="mt-2 w-full rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                          value={form.productId}
                          onChange={(event) => setForm((prev) => ({ ...prev, productId: event.target.value }))}
                        >
                          <option value="">Sin producto</option>
                          {products.map((product) => (
                            <option key={product.id} value={product.id}>
                              {product.name}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="label-pill">Programar</label>
                        <input
                          type="datetime-local"
                          className="mt-2 w-full rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                          value={form.scheduledAt}
                          onChange={(event) => setForm((prev) => ({ ...prev, scheduledAt: event.target.value }))}
                        />
                      </div>
                      <div>
                        <label className="label-pill">Imagen</label>
                        <input
                          className="mt-2 w-full rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                          value={form.imageUrl}
                          onChange={(event) => setForm((prev) => ({ ...prev, imageUrl: event.target.value }))}
                        />
                      </div>
                    </div>
                    <div>
                      <label className="label-pill">Mensaje</label>
                      <textarea
                        className="mt-2 w-full rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                        rows={4}
                        value={form.messageBody}
                        onChange={(event) => setForm((prev) => ({ ...prev, messageBody: event.target.value }))}
                      />
                    </div>
                    <div className="flex flex-wrap items-center gap-3">
                      <AdminActionButton onClick={handleUpdate}>Guardar cambios</AdminActionButton>
                      <AdminActionButton variant="ghost" onClick={handleSchedule} disabled={!canSchedule}>
                        Programar
                      </AdminActionButton>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="glass-panel p-5">
              <h3 className="text-lg font-semibold text-slate">Enviar test</h3>
              <div className="mt-4 grid gap-4 md:grid-cols-[2fr_1fr_auto]">
                <select
                  className="rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                  value={testForm.clientId}
                  onChange={(event) => setTestForm((prev) => ({ ...prev, clientId: event.target.value }))}
                >
                  <option value="">Selecciona cliente</option>
                  {customers.map((customer) => (
                    <option key={customer.id} value={customer.id}>
                      {customer.firstname} {customer.lastname} - {customer.email}
                    </option>
                  ))}
                </select>
                <select
                  className="rounded-lg border border-smoke/60 bg-white px-3 py-2 text-sm"
                  value={testForm.channel}
                  onChange={(event) => setTestForm((prev) => ({ ...prev, channel: event.target.value }))}
                >
                  {channels.map((channel) => (
                    <option key={channel} value={channel}>
                      {channel}
                    </option>
                  ))}
                </select>
                <AdminActionButton variant="ghost" onClick={handleSendTest}>
                  Enviar test
                </AdminActionButton>
              </div>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
