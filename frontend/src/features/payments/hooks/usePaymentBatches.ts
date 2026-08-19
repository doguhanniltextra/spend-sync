import { useQuery } from '@tanstack/react-query'
import { paymentApi } from '../services/paymentApi'
import { TIMING } from '@/constants/timing'

export function usePaymentBatches() {
  const query = useQuery({
    queryKey: ['payments', 'batches'],
    queryFn: paymentApi.getAllBatches,
    staleTime: TIMING.query.staleTime,
    refetchInterval: TIMING.query.refetchInterval,
  })

  return {
    batches:   query.data ?? [],
    isLoading: query.isLoading,
    isError:   query.isError,
    error:     query.error,
    refetch:   query.refetch,
  }
}
