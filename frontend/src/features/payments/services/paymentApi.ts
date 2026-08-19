import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type {
  DueInvoiceResponse,
  PaymentBatchResponse,
  CreatePaymentBatchRequest,
  ApprovePaymentBatchRequest,
} from '@/types/payment.types'

export const paymentApi = {
  getDueInvoices: async (): Promise<DueInvoiceResponse[]> => {
    const res = await apiClient.get<DueInvoiceResponse[]>(
      ENDPOINTS.payment.dueInvoices
    )
    return res.data
  },

  getAllBatches: async (): Promise<PaymentBatchResponse[]> => {
    const res = await apiClient.get<PaymentBatchResponse[]>(
      ENDPOINTS.payment.batches
    )
    return res.data
  },

  getBatchById: async (id: string): Promise<PaymentBatchResponse> => {
    const res = await apiClient.get<PaymentBatchResponse>(
      ENDPOINTS.payment.batchById(id)
    )
    return res.data
  },

  createPaymentBatch: async (payload: CreatePaymentBatchRequest): Promise<PaymentBatchResponse> => {
    const res = await apiClient.post<PaymentBatchResponse>(
      ENDPOINTS.payment.batches,
      payload
    )
    return res.data
  },

  approveAndDispatchBatch: async (
    id: string,
    payload?: ApprovePaymentBatchRequest
  ): Promise<PaymentBatchResponse> => {
    const res = await apiClient.post<PaymentBatchResponse>(
      ENDPOINTS.payment.approveBatch(id),
      payload ?? { approvalNote: 'Authorized for bank execution' }
    )
    return res.data
  },

  cancelBatch: async (id: string): Promise<PaymentBatchResponse> => {
    const res = await apiClient.post<PaymentBatchResponse>(
      ENDPOINTS.payment.cancelBatch(id)
    )
    return res.data
  },
}
