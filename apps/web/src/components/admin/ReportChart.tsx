"use client";

type ReportPoint = {
  label: string;
  value: number;
};

type ReportChartProps = {
  points: ReportPoint[];
  valueLabel?: string;
};

export default function ReportChart({ points, valueLabel = "Total" }: ReportChartProps) {
  if (points.length === 0) {
    return <p className="text-sm text-slate-400">Sin datos para graficar.</p>;
  }

  const maxValue = Math.max(...points.map((point) => point.value), 1);

  return (
    <div className="glass-card px-4 py-4">
      <div className="flex items-center justify-between text-xs uppercase tracking-[0.3em] text-slate-400">
        <span>{valueLabel}</span>
        <span>{maxValue.toFixed(0)}</span>
      </div>
      <div className="mt-4 flex items-end gap-2">
        {points.map((point) => {
          const height = Math.max(8, Math.round((point.value / maxValue) * 120));
          return (
            <div key={point.label} className="flex flex-1 flex-col items-center gap-2">
              <div
                className="w-full rounded-lg bg-gradient-to-t from-cyan-500/80 to-sky-300/80"
                style={{ height }}
                title={`${point.label}: ${point.value.toFixed(2)}`}
              />
              <span className="text-[10px] text-slate-400">{point.label}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
