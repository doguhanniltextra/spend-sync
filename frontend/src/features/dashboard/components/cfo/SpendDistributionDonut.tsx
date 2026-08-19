import { useState } from 'react'
import { PieChart } from 'lucide-react'
import { formatCurrency } from '@/utils/currency'
import type { BudgetPoolResponse } from '@/types/budget.types'

interface SpendDistributionDonutProps {
  pools: BudgetPoolResponse[]
}

const COLORS = [
  { fill: '#0f172a', bg: 'bg-slate-900', text: 'text-slate-900', light: 'bg-slate-100' },
  { fill: '#334155', bg: 'bg-slate-700', text: 'text-slate-700', light: 'bg-slate-100' },
  { fill: '#475569', bg: 'bg-slate-600', text: 'text-slate-600', light: 'bg-slate-100' },
  { fill: '#64748b', bg: 'bg-slate-500', text: 'text-slate-500', light: 'bg-slate-50' },
  { fill: '#94a3b8', bg: 'bg-slate-400', text: 'text-slate-400', light: 'bg-slate-50' },
  { fill: '#cbd5e1', bg: 'bg-slate-300', text: 'text-slate-500', light: 'bg-slate-50' },
]

export function SpendDistributionDonut({ pools }: SpendDistributionDonutProps) {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null)

  const data = pools.map((p, i) => {
    const committed = (p.spentAmount || 0) + (p.reservedAmount || 0)
    return {
      name:      p.costCenterName,
      code:      p.costCenterCode,
      allocated: p.allocatedAmount,
      spent:     p.spentAmount,
      reserved:  p.reservedAmount,
      committed,
      percent:   p.allocatedAmount > 0 ? Math.round((committed / p.allocatedAmount) * 100) : 0,
      color:     COLORS[i % COLORS.length],
    }
  })

  const totalCommitted = data.reduce((acc, d) => acc + d.committed, 0)
  const totalAllocated = data.reduce((acc, d) => acc + d.allocated, 0)

  // Compute SVG donut arcs
  const size = 200
  const center = size / 2
  const radius = 72
  const strokeWidth = 24
  const circumference = 2 * Math.PI * radius

  let cumulativePercent = 0
  const slices = data.map((d) => {
    const slicePercent = totalCommitted > 0 ? d.committed / totalCommitted : 1 / (data.length || 1)
    const strokeDasharray = `${slicePercent * circumference} ${circumference}`
    const strokeDashoffset = -(cumulativePercent * circumference)
    cumulativePercent += slicePercent
    return {
      ...d,
      strokeDasharray,
      strokeDashoffset,
    }
  })

  const activeItem = hoveredIdx !== null ? data[hoveredIdx] : null

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs">
      <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-700 flex items-center justify-center">
            <PieChart className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">Cost Center Spend Distribution</h3>
            <p className="text-[11px] text-slate-500">Departmental committed budget allocation ratio</p>
          </div>
        </div>
        <span className="text-[11px] font-mono text-slate-500 bg-slate-50 px-2 py-0.5 rounded border border-slate-200">
          {pools.length} units
        </span>
      </div>

      {pools.length === 0 ? (
        <div className="py-8 text-center text-xs text-slate-400">
          No budget pools configured for this fiscal year.
        </div>
      ) : (
        <div className="flex flex-col sm:flex-row items-center gap-6">
          {/* SVG Donut Chart */}
          <div className="relative shrink-0 flex items-center justify-center">
            <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="transform -rotate-90">
              {/* Background ring */}
              <circle
                cx={center}
                cy={center}
                r={radius}
                fill="transparent"
                stroke="#f1f5f9"
                strokeWidth={strokeWidth}
              />
              {/* Segments */}
              {slices.map((slice, i) => (
                <circle
                  key={slice.code + i}
                  cx={center}
                  cy={center}
                  r={radius}
                  fill="transparent"
                  stroke={slice.color.fill}
                  strokeWidth={hoveredIdx === i ? strokeWidth + 4 : strokeWidth}
                  strokeDasharray={slice.strokeDasharray}
                  strokeDashoffset={slice.strokeDashoffset}
                  className="transition-all duration-200 cursor-pointer"
                  onMouseEnter={() => setHoveredIdx(i)}
                  onMouseLeave={() => setHoveredIdx(null)}
                />
              ))}
            </svg>

            {/* Center Label */}
            <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none">
              <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400">
                {activeItem ? activeItem.code : 'Total Active'}
              </span>
              <span className="text-sm font-mono font-bold text-slate-900">
                {formatCurrency(activeItem ? activeItem.committed : totalCommitted)}
              </span>
              <span className="text-[9px] text-slate-400 font-mono">
                of {formatCurrency(totalAllocated)}
              </span>
            </div>
          </div>

          {/* Legend Items */}
          <div className="flex-1 space-y-2 w-full">
            {data.slice(0, 5).map((item, i) => (
              <div
                key={item.code}
                onMouseEnter={() => setHoveredIdx(i)}
                onMouseLeave={() => setHoveredIdx(null)}
                className={`p-2 rounded-lg transition-colors cursor-pointer border ${
                  hoveredIdx === i ? 'bg-slate-100/70 border-slate-300' : 'bg-slate-50/50 border-transparent hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2 truncate">
                    <span className={`w-2.5 h-2.5 rounded-full shrink-0 ${item.color.bg}`} />
                    <span className="font-medium text-slate-900 truncate max-w-[140px]">{item.name}</span>
                    <span className="text-[10px] text-slate-400 font-mono">({item.code})</span>
                  </div>
                  <div className="flex items-center gap-2 font-mono shrink-0 text-xs">
                    <span className="font-semibold text-slate-900">{formatCurrency(item.committed)}</span>
                    <span className="text-slate-400 text-[10px]">%{item.percent}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
