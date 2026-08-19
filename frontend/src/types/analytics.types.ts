export interface CategorySpendDto {
  category: string
  amount: number
  sharePercent: number
}

export interface MonthlyOutflowDto {
  month: string
  confirmedDueInvoices: number
  projectedPoDeliveries: number
  totalExpectedOutflow: number
}

export interface TopVendorSpendDto {
  vendorId: string
  vendorName: string
  taxNumber?: string
  tier: string
  volume: number
  sharePercent: number
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW'
}

export interface ThreeWayMatchIntegrityDto {
  totalInvoices: number
  matchedInvoices: number
  discrepancyHoldInvoices: number
  firstTimeMatchRatePercent: number
  discrepancyBlockedAmount: number
}

export interface CfoExecutiveDeckResponse {
  totalSpendYtd: number
  totalCommittedSpend: number
  totalAllocatedBudget: number
  overallBudgetUtilizationPercent: number
  currency: string
  categoryDistribution: CategorySpendDto[]
  cashOutflowForecast: MonthlyOutflowDto[]
  topVendors: TopVendorSpendDto[]
  matchIntegrity: ThreeWayMatchIntegrityDto
}
