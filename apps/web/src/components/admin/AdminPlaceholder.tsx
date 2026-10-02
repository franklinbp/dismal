"use client";

import AdminSidebar from "@/components/admin/AdminSidebar";
import AdminHeader from "@/components/admin/AdminHeader";
import { type AdminUser } from "@/components/admin/useAdminGuard";

type AdminPlaceholderProps = {
  title: string;
  subtitle: string;
  description: string;
  user: AdminUser;
  onLogout: () => void;
};

export default function AdminPlaceholder({
  title,
  subtitle,
  description,
  user,
  onLogout,
}: AdminPlaceholderProps) {
  return (
    <div className="min-h-screen flex">
      <AdminSidebar role={user.role} />
      <main className="flex-1 px-6 py-8 lg:px-10">
        <div className="glass-panel hero-band rounded-[32px] px-6 py-6 fade-up">
          <AdminHeader title={title} subtitle={subtitle} userEmail={user.email} onLogout={onLogout} />
        </div>
        <section className="mt-10 glass-panel rounded-3xl p-6 fade-up delay-1">
          <h2 className="font-display text-xl text-slate">Proximamente</h2>
          <p className="mt-3 max-w-xl text-sm text-slate/70">{description}</p>
          <div className="mt-6 rounded-2xl border border-dashed border-smoke/70 bg-white/60 p-6 text-sm text-slate/60">
            Estamos preparando este modulo para ofrecerte reportes mas profundos, filtros y acciones inteligentes.
          </div>
        </section>
      </main>
    </div>
  );
}

