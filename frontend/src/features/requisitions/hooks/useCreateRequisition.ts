import { useMutation, useQueryClient } from '@tanstack/react-query'
import { requisitionApi } from '../services/requisitionApi'
import type { CreateRequisitionRequest } from '@/types/requisition.types'
import { useToast } from '@/components/feedback/Toast'

export function useCreateRequisition() {
  const queryClient = useQueryClient()
  const toast = useToast()

  const createMutation = useMutation({
    mutationFn: (dto: CreateRequisitionRequest) => requisitionApi.createAndSubmit(dto),
    onSuccess: (data) => {
      toast.success(
        'Requisition Submitted',
        `Requisition ${data.requisitionNumber} was successfully submitted for approval.`
      )
      queryClient.invalidateQueries({ queryKey: ['requisitions'] })
      queryClient.invalidateQueries({ queryKey: ['budget'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error: any) => {
      const msg =
        error.response?.data?.message ||
        'Failed to submit requisition. Please check budget availability or try again.'
      toast.error('Submission Failed', msg)
    },
  })

  const cancelMutation = useMutation({
    mutationFn: (id: string) => requisitionApi.cancel(id),
    onSuccess: (data) => {
      toast.success(
        'Requisition Cancelled',
        `Requisition ${data.requisitionNumber} has been cancelled and reserved funds released.`
      )
      queryClient.invalidateQueries({ queryKey: ['requisitions'] })
      queryClient.invalidateQueries({ queryKey: ['budget'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error: any) => {
      const msg = error.response?.data?.message || 'Failed to cancel requisition.'
      toast.error('Action Failed', msg)
    },
  })

  return {
    createRequisition: createMutation.mutateAsync,
    isCreating:        createMutation.isPending,
    cancelRequisition: cancelMutation.mutateAsync,
    isCancelling:      cancelMutation.isPending,
  }
}
