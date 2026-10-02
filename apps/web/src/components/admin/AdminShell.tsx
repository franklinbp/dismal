"use client";

import { useSessionStore } from "@/lib/store/sessionStore";
import { useUser } from "@/lib/hooks/useUser";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import AdminSidebar from "./AdminSidebar";
import { type UserRole } from "@/lib/auth";
import { apiFetch } from "@/lib/api";

export default function AdminShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const clearSession = useSessionStore((state) => state.clearSession);
  const { user, isLoading, isError } = useUser();

  useEffect(() => {
    if (isError) {
      clearSession();
      router.replace("/login");
    }
  }, [isError, router, clearSession]);

  const handleLogout = async () => {
    try {
      await apiFetch("/api/auth/logout", { method: "POST" });
    } finally {
      clearSession();
      router.replace("/login");
    }
  };

  if (isLoading || !user) {
    return (
      <div className="min-h-screen grid place-items-center">
        <p>Loading...</p>
      </div>
    );
  }

  return (
    <div className="admin-theme min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <header className="admin-shell-header sticky top-0 z-30 border-b border-[var(--color-border)] px-5 py-3 shadow-sm">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2.5">
            <div className="grid h-10 w-10 place-items-center rounded-2xl border border-[var(--color-border)] bg-white/90 shadow-panel">
              <img src="/mi_logo.png" alt="Dismal Distribuciones" className="h-8 w-24 object-contain object-left" />
            </div>
            <div>
              <p className="text-[11px] uppercase tracking-[0.28em] text-[var(--color-text-muted)]">Dismal</p>
              <p className="font-display text-lg leading-none text-[var(--color-text)]">Control Center</p>
            </div>
          </div>
          <div className="hidden h-7 w-px bg-[var(--color-border)] lg:flex" />
          <div className="hidden items-center gap-2 text-xs uppercase tracking-[0.18em] text-[var(--color-text-muted)] lg:flex">
            <span className="admin-dot" />
            <span>Operacion administrativa</span>
          </div>
          <div className="flex-1" />
          <div className="flex flex-wrap items-center gap-2.5 text-sm">
            <span className="rounded-full border border-[var(--color-border)] bg-white/80 px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.14em] text-[var(--color-text-muted)]">
              {user.role}
            </span>
            <span className="text-[var(--color-text-muted)]">{user.email}</span>
            <button
              type="button"
              disabled
              className="rounded-full border border-[var(--color-border)] bg-white/80 px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.14em] text-[var(--color-text-muted)] opacity-80"
              title="Sync manual disponible en apps locales"
            >
              Sync local
            </button>
            <button
              type="button"
              onClick={handleLogout}
              className="btn btn-primary text-[10px] uppercase tracking-[0.16em]"
            >
              Salir
            </button>
          </div>
        </div>
      </header>
      <div className="flex min-h-[calc(100vh-56px)] min-w-0">
        <AdminSidebar role={user.role as UserRole} />
        <main className="min-w-0 flex-1 bg-[var(--color-bg)] px-5 py-5">{children}</main>
      </div>
    </div>
  );
}
