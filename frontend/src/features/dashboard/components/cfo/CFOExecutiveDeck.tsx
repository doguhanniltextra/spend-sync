import { SpendDistributionDonut } from './SpendDistributionDonut'
import { CashFlowForecastChart } from './CashFlowForecastChart'
import { FacilityLogisticsHeatmap } from './FacilityLogisticsHeatmap'
import { VendorConcentrationRisk } from './VendorConcentrationRisk'
import { SmartFinancialSignals } from './SmartFinancialSignals'
import type { BudgetPoolResponse } from '@/types/budget.types'

interface CFOExecutiveDeckProps {
  pools:          BudgetPoolResponse[]
  totalAllocated: number
  totalSpent:     number
  totalReserved:  number
}

export function CFOExecutiveDeck({
  pools,
  totalAllocated,
  totalSpent,
  totalReserved,
}: CFOExecutiveDeckProps) {
  return (
    <div className="space-y-5">
      {/* 1. Autonomous AI Financial Signals */}
      <SmartFinancialSignals />

      {/* 2. Top Charts Row: Donut Spend Distribution & 12-Month Burn Rate */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <SpendDistributionDonut pools={pools} />
        <CashFlowForecastChart
          totalAllocated={totalAllocated}
          totalSpent={totalSpent}
          totalReserved={totalReserved}
        />
      </div>

      {/* 3. Bottom Charts Row: Facility Radar & Vendor Concentration Risk */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <FacilityLogisticsHeatmap />
        <VendorConcentrationRisk />
      </div>
    </div>
  )
}
