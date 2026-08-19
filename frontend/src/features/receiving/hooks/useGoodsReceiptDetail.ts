import { useQuery } from '@tanstack/react-query'
import { receivingApi } from '../services/receivingApi'
import { TIMING } from '@/constants/timing'

export function useGoodsReceiptDetail(receiptId: string | null) {
  const query = useQuery({
    queryKey: ['receiving', 'receipt', receiptId],
    queryFn: () => (receiptId ? receivingApi.getReceiptById(receiptId) : null),
    enabled: Boolean(receiptId),
    staleTime: TIMING.query.staleTime,
  })

  return {
    receipt:   query.data ?? null,
    isLoading: query.isLoading,
    isError:   query.isError,
    refetch:   query.refetch,
  }
}
