import { useState } from 'react'
import {
  Receipt,
  UploadCloud,
  Eye,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Filter,
} from 'lucide-react'
import { useVendorInvoices } from '../hooks/useVendorPortalQueries'
import { VendorUblUploadModal } from '../components/invoices/VendorUblUploadModal'
import { VendorInvoiceHtmlModal } from '../components/invoices/VendorInvoiceHtmlModal'

export function VendorInvoicesPage() {
  const [statusFilter, setStatusFilter] = useState<string>('')
  const { data: invoices = [], isLoading } = useVendorInvoices(statusFilter || undefined)

  const [isUploadOpen, setIsUploadOpen] = useState(false)
  const [selectedHtmlInvoice, setSelectedHtmlInvoice] = useState<{ id: string; number: string } | null>(null)

  const getMatchBadge = (matchStatus?: string) => {
    switch (matchStatus) {
      case 'AUTO_MATCHED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3" /> Touchless Matched
          </span>
        )
      case 'MANUALLY_MATCHED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
            <CheckCircle2 className="w-3 h-3" /> Manual Matched
          </span>
        )
      case 'DISCREPANCY':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <AlertTriangle className="w-3 h-3" /> Discrepancy
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <Clock className="w-3 h-3" /> Evaluating
          </span>
        )
    }
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'APPROVED_FOR_PAYMENT':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-teal-100 text-teal-800">Approved for Payment</span>
      case 'PAID':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800">Paid (Bank Executed)</span>
      case 'SUBMITTED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-slate-100 text-slate-700">Submitted</span>
      case 'HOLD':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-800">On Hold</span>
      case 'REJECTED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-rose-100 text-rose-800">Rejected</span>
      default:
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-slate-100 text-slate-700">{status}</span>
    }
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight">
            e-Invoices & 3-Way Match Status
          </h2>
          <p className="text-sm text-slate-500 mt-1">
            Track submitted GİB e-invoices, withholding tax breakdowns, and automated touchless match status.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* Status Filter */}
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-slate-400" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-white border border-slate-300 rounded-xl px-3 py-2 text-xs font-medium text-slate-700 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            >
              <option value="">All Invoices</option>
              <option value="APPROVED_FOR_PAYMENT">Approved for Payment</option>
              <option value="PAID">Paid</option>
              <option value="SUBMITTED">Submitted</option>
              <option value="HOLD">On Hold</option>
            </select>
          </div>

          {/* Upload XML Button */}
          <button
            onClick={() => setIsUploadOpen(true)}
            className="px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs shadow-sm flex items-center gap-1.5 transition"
          >
            <UploadCloud className="w-4 h-4" />
            <span>Upload UBL-TR XML</span>
          </button>
        </div>
      </div>

      {/* Invoices Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-slate-500">
            <div className="w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
            Loading supplier invoices...
          </div>
        ) : invoices.length === 0 ? (
          <div className="p-12 text-center space-y-2">
            <Receipt className="w-10 h-10 text-slate-300 mx-auto" />
            <h3 className="text-base font-semibold text-slate-700">No Invoices Found</h3>
            <p className="text-xs text-slate-400">
              Flip an acknowledged purchase order or upload a GİB UBL XML invoice to get started.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/75 text-slate-500 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3.5 px-6">Invoice / ETTN</th>
                  <th className="py-3.5 px-6">PO Reference</th>
                  <th className="py-3.5 px-6">Invoice Date</th>
                  <th className="py-3.5 px-6">Due Date</th>
                  <th className="py-3.5 px-6 text-right">Net Payable</th>
                  <th className="py-3.5 px-6">Match Result</th>
                  <th className="py-3.5 px-6">Status</th>
                  <th className="py-3.5 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {invoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-4 px-6">
                      <p className="font-mono font-bold text-slate-900">{inv.invoiceNumber}</p>
                      <p className="text-[11px] font-mono text-slate-400 truncate max-w-[140px]" title={inv.ettn}>
                        {inv.ettn}
                      </p>
                    </td>
                    <td className="py-4 px-6 text-xs font-mono font-semibold text-slate-700">
                      {inv.poNumber || 'NON-PO'}
                    </td>
                    <td className="py-4 px-6 text-xs text-slate-600">
                      {inv.invoiceDate}
                    </td>
                    <td className="py-4 px-6 text-xs font-medium text-slate-800">
                      {inv.dueDate}
                    </td>
                    <td className="py-4 px-6 text-right">
                      <p className="font-bold text-slate-900">
                        {inv.payableAmount.toLocaleString()} {inv.currency}
                      </p>
                      {inv.withholdingTaxAmount > 0 && (
                        <p className="text-[10px] text-teal-700 font-medium">
                          Tevkifat: -{inv.withholdingTaxAmount.toLocaleString()}
                        </p>
                      )}
                    </td>
                    <td className="py-4 px-6">
                      {getMatchBadge(inv.matchStatus)}
                    </td>
                    <td className="py-4 px-6">
                      {getStatusBadge(inv.status)}
                    </td>
                    <td className="py-4 px-6 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {/* View Official GİB HTML Button */}
                        <button
                          onClick={() => setSelectedHtmlInvoice({ id: inv.id, number: inv.invoiceNumber })}
                          className="px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium text-xs shadow-sm flex items-center gap-1 transition"
                          title="View Official GİB e-Fatura Render"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>e-Fatura</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modals */}
      <VendorUblUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
      />

      {selectedHtmlInvoice && (
        <VendorInvoiceHtmlModal
          invoiceId={selectedHtmlInvoice.id}
          invoiceNumber={selectedHtmlInvoice.number}
          isOpen={Boolean(selectedHtmlInvoice)}
          onClose={() => setSelectedHtmlInvoice(null)}
        />
      )}
    </div>
  )
}
export default VendorInvoicesPage
