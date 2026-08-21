import { useState } from 'react'
import { Scale, ShieldCheck, Lock, AlertCircle } from 'lucide-react'
import { useReconciliationSummary, useApproveReconciliation } from '../hooks/useVendorPortalQueries'

export function VendorReconciliationPage() {
  const currentYear = 2026
  const [selectedMonth, setSelectedMonth] = useState<number>(8) // August
  const { data: rec, isLoading } = useReconciliationSummary(currentYear, selectedMonth)
  const approveMutation = useApproveReconciliation()

  const [notes, setNotes] = useState('Mutabakat onaylanmıştır.')
  const [error, setError] = useState<string | null>(null)

  const handleApprove = async () => {
    try {
      setError(null)
      await approveMutation.mutateAsync({
        periodYear: currentYear,
        periodMonth: selectedMonth,
        vendorNotes: notes.trim() || undefined,
      })
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to approve BA-BS reconciliation.')
    }
  }

  const months = [
    { value: 1, name: 'Ocak (January)' },
    { value: 2, name: 'Şubat (February)' },
    { value: 3, name: 'Mart (March)' },
    { value: 4, name: 'Nisan (April)' },
    { value: 5, name: 'Mayıs (May)' },
    { value: 6, name: 'Haziran (June)' },
    { value: 7, name: 'Temmuz (July)' },
    { value: 8, name: 'Ağustos (August)' },
    { value: 9, name: 'Eylül (September)' },
    { value: 10, name: 'Ekim (October)' },
    { value: 11, name: 'Kasım (November)' },
    { value: 12, name: 'Aralık (December)' },
  ]

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-slate-900 tracking-tight">
          Aylık BA-BS & Cari e-Mutabakat Paneli
        </h2>
        <p className="text-sm text-slate-500 mt-1">
          Perform monthly corporate tax and accounts ledger reconciliation sealed with a SHA-256 tamper-evident cryptographic signature.
        </p>
      </div>

      {/* Month Selector */}
      <div className="flex items-center gap-3 p-4 rounded-2xl bg-white border border-slate-200 shadow-sm">
        <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
          Reconciliation Period:
        </label>
        <select
          value={selectedMonth}
          onChange={(e) => setSelectedMonth(Number(e.target.value))}
          className="bg-slate-50 border border-slate-300 rounded-xl px-4 py-2 text-sm font-semibold text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
        >
          {months.map((m) => (
            <option key={m.value} value={m.value}>
              {currentYear} - {m.name}
            </option>
          ))}
        </select>
      </div>

      {/* Main Reconciliation Card */}
      {isLoading ? (
        <div className="p-12 text-center text-slate-500 bg-white rounded-2xl border border-slate-200">
          <div className="w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          Fetching BA-BS reconciliation data...
        </div>
      ) : rec ? (
        <div className="p-8 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-6">
          {error && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          {/* Status Header */}
          <div className="flex items-center justify-between pb-6 border-b border-slate-100">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-teal-50 text-teal-700 flex items-center justify-center">
                <Scale className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Form BS (Bildirim Satış) - {months.find((m) => m.value === rec.periodMonth)?.name} {rec.periodYear}
                </h3>
                <p className="text-xs text-slate-500">Corporate Counterparty: {rec.vendorName}</p>
              </div>
            </div>

            <span
              className={`px-3 py-1 rounded-full text-xs font-bold ${
                rec.status === 'APPROVED'
                  ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                  : 'bg-amber-100 text-amber-800 border border-amber-300'
              }`}
            >
              {rec.status === 'APPROVED' ? 'Mutabık Kalındı (Approved)' : 'Onay Bekliyor (Pending)'}
            </span>
          </div>

          {/* Summary Figures */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200">
              <p className="text-xs font-medium text-slate-400">Total Invoice Count (Fatura Adedi)</p>
              <p className="text-2xl font-black text-slate-900 mt-1">
                {rec.invoiceCount} Adet
              </p>
            </div>

            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200">
              <p className="text-xs font-medium text-slate-400">Gross Reconciled Amount (KDV Hariç Toplam)</p>
              <p className="text-2xl font-black text-teal-700 mt-1">
                {rec.totalAmount.toLocaleString()} {rec.currency}
              </p>
            </div>
          </div>

          {/* Sealed Cryptographic Hash Display */}
          {rec.status === 'APPROVED' && rec.signedChecksum && (
            <div className="p-4 rounded-xl bg-slate-900 text-slate-200 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="flex items-center gap-1.5 text-teal-400 font-semibold">
                  <ShieldCheck className="w-4 h-4" />
                  SHA-256 Cryptographic Digital Seal Verified
                </span>
                <span className="text-[11px] text-slate-400 font-mono">
                  Approved At: {rec.vendorApprovedAt}
                </span>
              </div>
              <p className="font-mono text-xs text-teal-300 break-all bg-slate-950 p-2.5 rounded-lg border border-slate-800">
                {rec.signedChecksum}
              </p>
            </div>
          )}

          {/* Action Approval Section */}
          {rec.status !== 'APPROVED' && (
            <div className="pt-4 border-t border-slate-100 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                  Approval Notes / Legal Declaration
                </label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Yukarıda belirtilen fatura adedi ve tutar şirketimiz yasal muhasebe kayıtlarıyla mutabıktır."
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end">
                <button
                  type="button"
                  disabled={approveMutation.isPending}
                  onClick={handleApprove}
                  className="px-6 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-lg shadow-emerald-600/20 flex items-center gap-2 transition disabled:opacity-50"
                >
                  <Lock className="w-4 h-4" />
                  <span>
                    {approveMutation.isPending ? 'Signing Seal...' : 'Mutabıkım — Sign with Digital Seal'}
                  </span>
                </button>
              </div>
            </div>
          )}
        </div>
      ) : null}
    </div>
  )
}
export default VendorReconciliationPage
