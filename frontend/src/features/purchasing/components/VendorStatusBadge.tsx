import { FileCheck } from 'lucide-react'
import type { VendorStatus, VendorTier } from '@/types/purchasing.types'
import { cn } from '@/utils/cn'

export function EInvoiceBadge({ isRegistered }: { isRegistered: boolean }) {
  if (!isRegistered) {
    return (
      <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-500 border border-slate-200">
        Paper Invoice
      </span>
    )
  }

  return (
    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-300 font-mono">
      <FileCheck className="w-3 h-3 text-emerald-600" />
      e-Invoice
    </span>
  )
}

export function VendorTierBadge({ tier }: { tier: VendorTier }) {
  const styles: Record<VendorTier, { label: string; class: string }> = {
    TIER_1_STRATEGIC: { label: 'Tier 1 Strategic', class: 'bg-slate-900 text-white border-slate-900 font-semibold' },
    TIER_2_PREFERRED: { label: 'Tier 2 Preferred', class: 'bg-emerald-50 text-emerald-800 border-emerald-300 font-semibold' },
    TIER_3_STANDARD:  { label: 'Tier 3 Standard',  class: 'bg-blue-50 text-blue-800 border-blue-200' },
  }

  const conf = styles[tier] || styles.TIER_3_STANDARD

  return (
    <span
      className={cn(
        'inline-flex items-center px-2 py-0.5 rounded text-[11px] border',
        conf.class
      )}
    >
      {conf.label}
    </span>
  )
}

export function VendorStatusBadge({ status }: { status: VendorStatus }) {
  const styles: Record<VendorStatus, { label: string; class: string }> = {
    ACTIVE:   { label: 'Active',   class: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
    BLOCKED:  { label: 'Blocked',  class: 'bg-red-50 text-red-700 border-red-200' },
    INACTIVE: { label: 'Inactive', class: 'bg-slate-100 text-slate-600 border-slate-200' },
  }

  const conf = styles[status] || styles.ACTIVE

  return (
    <span
      className={cn(
        'inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium border',
        conf.class
      )}
    >
      {conf.label}
    </span>
  )
}
