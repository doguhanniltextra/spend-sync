export interface CatalogItemResponse {
  id: string
  itemCode: string
  name: string
  description?: string
  categoryId?: string
  categoryName?: string
  categoryFullPath?: string
  preferredVendorId?: string
  preferredVendorName?: string
  preferredVendorTaxNumber?: string
  preferredVendorTier?: string
  unitPrice: number
  currency: string
  vatRate: number
  unitOfMeasure: string
  contractReference?: string
  validFrom?: string
  validUntil?: string
  isActive: boolean
  isPreferred: boolean
  glAccountCode?: string
  contractAlert?: string
  createdAt: string
  updatedAt: string
}

export interface CatalogItemCreateRequest {
  itemCode?: string
  name: string
  description?: string
  categoryId?: string
  preferredVendorId?: string
  unitPrice: number
  currency?: string
  vatRate?: number
  unitOfMeasure?: string
  contractReference?: string
  validFrom?: string
  validUntil?: string
  isPreferred?: boolean
  glAccountCode?: string
}

export interface CatalogItemUpdateRequest {
  name: string
  description?: string
  categoryId?: string
  preferredVendorId?: string
  unitPrice: number
  currency?: string
  vatRate?: number
  unitOfMeasure?: string
  contractReference?: string
  validFrom?: string
  validUntil?: string
  isPreferred?: boolean
  isActive?: boolean
  glAccountCode?: string
}

export interface CatalogCategoryDto {
  id: string
  code: string
  name: string
  fullPath: string
  iconCode?: string
  description?: string
  parentId?: string
  itemCount: number
  children: CatalogCategoryDto[]
}

export interface CatalogCategoryCreateRequest {
  parentId?: string
  code?: string
  name: string
  iconCode?: string
  description?: string
}

export interface LineItemSuggestion {
  description: string
  categoryCode: string
  categoryFullPath: string
  quantity: number
  unitOfMeasure: string
  unitPrice: number
  vatRate: number
  lineTotal: number
}

export interface SuggestedVendor {
  vendorId: string
  vendorName: string
  taxNumber?: string
  orderEmail?: string
  paymentTerms?: string
}

export interface BudgetHint {
  glAccountCode?: string
  suggestedCostCenterId?: string
}

export interface CatalogAutofillResponse {
  itemId: string
  itemCode: string
  itemName: string
  lineItemSuggestion: LineItemSuggestion
  suggestedVendor?: SuggestedVendor
  budgetHint?: BudgetHint
  contractAlert?: string
}

export interface TopItemMetric {
  itemCode: string
  name: string
  vendorName: string
  categoryFullPath: string
  unitPriceFormatted: string
}

export interface CatalogHealthMetricsDto {
  totalActiveItems: number
  totalCategories: number
  expiringIn30DaysCount: number
  expiringIn7DaysCount: number
  expiredItemsCount: number
  preferredItemsCount: number
  topPreferredItems: TopItemMetric[]
}

export interface CsvRowErrorDto {
  rowNumber: number
  itemCodeOrName: string
  errorMessage: string
}

export interface CsvImportResultDto {
  totalRows: number
  successCount: number
  failureCount: number
  errors: CsvRowErrorDto[]
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}
