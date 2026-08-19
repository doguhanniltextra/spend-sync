import { useQuery } from '@tanstack/react-query'
import { paymentApi } from '../services/paymentApi'
import { TIMING } from '@/constants/timing'

export function usePaymentBatchDetail(batchId: string | null) {
  const query = useQuery({
    queryKey: ['payments', 'batch', batchId],
    queryFn: () => (batchId ? paymentApi.getBatchById(batchId) : null),
    enabled: Boolean(batchId),
    staleTime: TIMING.query.staleTime,
  })

  return {
    batch:     query.data ?? null,
    isLoading: query.isLoading,
    isError:   query.isError,
    error:     query.error,
    refetch:   query.refetch,
  }
}
