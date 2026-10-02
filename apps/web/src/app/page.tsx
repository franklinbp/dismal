import ThemeToggle from "@/components/ThemeToggle";

export default function Home() {
  return (
    <main className="relative min-h-screen overflow-hidden bg-paper text-slate">
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute -top-40 left-1/2 h-[520px] w-[520px] -translate-x-1/2 rounded-full bg-[radial-gradient(circle_at_center,#1B9CFC33_0%,transparent_60%)] blur-2xl" />
        <div className="absolute bottom-[-220px] right-[-120px] h-[420px] w-[420px] rounded-full bg-[radial-gradient(circle_at_center,#0A254030_0%,transparent_70%)] blur-2xl" />
        <div className="absolute left-[-160px] top-[40%] h-[360px] w-[360px] rounded-full bg-[radial-gradient(circle_at_center,#1688DF30_0%,transparent_70%)] blur-2xl" />
      </div>

      <header className="relative z-10 mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-8">
        <div className="flex items-center gap-3">
          <div className="grid h-11 w-11 place-items-center rounded-2xl border border-ink/10 bg-white/90 shadow-panel">
            <img src="/mi_logo.png" alt="Dismal Distribuciones" className="h-10 w-32 object-contain object-left" />
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.3em] text-slate/50">Dismal</p>
            <p className="font-display text-lg text-ink">Operations Suite</p>
          </div>
        </div>
        <nav className="hidden items-center gap-6 text-sm text-slate/70 md:flex">
          <a className="transition hover:text-ink" href="#plataforma">Plataforma</a>
          <a className="transition hover:text-ink" href="#impacto">Impacto</a>
          <a className="transition hover:text-ink" href="#operacion">Operacion</a>
          <a className="transition hover:text-ink" href="#demo">Demo</a>
        </nav>
        <div className="flex items-center gap-3">
          <ThemeToggle />
          <a className="btn btn-secondary text-xs" href="/login">
            Entrar
          </a>
        </div>
      </header>

      <section className="relative z-10 mx-auto grid w-full max-w-6xl gap-10 px-6 pb-16 pt-6 md:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)]">
        <div className="space-y-6">
          <div className="inline-flex items-center gap-2 rounded-full border border-ink/10 bg-white/80 px-4 py-2 text-xs uppercase tracking-[0.32em] text-ink">
            Plataforma Operativa
            <span className="h-1.5 w-1.5 rounded-full bg-accent" />
          </div>
          <h1 className="font-display text-4xl leading-tight text-ink md:text-5xl">
            Productos tecnologicos y licencias listos para vender.
          </h1>
          <p className="text-base text-slate/70 md:text-lg">
            Dismal centraliza importacion, inventario, ventas y entregas de computadoras, impresoras,
            telefonia movil, discos SSD y licencias digitales.
          </p>
          <div className="flex flex-wrap items-center gap-4">
            <a href="#demo" className="btn btn-primary text-xs">
              Agendar demo
            </a>
            <a href="/login" className="btn btn-secondary text-xs">
              Entrar
            </a>
          </div>
          <div className="grid grid-cols-2 gap-6 pt-6 md:grid-cols-3">
            {[
              ["Unificado", "Inventario fisico y digital"],
              ["Mayorista", "Atencion a distribuidores"],
              ["24/7", "Control de stock y activaciones"],
            ].map(([value, label]) => (
              <div key={label} className="rounded-2xl border border-ink/10 bg-white/70 p-4 shadow-panel">
                <p className="font-display text-2xl text-ink">{value}</p>
                <p className="text-xs uppercase tracking-[0.25em] text-slate/50">{label}</p>
              </div>
            ))}
          </div>
        </div>
        <div className="glass-card relative rounded-[32px] border border-smoke/70 bg-white/80 p-6 shadow-panel">
          <p className="text-xs uppercase tracking-[0.3em] text-slate/50">Vista Ejecutiva</p>
          <h2 className="mt-3 font-display text-2xl text-ink">Panel Vivo</h2>
          <div className="mt-6 space-y-4">
            {[
              ["Ventas hoy", "$18,540", "+12%"],
              ["Productos gestionados", "128", "+18%"],
              ["Campanas activas", "5", "2 nuevas"],
            ].map(([label, value, delta]) => (
              <div key={label} className="rounded-2xl border border-smoke/70 bg-white p-4">
                <div className="flex items-center justify-between text-sm text-slate/60">
                  <span>{label}</span>
                  <span className="rounded-full bg-ink/5 px-3 py-1 text-xs uppercase tracking-[0.2em] text-ink">
                    {delta}
                  </span>
                </div>
                <p className="mt-2 font-display text-2xl text-ink">{value}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="plataforma" className="relative z-10 mx-auto w-full max-w-6xl px-6 py-16">
        <div className="grid gap-8 md:grid-cols-3">
          {[
            ["Venta fisica y digital", "Pedidos de equipos tecnologicos y entrega automatizada de licencias."],
            ["Inventario unificado", "Unidades fisicas y activaciones sincronizadas para evitar quiebres y sobreventa."],
            ["Marketing inteligente", "Campanas con foco en margen y acciones recomendadas."],
          ].map(([title, body]) => (
            <div key={title} className="rounded-3xl border border-ink/10 bg-white/80 p-6 shadow-panel">
              <h3 className="font-display text-xl text-ink">{title}</h3>
              <p className="mt-3 text-sm text-slate/70">{body}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="impacto" className="relative z-10 mx-auto w-full max-w-6xl px-6 py-12">
        <div className="grid gap-8 rounded-[36px] border border-ink/10 bg-ink px-8 py-10 text-white md:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
          <div>
            <p className="text-xs uppercase tracking-[0.3em] text-white/60">Impacto Operativo</p>
            <h2 className="mt-4 font-display text-3xl">Operacion comercial medible, decisiones mas rapidas.</h2>
            <p className="mt-4 text-sm text-white/70">
              Cada equipo trabaja con la misma fuente de datos. Metas, margen y entregas se observan en
              un mismo tablero.
            </p>
          </div>
          <div className="space-y-4">
            {[
              ["Rentabilidad", "Metas y margen por categoria y producto"],
              ["Entrega", "Seguimiento de despachos y licencias activadas"],
              ["Integraciones", "N8N, correo y automatizaciones"],
            ].map(([label, detail]) => (
              <div key={label} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <p className="text-xs uppercase tracking-[0.25em] text-white/60">{label}</p>
                <p className="mt-2 text-sm text-white/80">{detail}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="operacion" className="relative z-10 mx-auto w-full max-w-6xl px-6 py-16">
        <div className="grid gap-6 md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
          <div className="rounded-[32px] border border-ink/10 bg-white/80 p-6 shadow-panel">
            <p className="text-xs uppercase tracking-[0.3em] text-slate/50">Flujo Unificado</p>
            <h2 className="mt-3 font-display text-2xl text-ink">Importacion y distribucion listas para escalar</h2>
            <ul className="mt-6 space-y-4 text-sm text-slate/70">
              {[
                "Ventas conectadas con inventario fisico y activaciones.",
                "Alertas automaticas de licencias y stock fisico critico.",
                "Segmentacion de campañas por categoria y tipo de producto.",
                "Dashboards por rol para cada equipo.",
              ].map((item) => (
                <li key={item} className="flex items-start gap-3">
                  <span className="mt-1 h-2 w-2 rounded-full bg-accent" />
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="grid gap-4">
            {[
              ["Alertas en vivo", "Control de eventos criticos y fallos de integracion."],
              ["Panel por roles", "Admins, managers y operadores con flujos claros."],
              ["Automatizacion", "Notificaciones por email y WhatsApp."],
            ].map(([title, body]) => (
              <div key={title} className="rounded-3xl border border-ink/10 bg-white/80 p-6 shadow-panel">
                <h3 className="font-display text-xl text-ink">{title}</h3>
                <p className="mt-2 text-sm text-slate/70">{body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="demo" className="relative z-10 mx-auto w-full max-w-6xl px-6 pb-20 pt-8">
        <div className="rounded-[36px] border border-ink/10 bg-white/90 px-8 py-10 shadow-panel">
          <div className="flex flex-col items-start justify-between gap-6 md:flex-row md:items-center">
            <div>
              <p className="text-xs uppercase tracking-[0.3em] text-slate/50">Demo</p>
              <h2 className="mt-3 font-display text-3xl text-ink">Listos para ordenar la operacion?</h2>
              <p className="mt-2 text-sm text-slate/70">
                Agenda una demo y revisemos tus indicadores en vivo.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <a href="/login" className="btn btn-primary text-xs">
                Entrar al panel
              </a>
              <a href="mailto:demo@dismal.io" className="btn btn-secondary text-xs">
                Contactar
              </a>
            </div>
          </div>
        </div>
      </section>

      <footer className="relative z-10 mx-auto w-full max-w-6xl px-6 pb-10">
        <div className="flex flex-wrap items-center justify-between gap-4 border-t border-ink/10 pt-6 text-xs text-slate/50">
          <span>Dismal. Operacion con foco.</span>
          <span>2026</span>
        </div>
      </footer>
    </main>
  );
}
