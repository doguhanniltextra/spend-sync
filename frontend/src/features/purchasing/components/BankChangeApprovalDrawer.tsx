import { useState } from 'react'
import { ShieldAlert, CheckCircle2, XCircle, AlertCircle, FileText } from 'lucide-react'
import { Drawer } from '@/components/ui/Drawer'
import { useBuyerBankChangeRequests } from '@/features/vendorportal/hooks/useVendorPortalQueries'
import { vendorPortalApi } from '@/features/vendorportal/services/vendorPortalApi'
import { useQueryClient } from '@tanstack/react-query'

interface Props {
  isOpen: boolean
  onClose: () => void
}

export function BankChangeApprovalDrawer({ isOpen, onClose }: Props) {
  const qc = useQueryClient()
  const { data: requests = [], isLoading } = useBuyerBankChangeRequests()

  const [processingId, setProcessingId] = useState<string | null>(null)
  const [actionNotes, setActionNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  const handleApprove = async (id: string) => {
    try {
      setProcessingId(id)
      setError(null)
      await vendorPortalApi.approveBankChange(id, actionNotes.trim() || undefined)
      qc.invalidateQueries({ queryKey: ['buyer', 'bank-changes'] })
      setActionNotes('')
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to approve bank change request.')
    } finally {
      setProcessingId(null)
    }
  }

  const handleReject = async (id: string) => {
    try {
      setProcessingId(id)
      setError(null)
      await vendorPortalApi.rejectBankChange(id, actionNotes.trim() || undefined)
      qc.invalidateQueries({ queryKey: ['buyer', 'bank-changes'] })
      setActionNotes('')
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reject bank change request.')
    } finally {
      setProcessingId(null)
    }
  }

  const pendingRequests = requests.filter((r) => r.status === 'PENDING')

  return (
    <Drawer isOpen={isOpen} onClose={onClose} title="Dual-Control Bank IBAN Change Approvals">
      <div className="space-y-6 p-1">
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-start gap-2.5">
          <ShieldAlert className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-bold">CFO Four-Eyes Principle</p>
            <p className="mt-0.5 leading-relaxed">
              Verify the supporting signature circular (İmza Sirküleri) before approving IBAN updates. Approved IBANs immediately become active for automatic ISO 20022 bank settlement batches.
            </p>
          </div>
        </div>

        {error && (
          <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4" />
            <span>{error}</span>
          </div>
        )}

        {isLoading ? (
          <div className="p-8 text-center text-slate-400 text-xs">
            <div className="w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
            Loading bank change requests...
          </div>
        ) : pendingRequests.length === 0 ? (
          <div className="p-8 text-center bg-slate-50 border border-slate-200 rounded-2xl text-slate-500 space-y-2">
            <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto" />
            <p className="font-semibold text-sm text-slate-700">No Pending Bank Change Requests</p>
            <p className="text-xs text-slate-400">All supplier settlement accounts are verified and up to date.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {pendingRequests.map((req) => (
              <div
                key={req.id}
                className="p-5 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-4 text-xs"
              >
                <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                  <div>
                    <h4 className="font-bold text-slate-900 text-sm">{req.vendorName}</h4>
                    <p className="text-slate-400">Requested by: {req.requestedByUserName}</p>
                  </div>
                  <span className="px-2.5 py-0.5 rounded-full font-bold bg-amber-100 text-amber-800">
                    PENDING REVIEW
                  </span>
                </div>

                {/* Old vs New IBAN Comparison */}
                <div className="grid grid-cols-2 gap-3 p-3 rounded-xl bg-slate-50 border border-slate-200 font-mono">
                  <div>
                    <p className="text-[10px] text-slate-400 font-sans uppercase">Previous IBAN</p>
                    <p className="font-bold text-slate-600 text-xs mt-0.5">{req.oldIbanMasked || 'None'}</p>
                  </div>
                  <div>
                    <p className="text-[10px] text-teal-600 font-sans uppercase">New Target IBAN</p>
                    <p className="font-bold text-teal-800 text-xs mt-0.5">{req.newIbanMasked}</p>
                    <p className="text-[10px] text-slate-500 font-sans">{req.newBankName}</p>
                  </div>
                </div>

                <div className="space-y-1">
                  <p className="text-slate-500">
                    <strong className="text-slate-700">Reason:</strong> {req.reason}
                  </p>
                  {req.documentRef && (
                    <p className="text-slate-500 flex items-center gap-1 font-mono text-[11px]">
                      <FileText className="w-3.5 h-3.5 text-slate-400" />
                      Document Ref: {req.documentRef}
                    </p>
                  )}
                </div>

                <div>
                  <input
                    type="text"
                    value={actionNotes}
                    onChange={(e) => setActionNotes(e.target.value)}
                    placeholder="Audit decision note (Optional)..."
                    className="w-full bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-xs text-slate-800 focus:outline-none"
                  />
                </div>

                {/* Action Buttons */}
                <div className="flex items-center justify-end gap-2 pt-2">
                  <button
                    type="button"
                    disabled={processingId === req.id}
                    onClick={() => handleReject(req.id)}
                    className="px-3.5 py-1.5 rounded-lg border border-rose-200 hover:bg-rose-50 text-rose-700 font-semibold text-xs transition disabled:opacity-50 flex items-center gap-1"
                  >
                    <XCircle className="w-3.5 h-3.5" />
                    <span>Reject</span>
                  </button>
                  <button
                    type="button"
                    disabled={processingId === req.id}
                    onClick={() => handleApprove(req.id)}
                    className="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs shadow transition disabled:opacity-50 flex items-center gap-1"
                  >
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Approve IBAN</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </Drawer>
  )
}
