import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Eye, Send, Users, Filter, XCircle } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { StatusBadge } from '@/components/ui/Badge'
import { Select } from '@/components/ui/Select'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { DataTable } from '@/components/datatable/DataTable'
import { PurchaseOrderSummaryBar } from './components/PurchaseOrderSummaryBar'
import { PurchaseOrderDetailDrawer } from './components/PurchaseOrderDetailDrawer'
import { POCancelModal } from './components/POCancelModal'
import { usePurchaseOrders } from './hooks/usePurchaseOrders'
import { useCreatePurchaseOrder } from './hooks/useCreatePurchaseOrder'
import { useVendors } from './hooks/useVendors'
import type { PurchaseOrderSummaryResponse } from '@/types/purchasing.types'
import { formatDate } from '@/utils/date'
import { ROUTES } from '@/constants/routes'
import { PURCHASING_COPY } from './constants/purchasingCopy'

export default function PurchaseOrderListPage() {
  const navigate = useNavigate()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [vendorFilter, setVendorFilter] = useState('ALL')
  const [selectedPOId, setSelectedPOId] = useState<string | null>(null)
  const [cancelPOItem, setCancelPOItem] = useState<PurchaseOrderSummaryResponse | null>(null)

  const { orders, isLoading } = usePurchaseOrders(statusFilter, vendorFilter)
  const { vendors } = useVendors()
  const { issuePO, cancelPO, isIssuing, isCancelling } = useCreatePurchaseOrder()

  const statusOptions = [
    { value: 'ALL', label: 'All Statuses' },
    { value: 'DRAFT', label: 'Draft' },
    { value: 'ISSUED', label: 'Issued / In Transit' },
    { value: 'PARTIALLY_RECEIVED', label: 'Partially Received' },
    { value: 'FULLY_RECEIVED', label: 'Fully Received' },
    { value: 'REVISED', label: 'Revised' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ]

  const vendorOptions = [
    { value: 'ALL', label: 'All Vendors' },
    ...vendors.map((v) => ({ value: v.id, label: `${v.name} (${v.taxNumber})` })),
  ]

  const handleCancelConfirm = async (reason: string) => {
    if (!cancelPOItem) return
    await cancelPO({ id: cancelPOItem.id, payload: { cancellationReason: reason } })
    setCancelPOItem(null)
  }

  const columns = useMemo<ColumnDef<PurchaseOrderSummaryResponse>[]>(
    () => [
      {
        accessorKey: 'poNumber',
        header: PURCHASING_COPY.orders.colPoNumber,
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-bold text-slate-900 block">
              {row.original.poNumber}
            </span>
            <span className="text-[10px] font-mono text-slate-400">
              Rev {row.original.revisionNumber}
              {row.original.requisitionNumber && ` • Ref: ${row.original.requisitionNumber}`}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'vendorName',
        header: PURCHASING_COPY.orders.colVendor,
        cell: ({ row }) => (
          <div>
            <span className="font-medium text-slate-900 block">{row.original.vendorName}</span>
            <span className="text-[10px] font-mono text-slate-400">{row.original.vendorTaxNumber}</span>
          </div>
        ),
      },
      {
        accessorKey: 'costCenterName',
        header: PURCHASING_COPY.orders.colCostCenter,
        cell: ({ row }) => (
          <div>
            <span className="text-slate-800 font-medium">{row.original.costCenterName}</span>
            <span className="text-[10px] text-slate-400 block truncate max-w-xs">{row.original.deliveryFacilityName}</span>
          </div>
        ),
      },
      {
        accessorKey: 'incoterms',
        header: PURCHASING_COPY.orders.colIncoterms,
        cell: ({ row }) => (
          <span className="font-mono font-semibold text-[11px] px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
            {row.original.incoterms}
          </span>
        ),
      },
      {
        accessorKey: 'totalAmount',
        header: () => <div className="text-right">{PURCHASING_COPY.orders.colAmount}</div>,
        cell: ({ row }) => (
          <div className="text-right font-mono font-bold text-slate-900">
            <CurrencyDisplay amount={row.original.totalAmount} currency={row.original.currency as any} />
          </div>
        ),
      },
      {
        accessorKey: 'status',
        header: () => <div className="text-center">{PURCHASING_COPY.orders.colStatus}</div>,
        cell: ({ row }) => (
          <div className="text-center">
            <StatusBadge status={row.original.status} />
          </div>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: () => <div className="text-right">{PURCHASING_COPY.orders.colDate}</div>,
        cell: ({ row }) => (
          <div className="text-right text-slate-500 font-sans">
            {formatDate(row.original.issuedAt ?? row.original.createdAt)}
          </div>
        ),
      },
      {
        id: 'actions',
        header: () => <div className="text-right">{PURCHASING_COPY.orders.colActions}</div>,
        cell: ({ row }) => {
          const isDraft = row.original.status === 'DRAFT'
          const canCancel = isDraft || row.original.status === 'ISSUED'

          return (
            <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setSelectedPOId(row.original.id)}
                title={PURCHASING_COPY.orders.inspectAction}
              >
                <Eye className="w-3.5 h-3.5" />
              </Button>

              {isDraft && (
                <Button
                  size="sm"
                  onClick={() => issuePO(row.original.id)}
                  isLoading={isIssuing}
                  leftIcon={<Send className="w-3.5 h-3.5" />}
                  className="bg-slate-900 text-white hover:bg-slate-800"
                >
                  {PURCHASING_COPY.orders.issueAction}
                </Button>
              )}

              {canCancel && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCancelPOItem(row.original)}
                  className="text-red-600 hover:bg-red-50 border-red-200"
                  title={PURCHASING_COPY.orders.cancelAction}
                >
                  <XCircle className="w-3.5 h-3.5" />
                </Button>
              )}
            </div>
          )
        },
      },
    ],
    [isIssuing]
  )

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {PURCHASING_COPY.orders.pageTitle}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {PURCHASING_COPY.orders.pageSubtitle}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            onClick={() => navigate(ROUTES.purchasing.vendors)}
            leftIcon={<Users className="w-4 h-4" />}
          >
            {PURCHASING_COPY.orders.vendorsCTA}
          </Button>

          <Button
            onClick={() => navigate(ROUTES.purchasing.new)}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            {PURCHASING_COPY.orders.createCTA}
          </Button>
        </div>
      </div>

      {/* KPI Metrics Cockpit Bar */}
      <PurchaseOrderSummaryBar
        orders={orders}
        activeStatus={statusFilter}
        onStatusSelect={(st) => setStatusFilter(st)}
      />

      {/* Filter Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-white p-3.5 rounded-lg border border-slate-200 shadow-2xs">
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400 shrink-0" />
          <span className="text-xs font-semibold text-slate-700">Filters:</span>
        </div>

        <Select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          options={statusOptions}
        />

        <Select
          value={vendorFilter}
          onChange={(e) => setVendorFilter(e.target.value)}
          options={vendorOptions}
        />
      </div>

      {/* Orders Table */}
      <DataTable
        columns={columns}
        data={orders}
        isLoading={isLoading}
        searchPlaceholder={PURCHASING_COPY.orders.searchPlaceholder}
        onRowClick={(row) => setSelectedPOId(row.id)}
        emptyTitle={PURCHASING_COPY.orders.emptyTitle}
        emptyDescription={PURCHASING_COPY.orders.emptyDesc}
      />

      {/* Master-Detail Drawer */}
      <PurchaseOrderDetailDrawer
        poId={selectedPOId}
        onClose={() => setSelectedPOId(null)}
      />

      {/* Cancel Modal */}
      <POCancelModal
        isOpen={Boolean(cancelPOItem)}
        poNumber={cancelPOItem?.poNumber || ''}
        isLoading={isCancelling}
        onClose={() => setCancelPOItem(null)}
        onConfirm={handleCancelConfirm}
      />
    </div>
  )
}
