import { CreditCard, DollarSign, Clock, CheckCircle2 } from 'lucide-react'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import type { DueInvoiceResponse, PaymentBatchResponse } from '@/types/payment.types'
import { PAYMENT_COPY } from '../constants/paymentCopy'

interface PaymentSummaryBarProps {
  dueInvoices: DueInvoiceResponse[]
  batches:     PaymentBatchResponse[]
}

export function PaymentSummaryBar({ dueInvoices, batches }: PaymentSummaryBarProps) {
  const totalDueAmount   = dueInvoices.reduce((acc, inv) => acc + (inv.totalAmount || 0), 0)
  const draftBatches     = batches.filter((b) => b.status === 'DRAFT')
  const dispatchedBatches= batches.filter((b) => b.status === 'DISPATCHED')
  const totalDispatched  = dispatchedBatches.reduce((acc, b) => acc + (b.totalAmount || 0), 0)

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* 1. Approved Due Exposure */}
      <div className="bg-white rounded-lg p-4 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            {PAYMENT_COPY.kpi.dueExposure}
          </span>
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center">
            <DollarSign className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2">
          <CurrencyDisplay amount={totalDueAmount} className="text-xl font-bold text-slate-900" />
        </div>
        <p className="text-[11px] text-slate-500 mt-1">
          {dueInvoices.length} invoices cleared 3-way match
        </p>
      </div>

      {/* 2. Invoices Ready for Batch */}
      <div className="bg-white rounded-lg p-4 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            {PAYMENT_COPY.kpi.dueCount}
          </span>
          <div className="w-8 h-8 rounded-lg bg-slate-900 text-white flex items-center justify-center">
            <CreditCard className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 text-xl font-bold text-slate-900">
          {dueInvoices.length} Invoices
        </div>
        <p className="text-[11px] text-slate-500 mt-1">
          Ready for settlement grouping
        </p>
      </div>

      {/* 3. Batches Awaiting Dispatch */}
      <div className="bg-white rounded-lg p-4 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            {PAYMENT_COPY.kpi.activeBatches}
          </span>
          <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center">
            <Clock className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 text-xl font-bold text-slate-900">
          {draftBatches.length} Batches
        </div>
        <p className="text-[11px] text-amber-700 font-medium mt-1">
          Requires dual authorization / CFO approval
        </p>
      </div>

      {/* 4. Total Dispatched / Settled Volume */}
      <div className="bg-white rounded-lg p-4 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
            {PAYMENT_COPY.kpi.dispatchedVolume}
          </span>
          <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
            <CheckCircle2 className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2">
          <CurrencyDisplay amount={totalDispatched} className="text-xl font-bold text-slate-900" />
        </div>
        <p className="text-[11px] text-emerald-700 font-medium mt-1">
          {dispatchedBatches.length} settled packages
        </p>
      </div>
    </div>
  )
}
