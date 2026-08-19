import { Printer } from 'lucide-react'
import { Drawer } from '@/components/ui/Drawer'
import { Button } from '@/components/ui/Button'
import { GoodsReceiptStatusBadge } from './GoodsReceiptStatusBadge'
import { useGoodsReceiptDetail } from '../hooks/useGoodsReceiptDetail'
import { formatDateTime, formatDate } from '@/utils/date'
import { RECEIVING_COPY } from '../constants/receivingCopy'

interface GoodsReceiptDetailDrawerProps {
  receiptId: string | null
  onClose:   () => void
}

export function GoodsReceiptDetailDrawer({ receiptId, onClose }: GoodsReceiptDetailDrawerProps) {
  const { receipt, isLoading } = useGoodsReceiptDetail(receiptId)

  if (!receiptId) return null

  const handlePrint = () => {
    window.print()
  }

  const totalAccepted = receipt?.lineItems.reduce((acc, i) => acc + i.acceptedQuantity, 0) || 0
  const totalRejected = receipt?.lineItems.reduce((acc, i) => acc + i.rejectedQuantity, 0) || 0

  return (
    <Drawer
      isOpen={Boolean(receiptId)}
      onClose={onClose}
      title={receipt ? `${receipt.receiptNumber}` : 'Goods Receipt Details'}
      subtitle={receipt ? `PO Reference: ${receipt.poNumber} • Waybill: ${receipt.waybillNumber}` : undefined}
      size="xl"
      footer={
        <div className="flex items-center justify-between w-full">
          <Button variant="outline" size="sm" onClick={onClose}>
            Close
          </Button>
          <Button
            size="sm"
            onClick={handlePrint}
            leftIcon={<Printer className="w-3.5 h-3.5" />}
            className="bg-slate-900 text-white"
          >
            Print Inspection Report
          </Button>
        </div>
      }
    >
      {isLoading || !receipt ? (
        <div className="py-8 text-center text-xs text-slate-500">Loading goods receipt report...</div>
      ) : (
        <div className="space-y-6 text-xs text-slate-700">
          {/* Header Status & Summary */}
          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <GoodsReceiptStatusBadge status={receipt.status} />
              <span className="text-[11px] text-slate-500 font-mono">
                Inspect Date: {formatDateTime(receipt.createdAt)}
              </span>
            </div>

            <div className="flex items-center gap-4 text-right">
              <div>
                <span className="text-[10px] uppercase font-semibold text-slate-500 block">Accepted Units</span>
                <span className="text-sm font-bold text-emerald-700 font-mono">{totalAccepted.toFixed(2)}</span>
              </div>
              {totalRejected > 0 && (
                <div>
                  <span className="text-[10px] uppercase font-semibold text-slate-500 block">Rejected / Damage</span>
                  <span className="text-sm font-bold text-red-700 font-mono">{totalRejected.toFixed(2)}</span>
                </div>
              )}
            </div>
          </div>

          {/* Waybill & Delivery Metadata */}
          <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
            <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
              Shipment & Waybill Documentation
            </h4>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <span className="text-slate-400 block text-[10px]">Supplier / Vendor:</span>
                <strong className="text-slate-900 text-sm">{receipt.vendorName}</strong>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px]">Waybill # (İrsaliye No):</span>
                <span className="font-mono font-bold text-slate-900 text-sm">{receipt.waybillNumber}</span>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px]">Waybill Dispatch Date:</span>
                <span className="text-slate-800 font-medium">{formatDate(receipt.waybillDate)}</span>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px]">Delivery Dock / Facility:</span>
                <span className="text-slate-900 font-medium">{receipt.deliveryFacilityName}</span>
              </div>
              <div className="col-span-2 pt-2 border-t border-slate-100">
                <span className="text-slate-400 block text-[10px]">Inspecting Dock Officer:</span>
                <span className="text-slate-900 font-semibold">{receipt.receivedByUserName}</span>
              </div>
            </div>

            {receipt.notes && (
              <div className="pt-2 border-t border-slate-100">
                <span className="text-slate-400 block text-[10px]">Dock Inspection Remarks:</span>
                <p className="text-slate-800 italic mt-0.5 bg-slate-50 p-2.5 rounded border border-slate-100">{receipt.notes}</p>
              </div>
            )}
          </div>

          {/* Itemized Inspection Table */}
          <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
            <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/70">
              <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                {RECEIVING_COPY.drawer.sectionItems} ({receipt.lineItems.length})
              </h4>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-2.5">Item Description</th>
                    <th className="px-3 py-2.5 text-right">Received</th>
                    <th className="px-3 py-2.5 text-right text-emerald-700">Accepted</th>
                    <th className="px-3 py-2.5 text-right text-red-600">Rejected</th>
                    <th className="px-4 py-2.5">Reason / Remarks</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white font-mono">
                  {receipt.lineItems.map((item) => (
                    <tr key={item.id} className="hover:bg-slate-50/50">
                      <td className="px-4 py-2.5 font-sans font-medium text-slate-900">
                        {item.itemDescription}
                      </td>
                      <td className="px-3 py-2.5 text-right text-slate-700">{item.receivedQuantity}</td>
                      <td className="px-3 py-2.5 text-right font-bold text-emerald-700">{item.acceptedQuantity}</td>
                      <td className="px-3 py-2.5 text-right font-bold text-red-600">{item.rejectedQuantity}</td>
                      <td className="px-4 py-2.5 font-sans text-[11px]">
                        {item.rejectionReason ? (
                          <span className="text-red-700 font-medium">⚠️ {item.rejectionReason}</span>
                        ) : item.notes ? (
                          <span className="text-slate-600">{item.notes}</span>
                        ) : (
                          <span className="text-slate-400 italic">No remarks</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </Drawer>
  )
}
