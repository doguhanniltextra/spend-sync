import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type { RequisitionDetailResponse } from '@/types/requisition.types'

export interface EffectiveLimitResponse {
  hasConfiguredLimit: boolean
  isUnlimited:        boolean
  maxAmount:          number | string | null
  approvalLevel?:     number
  currency?:          string
  scope?:             'LEGAL_ENTITY' | 'COST_CENTER'
}

export interface ApproveStepPayload {
  decisionNote?: string
}

export interface RejectStepPayload {
  rejectionReason: string
}

export const approvalApi = {
  getPendingApprovals: (): Promise<RequisitionDetailResponse[]> =>
    apiClient
      .get<RequisitionDetailResponse[]>(ENDPOINTS.requisitions.pendingApprovals)
      .then((r) => r.data),

  getRequisitionById: (id: string): Promise<RequisitionDetailResponse> =>
    apiClient
      .get<RequisitionDetailResponse>(ENDPOINTS.requisitions.byId(id))
      .then((r) => r.data),

  approve: (id: string, payload: ApproveStepPayload): Promise<RequisitionDetailResponse> =>
    apiClient
      .post<RequisitionDetailResponse>(ENDPOINTS.requisitions.approve(id), payload)
      .then((r) => r.data),

  reject: (id: string, payload: RejectStepPayload): Promise<RequisitionDetailResponse> =>
    apiClient
      .post<RequisitionDetailResponse>(ENDPOINTS.requisitions.reject(id), payload)
      .then((r) => r.data),

  getEffectiveLimit: (
    userId: string,
    legalEntityId: string,
    costCenterId?: string
  ): Promise<EffectiveLimitResponse> => {
    let url = `${ENDPOINTS.requisitions.effectiveLimit}?userId=${userId}&legalEntityId=${legalEntityId}`
    if (costCenterId) {
      url += `&costCenterId=${costCenterId}`
    }
    return apiClient.get<EffectiveLimitResponse>(url).then((r) => r.data)
  },
} as const
