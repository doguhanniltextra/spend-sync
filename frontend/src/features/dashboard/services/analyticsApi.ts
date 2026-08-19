import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type { CfoExecutiveDeckResponse } from '@/types/analytics.types'

export const analyticsApi = {
  getCfoDeck: async (): Promise<CfoExecutiveDeckResponse> => {
    const res = await apiClient.get<CfoExecutiveDeckResponse>(ENDPOINTS.analytics.cfoDeck)
    return res.data
  },
}
