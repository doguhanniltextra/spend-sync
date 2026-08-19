import { useState } from 'react'
import { Bell, Search } from 'lucide-react'
import { useAuthStore } from '@/store/useAuthStore'
import { useTenantStore } from '@/store/useTenantStore'
import { MESSAGES } from '@/constants/messages'
import { NotificationDropdown } from './NotificationDropdown'
import { UserProfileDropdown } from './UserProfileDropdown'

export function Topbar() {
  const user = useAuthStore((s) => s.user)
  const tenant = useTenantStore((s) => s.companyName)

  const [notificationOpen, setNotificationOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between shrink-0 relative z-30">
      <div className="flex items-center gap-4 flex-1 max-w-md">
        <div className="relative w-full">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder={MESSAGES.common.search}
            className="w-full pl-9 pr-4 py-1.5 text-sm bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white transition-all text-slate-900 placeholder:text-slate-400"
          />
        </div>
      </div>

      <div className="flex items-center gap-4">
        {tenant && (
          <span className="text-xs font-medium bg-slate-100 text-slate-600 px-2.5 py-1 rounded-full border border-slate-200">
            {tenant}
          </span>
        )}

        {/* Notifications Popover Trigger */}
        <div className="relative">
          <button
            type="button"
            onClick={() => {
              setProfileOpen(false)
              setNotificationOpen(!notificationOpen)
            }}
            className={`p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors relative cursor-pointer ${
              notificationOpen ? 'bg-slate-100 text-slate-900' : ''
            }`}
            aria-label="Notifications"
          >
            <Bell className="w-4 h-4" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-blue-600 rounded-full ring-2 ring-white" />
          </button>

          <NotificationDropdown
            isOpen={notificationOpen}
            onClose={() => setNotificationOpen(false)}
          />
        </div>

        <div className="h-6 w-px bg-slate-200" />

        {/* User Profile Popover Trigger */}
        <div className="relative">
          <button
            type="button"
            onClick={() => {
              setNotificationOpen(false)
              setProfileOpen(!profileOpen)
            }}
            className={`flex items-center gap-3 p-1.5 -m-1.5 rounded-lg hover:bg-slate-50 transition-colors text-left cursor-pointer ${
              profileOpen ? 'bg-slate-50' : ''
            }`}
          >
            <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 font-bold text-xs flex items-center justify-center border border-blue-200 shrink-0">
              {user?.fullName?.charAt(0) ?? 'U'}
            </div>
            <div className="hidden md:block text-left">
              <p className="text-xs font-semibold text-slate-900 leading-tight">
                {user?.fullName ?? 'User'}
              </p>
              <p className="text-[11px] text-slate-500 font-mono leading-tight">
                {user?.roles?.[0]?.replace('ROLE_', '') ?? 'Member'}
              </p>
            </div>
          </button>

          <UserProfileDropdown
            isOpen={profileOpen}
            onClose={() => setProfileOpen(false)}
          />
        </div>
      </div>
    </header>
  )
}
