import { useState, useMemo } from 'react'
import { Check, X, Eye, RefreshCw } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { DataTable } from '@/components/datatable/DataTable'
import { ApprovalSummaryBar } from './components/ApprovalSummaryBar'
import { ApprovalDecisionModal, type DecisionMode } from './components/ApprovalDecisionModal'
import { ApprovalDetailDrawer } from './components/ApprovalDetailDrawer'
import { BulkApprovalActionBar } from './components/BulkApprovalActionBar'
import { useApprovalQueue } from './hooks/useApprovalQueue'
import { useApprovalDecision } from './hooks/useApprovalDecision'
import { useEffectiveLimit } from './hooks/useEffectiveLimit'
import type { RequisitionDetailResponse } from '@/types/requisition.types'
import { formatTimeAgo } from '@/utils/date'
import { APPROVAL_COPY } from './constants/approvalCopy'

export default function ApprovalQueuePage() {
  const { pendingList, totalExposure, isLoading, isRefetching, refetch } = useApprovalQueue()
  const { isCFOorRoot, limitData } = useEffectiveLimit()
  const { approveRequisition, rejectRequisition, isApproving, isRejecting } = useApprovalDecision()

  // Selected item for decision modal
  const [modalMode, setModalMode] = useState<DecisionMode>('APPROVE')
  const [modalRequisition, setModalRequisition] = useState<RequisitionDetailResponse | null>(null)

  // Selected item for detail drawer
  const [drawerRequisition, setDrawerRequisition] = useState<RequisitionDetailResponse | null>(null)

  // Bulk selection tracking
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [isBulkApproving, setIsBulkApproving] = useState(false)

  // Open Decision Modal
  const handleOpenDecision = (req: RequisitionDetailResponse, mode: DecisionMode) => {
    setModalMode(mode)
    setModalRequisition(req)
  }

  // Handle Decision Confirmation
  const handleConfirmDecision = async (text: string) => {
    if (!modalRequisition) return
    if (modalMode === 'APPROVE') {
      await approveRequisition({
        id: modalRequisition.id,
        payload: { decisionNote: text || undefined },
      })
    } else {
      await rejectRequisition({
        id: modalRequisition.id,
        payload: { rejectionReason: text },
      })
    }
  }

  // Handle Bulk Approval
  const handleBulkApprove = async () => {
    setIsBulkApproving(true)
    try {
      for (const id of selectedIds) {
        await approveRequisition({
          id,
          payload: { decisionNote: 'Batch approved via Manager Workbench' },
        })
      }
      setSelectedIds(new Set())
    } finally {
      setIsBulkApproving(false)
    }
  }

  // Toggle selection
  const handleToggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const selectedItemsValue = useMemo(() => {
    return pendingList
      .filter((p) => selectedIds.has(p.id))
      .reduce((acc, p) => acc + (p.totalAmount ?? p.totalEstimatedAmount ?? 0), 0)
  }, [pendingList, selectedIds])

  const columns = useMemo<ColumnDef<RequisitionDetailResponse>[]>(
    () => [
      {
        id: 'select',
        header: () => null,
        cell: ({ row }) => (
          <input
            type="checkbox"
            checked={selectedIds.has(row.original.id)}
            onChange={(e) => {
              e.stopPropagation()
              handleToggleSelect(row.original.id)
            }}
            className="w-4 h-4 rounded border-slate-300 text-slate-900 focus:ring-slate-900 cursor-pointer"
          />
        ),
      },
      {
        accessorKey: 'requisitionNumber',
        header: APPROVAL_COPY.queue.colPrNumber,
        cell: ({ row }) => {
          const prNum = row.original.requisitionNumber ?? row.original.prNumber
          return (
            <div>
              <span className="font-mono font-bold text-slate-900 block">
                {prNum}
              </span>
            </div>
          )
        },
      },
      {
        accessorKey: 'title',
        header: APPROVAL_COPY.queue.colTitle,
        cell: ({ row }) => (
          <div className="max-w-xs truncate">
            <span className="font-medium text-slate-900 block">{row.original.title}</span>
            <span className="text-[10px] text-slate-400 font-normal truncate block">
              {row.original.justification || 'Standard business purchase'}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'requisitionerName',
        header: APPROVAL_COPY.queue.colRequester,
        cell: ({ row }) => {
          const reqName = row.original.requisitionerName ?? row.original.requesterName
          return (
            <div>
              <span className="text-slate-800 font-medium">{reqName}</span>
              <span className="text-[10px] font-mono text-slate-400 block">
                {row.original.costCenterCode} • {row.original.costCenterName}
              </span>
            </div>
          )
        },
      },
      {
        id: 'approvalLevel',
        header: APPROVAL_COPY.queue.colLevel,
        cell: ({ row }) => {
          const currentStep = row.original.approvalSteps?.find((s) => s.status === 'PENDING')
          return (
            <span className="inline-flex items-center gap-1 font-mono text-[11px] px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
              Step {currentStep?.stepOrder ?? 1} (L{currentStep?.approvalLevel ?? 1})
            </span>
          )
        },
      },
      {
        accessorKey: 'totalAmount',
        header: () => (
          <div className="text-right">{APPROVAL_COPY.queue.colAmount}</div>
        ),
        cell: ({ row }) => (
          <div className="text-right font-mono font-bold text-slate-900">
            <CurrencyDisplay
              amount={row.original.totalAmount ?? row.original.totalEstimatedAmount ?? 0}
              currency={row.original.currency as any}
            />
          </div>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: () => <div className="text-right">{APPROVAL_COPY.queue.colAge}</div>,
        cell: ({ row }) => (
          <div className="text-right text-slate-500 font-sans">
            {formatTimeAgo(row.original.createdAt)}
          </div>
        ),
      },
      {
        id: 'actions',
        header: () => <div className="text-right">{APPROVAL_COPY.queue.colActions}</div>,
        cell: ({ row }) => (
          <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setDrawerRequisition(row.original)}
              title={APPROVAL_COPY.queue.inspect}
            >
              <Eye className="w-3.5 h-3.5" />
            </Button>

            <Button
              variant="outline"
              size="sm"
              onClick={() => handleOpenDecision(row.original, 'REJECT')}
              className="text-red-700 hover:bg-red-50 border-red-200"
              title={APPROVAL_COPY.queue.quickReject}
            >
              <X className="w-3.5 h-3.5" />
            </Button>

            <Button
              size="sm"
              onClick={() => handleOpenDecision(row.original, 'APPROVE')}
              className="bg-slate-900 hover:bg-slate-800 text-white"
              leftIcon={<Check className="w-3.5 h-3.5" />}
            >
              {APPROVAL_COPY.queue.quickApprove}
            </Button>
          </div>
        ),
      },
    ],
    [selectedIds]
  )

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-20">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {APPROVAL_COPY.queue.pageTitle}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {APPROVAL_COPY.queue.pageSubtitle}
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          isLoading={isRefetching}
          leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
        >
          {APPROVAL_COPY.queue.refreshCTA}
        </Button>
      </div>

      {/* Summary Pulse KPI Bar */}
      <ApprovalSummaryBar
        pendingCount={pendingList.length}
        totalExposure={totalExposure}
        isCFOorRoot={isCFOorRoot}
        limitData={limitData}
      />

      {/* DataTable */}
      <DataTable
        columns={columns}
        data={pendingList}
        isLoading={isLoading}
        searchPlaceholder={APPROVAL_COPY.queue.searchPlaceholder}
        onRowClick={(row) => setDrawerRequisition(row)}
        emptyTitle={APPROVAL_COPY.queue.emptyTitle}
        emptyDescription={APPROVAL_COPY.queue.emptyDesc}
      />

      {/* Decision Modal */}
      <ApprovalDecisionModal
        isOpen={Boolean(modalRequisition)}
        mode={modalMode}
        requisition={modalRequisition}
        isLoading={isApproving || isRejecting}
        onClose={() => setModalRequisition(null)}
        onConfirm={handleConfirmDecision}
      />

      {/* Inspection Drawer */}
      <ApprovalDetailDrawer
        isOpen={Boolean(drawerRequisition)}
        requisition={drawerRequisition}
        onClose={() => setDrawerRequisition(null)}
        onApprove={(req) => {
          setDrawerRequisition(null)
          handleOpenDecision(req, 'APPROVE')
        }}
        onReject={(req) => {
          setDrawerRequisition(null)
          handleOpenDecision(req, 'REJECT')
        }}
      />

      {/* Floating Bulk Action Bar */}
      <BulkApprovalActionBar
        selectedCount={selectedIds.size}
        totalValue={selectedItemsValue}
        isLoading={isBulkApproving}
        onApproveAll={handleBulkApprove}
        onClear={() => setSelectedIds(new Set())}
      />
    </div>
  )
}
