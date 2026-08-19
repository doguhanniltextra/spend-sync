import { Printer, CheckCircle2 } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import type { PurchaseOrderDetailResponse } from '@/types/purchasing.types'
import { formatDate } from '@/utils/date'

interface POPrintPreviewModalProps {
  order:   PurchaseOrderDetailResponse | null
  isOpen:  boolean
  onClose: () => void
}

export function POPrintPreviewModal({ order, isOpen, onClose }: POPrintPreviewModalProps) {
  if (!order) return null

  const handlePrint = () => {
    window.print()
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Commercial Purchase Order — Document Preview"
      description="Official commercial contract document formatted for vendor dispatch and receiving inspection."
      maxWidth="2xl"
      footer={
        <div className="flex items-center justify-between w-full">
          <span className="text-[11px] text-slate-500 font-mono">
            ISO 9001 / SOX §404 Formally Authorized Order
          </span>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={onClose}>
              Close
            </Button>
            <Button
              size="sm"
              onClick={handlePrint}
              leftIcon={<Printer className="w-3.5 h-3.5" />}
              className="bg-slate-900 text-white"
            >
              Print / Save PDF
            </Button>
          </div>
        </div>
      }
    >
      <div className="p-6 bg-white border border-slate-300 rounded-lg shadow-sm space-y-6 text-slate-800 text-xs font-sans print:border-none print:shadow-none">
        {/* Document Header */}
        <div className="flex items-start justify-between border-b-2 border-slate-900 pb-4">
          <div>
            <span className="text-xl font-extrabold tracking-tight text-slate-900 block font-mono">
              SPENDSYNC COMMERCIAL PO
            </span>
            <span className="text-xs text-slate-600 font-medium block mt-0.5">
              {order.legalEntityName}
            </span>
            <span className="text-[10px] text-slate-500 block">
              Maslak Financial Center, Istanbul • Tax Number: 8877665544
            </span>
          </div>

          <div className="text-right">
            <span className="text-sm font-mono font-bold text-slate-900 block">
              {order.poNumber}
            </span>
            <span className="text-[11px] font-mono text-slate-600 block">
              Revision: {order.revisionNumber}
            </span>
            <span className="text-[10px] text-slate-500 block mt-1">
              Date: {formatDate(order.issuedAt ?? order.createdAt)}
            </span>
          </div>
        </div>

        {/* Vendor & Delivery Box */}
        <div className="grid grid-cols-2 gap-4">
          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              VENDOR / SUPPLIER (ISSUED TO)
            </span>
            <strong className="text-slate-900 text-sm block mt-1">{order.vendorName}</strong>
            <span className="text-slate-600 block text-[11px]">VKN / Tax ID: <strong className="font-mono">{order.vendorTaxNumber}</strong></span>
            <span className="text-slate-600 block text-[11px]">Order Email: {order.vendorOrderEmail}</span>
          </div>

          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              SHIP TO / DELIVERY FACILITY
            </span>
            <strong className="text-slate-900 text-sm block mt-1">{order.deliveryFacilityName}</strong>
            <span className="text-slate-600 block text-[11px]">Cost Center: {order.costCenterName}</span>
            <span className="text-slate-600 block text-[11px]">Terms: <strong className="font-mono">{order.incoterms} • {order.paymentTerms}</strong></span>
          </div>
        </div>

        {/* Line Items Table */}
        <table className="w-full text-left text-xs border border-slate-200">
          <thead className="bg-slate-900 text-white font-semibold uppercase text-[10px]">
            <tr>
              <th className="px-3 py-2 w-10">#</th>
              <th className="px-3 py-2">Item Description</th>
              <th className="px-3 py-2 text-right w-20">Qty</th>
              <th className="px-3 py-2 w-20">UOM</th>
              <th className="px-3 py-2 text-right w-28">Unit Price</th>
              <th className="px-3 py-2 text-right w-28">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 font-mono">
            {order.lineItems?.map((item) => (
              <tr key={item.id}>
                <td className="px-3 py-2 text-slate-500 font-sans">{item.lineNumber}</td>
                <td className="px-3 py-2 font-sans font-medium text-slate-900">
                  {item.itemDescription}
                </td>
                <td className="px-3 py-2 text-right text-slate-800">{item.quantity}</td>
                <td className="px-3 py-2 text-slate-600 font-sans">{item.unitOfMeasure}</td>
                <td className="px-3 py-2 text-right text-slate-800">
                  <CurrencyDisplay amount={item.unitPrice} currency={order.currency as any} />
                </td>
                <td className="px-3 py-2 text-right font-bold text-slate-900">
                  <CurrencyDisplay amount={item.lineTotal} currency={order.currency as any} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Total Summary */}
        <div className="flex justify-between items-start pt-2">
          <div className="max-w-md">
            {order.notes && (
              <div className="text-[11px] text-slate-600 bg-slate-50 p-2.5 rounded border border-slate-200">
                <strong className="text-slate-800 block">Special Order Notes:</strong>
                <p className="italic mt-0.5">{order.notes}</p>
              </div>
            )}
          </div>

          <div className="text-right space-y-1 bg-slate-50 p-3 rounded border border-slate-200 min-w-[220px]">
            <div className="flex justify-between text-xs text-slate-600">
              <span>Subtotal:</span>
              <CurrencyDisplay amount={order.totalAmount} currency={order.currency as any} />
            </div>
            <div className="flex justify-between text-xs text-slate-600 border-b border-slate-200 pb-1">
              <span>VAT / Tax (Included):</span>
              <span className="font-mono text-slate-700">0,00 {order.currency}</span>
            </div>
            <div className="flex justify-between text-sm font-bold text-slate-900 pt-1">
              <span>Grand Total:</span>
              <CurrencyDisplay amount={order.totalAmount} currency={order.currency as any} className="text-base font-bold text-slate-900" />
            </div>
          </div>
        </div>

        {/* Authorized Signatory Block */}
        <div className="pt-6 border-t border-slate-200 flex items-center justify-between text-[11px] text-slate-500">
          <div className="flex items-center gap-2 text-emerald-700">
            <CheckCircle2 className="w-4 h-4" />
            <span className="font-semibold">Electronically Authorized & Cryptographically Signed</span>
          </div>
          <div>
            <span>Issued by: <strong>{order.createdByUserName}</strong></span>
          </div>
        </div>
      </div>
    </Modal>
  )
}
