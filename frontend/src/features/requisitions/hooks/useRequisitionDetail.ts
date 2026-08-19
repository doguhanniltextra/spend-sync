import { useQuery } from '@tanstack/react-query'
import { requisitionApi } from '../services/requisitionApi'
import { TIMING } from '@/constants/timing'

export function useRequisitionDetail(id: string | null) {
  const query = useQuery({
    queryKey: ['requisitions', 'detail', id],
    queryFn: () => (id ? requisitionApi.getById(id) : null),
    enabled: Boolean(id),
    staleTime: TIMING.query.staleTime,
  })

  return {
    requisition: query.data,
    isLoading:   query.isLoading,
    refetch:     query.refetch,
  }
}
