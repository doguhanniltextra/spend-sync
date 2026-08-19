import { Star } from 'lucide-react'
import { clsx } from 'clsx'

interface PreferredVendorBadgeProps {
  vendorName?: string
  tier?: string
  className?: string
}

export function PreferredVendorBadge({ vendorName, tier, className }: PreferredVendorBadgeProps) {
  if (!vendorName) return null

  const isStrategic = tier === 'TIER_1_STRATEGIC'
  const isPreferred = tier === 'TIER_2_PREFERRED' || !tier

  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-semibold',
        isStrategic
          ? 'bg-purple-50 text-purple-700 border border-purple-200'
          : isPreferred
          ? 'bg-amber-50 text-amber-700 border border-amber-200'
          : 'bg-slate-100 text-slate-700',
        className
      )}
    >
      <Star className="w-3 h-3 fill-current" />
      <span>{vendorName}</span>
    </span>
  )
}
