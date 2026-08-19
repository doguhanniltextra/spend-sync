import { CheckCheck, X } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { APPROVAL_COPY } from '../constants/approvalCopy'

interface BulkApprovalActionBarProps {
  selectedCount: number
  totalValue:    number
  isLoading:     boolean
  onApproveAll:  () => void
  onClear:       () => void
}

export function BulkApprovalActionBar({
  selectedCount,
  totalValue,
  isLoading,
  onApproveAll,
  onClear,
}: BulkApprovalActionBarProps) {
  if (selectedCount === 0) return null

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-slate-900 text-white px-5 py-3.5 rounded-xl shadow-2xl flex items-center gap-6 border border-slate-700 animate-slide-up">
      <div className="flex items-center gap-3">
        <span className="font-mono font-bold bg-slate-800 text-slate-200 px-2 py-0.5 rounded text-xs">
          {selectedCount}
        </span>
        <span className="text-xs text-slate-300 font-medium">
          {APPROVAL_COPY.bulk.selectedCount}
        </span>
      </div>

      <div className="h-4 w-px bg-slate-700" />

      <div className="flex items-center gap-1.5 text-xs font-mono">
        <span className="text-slate-400 font-sans">{APPROVAL_COPY.bulk.totalValue}</span>
        <CurrencyDisplay amount={totalValue} currency="TRY" className="font-bold text-white text-sm" />
      </div>

      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={onClear}
          className="bg-transparent text-slate-300 border-slate-700 hover:bg-slate-800 hover:text-white"
        >
          <X className="w-3.5 h-3.5 mr-1" />
          {APPROVAL_COPY.bulk.clearSelection}
        </Button>

        <Button
          size="sm"
          onClick={onApproveAll}
          isLoading={isLoading}
          leftIcon={<CheckCheck className="w-3.5 h-3.5" />}
          className="bg-emerald-600 hover:bg-emerald-700 text-white"
        >
          {APPROVAL_COPY.bulk.bulkApproveCTA}
        </Button>
      </div>
    </div>
  )
}
