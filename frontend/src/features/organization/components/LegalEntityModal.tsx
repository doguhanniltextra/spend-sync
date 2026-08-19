import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Textarea } from '@/components/ui/Textarea'
import { Button } from '@/components/ui/Button'
import type { LegalEntityResponse, CreateLegalEntityRequest } from '@/types/organization.types'

const legalEntitySchema = z.object({
  name:              z.string().min(2, 'Name must be at least 2 characters'),
  companyCode:       z.string().min(1, 'Company code is required').max(20),
  taxNumber:         z.string().min(5, 'Tax ID / VKN is required'),
  taxOffice:         z.string().optional(),
  baseCurrency:      z.string().length(3, '3-letter currency required'),
  registeredAddress: z.string().min(5, 'Address must be at least 5 characters'),
  country:           z.string().length(2, '2-letter ISO country code required (e.g. TR, GB, US)'),
})

type FormValues = z.infer<typeof legalEntitySchema>

interface LegalEntityModalProps {
  isOpen:   boolean
  onClose:  () => void
  initial?: LegalEntityResponse | null
  onSubmit: (values: CreateLegalEntityRequest) => Promise<any>
}

export function LegalEntityModal({ isOpen, onClose, initial, onSubmit }: LegalEntityModalProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(legalEntitySchema),
    defaultValues: {
      name:              '',
      companyCode:       '',
      taxNumber:         '',
      taxOffice:         '',
      baseCurrency:      'TRY',
      registeredAddress: '',
      country:           'TR',
    },
  })

  useEffect(() => {
    if (initial) {
      reset({
        name:              initial.name,
        companyCode:       initial.companyCode,
        taxNumber:         initial.taxNumber,
        taxOffice:         initial.taxOffice || '',
        baseCurrency:      initial.baseCurrency,
        registeredAddress: initial.registeredAddress,
        country:           initial.country,
      })
    } else {
      reset({
        name:              '',
        companyCode:       '',
        taxNumber:         '',
        taxOffice:         '',
        baseCurrency:      'TRY',
        registeredAddress: '',
        country:           'TR',
      })
    }
  }, [initial, reset, isOpen])

  const handleFormSubmit = async (values: FormValues) => {
    await onSubmit({
      ...values,
      country: values.country.toUpperCase(),
      baseCurrency: values.baseCurrency.toUpperCase(),
    })
    onClose()
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={initial ? 'Edit Legal Entity' : 'Register New Legal Entity'}
      description="Configure operating subsidiary corporate identifier, VKN, and functional currency."
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
            {initial ? 'Save Changes' : 'Create Entity'}
          </Button>
        </>
      }
    >
      <form className="space-y-3.5 text-xs">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="sm:col-span-2">
            <Input
              label="Legal Company Name"
              placeholder="e.g. SpendSync Technology Solutions Ltd."
              {...register('name')}
              error={errors.name?.message}
              required
            />
          </div>
          <div>
            <Input
              label="Company Code"
              placeholder="e.g. 1000"
              {...register('companyCode')}
              error={errors.companyCode?.message}
              required
            />
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label="Tax ID / VKN"
            placeholder="e.g. 9876543210"
            {...register('taxNumber')}
            error={errors.taxNumber?.message}
            required
          />
          <Input
            label="Tax Office (Optional)"
            placeholder="e.g. Maslak VD"
            {...register('taxOffice')}
            error={errors.taxOffice?.message}
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Select
            label="Base Currency"
            {...register('baseCurrency')}
            error={errors.baseCurrency?.message}
            options={[
              { value: 'TRY', label: 'TRY (Turkish Lira ₺)' },
              { value: 'USD', label: 'USD (US Dollar $)' },
              { value: 'EUR', label: 'EUR (Euro €)' },
              { value: 'GBP', label: 'GBP (British Pound £)' },
            ]}
            required
          />
          <Input
            label="Country Code (ISO-2)"
            placeholder="TR / GB / US / DE"
            maxLength={2}
            {...register('country')}
            error={errors.country?.message}
            required
          />
        </div>

        <Textarea
          label="Registered Legal Address"
          placeholder="Official registered company headquarters address..."
          rows={2}
          {...register('registeredAddress')}
          error={errors.registeredAddress?.message}
          required
        />
      </form>
    </Modal>
  )
}
