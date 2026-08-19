import { Trash2, Plus } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { MoneyInput, CurrencyDisplay } from '@/components/ui/MoneyInput'
import type { CreateLineItemRequest } from '@/types/requisition.types'
import { REQUISITION_COPY } from '../constants/requisitionCopy'

interface LineItemsEditorTableProps {
  items:        CreateLineItemRequest[]
  currency:     string
  onChange:     (items: CreateLineItemRequest[]) => void
}

export function LineItemsEditorTable({
  items,
  currency,
  onChange,
}: LineItemsEditorTableProps) {
  const handleAddItem = () => {
    const newItem: CreateLineItemRequest = {
      itemDescription: '',
      itemCategory:    'IT_HARDWARE',
      quantity:        1,
      unitOfMeasure:   'PIECE',
      unitPrice:       0,
    }
    onChange([...items, newItem])
  }

  const handleRemoveItem = (index: number) => {
    if (items.length <= 1) return
    const updated = items.filter((_, i) => i !== index)
    onChange(updated)
  }

  const handleUpdateItem = (index: number, patch: Partial<CreateLineItemRequest>) => {
    const updated = items.map((item, i) => (i === index ? { ...item, ...patch } : item))
    onChange(updated)
  }

  const totalAmount = items.reduce(
    (acc, item) => acc + (item.quantity || 0) * (item.unitPrice || 0),
    0
  )

  return (
    <div className="bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
      <div className="p-4 border-b border-slate-200 bg-white flex items-center justify-between">
        <div>
          <h3 className="text-sm font-bold text-slate-900">
            {REQUISITION_COPY.create.sectionLineItems}
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Add at least one line item with unit price and quantity.
          </p>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleAddItem}
          leftIcon={<Plus className="w-3.5 h-3.5" />}
        >
          {REQUISITION_COPY.create.addLineItemCTA}
        </Button>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
            <tr>
              <th className="px-4 py-3 min-w-[220px]">{REQUISITION_COPY.create.colItemDesc}</th>
              <th className="px-3 py-3 min-w-[170px]">{REQUISITION_COPY.create.colCategory}</th>
              <th className="px-3 py-3 w-24 text-right">{REQUISITION_COPY.create.colQty}</th>
              <th className="px-3 py-3 w-32">{REQUISITION_COPY.create.colUOM}</th>
              <th className="px-3 py-3 w-40 text-right">{REQUISITION_COPY.create.colUnitPrice}</th>
              <th className="px-4 py-3 w-40 text-right">{REQUISITION_COPY.create.colLineTotal}</th>
              <th className="px-3 py-3 w-12 text-center"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {items.map((item, index) => {
              const lineTotal = (item.quantity || 0) * (item.unitPrice || 0)

              return (
                <tr key={index} className="hover:bg-slate-50/40">
                  {/* Description */}
                  <td className="px-4 py-2.5">
                    <Input
                      value={item.itemDescription}
                      onChange={(e) =>
                        handleUpdateItem(index, { itemDescription: e.target.value })
                      }
                      placeholder="Item description or service..."
                      required
                    />
                  </td>

                  {/* Category */}
                  <td className="px-3 py-2.5">
                    <Select
                      value={item.itemCategory}
                      onChange={(e) =>
                        handleUpdateItem(index, { itemCategory: e.target.value })
                      }
                      options={[...REQUISITION_COPY.categoryOptions]}
                    />
                  </td>

                  {/* Quantity */}
                  <td className="px-3 py-2.5 text-right">
                    <input
                      type="number"
                      min="0.01"
                      step="1"
                      value={item.quantity}
                      onChange={(e) =>
                        handleUpdateItem(index, {
                          quantity: parseFloat(e.target.value) || 0,
                        })
                      }
                      className="w-full text-right py-2 px-2.5 text-sm font-mono bg-slate-50 border border-slate-200 rounded-lg text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900"
                    />
                  </td>

                  {/* UOM */}
                  <td className="px-3 py-2.5">
                    <Select
                      value={item.unitOfMeasure}
                      onChange={(e) =>
                        handleUpdateItem(index, { unitOfMeasure: e.target.value })
                      }
                      options={[...REQUISITION_COPY.uomOptions]}
                    />
                  </td>

                  {/* Unit Price */}
                  <td className="px-3 py-2.5 text-right">
                    <MoneyInput
                      value={item.unitPrice}
                      currency={currency as any}
                      onChange={(unitPrice) => handleUpdateItem(index, { unitPrice })}
                    />
                  </td>

                  {/* Line Total */}
                  <td className="px-4 py-2.5 text-right font-mono font-bold text-slate-900 text-sm">
                    <CurrencyDisplay amount={lineTotal} currency={currency as any} />
                  </td>

                  {/* Remove Action */}
                  <td className="px-3 py-2.5 text-center">
                    <button
                      type="button"
                      disabled={items.length <= 1}
                      onClick={() => handleRemoveItem(index)}
                      className="p-1.5 text-slate-400 hover:text-red-600 rounded disabled:opacity-30 disabled:pointer-events-none transition-colors"
                      aria-label="Remove line item"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* Footer with Overall Total */}
      <div className="p-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between">
        <span className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
          {REQUISITION_COPY.create.totalLabel}
        </span>
        <div className="text-right">
          <CurrencyDisplay
            amount={totalAmount}
            currency={currency as any}
            className="text-lg font-bold text-slate-900"
          />
        </div>
      </div>
    </div>
  )
}
