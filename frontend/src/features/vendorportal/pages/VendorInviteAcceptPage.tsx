import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Lock, User, Landmark, ShieldCheck, CheckCircle2, AlertCircle, ArrowRight } from 'lucide-react'
import { vendorPortalApi } from '../services/vendorPortalApi'
import { useVendorAuthStore } from '../store/useVendorAuthStore'
import { ROUTES } from '@/constants/routes'

export function VendorInviteAcceptPage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const setAuth = useVendorAuthStore((s) => s.setAuth)

  const [fullName, setFullName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('+90 ')
  const [taxOffice, setTaxOffice] = useState('')
  const [bankName, setBankName] = useState('Garanti BBVA')
  const [iban, setIban] = useState('TR')
  const [address, setAddress] = useState('')
  const [city, setCity] = useState('Istanbul')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!token) {
      setError('Invalid or missing invitation token.')
      return
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters long.')
      return
    }

    try {
      setLoading(true)
      setError(null)
      const res = await vendorPortalApi.acceptInvite({
        invitationToken: token,
        fullName: fullName.trim(),
        password,
        phoneNumber: phoneNumber.trim(),
        taxOffice: taxOffice.trim(),
        bankName: bankName.trim(),
        iban: iban.replace(/\s+/g, '').toUpperCase(),
        address: address.trim(),
        city: city.trim(),
      })

      setAuth(res)
      setSuccess(true)
      setTimeout(() => {
        navigate(ROUTES.vendor.orders, { replace: true })
      }, 1500)
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to accept invitation. The link may have expired or already been used.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <div className="min-h-screen w-screen flex items-center justify-center bg-slate-950 px-4">
        <div className="max-w-md w-full p-8 rounded-2xl bg-slate-900 border border-slate-800 text-center space-y-4 shadow-2xl">
          <div className="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto ring-8 ring-emerald-500/10">
            <CheckCircle2 className="w-8 h-8" />
          </div>
          <h2 className="text-2xl font-bold text-white">Onboarding Complete!</h2>
          <p className="text-sm text-slate-400">
            Your vendor account and ISO 27001 encrypted bank connection have been configured. Redirecting to your orders dashboard...
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen w-screen bg-slate-950 py-12 px-4 sm:px-6 lg:px-8 text-slate-100 flex flex-col justify-center">
      <div className="max-w-2xl mx-auto w-full space-y-8">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-teal-500/10 text-teal-300 border border-teal-500/30">
            <ShieldCheck className="w-4 h-4 text-teal-400" />
            SpendSync B2B Supplier Self-Onboarding
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">
            Complete Your Supplier Registration
          </h1>
          <p className="text-sm text-slate-400 max-w-lg mx-auto">
            You were invited to connect directly with the enterprise buyer. Configure your credentials and settlement bank account.
          </p>
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-start gap-3">
            <AlertCircle className="w-5 h-5 flex-shrink-0 text-rose-400 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        {/* Form Container */}
        <form onSubmit={handleSubmit} className="p-8 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-2xl space-y-6">
          {/* Section 1: Authorized Contact */}
          <div>
            <h3 className="text-sm font-semibold text-white uppercase tracking-wider flex items-center gap-2 mb-4">
              <User className="w-4 h-4 text-teal-400" />
              1. Authorized Contact Person
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Full Name *</label>
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Ahmet Yılmaz"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Phone Number</label>
                <input
                  type="text"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="+90 555 123 4567"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Section 2: Security Credentials */}
          <div className="pt-4 border-t border-slate-800">
            <h3 className="text-sm font-semibold text-white uppercase tracking-wider flex items-center gap-2 mb-4">
              <Lock className="w-4 h-4 text-teal-400" />
              2. Portal Security Password
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Password *</label>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Confirm Password *</label>
                <input
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Section 3: Bank Account & Payout Info */}
          <div className="pt-4 border-t border-slate-800">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-semibold text-white uppercase tracking-wider flex items-center gap-2">
                <Landmark className="w-4 h-4 text-teal-400" />
                3. Direct Bank Settlement Account
              </h3>
              <span className="text-[11px] text-teal-400 font-medium">AES-256 Encrypted</span>
            </div>
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Bank Name *</label>
                  <input
                    type="text"
                    required
                    value={bankName}
                    onChange={(e) => setBankName(e.target.value)}
                    placeholder="Garanti BBVA / Akbank / Yapı Kredi"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:ring-2 focus:ring-teal-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Tax Office (Vergi Dairesi)</label>
                  <input
                    type="text"
                    value={taxOffice}
                    onChange={(e) => setTaxOffice(e.target.value)}
                    placeholder="Maslak V.D."
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:ring-2 focus:ring-teal-500 focus:outline-none"
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">
                  Settlement IBAN * (Must belong to the registered Tax ID)
                </label>
                <input
                  type="text"
                  required
                  value={iban}
                  onChange={(e) => setIban(e.target.value)}
                  placeholder="TR33 0006 2000 0001 2345 6789 01"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 font-mono text-sm text-teal-300 tracking-wider focus:ring-2 focus:ring-teal-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Company Address</label>
                  <input
                    type="text"
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    placeholder="Maslak Mah. Büyükdere Cad. No:100"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:ring-2 focus:ring-teal-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">City</label>
                  <input
                    type="text"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Istanbul"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:ring-2 focus:ring-teal-500 focus:outline-none"
                  />
                </div>
              </div>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-4 rounded-xl bg-gradient-to-r from-teal-500 to-emerald-600 hover:from-teal-400 hover:to-emerald-500 text-white font-bold text-sm shadow-xl shadow-teal-500/20 flex items-center justify-center gap-2 transition disabled:opacity-50"
          >
            {loading ? (
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <>
                <span>Complete Registration & Access Portal</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  )
}
export default VendorInviteAcceptPage
