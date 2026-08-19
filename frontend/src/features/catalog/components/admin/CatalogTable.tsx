import { useState } from 'react'
import { Search, Edit2, Trash2, Tag, Star, Package } from 'lucide-react'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { ContractStatusBadge } from '../common/ContractStatusBadge'
import { useCatalogSearch } from '../../hooks/useCatalogSearch'
import { useCatalogAdmin } from '../../hooks/useCatalogAdmin'
import { CATALOG_COPY } from '../../constants/catalogCopy'
import type { CatalogItemResponse } from '../../types/catalog.types'

interface CatalogTableProps {
  onEditItem: (item: CatalogItemResponse) => void
}

export function CatalogTable({ onEditItem }: CatalogTableProps) {
  const [search, setSearch] = useState('')
  const [activeOnly, setActiveOnly] = useState(true)
  const [page, setPage] = useState(0)

  const { data: searchResult, isLoading } = useCatalogSearch({
    q: search,
    activeOnly,
    page,
    size: 15,
  })

  const { deleteItem, isDeletingItem } = useCatalogAdmin()

  const items = searchResult?.content || []
  const totalElements = searchResult?.totalElements || 0
  const totalPages = searchResult?.totalPages || 0

  const handleDelete = async (item: CatalogItemResponse) => {
    if (window.confirm(`Are you sure you want to deactivate ${item.name} (${item.itemCode})?`)) {
      await deleteItem(item.id)
    }
  }

  return (
    <div className="bg-white border border-slate-200 rounded-xl shadow-2xs overflow-hidden">
      {/* Table Header Controls */}
      <div className="p-4 border-b border-slate-200 bg-white flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(0)
            }}
            placeholder={CATALOG_COPY.picker.searchPlaceholder}
            className="w-full pl-9 pr-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-lg text-slate-900 placeholder:text-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900"
          />
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
          <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-slate-700">
            <input
              type="checkbox"
              checked={activeOnly}
              onChange={(e) => {
                setActiveOnly(e.target.checked)
                setPage(0)
              }}
              className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 border-slate-300"
            />
            <span>Active Items Only</span>
          </label>
        </div>
      </div>

      {/* Table Content */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
            <tr>
              <th className="px-4 py-3 min-w-[140px]">{CATALOG_COPY.admin.table.colCode}</th>
              <th className="px-4 py-3 min-w-[240px]">{CATALOG_COPY.admin.table.colName}</th>
              <th className="px-3 py-3 min-w-[180px]">{CATALOG_COPY.admin.table.colCategory}</th>
              <th className="px-3 py-3 min-w-[180px]">{CATALOG_COPY.admin.table.colVendor}</th>
              <th className="px-3 py-3 text-right min-w-[130px]">{CATALOG_COPY.admin.table.colPrice}</th>
              <th className="px-3 py-3 min-w-[120px]">{CATALOG_COPY.admin.table.colUom}</th>
              <th className="px-3 py-3 min-w-[160px]">{CATALOG_COPY.admin.table.colContract}</th>
              <th className="px-4 py-3 text-center w-24">{CATALOG_COPY.admin.table.colActions}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {isLoading ? (
              <tr>
                <td colSpan={8} className="text-center py-12 text-slate-400">
                  Loading catalog...
                </td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={8} className="text-center py-12">
                  <Package className="w-10 h-10 text-slate-300 mx-auto mb-2 stroke-1" />
                  <p className="font-semibold text-slate-800 text-sm">
                    {CATALOG_COPY.admin.table.empty}
                  </p>
                  <p className="text-xs text-slate-500 mt-1">
                    {CATALOG_COPY.admin.table.emptyAction}
                  </p>
                </td>
              </tr>
            ) : (
              items.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                  {/* Code */}
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1.5">
                      <span className="font-mono font-bold text-slate-900 bg-slate-100 px-2 py-0.5 rounded text-xs">
                        {item.itemCode}
                      </span>
                      {item.isPreferred && (
                        <span title="Preferred Item">
                          <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                        </span>
                      )}
                    </div>
                  </td>

                  {/* Name & Description */}
                  <td className="px-4 py-3">
                    <div className="font-semibold text-slate-900 text-sm">{item.name}</div>
                    {item.description && (
                      <div className="text-slate-500 text-xs line-clamp-1 mt-0.5">{item.description}</div>
                    )}
                  </td>

                  {/* Category */}
                  <td className="px-3 py-3 text-slate-600">
                    {item.categoryFullPath ? (
                      <div className="flex items-center gap-1 text-xs truncate max-w-[180px]">
                        <Tag className="w-3 h-3 text-slate-400 shrink-0" />
                        <span className="truncate">{item.categoryFullPath}</span>
                      </div>
                    ) : (
                      <span className="text-slate-400">-</span>
                    )}
                  </td>

                  {/* Vendor */}
                  <td className="px-3 py-3 text-slate-700 font-medium">
                    {item.preferredVendorName ? (
                      <div>
                        <div>{item.preferredVendorName}</div>
                        {item.preferredVendorTier && (
                          <span className="text-[10px] font-mono text-slate-400">
                            {item.preferredVendorTier}
                          </span>
                        )}
                      </div>
                    ) : (
                      <span className="text-slate-400">-</span>
                    )}
                  </td>

                  {/* Price */}
                  <td className="px-3 py-3 text-right font-mono font-bold text-slate-900">
                    <CurrencyDisplay amount={item.unitPrice} currency={item.currency as any} />
                  </td>

                  {/* UOM & VAT */}
                  <td className="px-3 py-3 text-slate-600">
                    <div>{item.unitOfMeasure}</div>
                    <span className="text-[11px] text-slate-400 font-mono">
                      +%{Math.round(item.vatRate * 100)} VAT
                    </span>
                  </td>

                  {/* Contract Status */}
                  <td className="px-3 py-3">
                    <ContractStatusBadge
                      validUntil={item.validUntil}
                      contractAlert={item.contractAlert}
                    />
                    {item.contractReference && (
                      <div className="text-[10px] font-mono text-slate-400 mt-0.5">
                        {item.contractReference}
                      </div>
                    )}
                  </td>

                  {/* Actions */}
                  <td className="px-4 py-3 text-center">
                    <div className="flex items-center justify-center gap-1">
                      <button
                        type="button"
                        onClick={() => onEditItem(item)}
                        className="p-1.5 text-slate-500 hover:text-indigo-600 rounded-lg hover:bg-indigo-50 transition-colors"
                        title="Edit"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>

                      <button
                        type="button"
                        disabled={isDeletingItem || !item.isActive}
                        onClick={() => handleDelete(item)}
                        className="p-1.5 text-slate-400 hover:text-red-600 rounded-lg hover:bg-red-50 disabled:opacity-30 transition-colors"
                        title="Deactivate"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div className="px-4 py-3 border-t border-slate-200 bg-slate-50/70 flex items-center justify-between text-xs text-slate-600">
        <div>
          Total <span className="font-semibold text-slate-900 font-mono">{totalElements}</span> records
        </div>

        {totalPages > 1 && (
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-2.5 py-1 rounded border border-slate-200 font-medium hover:bg-white disabled:opacity-40"
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
              className="px-2.5 py-1 rounded border border-slate-200 font-medium hover:bg-white disabled:opacity-40"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
