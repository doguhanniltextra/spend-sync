import { useState } from 'react'
import { Search, X, Package, Loader2 } from 'lucide-react'
import { useCatalogSearch } from '../../hooks/useCatalogSearch'
import { useCategoryTree } from '../../hooks/useCategoryTree'
import { useCatalogAutofill } from '../../hooks/useCatalogAutofill'
import { CategoryFilterChips } from './CategoryFilterChips'
import { CatalogItemCard } from './CatalogItemCard'
import { CATALOG_COPY } from '../../constants/catalogCopy'
import type { CatalogItemResponse, CatalogAutofillResponse } from '../../types/catalog.types'

interface CatalogPickerModalProps {
  isOpen: boolean
  onClose: () => void
  onSelect: (autofill: CatalogAutofillResponse) => void
}

export function CatalogPickerModal({ isOpen, onClose, onSelect }: CatalogPickerModalProps) {
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string | undefined>()
  const [page, setPage] = useState(0)

  const { data: categories = [] } = useCategoryTree()

  const {
    data: searchResults,
    isLoading: isSearching,
  } = useCatalogSearch({
    q: searchQuery,
    categoryId: selectedCategory,
    activeOnly: true,
    page,
    size: 9,
  })

  const { fetchAutofill, isLoading: isAutofilling } = useCatalogAutofill((data) => {
    onSelect(data)
    onClose()
  })

  if (!isOpen) return null

  const handleSelectItem = (item: CatalogItemResponse) => {
    fetchAutofill(item.id)
  }

  const items = searchResults?.content || []
  const totalElements = searchResults?.totalElements || 0
  const totalPages = searchResults?.totalPages || 0

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 sm:p-6 animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/70">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
              <Package className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {CATALOG_COPY.picker.title}
              </h3>
              <p className="text-xs text-slate-500">
                {CATALOG_COPY.picker.subtitle}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200/60 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search & Filters */}
        <div className="p-6 border-b border-slate-100 bg-white space-y-3">
          {/* Search Input */}
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value)
                setPage(0)
              }}
              placeholder={CATALOG_COPY.picker.searchPlaceholder}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 placeholder:text-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all"
              autoFocus
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Category Pills */}
          {categories.length > 0 && (
            <CategoryFilterChips
              categories={categories}
              selectedCategoryId={selectedCategory}
              onSelectCategory={(id) => {
                setSelectedCategory(id)
                setPage(0)
              }}
            />
          )}
        </div>

        {/* Catalog Items Grid */}
        <div className="flex-1 overflow-y-auto p-6 bg-slate-50/50">
          {isSearching ? (
            <div className="h-64 flex flex-col items-center justify-center text-slate-400 gap-3">
              <Loader2 className="w-8 h-8 animate-spin text-indigo-600" />
              <span className="text-xs font-medium">Scanning catalog...</span>
            </div>
          ) : items.length === 0 ? (
            <div className="h-64 flex flex-col items-center justify-center text-center p-8 bg-white border border-dashed border-slate-200 rounded-xl">
              <Package className="w-12 h-12 text-slate-300 mb-3 stroke-1" />
              <h4 className="text-sm font-semibold text-slate-800">
                {CATALOG_COPY.picker.noResults}
              </h4>
              <p className="text-xs text-slate-500 mt-1 max-w-sm">
                {CATALOG_COPY.picker.noResultsSub}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {items.map((item) => (
                <CatalogItemCard
                  key={item.id}
                  item={item}
                  onSelect={handleSelectItem}
                  isLoading={isAutofilling}
                />
              ))}
            </div>
          )}
        </div>

        {/* Footer & Pagination */}
        <div className="px-6 py-3.5 border-t border-slate-200 bg-white flex items-center justify-between text-xs text-slate-600">
          <div>
            Total <span className="font-semibold text-slate-900 font-mono">{totalElements}</span> approved items
          </div>

          {totalPages > 1 && (
            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1.5 rounded-lg border border-slate-200 font-medium hover:bg-slate-50 disabled:opacity-40 disabled:pointer-events-none"
              >
                Previous
              </button>
              <span className="font-mono text-slate-500">
                {page + 1} / {totalPages}
              </span>
              <button
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 font-medium hover:bg-slate-50 disabled:opacity-40 disabled:pointer-events-none"
              >
                Next
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
