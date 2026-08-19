import { useState, useMemo, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ArrowLeft, Save, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import { Select } from '@/components/ui/Select'
import { MoneyInput, CurrencyDisplay } from '@/components/ui/MoneyInput'
import { useOrgContext } from '@/features/requisitions/hooks/useOrgContext'
import { useRequisitions } from '@/features/requisitions/hooks/useRequisitions'
import { useRequisitionDetail } from '@/features/requisitions/hooks/useRequisitionDetail'
import { useVendors } from './hooks/useVendors'
import { useCreatePurchaseOrder } from './hooks/useCreatePurchaseOrder'
import type { POLineItemRequest, Incoterms, PaymentTerms } from '@/types/purchasing.types'
import { ROUTES } from '@/constants/routes'
import { PURCHASING_COPY } from './constants/purchasingCopy'

const poSchema = z.object({
  requisitionId:      z.string().optional(),
  legalEntityId:      z.string().min(1, 'Legal Entity is required'),
  costCenterId:       z.string().min(1, 'Cost Center is required'),
  deliveryFacilityId: z.string().min(1, 'Delivery Facility is required'),
  vendorId:           z.string().min(1, 'Vendor is required'),
  incoterms:          z.string().min(1, 'Incoterms required'),
  paymentTerms:       z.string().min(1, 'Payment terms required'),
  currency:           z.string().min(3, 'Currency required'),
  notes:              z.string().optional(),
})

type FormValues = z.infer<typeof poSchema>

export default function PurchaseOrderCreatePage() {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<'CONVERT' | 'DIRECT'>('CONVERT')
  const [selectedPRId, setSelectedPRId] = useState<string>('')

  const { legalEntities, costCenters, facilities, isLoading: isOrgLoading } = useOrgContext()
  const { vendors, isLoading: isVendorsLoading } = useVendors('ACTIVE')
  const { requisitions: approvedPRs } = useRequisitions('APPROVED')
  const { requisition: selectedPR } = useRequisitionDetail(selectedPRId || null)
  const { createPO, isCreating } = useCreatePurchaseOrder()

  const [lineItems, setLineItems] = useState<POLineItemRequest[]>([
    {
      itemDescription: '',
      itemCategory:    'IT_AND_SOFTWARE',
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
    resolver: zodResolver(poSchema),
    defaultValues: {
      requisitionId:      '',
      legalEntityId:      '',
      costCenterId:       '',
      deliveryFacilityId: '',
      vendorId:           '',
      incoterms:          'DAP',
      paymentTerms:       'NET_30',
      currency:           'TRY',
      notes:              '',
    },
  })

  const selectedCurrency = watch('currency') || 'TRY'

  // When an approved PR is chosen in Convert mode, auto-populate fields & line items
  useEffect(() => {
    if (selectedPR && activeTab === 'CONVERT') {
      setValue('requisitionId', selectedPR.id)
      setValue('legalEntityId', selectedPR.legalEntityId)
      setValue('costCenterId', selectedPR.costCenterId)
      setValue('deliveryFacilityId', selectedPR.deliveryFacilityId)
      setValue('currency', selectedPR.currency)
      setValue('notes', `Generated from Approved PR: ${selectedPR.requisitionNumber} (${selectedPR.title})`)

      if (selectedPR.lineItems && selectedPR.lineItems.length > 0) {
        setLineItems(
          selectedPR.lineItems.map((item) => ({
            itemDescription:       item.itemDescription,
            itemCategory:          item.itemCategory,
            quantity:              item.quantity,
            unitOfMeasure:         item.unitOfMeasure,
            unitPrice:             item.unitPrice,
            estimatedDeliveryDate: item.estimatedDeliveryDate ?? undefined,
          }))
        )
      }
    }
  }, [selectedPR, activeTab, setValue])

  const handleAddItem = () => {
    setLineItems((prev) => [
      ...prev,
      {
        itemDescription: '',
        itemCategory:    'IT_AND_SOFTWARE',
        quantity:        1,
        unitOfMeasure:   'PIECE',
        unitPrice:       0,
      },
    ])
  }

  const handleRemoveItem = (idx: number) => {
    if (lineItems.length <= 1) return
    setLineItems((prev) => prev.filter((_, i) => i !== idx))
  }

  const handleUpdateItem = (idx: number, patch: Partial<POLineItemRequest>) => {
    setLineItems((prev) => prev.map((item, i) => (i === idx ? { ...item, ...patch } : item)))
  }

  const totalAmount = useMemo(() => {
    return lineItems.reduce((acc, item) => acc + (item.quantity || 0) * (item.unitPrice || 0), 0)
  }, [lineItems])

  const onSubmit = async (values: FormValues) => {
    const invalid = lineItems.some(
      (item) => !item.itemDescription.trim() || item.quantity <= 0 || item.unitPrice <= 0
    )
    if (invalid) {
      alert('Please fill out all line item descriptions, quantities, and valid unit prices.')
      return
    }

    try {
      await createPO({
        ...values,
        requisitionId: values.requisitionId || undefined,
        incoterms:     values.incoterms as Incoterms,
        paymentTerms:  values.paymentTerms as PaymentTerms,
        lineItems,
      })
      navigate(ROUTES.purchasing.root)
    } catch {
      // Handled in mutation hook
    }
  }

  const selectedLegalEntityId = watch('legalEntityId')
  const selectedLegalEntity = useMemo(() => {
    return legalEntities.find((le) => le.id === selectedLegalEntityId)
  }, [legalEntities, selectedLegalEntityId])

  useEffect(() => {
    if (selectedLegalEntity && activeTab === 'DIRECT') {
      setValue('currency', selectedLegalEntity.baseCurrency)
    }
  }, [selectedLegalEntity, activeTab, setValue])

  const filteredCostCenters = useMemo(() => {
    if (!selectedLegalEntityId) return costCenters
    return costCenters.filter((cc) => cc.legalEntityId === selectedLegalEntityId)
  }, [costCenters, selectedLegalEntityId])

  const filteredFacilities = useMemo(() => {
    if (!selectedLegalEntityId) return facilities
    return facilities.filter((f) => f.legalEntityId === selectedLegalEntityId)
  }, [facilities, selectedLegalEntityId])

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

  const vendorOptions = [
    { value: '', label: 'Select Approved Vendor...' },
    ...vendors.map((v) => ({ value: v.id, label: `${v.name} (VKN: ${v.taxNumber} • ${v.tier})` })),
  ]

  const approvedPROptions = [
    { value: '', label: PURCHASING_COPY.create.selectPRPlaceholder },
    ...approvedPRs.map((pr) => {
      const num = pr.requisitionNumber || pr.prNumber || 'PR'
      const amt = pr.totalAmount ?? pr.totalEstimatedAmount ?? 0
      return {
        value: pr.id,
        label: `${num} • ${pr.title} (${amt.toLocaleString('tr-TR')} ${pr.currency})`,
      }
    }),
  ]

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-20">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div>
          <button
            onClick={() => navigate(ROUTES.purchasing.root)}
            type="button"
            className="inline-flex items-center gap-1 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors mb-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            {PURCHASING_COPY.create.backToList}
          </button>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {PURCHASING_COPY.create.pageTitle}
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {PURCHASING_COPY.create.pageSubtitle}
          </p>
        </div>
      </div>

      {/* Creation Mode Tabs */}
      <div className="flex border-b border-slate-200 gap-6 text-xs font-semibold">
        <button
          type="button"
          onClick={() => {
            setActiveTab('CONVERT')
            setValue('requisitionId', '')
          }}
          className={`pb-3 border-b-2 transition-all ${
            activeTab === 'CONVERT'
              ? 'border-slate-900 text-slate-900 font-bold'
              : 'border-transparent text-slate-400 hover:text-slate-700'
          }`}
        >
          {PURCHASING_COPY.create.tabConvertPR}
        </button>

        <button
          type="button"
          onClick={() => {
            setActiveTab('DIRECT')
            setSelectedPRId('')
            setValue('requisitionId', '')
          }}
          className={`pb-3 border-b-2 transition-all ${
            activeTab === 'DIRECT'
              ? 'border-slate-900 text-slate-900 font-bold'
              : 'border-transparent text-slate-400 hover:text-slate-700'
          }`}
        >
          {PURCHASING_COPY.create.tabDirectPO}
        </button>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Approved PR Selector if in Convert Mode */}
        {activeTab === 'CONVERT' && (
          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 space-y-2">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-800">
              {PURCHASING_COPY.create.selectPRLabel}
            </label>
            <select
              value={selectedPRId}
              onChange={(e) => setSelectedPRId(e.target.value)}
              className="w-full bg-white border border-slate-300 rounded-lg py-2 px-3 text-xs text-slate-900 focus:ring-2 focus:ring-slate-900 focus:outline-none cursor-pointer"
            >
              {approvedPROptions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            {approvedPRs.length === 0 && (
              <p className="text-[11px] text-amber-700">
                Notice: No approved purchase requisitions available. You can create a direct standalone PO using the tab above.
              </p>
            )}
          </div>
        )}

        {/* Section 1: Commercial Scope */}
        <div className="bg-white rounded-lg p-6 border border-slate-200 shadow-2xs space-y-4">
          <h3 className="text-sm font-bold text-slate-900 border-b border-slate-100 pb-3">
            {PURCHASING_COPY.create.sectionHeader}
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Controller
              name="vendorId"
              control={control}
              render={({ field }) => (
                <Select
                  label={PURCHASING_COPY.create.vendorLabel}
                  options={vendorOptions}
                  error={errors.vendorId?.message}
                  disabled={isVendorsLoading}
                  required
                  {...field}
                />
              )}
            />

            <Controller
              name="legalEntityId"
              control={control}
              render={({ field }) => (
                <Select
                  label={PURCHASING_COPY.create.legalEntityLabel}
                  options={legalEntityOptions}
                  error={errors.legalEntityId?.message}
                  disabled={isOrgLoading}
                  required
                  {...field}
                />
              )}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Controller
              name="costCenterId"
              control={control}
              render={({ field }) => (
                <Select
                  label={PURCHASING_COPY.create.costCenterLabel}
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
                  label={PURCHASING_COPY.create.facilityLabel}
                  options={facilityOptions}
                  error={errors.deliveryFacilityId?.message}
                  disabled={isOrgLoading}
                  required
                  {...field}
                />
              )}
            />
          </div>
        </div>

        {/* Section 2: Commercial Terms */}
        <div className="bg-white rounded-lg p-6 border border-slate-200 shadow-2xs space-y-4">
          <h3 className="text-sm font-bold text-slate-900 border-b border-slate-100 pb-3">
            {PURCHASING_COPY.create.sectionTerms}
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Controller
              name="incoterms"
              control={control}
              render={({ field }) => (
                <Select
                  label={PURCHASING_COPY.create.incotermsLabel}
                  options={[...PURCHASING_COPY.incotermsOptions]}
                  error={errors.incoterms?.message}
                  required
                  {...field}
                />
              )}
            />

            <Controller
              name="paymentTerms"
              control={control}
              render={({ field }) => (
                <Select
                  label={PURCHASING_COPY.create.paymentTermsLabel}
                  options={[...PURCHASING_COPY.paymentTermsOptions]}
                  error={errors.paymentTerms?.message}
                  required
                  {...field}
                />
              )}
            />

            <Input
              label={PURCHASING_COPY.create.currencyLabel}
              value={selectedCurrency}
              disabled
            />
          </div>

          <div>
            <Textarea
              label={PURCHASING_COPY.create.notesLabel}
              placeholder={PURCHASING_COPY.create.notesPlaceholder}
              rows={2}
              {...register('notes')}
            />
          </div>
        </div>

        {/* Section 3: Line Items Editor */}
        <div className="bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
          <div className="p-4 border-b border-slate-200 flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-900">
              {PURCHASING_COPY.create.sectionItems}
            </h3>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleAddItem}
              leftIcon={<Plus className="w-3.5 h-3.5" />}
            >
              {PURCHASING_COPY.create.addLineCTA}
            </Button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3 min-w-[220px]">Item Description</th>
                  <th className="px-3 py-3 w-24 text-right">Qty</th>
                  <th className="px-3 py-3 w-32">UOM</th>
                  <th className="px-3 py-3 w-40 text-right">Unit Price</th>
                  <th className="px-4 py-3 w-40 text-right">Line Total</th>
                  <th className="px-3 py-3 w-12 text-center"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {lineItems.map((item, index) => {
                  const lineTotal = (item.quantity || 0) * (item.unitPrice || 0)

                  return (
                    <tr key={index} className="hover:bg-slate-50/40">
                      <td className="px-4 py-2.5">
                        <Input
                          value={item.itemDescription}
                          onChange={(e) =>
                            handleUpdateItem(index, { itemDescription: e.target.value })
                          }
                          placeholder="Item or service description..."
                          required
                        />
                      </td>

                      <td className="px-3 py-2.5 text-right">
                        <input
                          type="number"
                          min="0.01"
                          step="1"
                          value={item.quantity}
                          onChange={(e) =>
                            handleUpdateItem(index, { quantity: parseFloat(e.target.value) || 0 })
                          }
                          className="w-full text-right py-2 px-2.5 text-sm font-mono bg-slate-50 border border-slate-200 rounded-lg text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900"
                        />
                      </td>

                      <td className="px-3 py-2.5">
                        <Select
                          value={item.unitOfMeasure}
                          onChange={(e) =>
                            handleUpdateItem(index, { unitOfMeasure: e.target.value })
                          }
                          options={[
                            { value: 'PIECE', label: 'Piece (Pcs)' },
                            { value: 'BOX', label: 'Box' },
                            { value: 'HOUR', label: 'Hour (Hrs)' },
                            { value: 'SET', label: 'Set' },
                          ]}
                        />
                      </td>

                      <td className="px-3 py-2.5 text-right">
                        <MoneyInput
                          value={item.unitPrice}
                          currency={selectedCurrency as any}
                          onChange={(unitPrice) => handleUpdateItem(index, { unitPrice })}
                        />
                      </td>

                      <td className="px-4 py-2.5 text-right font-mono font-bold text-slate-900 text-sm">
                        <CurrencyDisplay amount={lineTotal} currency={selectedCurrency as any} />
                      </td>

                      <td className="px-3 py-2.5 text-center">
                        <button
                          type="button"
                          disabled={lineItems.length <= 1}
                          onClick={() => handleRemoveItem(index)}
                          className="p-1.5 text-slate-400 hover:text-red-600 rounded disabled:opacity-30 transition-colors"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <div className="p-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
              Total Commercial Value:
            </span>
            <CurrencyDisplay amount={totalAmount} currency={selectedCurrency as any} className="text-lg font-bold text-slate-900" />
          </div>
        </div>

        {/* Footer Actions */}
        <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(ROUTES.purchasing.root)}
            disabled={isCreating}
          >
            {PURCHASING_COPY.create.cancelButton}
          </Button>

          <Button
            type="submit"
            isLoading={isCreating}
            leftIcon={<Save className="w-4 h-4" />}
          >
            {isCreating ? PURCHASING_COPY.create.savingStatus : PURCHASING_COPY.create.saveDraftCTA}
          </Button>
        </div>
      </form>
    </div>
  )
}
