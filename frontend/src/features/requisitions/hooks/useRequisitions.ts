import { useQuery } from '@tanstack/react-query'
import { requisitionApi } from '../services/requisitionApi'
import { useAuthStore } from '@/store/useAuthStore'
import { PERMISSIONS } from '@/constants/permissions'
import { TIMING } from '@/constants/timing'

export function useRequisitions(statusFilter?: string) {
  const hasPermission = useAuthStore((s) => s.hasPermission)
  const canReadAll = hasPermission(PERMISSIONS.requisition.readAll)

  const query = useQuery({
    queryKey: ['requisitions', 'list', canReadAll ? 'all' : 'my', statusFilter ?? 'ALL'],
    queryFn: () => {
      if (canReadAll) {
        return requisitionApi.getAll(statusFilter === 'ALL' ? undefined : statusFilter)
      }
      return requisitionApi.getMyRequisitions()
    },
    staleTime: TIMING.query.staleTime,
  })

  return {
    requisitions: query.data ?? [],
    isLoading:    query.isLoading,
    refetch:      query.refetch,
  }
}
