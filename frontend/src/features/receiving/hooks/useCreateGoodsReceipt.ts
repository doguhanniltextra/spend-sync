import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useToastStore } from '@/components/feedback/Toast'
import { receivingApi } from '../services/receivingApi'
import type { CreateGoodsReceiptRequest, GoodsReceiptResponse } from '@/types/receiving.types'

export function useCreateGoodsReceipt() {
  const queryClient = useQueryClient()

  const createMutation = useMutation<GoodsReceiptResponse, Error, CreateGoodsReceiptRequest>({
    mutationFn: (payload) => receivingApi.createGoodsReceipt(payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Goods Receipt Processed',
        message: `Goods Receipt ${data.receiptNumber} successfully created and verified!`,
      })
      queryClient.invalidateQueries({ queryKey: ['receiving'] })
      queryClient.invalidateQueries({ queryKey: ['purchasing'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Failed to submit goods receipt.'
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Receipt Error',
        message: msg,
      })
    },
  })

  return {
    createReceipt: createMutation.mutateAsync,
    isCreating:    createMutation.isPending,
  }
}
