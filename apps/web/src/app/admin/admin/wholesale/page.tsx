"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import AdminActionButton from "@/components/admin/AdminActionButton";
import AdminHeader from "@/components/admin/AdminHeader";
import { RefreshIcon, UsersIcon } from "@/components/admin/icons";
import { useAdminGuard } from "@/components/admin/useAdminGuard";
import { apiFetch, type ApiError } from "@/lib/api";
import { formatDateTime } from "@/lib/format";

type ApplicationStatus = "PENDING" | "APPROVED" | "REJECTED";

type WholesaleApplication = {
  id: string;
  userId: string;
  customerEmail: string;
  customerName: string;
  country: "EC" | "PE";
  businessName: string;
  taxId: string;
  phone: string;
  website?: string | null;
  notes?: string | null;
  status: ApplicationStatus;
  reviewNotes?: string | null;
  reviewedAt?: string | null;
  createdAt: string;
};

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

const emptyPage: PageResponse<WholesaleApplication> = {
  content: [], totalElements: 0, totalPages: 0, number: 0, size: 20,
};

const statusLabels: Record<ApplicationStatus, string> = {
  PENDING: "Pendientes",
  APPROVED: "Aprobadas",
  REJECTED: "Rechazadas",
};

export default function AdminWholesalePage() {
  const router = useRouter();
  const { user, loading: authLoading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const [status, setStatus] = useState<ApplicationStatus>("PENDING");
  const [page, setPage] = useState(0);
  const [applications, setApplications] = useState(emptyPage);
  const [selected, setSelected] = useState<WholesaleApplication | null>(null);
  const [reviewNotes, setReviewNotes] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadApplications = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiFetch<PageResponse<WholesaleApplication>>(
        `/api/backend/api/v1/admin/wholesale-applications?status=${status}&page=${page}&size=20`
      );
      setApplications(result);
      setSelected((current) => {
        const retained = result.content.find((item) => item.id === current?.id);
        return retained || result.content[0] || null;
      });
    } catch (err) {
      setError((err as ApiError)?.message || "No se pudieron cargar las solicitudes.");
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    if (allowed) void loadApplications();
  }, [allowed, loadApplications]);

  useEffect(() => {
    setPage(0);
    setSelected(null);
    setReviewNotes("");
  }, [status]);

  const review = async (nextStatus: Exclude<ApplicationStatus, "PENDING">) => {
    if (!selected) return;
    setSubmitting(true);
    setError(null);
    setNotice(null);
    try {
      await apiFetch(`/api/backend/api/v1/admin/wholesale-applications/${selected.id}`, {
        method: "PATCH",
        body: JSON.stringify({ status: nextStatus, reviewNotes: reviewNotes.trim() || null }),
      });
      setNotice(nextStatus === "APPROVED"
        ? "Cuenta mayorista aprobada y tarifa comercial habilitada."
        : "Solicitud rechazada correctamente.");
      setReviewNotes("");
      await loadApplications();
    } catch (err) {
      setError((err as ApiError)?.message || "No se pudo revisar la solicitud.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  if (authLoading || !allowed || !user) {
    return <div className="p-8 text-sm text-slate/70">Validando acceso...</div>;
  }

  return (
    <main className="space-y-6 p-5 md:p-8">
      <AdminHeader
        title="Solicitudes mayoristas"
        subtitle="Clientes y distribuidores"
        userEmail={user.email}
        onLogout={handleLogout}
        actions={
          <AdminActionButton
            icon={<RefreshIcon className="h-4 w-4" />}
            label="Actualizar"
            onClick={() => void loadApplications()}
            disabled={loading}
          />
        }
      />

      <section className="border-y border-smoke/80 bg-white/70 py-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="inline-flex border border-smoke bg-white p-1" role="group" aria-label="Estado de solicitud">
            {(Object.keys(statusLabels) as ApplicationStatus[]).map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setStatus(item)}
                className={`min-h-9 px-4 text-xs font-semibold ${status === item ? "bg-ink text-white" : "text-slate hover:bg-paper"}`}
              >
                {statusLabels[item]}
              </button>
            ))}
          </div>
          <p className="text-sm text-slate/70">{applications.totalElements} solicitudes</p>
        </div>
      </section>

      {error ? <div className="border-l-4 border-red-500 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">{error}</div> : null}
      {notice ? <div className="border-l-4 border-emerald-600 bg-emerald-50 px-4 py-3 text-sm text-emerald-800" role="status">{notice}</div> : null}

      <div className="grid min-h-[520px] gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,0.65fr)]">
        <section className="overflow-hidden border border-smoke bg-white">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead className="bg-paper text-[11px] uppercase text-slate/60">
                <tr>
                  <th className="px-4 py-3">Negocio</th>
                  <th className="px-4 py-3">Cliente</th>
                  <th className="px-4 py-3">País</th>
                  <th className="px-4 py-3">Solicitud</th>
                </tr>
              </thead>
              <tbody>
                {applications.content.map((application) => (
                  <tr
                    key={application.id}
                    onClick={() => { setSelected(application); setReviewNotes(application.reviewNotes || ""); }}
                    className={`cursor-pointer border-t border-smoke transition ${selected?.id === application.id ? "bg-cyan-50" : "hover:bg-paper/70"}`}
                  >
                    <td className="px-4 py-3 font-semibold text-ink">{application.businessName}</td>
                    <td className="px-4 py-3"><span className="block text-ink">{application.customerName || "Sin nombre"}</span><span className="text-xs text-slate/60">{application.customerEmail}</span></td>
                    <td className="px-4 py-3">{application.country}</td>
                    <td className="px-4 py-3 text-xs text-slate/70">{formatDateTime(application.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!loading && applications.content.length === 0 ? (
            <div className="grid min-h-72 place-items-center px-6 text-center text-sm text-slate/65">
              <div><UsersIcon className="mx-auto mb-3 h-8 w-8" /><p>No hay solicitudes en este estado.</p></div>
            </div>
          ) : null}
          {loading ? <div className="p-6 text-sm text-slate/65">Cargando solicitudes...</div> : null}
          {applications.totalPages > 1 ? (
            <div className="flex items-center justify-between border-t border-smoke px-4 py-3 text-sm">
              <button type="button" className="btn btn-secondary" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Anterior</button>
              <span>Página {page + 1} de {applications.totalPages}</span>
              <button type="button" className="btn btn-secondary" disabled={page + 1 >= applications.totalPages} onClick={() => setPage((value) => value + 1)}>Siguiente</button>
            </div>
          ) : null}
        </section>

        <aside className="border border-smoke bg-white p-5">
          {selected ? (
            <div className="space-y-5">
              <div><p className="text-[11px] uppercase text-slate/55">Solicitud seleccionada</p><h2 className="mt-1 font-display text-2xl text-ink">{selected.businessName}</h2></div>
              <dl className="grid grid-cols-[110px_minmax(0,1fr)] gap-x-3 gap-y-3 text-sm">
                <dt className="text-slate/60">Cliente</dt><dd className="break-words text-ink">{selected.customerName || selected.customerEmail}</dd>
                <dt className="text-slate/60">Correo</dt><dd className="break-all text-ink">{selected.customerEmail}</dd>
                <dt className="text-slate/60">RUC / ID</dt><dd className="text-ink">{selected.taxId}</dd>
                <dt className="text-slate/60">WhatsApp</dt><dd className="text-ink">{selected.phone}</dd>
                <dt className="text-slate/60">País</dt><dd className="text-ink">{selected.country}</dd>
                <dt className="text-slate/60">Sitio</dt><dd className="break-all text-ink">{selected.website || "No registrado"}</dd>
              </dl>
              {selected.notes ? <div className="border-y border-smoke py-4"><p className="text-xs font-semibold text-slate/60">Información comercial</p><p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-ink">{selected.notes}</p></div> : null}
              {selected.status === "PENDING" ? (
                <div className="space-y-3">
                  <label className="block text-sm font-semibold text-slate/75">Observación<textarea className="mt-2 min-h-28 w-full border border-smoke p-3 text-sm outline-none focus:border-accent" maxLength={1000} value={reviewNotes} onChange={(event) => setReviewNotes(event.target.value)} /></label>
                  <div className="grid grid-cols-2 gap-3">
                    <button type="button" className="btn btn-primary" disabled={submitting} onClick={() => void review("APPROVED")}>Aprobar</button>
                    <button type="button" className="btn btn-secondary border-red-200 text-red-700" disabled={submitting} onClick={() => void review("REJECTED")}>Rechazar</button>
                  </div>
                </div>
              ) : (
                <div className="border-l-4 border-accent bg-paper px-4 py-3 text-sm"><strong>{statusLabels[selected.status]}</strong>{selected.reviewNotes ? <p className="mt-2 text-slate/70">{selected.reviewNotes}</p> : null}</div>
              )}
            </div>
          ) : <p className="text-sm text-slate/65">Selecciona una solicitud para revisar sus datos.</p>}
        </aside>
      </div>
    </main>
  );
}
