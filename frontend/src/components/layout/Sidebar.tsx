import { useState, useEffect } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard,
  ShoppingCart,
  CheckSquare,
  Package,
  Truck,
  FileSearch,
  CreditCard,
  PieChart,
  Building2,
  Shield,
  Boxes,
  LogOut,
  ChevronLeft,
  ChevronRight,
  type LucideIcon,
} from 'lucide-react'
import { clsx } from 'clsx'
import { useAuthStore } from '@/store/useAuthStore'
import { ROUTES } from '@/constants/routes'
import { MESSAGES } from '@/constants/messages'
import { PERMISSIONS } from '@/constants/permissions'
import { TIMING } from '@/constants/timing'

// ─── Nav item definition ───────────────────────────────────────────────────────

interface NavItem {
  label:      string
  to:         string
  icon:       LucideIcon
  permission?: string    // if set, item is hidden unless user has this permission
}

const NAV_ITEMS: NavItem[] = [
  { label: MESSAGES.navigation.dashboard,     to: ROUTES.dashboard,          icon: LayoutDashboard },
  { label: MESSAGES.navigation.requisitions,  to: ROUTES.requisitions.root,  icon: ShoppingCart,  permission: PERMISSIONS.requisition.readOwn },
  { label: MESSAGES.navigation.approvals,     to: ROUTES.approvals.root,     icon: CheckSquare,   permission: PERMISSIONS.requisition.approve },
  { label: MESSAGES.navigation.purchasing,    to: ROUTES.purchasing.root,    icon: Package,       permission: PERMISSIONS.purchasing.readPO },
  { label: MESSAGES.navigation.catalog,       to: ROUTES.catalog.root,       icon: Boxes,         permission: PERMISSIONS.purchasing.manageVendors },
  { label: MESSAGES.navigation.receiving,     to: ROUTES.receiving.root,     icon: Truck,         permission: PERMISSIONS.receiving.read },
  { label: MESSAGES.navigation.matching,      to: ROUTES.matching.root,      icon: FileSearch,    permission: PERMISSIONS.matching.evaluate },
  { label: MESSAGES.navigation.payments,      to: ROUTES.payments.root,      icon: CreditCard,    permission: PERMISSIONS.payment.read },
  { label: MESSAGES.navigation.budgets,       to: ROUTES.budgets.root,       icon: PieChart,      permission: PERMISSIONS.budget.read },
  { label: MESSAGES.navigation.organization,  to: ROUTES.organization.root,  icon: Building2,     permission: PERMISSIONS.organization.read },
  { label: MESSAGES.navigation.audit,         to: ROUTES.audit.root,         icon: Shield,        permission: PERMISSIONS.audit.read },
]

const COLLAPSED_STORAGE_KEY = 'spendsync_sidebar_collapsed'

// ─── Component ─────────────────────────────────────────────────────────────────

export function Sidebar() {
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    return localStorage.getItem(COLLAPSED_STORAGE_KEY) === 'true'
  })

  const hasPermission = useAuthStore((s) => s.hasPermission)
  const user          = useAuthStore((s) => s.user)
  const logout        = useAuthStore((s) => s.logout)
  const navigate      = useNavigate()

  // Persist collapse state
  useEffect(() => {
    localStorage.setItem(COLLAPSED_STORAGE_KEY, String(collapsed))
  }, [collapsed])

  const handleLogout = () => {
    logout()
    navigate(ROUTES.login, { replace: true })
  }

  const visibleItems = NAV_ITEMS.filter(
    (item) => !item.permission || hasPermission(item.permission)
  )

  return (
    <aside
      style={{ transition: `width ${TIMING.animation.normal}ms ease-in-out` }}
      className={clsx(
        'flex flex-col bg-white border-r border-slate-200 shrink-0 overflow-hidden',
        collapsed ? 'w-16' : 'w-60'
      )}
    >
      {/* Logo */}
      <div className="flex items-center h-16 px-4 border-b border-slate-200 shrink-0">
        <div className="flex items-center gap-2 min-w-0">
          <div className="w-8 h-8 rounded-lg bg-slate-900 flex items-center justify-center shrink-0">
            <span className="text-white text-xs font-bold">SS</span>
          </div>
          {!collapsed && (
            <span className="font-semibold text-slate-900 text-sm truncate">SpendSync</span>
          )}
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-4 px-2 space-y-0.5">
        {visibleItems.map((item) => (
          <SidebarNavItem key={item.to} item={item} collapsed={collapsed} />
        ))}
      </nav>

      {/* User info */}
      {!collapsed && user && (
        <div className="px-4 py-3 border-t border-slate-200 bg-slate-50/50">
          <p className="text-xs font-medium text-slate-900 truncate">{user.fullName}</p>
          <p className="text-xs text-slate-500 truncate">{user.email}</p>
        </div>
      )}

      {/* Footer: Logout + Collapse toggle */}
      <div className="border-t border-slate-200 p-2 space-y-0.5 shrink-0">
        <button
          onClick={handleLogout}
          className={clsx(
            'flex items-center gap-3 w-full rounded-md px-2 py-2 text-sm text-slate-600',
            'hover:bg-slate-100 hover:text-slate-900 transition-colors',
            collapsed && 'justify-center'
          )}
          title={collapsed ? MESSAGES.navigation.logout : undefined}
          aria-label={MESSAGES.navigation.logout}
        >
          <LogOut className="w-4 h-4 shrink-0" />
          {!collapsed && <span>{MESSAGES.navigation.logout}</span>}
        </button>

        <button
          onClick={() => setCollapsed((c) => !c)}
          className={clsx(
            'flex items-center gap-3 w-full rounded-md px-2 py-2 text-sm text-slate-500',
            'hover:bg-slate-100 hover:text-slate-800 transition-colors',
            collapsed && 'justify-center'
          )}
          aria-label={collapsed ? MESSAGES.navigation.expand : MESSAGES.navigation.collapse}
        >
          {collapsed
            ? <ChevronRight className="w-4 h-4 shrink-0" />
            : <><ChevronLeft className="w-4 h-4 shrink-0" /><span className="text-xs">Collapse</span></>
          }
        </button>
      </div>
    </aside>
  )
}

// ─── Nav item sub-component ────────────────────────────────────────────────────

function SidebarNavItem({ item, collapsed }: { item: NavItem; collapsed: boolean }) {
  return (
    <NavLink
      to={item.to}
      title={collapsed ? item.label : undefined}
      className={({ isActive }) =>
        clsx(
          'flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium transition-colors',
          collapsed && 'justify-center',
          isActive
            ? 'bg-slate-100 text-slate-900 border-r-2 border-slate-900 font-semibold'
            : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
        )
      }
    >
      <item.icon className="w-4 h-4 shrink-0" />
      {!collapsed && <span className="truncate">{item.label}</span>}
    </NavLink>
  )
}
