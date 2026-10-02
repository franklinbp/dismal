"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import {
  DashboardIcon,
  SalesIcon,
  UsersIcon,
  InvoiceIcon,
  QuoteIcon,
  ReportIcon,
  ProductIcon,
  LicenseIcon,
  CampaignIcon,
  SparkIcon,
  ChevronIcon,
  IntegrationIcon,
  TargetIcon,
} from "@/components/admin/icons";
import { type UserRole, isAdmin, isOperator } from "@/lib/auth";

type NavItem = {
  label: string;
  href?: string;
  icon?: (props: { className?: string }) => JSX.Element;
  children?: NavItem[];
  id?: string;
  roles?: UserRole[];
};

type NavSection = {
  label: string;
  items: NavItem[];
  adminOnly?: boolean;
};

const navSections: NavSection[] = [
  {
    label: "Operaciones",
    items: [
      { label: "Dashboard", href: "/admin/dashboard", icon: DashboardIcon },
      { label: "Ventas", href: "/admin/sales", icon: SalesIcon },
      { label: "Pedidos web", href: "/admin/storefront/orders", icon: InvoiceIcon },
      { label: "Bancos y cajas", href: "/admin/finance/accounts", icon: SalesIcon, roles: ["ADMIN", "MANAGER"] },
      { label: "Facturas", href: "/admin/billing/invoices", icon: InvoiceIcon },
      { label: "Cotizaciones", href: "/admin/billing/quotes", icon: QuoteIcon },
      { label: "Gastos y pagos", href: "/admin/expenses", icon: SalesIcon },
      { label: "AR", href: "/admin/ar", icon: SalesIcon },
    ],
  },
  {
    label: "Catalogo",
    items: [
      { label: "Productos", href: "/admin/inventory/products", icon: ProductIcon },
      { label: "Existencias físicas", href: "/admin/inventory/products", icon: LicenseIcon },
    ],
  },
  {
    label: "Estrategia",
    items: [
      { label: "Marketing", href: "/admin/marketing/campaigns", icon: CampaignIcon },
      { label: "Acciones", href: "/admin/marketing/actions", icon: SparkIcon },
      { label: "Metas", href: "/admin/targets", icon: TargetIcon },
      { label: "Planeacion", href: "/admin/planning/targets", icon: TargetIcon },
      { label: "Inteligencia MIP", href: "/admin/marketing/mip", icon: SparkIcon },
    ],
  },
  {
    label: "Administracion",
    items: [
      { label: "Usuarios", href: "/admin/admin/users", icon: UsersIcon, roles: ["ADMIN"] },
      { label: "Solicitudes de distribuidor", href: "/admin/admin/wholesale", icon: UsersIcon, roles: ["ADMIN", "MANAGER"] },
      { label: "Integraciones", href: "/admin/integrations", icon: IntegrationIcon, roles: ["ADMIN", "MANAGER", "OPERATOR"] },
    ],
  },
  {
    label: "Analisis",
    items: [
      {
        label: "Reportes",
        id: "sales-reports",
        children: [
          { label: "Ventas diarias", href: "/admin/reports/sales/daily", icon: ReportIcon },
          { label: "Ventas semanales", href: "/admin/reports/sales/weekly", icon: ReportIcon },
          { label: "Ventas mensuales", href: "/admin/reports/sales/monthly", icon: ReportIcon },
          { label: "Vs metas", href: "/admin/reports/sales/vs-targets", icon: ReportIcon },
        ],
      },
    ],
  },
];

type AdminSidebarProps = {
  role?: UserRole | null;
};

function isActivePath(pathname: string, href?: string) {
  if (!href) return false;
  return pathname === href;
}

function hasActiveChild(pathname: string, item: NavItem) {
  if (!item.children) return false;
  return item.children.some((child) => isActivePath(pathname, child.href));
}

export default function AdminSidebar({ role }: AdminSidebarProps) {
  const pathname = usePathname();
  const allowAdmin = isAdmin(role);
  const operatorOnly = isOperator(role);
  const defaultOpen = useMemo(() => {
    const initial: Record<string, boolean> = {};
    navSections.forEach((section) => {
      const activeInSection = section.items.some(
        (item) => isActivePath(pathname, item.href) || hasActiveChild(pathname, item)
      );
      initial[section.label] = section.label === "Dashboard" || activeInSection;
      section.items.forEach((item) => {
        if (item.children && item.id) {
          initial[item.id] = hasActiveChild(pathname, item);
        }
      });
    });
    return initial;
  }, [pathname]);
  const [openSections, setOpenSections] = useState<Record<string, boolean>>(defaultOpen);

  useEffect(() => {
    setOpenSections((prev) => ({ ...defaultOpen, ...prev }));
  }, [defaultOpen]);

  return (
    <aside className="admin-shell-sidebar hidden w-[248px] shrink-0 flex-col gap-4 border-r border-[var(--color-border)] text-[var(--color-text)] lg:flex">
      <div className="border-b border-[var(--color-border)] px-4 py-5">
        <div className="flex items-center gap-2.5">
          <div className="grid h-10 w-10 place-items-center rounded-2xl border border-[var(--color-border)] bg-white/90 shadow-panel">
            <img src="/mi_logo.png" alt="Dismal Distribuciones" className="h-8 w-24 object-contain object-left" />
          </div>
          <div>
            <p className="font-display text-lg leading-none text-[var(--color-text)]">Dismal</p>
            <p className="mt-1 text-[11px] uppercase tracking-[0.18em] text-[var(--color-text-muted)]">Panel administrativo</p>
          </div>
        </div>
      </div>
      <nav className="flex flex-col gap-4 px-3 pb-5 text-sm">
        {navSections
          .filter((section) => (section.adminOnly ? allowAdmin || operatorOnly : true))
          .map((section) => {
            const visibleItems = section.items.filter((item) =>
              operatorOnly
                ? item.roles?.includes("OPERATOR") === true
                : !item.roles || (role ? item.roles.includes(role) : false)
            );
            if (visibleItems.length === 0) {
              return null;
            }
            const sectionOpen = openSections[section.label] ?? true;
            return (
              <div key={section.label}>
                <button
                  type="button"
                  onClick={() =>
                    setOpenSections((prev) => ({
                      ...prev,
                      [section.label]: !sectionOpen,
                    }))
                  }
                  className="mb-1.5 flex w-full items-center justify-between px-2 text-[10px] uppercase tracking-[0.22em] text-[var(--color-text-muted)]"
                >
                  <span>{section.label}</span>
                  <ChevronIcon
                    className={`h-4 w-4 transition-transform ${sectionOpen ? "rotate-180" : ""}`}
                  />
                </button>
                <div
                  className={`flex flex-col gap-1 overflow-hidden transition-all duration-300 ${
                    sectionOpen ? "max-h-[520px] opacity-100" : "max-h-0 opacity-0"
                  }`}
                >
                  {visibleItems.map((item) => {
                    if (item.children) {
                      const groupKey = item.id || item.label;
                      const groupOpen = openSections[groupKey] ?? hasActiveChild(pathname, item);
                      return (
                        <div key={item.label} className="rounded-2xl border border-[var(--color-border)] bg-white/60 p-2 shadow-sm">
                          <button
                            type="button"
                            onClick={() =>
                              setOpenSections((prev) => ({
                                ...prev,
                                [groupKey]: !groupOpen,
                              }))
                            }
                            className="flex w-full items-center justify-between text-[10px] uppercase tracking-[0.18em] text-[var(--color-text-muted)]"
                          >
                            <span>{item.label}</span>
                            <ChevronIcon
                              className={`h-4 w-4 transition-transform ${groupOpen ? "rotate-180" : ""}`}
                            />
                          </button>
                          <div
                            className={`mt-2 flex flex-col gap-1 overflow-hidden transition-all duration-300 ${
                              groupOpen ? "max-h-[320px] opacity-100" : "max-h-0 opacity-0"
                            }`}
                          >
                            {item.children.map((child) => {
                              const isActive = isActivePath(pathname, child.href);
                              const Icon = child.icon;
                              return (
                                <Link
                                  key={child.label}
                                  href={child.href || "#"}
                                  className={`group flex items-center gap-2.5 rounded-xl px-3 py-2 text-left transition ${
                                    isActive
                                      ? "bg-ink text-white font-semibold shadow-panel"
                                      : "text-[var(--color-text)] hover:bg-white/90"
                                  }`}
                                >
                                  {Icon && (
                                    <span
                                      className={`grid h-7 w-7 place-items-center rounded-full border ${
                                        isActive
                                          ? "border-white/20 bg-white/15 text-white"
                                          : "border-[var(--color-border)] bg-white text-[var(--color-text-muted)]"
                                      }`}
                                    >
                                      <Icon className="h-4 w-4" />
                                    </span>
                                  )}
                                  {child.label}
                                </Link>
                              );
                            })}
                          </div>
                        </div>
                      );
                    }

                    const isActive = isActivePath(pathname, item.href);
                    const Icon = item.icon;
                    return (
                      <Link
                        key={item.label}
                        href={item.href || "#"}
                        className={`group flex items-center gap-2.5 rounded-xl px-3 py-2 text-left transition ${
                          isActive
                            ? "bg-ink text-white font-semibold shadow-panel"
                            : "text-[var(--color-text)] hover:bg-white/90"
                        }`}
                      >
                        {Icon && (
                          <span
                            className={`grid h-7 w-7 place-items-center rounded-full border ${
                              isActive
                                ? "border-white/20 bg-white/15 text-white"
                                : "border-[var(--color-border)] bg-white text-[var(--color-text-muted)]"
                            }`}
                          >
                            <Icon className="h-4 w-4" />
                          </span>
                        )}
                        {item.label}
                      </Link>
                    );
                  })}
                </div>
              </div>
            );
          })}
      </nav>
    </aside>
  );
}
