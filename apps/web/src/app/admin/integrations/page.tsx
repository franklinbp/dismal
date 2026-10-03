"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatDateTime } from "@/lib/format";
import { RefreshIcon, SparkIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type OutboxItem = {
  id: string;
  eventType: string;
  aggregateId: string;
  status: string;
  attempts: number;
  lastError: string | null;
  createdAt: string;
  nextAttemptAt: string | null;
};

type IntegrationSettings = {
  id: string | null;
  webhookUrl: string;
  dispatchEnabled: boolean;
  dispatchRateMs: number;
  dispatchMaxAttempts: number;
  hasSecret: boolean;
  smtpEnabled: boolean;
  smtpHost: string | null;
  smtpPort: number | null;
  smtpUser: string | null;
  smtpTls: boolean;
  smtpFromEmail: string | null;
  smtpFromName: string | null;
  hasSmtpPassword: boolean;
  crmWhatsappEnabled: boolean;
  crmWhatsappDefaultId: string | null;
  crmWhatsappIdByCountry: string | null;
  updatedAt: string | null;
};

type NotificationTemplate = {
  id: string;
  eventType: string;
  channel: "EMAIL" | "WHATSAPP";
  subject: string | null;
  body: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
};

const defaultPage: PageResponse<unknown> = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };
const eventTypes = [
  "SALE_CONFIRMED",
  "INVOICE_ISSUED",
  "PAYMENT_RECEIVED",
  "LICENSES_DELIVERED",
  "AR_OVERDUE",
  "SALE_CANCELLED",
  "CAMPAIGN_SCHEDULED",
  "CAMPAIGN_SENT",
  "PRICE_LIST_SHARED",
];

export default function AdminIntegrationsPage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER_OPERATOR");
  const isOperator = user?.role === "OPERATOR";
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [outboxFailed, setOutboxFailed] = useState<PageResponse<OutboxItem>>(
    defaultPage as PageResponse<OutboxItem>
  );
  const [retryingId, setRetryingId] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [settings, setSettings] = useState<IntegrationSettings | null>(null);
  const [settingsForm, setSettingsForm] = useState({
    webhookUrl: "",
    dispatchEnabled: true,
    dispatchRateMs: 5000,
    dispatchMaxAttempts: 10,
    secret: "",
    smtpEnabled: false,
    smtpHost: "",
    smtpPort: "",
    smtpUser: "",
    smtpPassword: "",
    smtpTls: true,
    smtpFromEmail: "",
    smtpFromName: "",
    crmWhatsappEnabled: false,
    crmWhatsappDefaultId: "",
    crmWhatsappIdByCountry: "EC:34",
  });
  const [templates, setTemplates] = useState<NotificationTemplate[]>([]);
  const [templateForm, setTemplateForm] = useState({
    eventType: eventTypes[0],
    channel: "EMAIL" as "EMAIL" | "WHATSAPP",
    subject: "",
    body: "",
    enabled: true,
  });
  const [editingTemplateId, setEditingTemplateId] = useState<string | null>(null);
  const [savingTemplate, setSavingTemplate] = useState(false);
  const [savingSettings, setSavingSettings] = useState(false);
  const [testEmailTo, setTestEmailTo] = useState("");
  const [testEmailSubject, setTestEmailSubject] = useState("Test SMTP - Dismal");
  const [testEmailBody, setTestEmailBody] = useState(
    "Este es un correo de prueba desde Integraciones > Email SMTP en Dismal."
  );
  const [testingEmail, setTestingEmail] = useState(false);
  const [n8nTestTo, setN8nTestTo] = useState("");
  const [n8nTestMessage, setN8nTestMessage] = useState("");
  const [runningWizard, setRunningWizard] = useState(false);

  const integrationGuides = useMemo(
    () => [
      {
        title: "WordPress / WooCommerce -> Dismal",
        tone: "border-sky-200 bg-sky-50/70",
        endpoint: "Compra pagada desde WooCommerce",
        method: "POST",
        auth: "Clave privada configurada solo en servidor",
        tokenSource: "La clave no se muestra en pantalla. Se valida automaticamente en el backend.",
        fields: [
          "Orden",
          "Cliente",
          "Telefono",
          "Productos",
          "Cantidad",
          "Precio",
        ],
        notes: [
          "Recibe compras pagadas desde WooCommerce y genera la venta en Dismal.",
          "Entrega licencias cuando el cliente tiene credito o cuando la venta queda pagada.",
          "Las credenciales se administran fuera de esta pantalla.",
        ],
      },
      {
        title: "Frontends / Apps -> API Dismal",
        tone: "border-emerald-200 bg-emerald-50/70",
        endpoint: "Acceso de panel web y app movil",
        method: "POST",
        auth: "Sesion segura del usuario autenticado",
        tokenSource: "El token de sesion no se muestra ni se reutiliza como clave publica.",
        fields: ["Email", "Password"],
        notes: [
          "Usado por el panel administrativo y la app movil.",
          "Cada usuario opera segun su rol y permisos.",
          "Las integraciones externas no deben usar credenciales personales.",
        ],
      },
      {
        title: "Dismal -> DismalCRM",
        tone: "border-amber-200 bg-amber-50/70",
        endpoint: "Sincronizacion y notificaciones WhatsApp",
        method: "POST",
        auth: "Credenciales privadas configuradas solo en servidor",
        tokenSource: "URL, token y canal WhatsApp se leen desde variables del servidor.",
        fields: ["Clientes", "Campanas", "WhatsApp", "Recordatorios"],
        notes: [
          "Dismal usa DismalCRM para sincronizar clientes y enviar WhatsApp.",
          "Ventas, licencias, listas de precios y recordatorios AR usan esta integracion.",
          "Las credenciales del CRM no deben estar visibles en el panel.",
        ],
      },
    ],
    []
  );

  useEffect(() => {
    if (!allowed) {
      return;
    }
    let active = true;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const outboxData = await apiFetch<PageResponse<OutboxItem>>(
          "/api/backend/api/v1/integrations/outbox?status=FAILED&page=0&size=20"
        );
        if (!active) return;
        setOutboxFailed(outboxData);
        if (!isOperator) {
          const [settingsData, templateData] = await Promise.all([
            apiFetch<IntegrationSettings>("/api/backend/api/v1/integrations/settings"),
            apiFetch<NotificationTemplate[]>("/api/backend/api/v1/integrations/notifications/templates"),
          ]);
          if (!active) return;
          setSettings(settingsData);
          setSettingsForm({
            webhookUrl: settingsData.webhookUrl || "",
            dispatchEnabled: settingsData.dispatchEnabled,
            dispatchRateMs: settingsData.dispatchRateMs,
            dispatchMaxAttempts: settingsData.dispatchMaxAttempts,
            secret: "",
            smtpEnabled: settingsData.smtpEnabled,
            smtpHost: settingsData.smtpHost || "",
            smtpPort: settingsData.smtpPort ? String(settingsData.smtpPort) : "",
            smtpUser: settingsData.smtpUser || "",
            smtpPassword: "",
            smtpTls: settingsData.smtpTls,
            smtpFromEmail: settingsData.smtpFromEmail || "",
            smtpFromName: settingsData.smtpFromName || "",
            crmWhatsappEnabled: settingsData.crmWhatsappEnabled,
            crmWhatsappDefaultId: settingsData.crmWhatsappDefaultId || "",
            crmWhatsappIdByCountry: settingsData.crmWhatsappIdByCountry || "EC:34",
          });
          setTemplates(templateData);
        } else {
          setSettings(null);
          setTemplates([]);
        }
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar integraciones.");
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
  }, [allowed, router, isOperator]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleAction = (label: string) => {
    setNotice(`Accion "${label}" en desarrollo.`);
    setTimeout(() => setNotice(null), 2500);
  };

  const refreshOutbox = async () => {
    const outboxData = await apiFetch<PageResponse<OutboxItem>>(
      "/api/backend/api/v1/integrations/outbox?status=FAILED&page=0&size=20"
    );
    setOutboxFailed(outboxData);
  };

  const handleTestN8n = async () => {
    setError(null);
    if (!n8nTestTo.trim() || !n8nTestMessage.trim()) {
      setError("Completa destino y mensaje para test n8n.");
      return;
    }
    try {
      await apiFetch("/api/backend/api/v1/integrations/n8n/test", {
        method: "POST",
        body: JSON.stringify({ to: n8nTestTo, message: n8nTestMessage }),
      });
      setNotice("Evento de prueba enviado.");
      setTimeout(() => setNotice(null), 2500);
    } catch (err: any) {
      setError(err?.message || "No se pudo enviar el test.");
    }
  };

  const saveSettingsDirect = async (override: Partial<typeof settingsForm>) => {
    setSavingSettings(true);
    setError(null);
    const merged = { ...settingsForm, ...override };
    try {
      const payload: Record<string, unknown> = {
        webhookUrl: merged.webhookUrl,
        dispatchEnabled: merged.dispatchEnabled,
        dispatchRateMs: Number(merged.dispatchRateMs),
        dispatchMaxAttempts: Number(merged.dispatchMaxAttempts),
        smtpEnabled: merged.smtpEnabled,
        smtpHost: merged.smtpHost,
        smtpPort: merged.smtpPort ? Number(merged.smtpPort) : null,
        smtpUser: merged.smtpUser,
        smtpTls: merged.smtpTls,
        smtpFromEmail: merged.smtpFromEmail,
        smtpFromName: merged.smtpFromName,
        crmWhatsappEnabled: merged.crmWhatsappEnabled,
        crmWhatsappDefaultId: merged.crmWhatsappDefaultId,
        crmWhatsappIdByCountry: merged.crmWhatsappIdByCountry,
      };
      if (merged.secret.trim()) {
        payload.secret = merged.secret.trim();
      }
      if (merged.smtpPassword.trim()) {
        payload.smtpPassword = merged.smtpPassword.trim();
      }
      const updated = await apiFetch<IntegrationSettings>("/api/backend/api/v1/integrations/settings", {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      setSettings(updated);
      setSettingsForm({ ...merged, secret: "", smtpPassword: "" });
      setNotice("Configuracion guardada.");
      setTimeout(() => setNotice(null), 2500);
    } catch (err: any) {
      setError(err?.message || "No se pudo guardar configuracion.");
    } finally {
      setSavingSettings(false);
    }
  };

  const handleQuickGmail = async () => {
    await saveSettingsDirect({
      smtpHost: "smtp.gmail.com",
      smtpPort: "587",
      smtpTls: true,
      smtpEnabled: true,
    });
  };

  const upsertTemplate = async (payload: {
    eventType: string;
    channel: "EMAIL" | "WHATSAPP";
    subject: string | null;
    body: string;
    enabled: boolean;
  }) => {
    const existing = templates.find(
      (template) => template.eventType === payload.eventType && template.channel === payload.channel
    );
    if (existing) {
      await apiFetch(`/api/backend/api/v1/integrations/notifications/templates/${existing.id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      return;
    }
    await apiFetch("/api/backend/api/v1/integrations/notifications/templates", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  };

  const refreshTemplatesAndNotify = async (message: string) => {
    const templateData = await apiFetch<NotificationTemplate[]>(
      "/api/backend/api/v1/integrations/notifications/templates"
    );
    setTemplates(templateData);
    setNotice(message);
    setTimeout(() => setNotice(null), 2500);
  };

  const handleQuickSaleTemplate = async () => {
    setError(null);
    setRunningWizard(true);
    try {
      await upsertTemplate({
        eventType: "SALE_CONFIRMED",
        channel: "WHATSAPP",
        subject: null,
        body:
          "Hola {clientName}, tu compra en {companyName} fue confirmada.\nVenta: {saleId}\nTotal: ${total} USD\nEstado: {paymentStatus}\nSoporte: {supportEmail}",
        enabled: true,
      });
      await upsertTemplate({
        eventType: "SALE_CONFIRMED",
        channel: "EMAIL",
        subject: "Compra confirmada en Dismal",
        body:
          "Hola {clientName},\n\nConfirmamos tu compra en {companyName}.\n\nResumen de la venta\nVenta: {saleId}\nFecha: {saleDate}\nEstado de pago: {paymentStatus}\nTotal: ${total} USD\n\nProductos\n{items}\n\nSaldo pendiente: ${balance} USD\nFecha de vencimiento: {dueDate}\n\nTus licencias se enviaran en un correo separado cuando queden asignadas.\nSoporte: {supportEmail}\n\nGracias por comprar en {companyName}.",
        enabled: true,
      });
      await refreshTemplatesAndNotify("Plantillas de venta listas.");
    } catch (err: any) {
      setError(err?.message || "No se pudo crear la plantilla de venta.");
    } finally {
      setRunningWizard(false);
    }
  };

  const handleQuickWhatsAppTemplate = async () => {
    setError(null);
    setRunningWizard(true);
    try {
      await upsertTemplate({
        eventType: "LICENSES_DELIVERED",
        channel: "WHATSAPP",
        subject: null,
        body:
          "Hola {clientName}, tus licencias digitales ya estan listas:\n{licenses}\n\nVenta: {saleId}\nTotal: ${total} USD\nSoporte: {supportEmail}",
        enabled: true,
      });
      await upsertTemplate({
        eventType: "LICENSES_DELIVERED",
        channel: "EMAIL",
        subject: "Tus licencias digitales de Dismal",
        body:
          "Hola {clientName},\n\nTus licencias digitales ya estan listas.\n\nDetalle de licencias\n{licenses}\n\nResumen\nVenta: {saleId}\nTotal: ${total} USD\nFactura: {invoiceNumber}\nEstado de pago: {paymentStatus}\nSaldo pendiente: ${balance} USD\nVence: {dueDate}\n\nRecomendacion: guarda este correo en un lugar seguro.\nSi necesitas ayuda con la activacion, responde este correo o escribe a {supportEmail}.\n\nGracias por comprar en {companyName}.",
        enabled: true,
      });
      await refreshTemplatesAndNotify("Plantillas de licencias listas.");
    } catch (err: any) {
      setError(err?.message || "No se pudo crear la plantilla de licencias.");
    } finally {
      setRunningWizard(false);
    }
  };

  const handleSaveSettings = async () => {
    setSavingSettings(true);
    setError(null);
    try {
      const payload: Record<string, unknown> = {
        webhookUrl: settingsForm.webhookUrl,
        dispatchEnabled: settingsForm.dispatchEnabled,
        dispatchRateMs: Number(settingsForm.dispatchRateMs),
        dispatchMaxAttempts: Number(settingsForm.dispatchMaxAttempts),
        smtpEnabled: settingsForm.smtpEnabled,
        smtpHost: settingsForm.smtpHost,
        smtpPort: settingsForm.smtpPort ? Number(settingsForm.smtpPort) : null,
        smtpUser: settingsForm.smtpUser,
        smtpTls: settingsForm.smtpTls,
        smtpFromEmail: settingsForm.smtpFromEmail,
        smtpFromName: settingsForm.smtpFromName,
      };
      if (settingsForm.secret.trim()) {
        payload.secret = settingsForm.secret.trim();
      }
      if (settingsForm.smtpPassword.trim()) {
        payload.smtpPassword = settingsForm.smtpPassword.trim();
      }
      const updated = await apiFetch<IntegrationSettings>("/api/backend/api/v1/integrations/settings", {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      setSettings(updated);
      setSettingsForm((prev) => ({ ...prev, secret: "", smtpPassword: "" }));
      setNotice("Configuracion guardada.");
      setTimeout(() => setNotice(null), 2500);
    } catch (err: any) {
      setError(err?.message || "No se pudo guardar configuracion.");
    } finally {
      setSavingSettings(false);
    }
  };

  const handleTestEmail = async () => {
    setTestingEmail(true);
    setError(null);
    try {
      const payload = {
        to: testEmailTo || settingsForm.smtpFromEmail || settingsForm.smtpUser || null,
        subject: testEmailSubject,
        body: testEmailBody,
        smtpEnabled: settingsForm.smtpEnabled,
        smtpHost: settingsForm.smtpHost,
        smtpPort: settingsForm.smtpPort ? Number(settingsForm.smtpPort) : null,
        smtpUser: settingsForm.smtpUser,
        smtpPassword: settingsForm.smtpPassword,
        smtpTls: settingsForm.smtpTls,
        smtpFromEmail: settingsForm.smtpFromEmail,
        smtpFromName: settingsForm.smtpFromName,
        crmWhatsappEnabled: settingsForm.crmWhatsappEnabled,
        crmWhatsappDefaultId: settingsForm.crmWhatsappDefaultId,
        crmWhatsappIdByCountry: settingsForm.crmWhatsappIdByCountry,
      };
      await apiFetch("/api/backend/api/v1/integrations/settings/test-email", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setNotice("Email de prueba enviado.");
      setTimeout(() => setNotice(null), 2500);
    } catch (err: any) {
      setError(err?.message || "No se pudo enviar el email de prueba.");
    } finally {
      setTestingEmail(false);
    }
  };

  const resetTemplateForm = () => {
    setEditingTemplateId(null);
    setTemplateForm({
      eventType: eventTypes[0],
      channel: "EMAIL",
      subject: "",
      body: "",
      enabled: true,
    });
  };

  const handleQuickPriceListTemplate = async () => {
    setError(null);
    setRunningWizard(true);
    try {
      await upsertTemplate({
        eventType: "PRICE_LIST_SHARED",
        channel: "WHATSAPP",
        subject: null,
        body:
          "Hola {clientName}, te compartimos nuestra lista de precios actualizada:\n{priceListText}\nSi deseas una recomendacion o pedido, quedamos atentos.",
        enabled: true,
      });
      await refreshTemplatesAndNotify("Plantilla WhatsApp de lista de precios lista.");
    } catch (err: any) {
      setError(err?.message || "No se pudo crear la plantilla de lista de precios.");
    } finally {
      setRunningWizard(false);
    }
  };

  const handleInstallCommercialTemplates = async () => {
    setError(null);
    setRunningWizard(true);
    try {
      await upsertTemplate({
        eventType: "SALE_CONFIRMED",
        channel: "WHATSAPP",
        subject: null,
        body:
          "Hola {clientName}, tu compra en {companyName} fue confirmada.\nVenta: {saleId}\nTotal: ${total} USD\nEstado: {paymentStatus}\nSoporte: {supportEmail}",
        enabled: true,
      });
      await upsertTemplate({
        eventType: "SALE_CONFIRMED",
        channel: "EMAIL",
        subject: "Compra confirmada en Dismal",
        body:
          "Hola {clientName},\n\nConfirmamos tu compra en {companyName}.\n\nResumen de la venta\nVenta: {saleId}\nFecha: {saleDate}\nEstado de pago: {paymentStatus}\nTotal: ${total} USD\n\nProductos\n{items}\n\nSaldo pendiente: ${balance} USD\nFecha de vencimiento: {dueDate}\n\nTus licencias se enviaran en un correo separado cuando queden asignadas.\nSoporte: {supportEmail}\n\nGracias por comprar en {companyName}.",
        enabled: true,
      });
      await upsertTemplate({
        eventType: "LICENSES_DELIVERED",
        channel: "WHATSAPP",
        subject: null,
        body:
          "Hola {clientName}, tus licencias digitales ya estan listas:\n{licenses}\n\nVenta: {saleId}\nTotal: ${total} USD\nSoporte: {supportEmail}",
        enabled: true,
      });
      await upsertTemplate({
        eventType: "LICENSES_DELIVERED",
        channel: "EMAIL",
        subject: "Tus licencias digitales de Dismal",
        body:
          "Hola {clientName},\n\nTus licencias digitales ya estan listas.\n\nDetalle de licencias\n{licenses}\n\nResumen\nVenta: {saleId}\nTotal: ${total} USD\nFactura: {invoiceNumber}\nEstado de pago: {paymentStatus}\nSaldo pendiente: ${balance} USD\nVence: {dueDate}\n\nRecomendacion: guarda este correo en un lugar seguro.\nSi necesitas ayuda con la activacion, responde este correo o escribe a {supportEmail}.\n\nGracias por comprar en {companyName}.",
        enabled: true,
      });
      await upsertTemplate({
        eventType: "PRICE_LIST_SHARED",
        channel: "WHATSAPP",
        subject: null,
        body:
          "Hola {clientName}, te compartimos nuestra lista de precios actualizada:\n{priceListText}\nSi deseas una recomendacion o pedido, quedamos atentos.",
        enabled: true,
      });
      await refreshTemplatesAndNotify("Plantillas comerciales listas.");
    } catch (err: any) {
      setError(err?.message || "No se pudieron instalar las plantillas comerciales.");
    } finally {
      setRunningWizard(false);
    }
  };

  const canTestSmtp =
    settingsForm.smtpEnabled &&
    settingsForm.smtpHost.trim().length > 0 &&
    settingsForm.smtpPort.toString().trim().length > 0 &&
    settingsForm.smtpFromEmail.trim().length > 0;

  const handleSaveTemplate = async () => {
    setSavingTemplate(true);
    setError(null);
    try {
      const payload = {
        eventType: templateForm.eventType,
        channel: templateForm.channel,
        subject: templateForm.channel === "EMAIL" ? templateForm.subject : null,
        body: templateForm.body,
        enabled: templateForm.enabled,
      };
      let updated: NotificationTemplate;
      if (editingTemplateId) {
        updated = await apiFetch<NotificationTemplate>(
          `/api/backend/api/v1/integrations/notifications/templates/${editingTemplateId}`,
          {
            method: "PUT",
            body: JSON.stringify(payload),
          }
        );
        setTemplates((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
      } else {
        updated = await apiFetch<NotificationTemplate>(
          "/api/backend/api/v1/integrations/notifications/templates",
          {
            method: "POST",
            body: JSON.stringify(payload),
          }
        );
        setTemplates((prev) => [updated, ...prev]);
      }
      resetTemplateForm();
      setNotice("Plantilla guardada.");
      setTimeout(() => setNotice(null), 2500);
    } catch (err: any) {
      setError(err?.message || "No se pudo guardar plantilla.");
    } finally {
      setSavingTemplate(false);
    }
  };

  const handleEditTemplate = (template: NotificationTemplate) => {
    setEditingTemplateId(template.id);
    setTemplateForm({
      eventType: template.eventType,
      channel: template.channel,
      subject: template.subject || "",
      body: template.body || "",
      enabled: template.enabled,
    });
  };

  const handleDeleteTemplate = async (templateId: string) => {
    const ok = window.confirm("Eliminar plantilla? Esta accion no se puede deshacer.");
    if (!ok) return;
    setError(null);
    try {
      await apiFetch(`/api/backend/api/v1/integrations/notifications/templates/${templateId}`, {
        method: "DELETE",
      });
      setTemplates((prev) => prev.filter((item) => item.id !== templateId));
      if (editingTemplateId === templateId) {
        resetTemplateForm();
      }
    } catch (err: any) {
      setError(err?.message || "No se pudo eliminar plantilla.");
    }
  };

  const retryOutbox = async (id: string) => {
    setRetryingId(id);
    try {
      await apiFetch(`/api/backend/api/v1/integrations/outbox/${id}/retry`, { method: "POST" });
      await refreshOutbox();
    } catch (err: any) {
      setError(err?.message || "No se pudo reintentar el evento.");
    } finally {
      setRetryingId(null);
    }
  };

  if (authLoading || loading) {
    return (
      <div className="min-h-screen grid place-items-center">
        <div className="rounded-3xl bg-white/70 px-6 py-4 shadow-panel">Cargando integraciones...</div>
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
            title="Integraciones"
            subtitle={isOperator ? "Outbox" : "Configuracion"}
            userEmail={user.email}
            onLogout={handleLogout}
            actions={
              <>
                {!isOperator && (
                  <AdminActionButton
                    icon={<SparkIcon className="h-4 w-4" />}
                    label="Test n8n"
                    variant="primary"
                    onClick={handleTestN8n}
                  />
                )}
                <AdminActionButton
                  icon={<RefreshIcon className="h-4 w-4" />}
                  label="Actualizar"
                  onClick={() => Promise.all([refreshOutbox()])}
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
          <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
            {notice}
          </div>
        )}

        {!isOperator && (
          <section className="mt-10 grid gap-4 lg:grid-cols-3">
            {integrationGuides.map((guide) => (
              <div
                key={guide.title}
                className={`rounded-3xl border p-5 text-sm text-slate/80 shadow-panel ${guide.tone}`}
              >
                <p className="text-xs uppercase tracking-[0.22em] text-slate/50">Referencia tecnica</p>
                <h2 className="mt-2 font-display text-lg text-slate">{guide.title}</h2>
                <div className="mt-4 space-y-3">
                  <div className="rounded-2xl border border-white/70 bg-white/80 p-3">
                    <p className="text-[11px] uppercase tracking-[0.18em] text-slate/50">Flujo</p>
                    <p className="mt-1 break-all font-mono text-xs text-ink">{guide.endpoint}</p>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="rounded-2xl border border-white/70 bg-white/80 p-3">
                      <p className="text-[11px] uppercase tracking-[0.18em] text-slate/50">Metodo</p>
                      <p className="mt-1 font-mono text-xs text-ink">{guide.method}</p>
                    </div>
                    <div className="rounded-2xl border border-white/70 bg-white/80 p-3">
                      <p className="text-[11px] uppercase tracking-[0.18em] text-slate/50">Auth</p>
                      <p className="mt-1 text-xs text-ink">{guide.auth}</p>
                    </div>
                  </div>
                  <div className="rounded-2xl border border-white/70 bg-white/80 p-3">
                    <p className="text-[11px] uppercase tracking-[0.18em] text-slate/50">Seguridad</p>
                    <p className="mt-1 text-xs text-ink">{guide.tokenSource}</p>
                  </div>
                  <div className="rounded-2xl border border-white/70 bg-white/80 p-3">
                    <p className="text-[11px] uppercase tracking-[0.18em] text-slate/50">
                      Datos minimos a enviar o conocer
                    </p>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {guide.fields.map((field) => (
                        <span
                          key={field}
                          className="rounded-full border border-smoke/60 bg-white px-3 py-1 text-[11px] text-slate"
                        >
                          {field}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="rounded-2xl border border-white/70 bg-white/80 p-3">
                    <p className="text-[11px] uppercase tracking-[0.18em] text-slate/50">Notas operativas</p>
                    <div className="mt-2 space-y-1 text-xs text-slate/80">
                      {guide.notes.map((note) => (
                        <p key={note}>{note}</p>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </section>
        )}

        {!isOperator && (
          <section className="mt-10 grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
            <div className="glass-panel rounded-3xl p-6 fade-up delay-1">
              <div className="flex items-center justify-between">
                <h2 className="font-display text-xl text-slate">Conexion n8n</h2>
                {settings?.updatedAt && (
                  <span className="text-xs text-slate/50">
                    Actualizado {formatDateTime(settings.updatedAt)}
                  </span>
                )}
              </div>
              <div className="mt-4 grid gap-4 text-sm text-slate/80">
                <label className="flex flex-col gap-2">
                  Webhook URL
                  <input
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    placeholder="https://n8n.tu-dominio/webhook/..."
                    value={settingsForm.webhookUrl}
                    onChange={(event) =>
                      setSettingsForm((prev) => ({ ...prev, webhookUrl: event.target.value }))
                    }
                  />
                </label>
                <label className="flex flex-col gap-2">
                  Secret (opcional)
                  <input
                    type="password"
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    placeholder={settings?.hasSecret ? "********" : "Sin secret configurado"}
                    value={settingsForm.secret}
                    onChange={(event) => setSettingsForm((prev) => ({ ...prev, secret: event.target.value }))}
                  />
                  <span className="text-xs text-slate/50">
                    {settings?.hasSecret ? "Secret configurado" : "Sin secret guardado"}
                  </span>
                </label>
                <div className="grid gap-3 md:grid-cols-2">
                  <label className="flex flex-col gap-2">
                    Dispatch activo
                    <select
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                      value={settingsForm.dispatchEnabled ? "true" : "false"}
                      onChange={(event) =>
                        setSettingsForm((prev) => ({
                          ...prev,
                          dispatchEnabled: event.target.value === "true",
                        }))
                      }
                    >
                      <option value="true">Activo</option>
                      <option value="false">Pausado</option>
                    </select>
                  </label>
                  <label className="flex flex-col gap-2">
                    Rate (ms)
                    <input
                      type="number"
                      min={1000}
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                      value={settingsForm.dispatchRateMs}
                      onChange={(event) =>
                        setSettingsForm((prev) => ({
                          ...prev,
                          dispatchRateMs: Number(event.target.value),
                        }))
                      }
                    />
                  </label>
                  <label className="flex flex-col gap-2">
                    Max attempts
                    <input
                      type="number"
                      min={1}
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                      value={settingsForm.dispatchMaxAttempts}
                      onChange={(event) =>
                        setSettingsForm((prev) => ({
                          ...prev,
                          dispatchMaxAttempts: Number(event.target.value),
                        }))
                      }
                    />
                  </label>
                </div>
                <div className="rounded-2xl border border-smoke/70 bg-white/80 p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Test n8n (WhatsApp / Email)</p>
                  <div className="mt-3 grid gap-3">
                    <input
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                      placeholder="+593999000000 o correo@dominio.com"
                      value={n8nTestTo}
                      onChange={(event) => setN8nTestTo(event.target.value)}
                    />
                    <textarea
                      className="min-h-[88px] rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                      placeholder="Mensaje de prueba para WhatsApp o Email..."
                      value={n8nTestMessage}
                      onChange={(event) => setN8nTestMessage(event.target.value)}
                    />
                    <div className="flex flex-wrap items-center gap-3">
                      <button
                        type="button"
                        onClick={handleTestN8n}
                        className="rounded-full bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white"
                      >
                        Enviar test n8n
                      </button>
                      <p className="text-xs text-slate/50">
                        Usa canal WHATSAPP en plantillas para enviar licencias.
                      </p>
                    </div>
                  </div>
                </div>
                <div className="rounded-2xl border border-smoke/70 bg-white/80 p-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Configuracion rapida</p>
                  <p className="mt-2 text-sm text-slate/70">
                    Sigue los pasos 1, 2 y 3 para activar Gmail, validar n8n y dejar listas las plantillas comerciales.
                  </p>
                  <div className="mt-3 flex flex-wrap items-center gap-3">
                    <button
                      type="button"
                      onClick={handleQuickGmail}
                      className="rounded-full border border-smoke/70 bg-white px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate"
                    >
                      1. Gmail
                    </button>
                    <button
                      type="button"
                      onClick={handleTestN8n}
                      className="rounded-full border border-smoke/70 bg-white px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate"
                    >
                      2. Test n8n
                    </button>
                    <button
                      type="button"
                      onClick={handleInstallCommercialTemplates}
                      disabled={runningWizard}
                      className="rounded-full bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white disabled:opacity-60"
                    >
                      3. Plantillas comerciales
                    </button>
                    <button
                      type="button"
                      onClick={handleQuickSaleTemplate}
                      disabled={runningWizard}
                      className="rounded-full border border-smoke/70 bg-white px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate disabled:opacity-60"
                    >
                      Venta
                    </button>
                    <button
                      type="button"
                      onClick={handleQuickWhatsAppTemplate}
                      disabled={runningWizard}
                      className="rounded-full border border-smoke/70 bg-white px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate disabled:opacity-60"
                    >
                      Licencias
                    </button>
                    <button
                      type="button"
                      onClick={handleQuickPriceListTemplate}
                      disabled={runningWizard}
                      className="rounded-full border border-smoke/70 bg-white px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate disabled:opacity-60"
                    >
                      Lista de precios
                    </button>
                  </div>
                </div>
                <div className="mt-2 rounded-2xl border border-emerald-200 bg-emerald-50/70 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <h3 className="text-sm font-semibold text-slate">WhatsApp DismalCRM</h3>
                      <p className="mt-1 text-xs text-slate/60">
                        Define que linea de DismalCRM envia ventas, licencias, AR y listas de precios segun el pais.
                      </p>
                    </div>
                    <span className="rounded-full border border-emerald-200 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.18em] text-emerald-700">
                      Sin tocar sesiones
                    </span>
                  </div>
                  <div className="mt-4 grid gap-3 md:grid-cols-2">
                    <label className="flex flex-col gap-2">
                      Estado
                      <select
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={settingsForm.crmWhatsappEnabled ? "true" : "false"}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({
                            ...prev,
                            crmWhatsappEnabled: event.target.value === "true",
                          }))
                        }
                      >
                        <option value="true">Activo</option>
                        <option value="false">Inactivo</option>
                      </select>
                    </label>
                    <label className="flex flex-col gap-2">
                      WhatsApp ID por defecto
                      <input
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        placeholder="34"
                        value={settingsForm.crmWhatsappDefaultId}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({ ...prev, crmWhatsappDefaultId: event.target.value }))
                        }
                      />
                    </label>
                    <label className="flex flex-col gap-2 md:col-span-2">
                      WhatsApp ID por pais
                      <input
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        placeholder="EC:34"
                        value={settingsForm.crmWhatsappIdByCountry}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({ ...prev, crmWhatsappIdByCountry: event.target.value }))
                        }
                      />
                    </label>
                  </div>
                  <div className="mt-3 grid gap-2 rounded-2xl border border-emerald-200 bg-white/80 p-3 text-xs text-slate/70">
                    <p>
                      Ejemplo operativo: <span className="font-semibold">EC:34</span> envia clientes +593 por Dismal Ecuador.
                    </p>
                    <p>
                      Si el pais no se detecta, se usa el ID por defecto. Esta configuracion queda guardada en base de
                      datos y puede cambiarse desde aqui.
                    </p>
                  </div>
                </div>
                <div className="mt-2 rounded-2xl border border-smoke/60 bg-white/80 p-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-slate">Email SMTP</h3>
                    <button
                      type="button"
                      onClick={() =>
                        setSettingsForm((prev) => ({
                          ...prev,
                          smtpHost: "smtp.gmail.com",
                          smtpPort: "587",
                          smtpTls: true,
                        }))
                      }
                      className="rounded-full border border-smoke/60 bg-white px-3 py-1 text-[10px] uppercase tracking-[0.2em] text-slate"
                    >
                      Usar Gmail
                    </button>
                  </div>
                  <div className="mt-3 rounded-2xl border border-emerald-200 bg-emerald-50/80 p-3 text-xs text-emerald-900">
                    Esta configuracion se usa directamente para enviar listas de precios y correos de licencias al
                    cliente desde Dismal, sin depender de <span className="font-semibold">.env</span> ni de n8n.
                  </div>
                  <div className="mt-4 grid gap-3 md:grid-cols-2">
                    <label className="flex flex-col gap-2 md:col-span-2">
                      Habilitado
                      <select
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={settingsForm.smtpEnabled ? "true" : "false"}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({
                            ...prev,
                            smtpEnabled: event.target.value === "true",
                          }))
                        }
                      >
                        <option value="true">Activo</option>
                        <option value="false">Inactivo</option>
                      </select>
                    </label>
                    <label className="flex flex-col gap-2">
                      Host
                      <input
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={settingsForm.smtpHost}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({ ...prev, smtpHost: event.target.value }))
                        }
                      />
                    </label>
                    <label className="flex flex-col gap-2">
                      Puerto
                      <input
                        type="number"
                        min={1}
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={settingsForm.smtpPort}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({ ...prev, smtpPort: event.target.value }))
                        }
                      />
                    </label>
                    <label className="flex flex-col gap-2">
                      Usuario
                      <input
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={settingsForm.smtpUser}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({ ...prev, smtpUser: event.target.value }))
                        }
                      />
                    </label>
                    <label className="flex flex-col gap-2">
                      Password
                      <input
                        type="password"
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        placeholder={settings?.hasSmtpPassword ? "********" : "Sin password"}
                        value={settingsForm.smtpPassword}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({ ...prev, smtpPassword: event.target.value }))
                        }
                      />
                    </label>
                    <label className="flex flex-col gap-2">
                      TLS
                      <select
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={settingsForm.smtpTls ? "true" : "false"}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({
                            ...prev,
                            smtpTls: event.target.value === "true",
                          }))
                        }
                      >
                        <option value="true">Activo</option>
                        <option value="false">Inactivo</option>
                      </select>
                    </label>
                    <label className="flex flex-col gap-2">
                      From email
                      <input
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={settingsForm.smtpFromEmail}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({ ...prev, smtpFromEmail: event.target.value }))
                        }
                      />
                    </label>
                    <label className="flex flex-col gap-2">
                      From name
                      <input
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={settingsForm.smtpFromName}
                        onChange={(event) =>
                          setSettingsForm((prev) => ({ ...prev, smtpFromName: event.target.value }))
                        }
                      />
                    </label>
                  </div>
                  <div className="mt-4 rounded-2xl border border-smoke/70 bg-white/80 p-4">
                    <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Test SMTP</p>
                    <div className="mt-3 grid gap-3">
                      <input
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                        placeholder={settingsForm.smtpFromEmail || "email@dominio.com"}
                        value={testEmailTo}
                        onChange={(event) => setTestEmailTo(event.target.value)}
                      />
                      <input
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                        placeholder="Asunto de prueba"
                        value={testEmailSubject}
                        onChange={(event) => setTestEmailSubject(event.target.value)}
                      />
                      <textarea
                        rows={3}
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                        placeholder="Mensaje de prueba"
                        value={testEmailBody}
                        onChange={(event) => setTestEmailBody(event.target.value)}
                      />
                      <div className="flex flex-wrap items-center gap-3">
                      <button
                        type="button"
                        onClick={handleTestEmail}
                        disabled={testingEmail || !canTestSmtp}
                        className="rounded-full bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white disabled:opacity-60"
                      >
                        {testingEmail ? "Probando..." : "Probar email"}
                      </button>
                        <button
                          type="button"
                          onClick={handleSaveSettings}
                          disabled={savingSettings}
                          className="rounded-full border border-smoke/60 bg-white px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate disabled:opacity-60"
                        >
                          {savingSettings ? "Guardando" : "Guardar SMTP"}
                        </button>
                      </div>
                    </div>
                    <p className="mt-2 text-xs text-slate/50">
                      La prueba usa la configuracion que ves en pantalla. Si no escribes un correo, se usara el From
                      email configurado.
                    </p>
                  </div>
                </div>
                <button
                  onClick={handleSaveSettings}
                  disabled={savingSettings}
                  className="rounded-full bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white disabled:opacity-60"
                >
                  {savingSettings ? "Guardando" : "Guardar configuracion"}
                </button>
              </div>
            </div>

            <div className="glass-panel rounded-3xl p-6 fade-up delay-2">
              <div className="flex items-center justify-between">
                <h2 className="font-display text-xl text-slate">Plantillas</h2>
                <button
                  onClick={resetTemplateForm}
                  className="rounded-full border border-smoke/70 bg-white px-3 py-1 text-xs uppercase tracking-[0.2em] text-slate"
                >
                  Nueva
                </button>
              </div>
              <div className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
                <div className="space-y-3">
                  {templates.map((template) => (
                    <button
                      key={template.id}
                      onClick={() => handleEditTemplate(template)}
                      className={`w-full rounded-2xl border px-4 py-3 text-left transition ${
                        editingTemplateId === template.id
                          ? "border-ink bg-ink/90 text-white"
                          : "border-smoke/60 bg-white/80 text-slate hover:bg-white"
                      }`}
                    >
                      <p className="text-xs uppercase text-slate/50">{template.eventType}</p>
                      <p className="text-sm font-medium">
                        {template.channel} {template.enabled ? "• Activo" : "• Pausado"}
                      </p>
                      <p className="text-xs text-slate/50">{formatDateTime(template.updatedAt)}</p>
                    </button>
                  ))}
                  {templates.length === 0 && (
                    <div className="rounded-2xl border border-dashed border-smoke/60 p-4 text-center text-sm text-slate/60">
                      No hay plantillas aun.
                    </div>
                  )}
                </div>

                <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4 text-sm text-slate/80">
                  <div className="grid gap-3">
                    <label className="flex flex-col gap-2">
                      Evento
                      <select
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={templateForm.eventType}
                        onChange={(event) =>
                          setTemplateForm((prev) => ({ ...prev, eventType: event.target.value }))
                        }
                      >
                        {eventTypes.map((type) => (
                          <option key={type} value={type}>
                            {type}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label className="flex flex-col gap-2">
                      Canal
                      <select
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={templateForm.channel}
                        onChange={(event) =>
                          setTemplateForm((prev) => ({
                            ...prev,
                            channel: event.target.value as "EMAIL" | "WHATSAPP",
                          }))
                        }
                      >
                        <option value="EMAIL">EMAIL</option>
                        <option value="WHATSAPP">WHATSAPP</option>
                      </select>
                    </label>
                    {templateForm.channel === "EMAIL" && (
                      <label className="flex flex-col gap-2">
                        Subject
                        <input
                          className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                          value={templateForm.subject}
                          onChange={(event) =>
                            setTemplateForm((prev) => ({ ...prev, subject: event.target.value }))
                          }
                        />
                      </label>
                    )}
                    <label className="flex flex-col gap-2">
                      Body
                      <textarea
                        rows={5}
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                        value={templateForm.body}
                        onChange={(event) =>
                          setTemplateForm((prev) => ({ ...prev, body: event.target.value }))
                        }
                      />
                    </label>
                    <label className="flex items-center gap-2 text-xs uppercase tracking-[0.2em] text-slate/60">
                      <input
                        type="checkbox"
                        checked={templateForm.enabled}
                        onChange={(event) =>
                          setTemplateForm((prev) => ({ ...prev, enabled: event.target.checked }))
                        }
                      />
                      Activo
                    </label>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={handleSaveTemplate}
                        disabled={savingTemplate}
                        className="rounded-full bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white disabled:opacity-60"
                      >
                        {savingTemplate ? "Guardando" : editingTemplateId ? "Actualizar" : "Crear"}
                      </button>
                      {editingTemplateId && (
                        <button
                          onClick={() => handleDeleteTemplate(editingTemplateId)}
                          className="rounded-full border border-red-200 bg-red-50 px-4 py-2 text-xs uppercase tracking-[0.2em] text-red-700"
                        >
                          Eliminar
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>
        )}

        <section className="mt-10 glass-panel rounded-3xl p-6 fade-up delay-3">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-xl text-slate">Eventos fallidos</h2>
            <span className="text-xs uppercase tracking-[0.2em] text-slate/50">
              Total {outboxFailed.totalElements}
            </span>
          </div>
          <div className="mt-4 space-y-3">
            {outboxFailed.content.map((item) => (
              <div key={item.id} className="rounded-2xl border border-smoke/60 bg-white/80 p-4 soft-shadow">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-xs uppercase text-slate/50">{item.eventType}</p>
                    <p className="text-sm font-medium text-slate">{item.aggregateId}</p>
                    <p className="text-xs text-slate/50">{formatDateTime(item.createdAt)}</p>
                  </div>
                  <button
                    onClick={() => retryOutbox(item.id)}
                    disabled={retryingId === item.id}
                    className="rounded-full bg-accent px-4 py-1 text-xs uppercase tracking-[0.2em] text-white disabled:opacity-60"
                  >
                    {retryingId === item.id ? "Reintentando" : "Reintentar"}
                  </button>
                </div>
                <p className="mt-2 text-xs text-slate/60">{item.lastError || "Sin detalle"}</p>
                <p className="mt-2 text-xs text-slate/50">
                  Intentos: {item.attempts} | Proximo: {formatDateTime(item.nextAttemptAt)}
                </p>
              </div>
            ))}
            {outboxFailed.content.length === 0 && (
              <div className="rounded-2xl border border-dashed border-smoke/60 p-6 text-center text-sm text-slate/60">
                No hay eventos fallidos en este momento.
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
