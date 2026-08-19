import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Textarea } from '@/components/ui/Textarea'
import { Button } from '@/components/ui/Button'
import type { FacilityResponse, CreateFacilityRequest, LegalEntityResponse } from '@/types/organization.types'

const facilitySchema = z.object({
  legalEntityId:   z.string().min(1, 'Legal Entity is required'),
  facilityCode:    z.string().min(2, 'Facility code is required').max(50),
  name:            z.string().min(2, 'Facility name must be at least 2 characters').max(255),
  facilityType:    z.enum(['OFFICE', 'WAREHOUSE', 'PLANT', 'RETAIL', 'DATA_CENTER']),
  shippingAddress: z.string().min(5, 'Shipping address must be at least 5 characters'),
  contactPerson:   z.string().optional(),
  contactPhone:    z.string().optional(),
})

type FormValues = z.infer<typeof facilitySchema>

interface FacilityModalProps {
  isOpen:        boolean
  onClose:       () => void
  initial?:      FacilityResponse | null
  legalEntities: LegalEntityResponse[]
  onSubmit:      (values: CreateFacilityRequest) => Promise<any>
}

export function FacilityModal({
  isOpen,
  onClose,
  initial,
  legalEntities,
  onSubmit,
}: FacilityModalProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(facilitySchema),
    defaultValues: {
      legalEntityId:   legalEntities[0]?.id || '',
      facilityCode:    '',
      name:            '',
      facilityType:    'WAREHOUSE',
      shippingAddress: '',
      contactPerson:   '',
      contactPhone:    '',
    },
  })

  useEffect(() => {
    if (initial) {
      reset({
        legalEntityId:   initial.legalEntityId,
        facilityCode:    initial.facilityCode,
        name:            initial.name,
        facilityType:    initial.facilityType,
        shippingAddress: initial.shippingAddress,
        contactPerson:   initial.contactPerson || '',
        contactPhone:    initial.contactPhone || '',
      })
    } else {
      reset({
        legalEntityId:   legalEntities[0]?.id || '',
        facilityCode:    '',
        name:            '',
        facilityType:    'WAREHOUSE',
        shippingAddress: '',
        contactPerson:   '',
        contactPhone:    '',
      })
    }
  }, [initial, reset, isOpen, legalEntities])

  const handleFormSubmit = async (values: FormValues) => {
    await onSubmit(values)
    onClose()
  }

  const entityOptions = [
    { value: '', label: 'Select Legal Entity...' },
    ...legalEntities.map((e) => ({ value: e.id, label: `${e.name} (${e.companyCode})` })),
  ]

  const typeOptions = [
    { value: 'WAREHOUSE', label: 'Warehouse & Logistics Hub' },
    { value: 'DATA_CENTER', label: 'Data Center & Compute Facility' },
    { value: 'OFFICE', label: 'Corporate Office HQ' },
    { value: 'PLANT', label: 'Manufacturing Plant' },
    { value: 'RETAIL', label: 'Retail Store Location' },
  ]

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={initial ? 'Edit Logistics Facility' : 'Register New Logistics Facility'}
      description="Define receiving dock, shipping address, and site contact for purchase orders."
      maxWidth="lg"
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
            {initial ? 'Save Changes' : 'Register Facility'}
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
              label="Facility Code"
              placeholder="e.g. FAC-IST-01"
              {...register('facilityCode')}
              error={errors.facilityCode?.message}
              required
            />
          </div>
          <div className="sm:col-span-2">
            <Input
              label="Facility / Dock Name"
              placeholder="e.g. Istanbul Maltepe Central Logistics DC"
              {...register('name')}
              error={errors.name?.message}
              required
            />
          </div>
        </div>

        <Select
          label="Facility Type"
          {...register('facilityType')}
          error={errors.facilityType?.message}
          options={typeOptions}
          required
        />

        <Textarea
          label="Physical Shipping & Dock Address"
          placeholder="Detailed physical delivery address for waybill logistics..."
          rows={2}
          {...register('shippingAddress')}
          error={errors.shippingAddress?.message}
          required
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label="Dock Manager / Contact Person"
            placeholder="e.g. Ahmet Yilmaz"
            {...register('contactPerson')}
            error={errors.contactPerson?.message}
          />
          <Input
            label="Contact Phone"
            placeholder="e.g. +90 216 555 0199"
            {...register('contactPhone')}
            error={errors.contactPhone?.message}
          />
        </div>
      </form>
    </Modal>
  )
}
