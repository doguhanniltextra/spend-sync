import { Truck, AlertOctagon, DollarSign } from 'lucide-react'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import type { PendingPOForReceivingResponse } from '@/types/receiving.types'

interface ReceivingSummaryBarProps {
  pendingOrders: PendingPOForReceivingResponse[]
}

export function ReceivingSummaryBar({ pendingOrders }: ReceivingSummaryBarProps) {
  const pendingCount  = pendingOrders.length
  const totalValue    = pendingOrders.reduce((acc, o) => acc + (o.totalAmount || 0), 0)
  const partialCount  = pendingOrders.filter((o) => o.status === 'PARTIALLY_RECEIVED').length

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      {/* 1. Pending Inbound Shipments */}
      <div className="bg-white rounded-lg p-4 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            Pending Dock Deliveries
          </span>
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center">
            <Truck className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 text-xl font-bold text-slate-900">
          {pendingCount} Orders
        </div>
        <p className="text-[11px] text-slate-500 mt-1">
          Awaiting physical receiving inspection
        </p>
      </div>

      {/* 2. Total Inbound Inventory Value */}
      <div className="bg-white rounded-lg p-4 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            Inbound Delivery Value
          </span>
          <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
            <DollarSign className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2">
          <CurrencyDisplay amount={totalValue} className="text-xl font-bold text-slate-900" />
        </div>
        <p className="text-[11px] text-emerald-700 font-medium mt-1">
          Open commercial purchase commitments
        </p>
      </div>

      {/* 3. Partial Receipts in Progress */}
      <div className="bg-white rounded-lg p-4 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            Partially Received
          </span>
          <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center">
            <AlertOctagon className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 text-xl font-bold text-slate-900">
          {partialCount} In-Progress
        </div>
        <p className="text-[11px] text-amber-700 font-medium mt-1">
          Split / installment deliveries
        </p>
      </div>
    </div>
  )
}
