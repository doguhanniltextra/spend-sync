import { useQuery } from '@tanstack/react-query'
import { catalogApi } from '../services/catalogApi'
import { useDebounce } from '@/hooks/useDebounce'

export const CATALOG_QUERY_KEYS = {
  all: ['catalog'] as const,
  search: (params: any) => ['catalog', 'search', params] as const,
  categories: () => ['catalog', 'categories'] as const,
  item: (id: string) => ['catalog', 'item', id] as const,
  health: () => ['catalog', 'health'] as const,
}

export function useCatalogSearch(params: {
  q?: string
  categoryId?: string
  activeOnly?: boolean
  page?: number
  size?: number
}) {
  const debouncedQuery = useDebounce(params.q || '', 300)

  return useQuery({
    queryKey: CATALOG_QUERY_KEYS.search({
      ...params,
      q: debouncedQuery,
    }),
    queryFn: () =>
      catalogApi.searchItems({
        ...params,
        q: debouncedQuery.trim() ? debouncedQuery.trim() : undefined,
      }),
    staleTime: 1000 * 60 * 2, // 2 minutes
  })
}
