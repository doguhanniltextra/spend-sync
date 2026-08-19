import { Package, Send, CheckCircle2, DollarSign } from 'lucide-react'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import type { PurchaseOrderSummaryResponse } from '@/types/purchasing.types'

interface PurchaseOrderSummaryBarProps {
  orders:          PurchaseOrderSummaryResponse[]
  activeStatus:    string
  onStatusSelect:  (status: string) => void
}

export function PurchaseOrderSummaryBar({
  orders,
  activeStatus,
  onStatusSelect,
}: PurchaseOrderSummaryBarProps) {
  const draftCount    = orders.filter((o) => o.status === 'DRAFT').length
  const issuedOrders  = orders.filter((o) => o.status === 'ISSUED' || o.status === 'PARTIALLY_RECEIVED')
  const closedCount   = orders.filter((o) => o.status === 'FULFILLED').length

  const activeExposure = issuedOrders.reduce((acc, o) => acc + (o.totalAmount || 0), 0)
  const totalVolume    = orders.reduce((acc, o) => acc + (o.totalAmount || 0), 0)

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* 1. Active Commercial Exposure */}
      <div className="bg-white rounded-lg p-4 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            Active PO Exposure
          </span>
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center">
            <DollarSign className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2">
          <CurrencyDisplay amount={activeExposure} className="text-xl font-bold text-slate-900" />
        </div>
        <p className="text-[11px] text-slate-500 mt-1">
          {issuedOrders.length} orders in commercial transit
        </p>
      </div>

      {/* 2. Draft Orders Awaiting Dispatch */}
      <div
        onClick={() => onStatusSelect(activeStatus === 'DRAFT' ? 'ALL' : 'DRAFT')}
        className={`bg-white rounded-lg p-4 border transition-all cursor-pointer shadow-2xs hover:border-slate-400 ${
          activeStatus === 'DRAFT' ? 'border-amber-400 ring-2 ring-amber-100 bg-amber-50/20' : 'border-slate-200'
        }`}
      >
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            Draft Orders
          </span>
          <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center">
            <Package className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 text-xl font-bold text-slate-900">
          {draftCount}
        </div>
        <p className="text-[11px] text-amber-700 font-medium mt-1">
          {draftCount > 0 ? 'Awaiting official vendor dispatch' : 'All orders issued'}
        </p>
      </div>

      {/* 3. Live Supplier Orders */}
      <div
        onClick={() => onStatusSelect(activeStatus === 'ISSUED' ? 'ALL' : 'ISSUED')}
        className={`bg-white rounded-lg p-4 border transition-all cursor-pointer shadow-2xs hover:border-slate-400 ${
          activeStatus === 'ISSUED' ? 'border-blue-400 ring-2 ring-blue-100 bg-blue-50/20' : 'border-slate-200'
        }`}
      >
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            Issued Orders
          </span>
          <div className="w-8 h-8 rounded-lg bg-slate-900 text-white flex items-center justify-center">
            <Send className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 text-xl font-bold text-slate-900">
          {issuedOrders.length}
        </div>
        <p className="text-[11px] text-slate-500 mt-1">
          Dispatched to supplier order desks
        </p>
      </div>

      {/* 4. Total Procurement Volume */}
      <div className="bg-white rounded-lg p-4 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            Total PO Pipeline
          </span>
          <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
            <CheckCircle2 className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2">
          <CurrencyDisplay amount={totalVolume} className="text-xl font-bold text-slate-900" />
        </div>
        <p className="text-[11px] text-emerald-700 font-medium mt-1">
          {closedCount} fulfilled / closed orders
        </p>
      </div>
    </div>
  )
}
