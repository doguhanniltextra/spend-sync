import { Clock, ShieldCheck, AlertTriangle } from 'lucide-react'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { formatCurrency } from '@/utils/currency'
import { APPROVAL_COPY } from '../constants/approvalCopy'
import type { EffectiveLimitResponse } from '../services/approvalApi'

interface ApprovalSummaryBarProps {
  pendingCount:  number
  totalExposure: number
  isCFOorRoot:   boolean
  limitData?:    EffectiveLimitResponse | null
}

export function ApprovalSummaryBar({
  pendingCount,
  totalExposure,
  isCFOorRoot,
  limitData,
}: ApprovalSummaryBarProps) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      {/* Metric 1: Pending Approvals */}
      <div className="bg-white rounded-lg p-5 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            {APPROVAL_COPY.metrics.pendingCount}
          </span>
          <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center border border-amber-200">
            <Clock className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-3 flex items-baseline gap-2">
          <span className="text-2xl font-bold font-mono text-slate-900">
            {pendingCount}
          </span>
          <span className="text-xs text-slate-500">
            {APPROVAL_COPY.metrics.pendingDesc}
          </span>
        </div>
      </div>

      {/* Metric 2: Total Exposure */}
      <div className="bg-white rounded-lg p-5 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            {APPROVAL_COPY.metrics.totalExposure}
          </span>
          <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-700 flex items-center justify-center border border-slate-200">
            <span className="font-mono text-sm font-bold">₺</span>
          </div>
        </div>
        <div className="mt-3 flex items-baseline gap-2">
          <CurrencyDisplay
            amount={totalExposure}
            currency="TRY"
            className="text-2xl font-bold font-mono text-slate-900"
          />
        </div>
      </div>

      {/* Metric 3: Manager Authority Limit */}
      <div className="bg-white rounded-lg p-5 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            {APPROVAL_COPY.metrics.authorizedLimit}
          </span>
          <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-200">
            <ShieldCheck className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-3">
          {isCFOorRoot || limitData?.isUnlimited ? (
            <span className="text-sm font-bold text-emerald-700 block">
              {APPROVAL_COPY.metrics.unlimitedLimit}
            </span>
          ) : limitData?.maxAmount ? (
            <div className="flex items-baseline gap-1 font-mono">
              <span className="text-xs text-slate-500 font-sans">{APPROVAL_COPY.metrics.thresholdLabel}</span>
              <strong className="text-lg font-bold text-slate-900">
                {formatCurrency(Number(limitData.maxAmount), (limitData.currency as any) || 'TRY')}
              </strong>
            </div>
          ) : (
            <div className="flex items-center gap-1.5 text-xs text-amber-700">
              <AlertTriangle className="w-3.5 h-3.5" />
              <span>Standard Delegation Matrix</span>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
