import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Send, XCircle, Copy, Check, Download, FileCode, AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { PaymentBatchStatusBadge } from './components/PaymentBatchStatusBadge'
import { usePaymentBatchDetail } from './hooks/usePaymentBatchDetail'
import { usePaymentActions } from './hooks/usePaymentActions'
import { formatDateTime } from '@/utils/date'
import { ROUTES } from '@/constants/routes'
import { PAYMENT_COPY } from './constants/paymentCopy'
import { useToast } from '@/components/feedback/Toast'

export default function PaymentRunDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const toast = useToast()

  const { batch, isLoading, refetch } = usePaymentBatchDetail(id ?? null)
  const { approveBatch, cancelBatch, isApproving, isCancelling } = usePaymentActions()

  const [confirmModalOpen, setConfirmModalOpen] = useState(false)
  const [copied, setCopied] = useState(false)
  const [activeTab, setActiveTab] = useState<'ITEMS' | 'XML'>('ITEMS')

  if (isLoading || !batch) {
    return (
      <div className="max-w-5xl mx-auto py-16 text-center text-xs text-slate-500">
        Loading payment batch details...
      </div>
    )
  }

  const handleCopyXml = () => {
    if (!batch.xmlPayload) return
    navigator.clipboard.writeText(batch.xmlPayload)
    setCopied(true)
    toast.success(PAYMENT_COPY.drawer.xmlCopied)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleDownloadXml = () => {
    if (!batch.xmlPayload) return
    const element = document.createElement('a')
    const file = new Blob([batch.xmlPayload], { type: 'application/xml' })
    element.href = URL.createObjectURL(file)
    element.download = `${batch.batchNumber}_pain.001.xml`
    document.body.appendChild(element)
    element.click()
    document.body.removeChild(element)
  }

  const handleApproveConfirm = async () => {
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
    try {
      await cancelBatch(batch.id)
      refetch()
    } catch {
      // Handled in mutation hook
    }
  }

  const isDraft = batch.status === 'DRAFT'

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-20">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <button
            onClick={() => navigate(ROUTES.payments.root)}
            type="button"
            className="inline-flex items-center gap-1 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors mb-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            Back to Payments Console
          </button>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight font-mono">
              {batch.batchNumber}
            </h1>
            <PaymentBatchStatusBadge status={batch.status} />
          </div>
          <p className="text-xs text-slate-500 mt-1">
            {batch.legalEntityName} • Rail: <strong className="font-mono text-slate-700">{batch.paymentMethod}</strong>
          </p>
        </div>

        {isDraft && (
          <div className="flex items-center gap-2">
            <Button
              variant="danger"
              size="sm"
              onClick={handleCancelBatch}
              disabled={isCancelling}
              leftIcon={<XCircle className="w-3.5 h-3.5" />}
            >
              {PAYMENT_COPY.drawer.cancelCTA}
            </Button>
            <Button
              size="sm"
              onClick={() => setConfirmModalOpen(true)}
              disabled={isApproving}
              leftIcon={<Send className="w-3.5 h-3.5" />}
              className="bg-slate-900 text-white hover:bg-slate-800"
            >
              {PAYMENT_COPY.drawer.approveCTA}
            </Button>
          </div>
        )}
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded-lg border border-slate-200 shadow-2xs">
          <span className="text-[10px] text-slate-400 font-bold uppercase block">Total Net Settlement</span>
          <CurrencyDisplay amount={batch.totalAmount} currency={batch.currency as any} className="text-xl font-bold text-slate-900 mt-1" />
          <p className="text-[11px] text-slate-500 mt-1">{batch.itemCount} included supplier invoices</p>
        </div>

        <div className="bg-white p-4 rounded-lg border border-slate-200 shadow-2xs">
          <span className="text-[10px] text-slate-400 font-bold uppercase block">Compiled By (Maker)</span>
          <strong className="text-slate-900 text-sm block mt-1">{batch.createdByUserName}</strong>
          <p className="text-[11px] text-slate-500 mt-0.5">{formatDateTime(batch.createdAt)}</p>
        </div>

        <div className="bg-white p-4 rounded-lg border border-slate-200 shadow-2xs">
          <span className="text-[10px] text-slate-400 font-bold uppercase block">Authorized By (Checker)</span>
          <strong className="text-slate-900 text-sm block mt-1">
            {batch.approvedByUserName || 'Pending Dual Authorization'}
          </strong>
          <p className="text-[11px] text-slate-500 mt-0.5">
            {batch.approvedAt ? formatDateTime(batch.approvedAt) : 'Four-Eyes Review Required'}
          </p>
        </div>
      </div>

      {/* View Tabs: Items vs ISO XML */}
      <div className="flex border-b border-slate-200 gap-6">
        <button
          type="button"
          onClick={() => setActiveTab('ITEMS')}
          className={`pb-3 font-bold text-xs transition-colors border-b-2 ${
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
          className={`pb-3 font-bold text-xs flex items-center gap-1.5 transition-colors border-b-2 ${
            activeTab === 'XML'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          <FileCode className="w-3.5 h-3.5" />
          {PAYMENT_COPY.drawer.sectionXml}
        </button>
      </div>

      {/* Tab 1: Line Items */}
      {activeTab === 'ITEMS' && (
        <div className="bg-white rounded-lg border border-slate-200 overflow-hidden shadow-2xs">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-[10px]">
              <tr>
                <th className="px-4 py-3">Invoice #</th>
                <th className="px-4 py-3">Payee Supplier</th>
                <th className="px-4 py-3">Settlement IBAN</th>
                <th className="px-4 py-3 text-right">Gross Amount</th>
                <th className="px-4 py-3 text-right">Net Payable</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white font-mono">
              {batch.items.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/50">
                  <td className="px-4 py-3 font-bold text-slate-900">{item.invoiceNumber}</td>
                  <td className="px-4 py-3 font-sans font-medium text-slate-900">{item.vendorName}</td>
                  <td className="px-4 py-3 text-slate-600 font-mono text-xs">{item.vendorIban || '—'}</td>
                  <td className="px-4 py-3 text-right text-slate-600">
                    <CurrencyDisplay amount={item.amount} currency={batch.currency as any} />
                  </td>
                  <td className="px-4 py-3 text-right font-bold text-slate-900">
                    <CurrencyDisplay amount={item.netPayableAmount} currency={batch.currency as any} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Tab 2: ISO 20022 XML Inspector */}
      {activeTab === 'XML' && (
        <div className="space-y-3 bg-white p-5 rounded-lg border border-slate-200 shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-500 font-mono">
              Standard: ISO 20022 pain.001.001.03 (CustomerCreditTransferInitiation)
            </span>
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                variant="outline"
                onClick={handleCopyXml}
                leftIcon={copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
              >
                {copied ? 'Copied!' : PAYMENT_COPY.drawer.copyXml}
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={handleDownloadXml}
                leftIcon={<Download className="w-3.5 h-3.5" />}
              >
                Download XML
              </Button>
            </div>
          </div>

          <div className="bg-slate-900 text-slate-100 p-5 rounded-lg font-mono text-xs overflow-x-auto max-h-[500px] border border-slate-800 leading-relaxed">
            <pre>{batch.xmlPayload || '<!-- XML Payload will be generated upon dispatch -->'}</pre>
          </div>
        </div>
      )}

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
              Authorizing batch <strong>{batch.batchNumber}</strong> will trigger live wire disbursement of{' '}
              <strong>{batch.totalAmount.toLocaleString()} {batch.currency}</strong> across {batch.itemCount} suppliers.
            </p>
          </div>
        </div>
      </Modal>
    </div>
  )
}
