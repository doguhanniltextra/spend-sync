import { useQuery } from '@tanstack/react-query'
import { analyticsApi } from '../services/analyticsApi'
import { TIMING } from '@/constants/timing'

export function useCfoAnalytics() {
  const query = useQuery({
    queryKey: ['analytics', 'cfo-deck'],
    queryFn: () => analyticsApi.getCfoDeck(),
    staleTime: TIMING.query.staleTime,
  })

  return {
    deck:      query.data,
    isLoading: query.isLoading,
    refetch:   query.refetch,
  }
}
