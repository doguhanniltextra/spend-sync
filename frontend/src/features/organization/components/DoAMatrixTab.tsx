import { useState, useMemo } from 'react'
import { Plus, CheckCircle2, XCircle } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { DataTable } from '@/components/datatable/DataTable'
import { SetApprovalLimitModal } from './SetApprovalLimitModal'
import { useDoAMatrix } from '../hooks/useDoAMatrix'
import { useLegalEntities } from '../hooks/useLegalEntities'
import { useCostCenters } from '../hooks/useCostCenters'
import { useUserManagement } from '../hooks/useUserManagement'
import type { ApprovalLimitResponse } from '@/types/organization.types'
import { formatDate } from '@/utils/date'
import { ORG_COPY } from '../constants/organizationCopy'

export function DoAMatrixTab() {
  const { legalEntities } = useLegalEntities()
  const { costCenters } = useCostCenters()
  const { users } = useUserManagement()

  const {
    approvalLimits,
    isLoading,
    setApprovalLimit,
    toggleLimitStatus,
  } = useDoAMatrix()

  const [modalOpen, setModalOpen] = useState(false)

  const columns = useMemo<ColumnDef<ApprovalLimitResponse>[]>(
    () => [
      {
        accessorKey: 'userFullName',
        header: ORG_COPY.doa.user,
        cell: ({ row }) => (
          <div>
            <strong className="text-slate-900 font-semibold block text-xs">{row.original.userFullName}</strong>
            <span className="text-[10px] text-slate-400 font-mono">{row.original.userEmail}</span>
          </div>
        ),
      },
      {
        accessorKey: 'approvalLevel',
        header: ORG_COPY.doa.level,
        cell: ({ row }) => (
          <span className="inline-flex items-center gap-1 font-mono font-bold text-xs bg-slate-900 text-white px-2 py-0.5 rounded">
            Tier {row.original.approvalLevel}
          </span>
        ),
      },
      {
        accessorKey: 'legalEntityName',
        header: ORG_COPY.doa.entity,
        cell: ({ row }) => (
          <span className="text-slate-700 text-xs font-medium">{row.original.legalEntityName}</span>
        ),
      },
      {
        accessorKey: 'costCenterName',
        header: ORG_COPY.doa.costCenter,
        cell: ({ row }) => (
          <span className="text-slate-800 text-xs">
            {row.original.costCenterName || <span className="text-slate-400 italic">Entire Legal Entity</span>}
          </span>
        ),
      },
      {
        accessorKey: 'maxAmount',
        header: () => <div className="text-right">{ORG_COPY.doa.maxAmount}</div>,
        cell: ({ row }) => (
          <div className="text-right">
            {row.original.isUnlimited ? (
              <span className="font-bold text-emerald-600 font-mono text-xs uppercase tracking-wider">
                Unlimited (CFO/Board)
              </span>
            ) : (
              <CurrencyDisplay
                amount={row.original.maxAmount ?? 0}
                currency={row.original.currency as any}
                className="font-mono font-bold text-slate-900 text-xs"
              />
            )}
          </div>
        ),
      },
      {
        accessorKey: 'isActive',
        header: () => <div className="text-center">{ORG_COPY.doa.status}</div>,
        cell: ({ row }) => (
          <div className="text-center" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              onClick={() => toggleLimitStatus({ id: row.original.id, active: !row.original.isActive })}
              className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold cursor-pointer transition-colors ${
                row.original.isActive
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100'
                  : 'bg-slate-100 text-slate-500 border border-slate-200 hover:bg-slate-200'
              }`}
            >
              {row.original.isActive ? (
                <>
                  <CheckCircle2 className="w-3 h-3 text-emerald-600" /> Active
                </>
              ) : (
                <>
                  <XCircle className="w-3 h-3 text-slate-400" /> Inactive
                </>
              )}
            </button>
          </div>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: 'Configured On',
        cell: ({ row }) => <span className="text-slate-500 text-xs">{formatDate(row.original.createdAt)}</span>,
      },
    ],
    [toggleLimitStatus]
  )

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-bold text-slate-900">Delegation of Authority (DoA) Signing Matrix</h2>
          <p className="text-xs text-slate-500">
            Define statutory signing authority ceilings and automated multi-tier approval routing.
          </p>
        </div>

        <Button
          size="sm"
          onClick={() => setModalOpen(true)}
          leftIcon={<Plus className="w-3.5 h-3.5" />}
          className="bg-slate-900 text-white"
        >
          {ORG_COPY.doa.addCTA}
        </Button>
      </div>

      <DataTable
        columns={columns}
        data={approvalLimits}
        isLoading={isLoading}
        searchPlaceholder="Search approver name, email, level, or entity..."
        emptyTitle={ORG_COPY.doa.emptyTitle}
        emptyDescription={ORG_COPY.doa.emptyDesc}
      />

      <SetApprovalLimitModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        legalEntities={legalEntities}
        costCenters={costCenters}
        users={users}
        onSubmit={setApprovalLimit}
      />
    </div>
  )
}
