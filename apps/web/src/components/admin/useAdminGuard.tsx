"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { type UserRole, isAdmin, isAdminOrManager, isOperator } from "@/lib/auth";

export type AdminUser = {
  id: string;
  email: string;
  role: UserRole;
};

export type AdminAccess = "ADMIN_ONLY" | "ADMIN_MANAGER" | "ADMIN_MANAGER_OPERATOR";

export function useAdminGuard(access: AdminAccess) {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<AdminUser | null>(null);

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      try {
        const me = await apiFetch<AdminUser>("/api/auth/me");
        if (!active) return;
        setUser(me);
        if (!isAdminOrManager(me.role) && !isOperator(me.role)) {
          router.push("/login");
          return;
        }
        if (access === "ADMIN_ONLY" && !isAdmin(me.role)) {
          router.push(me.role === "OPERATOR" ? "/admin/integrations" : "/admin/dashboard");
          return;
        }
        if (access === "ADMIN_MANAGER" && !isAdminOrManager(me.role)) {
          router.push(me.role === "OPERATOR" ? "/admin/integrations" : "/admin/dashboard");
          return;
        }
        if (access === "ADMIN_MANAGER_OPERATOR" && !isAdminOrManager(me.role) && !isOperator(me.role)) {
          router.push("/admin/dashboard");
          return;
        }
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        router.push("/login");
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
  }, [access, router]);

  const allowed = user
    ? access === "ADMIN_ONLY"
      ? isAdmin(user.role)
      : access === "ADMIN_MANAGER"
        ? isAdminOrManager(user.role)
        : isAdminOrManager(user.role) || isOperator(user.role)
    : false;

  return { user, loading, allowed };
}
