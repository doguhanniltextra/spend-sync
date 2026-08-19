import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Eye, Filter } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { StatusBadge } from '@/components/ui/Badge'
import { Select } from '@/components/ui/Select'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { DataTable } from '@/components/datatable/DataTable'
import { RequisitionDetailDrawer } from './components/RequisitionDetailDrawer'
import { useRequisitions } from './hooks/useRequisitions'
import { useOrgContext } from './hooks/useOrgContext'
import type { RequisitionSummaryResponse } from '@/types/requisition.types'
import { formatDate } from '@/utils/date'
import { ROUTES } from '@/constants/routes'
import { REQUISITION_COPY } from './constants/requisitionCopy'

export default function RequisitionListPage() {
  const navigate = useNavigate()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [costCenterFilter, setCostCenterFilter] = useState('ALL')
  const [selectedPRId, setSelectedPRId] = useState<string | null>(null)

  const { requisitions, isLoading } = useRequisitions(statusFilter)
  const { costCenters } = useOrgContext()

  // Filter cost centers if selected
  const filteredData = useMemo(() => {
    if (costCenterFilter === 'ALL') return requisitions
    return requisitions.filter((r) => r.costCenterId === costCenterFilter)
  }, [requisitions, costCenterFilter])

  const statusOptions = [
    { value: 'ALL', label: 'All Statuses' },
    { value: 'PENDING_APPROVAL', label: 'Pending Approval' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ]

  const costCenterOptions = [
    { value: 'ALL', label: 'All Cost Centers' },
    ...costCenters.map((cc) => ({
      value: cc.id,
      label: `${cc.code} - ${cc.name}`,
    })),
  ]

  const columns = useMemo<ColumnDef<RequisitionSummaryResponse>[]>(
    () => [
      {
        accessorKey: 'prNumber',
        header: REQUISITION_COPY.list.colPrNumber,
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-bold text-slate-900 block">
              {row.original.requisitionNumber || row.original.prNumber}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'title',
        header: REQUISITION_COPY.list.colTitle,
        cell: ({ row }) => (
          <div className="max-w-xs truncate">
            <span className="font-medium text-slate-800">{row.original.title}</span>
          </div>
        ),
      },
      {
        accessorKey: 'costCenterName',
        header: REQUISITION_COPY.list.colCostCenter,
        cell: ({ row }) => (
          <div>
            <span className="text-slate-900 font-medium">{row.original.costCenterName}</span>
            {row.original.costCenterCode && (
              <span className="text-[10px] font-mono text-slate-400 block">
                {row.original.costCenterCode}
              </span>
            )}
          </div>
        ),
      },
      {
        accessorKey: 'requesterName',
        header: REQUISITION_COPY.list.colRequester,
        cell: ({ row }) => (
          <span className="text-slate-700">
            {row.original.requisitionerName || row.original.requesterName}
          </span>
        ),
      },
      {
        accessorKey: 'totalEstimatedAmount',
        header: () => (
          <div className="text-right">{REQUISITION_COPY.list.colAmount}</div>
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
        accessorKey: 'status',
        header: () => <div className="text-center">{REQUISITION_COPY.list.colStatus}</div>,
        cell: ({ row }) => (
          <div className="text-center">
            <StatusBadge status={row.original.status} />
          </div>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: () => <div className="text-right">{REQUISITION_COPY.list.colDate}</div>,
        cell: ({ row }) => (
          <div className="text-right text-slate-500 font-sans">
            {formatDate(row.original.createdAt)}
          </div>
        ),
      },
      {
        id: 'actions',
        header: () => <div className="text-right">{REQUISITION_COPY.list.colActions}</div>,
        cell: ({ row }) => (
          <div className="text-right">
            <Button
              variant="ghost"
              size="sm"
              onClick={(e) => {
                e.stopPropagation()
                setSelectedPRId(row.original.id)
              }}
              leftIcon={<Eye className="w-3.5 h-3.5" />}
            >
              {REQUISITION_COPY.list.viewDetail}
            </Button>
          </div>
        ),
      },
    ],
    []
  )

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-10">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {REQUISITION_COPY.list.pageTitle}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {REQUISITION_COPY.list.pageSubtitle}
          </p>
        </div>

        <Button
          onClick={() => navigate(ROUTES.requisitions.new)}
          leftIcon={<Plus className="w-4 h-4" />}
        >
          {REQUISITION_COPY.list.createCTA}
        </Button>
      </div>

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
          value={costCenterFilter}
          onChange={(e) => setCostCenterFilter(e.target.value)}
          options={costCenterOptions}
        />
      </div>

      {/* Requisitions Table */}
      <DataTable
        columns={columns}
        data={filteredData}
        isLoading={isLoading}
        searchPlaceholder={REQUISITION_COPY.list.searchPlaceholder}
        onRowClick={(row) => setSelectedPRId(row.id)}
        emptyTitle={REQUISITION_COPY.list.emptyTitle}
        emptyDescription={REQUISITION_COPY.list.emptyDesc}
      />

      {/* Master-Detail Slide-over Drawer */}
      <RequisitionDetailDrawer
        requisitionId={selectedPRId}
        onClose={() => setSelectedPRId(null)}
      />
    </div>
  )
}
