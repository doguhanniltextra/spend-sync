import { CheckCircle2, Clock } from 'lucide-react'
import type { GoodsReceiptStatus } from '@/types/receiving.types'

interface GoodsReceiptStatusBadgeProps {
  status: GoodsReceiptStatus
}

export function GoodsReceiptStatusBadge({ status }: GoodsReceiptStatusBadgeProps) {
  switch (status) {
    case 'COMPLETED':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
          <CheckCircle2 className="w-3 h-3 text-emerald-600" />
          Received & Verified (GR Issued)
        </span>
      )
    case 'CANCELLED':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-600 border border-slate-200">
          <Clock className="w-3 h-3 text-slate-500" />
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
