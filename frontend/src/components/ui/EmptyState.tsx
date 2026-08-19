import type { LucideIcon } from 'lucide-react'
import { FolderOpen } from 'lucide-react'
import { Button } from './Button'

export interface EmptyStateProps {
  icon?:        LucideIcon
  title:        string
  description?: string
  actionLabel?: string
  onAction?:    () => void
}

export function EmptyState({
  icon: Icon = FolderOpen,
  title,
  description,
  actionLabel,
  onAction,
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center p-10 text-center bg-white rounded-lg border border-slate-200">
      <div className="w-12 h-12 rounded-full bg-slate-100 text-slate-500 flex items-center justify-center mb-3.5 border border-slate-200">
        <Icon className="w-6 h-6" />
      </div>
      <h3 className="text-sm font-bold text-slate-900 mb-1">{title}</h3>
      {description && (
        <p className="text-xs text-slate-500 max-w-sm mb-5 leading-relaxed">
          {description}
        </p>
      )}
      {actionLabel && onAction && (
        <Button size="sm" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  )
}
