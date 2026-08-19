import { useState } from 'react'
import { Send, XCircle, Copy, Check, FileCode, AlertTriangle } from 'lucide-react'
import { Drawer } from '@/components/ui/Drawer'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { PaymentBatchStatusBadge } from './PaymentBatchStatusBadge'
import { usePaymentBatchDetail } from '../hooks/usePaymentBatchDetail'
import { usePaymentActions } from '../hooks/usePaymentActions'
import { formatDateTime } from '@/utils/date'
import { PAYMENT_COPY } from '../constants/paymentCopy'
import { useToast } from '@/components/feedback/Toast'

interface PaymentBatchDetailDrawerProps {
  batchId:  string | null
  onClose:  () => void
}

export function PaymentBatchDetailDrawer({ batchId, onClose }: PaymentBatchDetailDrawerProps) {
  const toast = useToast()
  const { batch, isLoading, refetch } = usePaymentBatchDetail(batchId)
  const { approveBatch, cancelBatch, isApproving, isCancelling } = usePaymentActions()

  const [confirmModalOpen, setConfirmModalOpen] = useState(false)
  const [copied, setCopied] = useState(false)
  const [activeTab, setActiveTab] = useState<'ITEMS' | 'XML'>('ITEMS')

  if (!batchId) return null

  const handleCopyXml = () => {
    if (!batch?.xmlPayload) return
    navigator.clipboard.writeText(batch.xmlPayload)
    setCopied(true)
    toast.success(PAYMENT_COPY.drawer.xmlCopied)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleApproveConfirm = async () => {
    if (!batch) return
    try {
      await approveBatch({
        id: batch.id,
        payload: { approvalNote: 'Authorized for final bank settlement by Treasury' },
      })
      setConfirmModalOpen(false)
      refetch()
    } catch {
      // Handled in mutation hook
    }
  }

  const handleCancelBatch = async () => {
    if (!batch) return
    try {
      await cancelBatch(batch.id)
      refetch()
    } catch {
      // Handled in mutation hook
    }
  }

  const isDraft = batch?.status === 'DRAFT'

  return (
    <>
      <Drawer
        isOpen={Boolean(batchId)}
        onClose={onClose}
        title={batch ? `${batch.batchNumber}` : 'Payment Batch Details'}
        subtitle={batch ? `${batch.legalEntityName} • Rail: ${batch.paymentMethod}` : undefined}
        size="xl"
        footer={
          <div className="flex items-center justify-between w-full">
            <div>
              {isDraft && (
                <Button
                  variant="danger"
                  size="sm"
                  onClick={handleCancelBatch}
                  disabled={isCancelling}
                  leftIcon={<XCircle className="w-3.5 h-3.5" />}
                >
                  {PAYMENT_COPY.drawer.cancelCTA}
                </Button>
              )}
            </div>

            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={onClose}>
                Close
              </Button>

              {isDraft && (
                <Button
                  size="sm"
                  onClick={() => setConfirmModalOpen(true)}
                  disabled={isApproving}
                  leftIcon={<Send className="w-3.5 h-3.5" />}
                  className="bg-slate-900 text-white hover:bg-slate-800"
                >
                  {PAYMENT_COPY.drawer.approveCTA}
                </Button>
              )}
            </div>
          </div>
        }
      >
        {isLoading || !batch ? (
          <div className="py-8 text-center text-xs text-slate-500">Loading payment batch details...</div>
        ) : (
          <div className="space-y-6 text-xs text-slate-700">
            {/* Header Status & Total Value */}
            <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 flex items-center justify-between">
              <div>
                <span className="text-[10px] uppercase font-semibold text-slate-500 block">Total Net Settlement</span>
                <CurrencyDisplay amount={batch.totalAmount} currency={batch.currency as any} className="text-xl font-bold text-slate-900" />
              </div>

              <div className="flex items-center gap-3">
                <PaymentBatchStatusBadge status={batch.status} />
                <span className="font-mono text-xs bg-white px-2 py-0.5 rounded border border-slate-200 text-slate-700">
                  {batch.itemCount} Invoices
                </span>
              </div>
            </div>

            {/* Commercial Metadata Card */}
            <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
              <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
                {PAYMENT_COPY.drawer.sectionSummary}
              </h4>
              <div className="grid grid-cols-2 gap-3 text-slate-700">
                <div>
                  <span className="text-slate-400 block text-[10px]">Originating Entity:</span>
                  <strong className="text-slate-900 font-medium">{batch.legalEntityName}</strong>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">Payment Rail:</span>
                  <span className="font-mono font-bold text-slate-900">{batch.paymentMethod}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">Compiled By:</span>
                  <span className="text-slate-800">{batch.createdByUserName} ({formatDateTime(batch.createdAt)})</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">Authorized By:</span>
                  <span className="text-slate-800">
                    {batch.approvedByUserName ? `${batch.approvedByUserName} (${formatDateTime(batch.approvedAt)})` : 'Pending Dual-Authorization'}
                  </span>
                </div>
              </div>
            </div>

            {/* View Tabs: Items vs ISO XML */}
            <div className="flex border-b border-slate-200 gap-4">
              <button
                type="button"
                onClick={() => setActiveTab('ITEMS')}
                className={`pb-2.5 font-bold text-xs transition-colors border-b-2 ${
                  activeTab === 'ITEMS'
                    ? 'border-slate-900 text-slate-900'
                    : 'border-transparent text-slate-500 hover:text-slate-900'
                }`}
              >
                {PAYMENT_COPY.drawer.sectionItems} ({batch.items.length})
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('XML')}
                className={`pb-2.5 font-bold text-xs flex items-center gap-1.5 transition-colors border-b-2 ${
                  activeTab === 'XML'
                    ? 'border-slate-900 text-slate-900'
                    : 'border-transparent text-slate-500 hover:text-slate-900'
                }`}
              >
                <FileCode className="w-3.5 h-3.5" />
                {PAYMENT_COPY.drawer.sectionXml}
              </button>
            </div>

            {/* Tab 1: Line Items Table */}
            {activeTab === 'ITEMS' && (
              <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-[10px]">
                      <tr>
                        <th className="px-3 py-2.5">Invoice #</th>
                        <th className="px-3 py-2.5">Payee Supplier</th>
                        <th className="px-3 py-2.5">IBAN</th>
                        <th className="px-3 py-2.5 text-right">Gross</th>
                        <th className="px-3 py-2.5 text-right">Net Payable</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white font-mono">
                      {batch.items.map((item) => (
                        <tr key={item.id} className="hover:bg-slate-50/50">
                          <td className="px-3 py-2.5 font-bold text-slate-900">{item.invoiceNumber}</td>
                          <td className="px-3 py-2.5 font-sans font-medium text-slate-900">{item.vendorName}</td>
                          <td className="px-3 py-2.5 text-slate-600 font-mono text-[11px] truncate max-w-[180px]">
                            {item.vendorIban || '—'}
                          </td>
                          <td className="px-3 py-2.5 text-right text-slate-600">
                            <CurrencyDisplay amount={item.amount} currency={batch.currency as any} />
                          </td>
                          <td className="px-3 py-2.5 text-right font-bold text-slate-900">
                            <CurrencyDisplay amount={item.netPayableAmount} currency={batch.currency as any} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Tab 2: ISO 20022 XML Inspector */}
            {activeTab === 'XML' && (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] text-slate-500 font-mono">
                    Standard: ISO 20022 pain.001.001.03 (CustomerCreditTransferInitiation)
                  </span>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={handleCopyXml}
                    leftIcon={copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                  >
                    {copied ? 'Copied!' : PAYMENT_COPY.drawer.copyXml}
                  </Button>
                </div>

                <div className="bg-slate-900 text-slate-100 p-4 rounded-lg font-mono text-[11px] overflow-x-auto max-h-96 border border-slate-800 leading-relaxed">
                  <pre>{batch.xmlPayload || '<!-- XML Payload will be generated upon dispatch -->'}</pre>
                </div>
              </div>
            )}
          </div>
        )}
      </Drawer>

      {/* Four-Eyes Double-Confirm Authorization Modal */}
      <Modal
        isOpen={confirmModalOpen}
        onClose={() => setConfirmModalOpen(false)}
        title={PAYMENT_COPY.drawer.doubleConfirmTitle}
        description={PAYMENT_COPY.drawer.doubleConfirmDesc}
        maxWidth="md"
        footer={
          <>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setConfirmModalOpen(false)}
              disabled={isApproving}
            >
              Cancel
            </Button>
            <Button
              type="button"
              size="sm"
              onClick={handleApproveConfirm}
              isLoading={isApproving}
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
            >
              Confirm & Dispatch to Bank
            </Button>
          </>
        }
      >
        <div className="p-3.5 bg-amber-50 rounded-lg border border-amber-200 flex items-start gap-2.5 text-amber-900 text-xs">
          <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
          <div>
            <strong className="block font-semibold">Treasury Execution Notice</strong>
            <p className="text-amber-800 text-[11px] mt-0.5">
              Authorizing batch <strong>{batch?.batchNumber}</strong> will trigger live wire disbursement of{' '}
              <strong>{batch?.totalAmount.toLocaleString()} {batch?.currency}</strong> across {batch?.itemCount} suppliers.
            </p>
          </div>
        </div>
      </Modal>
    </>
  )
}
