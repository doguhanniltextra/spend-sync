import { Shield, Package } from 'lucide-react'
import { formatCurrency } from '@/utils/currency'
import type { TopVendorSpendDto } from '@/types/analytics.types'

interface VendorConcentrationRiskProps {
  topVendors: TopVendorSpendDto[]
  currency?: string
}

export function VendorConcentrationRisk({ topVendors = [], currency = 'TRY' }: VendorConcentrationRiskProps) {
  const topTwoShare = topVendors.slice(0, 2).reduce((acc, v) => acc + v.sharePercent, 0)

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs">
      <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-700 flex items-center justify-center">
            <Shield className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">Supplier Concentration & Pareto Risk</h3>
            <p className="text-[11px] text-slate-500">Live spend dependency ratio calculated from issued purchase orders</p>
          </div>
        </div>
        {topVendors.length > 0 && topTwoShare > 0 && (
          <span className="text-[10px] font-semibold text-slate-700 bg-slate-100 border border-slate-200 px-2 py-0.5 rounded">
            Top 2 = %{Math.round(topTwoShare)} Total PO Spend
          </span>
        )}
      </div>

      {topVendors.length === 0 ? (
        <div className="py-8 text-center text-xs text-slate-500 flex flex-col items-center justify-center gap-2">
          <Package className="w-8 h-8 text-slate-300 stroke-1" />
          <span>No purchase order history available yet to calculate supplier concentration.</span>
        </div>
      ) : (
        <div className="space-y-3">
          {topVendors.map((v, idx) => (
            <div key={v.vendorId} className="space-y-1.5 bg-slate-50/60 p-2.5 rounded-lg border border-slate-100">
              <div className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2 truncate max-w-[280px]">
                  <span className="text-[10px] font-mono text-slate-400">#{idx + 1}</span>
                  <strong className="text-slate-900 font-semibold truncate">{v.vendorName}</strong>
                  {v.tier === 'TIER_1_STRATEGIC' && (
                    <span className="text-[9px] uppercase px-1.5 py-0.2 bg-slate-200 text-slate-700 rounded font-medium">
                      Strategic
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-2 font-mono shrink-0">
                  <span className="font-bold text-slate-900">{formatCurrency(v.volume, currency as any)}</span>
                  <span className="text-slate-500 font-semibold text-[11px]">%{Math.round(v.sharePercent)}</span>
                </div>
              </div>

              {/* Minimalist Corporate Progress Bar */}
              <div className="w-full bg-slate-200/80 h-1.5 rounded-full overflow-hidden flex">
                <div
                  style={{ width: `${Math.max(v.sharePercent, 3)}%` }}
                  className={`h-full rounded-full transition-all ${
                    idx === 0
                      ? 'bg-slate-900'
                      : idx === 1
                      ? 'bg-slate-700'
                      : 'bg-slate-500'
                  }`}
                />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
