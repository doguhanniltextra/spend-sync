import { create } from 'zustand'
import { CheckCircle2, AlertCircle, Info, AlertTriangle, X } from 'lucide-react'
import { TIMING } from '@/constants/timing'
import { cn } from '@/utils/cn'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface ToastItem {
  id:       string
  type:     ToastType
  title:    string
  message?: string
}

interface ToastStore {
  toasts: ToastItem[]
  addToast: (toast: Omit<ToastItem, 'id'>) => void
  removeToast: (id: string) => void
}

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],
  addToast: (toast) => {
    const id = Math.random().toString(36).substring(2, 9)
    set((state) => ({ toasts: [...state.toasts, { ...toast, id }] }))

    const timeout =
      toast.type === 'error' ? TIMING.toast.errorDismiss : TIMING.toast.autoDismiss

    setTimeout(() => {
      set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }))
    }, timeout)
  },
  removeToast: (id) =>
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),
}))

export function useToast() {
  const addToast = useToastStore((s) => s.addToast)

  return {
    success: (title: string, message?: string) => addToast({ type: 'success', title, message }),
    error:   (title: string, message?: string) => addToast({ type: 'error', title, message }),
    warning: (title: string, message?: string) => addToast({ type: 'warning', title, message }),
    info:    (title: string, message?: string) => addToast({ type: 'info', title, message }),
  }
}

export function ToastContainer() {
  const toasts = useToastStore((s) => s.toasts)
  const removeToast = useToastStore((s) => s.removeToast)

  if (toasts.length === 0) return null

  const icons: Record<ToastType, React.ReactNode> = {
    success: <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />,
    error:   <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />,
    warning: <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />,
    info:    <Info className="w-4 h-4 text-blue-600 shrink-0" />,
  }

  const borderColors: Record<ToastType, string> = {
    success: 'border-emerald-300 bg-white',
    error:   'border-red-300 bg-white',
    warning: 'border-amber-300 bg-white',
    info:    'border-blue-300 bg-white',
  }

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2.5 max-w-sm w-full pointer-events-none">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={cn(
            'pointer-events-auto flex items-start gap-3 p-4 rounded-lg border shadow-lg animate-slide-up',
            borderColors[toast.type]
          )}
        >
          <div className="mt-0.5">{icons[toast.type]}</div>
          <div className="flex-1 min-w-0">
            <h5 className="text-xs font-bold text-slate-900">{toast.title}</h5>
            {toast.message && (
              <p className="text-xs text-slate-600 mt-0.5 leading-relaxed">{toast.message}</p>
            )}
          </div>
          <button
            onClick={() => removeToast(toast.id)}
            type="button"
            className="text-slate-400 hover:text-slate-600 rounded p-1 transition-colors"
            aria-label="Dismiss toast"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      ))}
    </div>
  )
}
