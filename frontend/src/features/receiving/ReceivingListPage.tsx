import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Truck, Plus, History } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { StatusBadge } from '@/components/ui/Badge'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { DataTable } from '@/components/datatable/DataTable'
import { ReceivingSummaryBar } from './components/ReceivingSummaryBar'
import { usePendingOrders } from './hooks/usePendingOrders'
import type { PendingPOForReceivingResponse } from '@/types/receiving.types'
import { formatDate } from '@/utils/date'
import { ROUTES } from '@/constants/routes'
import { RECEIVING_COPY } from './constants/receivingCopy'

export default function ReceivingListPage() {
  const navigate = useNavigate()
  const { pendingOrders, isLoading } = usePendingOrders()

  const columns = useMemo<ColumnDef<PendingPOForReceivingResponse>[]>(
    () => [
      {
        accessorKey: 'poNumber',
        header: RECEIVING_COPY.table.colPoNumber,
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-bold text-slate-900 block">
              {row.original.poNumber}
            </span>
            <span className="text-[10px] text-slate-500 font-mono">
              Incoterms: {row.original.incoterms}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'vendorName',
        header: RECEIVING_COPY.table.colVendor,
        cell: ({ row }) => (
          <div>
            <span className="font-semibold text-slate-900 block">{row.original.vendorName}</span>
          </div>
        ),
      },
      {
        accessorKey: 'deliveryFacilityName',
        header: RECEIVING_COPY.table.colFacility,
        cell: ({ row }) => (
          <span className="text-slate-700 text-xs font-medium">
            {row.original.deliveryFacilityName}
          </span>
        ),
      },
      {
        accessorKey: 'lineItemCount',
        header: () => <div className="text-center">{RECEIVING_COPY.table.colItems}</div>,
        cell: ({ row }) => (
          <div className="text-center font-mono font-medium text-slate-700">
            {row.original.lineItemCount} Lines
          </div>
        ),
      },
      {
        accessorKey: 'totalAmount',
        header: () => <div className="text-right">{RECEIVING_COPY.table.colAmount}</div>,
        cell: ({ row }) => (
          <div className="text-right">
            <CurrencyDisplay
              amount={row.original.totalAmount}
              currency={row.original.currency as any}
              className="font-mono font-bold text-slate-900 text-xs"
            />
          </div>
        ),
      },
      {
        accessorKey: 'issuedAt',
        header: RECEIVING_COPY.table.colIssued,
        cell: ({ row }) => (
          <span className="text-slate-500 text-xs">
            {row.original.issuedAt ? formatDate(row.original.issuedAt) : '—'}
          </span>
        ),
      },
      {
        accessorKey: 'status',
        header: () => <div className="text-center">{RECEIVING_COPY.table.colStatus}</div>,
        cell: ({ row }) => (
          <div className="text-center">
            <StatusBadge status={row.original.status} />
          </div>
        ),
      },
      {
        id: 'actions',
        header: () => <div className="text-right">{RECEIVING_COPY.table.colActions}</div>,
        cell: ({ row }) => (
          <div className="flex items-center justify-end gap-2" onClick={(e) => e.stopPropagation()}>
            <Button
              size="sm"
              onClick={() => navigate(`${ROUTES.receiving.new}?poId=${row.original.id}`)}
              leftIcon={<Truck className="w-3 h-3" />}
              className="bg-slate-900 text-white hover:bg-slate-800"
            >
              {RECEIVING_COPY.table.receiveAction}
            </Button>
          </div>
        ),
      },
    ],
    [navigate]
  )

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {RECEIVING_COPY.header.title}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {RECEIVING_COPY.header.subtitle}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={() => navigate(ROUTES.receiving.history)}
            leftIcon={<History className="w-4 h-4" />}
          >
            {RECEIVING_COPY.header.viewHistoryCTA}
          </Button>

          <Button
            onClick={() => navigate(ROUTES.receiving.new)}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            {RECEIVING_COPY.header.newReceivingCTA}
          </Button>
        </div>
      </div>

      {/* Summary KPI Cards */}
      <ReceivingSummaryBar pendingOrders={pendingOrders} />

      {/* Pending Orders Table */}
      <DataTable
        columns={columns}
        data={pendingOrders}
        isLoading={isLoading}
        onRowClick={(row) => navigate(`${ROUTES.receiving.new}?poId=${row.id}`)}
        searchPlaceholder={RECEIVING_COPY.table.searchPlaceholder}
        emptyTitle={RECEIVING_COPY.table.emptyTitle}
        emptyDescription={RECEIVING_COPY.table.emptyDesc}
      />
    </div>
  )
}
