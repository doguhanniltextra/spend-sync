import { useMutation, useQueryClient } from '@tanstack/react-query'
import { approvalApi, type ApproveStepPayload, type RejectStepPayload } from '../services/approvalApi'
import { useToast } from '@/components/feedback/Toast'

export function useApprovalDecision() {
  const queryClient = useQueryClient()
  const toast = useToast()

  const approveMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: ApproveStepPayload }) =>
      approvalApi.approve(id, payload),
    onSuccess: (data) => {
      toast.success(
        'Requisition Approved',
        `Requisition ${data.requisitionNumber ?? data.prNumber} was successfully signed off.`
      )
      queryClient.invalidateQueries({ queryKey: ['requisitions'] })
      queryClient.invalidateQueries({ queryKey: ['budget'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error: any) => {
      const msg =
        error.response?.data?.message ||
        'Failed to approve requisition. Please verify authority limits.'
      toast.error('Approval Error', msg)
    },
  })

  const rejectMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: RejectStepPayload }) =>
      approvalApi.reject(id, payload),
    onSuccess: (data) => {
      toast.info(
        'Requisition Rejected',
        `Requisition ${data.requisitionNumber ?? data.prNumber} was rejected and funds released.`
      )
      queryClient.invalidateQueries({ queryKey: ['requisitions'] })
      queryClient.invalidateQueries({ queryKey: ['budget'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error: any) => {
      const msg = error.response?.data?.message || 'Failed to reject requisition.'
      toast.error('Rejection Error', msg)
    },
  })

  return {
    approveRequisition: approveMutation.mutateAsync,
    isApproving:        approveMutation.isPending,
    rejectRequisition:  rejectMutation.mutateAsync,
    isRejecting:        rejectMutation.isPending,
  }
}
