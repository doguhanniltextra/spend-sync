import { useQuery } from '@tanstack/react-query'
import { paymentApi } from '../services/paymentApi'
import { TIMING } from '@/constants/timing'

export function useDueInvoices() {
  const query = useQuery({
    queryKey: ['payments', 'dueInvoices'],
    queryFn: paymentApi.getDueInvoices,
    staleTime: TIMING.query.staleTime,
    refetchInterval: TIMING.query.refetchInterval,
  })

  return {
    dueInvoices: query.data ?? [],
    isLoading:   query.isLoading,
    isError:     query.isError,
    error:       query.error,
    refetch:     query.refetch,
  }
}
