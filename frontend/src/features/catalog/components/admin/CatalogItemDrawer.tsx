import { useState, useEffect } from 'react'
import { X, Save, Package } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { MoneyInput } from '@/components/ui/MoneyInput'
import { useCategoryTree } from '../../hooks/useCategoryTree'
import { useVendors } from '@/features/purchasing/hooks/useVendors'
import { useCatalogAdmin } from '../../hooks/useCatalogAdmin'
import { CATALOG_COPY } from '../../constants/catalogCopy'
import type { CatalogItemResponse } from '../../types/catalog.types'

interface CatalogItemDrawerProps {
  isOpen: boolean
  onClose: () => void
  itemToEdit?: CatalogItemResponse | null
}

export function CatalogItemDrawer({ isOpen, onClose, itemToEdit }: CatalogItemDrawerProps) {
  const isEditing = !!itemToEdit

  const { data: categories = [] } = useCategoryTree()
  const { vendors = [] } = useVendors()
  const { createItem, updateItem, isCreatingItem, isUpdatingItem } = useCatalogAdmin()

  const [formData, setFormData] = useState({
    itemCode: '',
    name: '',
    description: '',
    categoryId: '',
    preferredVendorId: '',
    unitPrice: 0,
    currency: 'TRY',
    vatRate: 0.20,
    unitOfMeasure: 'PIECE',
    contractReference: '',
    validFrom: '',
    validUntil: '',
    isPreferred: false,
    isActive: true,
    glAccountCode: '',
  })

  useEffect(() => {
    if (itemToEdit) {
      setFormData({
        itemCode: itemToEdit.itemCode || '',
        name: itemToEdit.name || '',
        description: itemToEdit.description || '',
        categoryId: itemToEdit.categoryId || '',
        preferredVendorId: itemToEdit.preferredVendorId || '',
        unitPrice: itemToEdit.unitPrice || 0,
        currency: itemToEdit.currency || 'TRY',
        vatRate: itemToEdit.vatRate || 0.20,
        unitOfMeasure: itemToEdit.unitOfMeasure || 'PIECE',
        contractReference: itemToEdit.contractReference || '',
        validFrom: itemToEdit.validFrom || '',
        validUntil: itemToEdit.validUntil || '',
        isPreferred: itemToEdit.isPreferred || false,
        isActive: itemToEdit.isActive ?? true,
        glAccountCode: itemToEdit.glAccountCode || '',
      })
    } else {
      setFormData({
        itemCode: '',
        name: '',
        description: '',
        categoryId: '',
        preferredVendorId: '',
        unitPrice: 0,
        currency: 'TRY',
        vatRate: 0.20,
        unitOfMeasure: 'PIECE',
        contractReference: '',
        validFrom: '',
        validUntil: '',
        isPreferred: false,
        isActive: true,
        glAccountCode: '',
      })
    }
  }, [itemToEdit, isOpen])

  if (!isOpen) return null

  // Flatten categories for select dropdown
  const flatCategoryOptions: { value: string; label: string }[] = [{ value: '', label: 'Select category...' }]
  const flatten = (cats: typeof categories) => {
    for (const c of cats) {
      flatCategoryOptions.push({ value: c.id, label: c.fullPath })
      if (c.children?.length) {
        flatten(c.children)
      }
    }
  }
  flatten(categories)

  const vendorOptions = [
    { value: '', label: 'Select supplier...' },
    ...vendors.map((v) => ({
      value: v.id,
      label: `${v.name} (${v.taxNumber})`,
    })),
  ]

  const uomOptions = [
    { value: 'PIECE', label: 'Piece (PIECE)' },
    { value: 'BOX', label: 'Box (BOX)' },
    { value: 'SET', label: 'Set (SET)' },
    { value: 'LICENSE', label: 'License (LICENSE)' },
    { value: 'HOUR', label: 'Hour (HOUR)' },
    { value: 'KG', label: 'Kilogram (KG)' },
    { value: 'METER', label: 'Meter (METER)' },
  ]

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (isEditing && itemToEdit) {
      await updateItem({
        id: itemToEdit.id,
        data: {
          name: formData.name,
          description: formData.description || undefined,
          categoryId: formData.categoryId || undefined,
          preferredVendorId: formData.preferredVendorId || undefined,
          unitPrice: formData.unitPrice,
          currency: formData.currency,
          vatRate: formData.vatRate,
          unitOfMeasure: formData.unitOfMeasure,
          contractReference: formData.contractReference || undefined,
          validFrom: formData.validFrom || undefined,
          validUntil: formData.validUntil || undefined,
          isPreferred: formData.isPreferred,
          isActive: formData.isActive,
          glAccountCode: formData.glAccountCode || undefined,
        },
      })
    } else {
      await createItem({
        itemCode: formData.itemCode || undefined,
        name: formData.name,
        description: formData.description || undefined,
        categoryId: formData.categoryId || undefined,
        preferredVendorId: formData.preferredVendorId || undefined,
        unitPrice: formData.unitPrice,
        currency: formData.currency,
        vatRate: formData.vatRate,
        unitOfMeasure: formData.unitOfMeasure,
        contractReference: formData.contractReference || undefined,
        validFrom: formData.validFrom || undefined,
        validUntil: formData.validUntil || undefined,
        isPreferred: formData.isPreferred,
        glAccountCode: formData.glAccountCode || undefined,
      })
    }
    onClose()
  }

  const isPending = isCreatingItem || isUpdatingItem

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-slate-900/50 backdrop-blur-xs flex justify-end animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-xl h-full shadow-2xl flex flex-col overflow-hidden animate-in slide-in-from-right duration-200">
        {/* Drawer Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
              <Package className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {isEditing ? CATALOG_COPY.admin.drawer.editTitle : CATALOG_COPY.admin.drawer.createTitle}
              </h3>
              <p className="text-xs text-slate-500">
                {isEditing ? `Item Code: ${itemToEdit?.itemCode}` : 'Add a new pre-approved item to the catalog.'}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200/60 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Drawer Body Form */}
        <form id="catalog-item-form" onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Section 1: Basic info */}
          <div className="space-y-4">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
              {CATALOG_COPY.admin.drawer.sectionBasic}
            </h4>

            {!isEditing && (
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {CATALOG_COPY.admin.drawer.codeLabel}
                </label>
                <Input
                  value={formData.itemCode}
                  onChange={(e) => setFormData({ ...formData, itemCode: e.target.value })}
                  placeholder={CATALOG_COPY.admin.drawer.codePlaceholder}
                  className="font-mono text-sm uppercase"
                />
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {CATALOG_COPY.admin.drawer.nameLabel} <span className="text-red-500">*</span>
              </label>
              <Input
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder={CATALOG_COPY.admin.drawer.namePlaceholder}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {CATALOG_COPY.admin.drawer.categoryLabel}
              </label>
              <Select
                value={formData.categoryId}
                onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                options={flatCategoryOptions}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {CATALOG_COPY.admin.drawer.descLabel}
              </label>
              <textarea
                rows={3}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder={CATALOG_COPY.admin.drawer.descPlaceholder}
                className="w-full px-3 py-2 text-sm bg-slate-50 border border-slate-200 rounded-lg text-slate-900 placeholder:text-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900"
              />
            </div>
          </div>

          {/* Section 2: Pricing & Tax */}
          <div className="space-y-4">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
              {CATALOG_COPY.admin.drawer.sectionPricing}
            </h4>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {CATALOG_COPY.admin.drawer.priceLabel} <span className="text-red-500">*</span>
                </label>
                <MoneyInput
                  value={formData.unitPrice}
                  currency={formData.currency as any}
                  onChange={(val) => setFormData({ ...formData, unitPrice: val })}
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {CATALOG_COPY.admin.drawer.uomLabel}
                </label>
                <Select
                  value={formData.unitOfMeasure}
                  onChange={(e) => setFormData({ ...formData, unitOfMeasure: e.target.value })}
                  options={uomOptions}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {CATALOG_COPY.admin.drawer.vatLabel}
                </label>
                <Select
                  value={String(formData.vatRate)}
                  onChange={(e) => setFormData({ ...formData, vatRate: parseFloat(e.target.value) || 0.20 })}
                  options={[
                    { value: '0.20', label: '20% Standard VAT' },
                    { value: '0.10', label: '10% Reduced VAT' },
                    { value: '0.01', label: '1% Basic Goods VAT' },
                    { value: '0.00', label: '0% Tax Exempt' },
                  ]}
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {CATALOG_COPY.admin.drawer.glAccountLabel}
                </label>
                <Input
                  value={formData.glAccountCode}
                  onChange={(e) => setFormData({ ...formData, glAccountCode: e.target.value })}
                  placeholder={CATALOG_COPY.admin.drawer.glAccountPlaceholder}
                  className="font-mono text-sm"
                />
              </div>
            </div>
          </div>

          {/* Section 3: Vendor & Contract */}
          <div className="space-y-4">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
              {CATALOG_COPY.admin.drawer.sectionContract}
            </h4>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {CATALOG_COPY.admin.drawer.vendorLabel}
              </label>
              <Select
                value={formData.preferredVendorId}
                onChange={(e) => setFormData({ ...formData, preferredVendorId: e.target.value })}
                options={vendorOptions}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {CATALOG_COPY.admin.drawer.contractRefLabel}
              </label>
              <Input
                value={formData.contractReference}
                onChange={(e) => setFormData({ ...formData, contractReference: e.target.value })}
                placeholder={CATALOG_COPY.admin.drawer.contractRefPlaceholder}
                className="font-mono text-sm"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {CATALOG_COPY.admin.drawer.validFromLabel}
                </label>
                <input
                  type="date"
                  value={formData.validFrom}
                  onChange={(e) => setFormData({ ...formData, validFrom: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 border border-slate-200 rounded-lg text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {CATALOG_COPY.admin.drawer.validUntilLabel}
                </label>
                <input
                  type="date"
                  value={formData.validUntil}
                  onChange={(e) => setFormData({ ...formData, validUntil: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 border border-slate-200 rounded-lg text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900"
                />
              </div>
            </div>

            {/* Checkboxes */}
            <div className="space-y-2 pt-2">
              <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-slate-700">
                <input
                  type="checkbox"
                  checked={formData.isPreferred}
                  onChange={(e) => setFormData({ ...formData, isPreferred: e.target.checked })}
                  className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 border-slate-300"
                />
                <span>{CATALOG_COPY.admin.drawer.preferredCheckbox}</span>
              </label>

              {isEditing && (
                <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-slate-700">
                  <input
                    type="checkbox"
                    checked={formData.isActive}
                    onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                    className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 border-slate-300"
                  />
                  <span>{CATALOG_COPY.admin.drawer.activeCheckbox}</span>
                </label>
              )}
            </div>
          </div>
        </form>

        {/* Drawer Footer Actions */}
        <div className="px-6 py-4 border-t border-slate-200 bg-slate-50 flex items-center justify-end gap-3">
          <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
            Cancel
          </Button>
          <Button
            type="submit"
            form="catalog-item-form"
            variant="primary"
            isLoading={isPending}
            leftIcon={<Save className="w-4 h-4" />}
          >
            {isEditing ? 'Update Item' : 'Save Item'}
          </Button>
        </div>
      </div>
    </div>
  )
}
