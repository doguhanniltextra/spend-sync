import { NavLink } from 'react-router-dom'
import {
  Truck,
  Receipt,
  DollarSign,
  Scale,
  Building2,
  LogOut,
  ExternalLink,
} from 'lucide-react'
import { ROUTES } from '@/constants/routes'
import { useVendorAuthStore } from '../../store/useVendorAuthStore'

interface NavItem {
  label: string
  to: string
  icon: React.ComponentType<{ className?: string }>
  badge?: string
}

const NAV_ITEMS: NavItem[] = [
  {
    label: 'Orders & Shipments',
    to: ROUTES.vendor.orders,
    icon: Truck,
  },
  {
    label: 'Invoices & PO-Flip',
    to: ROUTES.vendor.invoices,
    icon: Receipt,
  },
  {
    label: 'Finance & Payments',
    to: ROUTES.vendor.finance,
    icon: DollarSign,
  },
  {
    label: 'BA-BS Reconciliation',
    to: ROUTES.vendor.reconciliation,
    icon: Scale,
  },
  {
    label: 'Company Profile',
    to: ROUTES.vendor.profile,
    icon: Building2,
  },
]

export function VendorSidebar() {
  const { vendorName, logout } = useVendorAuthStore()

  return (
    <aside className="w-64 bg-slate-900 text-slate-200 flex flex-col flex-shrink-0 min-h-screen border-r border-slate-800 shadow-xl">
      {/* Brand Header */}
      <div className="h-16 flex items-center px-6 border-b border-slate-800 bg-slate-950/60">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-400 flex items-center justify-center font-bold text-white shadow-md shadow-teal-500/20">
            SS
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-bold tracking-tight text-white text-base">SpendSync</span>
              <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-teal-500/20 text-teal-300 border border-teal-500/30">
                B2B
              </span>
            </div>
            <p className="text-xs text-slate-400 font-medium">Supplier Portal</p>
          </div>
        </div>
      </div>

      {/* Vendor Entity Badge */}
      <div className="px-4 py-3 border-b border-slate-800/80 bg-slate-900/40">
        <div className="p-2.5 rounded-lg bg-slate-800/60 border border-slate-700/50">
          <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Connected Vendor</p>
          <p className="text-sm font-semibold text-white truncate mt-0.5" title={vendorName || ''}>
            {vendorName || 'Supplier Account'}
          </p>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-4 space-y-1.5 overflow-y-auto">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-teal-500/15 text-teal-300 font-semibold border border-teal-500/30 shadow-sm'
                    : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/60'
                }`
              }
            >
              <div className="flex items-center gap-3">
                <Icon className="w-4 h-4 text-slate-400 group-hover:text-slate-200" />
                <span>{item.label}</span>
              </div>
              {item.badge && (
                <span className="px-2 py-0.5 text-[11px] font-semibold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  {item.badge}
                </span>
              )}
            </NavLink>
          )
        })}
      </nav>

      {/* Buyer Quick Switch & Logout */}
      <div className="p-3 border-t border-slate-800 bg-slate-950/40 space-y-1.5">
        <a
          href="/login"
          className="flex items-center justify-between px-3 py-2 rounded-lg text-xs font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800/50 transition"
        >
          <span className="flex items-center gap-2">
            <ExternalLink className="w-3.5 h-3.5" />
            Switch to Buyer Portal
          </span>
          <span className="text-[10px] text-slate-500">SSO</span>
        </a>
        <button
          onClick={() => {
            logout()
            window.location.href = ROUTES.vendor.login
          }}
          className="w-full flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl text-sm font-medium text-rose-400 hover:text-rose-300 hover:bg-rose-950/30 border border-transparent hover:border-rose-800/40 transition"
        >
          <LogOut className="w-4 h-4" />
          <span>Sign Out</span>
        </button>
      </div>
    </aside>
  )
}
