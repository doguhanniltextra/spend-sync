import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  ArrowLeft,
  Truck,
  CheckCircle2,
  Receipt,
  MapPin,
  CreditCard,
  Package,
  AlertCircle,
} from 'lucide-react'
import { useVendorOrder } from '../hooks/useVendorPortalQueries'
import { VendorPoAcknowledgeModal } from '../components/orders/VendorPoAcknowledgeModal'
import { VendorAsnDispatchModal } from '../components/orders/VendorAsnDispatchModal'
import { VendorPoFlipModal } from '../components/invoices/VendorPoFlipModal'
import { ROUTES } from '@/constants/routes'

export function VendorOrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: order, isLoading, error } = useVendorOrder(id || '')

  const [isAcknowledgeOpen, setIsAcknowledgeOpen] = useState(false)
  const [isAsnOpen, setIsAsnOpen] = useState(false)
  const [isPoFlipOpen, setIsPoFlipOpen] = useState(false)

  if (isLoading) {
    return (
      <div className="p-12 text-center text-slate-500">
        <div className="w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
        Loading purchase order details...
      </div>
    )
  }

  if (error || !order) {
    return (
      <div className="max-w-3xl mx-auto p-8 bg-rose-50 border border-rose-200 rounded-2xl text-center space-y-4">
        <AlertCircle className="w-8 h-8 text-rose-500 mx-auto" />
        <h3 className="text-base font-bold text-rose-800">Purchase Order Not Found</h3>
        <p className="text-xs text-rose-600">The requested order could not be retrieved.</p>
        <Link
          to={ROUTES.vendor.orders}
          className="inline-flex items-center gap-1 text-xs font-semibold text-rose-700 underline"
        >
          <ArrowLeft className="w-3.5 h-3.5" /> Back to Orders
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Top Breadcrumb & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to={ROUTES.vendor.orders}
            className="p-2 rounded-xl bg-white border border-slate-200 text-slate-600 hover:text-slate-900 transition shadow-sm"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-2xl font-extrabold text-slate-900 font-mono tracking-tight">
                {order.poNumber}
              </h2>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">
                Rev {order.revisionNumber}
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Buyer: <strong className="text-slate-700">{order.legalEntityName}</strong>
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2.5">
          {/* Acknowledge Button */}
          {order.status === 'ISSUED' && (
            <button
              onClick={() => setIsAcknowledgeOpen(true)}
              className="px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs shadow-sm flex items-center gap-1.5 transition"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>Acknowledge PO</span>
            </button>
          )}

          {/* Dispatch ASN Button */}
          {(order.status === 'ACKNOWLEDGED' || order.status === 'PARTIALLY_DELIVERED') && (
            <button
              onClick={() => setIsAsnOpen(true)}
              className="px-4 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-semibold text-xs shadow-sm flex items-center gap-1.5 transition"
            >
              <Truck className="w-4 h-4" />
              <span>Dispatch ASN / e-İrsaliye</span>
            </button>
          )}

          {/* 1-Click PO-Flip Button */}
          {(order.status === 'RECEIVED' || order.status === 'IN_TRANSIT' || order.status === 'ACKNOWLEDGED') && (
            <button
              onClick={() => setIsPoFlipOpen(true)}
              className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs shadow-sm flex items-center gap-1.5 transition"
            >
              <Receipt className="w-4 h-4" />
              <span>1-Click PO-Flip Invoicing</span>
            </button>
          )}
        </div>
      </div>

      {/* Info Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {/* Card 1: Destination Facility */}
        <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold uppercase tracking-wider">
            <MapPin className="w-4 h-4 text-teal-600" />
            <span>Delivery Facility</span>
          </div>
          <div>
            <p className="font-bold text-slate-800 text-sm">{order.deliveryFacilityName}</p>
            <p className="text-xs text-slate-500 mt-1">{order.deliveryAddress}</p>
          </div>
        </div>

        {/* Card 2: Commercial Terms */}
        <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold uppercase tracking-wider">
            <CreditCard className="w-4 h-4 text-indigo-600" />
            <span>Payment & Incoterms</span>
          </div>
          <div>
            <p className="font-bold text-slate-800 text-sm">
              {order.paymentTerms} • {order.incoterms}
            </p>
            <p className="text-xs text-slate-500 mt-1">
              Promised Delivery: {order.vendorPromisedDeliveryDate || 'Pending Acknowledgment'}
            </p>
          </div>
        </div>

        {/* Card 3: Financial Summary */}
        <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold uppercase tracking-wider">
            <Receipt className="w-4 h-4 text-emerald-600" />
            <span>Order Value</span>
          </div>
          <div>
            <p className="text-2xl font-black text-slate-900">
              {order.totalAmount.toLocaleString()} {order.currency}
            </p>
            <p className="text-xs text-slate-500 mt-1">
              Status: <span className="font-semibold text-slate-700">{order.status}</span>
            </p>
          </div>
        </div>
      </div>

      {/* Order Line Items Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 bg-slate-50/50 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Package className="w-4 h-4 text-slate-600" />
            <h3 className="font-bold text-slate-800 text-sm">Line Items & Specifications</h3>
          </div>
          <span className="text-xs text-slate-500 font-medium">
            {order.lineItems.length} item(s)
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500 font-semibold text-xs uppercase tracking-wider bg-slate-50/25">
                <th className="py-3 px-6">Description / SKU</th>
                <th className="py-3 px-6 text-center">Ordered</th>
                <th className="py-3 px-6 text-center">Received (Mal Kabul)</th>
                <th className="py-3 px-6 text-right">Unit Price</th>
                <th className="py-3 px-6 text-right">KDV %</th>
                <th className="py-3 px-6 text-right">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {order.lineItems.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/60 transition">
                  <td className="py-4 px-6">
                    <p className="font-semibold text-slate-800">{item.itemDescription}</p>
                    {item.sku && <p className="text-xs font-mono text-slate-400">SKU: {item.sku}</p>}
                  </td>
                  <td className="py-4 px-6 text-center font-semibold text-slate-700">
                    {item.quantityOrdered} {item.unitOfMeasure || 'EA'}
                  </td>
                  <td className="py-4 px-6 text-center font-bold text-emerald-700">
                    {item.quantityReceived} {item.unitOfMeasure || 'EA'}
                  </td>
                  <td className="py-4 px-6 text-right font-medium text-slate-800">
                    {item.unitPrice.toLocaleString()} {order.currency}
                  </td>
                  <td className="py-4 px-6 text-right text-xs text-slate-600">
                    %{item.taxRate}
                  </td>
                  <td className="py-4 px-6 text-right font-bold text-slate-900">
                    {item.totalAmount.toLocaleString()} {order.currency}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modals */}
      <VendorPoAcknowledgeModal
        order={order}
        isOpen={isAcknowledgeOpen}
        onClose={() => setIsAcknowledgeOpen(false)}
      />

      <VendorAsnDispatchModal
        order={order}
        isOpen={isAsnOpen}
        onClose={() => setIsAsnOpen(false)}
      />

      <VendorPoFlipModal
        order={order}
        isOpen={isPoFlipOpen}
        onClose={() => setIsPoFlipOpen(false)}
      />
    </div>
  )
}
export default VendorOrderDetailPage
