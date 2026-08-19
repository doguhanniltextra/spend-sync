import { Clock, AlertTriangle, CheckCircle2, XCircle } from 'lucide-react'
import { clsx } from 'clsx'

interface ContractStatusBadgeProps {
  validUntil?: string
  contractAlert?: string
  className?: string
}

export function ContractStatusBadge({ validUntil, contractAlert, className }: ContractStatusBadgeProps) {
  if (!validUntil) {
    return (
      <span className={clsx('inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-600', className)}>
        <span>Standard Price</span>
      </span>
    )
  }

  const today = new Date()
  const expDate = new Date(validUntil)
  const diffDays = Math.ceil((expDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))

  if (diffDays < 0) {
    return (
      <span className={clsx('inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-red-50 text-red-700 border border-red-200', className)} title={contractAlert || 'Contract has expired'}>
        <XCircle className="w-3 h-3 text-red-600" />
        <span>Expired ({validUntil})</span>
      </span>
    )
  }

  if (diffDays <= 7) {
    return (
      <span className={clsx('inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-amber-50 text-amber-800 border border-amber-300 animate-pulse', className)} title={contractAlert || `${diffDays} days remaining`}>
        <AlertTriangle className="w-3 h-3 text-amber-600" />
        <span>Critical: {diffDays}d Left</span>
      </span>
    )
  }

  if (diffDays <= 30) {
    return (
      <span className={clsx('inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-yellow-50 text-yellow-800 border border-yellow-200', className)} title={contractAlert || `${diffDays} days remaining`}>
        <Clock className="w-3 h-3 text-yellow-600" />
        <span>{diffDays}d Left</span>
      </span>
    )
  }

  return (
    <span className={clsx('inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200', className)}>
      <CheckCircle2 className="w-3 h-3 text-emerald-600" />
      <span>Contracted ({validUntil})</span>
    </span>
  )
}
