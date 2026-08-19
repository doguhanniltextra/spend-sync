import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type {
  CreatePurchaseOrderRequest,
  PurchaseOrderDetailResponse,
  PurchaseOrderSummaryResponse,
  PORevisionResponse,
} from '@/types/purchasing.types'

export interface RevisePOPayload {
  revisionReason: string
  notes?:         string
  lineItems:      any[]
}

export interface CancelPOPayload {
  cancellationReason: string
}

export const purchaseOrderApi = {
  create: (dto: CreatePurchaseOrderRequest): Promise<PurchaseOrderDetailResponse> =>
    apiClient
      .post<PurchaseOrderDetailResponse>(ENDPOINTS.purchasing.orders, dto)
      .then((r) => r.data),

  getAll: (status?: string, vendorId?: string): Promise<PurchaseOrderSummaryResponse[]> => {
    const params = new URLSearchParams()
    if (status && status !== 'ALL') params.append('status', status)
    if (vendorId && vendorId !== 'ALL') params.append('vendorId', vendorId)
    const queryString = params.toString() ? `?${params.toString()}` : ''
    return apiClient
      .get<PurchaseOrderSummaryResponse[]>(`${ENDPOINTS.purchasing.orders}${queryString}`)
      .then((r) => r.data)
  },

  getById: (id: string): Promise<PurchaseOrderDetailResponse> =>
    apiClient
      .get<PurchaseOrderDetailResponse>(ENDPOINTS.purchasing.orderById(id))
      .then((r) => r.data),

  issue: (id: string): Promise<PurchaseOrderDetailResponse> =>
    apiClient
      .post<PurchaseOrderDetailResponse>(ENDPOINTS.purchasing.orderIssue(id))
      .then((r) => r.data),

  revise: (id: string, payload: RevisePOPayload): Promise<PurchaseOrderDetailResponse> =>
    apiClient
      .post<PurchaseOrderDetailResponse>(ENDPOINTS.purchasing.orderRevise(id), payload)
      .then((r) => r.data),

  getRevisions: (id: string): Promise<PORevisionResponse[]> =>
    apiClient
      .get<PORevisionResponse[]>(`${ENDPOINTS.purchasing.orderById(id)}/revisions`)
      .then((r) => r.data),

  cancel: (id: string, payload: CancelPOPayload): Promise<PurchaseOrderDetailResponse> =>
    apiClient
      .post<PurchaseOrderDetailResponse>(ENDPOINTS.purchasing.orderCancel(id), payload)
      .then((r) => r.data),
} as const
