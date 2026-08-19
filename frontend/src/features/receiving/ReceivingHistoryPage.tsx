import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Eye } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { DataTable } from '@/components/datatable/DataTable'
import { GoodsReceiptStatusBadge } from './components/GoodsReceiptStatusBadge'
import { GoodsReceiptDetailDrawer } from './components/GoodsReceiptDetailDrawer'
import { useReceivingHistory } from './hooks/useReceivingHistory'
import type { GoodsReceiptResponse } from '@/types/receiving.types'
import { formatDateTime, formatDate } from '@/utils/date'
import { ROUTES } from '@/constants/routes'
import { RECEIVING_COPY } from './constants/receivingCopy'

export default function ReceivingHistoryPage() {
  const navigate = useNavigate()
  const { receipts, isLoading } = useReceivingHistory()
  const [selectedReceiptId, setSelectedReceiptId] = useState<string | null>(null)

  const columns = useMemo<ColumnDef<GoodsReceiptResponse>[]>(
    () => [
      {
        accessorKey: 'receiptNumber',
        header: RECEIVING_COPY.history.colGrNumber,
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-bold text-slate-900 block">
              {row.original.receiptNumber}
            </span>
            <span className="text-[10px] text-slate-500 font-mono">
              PO: {row.original.poNumber}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'waybillNumber',
        header: RECEIVING_COPY.history.colWaybill,
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-semibold text-slate-900 block">
              {row.original.waybillNumber}
            </span>
            <span className="text-[10px] text-slate-500">
              Date: {formatDate(row.original.waybillDate)}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'vendorName',
        header: RECEIVING_COPY.history.colVendor,
        cell: ({ row }) => (
          <span className="font-semibold text-slate-900 block">{row.original.vendorName}</span>
        ),
      },
      {
        accessorKey: 'deliveryFacilityName',
        header: RECEIVING_COPY.history.colFacility,
        cell: ({ row }) => (
          <span className="text-slate-700 text-xs font-medium">
            {row.original.deliveryFacilityName}
          </span>
        ),
      },
      {
        accessorKey: 'receivedByUserName',
        header: RECEIVING_COPY.history.colReceivedBy,
        cell: ({ row }) => (
          <span className="text-slate-800 text-xs">{row.original.receivedByUserName}</span>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: RECEIVING_COPY.history.colDate,
        cell: ({ row }) => (
          <span className="text-slate-500 text-xs font-mono">
            {formatDateTime(row.original.createdAt)}
          </span>
        ),
      },
      {
        accessorKey: 'status',
        header: () => <div className="text-center">{RECEIVING_COPY.history.colStatus}</div>,
        cell: ({ row }) => (
          <div className="text-center">
            <GoodsReceiptStatusBadge status={row.original.status} />
          </div>
        ),
      },
      {
        id: 'actions',
        header: () => <div className="text-right">Actions</div>,
        cell: ({ row }) => (
          <div className="flex items-center justify-end gap-2" onClick={(e) => e.stopPropagation()}>
            <Button
              size="sm"
              variant="outline"
              onClick={() => setSelectedReceiptId(row.original.id)}
              leftIcon={<Eye className="w-3 h-3" />}
            >
              Inspect
            </Button>
          </div>
        ),
      },
    ],
    []
  )

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <button
            onClick={() => navigate(ROUTES.receiving.root)}
            type="button"
            className="inline-flex items-center gap-1 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors mb-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            {RECEIVING_COPY.history.backToQueue}
          </button>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {RECEIVING_COPY.history.title}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {RECEIVING_COPY.history.subtitle}
          </p>
        </div>
      </div>

      {/* History Table */}
      <DataTable
        columns={columns}
        data={receipts}
        isLoading={isLoading}
        onRowClick={(row) => setSelectedReceiptId(row.id)}
        searchPlaceholder="Search receipt number, PO reference, waybill, or supplier..."
        emptyTitle="No Goods Receipts Found"
        emptyDescription="No goods receipts have been processed at receiving docks yet."
      />

      {/* Detail Drawer */}
      <GoodsReceiptDetailDrawer
        receiptId={selectedReceiptId}
        onClose={() => setSelectedReceiptId(null)}
      />
    </div>
  )
}
