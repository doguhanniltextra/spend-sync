import { useQuery } from '@tanstack/react-query'
import { approvalApi } from '../services/approvalApi'
import { useAuthStore } from '@/store/useAuthStore'
import { TIMING } from '@/constants/timing'

export function useEffectiveLimit(legalEntityId?: string, costCenterId?: string) {
  const user = useAuthStore((s) => s.user)
  const isCFOorRoot = useAuthStore((s) => s.hasRole('ROOT_USER') || s.hasRole('CFO'))

  const query = useQuery({
    queryKey: ['approvalLimits', 'effective', user?.id, legalEntityId, costCenterId],
    queryFn: () => {
      if (!user?.id || !legalEntityId) return null
      return approvalApi.getEffectiveLimit(user.id, legalEntityId, costCenterId)
    },
    enabled: Boolean(user?.id && legalEntityId) && !isCFOorRoot,
    staleTime: TIMING.query.staleTime * 5,
  })

  return {
    isCFOorRoot,
    limitData: query.data,
    isLoading: query.isLoading,
  }
}
