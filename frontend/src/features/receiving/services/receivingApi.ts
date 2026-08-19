import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type {
  CreateGoodsReceiptRequest,
  GoodsReceiptResponse,
  PendingPOForReceivingResponse,
} from '@/types/receiving.types'
import type { PurchaseOrderDetailResponse } from '@/types/purchasing.types'

export const receivingApi = {
  getPendingOrders: async (): Promise<PendingPOForReceivingResponse[]> => {
    const res = await apiClient.get<PendingPOForReceivingResponse[]>(
      ENDPOINTS.receiving.pendingOrders
    )
    return res.data
  },

  createGoodsReceipt: async (payload: CreateGoodsReceiptRequest): Promise<GoodsReceiptResponse> => {
    const res = await apiClient.post<GoodsReceiptResponse>(
      ENDPOINTS.receiving.receipts,
      payload
    )
    return res.data
  },

  getAllReceipts: async (): Promise<GoodsReceiptResponse[]> => {
    const res = await apiClient.get<GoodsReceiptResponse[]>(
      ENDPOINTS.receiving.receipts
    )
    return res.data
  },

  getReceiptById: async (id: string): Promise<GoodsReceiptResponse> => {
    const res = await apiClient.get<GoodsReceiptResponse>(
      `${ENDPOINTS.receiving.receipts}/${id}`
    )
    return res.data
  },

  getReceiptsByPO: async (poId: string): Promise<GoodsReceiptResponse[]> => {
    const res = await apiClient.get<GoodsReceiptResponse[]>(
      ENDPOINTS.receiving.receiptsByPo(poId)
    )
    return res.data
  },

  getPODetail: async (poId: string): Promise<PurchaseOrderDetailResponse> => {
    const res = await apiClient.get<PurchaseOrderDetailResponse>(
      `${ENDPOINTS.purchasing.orders}/${poId}`
    )
    return res.data
  },
}
