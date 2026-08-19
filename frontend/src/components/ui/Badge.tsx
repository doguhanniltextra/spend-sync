import { cn } from '@/utils/cn'
import { getStatusConfig } from '@/constants/workflow'

export type BadgeVariant = 'default' | 'success' | 'warning' | 'danger' | 'info' | 'neutral'

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant
  icon?:    React.ReactNode
}

export function Badge({
  className,
  variant = 'default',
  icon,
  children,
  ...props
}: BadgeProps) {
  const variantStyles: Record<BadgeVariant, string> = {
    default: 'bg-slate-100 text-slate-700 border-slate-200',
    success: 'bg-emerald-50 text-emerald-700 border-emerald-300',
    warning: 'bg-amber-50 text-amber-700 border-amber-300',
    danger:  'bg-red-50 text-red-700 border-red-300',
    info:    'bg-blue-50 text-blue-700 border-blue-300',
    neutral: 'bg-slate-50 text-slate-600 border-slate-200',
  }

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium border',
        variantStyles[variant],
        className
      )}
      {...props}
    >
      {icon && <span className="shrink-0">{icon}</span>}
      {children}
    </span>
  )
}

interface StatusBadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  status: string
  showIcon?: boolean
}

/**
 * Domain-specific StatusBadge automatically mapped to STATUS_CONFIG.
 * Open/Closed Principle: Never needs modification when new workflow statuses are added.
 */
export function StatusBadge({
  status,
  showIcon = true,
  className,
  ...props
}: StatusBadgeProps) {
  const config = getStatusConfig(status)
  const Icon = config.icon

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium border',
        config.className,
        className
      )}
      {...props}
    >
      {showIcon && <Icon className="w-3 h-3 shrink-0" />}
      <span>{config.label}</span>
    </span>
  )
}
