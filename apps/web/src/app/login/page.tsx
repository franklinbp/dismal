"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch, type ApiError } from "@/lib/api";
import { useSessionStore, type User } from "@/lib/store/sessionStore";

export default function LoginPage() {
  const router = useRouter();
  const setUser = useSessionStore((state) => state.setUser);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const user = await apiFetch<User>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });

      setUser(user);

      if (user.role === "ADMIN" || user.role === "MANAGER") {
        router.push("/admin/dashboard");
      } else if (user.role === "OPERATOR") {
        router.push("/admin/integrations");
      } else {
        setError("Tu usuario no tiene permisos para el dashboard.");
      }
    } catch (err) {
      const apiError = err as ApiError;
      setError(apiError?.message || "Credenciales invalidas.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="relative min-h-screen overflow-hidden bg-paper px-6 py-10 text-slate">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(29,180,231,0.16),transparent_24%),radial-gradient(circle_at_80%_15%,rgba(43,42,114,0.14),transparent_24%)]" />
      <div className="relative mx-auto grid min-h-[calc(100vh-5rem)] w-full max-w-6xl items-center gap-10 lg:grid-cols-[minmax(0,1.05fr)_minmax(420px,0.95fr)]">
        <section className="hidden lg:block">
          <div className="max-w-xl space-y-6">
            <p className="admin-kicker">
              <span className="admin-dot" />
              Importadora tecnologica ecuatoriana
            </p>
            <h1 className="font-display text-6xl leading-[0.95] text-ink">
              Tecnologia para empresas, hogares y distribuidores.
            </h1>
            <p className="max-w-lg text-base leading-7 text-slate/80">
              Dismal Distribuciones importa y comercializa equipos informaticos, impresion, telefonia movil,
              almacenamiento y licencias digitales con inventario, ventas y posventa en una sola operacion.
            </p>
            <div className="grid max-w-xl grid-cols-2 gap-3">
              {[
                ["Computacion", "Laptops, desktops y componentes"],
                ["Impresion", "Impresoras, suministros y accesorios"],
                ["Telefonia movil", "Smartphones y dispositivos"],
                ["Almacenamiento", "Discos SSD y soluciones de respaldo"],
                ["Licencias digitales", "Software, seguridad y productividad"],
                ["Distribucion", "Venta mayorista y atencion empresarial"],
              ].map(([title, body]) => (
                <div key={title} className="admin-stat-card p-4">
                  <p className="font-display text-lg text-ink">{title}</p>
                  <p className="mt-2 text-xs leading-5 text-slate/60">{body}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
        <section className="glass-panel mx-auto w-full max-w-md p-8 lg:p-10">
          <div className="mb-8 flex items-center gap-4">
            <div className="grid h-14 w-14 place-items-center rounded-[20px] border border-smoke/80 bg-white/95 shadow-panel">
              <img src="/mi_logo.png" alt="Dismal Distribuciones" className="h-12 w-40 object-contain object-left" />
            </div>
            <div>
              <p className="text-xs uppercase tracking-[0.32em] text-slate/50">Dismal</p>
              <p className="font-display text-2xl leading-none text-ink">Gestion de la importadora</p>
            </div>
          </div>
          <h1 className="font-display text-4xl leading-none text-ink">Iniciar sesion</h1>
          <p className="mt-3 text-sm leading-6 text-slate/75">
            Ingresa con tu usuario autorizado para gestionar productos fisicos, licencias, ventas y clientes.
          </p>
          <form onSubmit={handleSubmit} className="mt-8 space-y-4">
            <label className="block text-sm font-medium text-slate/80">
              Email
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-2 w-full rounded-[16px] border border-smoke/80 bg-white/90 px-4 py-3 text-sm focus:border-accent focus:outline-none"
              />
            </label>
            <label className="block text-sm font-medium text-slate/80">
              Contraseña
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-2 w-full rounded-[16px] border border-smoke/80 bg-white/90 px-4 py-3 text-sm focus:border-accent focus:outline-none"
              />
            </label>
            {error && (
              <div className="rounded-[16px] border border-red-200 bg-red-50/90 px-4 py-3 text-sm text-red-700">
                {error}
              </div>
            )}
            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary w-full disabled:opacity-60"
            >
              {loading ? "Ingresando..." : "Ingresar"}
            </button>
          </form>
        </section>
      </div>
    </main>
  );
}
