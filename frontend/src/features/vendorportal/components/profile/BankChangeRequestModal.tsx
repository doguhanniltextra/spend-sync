import { useState } from 'react'
import { Landmark, ShieldAlert, AlertCircle, CheckCircle2 } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { useRequestBankChange } from '../../hooks/useVendorPortalQueries'

interface Props {
  currentMaskedIban: string
  isOpen: boolean
  onClose: () => void
}

export function BankChangeRequestModal({ currentMaskedIban, isOpen, onClose }: Props) {
  const bankChangeMutation = useRequestBankChange()

  const [newBankName, setNewBankName] = useState('Akbank T.A.Ş.')
  const [newIban, setNewIban] = useState('TR')
  const [reason, setReason] = useState('Şirketimiz ana ticari banka hesabının güncellenmesi talebi.')
  const [documentRef, setDocumentRef] = useState('DOC-IMZA-SIRKULERI-2026')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      setError(null)
      await bankChangeMutation.mutateAsync({
        newBankName: newBankName.trim(),
        newIban: newIban.replace(/\s+/g, '').toUpperCase(),
        reason: reason.trim(),
        documentRef: documentRef.trim() || undefined,
      })
      setSuccess(true)
      setTimeout(() => {
        setSuccess(false)
        onClose()
      }, 1500)
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit bank change request.')
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Request Bank IBAN Update (Dual Control)">
      {success ? (
        <div className="p-8 text-center space-y-3">
          <div className="w-12 h-12 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-bold text-slate-900">Request Submitted for CFO Approval</h3>
          <p className="text-xs text-slate-500">
            For security reasons, changes to settlement bank accounts require secondary review by the Buyer CFO.
          </p>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-start gap-2.5">
            <ShieldAlert className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-bold">ISO 27001 Security Policy</p>
              <p className="mt-0.5 leading-relaxed">
                Bank account changes trigger an immutable audit event and require enterprise CFO authorization before becoming active for invoice payouts.
              </p>
            </div>
          </div>

          {error && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-xs">
            <span className="text-slate-500 font-medium">Currently Active IBAN:</span>
            <span className="font-mono font-bold text-slate-800 ml-2">{currentMaskedIban}</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
                New Bank Name *
              </label>
              <input
                type="text"
                required
                value={newBankName}
                onChange={(e) => setNewBankName(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Supporting Document Ref (İmza Sirküleri)
              </label>
              <input
                type="text"
                value={documentRef}
                onChange={(e) => setDocumentRef(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              New Settlement IBAN * (Must match registered corporate VKN)
            </label>
            <input
              type="text"
              required
              value={newIban}
              onChange={(e) => setNewIban(e.target.value)}
              placeholder="TR..."
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 font-mono text-sm text-teal-800 font-bold focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Reason for Bank Change *
            </label>
            <textarea
              rows={2}
              required
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
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
              disabled={bankChangeMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm shadow transition disabled:opacity-50 flex items-center gap-1.5"
            >
              <Landmark className="w-4 h-4" />
              <span>{bankChangeMutation.isPending ? 'Submitting...' : 'Submit Change Request'}</span>
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}
