import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { organizationApi } from '../services/organizationApi'
import { useToastStore } from '@/components/feedback/Toast'
import { TIMING } from '@/constants/timing'
import type { CreateFacilityRequest, UpdateFacilityRequest } from '@/types/organization.types'

export function useFacilities(legalEntityId?: string) {
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['organization', 'facilities', legalEntityId],
    queryFn: () => organizationApi.getFacilities(legalEntityId),
    staleTime: TIMING.query.staleTime,
  })

  const createMutation = useMutation({
    mutationFn: (payload: CreateFacilityRequest) => organizationApi.createFacility(payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Facility Created',
        message: `Facility ${data.facilityCode} - ${data.name} created successfully.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'facilities'] })
      queryClient.invalidateQueries({ queryKey: ['orgContext'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Creation Failed',
        message: err.response?.data?.message || err.message || 'Failed to create facility',
      })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateFacilityRequest }) =>
      organizationApi.updateFacility(id, payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Facility Updated',
        message: `Facility ${data.name} updated successfully.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'facilities'] })
      queryClient.invalidateQueries({ queryKey: ['orgContext'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Update Failed',
        message: err.response?.data?.message || err.message || 'Failed to update facility',
      })
    },
  })

  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      organizationApi.toggleFacilityStatus(id, active),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'info',
        title: 'Status Updated',
        message: `Facility ${data.name} is now ${data.isActive ? 'Active' : 'Inactive'}.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'facilities'] })
    },
  })

  return {
    facilities:           query.data ?? [],
    isLoading:            query.isLoading,
    isError:              query.isError,
    createFacility:       createMutation.mutateAsync,
    isCreating:           createMutation.isPending,
    updateFacility:       updateMutation.mutateAsync,
    isUpdating:           updateMutation.isPending,
    toggleFacilityStatus: toggleStatusMutation.mutateAsync,
    refetch:              query.refetch,
  }
}
