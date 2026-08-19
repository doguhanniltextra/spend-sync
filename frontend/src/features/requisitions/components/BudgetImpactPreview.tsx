import { AlertTriangle, AlertOctagon, CheckCircle2 } from 'lucide-react'
import type { BudgetPoolResponse } from '@/types/budget.types'
import { formatCurrency } from '@/utils/currency'
import { THRESHOLDS } from '@/constants/thresholds'
import { REQUISITION_COPY } from '../constants/requisitionCopy'
import { cn } from '@/utils/cn'

interface BudgetImpactPreviewProps {
  pool:          BudgetPoolResponse | null | undefined
  prAmount:      number
  currency:      string
}

export function BudgetImpactPreview({
  pool,
  prAmount,
  currency,
}: BudgetImpactPreviewProps) {
  if (!pool) {
    return (
      <div className="p-4 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-500">
        <p>{REQUISITION_COPY.budgetCheck.noPoolAlert}</p>
      </div>
    )
  }

  const allocated = pool.allocatedAmount || 0
  const currentCommitted = (pool.spentAmount || 0) + (pool.reservedAmount || 0)
  const currentAvailable = pool.availableAmount || 0
  const projectedCommitted = currentCommitted + prAmount

  const projectedPercent =
    allocated > 0 ? Math.min(Math.round((projectedCommitted / allocated) * 100), 100) : 0

  const isExceeded = projectedCommitted > allocated
  const isCritical = projectedPercent >= THRESHOLDS.budget.criticalPercent
  const isWarning  = projectedPercent >= THRESHOLDS.budget.warningPercent && !isCritical

  return (
    <div className="bg-white rounded-lg p-5 border border-slate-200 shadow-2xs space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-800">
            {REQUISITION_COPY.budgetCheck.title}
          </h4>
          <p className="text-[11px] text-slate-500 font-mono mt-0.5">
            {pool.costCenterCode} • {pool.costCenterName} ({pool.fiscalYear})
          </p>
        </div>

        <span
          className={cn(
            'text-xs font-bold font-mono px-2.5 py-1 rounded-full border',
            isExceeded
              ? 'bg-red-50 text-red-700 border-red-300'
              : isWarning
              ? 'bg-amber-50 text-amber-700 border-amber-300'
              : 'bg-slate-100 text-slate-700 border-slate-200'
          )}
        >
          Projected: %{projectedPercent}
        </span>
      </div>

      {/* 4 Financial Metric Columns */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs bg-slate-50 p-3.5 rounded-lg border border-slate-200 font-mono">
        <div>
          <span className="text-[10px] text-slate-500 font-sans block">
            {REQUISITION_COPY.budgetCheck.allocatedLabel}
          </span>
          <strong className="text-slate-900">
            {formatCurrency(allocated, currency as any)}
          </strong>
        </div>

        <div>
          <span className="text-[10px] text-slate-500 font-sans block">
            {REQUISITION_COPY.budgetCheck.committedLabel}
          </span>
          <strong className="text-slate-700">
            {formatCurrency(currentCommitted, currency as any)}
          </strong>
        </div>

        <div>
          <span className="text-[10px] text-slate-500 font-sans block">
            {REQUISITION_COPY.budgetCheck.thisPRLabel}
          </span>
          <strong className="text-brand-700">
            {formatCurrency(prAmount, currency as any)}
          </strong>
        </div>

        <div>
          <span className="text-[10px] text-slate-500 font-sans block">
            {REQUISITION_COPY.budgetCheck.availableLabel}
          </span>
          <strong className={isExceeded ? 'text-red-700' : 'text-emerald-700'}>
            {formatCurrency(currentAvailable, currency as any)}
          </strong>
        </div>
      </div>

      {/* Progress bar */}
      <div className="space-y-1.5">
        <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
          <div
            style={{ width: `${projectedPercent}%` }}
            className={cn(
              'h-full rounded-full transition-all duration-300',
              isExceeded
                ? 'bg-red-600'
                : isCritical
                ? 'bg-red-500'
                : isWarning
                ? 'bg-amber-500'
                : 'bg-slate-800'
            )}
          />
        </div>
      </div>

      {/* Warning/Alert Messages */}
      {isExceeded && (
        <div className="p-3 rounded-lg bg-red-50 border border-red-200 flex items-start gap-2.5 text-xs text-red-700">
          <AlertOctagon className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{REQUISITION_COPY.budgetCheck.hardStopAlert}</span>
        </div>
      )}

      {!isExceeded && isWarning && (
        <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 flex items-start gap-2.5 text-xs text-amber-800">
          <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{REQUISITION_COPY.budgetCheck.warningAlert}</span>
        </div>
      )}

      {!isExceeded && !isWarning && prAmount > 0 && (
        <div className="p-2.5 rounded-lg bg-emerald-50/70 border border-emerald-200 flex items-center gap-2 text-xs text-emerald-800">
          <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
          <span>Sufficient budget available for this purchase requisition.</span>
        </div>
      )}
    </div>
  )
}
