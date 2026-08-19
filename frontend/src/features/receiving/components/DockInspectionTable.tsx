import type { CreateGRLineItemRequest } from '@/types/receiving.types'
import { RECEIVING_COPY } from '../constants/receivingCopy'

export interface InspectionLineState extends CreateGRLineItemRequest {
  itemDescription:          string
  itemCategory:             string
  orderedQuantity:          number
  unitOfMeasure:            string
  overDeliveryTolerancePct: number
}

interface DockInspectionTableProps {
  lines:    InspectionLineState[]
  onChange: (lines: InspectionLineState[]) => void
}

export function DockInspectionTable({ lines, onChange }: DockInspectionTableProps) {
  const handleUpdate = (idx: number, patch: Partial<InspectionLineState>) => {
    const updated = lines.map((item, i) => {
      if (i !== idx) return item

      const merged = { ...item, ...patch }

      // If received changed and accepted wasn't explicitly patched, sync accepted = received - rejected
      if ('receivedQuantity' in patch && !('acceptedQuantity' in patch)) {
        merged.acceptedQuantity = Math.max(0, (patch.receivedQuantity ?? 0) - (merged.rejectedQuantity ?? 0))
      }

      // If accepted changed and received wasn't explicitly patched, sync received = accepted + rejected
      if ('acceptedQuantity' in patch) {
        merged.receivedQuantity = (patch.acceptedQuantity ?? 0) + (merged.rejectedQuantity ?? 0)
      }

      // If rejected changed, adjust accepted
      if ('rejectedQuantity' in patch) {
        merged.acceptedQuantity = Math.max(0, merged.receivedQuantity - (patch.rejectedQuantity ?? 0))
      }

      return merged
    })
    onChange(updated)
  }

  return (
    <div className="border border-slate-200 rounded-lg overflow-hidden bg-white shadow-2xs">
      <div className="bg-slate-50 px-4 py-3 border-b border-slate-200 flex items-center justify-between">
        <h4 className="font-bold text-slate-900 text-xs uppercase tracking-wider">
          {RECEIVING_COPY.create.inspectionSection} ({lines.length} Line Items)
        </h4>
        <span className="text-[11px] text-slate-500 font-mono">
          ISO 9001 Receiving Inspection Protocol
        </span>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-100/70 border-b border-slate-200 text-slate-600 font-semibold uppercase text-[10px]">
            <tr>
              <th className="px-3 py-2.5 w-10">#</th>
              <th className="px-3 py-2.5">Item Description</th>
              <th className="px-2 py-2.5 w-24 text-right">Ordered</th>
              <th className="px-2 py-2.5 w-24 text-right">Received</th>
              <th className="px-2 py-2.5 w-24 text-right">Accepted</th>
              <th className="px-2 py-2.5 w-24 text-right text-red-600">Damaged / Red</th>
              <th className="px-3 py-2.5 w-56">Rejection Reason</th>
              <th className="px-3 py-2.5 w-44">Dock Remarks</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200">
            {lines.map((line, idx) => {
              const maxAllowed = line.orderedQuantity * (1 + (line.overDeliveryTolerancePct || 0) / 100)
              const isOverTolerance = line.acceptedQuantity > maxAllowed
              const hasRejection = (line.rejectedQuantity || 0) > 0
              const isMissingReason = hasRejection && (!line.rejectionReason || line.rejectionReason.trim().length === 0)

              return (
                <tr key={line.purchaseOrderLineItemId} className={`hover:bg-slate-50/50 ${hasRejection ? 'bg-amber-50/20' : ''}`}>
                  <td className="px-3 py-3 text-slate-400 font-sans">{idx + 1}</td>
                  <td className="px-3 py-3">
                    <div className="font-semibold text-slate-900">{line.itemDescription}</div>
                    <div className="text-[10px] text-slate-400 font-mono mt-0.5">
                      {line.itemCategory} • {line.unitOfMeasure} • Tol: +{line.overDeliveryTolerancePct}% (Max: {maxAllowed.toFixed(2)})
                    </div>
                  </td>
                  <td className="px-2 py-3 text-right font-mono font-bold text-slate-700">
                    {line.orderedQuantity}
                  </td>
                  <td className="px-2 py-3 text-right">
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={line.receivedQuantity}
                      onChange={(e) =>
                        handleUpdate(idx, { receivedQuantity: parseFloat(e.target.value) || 0 })
                      }
                      className="w-20 text-right py-1 px-2 text-xs font-mono font-bold bg-slate-50 border border-slate-200 rounded text-slate-900 focus:bg-white focus:outline-none focus:ring-1 focus:ring-slate-900"
                    />
                  </td>
                  <td className="px-2 py-3 text-right">
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={line.acceptedQuantity}
                      onChange={(e) =>
                        handleUpdate(idx, { acceptedQuantity: parseFloat(e.target.value) || 0 })
                      }
                      className={`w-20 text-right py-1 px-2 text-xs font-mono font-bold border rounded focus:outline-none focus:ring-1 ${
                        isOverTolerance
                          ? 'bg-red-50 border-red-400 text-red-700 ring-1 ring-red-400'
                          : 'bg-emerald-50/50 border-emerald-300 text-emerald-900 focus:ring-emerald-600'
                      }`}
                    />
                    {isOverTolerance && (
                      <span className="block text-[9px] text-red-600 font-sans font-medium mt-0.5">
                        Exceeds +{line.overDeliveryTolerancePct}% tolerance!
                      </span>
                    )}
                  </td>
                  <td className="px-2 py-3 text-right">
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={line.rejectedQuantity || 0}
                      onChange={(e) =>
                        handleUpdate(idx, { rejectedQuantity: parseFloat(e.target.value) || 0 })
                      }
                      className={`w-20 text-right py-1 px-2 text-xs font-mono font-bold border rounded focus:outline-none focus:ring-1 ${
                        hasRejection
                          ? 'bg-red-50 border-red-300 text-red-700 focus:ring-red-600'
                          : 'bg-slate-50 border-slate-200 text-slate-700'
                      }`}
                    />
                  </td>
                  <td className="px-3 py-3">
                    <input
                      type="text"
                      placeholder={hasRejection ? 'Mandatory damage / rejection reason...' : 'Reason (if rejected)...'}
                      value={line.rejectionReason || ''}
                      onChange={(e) => handleUpdate(idx, { rejectionReason: e.target.value })}
                      disabled={!hasRejection}
                      className={`w-full py-1 px-2 text-xs rounded border transition-colors ${
                        isMissingReason
                          ? 'bg-red-50 border-red-300 text-red-900 placeholder-red-400 focus:ring-1 focus:ring-red-500'
                          : hasRejection
                          ? 'bg-white border-slate-300 text-slate-900'
                          : 'bg-slate-100 border-slate-200 text-slate-400 cursor-not-allowed'
                      }`}
                    />
                  </td>
                  <td className="px-3 py-3">
                    <input
                      type="text"
                      placeholder="Optional notes..."
                      value={line.notes || ''}
                      onChange={(e) => handleUpdate(idx, { notes: e.target.value })}
                      className="w-full py-1 px-2 text-xs bg-slate-50 border border-slate-200 rounded text-slate-900 focus:bg-white focus:outline-none focus:ring-1 focus:ring-slate-900"
                    />
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
