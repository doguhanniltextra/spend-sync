import { useState, useMemo } from 'react'
import { Plus, Edit3, CheckCircle2, XCircle, MapPin } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { Select } from '@/components/ui/Select'
import { DataTable } from '@/components/datatable/DataTable'
import { FacilityModal } from './FacilityModal'
import { useFacilities } from '../hooks/useFacilities'
import { useLegalEntities } from '../hooks/useLegalEntities'
import type { FacilityResponse } from '@/types/organization.types'
import { formatDate } from '@/utils/date'
import { ORG_COPY } from '../constants/organizationCopy'

export function FacilitiesTab() {
  const { legalEntities } = useLegalEntities()
  const [selectedEntityFilter, setSelectedEntityFilter] = useState<string>('')

  const {
    facilities,
    isLoading,
    createFacility,
    updateFacility,
    toggleFacilityStatus,
  } = useFacilities(selectedEntityFilter || undefined)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingFacility, setEditingFacility] = useState<FacilityResponse | null>(null)

  const handleEdit = (facility: FacilityResponse) => {
    setEditingFacility(facility)
    setModalOpen(true)
  }

  const handleOpenCreate = () => {
    setEditingFacility(null)
    setModalOpen(true)
  }

  const handleModalSubmit = async (values: any) => {
    if (editingFacility) {
      await updateFacility({ id: editingFacility.id, payload: values })
    } else {
      await createFacility(values)
    }
  }

  const columns = useMemo<ColumnDef<FacilityResponse>[]>(
    () => [
      {
        accessorKey: 'facilityCode',
        header: ORG_COPY.facility.code,
        cell: ({ row }) => (
          <div className="flex items-center gap-2">
            <span className="font-mono font-bold text-xs bg-amber-50 text-amber-800 border border-amber-200 px-2 py-0.5 rounded">
              {row.original.facilityCode}
            </span>
            <div>
              <strong className="text-slate-900 font-sans text-xs block">{row.original.name}</strong>
              <span className="text-[10px] text-slate-500 flex items-center gap-1">
                <MapPin className="w-2.5 h-2.5 text-slate-400" />
                {row.original.shippingAddress}
              </span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'facilityType',
        header: ORG_COPY.facility.type,
        cell: ({ row }) => (
          <span className="inline-flex items-center gap-1 font-mono text-[11px] font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
            {row.original.facilityType}
          </span>
        ),
      },
      {
        accessorKey: 'legalEntityName',
        header: 'Parent Entity',
        cell: ({ row }) => (
          <span className="text-slate-700 text-xs font-medium">{row.original.legalEntityName}</span>
        ),
      },
      {
        accessorKey: 'contactPerson',
        header: ORG_COPY.facility.contact,
        cell: ({ row }) => (
          <div>
            <span className="text-slate-900 text-xs block">
              {row.original.contactPerson || <span className="text-slate-400 italic">Unassigned</span>}
            </span>
            {row.original.contactPhone && (
              <span className="text-[10px] text-slate-400 font-mono">{row.original.contactPhone}</span>
            )}
          </div>
        ),
      },
      {
        accessorKey: 'isActive',
        header: () => <div className="text-center">{ORG_COPY.facility.status}</div>,
        cell: ({ row }) => (
          <div className="text-center" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              onClick={() => toggleFacilityStatus({ id: row.original.id, active: !row.original.isActive })}
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
              {ORG_COPY.facility.editAction}
            </Button>
          </div>
        ),
      },
    ],
    [toggleFacilityStatus]
  )

  const entityFilterOptions = [
    { value: '', label: 'All Legal Entities' },
    ...legalEntities.map((e) => ({ value: e.id, label: `${e.name} (${e.companyCode})` })),
  ]

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-bold text-slate-900">Logistics Facilities & Receiving Docks</h2>
          <p className="text-xs text-slate-500">
            Physical receiving destinations, shipping addresses, and site contact managers.
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
            {ORG_COPY.facility.addCTA}
          </Button>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={facilities}
        isLoading={isLoading}
        searchPlaceholder="Search facility code, name, address, or contact..."
        emptyTitle={ORG_COPY.facility.emptyTitle}
        emptyDescription={ORG_COPY.facility.emptyDesc}
      />

      <FacilityModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        initial={editingFacility}
        legalEntities={legalEntities}
        onSubmit={handleModalSubmit}
      />
    </div>
  )
}
