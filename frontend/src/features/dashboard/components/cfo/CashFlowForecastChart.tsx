import { TrendingUp, Calendar } from 'lucide-react'
import { formatCurrency } from '@/utils/currency'
import type { MonthlyOutflowDto } from '@/types/analytics.types'

interface CashFlowForecastChartProps {
  forecast: MonthlyOutflowDto[]
  currency?: string
}

export function CashFlowForecastChart({ forecast = [], currency = 'TRY' }: CashFlowForecastChartProps) {
  const maxVal = Math.max(
    ...forecast.map((f) => f.totalExpectedOutflow || 0),
    10000
  )

  const totalUpcomingOutflow = forecast.reduce((acc, f) => acc + (f.totalExpectedOutflow || 0), 0)

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs">
      <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-700 flex items-center justify-center">
            <TrendingUp className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">4-Month Liquidity & Cash Outflow Forecast</h3>
            <p className="text-[11px] text-slate-500">Confirmed invoice maturities and projected open PO deliveries</p>
          </div>
        </div>
        <div className="text-right">
          <span className="text-[10px] text-slate-400 block font-medium">Total 4-Month Demand</span>
          <span className="text-xs font-mono font-bold text-slate-900">
            {formatCurrency(totalUpcomingOutflow, currency as any)}
          </span>
        </div>
      </div>

      {forecast.length === 0 ? (
        <div className="py-8 text-center text-xs text-slate-400">
          No invoice maturities or open POs scheduled for upcoming months.
        </div>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {forecast.map((item, idx) => {
              const heightPct = Math.max(Math.round((item.totalExpectedOutflow / maxVal) * 100), 8)

              return (
                <div
                  key={item.month}
                  className="bg-slate-50/60 border border-slate-100 rounded-xl p-3 flex flex-col justify-between"
                >
                  <div className="flex items-center justify-between text-xs font-semibold text-slate-700 mb-2">
                    <span className="flex items-center gap-1.5 text-[11px]">
                      <Calendar className="w-3 h-3 text-slate-400" />
                      {item.month}
                    </span>
                  </div>

                  <div className="h-20 flex items-end justify-center py-1">
                    <div
                      style={{ height: `${heightPct}%` }}
                      className={`w-full max-w-[36px] rounded-t-md transition-all relative group ${
                        idx === 0
                          ? 'bg-slate-900'
                          : idx === 1
                          ? 'bg-slate-700'
                          : 'bg-slate-500'
                      }`}
                    >
                      <div className="opacity-0 group-hover:opacity-100 transition-opacity absolute -top-7 left-1/2 -translate-x-1/2 bg-slate-900 text-white text-[10px] font-mono px-1.5 py-0.5 rounded shadow pointer-events-none whitespace-nowrap z-10">
                        {formatCurrency(item.totalExpectedOutflow, currency as any)}
                      </div>
                    </div>
                  </div>

                  <div className="border-t border-slate-200/60 pt-2 mt-2 space-y-1 text-[10px]">
                    <div className="flex justify-between text-slate-500 font-mono">
                      <span>Due Inv:</span>
                      <span className="font-semibold text-slate-800">{formatCurrency(item.confirmedDueInvoices, currency as any)}</span>
                    </div>
                    <div className="flex justify-between text-slate-500 font-mono">
                      <span>Open PO:</span>
                      <span className="font-semibold text-slate-800">{formatCurrency(item.projectedPoDeliveries, currency as any)}</span>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
