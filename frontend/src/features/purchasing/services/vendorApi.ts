import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type {
  CreateVendorRequest,
  VendorResponse,
  VendorCategory,
  VendorStatus,
  VendorTier,
} from '@/types/purchasing.types'

export interface UpdateVendorStatusPayload {
  status: VendorStatus
  reason?: string
}

export const vendorApi = {
  create: (dto: CreateVendorRequest): Promise<VendorResponse> =>
    apiClient
      .post<VendorResponse>(ENDPOINTS.purchasing.vendors, dto)
      .then((r) => r.data),

  getAll: (
    status?: VendorStatus | 'ALL',
    category?: VendorCategory | 'ALL',
    tier?: VendorTier | 'ALL'
  ): Promise<VendorResponse[]> => {
    const params = new URLSearchParams()
    if (status && status !== 'ALL') params.append('status', status)
    if (category && category !== 'ALL') params.append('category', category)
    if (tier && tier !== 'ALL') params.append('tier', tier)
    const queryString = params.toString() ? `?${params.toString()}` : ''
    return apiClient
      .get<VendorResponse[]>(`${ENDPOINTS.purchasing.vendors}${queryString}`)
      .then((r) => r.data)
  },

  getById: (id: string): Promise<VendorResponse> =>
    apiClient
      .get<VendorResponse>(ENDPOINTS.purchasing.vendorById(id))
      .then((r) => r.data),

  updateStatus: (id: string, payload: UpdateVendorStatusPayload): Promise<VendorResponse> =>
    apiClient
      .patch<VendorResponse>(ENDPOINTS.purchasing.vendorStatus(id), payload)
      .then((r) => r.data),
} as const
