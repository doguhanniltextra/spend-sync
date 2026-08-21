import { useState } from 'react'
import { CheckCircle2, Calendar, XCircle, AlertCircle } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { useAcknowledgePo } from '../../hooks/useVendorPortalQueries'
import type { VendorOrderResponse } from '../../types/vendorPortal.types'

interface Props {
  order: VendorOrderResponse
  isOpen: boolean
  onClose: () => void
}

export function VendorPoAcknowledgeModal({ order, isOpen, onClose }: Props) {
  const acknowledgeMutation = useAcknowledgePo(order.id)

  const [decision, setDecision] = useState<'ACCEPTED' | 'REVISED_DATE_PROPOSED' | 'REJECTED'>('ACCEPTED')
  const [promisedDate, setPromisedDate] = useState(
    new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  )
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  const handleConfirm = async () => {
    try {
      setError(null)
      await acknowledgeMutation.mutateAsync({
        status: decision,
        promisedDeliveryDate: decision !== 'REJECTED' ? promisedDate : undefined,
        vendorNotes: notes.trim() || undefined,
      })
      onClose()
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to acknowledge order.')
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Purchase Order Confirmation: ${order.poNumber}`}>
      <div className="space-y-6">
        <p className="text-sm text-slate-600">
          Confirm receipt and delivery schedule for <strong className="text-slate-800">{order.poNumber}</strong> (Total: {order.totalAmount.toLocaleString()} {order.currency}).
        </p>

        {error && (
          <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4" />
            <span>{error}</span>
          </div>
        )}

        {/* Decision Selector */}
        <div className="grid grid-cols-3 gap-3">
          <button
            type="button"
            onClick={() => setDecision('ACCEPTED')}
            className={`p-3 rounded-xl border text-left flex flex-col justify-between gap-2 transition ${
              decision === 'ACCEPTED'
                ? 'border-emerald-500 bg-emerald-50 text-emerald-900 ring-2 ring-emerald-500/20'
                : 'border-slate-200 hover:border-slate-300 text-slate-600'
            }`}
          >
            <CheckCircle2 className={`w-5 h-5 ${decision === 'ACCEPTED' ? 'text-emerald-600' : 'text-slate-400'}`} />
            <div>
              <p className="font-semibold text-xs">Accept PO</p>
              <p className="text-[11px] opacity-75">Meet delivery terms</p>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setDecision('REVISED_DATE_PROPOSED')}
            className={`p-3 rounded-xl border text-left flex flex-col justify-between gap-2 transition ${
              decision === 'REVISED_DATE_PROPOSED'
                ? 'border-amber-500 bg-amber-50 text-amber-900 ring-2 ring-amber-500/20'
                : 'border-slate-200 hover:border-slate-300 text-slate-600'
            }`}
          >
            <Calendar className={`w-5 h-5 ${decision === 'REVISED_DATE_PROPOSED' ? 'text-amber-600' : 'text-slate-400'}`} />
            <div>
              <p className="font-semibold text-xs">Propose Date</p>
              <p className="text-[11px] opacity-75">Reschedule delivery</p>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setDecision('REJECTED')}
            className={`p-3 rounded-xl border text-left flex flex-col justify-between gap-2 transition ${
              decision === 'REJECTED'
                ? 'border-rose-500 bg-rose-50 text-rose-900 ring-2 ring-rose-500/20'
                : 'border-slate-200 hover:border-slate-300 text-slate-600'
            }`}
          >
            <XCircle className={`w-5 h-5 ${decision === 'REJECTED' ? 'text-rose-600' : 'text-slate-400'}`} />
            <div>
              <p className="font-semibold text-xs">Decline PO</p>
              <p className="text-[11px] opacity-75">Cannot fulfill</p>
            </div>
          </button>
        </div>

        {/* Promised Date Input */}
        {decision !== 'REJECTED' && (
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Promised Delivery Date *
            </label>
            <input
              type="date"
              value={promisedDate}
              onChange={(e) => setPromisedDate(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-4 py-2.5 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>
        )}

        {/* Notes */}
        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
            Vendor Note / Justification (Optional)
          </label>
          <textarea
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="e.g. Products will be dispatched from our Tuzla warehouse on Tuesday..."
            className="w-full bg-slate-50 border border-slate-300 rounded-xl px-4 py-2.5 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
          />
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 text-sm font-medium text-slate-600 hover:text-slate-800 transition"
          >
            Cancel
          </button>
          <button
            type="button"
            disabled={acknowledgeMutation.isPending}
            onClick={handleConfirm}
            className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm shadow transition disabled:opacity-50"
          >
            {acknowledgeMutation.isPending ? 'Submitting...' : 'Submit Acknowledgment'}
          </button>
        </div>
      </div>
    </Modal>
  )
}
