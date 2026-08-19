import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Select } from '@/components/ui/Select'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { useOrgContext } from '@/features/requisitions/hooks/useOrgContext'
import { usePaymentActions } from '../hooks/usePaymentActions'
import type { DueInvoiceResponse, PaymentMethod } from '@/types/payment.types'
import { PAYMENT_COPY } from '../constants/paymentCopy'

interface CreateBatchModalProps {
  selectedInvoices: DueInvoiceResponse[]
  isOpen:           boolean
  onClose:          () => void
  onSuccess:        () => void
}

export function CreateBatchModal({
  selectedInvoices,
  isOpen,
  onClose,
  onSuccess,
}: CreateBatchModalProps) {
  const { legalEntities } = useOrgContext()
  const { createBatch, isCreating } = usePaymentActions()

  const defaultEntityId = selectedInvoices[0]?.legalEntityId || legalEntities[0]?.id || ''
  const [legalEntityId, setLegalEntityId] = useState(defaultEntityId)
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('ISO_20022_PAIN_001')

  const totalAmount = selectedInvoices.reduce((acc, inv) => acc + (inv.totalAmount || 0), 0)
  const currency    = selectedInvoices[0]?.currency || 'TRY'

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!legalEntityId || selectedInvoices.length === 0) return

    try {
      await createBatch({
        legalEntityId,
        paymentMethod,
        invoiceIds: selectedInvoices.map((inv) => inv.id),
      })
      onSuccess()
      onClose()
    } catch {
      // Handled in mutation hook
    }
  }

  const entityOptions = [
    { value: '', label: 'Select Originating Entity...' },
    ...legalEntities.map((le) => ({ value: le.id, label: `${le.name} (${le.companyCode} • ${le.baseCurrency})` })),
  ]

  const methodOptions = [
    { value: 'ISO_20022_PAIN_001', label: PAYMENT_COPY.modal.methodIso },
    { value: 'BANK_TRANSFER_EFT', label: PAYMENT_COPY.modal.methodEft },
    { value: 'SWIFT_WIRE', label: PAYMENT_COPY.modal.methodSwift },
  ]

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={PAYMENT_COPY.modal.title}
      description={PAYMENT_COPY.modal.description}
      maxWidth="xl"
      footer={
        <>
          <Button type="button" variant="outline" size="sm" onClick={onClose} disabled={isCreating}>
            Cancel
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={handleSubmit}
            isLoading={isCreating}
            disabled={!legalEntityId || selectedInvoices.length === 0}
            className="bg-slate-900 text-white"
          >
            {PAYMENT_COPY.modal.submitCTA}
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4 text-xs">
        {/* Entity & Account Selector */}
        <Select
          label={PAYMENT_COPY.modal.selectEntity}
          value={legalEntityId}
          onChange={(e) => setLegalEntityId(e.target.value)}
          options={entityOptions}
          required
        />

        {/* Payment Method */}
        <Select
          label={PAYMENT_COPY.modal.paymentMethod}
          value={paymentMethod}
          onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
          options={methodOptions}
          required
        />

        {/* Selected Invoices Box */}
        <div className="border border-slate-200 rounded-lg overflow-hidden">
          <div className="bg-slate-50 px-3 py-2 border-b border-slate-200 flex justify-between items-center">
            <span className="font-bold text-slate-900 uppercase text-[10px] tracking-wider">
              {PAYMENT_COPY.modal.selectedSummary} ({selectedInvoices.length})
            </span>
            <span className="text-[10px] text-slate-500 font-mono">ISO 20022 pain.001 Ready</span>
          </div>

          <div className="max-h-48 overflow-y-auto divide-y divide-slate-100 bg-white">
            {selectedInvoices.map((inv) => (
              <div key={inv.id} className="p-2.5 flex items-center justify-between text-xs hover:bg-slate-50/50">
                <div>
                  <strong className="text-slate-900 block font-mono">{inv.invoiceNumber}</strong>
                  <span className="text-[10px] text-slate-500">{inv.vendorName} • {inv.vendorIban || 'No IBAN'}</span>
                </div>
                <div className="text-right font-mono font-bold text-slate-900">
                  <CurrencyDisplay amount={inv.totalAmount} currency={inv.currency as any} />
                </div>
              </div>
            ))}
          </div>

          {/* Batch Total Footer */}
          <div className="bg-slate-900 text-white p-3 flex justify-between items-center">
            <span className="text-xs font-semibold">{PAYMENT_COPY.modal.totalPayable}:</span>
            <CurrencyDisplay
              amount={totalAmount}
              currency={currency as any}
              className="text-base font-extrabold font-mono text-white"
            />
          </div>
        </div>
      </form>
    </Modal>
  )
}
