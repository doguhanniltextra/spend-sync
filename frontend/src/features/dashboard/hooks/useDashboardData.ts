import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { dashboardApi } from '../services/dashboardApi'
import { useAuthStore } from '@/store/useAuthStore'
import { PERMISSIONS } from '@/constants/permissions'
import { TIMING } from '@/constants/timing'

export function useDashboardData() {
  const queryClient = useQueryClient()
  const hasPermission = useAuthStore((s) => s.hasPermission)

  const canReadBudget    = hasPermission(PERMISSIONS.budget.read)
  const canApprovePR     = hasPermission(PERMISSIONS.requisition.approve)
  const canReadOwnPR     = hasPermission(PERMISSIONS.requisition.readOwn)
  const canReadPayment   = hasPermission(PERMISSIONS.payment.read)
  const canReadMatching  = hasPermission(PERMISSIONS.matching.evaluate)
  const canReadAudit     = hasPermission(PERMISSIONS.audit.read)

  // 1. Budget Summary Query
  const budgetQuery = useQuery({
    queryKey: ['dashboard', 'budgetSummary'],
    queryFn: () => dashboardApi.getBudgetSummary(2026),
    enabled: canReadBudget,
    refetchInterval: TIMING.query.dashboardRefresh,
  })

  // 2. Pending Approvals Query
  const pendingApprovalsQuery = useQuery({
    queryKey: ['dashboard', 'pendingApprovals'],
    queryFn: () => dashboardApi.getPendingApprovals(),
    enabled: canApprovePR,
    refetchInterval: TIMING.query.dashboardRefresh,
  })

  // 3. My Requisitions Query
  const myRequisitionsQuery = useQuery({
    queryKey: ['dashboard', 'myRequisitions'],
    queryFn: () => dashboardApi.getMyRequisitions(),
    enabled: canReadOwnPR,
    refetchInterval: TIMING.query.dashboardRefresh,
  })

  // 4. Due Invoices Query
  const dueInvoicesQuery = useQuery({
    queryKey: ['dashboard', 'dueInvoices'],
    queryFn: () => dashboardApi.getDueInvoices(),
    enabled: canReadPayment,
    refetchInterval: TIMING.query.dashboardRefresh,
  })

  // 5. Invoices Query (for Discrepancy Holds)
  const invoicesQuery = useQuery({
    queryKey: ['dashboard', 'allInvoices'],
    queryFn: () => dashboardApi.getAllInvoices(),
    enabled: canReadMatching,
    refetchInterval: TIMING.query.dashboardRefresh,
  })

  // 6. Recent Audit Logs Query
  const auditLogsQuery = useQuery({
    queryKey: ['dashboard', 'auditLogs'],
    queryFn: () => dashboardApi.getRecentAuditLogs(),
    enabled: canReadAudit,
    refetchInterval: TIMING.query.dashboardRefresh,
  })

  // Quick Approve PR Mutation
  const approveMutation = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) =>
      dashboardApi.approveRequisitionStep(id, note),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  // Quick Reject PR Mutation
  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      dashboardApi.rejectRequisition(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  const isLoading =
    budgetQuery.isLoading ||
    pendingApprovalsQuery.isLoading ||
    myRequisitionsQuery.isLoading

  const refetchAll = () => {
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  return {
    budgetSummary:    budgetQuery.data,
    pendingApprovals: pendingApprovalsQuery.data ?? [],
    myRequisitions:   myRequisitionsQuery.data ?? [],
    dueInvoices:      dueInvoicesQuery.data ?? [],
    allInvoices:      invoicesQuery.data ?? [],
    auditLogs:        auditLogsQuery.data ?? [],
    isLoading,
    refetchAll,
    approvePR:        approveMutation.mutateAsync,
    rejectPR:         rejectMutation.mutateAsync,
    isApproving:      approveMutation.isPending,
    isRejecting:      rejectMutation.isPending,
  }
}
