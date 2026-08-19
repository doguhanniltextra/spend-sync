import { useState } from 'react'
import { Drawer } from '@/components/ui/Drawer'
import { Button } from '@/components/ui/Button'
import { StatusBadge } from '@/components/ui/Badge'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { ApprovalChainTimeline } from './ApprovalChainTimeline'
import { useRequisitionDetail } from '../hooks/useRequisitionDetail'
import { useCreateRequisition } from '../hooks/useCreateRequisition'
import { REQUISITION_COPY } from '../constants/requisitionCopy'

interface RequisitionDetailDrawerProps {
  requisitionId: string | null
  onClose:       () => void
}

export function RequisitionDetailDrawer({
  requisitionId,
  onClose,
}: RequisitionDetailDrawerProps) {
  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false)
  const { requisition, isLoading } = useRequisitionDetail(requisitionId)
  const { cancelRequisition, isCancelling } = useCreateRequisition()

  const handleCancelConfirm = async () => {
    if (!requisitionId) return
    await cancelRequisition(requisitionId)
    setConfirmCancelOpen(false)
    onClose()
  }

  const canCancel =
    requisition?.status === 'DRAFT' || requisition?.status === 'PENDING_APPROVAL'

  return (
    <>
      <Drawer
        isOpen={Boolean(requisitionId)}
        onClose={onClose}
        title={requisition?.requisitionNumber || 'Requisition Details'}
        subtitle={requisition?.title}
        size="xl"
        footer={
          <div className="flex items-center justify-between w-full">
            {canCancel ? (
              <Button
                variant="danger"
                size="sm"
                onClick={() => setConfirmCancelOpen(true)}
              >
                {REQUISITION_COPY.drawer.cancelAction}
              </Button>
            ) : (
              <div />
            )}

            <Button variant="outline" size="sm" onClick={onClose}>
              {REQUISITION_COPY.drawer.closeDrawer}
            </Button>
          </div>
        }
      >
        {isLoading || !requisition ? (
          <div className="space-y-4 py-8 text-center text-xs text-slate-500">
            Loading requisition details...
          </div>
        ) : (
          <div className="space-y-6 text-xs">
            {/* Header Status Bar */}
            <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 flex items-center justify-between">
              <div>
                <span className="text-[10px] uppercase font-semibold text-slate-500 block">
                  Total Amount
                </span>
                <CurrencyDisplay
                  amount={requisition.totalAmount}
                  currency={requisition.currency as any}
                  className="text-lg font-bold text-slate-900"
                />
              </div>
              <StatusBadge status={requisition.status} />
            </div>

            {/* Scope / Metadata Section */}
            <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
              <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                {REQUISITION_COPY.drawer.sectionScope}
              </h4>

              <div className="grid grid-cols-2 gap-3 text-slate-700">
                <div>
                  <span className="text-slate-400 block text-[10px]">
                    {REQUISITION_COPY.drawer.requesterLabel}
                  </span>
                  <strong className="text-slate-900">{requisition.requisitionerName}</strong>{' '}
                  <span className="text-slate-500">({requisition.requisitionerEmail})</span>
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px]">
                    {REQUISITION_COPY.drawer.costCenterLabel}
                  </span>
                  <strong className="text-slate-900">{requisition.costCenterName}</strong>{' '}
                  <span className="font-mono text-slate-500">({requisition.costCenterCode})</span>
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px]">
                    {REQUISITION_COPY.drawer.entityLabel}
                  </span>
                  <span className="text-slate-900 font-medium">{requisition.legalEntityName}</span>
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px]">
                    {REQUISITION_COPY.drawer.facilityLabel}
                  </span>
                  <span className="text-slate-900 font-medium">{requisition.deliveryFacilityName}</span>
                </div>
              </div>

              {/* Justification */}
              <div className="pt-2 border-t border-slate-100">
                <span className="text-slate-400 block text-[10px]">
                  {REQUISITION_COPY.drawer.justificationLabel}
                </span>
                <p className="text-slate-800 mt-1 bg-slate-50 p-2.5 rounded border border-slate-100 italic">
                  {requisition.justification}
                </p>
              </div>

              {/* Rejection Reason if any */}
              {requisition.rejectionReason && (
                <div className="pt-2 border-t border-red-100 bg-red-50/60 p-2.5 rounded">
                  <span className="text-red-700 font-bold block text-[10px]">
                    {REQUISITION_COPY.drawer.rejectionLabel}
                  </span>
                  <p className="text-red-800 mt-0.5">{requisition.rejectionReason}</p>
                </div>
              )}
            </div>

            {/* Line Items Table */}
            <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
              <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/70">
                <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                  {REQUISITION_COPY.drawer.sectionItems} ({requisition.lineItems.length})
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
                    {requisition.lineItems.map((item) => (
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

            {/* Approval Stepper Timeline */}
            <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
              <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                {REQUISITION_COPY.drawer.sectionApproval}
              </h4>
              <ApprovalChainTimeline steps={requisition.approvalSteps} />
            </div>
          </div>
        )}
      </Drawer>

      {/* Confirm Cancel Dialog */}
      <ConfirmDialog
        isOpen={confirmCancelOpen}
        onClose={() => setConfirmCancelOpen(false)}
        onConfirm={handleCancelConfirm}
        title={REQUISITION_COPY.list.confirmCancelTitle}
        message={REQUISITION_COPY.list.confirmCancelDesc}
        confirmLabel={REQUISITION_COPY.list.cancelPR}
        isDestructive
        isLoading={isCancelling}
      />
    </>
  )
}
