import { Check, Tag } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import { ContractStatusBadge } from '../common/ContractStatusBadge'
import { PreferredVendorBadge } from '../common/PreferredVendorBadge'
import { CATALOG_COPY } from '../../constants/catalogCopy'
import type { CatalogItemResponse } from '../../types/catalog.types'

interface CatalogItemCardProps {
  item: CatalogItemResponse
  onSelect: (item: CatalogItemResponse) => void
  isLoading?: boolean
}

export function CatalogItemCard({ item, onSelect, isLoading }: CatalogItemCardProps) {
  return (
    <div className="bg-white border border-slate-200 hover:border-indigo-400 hover:shadow-md rounded-xl p-4 transition-all flex flex-col justify-between group">
      <div>
        {/* Header: Item Code & Badges */}
        <div className="flex items-start justify-between gap-2 mb-2">
          <div className="flex flex-wrap items-center gap-1.5">
            <span className="font-mono text-xs font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
              {item.itemCode}
            </span>
            {item.isPreferred && (
              <PreferredVendorBadge
                vendorName={item.preferredVendorName}
                tier={item.preferredVendorTier}
              />
            )}
          </div>
          <ContractStatusBadge
            validUntil={item.validUntil}
            contractAlert={item.contractAlert}
          />
        </div>

        {/* Item Title & Category */}
        <h4 className="text-sm font-bold text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-1 mb-1">
          {item.name}
        </h4>

        {item.categoryFullPath && (
          <div className="flex items-center gap-1 text-xs text-slate-500 mb-2">
            <Tag className="w-3 h-3 text-slate-400" />
            <span className="truncate">{item.categoryFullPath}</span>
          </div>
        )}

        {/* Description */}
        {item.description && (
          <p className="text-xs text-slate-600 line-clamp-2 mb-3 bg-slate-50/70 p-2 rounded-lg border border-slate-100">
            {item.description}
          </p>
        )}
      </div>

      {/* Footer: Price & Select CTA */}
      <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-3 mt-2">
        <div>
          <div className="text-[11px] text-slate-500 font-medium">
            {CATALOG_COPY.picker.unitPriceLabel} (+%{Math.round(item.vatRate * 100)} {CATALOG_COPY.picker.vatLabel})
          </div>
          <div className="flex items-baseline gap-1">
            <CurrencyDisplay
              amount={item.unitPrice}
              currency={item.currency as any}
              className="text-base font-bold text-slate-900 font-mono"
            />
            <span className="text-xs text-slate-500 font-medium">/ {item.unitOfMeasure}</span>
          </div>
        </div>

        <Button
          type="button"
          size="sm"
          variant="primary"
          onClick={() => onSelect(item)}
          isLoading={isLoading}
          leftIcon={<Check className="w-3.5 h-3.5" />}
        >
          {CATALOG_COPY.picker.selectButton}
        </Button>
      </div>
    </div>
  )
}
