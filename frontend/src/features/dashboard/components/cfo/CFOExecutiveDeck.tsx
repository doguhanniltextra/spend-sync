import { Loader2 } from 'lucide-react'
import { SpendDistributionDonut } from './SpendDistributionDonut'
import { CashFlowForecastChart } from './CashFlowForecastChart'
import { VendorConcentrationRisk } from './VendorConcentrationRisk'
import { useCfoAnalytics } from '../../hooks/useCfoAnalytics'
import type { BudgetPoolResponse } from '@/types/budget.types'

interface CFOExecutiveDeckProps {
  pools: BudgetPoolResponse[]
}

export function CFOExecutiveDeck({ pools }: CFOExecutiveDeckProps) {
  const { deck, isLoading } = useCfoAnalytics()

  if (isLoading && !deck) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-8 shadow-2xs flex items-center justify-center gap-2 text-slate-400 text-xs">
        <Loader2 className="w-4 h-4 animate-spin text-indigo-600" />
        <span>Loading live financial analytics...</span>
      </div>
    )
  }

  return (
    <div className="space-y-5">
      {/* Main Charts Grid: Live Cost Center Donut & Live 4-Month Outflow Forecast */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <SpendDistributionDonut pools={pools} />
        <CashFlowForecastChart forecast={deck?.cashOutflowForecast || []} currency={deck?.currency} />
      </div>

      {/* Bottom Grid: Live Vendor Concentration Pareto Analysis */}
      <VendorConcentrationRisk topVendors={deck?.topVendors || []} currency={deck?.currency} />
    </div>
  )
}
