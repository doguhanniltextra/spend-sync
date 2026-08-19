import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { organizationApi } from '../services/organizationApi'
import { useToastStore } from '@/components/feedback/Toast'
import { TIMING } from '@/constants/timing'
import type { CreateLegalEntityRequest, UpdateLegalEntityRequest } from '@/types/organization.types'

export function useLegalEntities() {
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['organization', 'legalEntities'],
    queryFn: organizationApi.getLegalEntities,
    staleTime: TIMING.query.staleTime,
  })

  const createMutation = useMutation({
    mutationFn: (payload: CreateLegalEntityRequest) => organizationApi.createLegalEntity(payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Legal Entity Created',
        message: `Entity ${data.name} (${data.companyCode}) registered successfully.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'legalEntities'] })
      queryClient.invalidateQueries({ queryKey: ['orgContext'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Creation Failed',
        message: err.response?.data?.message || err.message || 'Failed to create legal entity',
      })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateLegalEntityRequest }) =>
      organizationApi.updateLegalEntity(id, payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Legal Entity Updated',
        message: `Entity ${data.name} updated successfully.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'legalEntities'] })
      queryClient.invalidateQueries({ queryKey: ['orgContext'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Update Failed',
        message: err.response?.data?.message || err.message || 'Failed to update legal entity',
      })
    },
  })

  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      organizationApi.toggleLegalEntityStatus(id, active),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'info',
        title: 'Status Updated',
        message: `Entity ${data.name} is now ${data.isActive ? 'Active' : 'Inactive'}.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'legalEntities'] })
    },
  })

  return {
    legalEntities:       query.data ?? [],
    isLoading:           query.isLoading,
    isError:             query.isError,
    createLegalEntity:   createMutation.mutateAsync,
    isCreating:          createMutation.isPending,
    updateLegalEntity:   updateMutation.mutateAsync,
    isUpdating:          updateMutation.isPending,
    toggleEntityStatus:  toggleStatusMutation.mutateAsync,
    refetch:             query.refetch,
  }
}
