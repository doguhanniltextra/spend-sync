import { RotateCcw, DollarSign, PieChart, Clock, CreditCard, AlertCircle, Loader2 } from 'lucide-react'
import { useDashboardData } from './hooks/useDashboardData'
import { useAuthStore } from '@/store/useAuthStore'
import { MetricStatCard } from './components/MetricStatCard'
import { ExecutiveActionCards } from './components/ExecutiveActionCards'
import { CFOExecutiveDeck } from './components/cfo/CFOExecutiveDeck'
import { BudgetUtilizationList } from './components/BudgetUtilizationList'
import { PendingApprovalsList } from './components/PendingApprovalsList'
import { MyRequisitionsList } from './components/MyRequisitionsList'
import { AuditActivityFeed } from './components/AuditActivityFeed'
import { formatCurrency } from '@/utils/currency'
import { PERMISSIONS } from '@/constants/permissions'
import { DASHBOARD_COPY } from './constants/dashboardCopy'

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)
  const hasPermission = useAuthStore((s) => s.hasPermission)

  const {
    budgetSummary,
    pendingApprovals,
    myRequisitions,
    dueInvoices,
    allInvoices,
    auditLogs,
    isLoading,
    refetchAll,
    approvePR,
    rejectPR,
    isApproving,
    isRejecting,
  } = useDashboardData()

  const canReadBudget   = hasPermission(PERMISSIONS.budget.read)
  const canApprovePR    = hasPermission(PERMISSIONS.requisition.approve)
  const canReadOwnPR    = hasPermission(PERMISSIONS.requisition.readOwn)
  const canReadAudit    = hasPermission(PERMISSIONS.audit.read)

  // Calculations for KPI Cards
  const totalAllocated = budgetSummary?.totalAllocated || 0
  const totalCommitted = (budgetSummary?.totalSpent || 0) + (budgetSummary?.totalReserved || 0)
  const utilizationPercent = totalAllocated > 0
    ? Math.min(Math.round((totalCommitted / totalAllocated) * 100), 100)
    : 0

  const pendingApprovalsTotal = pendingApprovals.reduce(
    (acc, p) => acc + (p.totalAmount ?? p.totalEstimatedAmount ?? 0),
    0
  )
  const duePaymentsTotal = dueInvoices.reduce(
    (acc, i) => acc + (i.totalAmount || 0),
    0
  )
  const discrepancyHoldsCount = allInvoices.filter(
    (inv) => inv.status === 'DISCREPANCY_HOLD'
  ).length

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-10">
      {/* Header with Title & Refresh */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {DASHBOARD_COPY.header.title}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {DASHBOARD_COPY.header.subtitle} • Logged in as{' '}
            <strong className="text-slate-700">{user?.fullName}</strong> ({user?.roles?.[0]?.replace('ROLE_', '')})
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={refetchAll}
            disabled={isLoading}
            type="button"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 border border-slate-300 rounded-md transition-colors shadow-2xs disabled:opacity-50"
          >
            {isLoading ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin text-slate-500" />
            ) : (
              <RotateCcw className="w-3.5 h-3.5 text-slate-500" />
            )}
            {DASHBOARD_COPY.header.refreshButton}
          </button>
        </div>
      </div>

      {/* Top 4 KPI Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricStatCard
          label={DASHBOARD_COPY.metrics.totalBudget.label}
          value={formatCurrency(totalAllocated)}
          sublabel={DASHBOARD_COPY.metrics.totalBudget.sublabel}
          icon={DollarSign}
        />

        <MetricStatCard
          label={DASHBOARD_COPY.metrics.budgetUtilization.label}
          value={`%${utilizationPercent}`}
          sublabel={`Spent: ${formatCurrency(budgetSummary?.totalSpent || 0)}`}
          icon={PieChart}
          isWarning={utilizationPercent >= 80}
        />

        <MetricStatCard
          label={DASHBOARD_COPY.metrics.pendingApprovals.label}
          value={String(pendingApprovals.length)}
          sublabel={`Total: ${formatCurrency(pendingApprovalsTotal)}`}
          icon={Clock}
          isWarning={pendingApprovals.length > 0}
        />

        <MetricStatCard
          label={
            discrepancyHoldsCount > 0
              ? DASHBOARD_COPY.metrics.discrepancyHolds.label
              : DASHBOARD_COPY.metrics.duePayments.label
          }
          value={
            discrepancyHoldsCount > 0
              ? `${discrepancyHoldsCount} Holds`
              : formatCurrency(duePaymentsTotal)
          }
          sublabel={
            discrepancyHoldsCount > 0
              ? DASHBOARD_COPY.metrics.discrepancyHolds.sublabel
              : `${dueInvoices.length} invoices ready for batch`
          }
          icon={discrepancyHoldsCount > 0 ? AlertCircle : CreditCard}
          isWarning={discrepancyHoldsCount > 0}
        />
      </div>

      {/* 60-Second Executive Action Items */}
      <ExecutiveActionCards
        pendingApprovals={pendingApprovals}
        dueInvoices={dueInvoices}
        allInvoices={allInvoices}
      />

      {/* CFO / Executive Visual Analytics Deck */}
      {(user?.roles?.includes('ROOT_USER') ||
        user?.roles?.includes('ROLE_ROOT_USER') ||
        user?.email === 'cfo@spendsync.com') && (
        <CFOExecutiveDeck
          pools={budgetSummary?.pools ?? []}
        />
      )}

      {/* Main Content Grid: Ledger & Approvals */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left 2 Cols: Budget Pools or Requisitions */}
        <div className="lg:col-span-2 space-y-6">
          {canReadBudget && (
            <BudgetUtilizationList pools={budgetSummary?.pools ?? []} />
          )}

          {canApprovePR && (
            <PendingApprovalsList
              requests={pendingApprovals}
              onApprove={(id) => approvePR({ id })}
              onReject={(id, reason) => rejectPR({ id, reason })}
              isApproving={isApproving}
              isRejecting={isRejecting}
            />
          )}

          {canReadOwnPR && (
            <MyRequisitionsList requisitions={myRequisitions} />
          )}
        </div>

        {/* Right 1 Col: Audit Activity Feed */}
        <div className="space-y-6">
          {canReadAudit && (
            <AuditActivityFeed logs={auditLogs} />
          )}
        </div>
      </div>
    </div>
  )
}
