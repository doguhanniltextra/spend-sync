import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type { BudgetSummaryResponse } from '@/types/budget.types'
import type { RequisitionDetailResponse, RequisitionSummaryResponse } from '@/types/requisition.types'
import type { DueInvoiceResponse } from '@/types/payment.types'
import type { SupplierInvoiceResponse } from '@/types/matching.types'
import type { AuditLogResponse } from '@/types/audit.types'

export const dashboardApi = {
  getBudgetSummary: (fiscalYear = 2026): Promise<BudgetSummaryResponse> =>
    apiClient
      .get<BudgetSummaryResponse>(`${ENDPOINTS.budget.summary}?fiscalYear=${fiscalYear}`)
      .then((r) => r.data),

  getPendingApprovals: (): Promise<RequisitionDetailResponse[]> =>
    apiClient
      .get<RequisitionDetailResponse[]>(ENDPOINTS.requisitions.pendingApprovals)
      .then((r) => r.data),

  getMyRequisitions: (): Promise<RequisitionSummaryResponse[]> =>
    apiClient
      .get<RequisitionSummaryResponse[]>(ENDPOINTS.requisitions.my)
      .then((r) => r.data),

  getDueInvoices: (): Promise<DueInvoiceResponse[]> =>
    apiClient
      .get<DueInvoiceResponse[]>(ENDPOINTS.payment.dueInvoices)
      .then((r) => r.data),

  getAllInvoices: (): Promise<SupplierInvoiceResponse[]> =>
    apiClient
      .get<SupplierInvoiceResponse[]>(ENDPOINTS.matching.invoices)
      .then((r) => r.data),

  getRecentAuditLogs: (): Promise<AuditLogResponse[]> =>
    apiClient
      .get<AuditLogResponse[]>(ENDPOINTS.audit.logs)
      .then((r) => r.data),

  approveRequisitionStep: (id: string, decisionNote = 'Approved via Dashboard'): Promise<RequisitionDetailResponse> =>
    apiClient
      .post<RequisitionDetailResponse>(ENDPOINTS.requisitions.approve(id), { decisionNote })
      .then((r) => r.data),

  rejectRequisition: (id: string, rejectionReason: string): Promise<RequisitionDetailResponse> =>
    apiClient
      .post<RequisitionDetailResponse>(ENDPOINTS.requisitions.reject(id), { rejectionReason })
      .then((r) => r.data),
} as const
