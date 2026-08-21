import { Download, ArrowDownRight, ArrowUpRight } from 'lucide-react'
import type { StatementOfAccountsResponse } from '../../types/vendorPortal.types'

interface Props {
  soa: StatementOfAccountsResponse
}

export function StatementOfAccountsTable({ soa }: Props) {
  const handleExportCsv = () => {
    const headers = ['Date', 'Type', 'Document Number', 'Description', 'Debit (Paid)', 'Credit (Invoiced)', 'Running Balance', 'Currency']
    const rows = soa.items.map((it) => [
      it.date,
      it.type,
      it.documentNumber,
      `"${it.description.replace(/"/g, '""')}"`,
      it.debitAmount,
      it.creditAmount,
      it.runningBalance,
      it.currency,
    ])

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\n')
    const encodedUri = encodeURI(csvContent)
    const link = document.createElement('a')
    link.setAttribute('href', encodedUri)
    link.setAttribute('download', `Statement_Of_Accounts_${soa.vendorName.replace(/\s+/g, '_')}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden space-y-4">
      {/* Top Ledger Summary Cards */}
      <div className="p-6 border-b border-slate-200 bg-slate-50/50">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Cari Hesap Ekstresi (Statement of Accounts)
            </h3>
            <p className="text-xs text-slate-500">
              Period: {soa.startDate} to {soa.endDate} • Currency: {soa.currency}
            </p>
          </div>

          <button
            onClick={handleExportCsv}
            className="self-start sm:self-auto px-4 py-2 rounded-xl bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold text-xs shadow-sm flex items-center gap-1.5 transition"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export SOA (CSV)</span>
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 mt-6 text-xs">
          <div className="p-3.5 rounded-xl bg-white border border-slate-200">
            <p className="text-slate-400 font-medium">Opening Balance</p>
            <p className="text-base font-bold text-slate-900 mt-0.5">
              {soa.openingBalance.toLocaleString()} {soa.currency}
            </p>
          </div>
          <div className="p-3.5 rounded-xl bg-white border border-slate-200">
            <p className="text-slate-400 font-medium">Total Invoiced (Alacak)</p>
            <p className="text-base font-bold text-slate-900 mt-0.5">
              +{soa.totalInvoiced.toLocaleString()} {soa.currency}
            </p>
          </div>
          <div className="p-3.5 rounded-xl bg-white border border-slate-200">
            <p className="text-slate-400 font-medium">Total Paid (Borç)</p>
            <p className="text-base font-bold text-emerald-700 mt-0.5">
              -{soa.totalPaid.toLocaleString()} {soa.currency}
            </p>
          </div>
          <div className="p-3.5 rounded-xl bg-teal-50 border border-teal-200">
            <p className="text-teal-700 font-semibold">Open Balance (Bakiye)</p>
            <p className="text-lg font-black text-teal-900 mt-0.5">
              {soa.closingBalance.toLocaleString()} {soa.currency}
            </p>
          </div>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-200 text-slate-500 font-semibold text-xs uppercase tracking-wider bg-slate-50/30">
              <th className="py-3 px-6">Date</th>
              <th className="py-3 px-6">Type</th>
              <th className="py-3 px-6">Document Ref</th>
              <th className="py-3 px-6">Description</th>
              <th className="py-3 px-6 text-right">Debit (Borç)</th>
              <th className="py-3 px-6 text-right">Credit (Alacak)</th>
              <th className="py-3 px-6 text-right">Running Balance</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {soa.items.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-slate-400 text-xs">
                  No statement ledger transactions found for this period.
                </td>
              </tr>
            ) : (
              soa.items.map((item, idx) => (
                <tr key={idx} className="hover:bg-slate-50/60 transition">
                  <td className="py-3.5 px-6 text-xs text-slate-600 font-mono">
                    {item.date}
                  </td>
                  <td className="py-3.5 px-6 text-xs">
                    {item.type === 'PAYMENT' ? (
                      <span className="inline-flex items-center gap-1 font-bold text-emerald-700">
                        <ArrowDownRight className="w-3.5 h-3.5" /> Payout
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 font-bold text-indigo-700">
                        <ArrowUpRight className="w-3.5 h-3.5" /> Invoice
                      </span>
                    )}
                  </td>
                  <td className="py-3.5 px-6 font-mono font-bold text-slate-800 text-xs">
                    {item.documentNumber}
                  </td>
                  <td className="py-3.5 px-6 text-xs text-slate-600">
                    {item.description}
                  </td>
                  <td className="py-3.5 px-6 text-right text-xs text-emerald-700 font-semibold">
                    {item.debitAmount > 0 ? `-${item.debitAmount.toLocaleString()} ${soa.currency}` : '—'}
                  </td>
                  <td className="py-3.5 px-6 text-right text-xs text-slate-900 font-semibold">
                    {item.creditAmount > 0 ? `+${item.creditAmount.toLocaleString()} ${soa.currency}` : '—'}
                  </td>
                  <td className="py-3.5 px-6 text-right font-bold text-slate-900 text-xs">
                    {item.runningBalance.toLocaleString()} {soa.currency}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
