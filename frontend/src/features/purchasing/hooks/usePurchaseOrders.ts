import { useQuery } from '@tanstack/react-query'
import { purchaseOrderApi } from '../services/purchaseOrderApi'
import { TIMING } from '@/constants/timing'

export function usePurchaseOrders(statusFilter?: string, vendorFilter?: string) {
  const query = useQuery({
    queryKey: ['purchasing', 'orders', statusFilter ?? 'ALL', vendorFilter ?? 'ALL'],
    queryFn: () => purchaseOrderApi.getAll(statusFilter, vendorFilter),
    staleTime: TIMING.query.staleTime,
  })

  return {
    orders:    query.data ?? [],
    isLoading: query.isLoading,
    refetch:   query.refetch,
  }
}

export function usePurchaseOrderDetail(id: string | null) {
  const query = useQuery({
    queryKey: ['purchasing', 'order', id],
    queryFn: () => (id ? purchaseOrderApi.getById(id) : null),
    enabled: Boolean(id),
    staleTime: TIMING.query.staleTime,
  })

  return {
    order:     query.data,
    isLoading: query.isLoading,
    refetch:   query.refetch,
  }
}
