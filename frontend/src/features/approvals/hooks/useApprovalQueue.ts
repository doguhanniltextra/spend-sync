import { useQuery } from '@tanstack/react-query'
import { approvalApi } from '../services/approvalApi'
import { TIMING } from '@/constants/timing'

export function useApprovalQueue() {
  const query = useQuery({
    queryKey: ['requisitions', 'pendingApprovals'],
    queryFn: approvalApi.getPendingApprovals,
    staleTime: TIMING.query.staleTime,
    refetchInterval: TIMING.query.refetchInterval,
  })

  const pendingList = query.data ?? []

  const totalExposure = pendingList.reduce(
    (acc, item) => acc + (item.totalAmount ?? item.totalEstimatedAmount ?? 0),
    0
  )

  return {
    pendingList,
    totalExposure,
    isLoading: query.isLoading,
    isRefetching: query.isRefetching,
    refetch:   query.refetch,
  }
}
