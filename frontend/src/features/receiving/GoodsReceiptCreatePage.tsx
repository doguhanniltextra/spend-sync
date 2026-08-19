import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ArrowLeft, Truck } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import { Select } from '@/components/ui/Select'
import { DockInspectionTable, type InspectionLineState } from './components/DockInspectionTable'
import { usePendingOrders } from './hooks/usePendingOrders'
import { useCreateGoodsReceipt } from './hooks/useCreateGoodsReceipt'
import { receivingApi } from './services/receivingApi'
import type { PurchaseOrderDetailResponse } from '@/types/purchasing.types'
import { ROUTES } from '@/constants/routes'
import { RECEIVING_COPY } from './constants/receivingCopy'
import { useToast } from '@/components/feedback/Toast'

const createGRSchema = z.object({
  purchaseOrderId: z.string().min(1, 'Purchase order is required'),
  waybillNumber:   z.string().min(2, 'Waybill / delivery note number is required'),
  waybillDate:     z.string().min(10, 'Valid waybill date is required'),
  notes:           z.string().optional(),
})

type FormValues = z.infer<typeof createGRSchema>

export default function GoodsReceiptCreatePage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const preselectedPOId = searchParams.get('poId')
  const toast = useToast()

  const { pendingOrders } = usePendingOrders()
  const { createReceipt, isCreating } = useCreateGoodsReceipt()

  const [selectedPO, setSelectedPO] = useState<PurchaseOrderDetailResponse | null>(null)
  const [isLoadingPO, setIsLoadingPO] = useState(false)
  const [inspectionLines, setInspectionLines] = useState<InspectionLineState[]>([])

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(createGRSchema),
    defaultValues: {
      purchaseOrderId: preselectedPOId || '',
      waybillNumber:   '',
      waybillDate:     new Date().toISOString().split('T')[0],
      notes:           '',
    },
  })

  const selectedPOId = watch('purchaseOrderId')

  // Auto-fetch PO line items when PO is selected
  useEffect(() => {
    if (!selectedPOId) {
      setSelectedPO(null)
      setInspectionLines([])
      return
    }

    let isMounted = true
    setIsLoadingPO(true)

    receivingApi
      .getPODetail(selectedPOId)
      .then((po) => {
        if (!isMounted) return
        setSelectedPO(po)
        setInspectionLines(
          po.lineItems.map((item) => ({
            purchaseOrderLineItemId:  item.id,
            itemDescription:          item.itemDescription,
            itemCategory:             item.itemCategory,
            orderedQuantity:          item.quantity,
            receivedQuantity:         item.quantity,
            acceptedQuantity:         item.quantity,
            rejectedQuantity:         0,
            rejectionReason:          '',
            notes:                    '',
            unitOfMeasure:            item.unitOfMeasure,
            overDeliveryTolerancePct: item.overDeliveryTolerancePct ?? 0,
          }))
        )
      })
      .catch(() => {
        toast.error('Failed to load purchase order line items.')
      })
      .finally(() => {
        if (isMounted) setIsLoadingPO(false)
      })

    return () => {
      isMounted = false
    }
  }, [selectedPOId])

  const onSubmit = async (values: FormValues) => {
    if (inspectionLines.length === 0) {
      toast.error('Please select a purchase order with valid line items.')
      return
    }

    // Validate quantities & reasons
    for (const line of inspectionLines) {
      const sum = (line.acceptedQuantity || 0) + (line.rejectedQuantity || 0)
      if (Math.abs(line.receivedQuantity - sum) > 0.001) {
        toast.error(`Line '${line.itemDescription}': Received (${line.receivedQuantity}) must equal Accepted (${line.acceptedQuantity}) + Rejected (${line.rejectedQuantity || 0}).`)
        return
      }

      if ((line.rejectedQuantity || 0) > 0 && (!line.rejectionReason || line.rejectionReason.trim().length === 0)) {
        toast.error(`Line '${line.itemDescription}': Rejection reason is mandatory when rejected quantity is greater than zero.`)
        return
      }

      const maxAllowed = line.orderedQuantity * (1 + (line.overDeliveryTolerancePct || 0) / 100)
      if (line.acceptedQuantity > maxAllowed + 0.001) {
        toast.error(`Line '${line.itemDescription}': Accepted quantity (${line.acceptedQuantity}) exceeds maximum allowed (${maxAllowed.toFixed(2)}) under +${line.overDeliveryTolerancePct}% tolerance.`)
        return
      }
    }

    try {
      await createReceipt({
        purchaseOrderId:     values.purchaseOrderId,
        waybillNumber:       values.waybillNumber.trim(),
        waybillDate:         values.waybillDate,
        deliveryFacilityId:  selectedPO?.deliveryFacilityId,
        notes:               values.notes?.trim() || undefined,
        lineItems: inspectionLines.map((l) => ({
          purchaseOrderLineItemId: l.purchaseOrderLineItemId,
          receivedQuantity:        l.receivedQuantity,
          acceptedQuantity:        l.acceptedQuantity,
          rejectedQuantity:        l.rejectedQuantity || 0,
          rejectionReason:         l.rejectionReason?.trim() || undefined,
          notes:                   l.notes?.trim() || undefined,
        })),
      })
      navigate(ROUTES.receiving.root)
    } catch {
      // Handled in mutation hook
    }
  }

  const poOptions = [
    { value: '', label: RECEIVING_COPY.create.poSelectPlaceholder },
    ...pendingOrders.map((po) => ({
      value: po.id,
      label: `${po.poNumber} • ${po.vendorName} (${po.deliveryFacilityName} • ${po.lineItemCount} Lines)`,
    })),
  ]

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-20">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div>
          <button
            onClick={() => navigate(ROUTES.receiving.root)}
            type="button"
            className="inline-flex items-center gap-1 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors mb-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            {RECEIVING_COPY.create.backToList}
          </button>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {RECEIVING_COPY.create.pageTitle}
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {RECEIVING_COPY.create.pageSubtitle}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Step 1: Inbound Order Selector */}
        <div className="bg-white p-5 rounded-lg border border-slate-200 shadow-2xs space-y-4">
          <h3 className="text-sm font-bold text-slate-900">
            {RECEIVING_COPY.create.poSelectSection}
          </h3>

          <Select
            label={RECEIVING_COPY.create.poSelectLabel}
            {...register('purchaseOrderId')}
            error={errors.purchaseOrderId?.message}
            options={poOptions}
            required
          />

          {selectedPO && (
            <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-200/80 grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
              <div>
                <span className="text-[10px] text-slate-400 block font-semibold uppercase">Vendor</span>
                <strong className="text-slate-900">{selectedPO.vendorName}</strong>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 block font-semibold uppercase">Destination Dock</span>
                <span className="text-slate-800 font-medium">{selectedPO.deliveryFacilityName}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 block font-semibold uppercase">Terms</span>
                <span className="font-mono font-bold text-slate-900">{selectedPO.incoterms} • {selectedPO.paymentTerms}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 block font-semibold uppercase">Order Total</span>
                <span className="font-mono font-bold text-slate-900">{selectedPO.totalAmount.toLocaleString()} {selectedPO.currency}</span>
              </div>
            </div>
          )}
        </div>

        {/* Step 2: Waybill Documentation */}
        <div className="bg-white p-5 rounded-lg border border-slate-200 shadow-2xs space-y-4">
          <h3 className="text-sm font-bold text-slate-900">
            {RECEIVING_COPY.create.waybillSection}
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label={RECEIVING_COPY.create.waybillNumberLabel}
              placeholder={RECEIVING_COPY.create.waybillNumberPlaceholder}
              {...register('waybillNumber')}
              error={errors.waybillNumber?.message}
              required
            />

            <Input
              type="date"
              label={RECEIVING_COPY.create.waybillDateLabel}
              {...register('waybillDate')}
              error={errors.waybillDate?.message}
              required
            />
          </div>

          <Textarea
            label={RECEIVING_COPY.create.notesLabel}
            placeholder={RECEIVING_COPY.create.notesPlaceholder}
            {...register('notes')}
            rows={2}
          />
        </div>

        {/* Step 3: Physical Line Inspection */}
        {isLoadingPO ? (
          <div className="p-8 text-center bg-white rounded-lg border border-slate-200 text-xs text-slate-500">
            Loading order line items from purchase order...
          </div>
        ) : selectedPO ? (
          <DockInspectionTable
            lines={inspectionLines}
            onChange={(updated) => setInspectionLines(updated)}
          />
        ) : null}

        {/* Action Button */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(ROUTES.receiving.root)}
            disabled={isCreating}
          >
            Cancel
          </Button>

          <Button
            type="submit"
            isLoading={isCreating}
            disabled={!selectedPO || inspectionLines.length === 0}
            leftIcon={<Truck className="w-4 h-4" />}
            className="bg-slate-900 text-white hover:bg-slate-800"
          >
            {isCreating ? RECEIVING_COPY.create.submittingCTA : RECEIVING_COPY.create.submitCTA}
          </Button>
        </div>
      </form>
    </div>
  )
}
