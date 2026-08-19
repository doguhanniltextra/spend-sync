import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { vendorApi, type UpdateVendorStatusPayload } from '../services/vendorApi'
import type { CreateVendorRequest, VendorCategory, VendorStatus, VendorTier } from '@/types/purchasing.types'
import { useToast } from '@/components/feedback/Toast'
import { TIMING } from '@/constants/timing'

export function useVendors(
  status?: VendorStatus | 'ALL',
  category?: VendorCategory | 'ALL',
  tier?: VendorTier | 'ALL'
) {
  const query = useQuery({
    queryKey: ['purchasing', 'vendors', status ?? 'ALL', category ?? 'ALL', tier ?? 'ALL'],
    queryFn: () => vendorApi.getAll(status, category, tier),
    staleTime: TIMING.query.staleTime * 3,
  })

  return {
    vendors:   query.data ?? [],
    isLoading: query.isLoading,
    refetch:   query.refetch,
  }
}

export function useCreateVendor() {
  const queryClient = useQueryClient()
  const toast = useToast()

  const createMutation = useMutation({
    mutationFn: (dto: CreateVendorRequest) => vendorApi.create(dto),
    onSuccess: (data) => {
      toast.success(
        'Vendor Onboarded',
        `Supplier ${data.name} (VKN: ${data.taxNumber}) was successfully registered.`
      )
      queryClient.invalidateQueries({ queryKey: ['purchasing', 'vendors'] })
    },
    onError: (error: any) => {
      const msg = error.response?.data?.message || 'Failed to onboard vendor.'
      toast.error('Onboarding Error', msg)
    },
  })

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateVendorStatusPayload }) =>
      vendorApi.updateStatus(id, payload),
    onSuccess: (data) => {
      toast.info(
        'Vendor Status Updated',
        `Supplier ${data.name} is now ${data.status}.`
      )
      queryClient.invalidateQueries({ queryKey: ['purchasing', 'vendors'] })
    },
    onError: (error: any) => {
      const msg = error.response?.data?.message || 'Failed to update vendor status.'
      toast.error('Status Update Failed', msg)
    },
  })

  return {
    createVendor: createMutation.mutateAsync,
    isOnboarding: createMutation.isPending,
    updateStatus: updateStatusMutation.mutateAsync,
    isUpdatingStatus: updateStatusMutation.isPending,
  }
}
