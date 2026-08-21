import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Building2, Lock, Mail, ArrowRight, ShieldCheck, AlertCircle } from 'lucide-react'
import { vendorPortalApi } from '../services/vendorPortalApi'
import { useVendorAuthStore } from '../store/useVendorAuthStore'
import { ROUTES } from '@/constants/routes'

export function VendorLoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setAuth = useVendorAuthStore((s) => s.setAuth)

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || ROUTES.vendor.orders

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email || !password) {
      setError('Please enter your email and password.')
      return
    }

    try {
      setLoading(true)
      setError(null)
      const res = await vendorPortalApi.login(email.trim(), password)
      setAuth(res)
      navigate(from, { replace: true })
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Invalid credentials or inactive account.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen w-screen flex bg-slate-950 text-slate-100 antialiased selection:bg-teal-500 selection:text-white">
      {/* Left hero banner */}
      <div className="hidden lg:flex lg:w-1/2 relative flex-col justify-between p-12 overflow-hidden bg-gradient-to-br from-slate-900 via-slate-950 to-slate-900 border-r border-slate-800">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-teal-900/20 via-transparent to-transparent pointer-events-none" />

        <div className="relative z-10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center font-bold text-white shadow-lg shadow-teal-500/25">
              SS
            </div>
            <div>
              <span className="font-bold tracking-tight text-white text-lg">SpendSync</span>
              <span className="ml-2 px-2 py-0.5 rounded text-[11px] font-semibold bg-teal-500/20 text-teal-300 border border-teal-500/30">
                Supplier Hub
              </span>
            </div>
          </div>
        </div>

        <div className="relative z-10 space-y-6 max-w-lg">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold bg-teal-500/10 text-teal-300 border border-teal-500/30">
            <ShieldCheck className="w-4 h-4 text-teal-400" />
            ISO 27001 AES-256 P2P Settlement
          </div>
          <h2 className="text-3xl font-extrabold text-white leading-tight">
            Direct B2B Order Confirmation, PO-Flip Invoicing & Dynamic Early Payouts.
          </h2>
          <p className="text-sm text-slate-400 leading-relaxed">
            Eliminate manual invoice emails, PDFs, and delayed payments. Receive official POs, dispatch ASN e-waybills, and track 3-Way touchless match in real time.
          </p>

          <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-800/80 text-xs">
            <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800">
              <p className="font-semibold text-white">⚡ 1-Click PO-Flip</p>
              <p className="text-slate-400 mt-1">Generate compliant GİB e-Invoices with withholding tax.</p>
            </div>
            <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800">
              <p className="font-semibold text-white">💰 %2 T+3 Early Cash</p>
              <p className="text-slate-400 mt-1">Accelerate cash flow on approved invoices dynamically.</p>
            </div>
          </div>
        </div>

        <div className="relative z-10 text-xs text-slate-500">
          © {new Date().getFullYear()} SpendSync Enterprise Inc. All rights reserved.
        </div>
      </div>

      {/* Right Login Form */}
      <div className="flex-1 flex flex-col justify-center px-6 sm:px-12 lg:px-16 py-12 bg-slate-900/40">
        <div className="w-full max-w-md mx-auto space-y-8">
          <div>
            <div className="lg:hidden flex items-center gap-2 mb-6">
              <div className="w-8 h-8 rounded-xl bg-teal-500 flex items-center justify-center font-bold text-white text-sm">
                SS
              </div>
              <span className="font-bold text-white text-base">SpendSync Supplier Portal</span>
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-white">
              Sign in to Supplier Portal
            </h1>
            <p className="text-sm text-slate-400 mt-2">
              Enter your corporate credentials to access active purchase orders and invoices.
            </p>
          </div>

          {error && (
            <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-start gap-3">
              <AlertCircle className="w-5 h-5 flex-shrink-0 text-rose-400 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-5">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2">
                Corporate Email Address
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-500 absolute left-3.5 top-3.5 pointer-events-none" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="accounting@vendorcorp.com"
                  required
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 pl-10 text-sm text-white placeholder-slate-600 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-3.5 pointer-events-none" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  required
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 pl-10 text-sm text-white placeholder-slate-600 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-teal-500 to-emerald-600 hover:from-teal-400 hover:to-emerald-500 text-white font-semibold text-sm shadow-lg shadow-teal-500/20 flex items-center justify-center gap-2 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <span>Sign In to Supplier Portal</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          <div className="pt-6 border-t border-slate-800 text-center space-y-3">
            <p className="text-xs text-slate-400">
              Invited by a buyer? Check your email for the Magic Link onboarding invitation.
            </p>
            <a
              href="/login"
              className="inline-flex items-center gap-1.5 text-xs font-medium text-teal-400 hover:text-teal-300 transition"
            >
              <Building2 className="w-3.5 h-3.5" />
              Are you a buyer enterprise user? Sign in here →
            </a>
          </div>
        </div>
      </div>
    </div>
  )
}
export default VendorLoginPage
