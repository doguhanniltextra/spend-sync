import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { catalogApi } from '../services/catalogApi'
import { CATALOG_QUERY_KEYS } from './useCatalogSearch'
import { useToast } from '@/hooks/useToast'
import type { CsvImportResultDto } from '../types/catalog.types'

export function useCatalogCsvImport() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [result, setResult] = useState<CsvImportResultDto | null>(null)

  const mutation = useMutation({
    mutationFn: (file: File) => catalogApi.importCsv(file),
    onSuccess: (data) => {
      setResult(data)
      if (data.failureCount === 0) {
        toast.success('CSV Imported', `${data.successCount} items successfully imported.`)
      } else {
        toast.warning('Import Completed with Errors', `${data.successCount} items imported, ${data.failureCount} rows failed.`)
      }
      queryClient.invalidateQueries({ queryKey: CATALOG_QUERY_KEYS.all })
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || 'Error occurred while processing CSV file.'
      toast.error('Import Failed', msg)
    },
  })

  const reset = () => {
    setResult(null)
    mutation.reset()
  }

  return {
    importCsv: mutation.mutateAsync,
    isImporting: mutation.isPending,
    result,
    reset,
  }
}
