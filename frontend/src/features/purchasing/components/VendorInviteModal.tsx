import { useState } from 'react'
import { Send, CheckCircle2, AlertCircle, Copy } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { vendorPortalApi } from '@/features/vendorportal/services/vendorPortalApi'

interface Props {
  isOpen: boolean
  onClose: () => void
}

export function VendorInviteModal({ isOpen, onClose }: Props) {
  const [companyName, setCompanyName] = useState('')
  const [taxNumber, setTaxNumber] = useState('')
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [inviteResult, setInviteResult] = useState<{ token: string; email: string } | null>(null)
  const [copied, setCopied] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      setLoading(true)
      setError(null)
      const res = await vendorPortalApi.inviteVendor({
        companyName: companyName.trim(),
        taxNumber: taxNumber.trim(),
        email: email.trim(),
      })
      setInviteResult({
        token: res.invitationToken,
        email: res.email,
      })
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate vendor invitation.')
    } finally {
      setLoading(false)
    }
  }

  const magicLink = inviteResult ? `${window.location.origin}/vendor/invite/${inviteResult.token}` : ''

  const handleCopyLink = () => {
    navigator.clipboard.writeText(magicLink)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleReset = () => {
    setInviteResult(null)
    setCompanyName('')
    setTaxNumber('')
    setEmail('')
    onClose()
  }

  return (
    <Modal isOpen={isOpen} onClose={handleReset} title="Invite Supplier to B2B Portal (Magic Link)">
      {inviteResult ? (
        <div className="space-y-6">
          <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-center space-y-2">
            <div className="w-12 h-12 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900">Invitation Generated & Dispatched!</h3>
            <p className="text-xs text-slate-600">
              An invitation email with self-onboarding instructions has been sent to{' '}
              <strong className="font-semibold text-slate-800">{inviteResult.email}</strong>.
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5 flex items-center justify-between">
              <span>Magic Onboarding Link</span>
              {copied && <span className="text-emerald-600 font-bold">Copied to clipboard!</span>}
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={magicLink}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-xs font-mono text-slate-700 focus:outline-none"
              />
              <button
                type="button"
                onClick={handleCopyLink}
                className="px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold shadow flex items-center gap-1.5 transition flex-shrink-0"
              >
                <Copy className="w-3.5 h-3.5" />
                <span>Copy</span>
              </button>
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="button"
              onClick={handleReset}
              className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs shadow transition"
            >
              Done
            </button>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-5">
          <p className="text-xs text-slate-500">
            Invite a new supplier to self-onboard. They will securely establish their password and ISO 27001 AES-256 encrypted payout bank IBAN.
          </p>

          {error && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Supplier Legal Company Name *
            </label>
            <input
              type="text"
              required
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              placeholder="Delta Bilişim Sistemleri A.Ş."
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Tax Number (VKN / TCKN) *
              </label>
              <input
                type="text"
                required
                maxLength={11}
                value={taxNumber}
                onChange={(e) => setTaxNumber(e.target.value)}
                placeholder="1122334455"
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 font-mono text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Recipient Email *
              </label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="contact@vendor.com"
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-200">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-800 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm shadow transition disabled:opacity-50 flex items-center gap-1.5"
            >
              <Send className="w-4 h-4" />
              <span>{loading ? 'Sending...' : 'Send Magic Link Invitation'}</span>
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}
