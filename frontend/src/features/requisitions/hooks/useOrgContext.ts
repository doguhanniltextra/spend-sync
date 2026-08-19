import { useQuery } from '@tanstack/react-query'
import { requisitionApi } from '../services/requisitionApi'
import { TIMING } from '@/constants/timing'

export function useOrgContext() {
  const legalEntitiesQuery = useQuery({
    queryKey: ['org', 'legalEntities'],
    queryFn: requisitionApi.getLegalEntities,
    staleTime: TIMING.query.staleTime * 5,
  })

  const costCentersQuery = useQuery({
    queryKey: ['org', 'costCenters'],
    queryFn: requisitionApi.getCostCenters,
    staleTime: TIMING.query.staleTime * 5,
  })

  const facilitiesQuery = useQuery({
    queryKey: ['org', 'facilities'],
    queryFn: requisitionApi.getFacilities,
    staleTime: TIMING.query.staleTime * 5,
  })

  const budgetSummaryQuery = useQuery({
    queryKey: ['budget', 'summary', 2026],
    queryFn: () => requisitionApi.getBudgetSummary(2026),
    staleTime: TIMING.query.staleTime,
  })

  const isLoading =
    legalEntitiesQuery.isLoading ||
    costCentersQuery.isLoading ||
    facilitiesQuery.isLoading

  return {
    legalEntities: legalEntitiesQuery.data ?? [],
    costCenters:   costCentersQuery.data ?? [],
    facilities:    facilitiesQuery.data ?? [],
    budgetSummary: budgetSummaryQuery.data,
    isLoading,
  }
}
