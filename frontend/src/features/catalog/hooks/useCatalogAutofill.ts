import { useMutation } from '@tanstack/react-query'
import { catalogApi } from '../services/catalogApi'
import { useToast } from '@/hooks/useToast'
import { CATALOG_COPY } from '../constants/catalogCopy'
import type { CatalogAutofillResponse } from '../types/catalog.types'

export function useCatalogAutofill(onSelect?: (data: CatalogAutofillResponse) => void) {
  const toast = useToast()

  const mutation = useMutation({
    mutationFn: (itemId: string) => catalogApi.getAutofill(itemId),
    onSuccess: (data) => {
      toast.success('Item Auto-filled', CATALOG_COPY.picker.autofillSuccess)
      if (data.contractAlert) {
        toast.warning('Contract Alert', data.contractAlert)
      }
      onSelect?.(data)
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || 'Failed to fetch catalog item details.'
      toast.error('Error', msg)
    },
  })

  return {
    fetchAutofill: mutation.mutateAsync,
    isLoading: mutation.isPending,
    data: mutation.data,
  }
}
