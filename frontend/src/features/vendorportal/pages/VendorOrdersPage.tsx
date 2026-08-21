import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Truck, ChevronRight, Filter } from 'lucide-react'
import { useVendorOrders } from '../hooks/useVendorPortalQueries'
import { ROUTES } from '@/constants/routes'

export function VendorOrdersPage() {
  const [statusFilter, setStatusFilter] = useState<string>('')
  const { data: orders = [], isLoading } = useVendorOrders(statusFilter || undefined)

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ISSUED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">Action Required: New PO</span>
      case 'ACKNOWLEDGED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">Acknowledged</span>
      case 'IN_TRANSIT':
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">In Transit (ASN)</span>
      case 'RECEIVED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">Goods Received (Ready for PO-Flip)</span>
      default:
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">{status}</span>
    }
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight">
            Purchase Orders & Shipments
          </h2>
          <p className="text-sm text-slate-500 mt-1">
            Review incoming purchase orders from buyer enterprises, acknowledge delivery dates, and dispatch ASN waybills.
          </p>
        </div>

        {/* Status Filter */}
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-white border border-slate-300 rounded-xl px-3 py-2 text-xs font-medium text-slate-700 focus:ring-2 focus:ring-teal-500 focus:outline-none"
          >
            <option value="">All PO Statuses</option>
            <option value="ISSUED">Action Required (New)</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
            <option value="IN_TRANSIT">In Transit</option>
            <option value="RECEIVED">Goods Received</option>
          </select>
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-slate-500">
            <div className="w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
            Loading purchase orders...
          </div>
        ) : orders.length === 0 ? (
          <div className="p-12 text-center space-y-2">
            <Truck className="w-10 h-10 text-slate-300 mx-auto" />
            <h3 className="text-base font-semibold text-slate-700">No Orders Found</h3>
            <p className="text-xs text-slate-400">
              There are no purchase orders matching your selected status filter.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/75 text-slate-500 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3.5 px-6">PO Number</th>
                  <th className="py-3.5 px-6">Buyer Enterprise</th>
                  <th className="py-3.5 px-6">Delivery Destination</th>
                  <th className="py-3.5 px-6 text-right">Total Amount</th>
                  <th className="py-3.5 px-6">Terms</th>
                  <th className="py-3.5 px-6">Status</th>
                  <th className="py-3.5 px-6 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {orders.map((po) => (
                  <tr key={po.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-4 px-6 font-mono font-bold text-slate-900">
                      {po.poNumber}
                    </td>
                    <td className="py-4 px-6 font-medium text-slate-700">
                      {po.legalEntityName}
                    </td>
                    <td className="py-4 px-6 text-xs text-slate-600">
                      <p className="font-semibold text-slate-800">{po.deliveryFacilityName}</p>
                      <p className="text-slate-400 truncate max-w-[200px]">{po.deliveryAddress}</p>
                    </td>
                    <td className="py-4 px-6 text-right font-bold text-slate-900">
                      {po.totalAmount.toLocaleString()} {po.currency}
                    </td>
                    <td className="py-4 px-6 text-xs text-slate-600">
                      <span className="font-semibold">{po.paymentTerms}</span> • {po.incoterms}
                    </td>
                    <td className="py-4 px-6">
                      {getStatusBadge(po.status)}
                    </td>
                    <td className="py-4 px-6 text-right">
                      <Link
                        to={ROUTES.vendor.orderDetail(po.id)}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-xs shadow-sm transition"
                      >
                        <span>View Order</span>
                        <ChevronRight className="w-3.5 h-3.5" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
export default VendorOrdersPage
