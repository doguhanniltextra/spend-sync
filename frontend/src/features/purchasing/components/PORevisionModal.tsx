import { useState, useMemo } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Textarea } from '@/components/ui/Textarea'
import { MoneyInput, CurrencyDisplay } from '@/components/ui/MoneyInput'
import type { PurchaseOrderDetailResponse, POLineItemResponse } from '@/types/purchasing.types'
import { useCreatePurchaseOrder } from '../hooks/useCreatePurchaseOrder'

interface PORevisionModalProps {
  order:     PurchaseOrderDetailResponse | null
  isOpen:    boolean
  onClose:   () => void
  onSuccess: () => void
}

interface EditableRevisionItem {
  lineItemId:      string
  itemDescription: string
  itemCategory:    string
  quantity:        number
  unitOfMeasure:   string
  unitPrice:       number
}

export function PORevisionModal({ order, isOpen, onClose, onSuccess }: PORevisionModalProps) {
  const { revisePO, isRevising } = useCreatePurchaseOrder()
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)

  const [items, setItems] = useState<EditableRevisionItem[]>(() => {
    if (!order?.lineItems) return []
    return order.lineItems.map((item: POLineItemResponse) => ({
      lineItemId:      item.id,
      itemDescription: item.itemDescription,
      itemCategory:    item.itemCategory,
      quantity:        item.quantity,
      unitOfMeasure:   item.unitOfMeasure,
      unitPrice:       item.unitPrice,
    }))
  })

  // Re-sync items when order changes
  useMemo(() => {
    if (order?.lineItems) {
      setItems(
        order.lineItems.map((item: POLineItemResponse) => ({
          lineItemId:      item.id,
          itemDescription: item.itemDescription,
          itemCategory:    item.itemCategory,
          quantity:        item.quantity,
          unitOfMeasure:   item.unitOfMeasure,
          unitPrice:       item.unitPrice,
        }))
      )
    }
  }, [order])

  const previousTotal = order?.totalAmount || 0

  const newTotal = useMemo(() => {
    return items.reduce((acc, item) => acc + (item.quantity || 0) * (item.unitPrice || 0), 0)
  }, [items])

  const differential = newTotal - previousTotal

  const handleUpdateItem = (idx: number, patch: Partial<EditableRevisionItem>) => {
    setItems((prev) => prev.map((item, i) => (i === idx ? { ...item, ...patch } : item)))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!order) return
    if (reason.trim().length < 5) {
      setError('Revision reason is mandatory and must be at least 5 characters (SOX §404 requirement).')
      return
    }

    try {
      await revisePO({
        id: order.id,
        payload: {
          revisionReason: reason.trim(),
          lineItems: items.map((item) => ({
            lineItemId:      item.lineItemId,
            itemDescription: item.itemDescription,
            itemCategory:    item.itemCategory,
            quantity:        item.quantity,
            unitOfMeasure:   item.unitOfMeasure,
            unitPrice:       item.unitPrice,
          })),
        },
      })
      onSuccess()
      onClose()
    } catch {
      // Handled in mutation hook
    }
  }

  if (!order) return null

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Create PO Revision — ${order.poNumber} (Rev ${order.revisionNumber} → ${order.revisionNumber + 1})`}
      description="Update quantities or unit prices. The system will automatically compute the budget differential and adjust reservations."
      maxWidth="2xl"
      footer={
        <>
          <Button type="button" variant="outline" size="sm" onClick={onClose} disabled={isRevising}>
            Cancel
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={handleSubmit}
            isLoading={isRevising}
          >
            Submit Revision (Rev {order.revisionNumber + 1})
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4 text-xs">
        {/* Revision Header Summary */}
        <div className="grid grid-cols-3 gap-3 p-3 bg-slate-50 rounded-lg border border-slate-200">
          <div>
            <span className="text-[10px] text-slate-500 font-semibold block uppercase">Current Amount</span>
            <CurrencyDisplay amount={previousTotal} currency={order.currency as any} className="text-sm font-bold text-slate-700" />
          </div>
          <div>
            <span className="text-[10px] text-slate-500 font-semibold block uppercase">Revised Total</span>
            <CurrencyDisplay amount={newTotal} currency={order.currency as any} className="text-sm font-bold text-slate-900" />
          </div>
          <div>
            <span className="text-[10px] text-slate-500 font-semibold block uppercase">Budget Impact</span>
            <span
              className={`text-sm font-bold font-mono ${
                differential > 0
                  ? 'text-red-700'
                  : differential < 0
                  ? 'text-emerald-700'
                  : 'text-slate-500'
              }`}
            >
              {differential > 0 ? `+${differential.toLocaleString()} ${order.currency} (Reserve)` : differential < 0 ? `${differential.toLocaleString()} ${order.currency} (Release)` : 'No Change'}
            </span>
          </div>
        </div>

        {/* Editable Line Items */}
        <div className="border border-slate-200 rounded-lg overflow-hidden">
          <div className="bg-slate-100/70 px-3 py-2 border-b border-slate-200">
            <span className="font-bold text-slate-900 text-[11px] uppercase tracking-wider">
              Adjust Order Quantities & Pricing
            </span>
          </div>
          <div className="max-h-60 overflow-y-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase">
                <tr>
                  <th className="px-3 py-2">Item Description</th>
                  <th className="px-2 py-2 w-24 text-right">Qty</th>
                  <th className="px-3 py-2 w-32 text-right">Unit Price</th>
                  <th className="px-3 py-2 w-32 text-right">Line Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {items.map((item, idx) => {
                  const lineTotal = (item.quantity || 0) * (item.unitPrice || 0)
                  return (
                    <tr key={item.lineItemId} className="hover:bg-slate-50/40">
                      <td className="px-3 py-2 font-medium text-slate-900">
                        {item.itemDescription}
                      </td>
                      <td className="px-2 py-2 text-right">
                        <input
                          type="number"
                          min="0.01"
                          step="1"
                          value={item.quantity}
                          onChange={(e) =>
                            handleUpdateItem(idx, { quantity: parseFloat(e.target.value) || 0 })
                          }
                          className="w-full text-right py-1 px-2 text-xs font-mono bg-slate-50 border border-slate-200 rounded text-slate-900 focus:bg-white focus:outline-none focus:ring-1 focus:ring-slate-900"
                        />
                      </td>
                      <td className="px-3 py-2 text-right">
                        <MoneyInput
                          value={item.unitPrice}
                          currency={order.currency as any}
                          onChange={(unitPrice) => handleUpdateItem(idx, { unitPrice })}
                        />
                      </td>
                      <td className="px-3 py-2 text-right font-mono font-bold text-slate-900">
                        <CurrencyDisplay amount={lineTotal} currency={order.currency as any} />
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Mandatory Reason */}
        <div>
          <Textarea
            label="Revision Reason / Business Justification (Mandatory)"
            placeholder="Explain why this PO is being revised (e.g. price renegotiation, volume adjustment, scope changes)..."
            value={reason}
            onChange={(e) => {
              setReason(e.target.value)
              if (error) setError(null)
            }}
            error={error ?? undefined}
            rows={3}
            required
          />
        </div>
      </form>
    </Modal>
  )
}
