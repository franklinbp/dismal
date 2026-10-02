"use client";

import type { ReactNode } from "react";

type AdminActionButtonProps = {
  icon?: ReactNode;
  label?: string;
  children?: ReactNode;
  onClick?: () => void;
  variant?: "primary" | "ghost";
  disabled?: boolean;
};

export default function AdminActionButton({
  icon,
  label,
  onClick,
  variant = "ghost",
  children,
  disabled = false,
}: AdminActionButtonProps) {
  const base =
    "btn inline-flex items-center gap-2 text-[10px] uppercase tracking-[0.14em] transition duration-200";
  const style =
    variant === "primary"
      ? "btn-primary hover:translate-y-[-1px] hover:bg-slate-900/90"
      : "btn-secondary hover:translate-y-[-1px] hover:bg-slate-900/5";
  const disabledStyle = disabled ? "cursor-not-allowed opacity-50 shadow-none" : "";

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`${base} ${style} ${disabledStyle}`}
    >
      {icon && <span className="h-4 w-4">{icon}</span>}
      {label ?? children}
    </button>
  );
}
