import type { UUID, ISODateString } from './common.types'

export type BudgetStatus = 'DRAFT' | 'ACTIVE' | 'FROZEN' | 'CLOSED'
export type BudgetPeriodType = 'ANNUAL' | 'QUARTERLY' | 'MONTHLY'
export type BudgetEnforcementMode = 'HARD_STOP' | 'ADVISORY' | 'TOLERANCE_BUFFER'

export interface BudgetPoolResponse {
  id:                  UUID
  tenantId:            UUID
  legalEntityId:       UUID
  legalEntityName:     string
  costCenterId:        UUID
  costCenterName:      string
  costCenterCode:      string
  fiscalYear:          number
  periodType:          BudgetPeriodType
  periodValue:         string
  status:              BudgetStatus
  enforcementMode:     BudgetEnforcementMode
  tolerancePercentage: number
  allocatedAmount:     number
  reservedAmount:      number
  spentAmount:         number
  availableAmount:     number
  maxAllowedAllocation:number
  currency:            string
  createdAt:           ISODateString
  updatedAt:           ISODateString
}

export interface BudgetSummaryResponse {
  fiscalYear:     number
  totalPools:     number
  totalAllocated: number
  totalReserved:  number
  totalSpent:     number
  totalAvailable: number
  pools:          BudgetPoolResponse[]
}
