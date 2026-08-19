import { Drawer } from '@/components/ui/Drawer'
import { Button } from '@/components/ui/Button'
import { StatusBadge } from '@/components/ui/Badge'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { ApprovalChainTimeline } from '@/features/requisitions/components/ApprovalChainTimeline'
import type { RequisitionDetailResponse } from '@/types/requisition.types'
import { APPROVAL_COPY } from '../constants/approvalCopy'

interface ApprovalDetailDrawerProps {
  requisition: RequisitionDetailResponse | null
  isOpen:      boolean
  onClose:     () => void
  onApprove:   (req: RequisitionDetailResponse) => void
  onReject:    (req: RequisitionDetailResponse) => void
}

export function ApprovalDetailDrawer({
  requisition,
  isOpen,
  onClose,
  onApprove,
  onReject,
}: ApprovalDetailDrawerProps) {
  if (!requisition) return null

  const prNumber = requisition.requisitionNumber ?? requisition.prNumber
  const requester = requisition.requisitionerName ?? requisition.requesterName
  const amount = requisition.totalAmount ?? requisition.totalEstimatedAmount ?? 0

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={prNumber}
      subtitle={requisition.title}
      size="xl"
      footer={
        <div className="flex items-center justify-between w-full">
          <Button variant="outline" size="sm" onClick={onClose}>
            Close
          </Button>

          <div className="flex items-center gap-2">
            <Button
              variant="danger"
              size="sm"
              onClick={() => onReject(requisition)}
            >
              {APPROVAL_COPY.drawer.btnReject}
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => onApprove(requisition)}
            >
              {APPROVAL_COPY.drawer.btnApprove}
            </Button>
          </div>
        </div>
      }
    >
      <div className="space-y-6 text-xs">
        {/* Header Summary */}
        <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 flex items-center justify-between">
          <div>
            <span className="text-[10px] uppercase font-semibold text-slate-500 block">
              Total Amount
            </span>
            <CurrencyDisplay
              amount={amount}
              currency={requisition.currency as any}
              className="text-lg font-bold text-slate-900"
            />
          </div>
          <StatusBadge status={requisition.status} />
        </div>

        {/* Scope Overview */}
        <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
          <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
            {APPROVAL_COPY.drawer.sectionOverview}
          </h4>

          <div className="grid grid-cols-2 gap-3 text-slate-700">
            <div>
              <span className="text-slate-400 block text-[10px]">Requester:</span>
              <strong className="text-slate-900">{requester}</strong>{' '}
              <span className="text-slate-500">({requisition.requisitionerEmail})</span>
            </div>

            <div>
              <span className="text-slate-400 block text-[10px]">Cost Center:</span>
              <strong className="text-slate-900">{requisition.costCenterName}</strong>{' '}
              <span className="font-mono text-slate-500">({requisition.costCenterCode})</span>
            </div>

            <div>
              <span className="text-slate-400 block text-[10px]">Legal Entity:</span>
              <span className="text-slate-900 font-medium">{requisition.legalEntityName}</span>
            </div>

            <div>
              <span className="text-slate-400 block text-[10px]">Delivery Facility:</span>
              <span className="text-slate-900 font-medium">{requisition.deliveryFacilityName}</span>
            </div>
          </div>

          <div className="pt-2 border-t border-slate-100">
            <span className="text-slate-400 block text-[10px]">Business Justification:</span>
            <p className="text-slate-800 mt-1 bg-slate-50 p-2.5 rounded border border-slate-100 italic">
              {requisition.justification || 'No justification provided.'}
            </p>
          </div>
        </div>

        {/* Line Items Table */}
        <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/70">
            <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
              {APPROVAL_COPY.drawer.sectionItems} ({requisition.lineItems?.length || 0})
            </h4>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-2.5">#</th>
                  <th className="px-4 py-2.5">Description</th>
                  <th className="px-3 py-2.5 text-right">Qty</th>
                  <th className="px-3 py-2.5">UOM</th>
                  <th className="px-4 py-2.5 text-right">Unit Price</th>
                  <th className="px-4 py-2.5 text-right">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white font-mono">
                {requisition.lineItems?.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/50">
                    <td className="px-4 py-2.5 text-slate-400 font-sans">{item.lineNumber}</td>
                    <td className="px-4 py-2.5 font-sans font-medium text-slate-900">
                      <div>{item.itemDescription}</div>
                      <span className="text-[10px] text-slate-400 uppercase font-mono">
                        {item.itemCategory}
                      </span>
                    </td>
                    <td className="px-3 py-2.5 text-right text-slate-700">{item.quantity}</td>
                    <td className="px-3 py-2.5 text-slate-500 font-sans">{item.unitOfMeasure}</td>
                    <td className="px-4 py-2.5 text-right text-slate-700">
                      <CurrencyDisplay amount={item.unitPrice} currency={requisition.currency as any} />
                    </td>
                    <td className="px-4 py-2.5 text-right font-bold text-slate-900">
                      <CurrencyDisplay amount={item.lineTotal} currency={requisition.currency as any} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Approval Timeline */}
        <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
          <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
            {APPROVAL_COPY.drawer.sectionTimeline}
          </h4>
          <ApprovalChainTimeline steps={requisition.approvalSteps} />
        </div>
      </div>
    </Drawer>
  )
}
