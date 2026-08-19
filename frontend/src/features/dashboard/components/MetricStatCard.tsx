import type { LucideIcon } from 'lucide-react'
import { clsx } from 'clsx'

interface MetricStatCardProps {
  label:      string
  value:      string
  sublabel:   string
  icon:       LucideIcon
  trend?:     string
  isWarning?: boolean
}

export function MetricStatCard({
  label,
  value,
  sublabel,
  icon: Icon,
  trend,
  isWarning,
}: MetricStatCardProps) {
  return (
    <div
      className={clsx(
        'bg-white rounded-lg p-5 border shadow-2xs transition-all hover:border-slate-300',
        isWarning ? 'border-amber-300 bg-amber-50/20' : 'border-slate-200'
      )}
    >
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
          {label}
        </span>
        <div className="w-8 h-8 rounded bg-slate-100 text-slate-700 flex items-center justify-center border border-slate-200">
          <Icon className="w-4 h-4" />
        </div>
      </div>

      <div className="flex items-baseline justify-between">
        <p className="text-2xl font-bold font-mono text-slate-900 tracking-tight">
          {value}
        </p>
        {trend && (
          <span className="text-[11px] font-medium text-slate-500 font-mono">
            {trend}
          </span>
        )}
      </div>

      <p className="text-xs text-slate-500 mt-1.5">{sublabel}</p>
    </div>
  )
}
