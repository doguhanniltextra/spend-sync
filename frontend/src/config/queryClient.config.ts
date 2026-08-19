import { QueryClient } from '@tanstack/react-query'
import { TIMING } from '@/constants/timing'

/**
 * Global TanStack Query client configuration.
 * Centralised so staleTime, retry and error handling are consistent everywhere.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime:           TIMING.query.staleTime,
      gcTime:              TIMING.query.gcTime,
      retry:               1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
})
