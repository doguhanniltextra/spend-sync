import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import type { CostCenterResponse, CreateCostCenterRequest, LegalEntityResponse, UserResponse } from '@/types/organization.types'

const costCenterSchema = z.object({
  legalEntityId: z.string().min(1, 'Legal Entity is required'),
  code:          z.string().min(2, 'Code is required').max(50),
  name:          z.string().min(2, 'Name must be at least 2 characters').max(255),
  managerUserId: z.string().optional(),
})

type FormValues = z.infer<typeof costCenterSchema>

interface CostCenterModalProps {
  isOpen:        boolean
  onClose:       () => void
  initial?:      CostCenterResponse | null
  legalEntities: LegalEntityResponse[]
  users:         UserResponse[]
  onSubmit:      (values: CreateCostCenterRequest) => Promise<any>
}

export function CostCenterModal({
  isOpen,
  onClose,
  initial,
  legalEntities,
  users,
  onSubmit,
}: CostCenterModalProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(costCenterSchema),
    defaultValues: {
      legalEntityId: legalEntities[0]?.id || '',
      code:          '',
      name:          '',
      managerUserId: '',
    },
  })

  useEffect(() => {
    if (initial) {
      reset({
        legalEntityId: initial.legalEntityId,
        code:          initial.code,
        name:          initial.name,
        managerUserId: initial.managerUserId || '',
      })
    } else {
      reset({
        legalEntityId: legalEntities[0]?.id || '',
        code:          '',
        name:          '',
        managerUserId: '',
      })
    }
  }, [initial, reset, isOpen, legalEntities])

  const handleFormSubmit = async (values: FormValues) => {
    await onSubmit({
      ...values,
      managerUserId: values.managerUserId ? (values.managerUserId as any) : undefined,
    })
    onClose()
  }

  const entityOptions = [
    { value: '', label: 'Select Legal Entity...' },
    ...legalEntities.map((e) => ({ value: e.id, label: `${e.name} (${e.companyCode})` })),
  ]

  const userOptions = [
    { value: '', label: 'Select Department Manager (Optional)...' },
    ...users.map((u) => ({ value: u.id, label: `${u.fullName} (${u.email})` })),
  ]

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={initial ? 'Edit Cost Center' : 'Create New Cost Center'}
      description="Define operational department unit, ledger code, and assigning line manager."
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
            className="bg-slate-900 text-white"
          >
            {initial ? 'Save Changes' : 'Create Cost Center'}
          </Button>
        </>
      }
    >
      <form className="space-y-3.5 text-xs">
        <Select
          label="Parent Legal Entity"
          {...register('legalEntityId')}
          error={errors.legalEntityId?.message}
          options={entityOptions}
          disabled={Boolean(initial)}
          required
        />

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <Input
              label="Code"
              placeholder="e.g. CC-ENG-101"
              {...register('code')}
              error={errors.code?.message}
              required
            />
          </div>
          <div className="sm:col-span-2">
            <Input
              label="Department / Unit Name"
              placeholder="e.g. Core Engineering & R&D"
              {...register('name')}
              error={errors.name?.message}
              required
            />
          </div>
        </div>

        <Select
          label="Department Lead / Line Manager"
          {...register('managerUserId')}
          options={userOptions}
        />
      </form>
    </Modal>
  )
}
