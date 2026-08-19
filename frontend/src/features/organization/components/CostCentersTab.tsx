import { useState, useMemo } from 'react'
import { Plus, Edit3, CheckCircle2, XCircle } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { Select } from '@/components/ui/Select'
import { DataTable } from '@/components/datatable/DataTable'
import { CostCenterModal } from './CostCenterModal'
import { useCostCenters } from '../hooks/useCostCenters'
import { useLegalEntities } from '../hooks/useLegalEntities'
import { useUserManagement } from '../hooks/useUserManagement'
import type { CostCenterResponse } from '@/types/organization.types'
import { formatDate } from '@/utils/date'
import { ORG_COPY } from '../constants/organizationCopy'

export function CostCentersTab() {
  const { legalEntities } = useLegalEntities()
  const { users } = useUserManagement()
  const [selectedEntityFilter, setSelectedEntityFilter] = useState<string>('')

  const {
    costCenters,
    isLoading,
    createCostCenter,
    updateCostCenter,
    toggleCenterStatus,
  } = useCostCenters(selectedEntityFilter || undefined)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingCenter, setEditingCenter] = useState<CostCenterResponse | null>(null)

  const handleEdit = (center: CostCenterResponse) => {
    setEditingCenter(center)
    setModalOpen(true)
  }

  const handleOpenCreate = () => {
    setEditingCenter(null)
    setModalOpen(true)
  }

  const handleModalSubmit = async (values: any) => {
    if (editingCenter) {
      await updateCostCenter({ id: editingCenter.id, payload: values })
    } else {
      await createCostCenter(values)
    }
  }

  const columns = useMemo<ColumnDef<CostCenterResponse>[]>(
    () => [
      {
        accessorKey: 'code',
        header: ORG_COPY.costCenter.code,
        cell: ({ row }) => (
          <div className="flex items-center gap-2">
            <span className="font-mono font-bold text-xs bg-slate-100 text-slate-800 px-2 py-0.5 rounded">
              {row.original.code}
            </span>
            <strong className="text-slate-900 font-sans text-xs">{row.original.name}</strong>
          </div>
        ),
      },
      {
        accessorKey: 'legalEntityName',
        header: ORG_COPY.costCenter.entity,
        cell: ({ row }) => (
          <span className="text-slate-700 text-xs font-medium">{row.original.legalEntityName}</span>
        ),
      },
      {
        accessorKey: 'managerFullName',
        header: ORG_COPY.costCenter.manager,
        cell: ({ row }) => (
          <span className="text-slate-900 text-xs">
            {row.original.managerFullName || <span className="text-slate-400 italic">Unassigned</span>}
          </span>
        ),
      },
      {
        accessorKey: 'isActive',
        header: () => <div className="text-center">{ORG_COPY.costCenter.status}</div>,
        cell: ({ row }) => (
          <div className="text-center" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              onClick={() => toggleCenterStatus({ id: row.original.id, active: !row.original.isActive })}
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
        header: 'Created On',
        cell: ({ row }) => <span className="text-slate-500 text-xs">{formatDate(row.original.createdAt)}</span>,
      },
      {
        id: 'actions',
        header: () => <div className="text-right">Actions</div>,
        cell: ({ row }) => (
          <div className="flex items-center justify-end gap-2" onClick={(e) => e.stopPropagation()}>
            <Button
              size="sm"
              variant="outline"
              onClick={() => handleEdit(row.original)}
              leftIcon={<Edit3 className="w-3 h-3" />}
            >
              {ORG_COPY.costCenter.editAction}
            </Button>
          </div>
        ),
      },
    ],
    [toggleCenterStatus]
  )

  const entityFilterOptions = [
    { value: '', label: 'All Legal Entities' },
    ...legalEntities.map((e) => ({ value: e.id, label: `${e.name} (${e.companyCode})` })),
  ]

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-bold text-slate-900">Cost Center Department Trees</h2>
          <p className="text-xs text-slate-500">
            Allocate departmental budget ceilings and assign tier-1 approval managers.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="w-64">
            <Select
              value={selectedEntityFilter}
              onChange={(e) => setSelectedEntityFilter(e.target.value)}
              options={entityFilterOptions}
            />
          </div>

          <Button
            size="sm"
            onClick={handleOpenCreate}
            leftIcon={<Plus className="w-3.5 h-3.5" />}
            className="bg-slate-900 text-white shrink-0"
          >
            {ORG_COPY.costCenter.addCTA}
          </Button>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={costCenters}
        isLoading={isLoading}
        searchPlaceholder="Search cost center code, department name, or manager..."
        emptyTitle={ORG_COPY.costCenter.emptyTitle}
        emptyDescription={ORG_COPY.costCenter.emptyDesc}
      />

      <CostCenterModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        initial={editingCenter}
        legalEntities={legalEntities}
        users={users}
        onSubmit={handleModalSubmit}
      />
    </div>
  )
}
