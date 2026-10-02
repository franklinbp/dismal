"use client";

import ThemeToggle from "@/components/ThemeToggle";

type AdminHeaderProps = {
  title: string;
  subtitle?: string;
  userEmail: string;
  onLogout: () => void;
  actions?: React.ReactNode;
  showThemeToggle?: boolean;
};

export default function AdminHeader({
  title,
  subtitle,
  userEmail,
  onLogout,
  actions,
  showThemeToggle = true,
}: AdminHeaderProps) {
  return (
    <header className="flex flex-wrap items-start justify-between gap-4">
      <div className="flex items-start gap-3">
        <div className="grid h-12 w-12 place-items-center rounded-[18px] border border-smoke/80 bg-white/95 shadow-panel">
          <img src="/mi_logo.png" alt="Dismal Distribuciones" className="h-8 w-24 object-contain object-left" />
        </div>
        <div className="space-y-2">
          {subtitle && (
            <p className="admin-kicker">
              <span className="admin-dot" />
              {subtitle}
            </p>
          )}
          <h1 className="font-display text-[2rem] leading-none text-ink">{title}</h1>
        </div>
      </div>
      <div className="flex flex-col items-stretch gap-2.5 sm:items-end">
        <div className="flex flex-wrap items-center gap-2.5 rounded-2xl border border-smoke/80 bg-white/90 px-3 py-2.5 shadow-panel backdrop-blur">
          {showThemeToggle && <ThemeToggle />}
          <div className="hidden h-6 w-px bg-smoke/80 sm:block" />
          <span className="text-sm text-slate/70">{userEmail}</span>
          <button
            onClick={onLogout}
            className="btn btn-primary text-[10px] uppercase tracking-[0.18em]"
          >
            Salir
          </button>
        </div>
        {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
      </div>
    </header>
  );
}
