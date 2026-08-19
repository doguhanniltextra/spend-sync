import { useState } from 'react'
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  flexRender,
  type ColumnDef,
  type SortingState,
} from '@tanstack/react-table'
import { ArrowUpDown, ArrowUp, ArrowDown, ChevronLeft, ChevronRight, Search } from 'lucide-react'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { THRESHOLDS } from '@/constants/thresholds'
import { cn } from '@/utils/cn'

export interface DataTableProps<TData> {
  columns:          ColumnDef<TData, any>[]
  data:             TData[]
  isLoading?:       boolean
  searchPlaceholder?: string
  searchKey?:       string
  onRowClick?:      (row: TData) => void
  emptyTitle?:      string
  emptyDescription?: string
}

export function DataTable<TData>({
  columns,
  data,
  isLoading = false,
  searchPlaceholder = 'Search records...',
  onRowClick,
  emptyTitle = 'No records found',
  emptyDescription = 'There are no items matching your criteria.',
}: DataTableProps<TData>) {
  const [sorting, setSorting] = useState<SortingState>([])
  const [globalFilter, setGlobalFilter] = useState('')
  const [pageSize, setPageSize] = useState<number>(THRESHOLDS.table.defaultPageSize)

  const table = useReactTable({
    data,
    columns,
    state: {
      sorting,
      globalFilter,
    },
    onSortingChange: setSorting,
    onGlobalFilterChange: setGlobalFilter,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    initialState: {
      pagination: {
        pageSize: THRESHOLDS.table.defaultPageSize,
      },
    },
  })

  if (isLoading) {
    return (
      <div className="bg-white rounded-lg border border-slate-200 overflow-hidden shadow-2xs">
        <TableSkeleton rows={6} cols={columns.length} />
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
      {/* Search & Filter Header */}
      <div className="p-4 border-b border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3 bg-white">
        <div className="relative w-full sm:max-w-xs">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={globalFilter ?? ''}
            onChange={(e) => setGlobalFilter(e.target.value)}
            placeholder={searchPlaceholder}
            className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-md focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900 transition-all text-slate-900 placeholder:text-slate-400"
          />
        </div>

        <div className="flex items-center gap-2 text-xs text-slate-500 font-medium">
          <span>Rows per page:</span>
          <select
            value={pageSize}
            onChange={(e) => {
              const size = Number(e.target.value)
              setPageSize(size)
              table.setPageSize(size)
            }}
            className="bg-slate-50 border border-slate-200 rounded px-2 py-1 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-900 cursor-pointer"
          >
            {THRESHOLDS.table.pageSizeOptions.map((opt) => (
              <option key={opt} value={opt}>
                {opt}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      {table.getRowModel().rows.length === 0 ? (
        <div className="p-6">
          <EmptyState title={emptyTitle} description={emptyDescription} />
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
              {table.getHeaderGroups().map((headerGroup) => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map((header) => {
                    const canSort = header.column.getCanSort()
                    const isSorted = header.column.getIsSorted()

                    return (
                      <th
                        key={header.id}
                        className={cn(
                          'px-5 py-3 select-none',
                          canSort ? 'cursor-pointer hover:text-slate-800' : ''
                        )}
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        <div className="flex items-center gap-1.5">
                          {flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                          {canSort && (
                            <span className="text-slate-400">
                              {isSorted === 'asc' ? (
                                <ArrowUp className="w-3.5 h-3.5 text-slate-900" />
                              ) : isSorted === 'desc' ? (
                                <ArrowDown className="w-3.5 h-3.5 text-slate-900" />
                              ) : (
                                <ArrowUpDown className="w-3 h-3 text-slate-400" />
                              )}
                            </span>
                          )}
                        </div>
                      </th>
                    )
                  })}
                </tr>
              ))}
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {table.getRowModel().rows.map((row) => (
                <tr
                  key={row.id}
                  onClick={() => onRowClick?.(row.original)}
                  className={cn(
                    'transition-colors',
                    onRowClick ? 'cursor-pointer hover:bg-slate-50/80' : 'hover:bg-slate-50/40'
                  )}
                >
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="px-5 py-3.5 text-slate-700">
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination Footer */}
      {table.getPageCount() > 1 && (
        <div className="px-5 py-3 border-t border-slate-200 bg-slate-50/50 flex items-center justify-between text-xs text-slate-500">
          <div>
            Page <span className="font-semibold text-slate-900">{table.getState().pagination.pageIndex + 1}</span> of{' '}
            <span className="font-semibold text-slate-900">{table.getPageCount()}</span> ({data.length} total)
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage()}
              className="p-1 rounded border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              aria-label="Previous page"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage()}
              className="p-1 rounded border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              aria-label="Next page"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
