import { useState } from 'react'
import { Send, Printer, Edit3, AlertTriangle } from 'lucide-react'
import { Drawer } from '@/components/ui/Drawer'
import { Button } from '@/components/ui/Button'
import { StatusBadge } from '@/components/ui/Badge'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { POCancelModal } from './POCancelModal'
import { PORevisionModal } from './PORevisionModal'
import { POPrintPreviewModal } from './POPrintPreviewModal'
import { usePurchaseOrderDetail } from '../hooks/usePurchaseOrders'
import { useCreatePurchaseOrder } from '../hooks/useCreatePurchaseOrder'
import { formatDateTime } from '@/utils/date'
import { PURCHASING_COPY } from '../constants/purchasingCopy'

interface PurchaseOrderDetailDrawerProps {
  poId:    string | null
  onClose: () => void
}

export function PurchaseOrderDetailDrawer({ poId, onClose }: PurchaseOrderDetailDrawerProps) {
  const { order, isLoading, refetch } = usePurchaseOrderDetail(poId)
  const { issuePO, cancelPO, isIssuing, isCancelling } = useCreatePurchaseOrder()

  const [cancelModalOpen, setCancelModalOpen] = useState(false)
  const [revisionModalOpen, setRevisionModalOpen] = useState(false)
  const [printModalOpen, setPrintModalOpen] = useState(false)

  if (!poId) return null

  const handleIssue = async () => {
    if (!order) return
    await issuePO(order.id)
    refetch()
  }

  const handleCancelConfirm = async (reason: string) => {
    if (!order) return
    await cancelPO({ id: order.id, payload: { cancellationReason: reason } })
    setCancelModalOpen(false)
    refetch()
    onClose()
  }

  const canIssue  = order?.status === 'DRAFT'
  const canRevise = order?.status === 'ISSUED' || order?.status === 'PARTIALLY_RECEIVED'
  const canCancel = order?.status === 'DRAFT' || order?.status === 'ISSUED'

  return (
    <>
      <Drawer
        isOpen={Boolean(poId)}
        onClose={onClose}
        title={order ? `${order.poNumber} (Rev ${order.revisionNumber})` : 'Purchase Order Details'}
        subtitle={order ? `Supplier: ${order.vendorName}` : undefined}
        size="xl"
        footer={
          <div className="flex items-center justify-between w-full">
            <div>
              {canCancel && (
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => setCancelModalOpen(true)}
                  disabled={isCancelling}
                >
                  {PURCHASING_COPY.orders.cancelAction}
                </Button>
              )}
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPrintModalOpen(true)}
                leftIcon={<Printer className="w-3.5 h-3.5" />}
              >
                Print PO
              </Button>

              {canRevise && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setRevisionModalOpen(true)}
                  leftIcon={<Edit3 className="w-3.5 h-3.5" />}
                >
                  Revise PO
                </Button>
              )}

              {canIssue && (
                <Button
                  size="sm"
                  onClick={handleIssue}
                  isLoading={isIssuing}
                  leftIcon={<Send className="w-3.5 h-3.5" />}
                  className="bg-slate-900 text-white hover:bg-slate-800"
                >
                  {PURCHASING_COPY.orders.issueAction}
                </Button>
              )}

              <Button variant="outline" size="sm" onClick={onClose}>
                Close
              </Button>
            </div>
          </div>
        }
      >
        {isLoading || !order ? (
          <div className="py-8 text-center text-xs text-slate-500">Loading purchase order details...</div>
        ) : (
          <div className="space-y-6 text-xs">
            {/* Cross-Assignment Warning Alert */}
            {order.crossAssignmentWarning && (
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg flex items-start gap-2.5 text-amber-900">
                <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                <div>
                  <strong className="block font-semibold">Cross-Departmental Spend Notice</strong>
                  <p className="text-amber-800 text-[11px] mt-0.5">{order.crossAssignmentWarning.warningMessage}</p>
                </div>
              </div>
            )}

            {/* Header Status & Total */}
            <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 flex items-center justify-between">
              <div>
                <span className="text-[10px] uppercase font-semibold text-slate-500 block">Total Order Value</span>
                <CurrencyDisplay amount={order.totalAmount} currency={order.currency as any} className="text-xl font-bold text-slate-900" />
              </div>
              <div className="flex items-center gap-2">
                <StatusBadge status={order.status} />
                <span className="font-mono text-xs bg-white px-2 py-0.5 rounded border border-slate-200 text-slate-700">
                  Rev {order.revisionNumber}
                </span>
              </div>
            </div>

            {/* Vendor & Commercial Terms */}
            <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
              <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                {PURCHASING_COPY.drawer.sectionVendor}
              </h4>
              <div className="grid grid-cols-2 gap-3 text-slate-700">
                <div>
                  <span className="text-slate-400 block text-[10px]">{PURCHASING_COPY.drawer.issuedTo}</span>
                  <strong className="text-slate-900 text-sm">{order.vendorName}</strong>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">{PURCHASING_COPY.drawer.taxNumber}</span>
                  <span className="font-mono font-bold text-slate-900">{order.vendorTaxNumber}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">{PURCHASING_COPY.drawer.orderEmail}</span>
                  <span className="text-slate-800">{order.vendorOrderEmail}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">{PURCHASING_COPY.drawer.incoterms}</span>
                  <span className="font-bold text-slate-900 font-mono">{order.incoterms} • {order.paymentTerms}</span>
                </div>
              </div>
            </div>

            {/* Scope & Destination */}
            <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
              <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                {PURCHASING_COPY.drawer.sectionScope}
              </h4>
              <div className="grid grid-cols-2 gap-3 text-slate-700">
                <div>
                  <span className="text-slate-400 block text-[10px]">Legal Entity:</span>
                  <span className="text-slate-900 font-medium">{order.legalEntityName}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">Cost Center:</span>
                  <span className="text-slate-900 font-medium">{order.costCenterName}</span>
                </div>
                <div className="col-span-2">
                  <span className="text-slate-400 block text-[10px]">{PURCHASING_COPY.drawer.destination}</span>
                  <span className="text-slate-900 font-medium">{order.deliveryFacilityName}</span>
                </div>
                {order.requisitionNumber && (
                  <div>
                    <span className="text-slate-400 block text-[10px]">Requisition Ref:</span>
                    <span className="font-mono font-bold text-slate-800">{order.requisitionNumber}</span>
                  </div>
                )}
                {order.issuedAt && (
                  <div>
                    <span className="text-slate-400 block text-[10px]">Issued At:</span>
                    <span className="text-slate-800">{formatDateTime(order.issuedAt)}</span>
                  </div>
                )}
              </div>

              {order.notes && (
                <div className="pt-2 border-t border-slate-100">
                  <span className="text-slate-400 block text-[10px]">{PURCHASING_COPY.drawer.notes}</span>
                  <p className="text-slate-800 italic mt-0.5 bg-slate-50 p-2.5 rounded border border-slate-100">{order.notes}</p>
                </div>
              )}
            </div>

            {/* Line Items Table */}
            <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
              <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/70">
                <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                  {PURCHASING_COPY.drawer.sectionItems} ({order.lineItems?.length || 0})
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
                    {order.lineItems?.map((item) => (
                      <tr key={item.id} className="hover:bg-slate-50/50">
                        <td className="px-4 py-2.5 text-slate-400 font-sans">{item.lineNumber}</td>
                        <td className="px-4 py-2.5 font-sans font-medium text-slate-900">
                          <div>{item.itemDescription}</div>
                          <span className="text-[10px] text-slate-400 uppercase font-mono">{item.itemCategory}</span>
                        </td>
                        <td className="px-3 py-2.5 text-right text-slate-700">{item.quantity}</td>
                        <td className="px-3 py-2.5 text-slate-500 font-sans">{item.unitOfMeasure}</td>
                        <td className="px-4 py-2.5 text-right text-slate-700">
                          <CurrencyDisplay amount={item.unitPrice} currency={order.currency as any} />
                        </td>
                        <td className="px-4 py-2.5 text-right font-bold text-slate-900">
                          <CurrencyDisplay amount={item.lineTotal} currency={order.currency as any} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Revisions History */}
            <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
              <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                {PURCHASING_COPY.drawer.sectionRevisions}
              </h4>
              {order.revisions && order.revisions.length > 0 ? (
                <div className="space-y-2">
                  {order.revisions.map((rev) => (
                    <div key={rev.id} className="p-3 bg-slate-50 rounded border border-slate-200 text-xs">
                      <div className="flex items-center justify-between font-bold text-slate-900">
                        <span>Revision {rev.revisionNumber}</span>
                        <span className="text-[10px] text-slate-500 font-normal">{formatDateTime(rev.createdAt)}</span>
                      </div>
                      <p className="text-slate-600 mt-1 italic">"{rev.revisionReason}"</p>
                      <div className="mt-2 flex items-center justify-between text-[11px] font-mono text-slate-700 border-t border-slate-200/60 pt-1.5">
                        <span>Author: {rev.revisedByUserName}</span>
                        <span>Amount: {rev.previousAmount} → <strong>{rev.newAmount} {rev.currency}</strong></span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-slate-500 italic">{PURCHASING_COPY.drawer.noRevisions}</p>
              )}
            </div>
          </div>
        )}
      </Drawer>

      <POCancelModal
        isOpen={cancelModalOpen}
        poNumber={order?.poNumber || ''}
        isLoading={isCancelling}
        onClose={() => setCancelModalOpen(false)}
        onConfirm={handleCancelConfirm}
      />

      <PORevisionModal
        order={order ?? null}
        isOpen={revisionModalOpen}
        onClose={() => setRevisionModalOpen(false)}
        onSuccess={() => refetch()}
      />

      <POPrintPreviewModal
        order={order ?? null}
        isOpen={printModalOpen}
        onClose={() => setPrintModalOpen(false)}
      />
    </>
  )
}
