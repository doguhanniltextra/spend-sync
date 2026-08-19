import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type {
  CreateRequisitionRequest,
  RequisitionDetailResponse,
  RequisitionSummaryResponse,
} from '@/types/requisition.types'
import type {
  LegalEntityResponse,
  CostCenterResponse,
  FacilityResponse,
} from '@/types/organization.types'
import type { BudgetSummaryResponse } from '@/types/budget.types'

export const requisitionApi = {
  createAndSubmit: (dto: CreateRequisitionRequest): Promise<RequisitionDetailResponse> =>
    apiClient
      .post<RequisitionDetailResponse>(ENDPOINTS.requisitions.list, dto)
      .then((r) => r.data),

  getAll: (status?: string): Promise<RequisitionSummaryResponse[]> => {
    const url = status
      ? `${ENDPOINTS.requisitions.list}?status=${status}`
      : ENDPOINTS.requisitions.list
    return apiClient.get<RequisitionSummaryResponse[]>(url).then((r) => r.data)
  },

  getMyRequisitions: (): Promise<RequisitionSummaryResponse[]> =>
    apiClient
      .get<RequisitionSummaryResponse[]>(ENDPOINTS.requisitions.my)
      .then((r) => r.data),

  getById: (id: string): Promise<RequisitionDetailResponse> =>
    apiClient
      .get<RequisitionDetailResponse>(ENDPOINTS.requisitions.byId(id))
      .then((r) => r.data),

  cancel: (id: string): Promise<RequisitionDetailResponse> =>
    apiClient
      .post<RequisitionDetailResponse>(ENDPOINTS.requisitions.cancel(id))
      .then((r) => r.data),

  getLegalEntities: (): Promise<LegalEntityResponse[]> =>
    apiClient
      .get<LegalEntityResponse[]>(ENDPOINTS.organization.legalEntities)
      .then((r) => r.data),

  getCostCenters: (): Promise<CostCenterResponse[]> =>
    apiClient
      .get<CostCenterResponse[]>(ENDPOINTS.organization.costCenters)
      .then((r) => r.data),

  getFacilities: (): Promise<FacilityResponse[]> =>
    apiClient
      .get<FacilityResponse[]>(ENDPOINTS.organization.facilities)
      .then((r) => r.data),

  getBudgetSummary: (fiscalYear = 2026): Promise<BudgetSummaryResponse> =>
    apiClient
      .get<BudgetSummaryResponse>(`${ENDPOINTS.budget.summary}?fiscalYear=${fiscalYear}`)
      .then((r) => r.data),
} as const
