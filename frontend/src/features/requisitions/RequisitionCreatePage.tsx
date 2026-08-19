import { useState, useMemo, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ArrowLeft, Send } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import { Select } from '@/components/ui/Select'
import { LineItemsEditorTable } from './components/LineItemsEditorTable'
import { BudgetImpactPreview } from './components/BudgetImpactPreview'
import { useOrgContext } from './hooks/useOrgContext'
import { useCreateRequisition } from './hooks/useCreateRequisition'
import type { CreateLineItemRequest } from '@/types/requisition.types'
import { ROUTES } from '@/constants/routes'
import { REQUISITION_COPY } from './constants/requisitionCopy'

const createRequisitionSchema = z.object({
  legalEntityId:      z.string().min(1, 'Legal Entity is required'),
  costCenterId:       z.string().min(1, 'Cost Center is required'),
  deliveryFacilityId: z.string().min(1, 'Delivery Facility is required'),
  title:              z.string().min(3, 'Title must be at least 3 characters'),
  justification:      z.string().min(10, 'Business justification must be at least 10 characters'),
  currency:           z.string().min(3, 'Currency is required'),
})

type FormValues = z.infer<typeof createRequisitionSchema>

export default function RequisitionCreatePage() {
  const navigate = useNavigate()
  const { legalEntities, costCenters, facilities, budgetSummary, isLoading: isOrgLoading } = useOrgContext()
  const { createRequisition, isCreating } = useCreateRequisition()

  const [lineItems, setLineItems] = useState<CreateLineItemRequest[]>([
    {
      itemDescription: '',
      itemCategory:    'IT_HARDWARE',
      quantity:        1,
      unitOfMeasure:   'PIECE',
      unitPrice:       0,
    },
  ])

  const {
    register,
    handleSubmit,
    control,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(createRequisitionSchema),
    defaultValues: {
      legalEntityId:      '',
      costCenterId:       '',
      deliveryFacilityId: '',
      title:              '',
      justification:      '',
      currency:           'TRY',
    },
  })

  const selectedLegalEntityId = watch('legalEntityId')
  const selectedCostCenterId  = watch('costCenterId')
  const selectedCurrency      = watch('currency') || 'TRY'

  // Update default currency and reset cost center / facility when Legal Entity changes
  const selectedLegalEntity = useMemo(() => {
    return legalEntities.find((le) => le.id === selectedLegalEntityId)
  }, [legalEntities, selectedLegalEntityId])

  useEffect(() => {
    if (selectedLegalEntity) {
      setValue('currency', selectedLegalEntity.baseCurrency)
    }
  }, [selectedLegalEntity, setValue])

  // Locate active budget pool for the selected cost center
  const activeBudgetPool = useMemo(() => {
    if (!budgetSummary?.pools || !selectedCostCenterId) return null
    return budgetSummary.pools.find((p) => p.costCenterId === selectedCostCenterId)
  }, [budgetSummary, selectedCostCenterId])

  // Filter cost centers and facilities by selected Legal Entity
  const filteredCostCenters = useMemo(() => {
    if (!selectedLegalEntityId) return costCenters
    return costCenters.filter((cc) => cc.legalEntityId === selectedLegalEntityId)
  }, [costCenters, selectedLegalEntityId])

  const filteredFacilities = useMemo(() => {
    if (!selectedLegalEntityId) return facilities
    return facilities.filter((f) => f.legalEntityId === selectedLegalEntityId)
  }, [facilities, selectedLegalEntityId])

  // Calculate total PR estimated amount
  const totalPRAmount = useMemo(() => {
    return lineItems.reduce(
      (acc, item) => acc + (item.quantity || 0) * (item.unitPrice || 0),
      0
    )
  }, [lineItems])

  const onSubmit = async (values: FormValues) => {
    // Validate line items
    const invalidItems = lineItems.some(
      (item) => !item.itemDescription.trim() || item.quantity <= 0 || item.unitPrice <= 0
    )
    if (invalidItems) {
      alert('Please fill out all line item descriptions, valid quantities, and unit prices.')
      return
    }

    try {
      await createRequisition({
        ...values,
        lineItems,
      })
      navigate(ROUTES.requisitions.root)
    } catch {
      // Error toast is already handled in useCreateRequisition hook
    }
  }

  const legalEntityOptions = [
    { value: '', label: 'Select Legal Entity...' },
    ...legalEntities.map((le) => ({ value: le.id, label: `${le.name} (${le.companyCode} • ${le.baseCurrency})` })),
  ]

  const costCenterOptions = [
    { value: '', label: 'Select Cost Center...' },
    ...filteredCostCenters.map((cc) => ({ value: cc.id, label: `${cc.code} - ${cc.name}` })),
  ]

  const facilityOptions = [
    { value: '', label: 'Select Delivery Facility...' },
    ...filteredFacilities.map((f) => ({ value: f.id, label: `${f.name} (${f.facilityCode})` })),
  ]

  const currencyOptions = [
    { value: 'TRY', label: 'TRY - Turkish Lira' },
    { value: 'USD', label: 'USD - US Dollar' },
    { value: 'EUR', label: 'EUR - Euro' },
    { value: 'GBP', label: 'GBP - British Pound' },
  ]

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-16">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div>
          <button
            onClick={() => navigate(ROUTES.requisitions.root)}
            type="button"
            className="inline-flex items-center gap-1 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors mb-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            {REQUISITION_COPY.create.backToList}
          </button>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {REQUISITION_COPY.create.pageTitle}
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {REQUISITION_COPY.create.pageSubtitle}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Section 1: Organizational Context & Scope */}
        <div className="bg-white rounded-lg p-6 border border-slate-200 shadow-2xs space-y-4">
          <h3 className="text-sm font-bold text-slate-900 border-b border-slate-100 pb-3">
            {REQUISITION_COPY.create.sectionHeader}
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Controller
              name="legalEntityId"
              control={control}
              render={({ field }) => (
                <Select
                  label={REQUISITION_COPY.create.legalEntityLabel}
                  options={legalEntityOptions}
                  error={errors.legalEntityId?.message}
                  disabled={isOrgLoading}
                  required
                  {...field}
                />
              )}
            />

            <Controller
              name="costCenterId"
              control={control}
              render={({ field }) => (
                <Select
                  label={REQUISITION_COPY.create.costCenterLabel}
                  options={costCenterOptions}
                  error={errors.costCenterId?.message}
                  disabled={isOrgLoading}
                  required
                  {...field}
                />
              )}
            />

            <Controller
              name="deliveryFacilityId"
              control={control}
              render={({ field }) => (
                <Select
                  label={REQUISITION_COPY.create.facilityLabel}
                  options={facilityOptions}
                  error={errors.deliveryFacilityId?.message}
                  disabled={isOrgLoading}
                  required
                  {...field}
                />
              )}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 pt-2">
            <div className="sm:col-span-3">
              <Input
                label={REQUISITION_COPY.create.titleLabel}
                placeholder={REQUISITION_COPY.create.titlePlaceholder}
                error={errors.title?.message}
                required
                {...register('title')}
              />
            </div>

            <div className="sm:col-span-1">
              <Controller
                name="currency"
                control={control}
                render={({ field }) => (
                  <Select
                    label={REQUISITION_COPY.create.currencyLabel}
                    options={currencyOptions}
                    error={errors.currency?.message}
                    required
                    {...field}
                  />
                )}
              />
            </div>
          </div>

          <div>
            <Textarea
              label={REQUISITION_COPY.create.justificationLabel}
              placeholder={REQUISITION_COPY.create.justificationPlaceholder}
              error={errors.justification?.message}
              rows={3}
              required
              {...register('justification')}
            />
          </div>
        </div>

        {/* Section 2: Dynamic Line Items Breakdown */}
        <LineItemsEditorTable
          items={lineItems}
          currency={selectedCurrency}
          onChange={setLineItems}
        />

        {/* Section 3: Real-Time Budget Check Simulation */}
        {selectedCostCenterId && (
          <div className="space-y-2">
            <h3 className="text-sm font-bold text-slate-900">
              {REQUISITION_COPY.create.sectionBudget}
            </h3>
            <BudgetImpactPreview
              pool={activeBudgetPool}
              prAmount={totalPRAmount}
              currency={selectedCurrency}
            />
          </div>
        )}

        {/* Form Actions Footer */}
        <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(ROUTES.requisitions.root)}
            disabled={isCreating}
          >
            {REQUISITION_COPY.create.cancelButton}
          </Button>

          <Button
            type="submit"
            isLoading={isCreating}
            leftIcon={<Send className="w-4 h-4" />}
          >
            {isCreating
              ? REQUISITION_COPY.create.submitting
              : REQUISITION_COPY.create.submitButton}
          </Button>
        </div>
      </form>
    </div>
  )
}
