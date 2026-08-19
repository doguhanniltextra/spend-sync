import { useMutation, useQueryClient } from '@tanstack/react-query'
import { catalogApi } from '../services/catalogApi'
import { CATALOG_QUERY_KEYS } from './useCatalogSearch'
import { useToast } from '@/hooks/useToast'
import { CATALOG_COPY } from '../constants/catalogCopy'
import type {
  CatalogItemCreateRequest,
  CatalogItemUpdateRequest,
  CatalogCategoryCreateRequest,
} from '../types/catalog.types'

export function useCatalogAdmin() {
  const queryClient = useQueryClient()
  const toast = useToast()

  const invalidateCatalogQueries = () => {
    queryClient.invalidateQueries({ queryKey: CATALOG_QUERY_KEYS.all })
  }

  const createItemMutation = useMutation({
    mutationFn: (data: CatalogItemCreateRequest) => catalogApi.createItem(data),
    onSuccess: () => {
      toast.success(CATALOG_COPY.admin.drawer.saveSuccess)
      invalidateCatalogQueries()
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || 'Failed to create item.'
      toast.error('Error', msg)
    },
  })

  const updateItemMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CatalogItemUpdateRequest }) =>
      catalogApi.updateItem(id, data),
    onSuccess: () => {
      toast.success(CATALOG_COPY.admin.drawer.updateSuccess)
      invalidateCatalogQueries()
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || 'Failed to update item.'
      toast.error('Error', msg)
    },
  })

  const deleteItemMutation = useMutation({
    mutationFn: (id: string) => catalogApi.deleteItem(id),
    onSuccess: () => {
      toast.success(CATALOG_COPY.admin.drawer.deleteSuccess)
      invalidateCatalogQueries()
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || 'Failed to deactivate item.'
      toast.error('Error', msg)
    },
  })

  const createCategoryMutation = useMutation({
    mutationFn: (data: CatalogCategoryCreateRequest) => catalogApi.createCategory(data),
    onSuccess: () => {
      toast.success(CATALOG_COPY.admin.categoryModal.createSuccess)
      queryClient.invalidateQueries({ queryKey: CATALOG_QUERY_KEYS.categories() })
      invalidateCatalogQueries()
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || 'Failed to create category.'
      toast.error('Error', msg)
    },
  })

  const handleExportCsv = async () => {
    try {
      const blob = await catalogApi.downloadCsvExport()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `catalog_export_${new Date().toISOString().slice(0, 10)}.csv`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      toast.error('Export Error', 'Failed to export catalog CSV.')
    }
  }

  return {
    createItem: createItemMutation.mutateAsync,
    isCreatingItem: createItemMutation.isPending,
    updateItem: updateItemMutation.mutateAsync,
    isUpdatingItem: updateItemMutation.isPending,
    deleteItem: deleteItemMutation.mutateAsync,
    isDeletingItem: deleteItemMutation.isPending,
    createCategory: createCategoryMutation.mutateAsync,
    isCreatingCategory: createCategoryMutation.isPending,
    exportCsv: handleExportCsv,
  }
}
