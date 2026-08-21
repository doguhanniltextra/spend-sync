import { useState } from 'react'
import { Zap, Calendar, ArrowRight, CheckCircle2, AlertCircle } from 'lucide-react'
import { useAcceptEarlyDiscount } from '../../hooks/useVendorPortalQueries'

interface Props {
  invoiceId: string
  invoiceNumber: string
  originalAmount: number
  originalDueDate: string
  discountPercentage: number
  discountAmount: number
  netPayoutAmount: number
  currency: string
  acceleratedDate: string
  status?: string
}

export function EarlyDiscountOfferCard({
  invoiceId,
  invoiceNumber,
  originalAmount,
  originalDueDate,
  discountPercentage,
  discountAmount,
  netPayoutAmount,
  currency,
  acceleratedDate,
  status = 'AVAILABLE',
}: Props) {
  const discountMutation = useAcceptEarlyDiscount(invoiceId)
  const [accepted, setAccepted] = useState(status === 'ACCEPTED')
  const [error, setError] = useState<string | null>(null)

  const handleAccept = async () => {
    try {
      setError(null)
      await discountMutation.mutateAsync({ discountPercentage })
      setAccepted(true)
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to accept early payment offer.')
    }
  }

  if (accepted) {
    return (
      <div className="p-6 rounded-2xl bg-emerald-50 border border-emerald-300 shadow-sm flex items-center justify-between text-emerald-950">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-emerald-200 text-emerald-800 flex items-center justify-center flex-shrink-0">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-sm font-bold">Early Payment Discount Locked in</h4>
            <p className="text-xs text-emerald-700 mt-0.5">
              Net payout of <strong className="font-bold">{netPayoutAmount.toLocaleString()} {currency}</strong> is scheduled for bank execution on <strong className="font-bold">{acceleratedDate}</strong>.
            </p>
          </div>
        </div>
        <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-600 text-white">
          T+3 Priority
        </span>
      </div>
    )
  }

  return (
    <div className="p-6 rounded-2xl bg-gradient-to-r from-emerald-500/10 via-teal-500/10 to-indigo-500/10 border border-emerald-300 shadow-sm space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-lg bg-emerald-600 text-white shadow">
            <Zap className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-sm font-extrabold text-slate-900">
              Accelerated Cash Flow Opportunity: {invoiceNumber} (Dynamic Discounting)
            </h4>
            <p className="text-xs text-slate-500">
              Get paid in 3 business days instead of waiting for standard 30-day net terms.
            </p>
          </div>
        </div>

        <span className="self-start sm:self-auto px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-600 text-white shadow-sm">
          %{discountPercentage} Discount Rate
        </span>
      </div>

      {error && (
        <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4" />
          <span>{error}</span>
        </div>
      )}

      {/* Comparison Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2 text-xs">
        <div className="p-3.5 rounded-xl bg-white border border-slate-200 shadow-sm">
          <p className="text-slate-400 font-medium">Standard Maturity</p>
          <p className="text-sm font-bold text-slate-800 mt-1 flex items-center gap-1">
            <Calendar className="w-3.5 h-3.5 text-slate-400" />
            {originalDueDate}
          </p>
          <p className="text-[11px] text-slate-500 mt-0.5 font-medium">
            Full Amount: {originalAmount.toLocaleString()} {currency}
          </p>
        </div>

        <div className="p-3.5 rounded-xl bg-white border border-slate-200 shadow-sm">
          <p className="text-slate-400 font-medium">Discount Deducted</p>
          <p className="text-sm font-bold text-emerald-700 mt-1">
            -{discountAmount.toLocaleString()} {currency}
          </p>
          <p className="text-[11px] text-slate-500 mt-0.5 font-medium">
            %{discountPercentage} 2/10 Net 30 standard APR
          </p>
        </div>

        <div className="p-3.5 rounded-xl bg-teal-50 border border-teal-200 shadow-sm">
          <p className="text-teal-700 font-semibold">Immediate Bank Payout</p>
          <p className="text-base font-black text-teal-900 mt-0.5">
            {netPayoutAmount.toLocaleString()} {currency}
          </p>
          <p className="text-[11px] text-teal-700 font-medium mt-0.5">
            Payout on: <strong>{acceleratedDate}</strong> (T+3)
          </p>
        </div>
      </div>

      {/* Accept Offer Action */}
      <div className="flex items-center justify-end gap-3 pt-2">
        <button
          type="button"
          disabled={discountMutation.isPending}
          onClick={handleAccept}
          className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-xs shadow-md shadow-emerald-600/20 flex items-center gap-2 transition disabled:opacity-50"
        >
          {discountMutation.isPending ? (
            'Processing Acceptance...'
          ) : (
            <>
              <span>Accept %{discountPercentage} Discount & Accelerate Cash</span>
              <ArrowRight className="w-4 h-4" />
            </>
          )}
        </button>
      </div>
    </div>
  )
}
