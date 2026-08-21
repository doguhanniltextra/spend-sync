import { useState } from 'react'
import { Building2, Landmark, User, Edit3 } from 'lucide-react'
import { useVendorProfile } from '../hooks/useVendorPortalQueries'
import { BankChangeRequestModal } from '../components/profile/BankChangeRequestModal'

export function VendorProfilePage() {
  const { data: profile, isLoading } = useVendorProfile()
  const [isBankModalOpen, setIsBankModalOpen] = useState(false)

  if (isLoading) {
    return (
      <div className="p-12 text-center text-slate-500">
        <div className="w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
        Loading vendor profile...
      </div>
    )
  }

  if (!profile) return null

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-slate-900 tracking-tight">
          Supplier Corporate Profile
        </h2>
        <p className="text-sm text-slate-500 mt-1">
          Registered commercial information, AES-256 encrypted payout accounts, and primary B2B contacts.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Section 1: Corporate Legal Info */}
        <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
            <Building2 className="w-4 h-4 text-teal-600" />
            <span>Commercial Registration</span>
          </div>

          <div className="space-y-3 text-sm">
            <div>
              <p className="text-xs text-slate-400">Legal Company Name</p>
              <p className="font-bold text-slate-900 text-base">{profile.name}</p>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-2">
              <div>
                <p className="text-xs text-slate-400">Tax Number (VKN / TCKN)</p>
                <p className="font-mono font-bold text-slate-800">{profile.taxNumber}</p>
              </div>
              <div>
                <p className="text-xs text-slate-400">Tax Office (Vergi Dairesi)</p>
                <p className="font-semibold text-slate-800">{profile.taxOffice || '—'}</p>
              </div>
            </div>

            <div className="pt-2">
              <p className="text-xs text-slate-400">Registered Corporate Address</p>
              <p className="text-slate-700 text-xs mt-0.5">
                {profile.address ? `${profile.address}, ${profile.city || ''}` : '—'}
              </p>
            </div>

            <div className="pt-2 flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                e-Invoice Registered (GİB)
              </span>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-teal-50 text-teal-700 border border-teal-200">
                Status: {profile.status}
              </span>
            </div>
          </div>
        </div>

        {/* Section 2: Financial Settlement Account */}
        <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                <Landmark className="w-4 h-4 text-indigo-600" />
                <span>Settlement Bank Account</span>
              </div>
              <span className="text-[11px] font-semibold text-teal-700 bg-teal-50 px-2 py-0.5 rounded border border-teal-200">
                AES-256 Encrypted
              </span>
            </div>

            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
              <p className="text-xs text-slate-400">Partner Bank</p>
              <p className="font-bold text-slate-900 text-sm">{profile.bankName || 'Garanti BBVA'}</p>

              <p className="text-xs text-slate-400 pt-2">Active Payout IBAN</p>
              <p className="font-mono font-bold text-teal-800 text-sm tracking-wider">
                {profile.maskedIban}
              </p>
            </div>

            <p className="text-xs text-slate-500 leading-relaxed">
              All automatic bank transfer batches executed by the enterprise Accounts Payable will settle directly into this account.
            </p>
          </div>

          <button
            onClick={() => setIsBankModalOpen(true)}
            className="w-full py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 font-semibold text-xs transition flex items-center justify-center gap-1.5"
          >
            <Edit3 className="w-3.5 h-3.5" />
            <span>Request IBAN Update</span>
          </button>
        </div>
      </div>

      {/* Section 3: Current User Session */}
      <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-4">
        <div className="flex items-center gap-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
          <User className="w-4 h-4 text-teal-600" />
          <span>Active B2B Portal User</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
          <div>
            <p className="text-slate-400">User Full Name</p>
            <p className="font-bold text-slate-800 text-sm mt-0.5">{profile.currentUser.fullName}</p>
          </div>
          <div>
            <p className="text-slate-400">Authorized Email</p>
            <p className="font-medium text-slate-800 mt-0.5">{profile.currentUser.email}</p>
          </div>
          <div>
            <p className="text-slate-400">Access Role</p>
            <p className="font-bold text-teal-700 mt-0.5">{profile.currentUser.role}</p>
          </div>
        </div>
      </div>

      {/* Bank Change Request Modal */}
      <BankChangeRequestModal
        currentMaskedIban={profile.maskedIban}
        isOpen={isBankModalOpen}
        onClose={() => setIsBankModalOpen(false)}
      />
    </div>
  )
}
export default VendorProfilePage
