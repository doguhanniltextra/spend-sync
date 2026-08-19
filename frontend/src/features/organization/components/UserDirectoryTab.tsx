import { useState, useMemo } from 'react'
import { UserPlus, KeyRound, Shield, CheckCircle2, XCircle, Trash2, Mail } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/Button'
import { DataTable } from '@/components/datatable/DataTable'
import { UserRoleDrawer } from './UserRoleDrawer'
import { InviteUserModal } from './InviteUserModal'
import { RequisitionerLinkModal } from './RequisitionerLinkModal'
import { useUserManagement } from '../hooks/useUserManagement'
import { useLegalEntities } from '../hooks/useLegalEntities'
import type { UserResponse, SubAccountInvitationResponse } from '@/types/organization.types'
import { formatDate } from '@/utils/date'
import { ORG_COPY } from '../constants/organizationCopy'

export function UserDirectoryTab() {
  const { legalEntities } = useLegalEntities()
  const {
    users,
    isLoadingUsers,
    invitations,
    isLoadingInvites,
    updateRoles,
    updateLegalEntities,
    toggleUserStatus,
    inviteUser,
    generateLink,
    revokeInvitation,
  } = useUserManagement()

  const [activeSubTab, setActiveSubTab] = useState<'USERS' | 'INVITATIONS'>('USERS')
  const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null)
  const [inviteModalOpen, setInviteModalOpen] = useState(false)
  const [linkModalOpen, setLinkModalOpen] = useState(false)

  // ── Sub-Tab 1: Users Columns ───────────────────────────────────────────────
  const userColumns = useMemo<ColumnDef<UserResponse>[]>(
    () => [
      {
        accessorKey: 'fullName',
        header: ORG_COPY.user.name,
        cell: ({ row }) => (
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-full bg-slate-900 text-white font-bold flex items-center justify-center text-xs shrink-0">
              {row.original.firstName?.[0] || 'U'}
            </div>
            <div>
              <strong className="text-slate-900 font-sans text-xs block">{row.original.fullName}</strong>
              <span className="text-[10px] text-slate-400 font-mono">{row.original.email}</span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'roles',
        header: ORG_COPY.user.roles,
        cell: ({ row }) => (
          <div className="flex flex-wrap gap-1 max-w-md">
            {(row.original.roles || []).map((r) => (
              <span
                key={r}
                className="font-mono text-[10px] font-semibold bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-slate-200"
              >
                {r.replace(/^ROLE_/, '')}
              </span>
            ))}
          </div>
        ),
      },
      {
        accessorKey: 'jobTitle',
        header: ORG_COPY.user.jobTitle,
        cell: ({ row }) => (
          <span className="text-slate-700 text-xs">
            {row.original.jobTitle || <span className="text-slate-400 italic">—</span>}
          </span>
        ),
      },
      {
        accessorKey: 'isActive',
        header: () => <div className="text-center">{ORG_COPY.user.status}</div>,
        cell: ({ row }) => (
          <div className="text-center" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              onClick={() => toggleUserStatus({ id: row.original.id, active: !row.original.isActive })}
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
                  <XCircle className="w-3 h-3 text-slate-400" /> Suspended
                </>
              )}
            </button>
          </div>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: 'Registered',
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
              onClick={() => setSelectedUser(row.original)}
              leftIcon={<Shield className="w-3 h-3" />}
            >
              {ORG_COPY.user.editRolesAction}
            </Button>
          </div>
        ),
      },
    ],
    [toggleUserStatus]
  )

  // ── Sub-Tab 2: Invitations Columns ─────────────────────────────────────────
  const invitationColumns = useMemo<ColumnDef<SubAccountInvitationResponse>[]>(
    () => [
      {
        accessorKey: 'email',
        header: 'Invited Recipient',
        cell: ({ row }) => (
          <div className="flex items-center gap-2">
            <Mail className="w-3.5 h-3.5 text-slate-400" />
            <strong className="text-slate-900 font-mono text-xs">{row.original.email}</strong>
          </div>
        ),
      },
      {
        accessorKey: 'targetRoles',
        header: 'Pre-Allocated Roles',
        cell: ({ row }) => (
          <div className="flex flex-wrap gap-1">
            {(row.original.targetRoles || []).map((r) => (
              <span
                key={r}
                className="font-mono text-[10px] bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-slate-200"
              >
                {r}
              </span>
            ))}
          </div>
        ),
      },
      {
        accessorKey: 'expiresAt',
        header: 'Expires At',
        cell: ({ row }) => <span className="text-slate-500 text-xs font-mono">{formatDate(row.original.expiresAt)}</span>,
      },
      {
        id: 'actions',
        header: () => <div className="text-right">Actions</div>,
        cell: ({ row }) => (
          <div className="flex items-center justify-end gap-2" onClick={(e) => e.stopPropagation()}>
            <Button
              size="sm"
              variant="danger"
              onClick={() => revokeInvitation(row.original.id)}
              leftIcon={<Trash2 className="w-3 h-3" />}
            >
              Revoke
            </Button>
          </div>
        ),
      },
    ],
    [revokeInvitation]
  )

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-bold text-slate-900">User Directory & Sub-Account Governance</h2>
          <p className="text-xs text-slate-500">
            Control employee RBAC roles, multi-entity transaction boundaries, and invitation links.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={() => setLinkModalOpen(true)}
            leftIcon={<KeyRound className="w-3.5 h-3.5" />}
          >
            {ORG_COPY.user.linkCTA}
          </Button>

          <Button
            size="sm"
            onClick={() => setInviteModalOpen(true)}
            leftIcon={<UserPlus className="w-3.5 h-3.5" />}
            className="bg-slate-900 text-white"
          >
            {ORG_COPY.user.inviteCTA}
          </Button>
        </div>
      </div>

      {/* Sub-Tabs: Active Users vs Outbound Invites */}
      <div className="flex border-b border-slate-200 gap-4">
        <button
          type="button"
          onClick={() => setActiveSubTab('USERS')}
          className={`pb-2.5 font-bold text-xs transition-colors border-b-2 ${
            activeSubTab === 'USERS'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          Active Corporate Accounts ({users.length})
        </button>

        <button
          type="button"
          onClick={() => setActiveSubTab('INVITATIONS')}
          className={`pb-2.5 font-bold text-xs transition-colors border-b-2 ${
            activeSubTab === 'INVITATIONS'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          Pending Invitations ({invitations.length})
        </button>
      </div>

      {activeSubTab === 'USERS' ? (
        <DataTable
          columns={userColumns}
          data={users}
          isLoading={isLoadingUsers}
          searchPlaceholder="Search user name, email, or role..."
          emptyTitle={ORG_COPY.user.emptyTitle}
          emptyDescription={ORG_COPY.user.emptyDesc}
        />
      ) : (
        <DataTable
          columns={invitationColumns}
          data={invitations}
          isLoading={isLoadingInvites}
          searchPlaceholder="Search invited email..."
          emptyTitle="No Pending Invitations"
          emptyDescription="All issued invitation tokens have been activated or expired."
        />
      )}

      {/* Role Assignment Drawer */}
      <UserRoleDrawer
        user={selectedUser}
        legalEntities={legalEntities}
        isOpen={Boolean(selectedUser)}
        onClose={() => setSelectedUser(null)}
        onUpdateRoles={async (roles) => {
          if (!selectedUser) return
          await updateRoles({ id: selectedUser.id, payload: { roles } })
        }}
        onUpdateEntities={async (entityIds) => {
          if (!selectedUser) return
          await updateLegalEntities({ id: selectedUser.id, payload: { legalEntityIds: entityIds as any } })
        }}
      />

      {/* Invite Modal */}
      <InviteUserModal
        isOpen={inviteModalOpen}
        onClose={() => setInviteModalOpen(false)}
        legalEntities={legalEntities}
        onSubmit={inviteUser}
      />

      {/* Requisitioner Passkey Modal */}
      <RequisitionerLinkModal
        isOpen={linkModalOpen}
        onClose={() => setLinkModalOpen(false)}
        legalEntities={legalEntities}
        onGenerate={generateLink}
      />
    </div>
  )
}
