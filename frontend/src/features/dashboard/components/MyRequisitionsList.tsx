import { useNavigate } from 'react-router-dom'
import { Plus } from 'lucide-react'
import type { RequisitionSummaryResponse } from '@/types/requisition.types'
import { formatCurrency } from '@/utils/currency'
import { formatDate } from '@/utils/date'
import { getStatusConfig } from '@/constants/workflow'
import { ROUTES } from '@/constants/routes'
import { DASHBOARD_COPY } from '../constants/dashboardCopy'

interface MyRequisitionsListProps {
  requisitions: RequisitionSummaryResponse[]
}

export function MyRequisitionsList({ requisitions }: MyRequisitionsListProps) {
  const navigate = useNavigate()

  return (
    <div className="bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
      <div className="px-5 py-4 border-b border-slate-200 bg-white flex items-center justify-between">
        <div>
          <h3 className="text-sm font-bold text-slate-900">
            {DASHBOARD_COPY.requisitions.title}
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            {DASHBOARD_COPY.requisitions.subtitle}
          </p>
        </div>
        <button
          onClick={() => navigate(ROUTES.requisitions.new)}
          type="button"
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-white bg-slate-900 hover:bg-slate-800 rounded-md transition-colors shadow-2xs"
        >
          <Plus className="w-3.5 h-3.5" />
          {DASHBOARD_COPY.requisitions.createCTA}
        </button>
      </div>

      {requisitions.length === 0 ? (
        <div className="p-8 text-center text-xs text-slate-500">
          {DASHBOARD_COPY.requisitions.emptyText}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-5 py-3">{DASHBOARD_COPY.requisitions.colPrNumber}</th>
                <th className="px-4 py-3">{DASHBOARD_COPY.requisitions.colTitle}</th>
                <th className="px-4 py-3 text-right">{DASHBOARD_COPY.requisitions.colAmount}</th>
                <th className="px-4 py-3 text-center">{DASHBOARD_COPY.requisitions.colStatus}</th>
                <th className="px-5 py-3 text-right">{DASHBOARD_COPY.requisitions.colDate}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {requisitions.map((req) => {
                const statusCfg = getStatusConfig(req.status)
                const StatusIcon = statusCfg.icon

                return (
                  <tr key={req.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-5 py-3.5 font-mono font-semibold text-slate-900">
                      {req.prNumber}
                    </td>
                    <td className="px-4 py-3.5 font-medium text-slate-800">
                      <div>{req.title}</div>
                      <span className="text-[10px] font-mono text-slate-400">
                        {req.costCenterCode} • {req.costCenterName}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right font-mono font-bold text-slate-900">
                      {formatCurrency(req.totalAmount ?? req.totalEstimatedAmount ?? 0, req.currency as any)}
                    </td>
                    <td className="px-4 py-3.5 text-center">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium ${statusCfg.className}`}
                      >
                        <StatusIcon className="w-3 h-3" />
                        {statusCfg.label}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-right font-sans text-slate-500">
                      {formatDate(req.createdAt)}
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
