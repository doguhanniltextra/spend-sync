import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type {
  CatalogItemResponse,
  CatalogAutofillResponse,
  CatalogCategoryDto,
  CatalogHealthMetricsDto,
  CatalogItemCreateRequest,
  CatalogItemUpdateRequest,
  CatalogCategoryCreateRequest,
  CsvImportResultDto,
  PageResponse,
} from '../types/catalog.types'

export const catalogApi = {
  searchItems: async (params?: {
    q?: string
    categoryId?: string
    activeOnly?: boolean
    page?: number
    size?: number
  }): Promise<PageResponse<CatalogItemResponse>> => {
    const res = await apiClient.get<PageResponse<CatalogItemResponse>>(ENDPOINTS.catalog.search, {
      params,
    })
    return res.data
  },

  getCategories: async (): Promise<CatalogCategoryDto[]> => {
    const res = await apiClient.get<CatalogCategoryDto[]>(ENDPOINTS.catalog.categories)
    return res.data
  },

  getItemById: async (id: string): Promise<CatalogItemResponse> => {
    const res = await apiClient.get<CatalogItemResponse>(ENDPOINTS.catalog.itemById(id))
    return res.data
  },

  getAutofill: async (id: string): Promise<CatalogAutofillResponse> => {
    const res = await apiClient.get<CatalogAutofillResponse>(ENDPOINTS.catalog.autofill(id))
    return res.data
  },

  getHealthMetrics: async (): Promise<CatalogHealthMetricsDto> => {
    const res = await apiClient.get<CatalogHealthMetricsDto>(ENDPOINTS.catalog.health)
    return res.data
  },

  createItem: async (data: CatalogItemCreateRequest): Promise<CatalogItemResponse> => {
    const res = await apiClient.post<CatalogItemResponse>(ENDPOINTS.adminCatalog.items, data)
    return res.data
  },

  updateItem: async (id: string, data: CatalogItemUpdateRequest): Promise<CatalogItemResponse> => {
    const res = await apiClient.put<CatalogItemResponse>(ENDPOINTS.adminCatalog.itemById(id), data)
    return res.data
  },

  deleteItem: async (id: string): Promise<void> => {
    await apiClient.delete<void>(ENDPOINTS.adminCatalog.itemById(id))
  },

  createCategory: async (data: CatalogCategoryCreateRequest): Promise<CatalogCategoryDto> => {
    const res = await apiClient.post<CatalogCategoryDto>(ENDPOINTS.adminCatalog.categories, data)
    return res.data
  },

  importCsv: async (file: File): Promise<CsvImportResultDto> => {
    const formData = new FormData()
    formData.append('file', file)
    const res = await apiClient.post<CsvImportResultDto>(ENDPOINTS.adminCatalog.import, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return res.data
  },

  downloadCsvExport: async (): Promise<Blob> => {
    const res = await apiClient.get(ENDPOINTS.adminCatalog.export, {
      responseType: 'blob',
    })
    return res.data
  },
}
