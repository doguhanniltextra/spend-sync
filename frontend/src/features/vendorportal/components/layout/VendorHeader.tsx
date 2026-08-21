import { Bell, ShieldCheck, UserCircle } from 'lucide-react'
import { useVendorAuthStore } from '../../store/useVendorAuthStore'

export function VendorHeader() {
  const { vendorUser, vendorName } = useVendorAuthStore()

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-8 flex items-center justify-between flex-shrink-0 z-10">
      <div className="flex items-center gap-3">
        <div>
          <h1 className="text-base font-semibold text-slate-800 tracking-tight">
            B2B Supply Chain & Settlement Portal
          </h1>
          {vendorName && (
            <p className="text-[11px] text-slate-400 font-medium">{vendorName}</p>
          )}
        </div>
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/80">
          <ShieldCheck className="w-3.5 h-3.5" />
          ISO 27001 Certified
        </span>
      </div>

      <div className="flex items-center gap-4">
        {/* Notification Bell */}
        <button
          type="button"
          className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition relative"
          title="Notifications"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-teal-500 ring-2 ring-white" />
        </button>

        {/* User Pill */}
        <div className="flex items-center gap-3 pl-4 border-l border-slate-200">
          <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-600 font-semibold text-xs">
            {vendorUser?.fullName
              ?.split(' ')
              .map((n) => n[0])
              .join('')
              .toUpperCase() || <UserCircle className="w-5 h-5" />}
          </div>
          <div className="text-left">
            <p className="text-sm font-semibold text-slate-800 leading-none truncate max-w-[140px]">
              {vendorUser?.fullName || 'Vendor User'}
            </p>
            <p className="text-[11px] font-medium text-teal-700 leading-none mt-1">
              {vendorUser?.role === 'VENDOR_ADMIN' ? 'Supplier Admin' : 'Staff'}
            </p>
          </div>
        </div>
      </div>
    </header>
  )
}
