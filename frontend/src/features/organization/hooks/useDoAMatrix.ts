import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { organizationApi } from '../services/organizationApi'
import { useToastStore } from '@/components/feedback/Toast'
import { TIMING } from '@/constants/timing'
import type { SetApprovalLimitRequest } from '@/types/organization.types'

export function useDoAMatrix(legalEntityId?: string, userId?: string) {
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['organization', 'approvalLimits', legalEntityId, userId],
    queryFn: () => organizationApi.getApprovalLimits(legalEntityId, userId),
    staleTime: TIMING.query.staleTime,
  })

  const setLimitMutation = useMutation({
    mutationFn: (payload: SetApprovalLimitRequest) => organizationApi.setApprovalLimit(payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'DoA Limit Configured',
        message: `Approval Level ${data.approvalLevel} threshold set for ${data.userFullName}.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'approvalLimits'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Configuration Failed',
        message: err.response?.data?.message || err.message || 'Failed to configure signing limit',
      })
    },
  })

  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      organizationApi.toggleApprovalLimitStatus(id, active),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'info',
        title: 'DoA Status Updated',
        message: `Signing threshold is now ${data.isActive ? 'Active' : 'Disabled'}.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'approvalLimits'] })
    },
  })

  return {
    approvalLimits:   query.data ?? [],
    isLoading:        query.isLoading,
    isError:          query.isError,
    setApprovalLimit: setLimitMutation.mutateAsync,
    isSettingLimit:   setLimitMutation.isPending,
    toggleLimitStatus:toggleStatusMutation.mutateAsync,
    refetch:          query.refetch,
  }
}
