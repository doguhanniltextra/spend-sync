import { useState } from 'react'
import { Zap, ShieldCheck, AlertCircle, CheckCircle2 } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { usePoFlipInvoice } from '../../hooks/useVendorPortalQueries'
import type { VendorOrderResponse } from '../../types/vendorPortal.types'

interface Props {
  order: VendorOrderResponse
  isOpen: boolean
  onClose: () => void
}

export function VendorPoFlipModal({ order, isOpen, onClose }: Props) {
  const poFlipMutation = usePoFlipInvoice()

  const [invoiceNumber, setInvoiceNumber] = useState(`GIB2026${Math.floor(100000 + Math.random() * 900000)}FLIP`)
  const [invoiceDate, setInvoiceDate] = useState(new Date().toISOString().split('T')[0])
  const [withholdingCode, setWithholdingCode] = useState('601')
  const [notes, setNotes] = useState('1-Click PO-Flip e-Fatura oluşturuldu.')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  // Calculations
  const subtotal = order.totalAmount
  const taxRate = 0.20 // 20% standard VAT
  const baseTax = subtotal * taxRate

  let tevkifatRate = 0
  if (withholdingCode === '601') tevkifatRate = 2 / 10 // 2/10
  else if (withholdingCode === '608') tevkifatRate = 5 / 10 // 5/10
  else if (withholdingCode === '627') tevkifatRate = 7 / 10 // 7/10
  else if (withholdingCode === '610') tevkifatRate = 9 / 10 // 9/10

  const tevkifatAmount = baseTax * tevkifatRate
  const finalPayable = subtotal + baseTax - tevkifatAmount

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      setError(null)
      await poFlipMutation.mutateAsync({
        poId: order.id,
        payload: {
          invoiceNumber: invoiceNumber.trim(),
          invoiceDate,
          taxWithholdingCode: withholdingCode !== 'NONE' ? withholdingCode : undefined,
          notes: notes.trim() || undefined,
        },
      })
      setSuccess(true)
      setTimeout(() => {
        setSuccess(false)
        onClose()
      }, 1500)
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate PO-Flip invoice.')
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="1-Click PO-Flip e-Invoicing (Touchless Match)">
      {success ? (
        <div className="p-8 text-center space-y-3">
          <div className="w-12 h-12 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-bold text-slate-900">PO-Flip Invoice Created & Matched!</h3>
          <p className="text-xs text-slate-500">
            Invoice {invoiceNumber} automatically submitted and matched with PO {order.poNumber}.
          </p>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="p-4 rounded-xl bg-gradient-to-r from-indigo-50 to-teal-50 border border-indigo-100 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 text-indigo-950">
              <Zap className="w-4 h-4 text-indigo-600 flex-shrink-0" />
              <span>
                Flipping PO <strong className="font-semibold">{order.poNumber}</strong> into an official GİB e-Invoice.
              </span>
            </div>
            <span className="inline-flex items-center gap-1 font-semibold text-teal-700">
              <ShieldCheck className="w-3.5 h-3.5" />
              UBL-TR 1.2
            </span>
          </div>

          {error && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          {/* Form Inputs */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Official GİB Invoice No *
              </label>
              <input
                type="text"
                required
                value={invoiceNumber}
                onChange={(e) => setInvoiceNumber(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 font-mono text-sm text-slate-800 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Invoice Date *
              </label>
              <input
                type="date"
                required
                value={invoiceDate}
                onChange={(e) => setInvoiceDate(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
                KDV Tevkifat (Withholding Tax Code)
              </label>
              <select
                value={withholdingCode}
                onChange={(e) => setWithholdingCode(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              >
                <option value="NONE">No Tevkifat (Standard VAT %20)</option>
                <option value="601">601 - Yapım İşleri ile Bu İşlerle Birlikte İfa Edilen Mühendislik/Mimarlık (2/10)</option>
                <option value="608">608 - Temizlik, Çevre ve Bahçe Bakım Hizmetleri (5/10)</option>
                <option value="627">627 - Diğer Hizmetler / Danışmanlık ve Bilişim (7/10)</option>
                <option value="610">610 - Servis Taşımacılığı Hizmeti (9/10)</option>
              </select>
            </div>

            <div className="sm:col-span-2">
              <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Invoice Notes
              </label>
              <input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="e-Invoice notes..."
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Realtime Calculation Breakdown */}
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 text-xs space-y-2">
            <div className="flex justify-between text-slate-600">
              <span>Subtotal (Mal / Hizmet Toplamı):</span>
              <span className="font-semibold text-slate-800">{subtotal.toLocaleString()} {order.currency}</span>
            </div>
            <div className="flex justify-between text-slate-600">
              <span>Calculated KDV (%20):</span>
              <span className="font-semibold text-slate-800">+{baseTax.toLocaleString()} {order.currency}</span>
            </div>
            {tevkifatAmount > 0 && (
              <div className="flex justify-between text-teal-700 font-medium">
                <span>Alıcı Tarafından Tevkif Edilen KDV ({withholdingCode}):</span>
                <span>-{tevkifatAmount.toLocaleString()} {order.currency}</span>
              </div>
            )}
            <div className="pt-2 border-t border-slate-200 flex justify-between text-sm font-bold text-slate-900">
              <span>Net Ödenecek Tutar (Payable Amount):</span>
              <span className="text-indigo-700">{finalPayable.toLocaleString()} {order.currency}</span>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-800 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={poFlipMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm shadow transition disabled:opacity-50 flex items-center gap-1.5"
            >
              <Zap className="w-4 h-4" />
              <span>{poFlipMutation.isPending ? 'Generating...' : 'Submit & Execute 3-Way Match'}</span>
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}
