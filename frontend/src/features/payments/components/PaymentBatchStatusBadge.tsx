import { CheckCircle2, Clock, Send, XCircle } from 'lucide-react'
import type { PaymentBatchStatus } from '@/types/payment.types'

interface PaymentBatchStatusBadgeProps {
  status: PaymentBatchStatus
}

export function PaymentBatchStatusBadge({ status }: PaymentBatchStatusBadgeProps) {
  switch (status) {
    case 'DRAFT':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
          <Clock className="w-3 h-3 text-amber-600" />
          Draft (Awaiting Approval)
        </span>
      )
    case 'APPROVED':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
          <CheckCircle2 className="w-3 h-3 text-blue-600" />
          Approved (Ready for Dispatch)
        </span>
      )
    case 'DISPATCHED':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
          <Send className="w-3 h-3 text-emerald-600" />
          Dispatched & Settled
        </span>
      )
    case 'CANCELLED':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-600 border border-slate-200">
          <XCircle className="w-3 h-3 text-slate-400" />
          Cancelled
        </span>
      )
    default:
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-800">
          {status}
        </span>
      )
  }
}
