import { useState, useEffect } from 'react'
import { Drawer } from '@/components/ui/Drawer'
import { Button } from '@/components/ui/Button'
import type { UserResponse, LegalEntityResponse, RoleType } from '@/types/organization.types'
import { useToast } from '@/components/feedback/Toast'

interface UserRoleDrawerProps {
  user:          UserResponse | null
  legalEntities: LegalEntityResponse[]
  isOpen:        boolean
  onClose:       () => void
  onUpdateRoles: (roles: RoleType[]) => Promise<any>
  onUpdateEntities: (entityIds: string[]) => Promise<any>
}

const ALL_ROLES: { id: RoleType; label: string; desc: string }[] = [
  { id: 'ROOT_USER', label: 'Root Administrator (Super Admin)', desc: 'Full sovereign access to all system governance modules' },
  { id: 'APPROVER', label: 'Department Approver / Manager', desc: 'Authorizes departmental purchase requisitions within DoA limits' },
  { id: 'REQUISITIONER', label: 'Corporate Requisitioner', desc: 'Drafts and submits team purchase requests' },
  { id: 'PROCUREMENT', label: 'Procurement Specialist / Buyer', desc: 'Converts PRs into POs and manages vendor negotiations' },
  { id: 'AP_SPECIALIST', label: 'Accounts Payable Specialist', desc: 'Executes 3-way match reconciliation and builds payment batches' },
  { id: 'ACCOUNT_USER', label: 'Accountant / Controller', desc: 'Manages ledger pools, GL accounts, and tax reporting' },
  { id: 'FACILITY_USER', label: 'Dock Inspector / Receiving Clerk', desc: 'Inspects inbound freight and issues physical Goods Receipts' },
]

export function UserRoleDrawer({
  user,
  legalEntities,
  isOpen,
  onClose,
  onUpdateRoles,
  onUpdateEntities,
}: UserRoleDrawerProps) {
  const toast = useToast()
  const [selectedRoles, setSelectedRoles] = useState<Set<RoleType>>(new Set())
  const [selectedEntityIds, setSelectedEntityIds] = useState<Set<string>>(new Set())
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    if (user) {
      setSelectedRoles(new Set(user.roles || []))
      // default select all active entities if none set
      setSelectedEntityIds(new Set(legalEntities.map((e) => e.id)))
    }
  }, [user, legalEntities, isOpen])

  if (!user) return null

  const handleToggleRole = (role: RoleType) => {
    setSelectedRoles((prev) => {
      const next = new Set(prev)
      if (next.has(role)) next.delete(role)
      else next.add(role)
      return next
    })
  }

  const handleToggleEntity = (entityId: string) => {
    setSelectedEntityIds((prev) => {
      const next = new Set(prev)
      if (next.has(entityId)) next.delete(entityId)
      else next.add(entityId)
      return next
    })
  }

  const handleSave = async () => {
    if (selectedRoles.size === 0) {
      toast.error('User must have at least one assigned role.')
      return
    }

    try {
      setIsSaving(true)
      await onUpdateRoles(Array.from(selectedRoles))
      await onUpdateEntities(Array.from(selectedEntityIds))
      onClose()
    } catch {
      // Handled in hooks
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={`RBAC Governance: ${user.fullName}`}
      subtitle={`${user.email} • Created ${new Date(user.createdAt).toLocaleDateString()}`}
      size="lg"
      footer={
        <div className="flex items-center justify-end gap-2 w-full">
          <Button variant="outline" size="sm" onClick={onClose} disabled={isSaving}>
            Cancel
          </Button>
          <Button
            size="sm"
            onClick={handleSave}
            isLoading={isSaving}
            className="bg-slate-900 text-white"
          >
            Save Permissions
          </Button>
        </div>
      }
    >
      <div className="space-y-6 text-xs text-slate-700">
        {/* Section 1: Assigned Corporate Roles */}
        <div className="space-y-3">
          <div>
            <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
              Assigned Corporate Roles ({selectedRoles.size})
            </h4>
            <p className="text-[11px] text-slate-500">
              Grant functional authority and UI module navigation permissions.
            </p>
          </div>

          <div className="space-y-2 bg-slate-50 p-3 rounded-lg border border-slate-200">
            {ALL_ROLES.map((r) => {
              const isChecked = selectedRoles.has(r.id)
              return (
                <label
                  key={r.id}
                  className={`flex items-start gap-3 p-2 rounded border transition-colors cursor-pointer ${
                    isChecked
                      ? 'bg-white border-slate-900 shadow-2xs'
                      : 'bg-white/60 border-slate-200 hover:bg-white'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => handleToggleRole(r.id)}
                    className="mt-0.5 rounded border-slate-300 text-slate-900 focus:ring-slate-900"
                  />
                  <div>
                    <strong className="text-slate-900 font-semibold block">{r.label}</strong>
                    <span className="text-[10px] text-slate-500 leading-tight block mt-0.5">
                      {r.desc}
                    </span>
                  </div>
                </label>
              )
            })}
          </div>
        </div>

        {/* Section 2: Authorized Operating Entities */}
        <div className="space-y-3">
          <div>
            <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
              Operating Subsidiary Scope ({selectedEntityIds.size})
            </h4>
            <p className="text-[11px] text-slate-500">
              Restrict transactions, approvals, and budget visibility to designated legal entities.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 bg-slate-50 p-3 rounded-lg border border-slate-200">
            {legalEntities.map((e) => {
              const isChecked = selectedEntityIds.has(e.id)
              return (
                <label
                  key={e.id}
                  className={`flex items-center gap-2.5 p-2.5 rounded border transition-colors cursor-pointer ${
                    isChecked
                      ? 'bg-white border-slate-900'
                      : 'bg-white/60 border-slate-200 hover:bg-white'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => handleToggleEntity(e.id)}
                    className="rounded border-slate-300 text-slate-900 focus:ring-slate-900"
                  />
                  <div className="truncate">
                    <strong className="text-slate-900 font-semibold block truncate text-xs">
                      {e.name}
                    </strong>
                    <span className="text-[10px] font-mono text-slate-500">
                      Code: {e.companyCode} • {e.baseCurrency}
                    </span>
                  </div>
                </label>
              )
            })}
          </div>
        </div>
      </div>
    </Drawer>
  )
}
