import { useNavigate } from 'react-router-dom'
import { AlertTriangle, Clock, CreditCard, ArrowRight } from 'lucide-react'
import type { RequisitionDetailResponse } from '@/types/requisition.types'
import type { DueInvoiceResponse } from '@/types/payment.types'
import type { SupplierInvoiceResponse } from '@/types/matching.types'
import { formatCurrency } from '@/utils/currency'
import { ROUTES } from '@/constants/routes'
import { DASHBOARD_COPY } from '../constants/dashboardCopy'

interface ExecutiveActionCardsProps {
  pendingApprovals: RequisitionDetailResponse[]
  dueInvoices:      DueInvoiceResponse[]
  allInvoices:      SupplierInvoiceResponse[]
}

export function ExecutiveActionCards({
  pendingApprovals,
  dueInvoices,
  allInvoices,
}: ExecutiveActionCardsProps) {
  const navigate = useNavigate()

  const discrepancyInvoices = allInvoices.filter(
    (inv) => inv.status === 'DISCREPANCY_HOLD'
  )

  const hasItems =
    pendingApprovals.length > 0 ||
    dueInvoices.length > 0 ||
    discrepancyInvoices.length > 0

  if (!hasItems) {
    return (
      <div className="bg-white rounded-lg p-6 border border-slate-200 shadow-2xs text-center">
        <div className="w-8 h-8 rounded-full bg-emerald-50 text-emerald-700 flex items-center justify-center mx-auto mb-2 border border-emerald-200">
          ✓
        </div>
        <h4 className="text-sm font-semibold text-slate-900">
          {DASHBOARD_COPY.actions.emptyTitle}
        </h4>
        <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
          {DASHBOARD_COPY.actions.emptyDesc}
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-sm font-bold text-slate-900">
            {DASHBOARD_COPY.actions.sectionTitle}
          </h3>
          <p className="text-xs text-slate-500">
            {DASHBOARD_COPY.actions.sectionSubtitle}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* 1. Pending Approvals Card */}
        {pendingApprovals.length > 0 && (
          <div className="bg-white rounded-lg p-4 border border-amber-300 bg-amber-50/10 shadow-2xs flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between text-amber-700 mb-2">
                <span className="text-xs font-bold uppercase tracking-wider flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5" />
                  Approval Queue
                </span>
                <span className="px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 text-[11px] font-bold">
                  {pendingApprovals.length} Requests
                </span>
              </div>
              <p className="text-xs text-slate-700">
                Totaling{' '}
                <strong className="font-mono text-slate-900">
                  {formatCurrency(
                    pendingApprovals.reduce(
                      (acc, p) => acc + (p.totalAmount ?? p.totalEstimatedAmount ?? 0),
                      0
                    )
                  )}
                </strong>{' '}
                awaiting signature.
              </p>
            </div>
            <button
              onClick={() => navigate(ROUTES.approvals.root)}
              type="button"
              className="mt-4 w-full inline-flex items-center justify-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-900 bg-white hover:bg-slate-50 border border-slate-300 rounded-md transition-colors shadow-2xs"
            >
              {DASHBOARD_COPY.actions.viewAction}
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* 2. Due Invoices for Payment */}
        {dueInvoices.length > 0 && (
          <div className="bg-white rounded-lg p-4 border border-blue-300 bg-blue-50/10 shadow-2xs flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between text-blue-700 mb-2">
                <span className="text-xs font-bold uppercase tracking-wider flex items-center gap-1.5">
                  <CreditCard className="w-3.5 h-3.5" />
                  Ready for Payment
                </span>
                <span className="px-2 py-0.5 rounded-full bg-blue-100 text-blue-800 text-[11px] font-bold">
                  {dueInvoices.length} Invoices
                </span>
              </div>
              <p className="text-xs text-slate-700">
                <strong className="font-mono text-slate-900">
                  {formatCurrency(
                    dueInvoices.reduce((acc, i) => acc + (i.totalAmount || 0), 0)
                  )}
                </strong>{' '}
                ready to be batched and released.
              </p>
            </div>
            <button
              onClick={() => navigate(ROUTES.payments.root)}
              type="button"
              className="mt-4 w-full inline-flex items-center justify-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-900 bg-white hover:bg-slate-50 border border-slate-300 rounded-md transition-colors shadow-2xs"
            >
              {DASHBOARD_COPY.actions.approveAction}
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* 3. Discrepancy Holds */}
        {discrepancyInvoices.length > 0 && (
          <div className="bg-white rounded-lg p-4 border border-red-300 bg-red-50/10 shadow-2xs flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between text-red-700 mb-2">
                <span className="text-xs font-bold uppercase tracking-wider flex items-center gap-1.5">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  3-Way Discrepancy
                </span>
                <span className="px-2 py-0.5 rounded-full bg-red-100 text-red-800 text-[11px] font-bold">
                  {discrepancyInvoices.length} Holds
                </span>
              </div>
              <p className="text-xs text-slate-700">
                Price or quantity mismatch detected against Purchase Order.
              </p>
            </div>
            <button
              onClick={() => navigate(ROUTES.matching.root)}
              type="button"
              className="mt-4 w-full inline-flex items-center justify-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-900 bg-white hover:bg-slate-50 border border-slate-300 rounded-md transition-colors shadow-2xs"
            >
              {DASHBOARD_COPY.actions.resolveAction}
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
