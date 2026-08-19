import { useEffect } from 'react'
import { X } from 'lucide-react'
import { cn } from '@/utils/cn'

export interface DrawerProps {
  isOpen:      boolean
  onClose:     () => void
  title?:      string
  subtitle?:   string
  children:    React.ReactNode
  footer?:     React.ReactNode
  size?:       'md' | 'lg' | 'xl' | '2xl'
}

export function Drawer({
  isOpen,
  onClose,
  title,
  subtitle,
  children,
  footer,
  size = 'lg',
}: DrawerProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  const sizeStyles: Record<string, string> = {
    md:  'max-w-md',
    lg:  'max-w-lg',
    xl:  'max-w-xl',
    '2xl': 'max-w-2xl',
  }

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      <div className="absolute inset-0 overflow-hidden">
        {/* Backdrop */}
        <div
          onClick={onClose}
          className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs transition-opacity animate-fade-in"
        />

        <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
          <div
            className={cn(
              'w-screen bg-white border-l border-slate-200 shadow-2xl flex flex-col',
              'animate-slide-right',
              sizeStyles[size]
            )}
          >
            {/* Header */}
            <div className="px-6 py-5 border-b border-slate-200 flex items-center justify-between bg-white shrink-0">
              <div>
                {title && <h3 className="text-base font-bold text-slate-900">{title}</h3>}
                {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
              </div>
              <button
                onClick={onClose}
                type="button"
                className="text-slate-400 hover:text-slate-600 rounded-lg p-1.5 transition-colors"
                aria-label="Close drawer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6">{children}</div>

            {/* Footer */}
            {footer && (
              <div className="px-6 py-4 border-t border-slate-200 bg-slate-50 shrink-0 flex items-center justify-end gap-3">
                {footer}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
