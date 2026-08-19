import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Menu, X, ArrowRight, Shield } from 'lucide-react'
import { ROUTES } from '@/constants/routes'
import { LANDING_COPY } from '../constants/landingCopy'

export function LandingNavbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  return (
    <header className="sticky top-0 z-50 bg-white border-b border-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand Logo */}
          <Link to={ROUTES.home} className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-md bg-slate-900 flex items-center justify-center text-white font-bold text-xs tracking-wider">
              <Shield className="w-4 h-4 text-brand-400" />
            </div>
            <div className="flex items-baseline gap-1">
              <span className="text-base font-bold text-slate-900 tracking-tight">
                {LANDING_COPY.nav.brand}
              </span>
              <span className="text-[10px] font-semibold text-slate-500 uppercase tracking-widest">
                ERP
              </span>
            </div>
          </Link>

          {/* Desktop Navigation Links */}
          <nav className="hidden md:flex items-center gap-8">
            <a
              href="#features"
              className="text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
            >
              {LANDING_COPY.nav.features}
            </a>
            <a
              href="#lifecycle"
              className="text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
            >
              {LANDING_COPY.nav.lifecycle}
            </a>
            <a
              href="#security"
              className="text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
            >
              {LANDING_COPY.nav.security}
            </a>
          </nav>

          {/* Action Buttons */}
          <div className="hidden md:flex items-center gap-3">
            <button
              onClick={() => navigate(ROUTES.login)}
              type="button"
              className="px-3.5 py-1.5 text-sm font-medium text-slate-700 hover:text-slate-900 hover:bg-slate-50 rounded-md transition-colors"
            >
              {LANDING_COPY.nav.signIn}
            </button>
            <button
              onClick={() => navigate(ROUTES.login)}
              type="button"
              className="inline-flex items-center gap-1.5 px-4 py-1.5 text-sm font-medium text-white bg-slate-900 hover:bg-slate-800 rounded-md transition-colors shadow-xs"
            >
              {LANDING_COPY.nav.getStarted}
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Mobile menu toggle */}
          <div className="md:hidden">
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 text-slate-600 hover:text-slate-900 rounded-md"
              aria-label="Toggle menu"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden border-b border-slate-200 bg-white px-4 pt-2 pb-4 space-y-2">
          <a
            href="#features"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-md text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            {LANDING_COPY.nav.features}
          </a>
          <a
            href="#lifecycle"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-md text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            {LANDING_COPY.nav.lifecycle}
          </a>
          <a
            href="#security"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-md text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            {LANDING_COPY.nav.security}
          </a>
          <div className="pt-2 border-t border-slate-100 flex flex-col gap-2">
            <button
              onClick={() => navigate(ROUTES.login)}
              type="button"
              className="w-full text-center px-4 py-2 text-sm font-medium text-white bg-slate-900 rounded-md"
            >
              {LANDING_COPY.nav.signIn}
            </button>
          </div>
        </div>
      )}
    </header>
  )
}
