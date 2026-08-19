import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, ArrowLeft, Filter } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { Select } from '@/components/ui/Select'
import { DataTable } from '@/components/datatable/DataTable'
import { VendorStatusBadge, VendorTierBadge, EInvoiceBadge } from './components/VendorStatusBadge'
import { VendorCreateModal } from './components/VendorCreateModal'
import { VendorDetailDrawer } from './components/VendorDetailDrawer'
import { useVendors, useCreateVendor } from './hooks/useVendors'
import type { VendorResponse, VendorStatus, VendorCategory } from '@/types/purchasing.types'
import { ROUTES } from '@/constants/routes'
import { PURCHASING_COPY } from './constants/purchasingCopy'

export default function VendorListPage() {
  const navigate = useNavigate()
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL')
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [selectedVendor, setSelectedVendor] = useState<VendorResponse | null>(null)

  const { vendors, isLoading } = useVendors(
    statusFilter as VendorStatus | 'ALL',
    categoryFilter as VendorCategory | 'ALL'
  )
  const { updateStatus, isUpdatingStatus } = useCreateVendor()

  const handleStatusChange = async (vendor: VendorResponse, newStatus: VendorStatus) => {
    await updateStatus({ id: vendor.id, payload: { status: newStatus } })
  }

  const columns = useMemo<ColumnDef<VendorResponse>[]>(
    () => [
      {
        accessorKey: 'name',
        header: PURCHASING_COPY.vendors.colName,
        cell: ({ row }) => (
          <div>
            <span className="font-semibold text-slate-900 block">{row.original.name}</span>
            <div className="flex items-center gap-2 mt-0.5">
              <span className="text-[10px] font-mono font-bold text-slate-500">
                VKN: {row.original.taxNumber}
              </span>
              <span className="text-[10px] text-slate-400">({row.original.taxOffice})</span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'category',
        header: PURCHASING_COPY.vendors.colCategory,
        cell: ({ row }) => (
          <span className="text-slate-700 text-xs font-medium">
            {row.original.category.replace(/_/g, ' ')}
          </span>
        ),
      },
      {
        accessorKey: 'tier',
        header: PURCHASING_COPY.vendors.colTier,
        cell: ({ row }) => <VendorTierBadge tier={row.original.tier} />,
      },
      {
        accessorKey: 'isEInvoiceRegistered',
        header: PURCHASING_COPY.vendors.colEInvoice,
        cell: ({ row }) => <EInvoiceBadge isRegistered={row.original.isEInvoiceRegistered} />,
      },
      {
        accessorKey: 'paymentTerms',
        header: PURCHASING_COPY.vendors.colPaymentTerms,
        cell: ({ row }) => (
          <span className="font-mono text-xs text-slate-700">{row.original.paymentTerms}</span>
        ),
      },
      {
        accessorKey: 'iban',
        header: PURCHASING_COPY.vendors.colBank,
        cell: ({ row }) => (
          <div>
            <span className="text-slate-800 font-medium block text-[11px]">{row.original.bankName || '—'}</span>
            <span className="font-mono text-[10px] text-slate-400 truncate max-w-[150px] block">
              {row.original.iban || '—'}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'status',
        header: () => <div className="text-center">{PURCHASING_COPY.vendors.colStatus}</div>,
        cell: ({ row }) => (
          <div className="text-center">
            <VendorStatusBadge status={row.original.status} />
          </div>
        ),
      },
      {
        id: 'actions',
        header: () => <div className="text-right">{PURCHASING_COPY.vendors.colActions}</div>,
        cell: ({ row }) => (
          <div className="flex items-center justify-end gap-2" onClick={(e) => e.stopPropagation()}>
            <select
              value={row.original.status}
              disabled={isUpdatingStatus}
              onChange={(e) => handleStatusChange(row.original, e.target.value as VendorStatus)}
              className="text-[11px] bg-slate-50 border border-slate-200 rounded px-2 py-1 text-slate-700 focus:outline-none focus:ring-1 focus:ring-slate-900 cursor-pointer"
            >
              <option value="ACTIVE">Set Active</option>
              <option value="ON_HOLD">Set On Hold</option>
              <option value="BLOCKED">Set Blocked</option>
            </select>
          </div>
        ),
      },
    ],
    [isUpdatingStatus]
  )

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <button
            onClick={() => navigate(ROUTES.purchasing.root)}
            type="button"
            className="inline-flex items-center gap-1 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors mb-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            {PURCHASING_COPY.vendors.backToOrders}
          </button>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {PURCHASING_COPY.vendors.pageTitle}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {PURCHASING_COPY.vendors.pageSubtitle}
          </p>
        </div>

        <Button
          onClick={() => setCreateModalOpen(true)}
          leftIcon={<Plus className="w-4 h-4" />}
        >
          {PURCHASING_COPY.vendors.onboardCTA}
        </Button>
      </div>

      {/* Filter Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-white p-3.5 rounded-lg border border-slate-200 shadow-2xs">
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400 shrink-0" />
          <span className="text-xs font-semibold text-slate-700">Filters:</span>
        </div>

        <Select
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
          options={[
            { value: 'ALL', label: PURCHASING_COPY.vendors.filterCategory },
            ...PURCHASING_COPY.categoryOptions,
          ]}
        />

        <Select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          options={[
            { value: 'ALL', label: PURCHASING_COPY.vendors.filterStatus },
            { value: 'ACTIVE', label: 'Active Suppliers' },
            { value: 'ON_HOLD', label: 'On Hold' },
            { value: 'BLOCKED', label: 'Blocked Suppliers' },
          ]}
        />
      </div>

      {/* Vendor Table */}
      <DataTable
        columns={columns}
        data={vendors}
        isLoading={isLoading}
        onRowClick={(row) => setSelectedVendor(row)}
        searchPlaceholder={PURCHASING_COPY.vendors.searchPlaceholder}
        emptyTitle={PURCHASING_COPY.vendors.emptyTitle}
        emptyDescription={PURCHASING_COPY.vendors.emptyDesc}
      />

      {/* Onboard Modal */}
      <VendorCreateModal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
      />

      {/* Supplier Profile Drawer */}
      <VendorDetailDrawer
        vendor={selectedVendor}
        isOpen={Boolean(selectedVendor)}
        onClose={() => setSelectedVendor(null)}
      />
    </div>
  )
}
