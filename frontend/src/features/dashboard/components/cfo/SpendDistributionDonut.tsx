import { useState } from 'react'
import { PieChart } from 'lucide-react'
import { formatCurrency } from '@/utils/currency'
import type { BudgetPoolResponse } from '@/types/budget.types'

interface SpendDistributionDonutProps {
  pools: BudgetPoolResponse[]
}

const COLORS = [
  { fill: '#3b82f6', bg: 'bg-blue-500', text: 'text-blue-600', light: 'bg-blue-50' },
  { fill: '#6366f1', bg: 'bg-indigo-500', text: 'text-indigo-600', light: 'bg-indigo-50' },
  { fill: '#10b981', bg: 'bg-emerald-500', text: 'text-emerald-600', light: 'bg-emerald-50' },
  { fill: '#f59e0b', bg: 'bg-amber-500', text: 'text-amber-600', light: 'bg-amber-50' },
  { fill: '#8b5cf6', bg: 'bg-purple-500', text: 'text-purple-600', light: 'bg-purple-50' },
]

export function SpendDistributionDonut({ pools }: SpendDistributionDonutProps) {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null)

  const data = pools.map((p, i) => {
    const committed = p.spentAmount + p.reservedAmount
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

  // Compute SVG SVG donut arcs
  const size = 200
  const center = size / 2
  const radius = 72
  const strokeWidth = 26
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
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center">
            <PieChart className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">Cost Center Spend Distribution</h3>
            <p className="text-[11px] text-slate-500">Departmental committed budget allocation ratio</p>
          </div>
        </div>
        <span className="text-[11px] font-mono font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
          {data.length} Units
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
        {/* Interactive SVG Donut */}
        <div className="relative flex items-center justify-center">
          <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="transform -rotate-90">
            <circle
              cx={center}
              cy={center}
              r={radius}
              fill="transparent"
              stroke="#f1f5f9"
              strokeWidth={strokeWidth}
            />
            {slices.map((s, idx) => (
              <circle
                key={s.code}
                cx={center}
                cy={center}
                r={radius}
                fill="transparent"
                stroke={s.color.fill}
                strokeWidth={hoveredIdx === idx ? strokeWidth + 4 : strokeWidth}
                strokeDasharray={s.strokeDasharray}
                strokeDashoffset={s.strokeDashoffset}
                className="transition-all duration-300 cursor-pointer"
                onMouseEnter={() => setHoveredIdx(idx)}
                onMouseLeave={() => setHoveredIdx(null)}
              />
            ))}
          </svg>

          {/* Donut Center Overlay */}
          <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center">
            {activeItem ? (
              <>
                <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                  {activeItem.code}
                </span>
                <span className="text-sm font-extrabold text-slate-900 font-mono">
                  %{activeItem.percent}
                </span>
                <span className="text-[9px] text-slate-500 font-medium max-w-[80px] truncate">
                  {activeItem.name}
                </span>
              </>
            ) : (
              <>
                <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                  Total Active
                </span>
                <span className="text-xs font-extrabold text-slate-900 font-mono">
                  {formatCurrency(totalCommitted)}
                </span>
                <span className="text-[9px] text-slate-400">
                  of {formatCurrency(totalAllocated)}
                </span>
              </>
            )}
          </div>
        </div>

        {/* Legend & Breakdown List */}
        <div className="space-y-2 text-xs">
          {data.map((d, idx) => (
            <div
              key={d.code}
              onMouseEnter={() => setHoveredIdx(idx)}
              onMouseLeave={() => setHoveredIdx(null)}
              className={`p-2 rounded-lg border transition-all cursor-pointer ${
                hoveredIdx === idx
                  ? 'bg-slate-50 border-slate-300 shadow-2xs translate-x-1'
                  : 'bg-white border-slate-100 hover:bg-slate-50/50'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className={`w-2.5 h-2.5 rounded-full ${d.color.bg}`} />
                  <strong className="text-slate-900 text-xs font-semibold">{d.name}</strong>
                </div>
                <span className="font-mono font-bold text-slate-900">
                  {formatCurrency(d.committed)}
                </span>
              </div>
              <div className="flex items-center justify-between text-[10px] text-slate-400 mt-1 pl-4.5">
                <span>Code: {d.code}</span>
                <span className={d.percent > 80 ? 'text-amber-600 font-bold' : 'text-slate-500'}>
                  %{d.percent} Utilized
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
