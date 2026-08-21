import { CheckCircle2, Clock, Landmark } from 'lucide-react'
import type { InvoicePaymentTimelineStep } from '../../types/vendorPortal.types'

interface Props {
  invoiceNumber: string
  payableAmount: number
  currency: string
  dueDate: string
  maskedPayoutIban: string
  timeline: InvoicePaymentTimelineStep[]
}

export function VendorPaymentTimeline({
  invoiceNumber,
  payableAmount,
  currency,
  dueDate,
  maskedPayoutIban,
  timeline,
}: Props) {
  return (
    <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-6">
      {/* Header Info */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-100">
        <div>
          <span className="text-[11px] font-bold text-teal-700 uppercase tracking-wider">
            Live Accounts Payable Progression (Maturity: {dueDate})
          </span>
          <h3 className="text-base font-bold text-slate-900 mt-0.5">
            Invoice: <span className="font-mono">{invoiceNumber}</span>
          </h3>
        </div>

        <div className="flex items-center gap-4 text-right">
          <div>
            <p className="text-xs text-slate-400">Scheduled Net Payout</p>
            <p className="text-lg font-extrabold text-slate-900">
              {payableAmount.toLocaleString()} {currency}
            </p>
          </div>
          <div className="hidden sm:block pl-4 border-l border-slate-200 text-left">
            <p className="text-xs text-slate-400">Target Settlement Account</p>
            <p className="text-xs font-mono font-bold text-slate-800 flex items-center gap-1">
              <Landmark className="w-3.5 h-3.5 text-teal-600" />
              {maskedPayoutIban}
            </p>
          </div>
        </div>
      </div>

      {/* 4-Step Interactive Horizontal Stepper */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 relative">
        {timeline.map((step, idx) => {
          const isDone = step.completed
          return (
            <div
              key={step.step}
              className={`p-4 rounded-xl border relative transition ${
                isDone
                  ? 'bg-emerald-50/70 border-emerald-200 text-emerald-950 shadow-sm'
                  : 'bg-slate-50 border-slate-200 text-slate-500'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  Step {idx + 1}
                </span>
                {isDone ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                ) : (
                  <Clock className="w-4 h-4 text-slate-400" />
                )}
              </div>
              <p className={`text-xs font-bold ${isDone ? 'text-emerald-900' : 'text-slate-700'}`}>
                {step.title}
              </p>
              <p className="text-[11px] text-slate-500 mt-1 leading-snug">
                {step.description}
              </p>
              {step.timestamp && (
                <p className="text-[10px] text-slate-400 mt-2 font-mono">
                  {step.timestamp}
                </p>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
