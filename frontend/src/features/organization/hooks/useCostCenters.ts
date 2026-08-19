import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { organizationApi } from '../services/organizationApi'
import { useToastStore } from '@/components/feedback/Toast'
import { TIMING } from '@/constants/timing'
import type { CreateCostCenterRequest, UpdateCostCenterRequest } from '@/types/organization.types'

export function useCostCenters(legalEntityId?: string) {
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['organization', 'costCenters', legalEntityId],
    queryFn: () => organizationApi.getCostCenters(legalEntityId),
    staleTime: TIMING.query.staleTime,
  })

  const createMutation = useMutation({
    mutationFn: (payload: CreateCostCenterRequest) => organizationApi.createCostCenter(payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Cost Center Created',
        message: `Cost Center ${data.code} - ${data.name} created successfully.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'costCenters'] })
      queryClient.invalidateQueries({ queryKey: ['orgContext'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Creation Failed',
        message: err.response?.data?.message || err.message || 'Failed to create cost center',
      })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateCostCenterRequest }) =>
      organizationApi.updateCostCenter(id, payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Cost Center Updated',
        message: `Cost Center ${data.name} updated successfully.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'costCenters'] })
      queryClient.invalidateQueries({ queryKey: ['orgContext'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Update Failed',
        message: err.response?.data?.message || err.message || 'Failed to update cost center',
      })
    },
  })

  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      organizationApi.toggleCostCenterStatus(id, active),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'info',
        title: 'Status Updated',
        message: `Cost Center ${data.name} is now ${data.isActive ? 'Active' : 'Inactive'}.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'costCenters'] })
    },
  })

  return {
    costCenters:        query.data ?? [],
    isLoading:          query.isLoading,
    isError:            query.isError,
    createCostCenter:   createMutation.mutateAsync,
    isCreating:         createMutation.isPending,
    updateCostCenter:   updateMutation.mutateAsync,
    isUpdating:         updateMutation.isPending,
    toggleCenterStatus: toggleStatusMutation.mutateAsync,
    refetch:            query.refetch,
  }
}
