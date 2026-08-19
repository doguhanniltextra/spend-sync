import { useMutation, useQueryClient } from '@tanstack/react-query'
import { purchaseOrderApi, type CancelPOPayload, type RevisePOPayload } from '../services/purchaseOrderApi'
import type { CreatePurchaseOrderRequest } from '@/types/purchasing.types'
import { useToast } from '@/components/feedback/Toast'

export function useCreatePurchaseOrder() {
  const queryClient = useQueryClient()
  const toast = useToast()

  const createMutation = useMutation({
    mutationFn: (dto: CreatePurchaseOrderRequest) => purchaseOrderApi.create(dto),
    onSuccess: (data) => {
      toast.success(
        'Purchase Order Created',
        `Purchase Order ${data.poNumber} has been generated as DRAFT.`
      )
      queryClient.invalidateQueries({ queryKey: ['purchasing', 'orders'] })
    },
    onError: (error: any) => {
      const msg = error.response?.data?.message || 'Failed to create purchase order.'
      toast.error('PO Creation Failed', msg)
    },
  })

  const issueMutation = useMutation({
    mutationFn: (id: string) => purchaseOrderApi.issue(id),
    onSuccess: (data) => {
      toast.success(
        'Purchase Order Issued',
        `PO ${data.poNumber} was successfully issued and dispatched to ${data.vendorName}.`
      )
      queryClient.invalidateQueries({ queryKey: ['purchasing'] })
    },
    onError: (error: any) => {
      const msg = error.response?.data?.message || 'Failed to issue purchase order.'
      toast.error('Issue Failed', msg)
    },
  })

  const reviseMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: RevisePOPayload }) =>
      purchaseOrderApi.revise(id, payload),
    onSuccess: (data) => {
      toast.success(
        'Purchase Order Revised',
        `PO ${data.poNumber} updated to Revision ${data.revisionNumber}.`
      )
      queryClient.invalidateQueries({ queryKey: ['purchasing'] })
    },
    onError: (error: any) => {
      const msg = error.response?.data?.message || 'Failed to revise purchase order.'
      toast.error('Revision Failed', msg)
    },
  })

  const cancelMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: CancelPOPayload }) =>
      purchaseOrderApi.cancel(id, payload),
    onSuccess: (data) => {
      toast.info(
        'Purchase Order Cancelled',
        `PO ${data.poNumber} has been cancelled.`
      )
      queryClient.invalidateQueries({ queryKey: ['purchasing'] })
    },
    onError: (error: any) => {
      const msg = error.response?.data?.message || 'Failed to cancel purchase order.'
      toast.error('Cancellation Failed', msg)
    },
  })

  return {
    createPO:   createMutation.mutateAsync,
    isCreating: createMutation.isPending,
    issuePO:    issueMutation.mutateAsync,
    isIssuing:  issueMutation.isPending,
    revisePO:   reviseMutation.mutateAsync,
    isRevising: reviseMutation.isPending,
    cancelPO:   cancelMutation.mutateAsync,
    isCancelling: cancelMutation.isPending,
  }
}
