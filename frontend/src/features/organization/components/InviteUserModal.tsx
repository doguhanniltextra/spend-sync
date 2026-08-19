import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import type { InviteSubAccountRequest, LegalEntityResponse, RoleType } from '@/types/organization.types'

const inviteSchema = z.object({
  email:               z.string().email('Valid work email address required'),
  targetLegalEntityId: z.string().min(1, 'Target legal entity is required'),
  expirationHours:     z.number().min(1).max(168).optional(),
})

type FormValues = z.infer<typeof inviteSchema>

interface InviteUserModalProps {
  isOpen:        boolean
  onClose:       () => void
  legalEntities: LegalEntityResponse[]
  onSubmit:      (values: InviteSubAccountRequest) => Promise<any>
}

const AVAILABLE_ROLES: { id: RoleType; label: string }[] = [
  { id: 'APPROVER', label: 'Department Approver / Manager' },
  { id: 'REQUISITIONER', label: 'Requisitioner / Request Creator' },
  { id: 'PROCUREMENT', label: 'Procurement Specialist' },
  { id: 'AP_SPECIALIST', label: 'AP & Invoice Match Specialist' },
  { id: 'ACCOUNT_USER', label: 'Finance & Ledger Accountant' },
  { id: 'FACILITY_USER', label: 'Dock Receiving Clerk' },
]

export function InviteUserModal({
  isOpen,
  onClose,
  legalEntities,
  onSubmit,
}: InviteUserModalProps) {
  const [selectedRoles, setSelectedRoles] = useState<Set<RoleType>>(new Set(['REQUISITIONER']))

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(inviteSchema),
    defaultValues: {
      email:               '',
      targetLegalEntityId: legalEntities[0]?.id || '',
      expirationHours:     48,
    },
  })

  const toggleRole = (role: RoleType) => {
    setSelectedRoles((prev) => {
      const next = new Set(prev)
      if (next.has(role)) next.delete(role)
      else next.add(role)
      return next
    })
  }

  const handleFormSubmit = async (values: FormValues) => {
    if (selectedRoles.size === 0) return
    await onSubmit({
      ...values,
      targetRoles: Array.from(selectedRoles),
    })
    reset()
    onClose()
  }

  const entityOptions = [
    { value: '', label: 'Select Target Legal Entity...' },
    ...legalEntities.map((e) => ({ value: e.id, label: `${e.name} (${e.companyCode})` })),
  ]

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Invite Enterprise Sub-Account"
      description="Send secure invitation link with pre-assigned RBAC roles and operating entity scope."
      maxWidth="md"
      footer={
        <>
          <Button type="button" variant="outline" size="sm" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={handleSubmit(handleFormSubmit)}
            isLoading={isSubmitting}
            disabled={selectedRoles.size === 0}
            className="bg-slate-900 text-white"
          >
            Send Invitation
          </Button>
        </>
      }
    >
      <form className="space-y-3.5 text-xs">
        <Input
          label="Recipient Work Email"
          placeholder="colleague@company.com"
          type="email"
          {...register('email')}
          error={errors.email?.message}
          required
        />

        <Select
          label="Target Legal Entity"
          {...register('targetLegalEntityId')}
          error={errors.targetLegalEntityId?.message}
          options={entityOptions}
          required
        />

        <div className="space-y-2">
          <label className="text-xs font-semibold text-slate-800 block">
            Pre-Assigned Roles (At least one required)
          </label>
          <div className="space-y-1.5 bg-slate-50 p-2.5 rounded-lg border border-slate-200">
            {AVAILABLE_ROLES.map((r) => (
              <label
                key={r.id}
                className="flex items-center gap-2 text-xs text-slate-700 cursor-pointer hover:text-slate-900"
              >
                <input
                  type="checkbox"
                  checked={selectedRoles.has(r.id)}
                  onChange={() => toggleRole(r.id)}
                  className="rounded border-slate-300 text-slate-900 focus:ring-slate-900"
                />
                <span>{r.label}</span>
              </label>
            ))}
          </div>
        </div>

        <Input
          label="Token Expiration (Hours)"
          type="number"
          placeholder="48"
          {...register('expirationHours', { valueAsNumber: true })}
          error={errors.expirationHours?.message}
        />
      </form>
    </Modal>
  )
}
