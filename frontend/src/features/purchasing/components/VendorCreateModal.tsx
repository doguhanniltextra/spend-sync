import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { useCreateVendor } from '../hooks/useVendors'
import type { VendorCategory, VendorTier, PaymentTerms } from '@/types/purchasing.types'
import { PURCHASING_COPY } from '../constants/purchasingCopy'

const vendorSchema = z.object({
  name:                z.string().min(2, 'Name must be at least 2 characters'),
  taxNumber:           z.string().min(8, 'Tax number / VKN must be at least 8 digits'),
  taxOffice:           z.string().min(2, 'Tax office is required'),
  category:            z.string().min(1, 'Category is required'),
  tier:                z.string().min(1, 'Tier is required'),
  isEInvoiceRegistered:z.boolean(),
  orderEmail:          z.string().email('Valid order email is required'),
  phoneNumber:         z.string().optional(),
  address:             z.string().min(5, 'Address is required'),
  city:                z.string().optional(),
  country:             z.string().min(2, 'Country code required'),
  paymentTerms:        z.string().min(1, 'Payment terms required'),
  bankName:            z.string().optional(),
  iban:                z.string().optional(),
})

type FormValues = z.infer<typeof vendorSchema>

interface VendorCreateModalProps {
  isOpen:  boolean
  onClose: () => void
}

export function VendorCreateModal({ isOpen, onClose }: VendorCreateModalProps) {
  const { createVendor, isOnboarding } = useCreateVendor()

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(vendorSchema),
    defaultValues: {
      name:                '',
      taxNumber:           '',
      taxOffice:           '',
      category:            'SOFTWARE_SAAS',
      tier:                'TIER_2_PREFERRED',
      isEInvoiceRegistered:true,
      orderEmail:          '',
      phoneNumber:         '',
      address:             '',
      city:                'Istanbul',
      country:             'TR',
      paymentTerms:        'NET_30',
      bankName:            'Garanti BBVA',
      iban:                'TR000000000000000000000000',
    },
  })

  const onSubmit = async (values: FormValues) => {
    try {
      await createVendor({
        ...values,
        category:     values.category as VendorCategory,
        tier:         values.tier as VendorTier,
        paymentTerms: values.paymentTerms as PaymentTerms,
      })
      reset()
      onClose()
    } catch {
      // Handled in hook
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={PURCHASING_COPY.onboardModal.title}
      description={PURCHASING_COPY.onboardModal.desc}
      maxWidth="xl"
      footer={
        <>
          <Button type="button" variant="outline" size="sm" onClick={onClose} disabled={isOnboarding}>
            {PURCHASING_COPY.onboardModal.cancelCTA}
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={handleSubmit(onSubmit)}
            isLoading={isOnboarding}
          >
            {isOnboarding
              ? PURCHASING_COPY.onboardModal.submitting
              : PURCHASING_COPY.onboardModal.submitCTA}
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 text-xs">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label={PURCHASING_COPY.onboardModal.nameLabel}
            placeholder="e.g. AWS EMEA SARL or Koç Sistem A.Ş."
            error={errors.name?.message}
            required
            {...register('name')}
          />

          <Input
            label={PURCHASING_COPY.onboardModal.taxNumberLabel}
            placeholder="10-digit VKN or VAT number"
            error={errors.taxNumber?.message}
            required
            {...register('taxNumber')}
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <Input
            label={PURCHASING_COPY.onboardModal.taxOfficeLabel}
            placeholder="e.g. Büyük Mükellefler"
            error={errors.taxOffice?.message}
            required
            {...register('taxOffice')}
          />

          <Controller
            name="category"
            control={control}
            render={({ field }) => (
              <Select
                label={PURCHASING_COPY.onboardModal.categoryLabel}
                options={[...PURCHASING_COPY.categoryOptions]}
                error={errors.category?.message}
                required
                {...field}
              />
            )}
          />

          <Controller
            name="tier"
            control={control}
            render={({ field }) => (
              <Select
                label={PURCHASING_COPY.onboardModal.tierLabel}
                options={[...PURCHASING_COPY.tierOptions]}
                error={errors.tier?.message}
                required
                {...field}
              />
            )}
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label={PURCHASING_COPY.onboardModal.orderEmailLabel}
            type="email"
            placeholder="orders@supplier.com"
            error={errors.orderEmail?.message}
            required
            {...register('orderEmail')}
          />

          <Input
            label={PURCHASING_COPY.onboardModal.phoneLabel}
            placeholder="+90 212 000 0000"
            {...register('phoneNumber')}
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="sm:col-span-2">
            <Input
              label={PURCHASING_COPY.onboardModal.addressLabel}
              placeholder="Full registered headquarters address"
              error={errors.address?.message}
              required
              {...register('address')}
            />
          </div>

          <Controller
            name="paymentTerms"
            control={control}
            render={({ field }) => (
              <Select
                label={PURCHASING_COPY.onboardModal.paymentTermsLabel}
                options={[...PURCHASING_COPY.paymentTermsOptions]}
                error={errors.paymentTerms?.message}
                required
                {...field}
              />
            )}
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label={PURCHASING_COPY.onboardModal.bankNameLabel}
            placeholder="Bank Name (e.g. İş Bankası, Garanti)"
            {...register('bankName')}
          />

          <Input
            label={PURCHASING_COPY.onboardModal.ibanLabel}
            placeholder="TR00 0000 0000 0000 0000 0000 00"
            {...register('iban')}
          />
        </div>

        <div className="flex items-center gap-2 pt-2 border-t border-slate-100">
          <input
            id="eInvoiceCheck"
            type="checkbox"
            className="w-4 h-4 rounded border-slate-300 text-slate-900 focus:ring-slate-900 cursor-pointer"
            {...register('isEInvoiceRegistered')}
          />
          <label htmlFor="eInvoiceCheck" className="text-xs text-slate-700 font-medium cursor-pointer">
            {PURCHASING_COPY.onboardModal.eInvoiceLabel}
          </label>
        </div>
      </form>
    </Modal>
  )
}
