import { useNavigate } from 'react-router-dom'
import {
  Shield,
  Building2,
  LogOut,
} from 'lucide-react'
import { useAuthStore } from '@/store/useAuthStore'
import { ROUTES } from '@/constants/routes'

export function UserProfileDropdown({
  isOpen,
  onClose,
}: {
  isOpen: boolean
  onClose: () => void
}) {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)

  if (!isOpen) return null

  const handleSignOut = () => {
    onClose()
    logout()
    navigate(ROUTES.login)
  }

  const roleName = user?.roles?.[0]?.replace('ROLE_', '') || 'MEMBER'
  const isRoot = roleName === 'ROOT_USER'

  return (
    <>
      {/* Backdrop */}
      <div className="fixed inset-0 z-40" onClick={onClose} />

      {/* Popover Card */}
      <div className="absolute right-0 top-full mt-2 w-72 bg-white rounded-xl border border-slate-200 shadow-xl z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-150 text-xs">
        {/* User Identity Header */}
        <div className="p-4 bg-slate-50 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-blue-100 text-blue-700 font-bold text-sm flex items-center justify-center border border-blue-200 shrink-0">
              {user?.fullName?.charAt(0) ?? 'U'}
            </div>
            <div className="min-w-0">
              <strong className="text-slate-900 font-bold text-xs block truncate">
                {user?.fullName ?? 'User Profile'}
              </strong>
              <span className="text-[11px] text-slate-500 font-mono block truncate">
                {user?.email}
              </span>
              <div className="mt-1 flex items-center gap-1">
                <span className="px-2 py-0.5 rounded-full text-[9px] font-bold uppercase tracking-wider bg-slate-900 text-white">
                  {roleName}
                </span>
                {isRoot && (
                  <span className="px-1.5 py-0.5 rounded text-[9px] font-semibold bg-amber-100 text-amber-800">
                    Full Admin
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* DOA Authority Box */}
        <div className="px-4 py-2.5 bg-blue-50/50 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-blue-900">
            <Shield className="w-3.5 h-3.5 text-blue-600" />
            <span className="font-semibold text-[11px]">DoA Approval Limit:</span>
          </div>
          <span className="font-mono font-bold text-blue-700 text-[11px]">
            {isRoot ? 'Unlimited (Tier 4)' : 'Defined Matrix'}
          </span>
        </div>

        {/* Quick Action Navigation Links */}
        <div className="p-1.5 divide-y divide-slate-100">
          <div className="py-1">
            <button
              type="button"
              onClick={() => {
                onClose()
                navigate(ROUTES.organization.root)
              }}
              className="w-full px-3 py-2 text-left rounded-lg text-slate-700 hover:bg-slate-50 hover:text-slate-900 flex items-center gap-2.5 transition-colors cursor-pointer"
            >
              <Building2 className="w-4 h-4 text-slate-400" />
              <span>Organization & Legal Entities</span>
            </button>

            <button
              type="button"
              onClick={() => {
                onClose()
                navigate(ROUTES.audit.root)
              }}
              className="w-full px-3 py-2 text-left rounded-lg text-slate-700 hover:bg-slate-50 hover:text-slate-900 flex items-center gap-2.5 transition-colors cursor-pointer"
            >
              <Shield className="w-4 h-4 text-slate-400" />
              <span>Audit Trail & Security Logs</span>
            </button>
          </div>

          <div className="pt-1">
            <button
              type="button"
              onClick={handleSignOut}
              className="w-full px-3 py-2 text-left rounded-lg text-red-600 hover:bg-red-50 flex items-center gap-2.5 transition-colors font-semibold cursor-pointer"
            >
              <LogOut className="w-4 h-4 text-red-500" />
              <span>Sign Out Session</span>
            </button>
          </div>
        </div>
      </div>
    </>
  )
}
