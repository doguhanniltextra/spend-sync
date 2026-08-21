import { useState } from 'react'
import { Truck, ShieldCheck, AlertCircle, Package } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { useDispatchAsn } from '../../hooks/useVendorPortalQueries'
import type { VendorOrderResponse } from '../../types/vendorPortal.types'

interface Props {
  order: VendorOrderResponse
  isOpen: boolean
  onClose: () => void
}

export function VendorAsnDispatchModal({ order, isOpen, onClose }: Props) {
  const dispatchMutation = useDispatchAsn(order.id)

  const [waybillNumber, setWaybillNumber] = useState(`IRS2026${Math.floor(100000 + Math.random() * 900000)}`)
  const [carrierCompany, setCarrierCompany] = useState('Borusan Lojistik & Express')
  const [trackingNumber, setTrackingNumber] = useState(`TRK-${Math.floor(10000000 + Math.random() * 90000000)}`)
  const [vehiclePlate, setVehiclePlate] = useState('34 ABC 789')
  const [driverFullName, setDriverFullName] = useState('Mehmet Kaya')
  const [driverNationalId, setDriverNationalId] = useState('44556677889')
  const [arrivalDate, setArrivalDate] = useState(
    new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  )
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  // Track shipping quantities per line item
  const [lineQuantities, setLineQuantities] = useState<Record<string, number>>(() => {
    const initial: Record<string, number> = {}
    order.lineItems.forEach((item) => {
      initial[item.id] = Math.max(0, item.quantityOrdered - item.quantityReceived)
    })
    return initial
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      setError(null)
      const itemsPayload = Object.entries(lineQuantities)
        .filter(([_, qty]) => qty > 0)
        .map(([id, qty]) => ({
          poLineItemId: id,
          quantityShipped: qty,
        }))

      if (itemsPayload.length === 0) {
        setError('At least one item must have a shipment quantity greater than zero.')
        return
      }

      await dispatchMutation.mutateAsync({
        waybillNumber: waybillNumber.trim(),
        carrierCompany: carrierCompany.trim(),
        trackingNumber: trackingNumber.trim() || undefined,
        vehiclePlate: vehiclePlate.trim() || undefined,
        driverFullName: driverFullName.trim() || undefined,
        driverNationalId: driverNationalId.trim() || undefined,
        estimatedArrivalDate: arrivalDate,
        notes: notes.trim() || undefined,
        items: itemsPayload,
      })

      onClose()
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to dispatch shipment notice.')
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Dispatch Advanced Shipping Notice (ASN / e-İrsaliye)`}>
      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="flex items-center justify-between p-3 rounded-xl bg-teal-50 border border-teal-200 text-teal-900 text-xs">
          <div className="flex items-center gap-2">
            <Truck className="w-4 h-4 text-teal-700 flex-shrink-0" />
            <span>
              Notifying Maslak R&D Receiving Hub for PO <strong className="font-semibold">{order.poNumber}</strong>
            </span>
          </div>
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-teal-800">
            <ShieldCheck className="w-3.5 h-3.5" />
            TCKN Encrypted
          </span>
        </div>

        {error && (
          <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4" />
            <span>{error}</span>
          </div>
        )}

        {/* Shipment Details */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
              e-İrsaliye / Waybill No *
            </label>
            <input
              type="text"
              required
              value={waybillNumber}
              onChange={(e) => setWaybillNumber(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Carrier / Logistics Company *
            </label>
            <input
              type="text"
              required
              value={carrierCompany}
              onChange={(e) => setCarrierCompany(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Vehicle Plate (Plaka)
            </label>
            <input
              type="text"
              value={vehiclePlate}
              onChange={(e) => setVehiclePlate(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Driver Full Name
            </label>
            <input
              type="text"
              value={driverFullName}
              onChange={(e) => setDriverFullName(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Driver National ID (TCKN - Masked)
            </label>
            <input
              type="text"
              maxLength={11}
              value={driverNationalId}
              onChange={(e) => setDriverNationalId(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 font-mono text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Tracking Number
            </label>
            <input
              type="text"
              value={trackingNumber}
              onChange={(e) => setTrackingNumber(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Estimated Delivery Date *
            </label>
            <input
              type="date"
              required
              value={arrivalDate}
              onChange={(e) => setArrivalDate(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          <div className="sm:col-span-2">
            <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Shipment Notes / Dispatch Details
            </label>
            <input
              type="text"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="e.g. Dispatched with temperature controlled vehicle..."
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>
        </div>

        {/* Item Quantities */}
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Package className="w-4 h-4 text-slate-600" />
            <h4 className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
              Shipment Line Items
            </h4>
          </div>
          <div className="border border-slate-200 rounded-xl overflow-hidden divide-y divide-slate-200 text-xs">
            {order.lineItems.map((item) => (
              <div key={item.id} className="p-3 bg-slate-50/50 flex items-center justify-between">
                <div>
                  <p className="font-semibold text-slate-800">{item.itemDescription}</p>
                  <p className="text-slate-500 text-[11px]">
                    Ordered: {item.quantityOrdered} | Received: {item.quantityReceived}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-slate-600 font-medium">Shipped Qty:</span>
                  <input
                    type="number"
                    min={0}
                    max={item.quantityOrdered - item.quantityReceived}
                    value={lineQuantities[item.id] || 0}
                    onChange={(e) =>
                      setLineQuantities((prev) => ({
                        ...prev,
                        [item.id]: Number(e.target.value),
                      }))
                    }
                    className="w-20 bg-white border border-slate-300 rounded-lg px-2.5 py-1 text-center font-semibold text-slate-800"
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-800 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={dispatchMutation.isPending}
            className="px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-semibold text-sm shadow transition disabled:opacity-50"
          >
            {dispatchMutation.isPending ? 'Dispatching...' : 'Dispatch ASN / e-İrsaliye'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
