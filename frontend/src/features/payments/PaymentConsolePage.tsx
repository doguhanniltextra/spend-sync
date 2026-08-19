import { useState, useMemo } from 'react'
import { Plus, Eye, FileCode, CheckSquare, Square } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { StatusBadge } from '@/components/ui/Badge'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { DataTable } from '@/components/datatable/DataTable'
import { PaymentSummaryBar } from './components/PaymentSummaryBar'
import { PaymentBatchStatusBadge } from './components/PaymentBatchStatusBadge'
import { CreateBatchModal } from './components/CreateBatchModal'
import { PaymentBatchDetailDrawer } from './components/PaymentBatchDetailDrawer'
import { useDueInvoices } from './hooks/useDueInvoices'
import { usePaymentBatches } from './hooks/usePaymentBatches'
import type { DueInvoiceResponse, PaymentBatchResponse } from '@/types/payment.types'
import { formatDate, formatDateTime } from '@/utils/date'
import { PAYMENT_COPY } from './constants/paymentCopy'

export default function PaymentConsolePage() {
  const [activeTab, setActiveTab] = useState<'DUE' | 'BATCHES'>('DUE')
  const [selectedInvoiceIds, setSelectedInvoiceIds] = useState<Set<string>>(new Set())
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [selectedBatchId, setSelectedBatchId] = useState<string | null>(null)

  const { dueInvoices, isLoading: isLoadingDue, refetch: refetchDue } = useDueInvoices()
  const { batches, isLoading: isLoadingBatches, refetch: refetchBatches } = usePaymentBatches()

  const selectedInvoicesList = useMemo(() => {
    return dueInvoices.filter((inv) => selectedInvoiceIds.has(inv.id))
  }, [dueInvoices, selectedInvoiceIds])

  const totalSelectedAmount = useMemo(() => {
    return selectedInvoicesList.reduce((acc, inv) => acc + (inv.totalAmount || 0), 0)
  }, [selectedInvoicesList])

  const toggleSelectAll = () => {
    if (selectedInvoiceIds.size === dueInvoices.length) {
      setSelectedInvoiceIds(new Set())
    } else {
      setSelectedInvoiceIds(new Set(dueInvoices.map((i) => i.id)))
    }
  }

  const toggleSelectInvoice = (id: string) => {
    setSelectedInvoiceIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  // ── Tab 1 Columns: Due Invoices ──────────────────────────────────────────────
  const dueColumns = useMemo<ColumnDef<DueInvoiceResponse>[]>(
    () => [
      {
        id: 'select',
        header: () => (
          <div className="flex items-center">
            <button
              type="button"
              onClick={toggleSelectAll}
              className="text-slate-500 hover:text-slate-900"
            >
              {dueInvoices.length > 0 && selectedInvoiceIds.size === dueInvoices.length ? (
                <CheckSquare className="w-4 h-4 text-slate-900" />
              ) : (
                <Square className="w-4 h-4 text-slate-400" />
              )}
            </button>
          </div>
        ),
        cell: ({ row }) => (
          <div className="flex items-center" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              onClick={() => toggleSelectInvoice(row.original.id)}
              className="text-slate-500 hover:text-slate-900"
            >
              {selectedInvoiceIds.has(row.original.id) ? (
                <CheckSquare className="w-4 h-4 text-slate-900" />
              ) : (
                <Square className="w-4 h-4 text-slate-400" />
              )}
            </button>
          </div>
        ),
      },
      {
        accessorKey: 'invoiceNumber',
        header: PAYMENT_COPY.table.colInvoice,
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-bold text-slate-900 block">
              {row.original.invoiceNumber}
            </span>
            <span className="text-[10px] text-slate-400 font-mono truncate max-w-[140px] block">
              ETTN: {row.original.ettn}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'vendorName',
        header: PAYMENT_COPY.table.colVendor,
        cell: ({ row }) => (
          <span className="font-semibold text-slate-900 block">{row.original.vendorName}</span>
        ),
      },
      {
        accessorKey: 'vendorIban',
        header: PAYMENT_COPY.table.colIban,
        cell: ({ row }) => (
          <span className="font-mono text-xs text-slate-700 block truncate max-w-[180px]">
            {row.original.vendorIban || '—'}
          </span>
        ),
      },
      {
        accessorKey: 'legalEntityName',
        header: PAYMENT_COPY.table.colEntity,
        cell: ({ row }) => (
          <span className="text-slate-700 text-xs font-medium">
            {row.original.legalEntityName}
          </span>
        ),
      },
      {
        accessorKey: 'invoiceDate',
        header: PAYMENT_COPY.table.colDate,
        cell: ({ row }) => (
          <span className="text-slate-500 text-xs">{formatDate(row.original.invoiceDate)}</span>
        ),
      },
      {
        accessorKey: 'totalAmount',
        header: () => <div className="text-right">{PAYMENT_COPY.table.colAmount}</div>,
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
        accessorKey: 'matchStatus',
        header: () => <div className="text-center">{PAYMENT_COPY.table.colMatch}</div>,
        cell: ({ row }) => (
          <div className="text-center">
            <StatusBadge status={row.original.matchStatus as any} />
          </div>
        ),
      },
    ],
    [dueInvoices, selectedInvoiceIds]
  )

  // ── Tab 2 Columns: Payment Batches ───────────────────────────────────────────
  const batchColumns = useMemo<ColumnDef<PaymentBatchResponse>[]>(
    () => [
      {
        accessorKey: 'batchNumber',
        header: PAYMENT_COPY.table.colBatchNo,
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-bold text-slate-900 block">
              {row.original.batchNumber}
            </span>
            <span className="text-[10px] text-slate-500 font-mono">
              Rail: {row.original.paymentMethod}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'legalEntityName',
        header: PAYMENT_COPY.table.colEntity,
        cell: ({ row }) => (
          <span className="font-medium text-slate-900 block text-xs">{row.original.legalEntityName}</span>
        ),
      },
      {
        accessorKey: 'itemCount',
        header: () => <div className="text-center">{PAYMENT_COPY.table.colItemCount}</div>,
        cell: ({ row }) => (
          <div className="text-center font-mono font-semibold text-slate-700">
            {row.original.itemCount} Invoices
          </div>
        ),
      },
      {
        accessorKey: 'totalAmount',
        header: () => <div className="text-right">{PAYMENT_COPY.table.colTotal}</div>,
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
        accessorKey: 'createdAt',
        header: PAYMENT_COPY.table.colCreatedDate,
        cell: ({ row }) => (
          <span className="text-slate-500 text-xs font-mono">{formatDateTime(row.original.createdAt)}</span>
        ),
      },
      {
        accessorKey: 'status',
        header: () => <div className="text-center">{PAYMENT_COPY.table.colStatus}</div>,
        cell: ({ row }) => (
          <div className="text-center">
            <PaymentBatchStatusBadge status={row.original.status} />
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
              onClick={() => setSelectedBatchId(row.original.id)}
              leftIcon={<Eye className="w-3.5 h-3.5" />}
            >
              {PAYMENT_COPY.table.inspectBatchAction}
            </Button>
          </div>
        ),
      },
    ],
    []
  )

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-20">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {PAYMENT_COPY.header.title}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {PAYMENT_COPY.header.subtitle}
          </p>
        </div>

        {activeTab === 'DUE' && selectedInvoiceIds.size > 0 && (
          <Button
            onClick={() => setCreateModalOpen(true)}
            leftIcon={<Plus className="w-4 h-4" />}
            className="bg-slate-900 text-white"
          >
            {PAYMENT_COPY.header.createBatchCTA} ({selectedInvoiceIds.size})
          </Button>
        )}
      </div>

      {/* KPI Metrics Summary Bar */}
      <PaymentSummaryBar dueInvoices={dueInvoices} batches={batches} />

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-6">
        <button
          type="button"
          onClick={() => setActiveTab('DUE')}
          className={`pb-3 font-bold text-xs transition-colors border-b-2 ${
            activeTab === 'DUE'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          {PAYMENT_COPY.tabs.dueInvoices} ({dueInvoices.length})
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('BATCHES')}
          className={`pb-3 font-bold text-xs flex items-center gap-1.5 transition-colors border-b-2 ${
            activeTab === 'BATCHES'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          <FileCode className="w-3.5 h-3.5" />
          {PAYMENT_COPY.tabs.batches} ({batches.length})
        </button>
      </div>

      {/* Tab 1: Pending Invoices Table */}
      {activeTab === 'DUE' && (
        <>
          <DataTable
            columns={dueColumns}
            data={dueInvoices}
            isLoading={isLoadingDue}
            onRowClick={(row) => toggleSelectInvoice(row.id)}
            searchPlaceholder={PAYMENT_COPY.table.searchPlaceholder}
            emptyTitle={PAYMENT_COPY.table.emptyDueTitle}
            emptyDescription={PAYMENT_COPY.table.emptyDueDesc}
          />

          {/* Sticky Floating Selection Action Bar */}
          {selectedInvoiceIds.size > 0 && (
            <div className="fixed bottom-6 left-1/2 transform -translate-x-1/2 z-40 bg-slate-900 text-white px-6 py-3.5 rounded-full shadow-2xl flex items-center gap-6 animate-slide-up border border-slate-800">
              <div className="flex items-center gap-3">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
                <span className="text-xs font-semibold">
                  <strong>{selectedInvoiceIds.size}</strong> Invoices Selected
                </span>
                <span className="text-slate-500">|</span>
                <span className="text-xs font-mono font-bold text-emerald-300">
                  <CurrencyDisplay amount={totalSelectedAmount} currency={dueInvoices[0]?.currency as any} />
                </span>
              </div>

              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setSelectedInvoiceIds(new Set())}
                  className="text-white border-slate-700 hover:bg-slate-800"
                >
                  Clear Selection
                </Button>
                <Button
                  size="sm"
                  onClick={() => setCreateModalOpen(true)}
                  leftIcon={<Plus className="w-3.5 h-3.5" />}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold"
                >
                  Create Batch ({selectedInvoiceIds.size})
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Tab 2: Batches Archive Table */}
      {activeTab === 'BATCHES' && (
        <DataTable
          columns={batchColumns}
          data={batches}
          isLoading={isLoadingBatches}
          onRowClick={(row) => setSelectedBatchId(row.id)}
          searchPlaceholder="Search batch number, rail, or legal entity..."
          emptyTitle={PAYMENT_COPY.table.emptyBatchesTitle}
          emptyDescription={PAYMENT_COPY.table.emptyBatchesDesc}
        />
      )}

      {/* Create Batch Modal */}
      <CreateBatchModal
        selectedInvoices={selectedInvoicesList}
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={() => {
          setSelectedInvoiceIds(new Set())
          refetchDue()
          refetchBatches()
          setActiveTab('BATCHES')
        }}
      />

      {/* Batch Detail Inspector Drawer */}
      <PaymentBatchDetailDrawer
        batchId={selectedBatchId}
        onClose={() => {
          setSelectedBatchId(null)
          refetchBatches()
        }}
      />
    </div>
  )
}
