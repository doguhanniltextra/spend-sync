import { useState, useMemo } from 'react'
import { Plus, Edit3, CheckCircle2, XCircle } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { DataTable } from '@/components/datatable/DataTable'
import { LegalEntityModal } from './LegalEntityModal'
import { useLegalEntities } from '../hooks/useLegalEntities'
import type { LegalEntityResponse } from '@/types/organization.types'
import { formatDate } from '@/utils/date'
import { ORG_COPY } from '../constants/organizationCopy'

export function LegalEntitiesTab() {
  const {
    legalEntities,
    isLoading,
    createLegalEntity,
    updateLegalEntity,
    toggleEntityStatus,
  } = useLegalEntities()

  const [modalOpen, setModalOpen] = useState(false)
  const [editingEntity, setEditingEntity] = useState<LegalEntityResponse | null>(null)

  const handleEdit = (entity: LegalEntityResponse) => {
    setEditingEntity(entity)
    setModalOpen(true)
  }

  const handleOpenCreate = () => {
    setEditingEntity(null)
    setModalOpen(true)
  }

  const handleModalSubmit = async (values: any) => {
    if (editingEntity) {
      await updateLegalEntity({ id: editingEntity.id, payload: values })
    } else {
      await createLegalEntity(values)
    }
  }

  const columns = useMemo<ColumnDef<LegalEntityResponse>[]>(
    () => [
      {
        accessorKey: 'companyCode',
        header: ORG_COPY.legalEntity.code,
        cell: ({ row }) => (
          <div className="flex items-center gap-3">
            <span className="px-2.5 py-1 rounded-md bg-slate-100 border border-slate-200 text-slate-800 font-mono font-bold text-xs shrink-0 whitespace-nowrap">
              {row.original.companyCode}
            </span>
            <div className="min-w-0">
              <strong className="text-slate-900 block font-sans text-xs">{row.original.name}</strong>
              <span className="text-[10px] text-slate-400 font-mono block">
                {row.original.registeredAddress}
              </span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'taxNumber',
        header: ORG_COPY.legalEntity.taxNumber,
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-bold text-slate-900 text-xs">{row.original.taxNumber}</span>
            {row.original.taxOffice && (
              <span className="text-[10px] text-slate-400 block">{row.original.taxOffice}</span>
            )}
          </div>
        ),
      },
      {
        accessorKey: 'baseCurrency',
        header: ORG_COPY.legalEntity.currency,
        cell: ({ row }) => (
          <span className="inline-flex items-center gap-1 font-mono font-bold text-xs bg-slate-100 px-2 py-0.5 rounded text-slate-800">
            {row.original.baseCurrency} ({row.original.country})
          </span>
        ),
      },
      {
        accessorKey: 'isActive',
        header: () => <div className="text-center">{ORG_COPY.legalEntity.status}</div>,
        cell: ({ row }) => (
          <div className="text-center" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              onClick={() => toggleEntityStatus({ id: row.original.id, active: !row.original.isActive })}
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
        header: 'Registered On',
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
              {ORG_COPY.legalEntity.editAction}
            </Button>
          </div>
        ),
      },
    ],
    [toggleEntityStatus]
  )

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-base font-bold text-slate-900">Corporate Legal Entities</h2>
          <p className="text-xs text-slate-500">
            Registered corporate subsidiaries, statutory tax identifiers, and functional currencies.
          </p>
        </div>
        <Button
          size="sm"
          onClick={handleOpenCreate}
          leftIcon={<Plus className="w-3.5 h-3.5" />}
          className="bg-slate-900 text-white"
        >
          {ORG_COPY.legalEntity.addCTA}
        </Button>
      </div>

      <DataTable
        columns={columns}
        data={legalEntities}
        isLoading={isLoading}
        searchPlaceholder="Search legal entity, company code, VKN, or currency..."
        emptyTitle={ORG_COPY.legalEntity.emptyTitle}
        emptyDescription={ORG_COPY.legalEntity.emptyDesc}
      />

      <LegalEntityModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        initial={editingEntity}
        onSubmit={handleModalSubmit}
      />
    </div>
  )
}
