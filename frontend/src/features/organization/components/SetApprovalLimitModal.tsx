import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { MoneyInput } from '@/components/ui/MoneyInput'
import { Button } from '@/components/ui/Button'
import type {
  SetApprovalLimitRequest,
  LegalEntityResponse,
  CostCenterResponse,
  UserResponse,
} from '@/types/organization.types'

const doASchema = z.object({
  userId:        z.string().min(1, 'Approver user is required'),
  legalEntityId: z.string().min(1, 'Legal entity is required'),
  costCenterId:  z.string().optional(),
  approvalLevel: z.number().min(1).max(4),
  isUnlimited:   z.boolean(),
  maxAmount:     z.number().optional(),
  currency:      z.string().length(3),
})

type FormValues = z.infer<typeof doASchema>

interface SetApprovalLimitModalProps {
  isOpen:        boolean
  onClose:       () => void
  legalEntities: LegalEntityResponse[]
  costCenters:   CostCenterResponse[]
  users:         UserResponse[]
  onSubmit:      (values: SetApprovalLimitRequest) => Promise<any>
}

export function SetApprovalLimitModal({
  isOpen,
  onClose,
  legalEntities,
  costCenters,
  users,
  onSubmit,
}: SetApprovalLimitModalProps) {
  const [isUnlimited, setIsUnlimited] = useState(false)
  const [maxAmount, setMaxAmount] = useState<number | undefined>(50000)

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(doASchema),
    defaultValues: {
      userId:        users[0]?.id || '',
      legalEntityId: legalEntities[0]?.id || '',
      costCenterId:  '',
      approvalLevel: 1,
      isUnlimited:   false,
      currency:      'TRY',
    },
  })

  const selectedEntityId = watch('legalEntityId')

  const filteredCostCenters = costCenters.filter(
    (cc) => !selectedEntityId || cc.legalEntityId === selectedEntityId
  )

  const handleFormSubmit = async (values: FormValues) => {
    await onSubmit({
      userId:        values.userId as any,
      legalEntityId: values.legalEntityId as any,
      costCenterId:  values.costCenterId ? (values.costCenterId as any) : undefined,
      approvalLevel: values.approvalLevel,
      maxAmount:     isUnlimited ? undefined : maxAmount,
      currency:      values.currency,
    })
    reset()
    onClose()
  }

  const userOptions = [
    { value: '', label: 'Select Approver...' },
    ...users.map((u) => ({ value: u.id, label: `${u.fullName} (${u.email})` })),
  ]

  const entityOptions = [
    { value: '', label: 'Select Legal Entity...' },
    ...legalEntities.map((e) => ({ value: e.id, label: `${e.name} (${e.companyCode})` })),
  ]

  const costCenterOptions = [
    { value: '', label: 'Entire Legal Entity (All Departments)' },
    ...filteredCostCenters.map((cc) => ({ value: cc.id, label: `${cc.code} - ${cc.name}` })),
  ]

  const levelOptions = [
    { value: '1', label: 'Tier 1 — Department Lead / Manager' },
    { value: '2', label: 'Tier 2 — Director / Division Head' },
    { value: '3', label: 'Tier 3 — VP / Finance Controller' },
    { value: '4', label: 'Tier 4 — CFO / Board of Directors' },
  ]

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Configure Signing Authority (DoA)"
      description="Set monetary approval thresholds and signing levels for automated PR routing."
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
            Save Authority Limit
          </Button>
        </>
      }
    >
      <form className="space-y-3.5 text-xs">
        <Select
          label="Designated Approver"
          {...register('userId')}
          error={errors.userId?.message}
          options={userOptions}
          required
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Select
            label="Legal Entity Scope"
            {...register('legalEntityId')}
            error={errors.legalEntityId?.message}
            options={entityOptions}
            required
          />

          <Select
            label="Cost Center Scope (Optional)"
            {...register('costCenterId')}
            options={costCenterOptions}
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Select
            label="Approval Tier Level"
            value={String(watch('approvalLevel'))}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => setValue('approvalLevel', Number(e.target.value))}
            options={levelOptions}
            required
          />

          <Select
            label="Currency"
            {...register('currency')}
            options={[
              { value: 'TRY', label: 'TRY (₺)' },
              { value: 'USD', label: 'USD ($)' },
              { value: 'EUR', label: 'EUR (€)' },
              { value: 'GBP', label: 'GBP (£)' },
            ]}
            required
          />
        </div>

        {/* Unlimited Ceiling Toggle */}
        <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 space-y-3">
          <label className="flex items-center gap-2 cursor-pointer text-xs font-semibold text-slate-800">
            <input
              type="checkbox"
              checked={isUnlimited}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setIsUnlimited(e.target.checked)}
              className="rounded border-slate-300 text-slate-900 focus:ring-slate-900"
            />
            <span>Unlimited Signing Authority (CFO / Executive Sign-off)</span>
          </label>

          {!isUnlimited && (
            <div>
              <MoneyInput
                label="Maximum Single Requisition Threshold"
                value={maxAmount ?? 0}
                currency={(watch('currency') as any) || 'TRY'}
                onChange={(val: number) => setMaxAmount(val)}
              />
              <p className="text-[10px] text-slate-500 mt-1">
                Requisitions exceeding this ceiling will escalate to the next tier level.
              </p>
            </div>
          )}
        </div>
      </form>
    </Modal>
  )
}
