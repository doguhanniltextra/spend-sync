import { useState } from 'react'
import { Check, X, Loader2 } from 'lucide-react'
import type { RequisitionDetailResponse } from '@/types/requisition.types'
import { formatCurrency } from '@/utils/currency'
import { formatDate } from '@/utils/date'
import { DASHBOARD_COPY } from '../constants/dashboardCopy'

interface PendingApprovalsListProps {
  requests:    RequisitionDetailResponse[]
  onApprove:   (id: string) => Promise<unknown>
  onReject:    (id: string, reason: string) => Promise<unknown>
  isApproving: boolean
  isRejecting: boolean
}

export function PendingApprovalsList({
  requests,
  onApprove,
  onReject,
  isApproving,
  isRejecting,
}: PendingApprovalsListProps) {
  const [activeId, setActiveId] = useState<string | null>(null)

  const handleApprove = async (id: string) => {
    try {
      setActiveId(id)
      await onApprove(id)
    } finally {
      setActiveId(null)
    }
  }

  const handleReject = async (id: string) => {
    const reason = window.prompt('Please provide a reason for rejecting this request:')
    if (!reason || !reason.trim()) return
    try {
      setActiveId(id)
      await onReject(id, reason.trim())
    } finally {
      setActiveId(null)
    }
  }

  return (
    <div className="bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
      <div className="px-5 py-4 border-b border-slate-200 bg-white flex items-center justify-between">
        <div>
          <h3 className="text-sm font-bold text-slate-900">
            {DASHBOARD_COPY.pendingTable.title}
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            {DASHBOARD_COPY.pendingTable.subtitle}
          </p>
        </div>
        <span className="px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-800 text-xs font-semibold">
          {requests.length} Pending
        </span>
      </div>

      {requests.length === 0 ? (
        <div className="p-8 text-center text-xs text-slate-500">
          {DASHBOARD_COPY.pendingTable.emptyText}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-5 py-3">{DASHBOARD_COPY.pendingTable.colPrNumber}</th>
                <th className="px-4 py-3">{DASHBOARD_COPY.pendingTable.colTitle}</th>
                <th className="px-4 py-3">{DASHBOARD_COPY.pendingTable.colRequester}</th>
                <th className="px-4 py-3 text-right">{DASHBOARD_COPY.pendingTable.colAmount}</th>
                <th className="px-5 py-3 text-right">{DASHBOARD_COPY.pendingTable.colAction}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {requests.map((req) => {
                const isOperating = activeId === req.id && (isApproving || isRejecting)

                return (
                  <tr key={req.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-5 py-3.5 font-mono font-semibold text-slate-900">
                      <div>{req.requisitionNumber ?? req.prNumber}</div>
                      <span className="text-[10px] font-sans text-slate-400">
                        {formatDate(req.createdAt)}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-slate-800 font-medium max-w-xs truncate">
                      <div>{req.title}</div>
                      <span className="text-[10px] text-slate-400 font-normal truncate block">
                        {req.justification || 'Standard operational requisition'}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-slate-700">
                      <div>{req.requisitionerName ?? req.requesterName}</div>
                      <span className="text-[10px] font-mono text-slate-400">
                        {req.costCenterCode}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right font-mono font-bold text-slate-900">
                      {formatCurrency(req.totalAmount ?? req.totalEstimatedAmount ?? 0, req.currency as any)}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => handleApprove(req.id)}
                          disabled={isOperating}
                          type="button"
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-300 rounded transition-colors disabled:opacity-50"
                        >
                          {isOperating ? (
                            <Loader2 className="w-3 h-3 animate-spin" />
                          ) : (
                            <Check className="w-3 h-3" />
                          )}
                          {DASHBOARD_COPY.pendingTable.quickApprove}
                        </button>
                        <button
                          onClick={() => handleReject(req.id)}
                          disabled={isOperating}
                          type="button"
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-red-700 bg-red-50 hover:bg-red-100 border border-red-300 rounded transition-colors disabled:opacity-50"
                        >
                          <X className="w-3 h-3" />
                          {DASHBOARD_COPY.pendingTable.quickReject}
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
