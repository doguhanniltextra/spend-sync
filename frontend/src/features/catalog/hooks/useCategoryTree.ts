import { useQuery } from '@tanstack/react-query'
import { catalogApi } from '../services/catalogApi'
import { CATALOG_QUERY_KEYS } from './useCatalogSearch'

export function useCategoryTree() {
  return useQuery({
    queryKey: CATALOG_QUERY_KEYS.categories(),
    queryFn: () => catalogApi.getCategories(),
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}

export function useCatalogHealth() {
  return useQuery({
    queryKey: CATALOG_QUERY_KEYS.health(),
    queryFn: () => catalogApi.getHealthMetrics(),
    staleTime: 1000 * 60 * 2,
  })
}
