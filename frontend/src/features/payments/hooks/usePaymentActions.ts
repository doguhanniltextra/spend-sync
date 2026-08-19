import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useToastStore } from '@/components/feedback/Toast'
import { paymentApi } from '../services/paymentApi'
import type {
  CreatePaymentBatchRequest,
  ApprovePaymentBatchRequest,
  PaymentBatchResponse,
} from '@/types/payment.types'

export function usePaymentActions() {
  const queryClient = useQueryClient()

  // 1. Create Batch
  const createMutation = useMutation<PaymentBatchResponse, Error, CreatePaymentBatchRequest>({
    mutationFn: (payload) => paymentApi.createPaymentBatch(payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Payment Batch Created',
        message: `Batch ${data.batchNumber} with ${data.itemCount} invoices created successfully.`,
      })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Failed to create payment batch.'
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Batch Creation Error',
        message: msg,
      })
    },
  })

  // 2. Approve & Dispatch to Bank
  const approveMutation = useMutation<
    PaymentBatchResponse,
    Error,
    { id: string; payload?: ApprovePaymentBatchRequest }
  >({
    mutationFn: ({ id, payload }) => paymentApi.approveAndDispatchBatch(id, payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Settlement Dispatched to Bank',
        message: `Payment Batch ${data.batchNumber} has been authorized and dispatched via ${data.paymentMethod}!`,
      })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
      queryClient.invalidateQueries({ queryKey: ['matching'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Failed to approve and dispatch batch.'
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Dispatch Authorization Error',
        message: msg,
      })
    },
  })

  // 3. Cancel Batch
  const cancelMutation = useMutation<PaymentBatchResponse, Error, string>({
    mutationFn: (id) => paymentApi.cancelBatch(id),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'warning',
        title: 'Payment Batch Cancelled',
        message: `Batch ${data.batchNumber} was cancelled. Invoices returned to settlement queue.`,
      })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Failed to cancel payment batch.'
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Batch Cancel Error',
        message: msg,
      })
    },
  })

  return {
    createBatch:       createMutation.mutateAsync,
    isCreating:        createMutation.isPending,
    approveBatch:      approveMutation.mutateAsync,
    isApproving:       approveMutation.isPending,
    cancelBatch:       cancelMutation.mutateAsync,
    isCancelling:      cancelMutation.isPending,
  }
}
