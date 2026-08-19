import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Bell,
  CheckCircle2,
  AlertTriangle,
  FileCheck,
  CreditCard,
  Truck,
  Check,
  ExternalLink,
} from 'lucide-react'
import { useAuthStore } from '@/store/useAuthStore'
import { ROUTES } from '@/constants/routes'

interface NotificationItem {
  id:        string
  title:     string
  message:   string
  timestamp: string
  type:      'APPROVAL' | 'PAYMENT' | 'RECEIVING' | 'BUDGET' | 'SYSTEM'
  read:      boolean
  link:      string
}

const INITIAL_NOTIFICATIONS: Record<string, NotificationItem[]> = {
  ROOT_USER: [
    {
      id:        'n1',
      title:     'Settlement Run Dispatched',
      message:   'Payment batch PAY-2026-00001 released via ISO 20022 PAIN.001.',
      timestamp: '2 mins ago',
      type:      'PAYMENT',
      read:      false,
      link:      ROUTES.payments.root,
    },
    {
      id:        'n2',
      title:     'Goods Receipt Completed',
      message:   'Dock inspection GR-2026-00001 posted against PO-2026-00003.',
      timestamp: '10 mins ago',
      type:      'RECEIVING',
      read:      false,
      link:      ROUTES.receiving.root,
    },
    {
      id:        'n3',
      title:     'Budget Threshold Notice',
      message:   'Cloud & Infrastructure Operations (CC-300) reached 66% utilization.',
      timestamp: '1 hour ago',
      type:      'BUDGET',
      read:      true,
      link:      ROUTES.budgets.root,
    },
  ],
  APPROVER: [
    {
      id:        'n4',
      title:     'Purchase Requisition Assigned',
      message:   'PR-20260819-0005 requires your managerial DOA authorization.',
      timestamp: 'Just now',
      type:      'APPROVAL',
      read:      false,
      link:      ROUTES.approvals.root,
    },
    {
      id:        'n5',
      title:     'Budget Pool Update',
      message:   'Core Engineering & R&D budget pool allocated 12.000.000 TRY for FY2026.',
      timestamp: '3 hours ago',
      type:      'BUDGET',
      read:      true,
      link:      ROUTES.budgets.root,
    },
  ],
  DEFAULT: [
    {
      id:        'n6',
      title:     'Requisition Status Update',
      message:   'Your purchase requisition was approved and forwarded to purchasing.',
      timestamp: '15 mins ago',
      type:      'APPROVAL',
      read:      false,
      link:      ROUTES.requisitions.root,
    },
  ],
}

export function NotificationDropdown({
  isOpen,
  onClose,
}: {
  isOpen: boolean
  onClose: () => void
}) {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const roleKey = user?.roles?.[0]?.replace('ROLE_', '') || 'DEFAULT'

  const [notifications, setNotifications] = useState<NotificationItem[]>(
    () => INITIAL_NOTIFICATIONS[roleKey] ?? INITIAL_NOTIFICATIONS.DEFAULT
  )

  const unreadCount = notifications.filter((n) => !n.read).length

  const markAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
  }

  const handleItemClick = (n: NotificationItem) => {
    setNotifications((prev) =>
      prev.map((item) => (item.id === n.id ? { ...item, read: true } : item))
    )
    onClose()
    navigate(n.link)
  }

  if (!isOpen) return null

  const getIcon = (type: NotificationItem['type']) => {
    switch (type) {
      case 'PAYMENT':
        return <CreditCard className="w-4 h-4 text-purple-600" />
      case 'RECEIVING':
        return <Truck className="w-4 h-4 text-emerald-600" />
      case 'BUDGET':
        return <AlertTriangle className="w-4 h-4 text-amber-600" />
      case 'APPROVAL':
        return <FileCheck className="w-4 h-4 text-blue-600" />
      default:
        return <CheckCircle2 className="w-4 h-4 text-slate-600" />
    }
  }

  return (
    <>
      {/* Backdrop */}
      <div className="fixed inset-0 z-40" onClick={onClose} />

      {/* Popover Modal Card */}
      <div className="absolute right-0 top-full mt-2 w-84 sm:w-96 bg-white rounded-xl border border-slate-200 shadow-xl z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 bg-slate-50/70">
          <div className="flex items-center gap-2">
            <Bell className="w-4 h-4 text-slate-700" />
            <h4 className="text-xs font-bold text-slate-900">Notifications</h4>
            {unreadCount > 0 && (
              <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-100 text-blue-800">
                {unreadCount} New
              </span>
            )}
          </div>

          {unreadCount > 0 && (
            <button
              type="button"
              onClick={markAllAsRead}
              className="text-[11px] font-medium text-blue-600 hover:text-blue-700 hover:underline flex items-center gap-1 cursor-pointer"
            >
              <Check className="w-3 h-3" /> Mark all as read
            </button>
          )}
        </div>

        {/* Notifications List */}
        <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
          {notifications.length === 0 ? (
            <div className="p-6 text-center text-xs text-slate-400">
              No notifications at this time.
            </div>
          ) : (
            notifications.map((n) => (
              <div
                key={n.id}
                onClick={() => handleItemClick(n)}
                className={`p-3.5 flex items-start gap-3 hover:bg-slate-50 transition-colors cursor-pointer text-left ${
                  !n.read ? 'bg-blue-50/30' : 'bg-white'
                }`}
              >
                <div className="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center shrink-0 mt-0.5">
                  {getIcon(n.type)}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-1">
                    <strong className="text-xs font-semibold text-slate-900 truncate">
                      {n.title}
                    </strong>
                    <span className="text-[10px] text-slate-400 font-mono shrink-0">
                      {n.timestamp}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-600 line-clamp-2 mt-0.5">
                    {n.message}
                  </p>
                </div>

                {!n.read && (
                  <span className="w-2 h-2 rounded-full bg-blue-600 shrink-0 mt-2" />
                )}
              </div>
            ))
          )}
        </div>

        {/* Footer */}
        <div className="p-2 border-t border-slate-100 bg-slate-50 text-center">
          <button
            type="button"
            onClick={() => {
              onClose()
              navigate(ROUTES.audit.root)
            }}
            className="text-[11px] font-semibold text-slate-700 hover:text-slate-900 inline-flex items-center gap-1.5 py-1 px-3 rounded hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <span>View Full Audit & Event Timeline</span>
            <ExternalLink className="w-3 h-3 text-slate-400" />
          </button>
        </div>
      </div>
    </>
  )
}
