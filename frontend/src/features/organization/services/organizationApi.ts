import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type {
  LegalEntityResponse,
  CreateLegalEntityRequest,
  UpdateLegalEntityRequest,
  CostCenterResponse,
  CreateCostCenterRequest,
  UpdateCostCenterRequest,
  FacilityResponse,
  CreateFacilityRequest,
  UpdateFacilityRequest,
  UserResponse,
  UpdateUserRolesRequest,
  UpdateUserLegalEntitiesRequest,
  SubAccountInvitationResponse,
  InviteSubAccountRequest,
  GenerateRequisitionerLinkRequest,
  RequisitionerLinkResponse,
  ApprovalLimitResponse,
  SetApprovalLimitRequest,
} from '@/types/organization.types'

export const organizationApi = {
  // ─── Legal Entities ──────────────────────────────────────────────────────────
  getLegalEntities: async (): Promise<LegalEntityResponse[]> => {
    const res = await apiClient.get<LegalEntityResponse[]>(ENDPOINTS.organization.legalEntities)
    return res.data
  },

  getLegalEntityById: async (id: string): Promise<LegalEntityResponse> => {
    const res = await apiClient.get<LegalEntityResponse>(ENDPOINTS.organization.legalEntityById(id))
    return res.data
  },

  createLegalEntity: async (payload: CreateLegalEntityRequest): Promise<LegalEntityResponse> => {
    const res = await apiClient.post<LegalEntityResponse>(ENDPOINTS.organization.legalEntities, payload)
    return res.data
  },

  updateLegalEntity: async (id: string, payload: UpdateLegalEntityRequest): Promise<LegalEntityResponse> => {
    const res = await apiClient.put<LegalEntityResponse>(ENDPOINTS.organization.legalEntityById(id), payload)
    return res.data
  },

  toggleLegalEntityStatus: async (id: string, active: boolean): Promise<LegalEntityResponse> => {
    const res = await apiClient.patch<LegalEntityResponse>(ENDPOINTS.organization.legalEntityStatus(id), { active })
    return res.data
  },

  // ─── Cost Centers ────────────────────────────────────────────────────────────
  getCostCenters: async (legalEntityId?: string): Promise<CostCenterResponse[]> => {
    const params = legalEntityId ? { legalEntityId } : undefined
    const res = await apiClient.get<CostCenterResponse[]>(ENDPOINTS.organization.costCenters, { params })
    return res.data
  },

  getCostCenterById: async (id: string): Promise<CostCenterResponse> => {
    const res = await apiClient.get<CostCenterResponse>(ENDPOINTS.organization.costCenterById(id))
    return res.data
  },

  createCostCenter: async (payload: CreateCostCenterRequest): Promise<CostCenterResponse> => {
    const res = await apiClient.post<CostCenterResponse>(ENDPOINTS.organization.costCenters, payload)
    return res.data
  },

  updateCostCenter: async (id: string, payload: UpdateCostCenterRequest): Promise<CostCenterResponse> => {
    const res = await apiClient.put<CostCenterResponse>(ENDPOINTS.organization.costCenterById(id), payload)
    return res.data
  },

  toggleCostCenterStatus: async (id: string, active: boolean): Promise<CostCenterResponse> => {
    const res = await apiClient.patch<CostCenterResponse>(ENDPOINTS.organization.costCenterStatus(id), { active })
    return res.data
  },

  // ─── Facilities ──────────────────────────────────────────────────────────────
  getFacilities: async (legalEntityId?: string): Promise<FacilityResponse[]> => {
    const params = legalEntityId ? { legalEntityId } : undefined
    const res = await apiClient.get<FacilityResponse[]>(ENDPOINTS.organization.facilities, { params })
    return res.data
  },

  getFacilityById: async (id: string): Promise<FacilityResponse> => {
    const res = await apiClient.get<FacilityResponse>(ENDPOINTS.organization.facilityById(id))
    return res.data
  },

  createFacility: async (payload: CreateFacilityRequest): Promise<FacilityResponse> => {
    const res = await apiClient.post<FacilityResponse>(ENDPOINTS.organization.facilities, payload)
    return res.data
  },

  updateFacility: async (id: string, payload: UpdateFacilityRequest): Promise<FacilityResponse> => {
    const res = await apiClient.put<FacilityResponse>(ENDPOINTS.organization.facilityById(id), payload)
    return res.data
  },

  toggleFacilityStatus: async (id: string, active: boolean): Promise<FacilityResponse> => {
    const res = await apiClient.patch<FacilityResponse>(ENDPOINTS.organization.facilityStatus(id), { active })
    return res.data
  },

  // ─── User Management & Invitations ───────────────────────────────────────────
  getUsers: async (): Promise<UserResponse[]> => {
    const res = await apiClient.get<UserResponse[]>(ENDPOINTS.organization.users)
    return res.data
  },

  getUserById: async (id: string): Promise<UserResponse> => {
    const res = await apiClient.get<UserResponse>(ENDPOINTS.organization.userById(id))
    return res.data
  },

  updateUserRoles: async (id: string, payload: UpdateUserRolesRequest): Promise<UserResponse> => {
    const res = await apiClient.put<UserResponse>(ENDPOINTS.organization.userRoles(id), payload)
    return res.data
  },

  updateUserLegalEntities: async (id: string, payload: UpdateUserLegalEntitiesRequest): Promise<UserResponse> => {
    const res = await apiClient.put<UserResponse>(ENDPOINTS.organization.userLegalEntities(id), payload)
    return res.data
  },

  toggleUserStatus: async (id: string, active: boolean): Promise<UserResponse> => {
    const res = await apiClient.patch<UserResponse>(ENDPOINTS.organization.userStatus(id), { active })
    return res.data
  },

  inviteSubAccount: async (payload: InviteSubAccountRequest): Promise<SubAccountInvitationResponse> => {
    const res = await apiClient.post<SubAccountInvitationResponse>(ENDPOINTS.organization.inviteSubAccount, payload)
    return res.data
  },

  generateRequisitionerLink: async (payload: GenerateRequisitionerLinkRequest): Promise<RequisitionerLinkResponse> => {
    const res = await apiClient.post<RequisitionerLinkResponse>(ENDPOINTS.organization.generateRequisitionerLink, payload)
    return res.data
  },

  getInvitations: async (): Promise<SubAccountInvitationResponse[]> => {
    const res = await apiClient.get<SubAccountInvitationResponse[]>(ENDPOINTS.organization.invitations)
    return res.data
  },

  revokeInvitation: async (id: string): Promise<void> => {
    await apiClient.delete(ENDPOINTS.organization.invitationById(id))
  },

  // ─── Delegation of Authority (DoA) Limits ────────────────────────────────────
  getApprovalLimits: async (legalEntityId?: string, userId?: string): Promise<ApprovalLimitResponse[]> => {
    const params: Record<string, string> = {}
    if (legalEntityId) params.legalEntityId = legalEntityId
    if (userId) params.userId = userId
    const res = await apiClient.get<ApprovalLimitResponse[]>(ENDPOINTS.requisitions.approvalLimits, { params })
    return res.data
  },

  setApprovalLimit: async (payload: SetApprovalLimitRequest): Promise<ApprovalLimitResponse> => {
    const res = await apiClient.post<ApprovalLimitResponse>(ENDPOINTS.requisitions.approvalLimits, payload)
    return res.data
  },

  toggleApprovalLimitStatus: async (id: string, active: boolean): Promise<ApprovalLimitResponse> => {
    const res = await apiClient.patch<ApprovalLimitResponse>(ENDPOINTS.requisitions.approvalLimitStatus(id), { active })
    return res.data
  },
}
