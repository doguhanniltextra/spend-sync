import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { AppLayout } from '@/components/layout/AppLayout'
import { ROUTES } from '@/constants/routes'

// ─── Lazy-loaded pages ─────────────────────────────────────────────────────────

const LandingPage           = lazy(() => import('@/features/landing/LandingPage'))
const LoginPage             = lazy(() => import('@/features/auth/LoginPage'))
const DashboardPage         = lazy(() => import('@/features/dashboard/DashboardPage'))
const RequisitionListPage   = lazy(() => import('@/features/requisitions/RequisitionListPage'))
const RequisitionCreatePage = lazy(() => import('@/features/requisitions/RequisitionCreatePage'))
const ApprovalQueuePage     = lazy(() => import('@/features/approvals/ApprovalQueuePage'))
const PurchaseOrderListPage = lazy(() => import('@/features/purchasing/PurchaseOrderListPage'))
const PurchaseOrderCreatePage = lazy(() => import('@/features/purchasing/PurchaseOrderCreatePage'))
const VendorListPage        = lazy(() => import('@/features/purchasing/VendorListPage'))
const ReceivingListPage     = lazy(() => import('@/features/receiving/ReceivingListPage'))
const GoodsReceiptCreatePage = lazy(() => import('@/features/receiving/GoodsReceiptCreatePage'))
const ReceivingHistoryPage  = lazy(() => import('@/features/receiving/ReceivingHistoryPage'))
const PaymentConsolePage    = lazy(() => import('@/features/payments/PaymentConsolePage'))
const PaymentRunDetailPage  = lazy(() => import('@/features/payments/PaymentRunDetailPage'))
const OrganizationPage      = lazy(() => import('@/features/organization/OrganizationPage'))
const ComingSoonPage        = lazy(() => import('@/components/feedback/ComingSoonPage'))

// ─── Loading fallback ──────────────────────────────────────────────────────────

function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-[60vh] bg-slate-50">
      <div className="w-8 h-8 border-4 border-slate-900 border-t-transparent rounded-full animate-spin" />
    </div>
  )
}

function SuspenseWrapper({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<PageLoader />}>{children}</Suspense>
}

// ─── Router definition ─────────────────────────────────────────────────────────

export const router = createBrowserRouter([
  // Public Marketing Landing Page (Root)
  {
    path:    ROUTES.home,
    element: <SuspenseWrapper><LandingPage /></SuspenseWrapper>,
  },

  // Public Sign In
  {
    path:    ROUTES.login,
    element: <SuspenseWrapper><LoginPage /></SuspenseWrapper>,
  },

  // Protected application routes — wrapped in AppLayout
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            path:    ROUTES.dashboard,
            element: <SuspenseWrapper><DashboardPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.requisitions.root,
            element: <SuspenseWrapper><RequisitionListPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.requisitions.new,
            element: <SuspenseWrapper><RequisitionCreatePage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.requisitions.detail(':id'),
            element: <SuspenseWrapper><RequisitionListPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.approvals.root,
            element: <SuspenseWrapper><ApprovalQueuePage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.purchasing.root,
            element: <SuspenseWrapper><PurchaseOrderListPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.purchasing.new,
            element: <SuspenseWrapper><PurchaseOrderCreatePage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.purchasing.vendors,
            element: <SuspenseWrapper><VendorListPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.receiving.root,
            element: <SuspenseWrapper><ReceivingListPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.receiving.new,
            element: <SuspenseWrapper><GoodsReceiptCreatePage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.receiving.history,
            element: <SuspenseWrapper><ReceivingHistoryPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.matching.root,
            element: <SuspenseWrapper><ComingSoonPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.payments.root,
            element: <SuspenseWrapper><PaymentConsolePage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.payments.runDetail(':id'),
            element: <SuspenseWrapper><PaymentRunDetailPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.budgets.root,
            element: <SuspenseWrapper><ComingSoonPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.organization.root,
            element: <SuspenseWrapper><OrganizationPage /></SuspenseWrapper>,
          },
          {
            path:    ROUTES.audit.root,
            element: <SuspenseWrapper><ComingSoonPage /></SuspenseWrapper>,
          },
        ],
      },
    ],
  },
])
