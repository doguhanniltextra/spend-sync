import { useState } from 'react'
import { TrendingUp } from 'lucide-react'
import { formatCurrency } from '@/utils/currency'

interface CashFlowForecastChartProps {
  totalAllocated: number
  totalSpent:     number
  totalReserved?: number
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export function CashFlowForecastChart({
  totalAllocated,
  totalSpent,
}: CashFlowForecastChartProps) {
  const [activeMonthIdx, setActiveMonthIdx] = useState<number>(7) // August default

  // Simulated 12-month trajectory based on real live totals
  const monthlyData = MONTHS.map((m, idx) => {
    const isPastOrCurrent = idx <= 7
    const targetBudget = Math.round((totalAllocated / 12) * (idx + 1))
    const actualSpend = isPastOrCurrent ? Math.round(totalSpent * (Math.pow(idx + 1, 1.1) / Math.pow(8, 1.1))) : null
    const forecastOutflow = !isPastOrCurrent
      ? Math.round((totalSpent / 8) * (1 + (idx - 7) * 0.08))
      : null

    return {
      month: m,
      targetBudget,
      actualSpend,
      forecastOutflow,
      isFuture: !isPastOrCurrent,
    }
  })

  const maxVal = totalAllocated > 0 ? totalAllocated : 36500000

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs">
      <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
            <TrendingUp className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">12-Month Burn Rate & Cash Outflow Forecast</h3>
            <p className="text-[11px] text-slate-500">Cumulative actuals vs. algorithmic quarterly projection</p>
          </div>
        </div>
        <div className="flex items-center gap-3 text-[11px]">
          <span className="flex items-center gap-1 text-slate-600 font-medium">
            <span className="w-2.5 h-2.5 rounded-full bg-slate-900" /> Target
          </span>
          <span className="flex items-center gap-1 text-emerald-700 font-medium">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" /> Actual Spend
          </span>
          <span className="flex items-center gap-1 text-amber-700 font-medium">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-400" /> Projected Outflow
          </span>
        </div>
      </div>

      {/* Bar / Area Chart Container */}
      <div className="h-44 flex items-end justify-between gap-1.5 pt-6 pb-2 px-2 border-b border-slate-100">
        {monthlyData.map((d, idx) => {
          const actualHeightPct = d.actualSpend ? Math.min(Math.round((d.actualSpend / maxVal) * 100), 100) : 0
          const forecastHeightPct = d.forecastOutflow ? Math.min(Math.round((d.forecastOutflow / (maxVal / 6)) * 100), 100) : 0
          const isSelected = activeMonthIdx === idx

          return (
            <div
              key={d.month}
              onClick={() => setActiveMonthIdx(idx)}
              className="flex-1 flex flex-col items-center h-full justify-end group cursor-pointer"
            >
              <div className="w-full flex items-end justify-center gap-1 h-32 relative">
                {/* Bar for Past/Actual */}
                {!d.isFuture ? (
                  <div
                    style={{ height: `${Math.max(actualHeightPct, 6)}%` }}
                    className={`w-full max-w-[20px] rounded-t transition-all duration-300 ${
                      isSelected
                        ? 'bg-slate-900 shadow-md'
                        : 'bg-slate-800/80 group-hover:bg-slate-900'
                    }`}
                  />
                ) : (
                  /* Striped Bar for Forecast Outflow */
                  <div
                    style={{ height: `${Math.max(forecastHeightPct, 6)}%` }}
                    className={`w-full max-w-[20px] rounded-t transition-all duration-300 border border-dashed ${
                      isSelected
                        ? 'bg-amber-400/90 border-amber-600'
                        : 'bg-amber-100 border-amber-300 group-hover:bg-amber-200'
                    }`}
                  />
                )}
              </div>
              <span
                className={`text-[10px] font-mono mt-2 transition-colors ${
                  isSelected ? 'font-bold text-slate-900' : 'text-slate-400 group-hover:text-slate-700'
                }`}
              >
                {d.month}
              </span>
            </div>
          )
        })}
      </div>

      {/* Active Month Drilldown Strip */}
      {activeMonthIdx !== null && (
        <div className="mt-3 bg-slate-50 p-2.5 rounded-lg border border-slate-200 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-900 uppercase font-mono">
              {MONTHS[activeMonthIdx]} 2026 Overview:
            </span>
            <span className="text-slate-600">
              {monthlyData[activeMonthIdx].isFuture ? (
                <span className="text-amber-700 font-semibold">
                  Forecasted Monthly Cash Need: {formatCurrency(monthlyData[activeMonthIdx].forecastOutflow || 0)}
                </span>
              ) : (
                <span>
                  Cumulative Spend:{' '}
                  <strong>{formatCurrency(monthlyData[activeMonthIdx].actualSpend || 0)}</strong>
                </span>
              )}
            </span>
          </div>

          <span className="text-[11px] text-slate-400 font-mono">
            Annual Cap: {formatCurrency(totalAllocated)}
          </span>
        </div>
      )}
    </div>
  )
}
