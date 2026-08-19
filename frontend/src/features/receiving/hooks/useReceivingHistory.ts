import { useQuery } from '@tanstack/react-query'
import { receivingApi } from '../services/receivingApi'
import { TIMING } from '@/constants/timing'

export function useReceivingHistory() {
  const query = useQuery({
    queryKey: ['receiving', 'history'],
    queryFn: receivingApi.getAllReceipts,
    staleTime: TIMING.query.staleTime,
  })

  return {
    receipts:  query.data ?? [],
    isLoading: query.isLoading,
    isError:   query.isError,
    error:     query.error,
    refetch:   query.refetch,
  }
}
