import { useQuery } from '@tanstack/react-query'
import { receivingApi } from '../services/receivingApi'
import { TIMING } from '@/constants/timing'

export function usePendingOrders() {
  const query = useQuery({
    queryKey: ['receiving', 'pendingOrders'],
    queryFn: receivingApi.getPendingOrders,
    staleTime: TIMING.query.staleTime,
    refetchInterval: TIMING.query.refetchInterval,
  })

  return {
    pendingOrders: query.data ?? [],
    isLoading:     query.isLoading,
    isError:       query.isError,
    error:         query.error,
    refetch:       query.refetch,
  }
}
