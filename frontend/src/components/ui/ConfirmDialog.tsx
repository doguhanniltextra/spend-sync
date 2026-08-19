import { AlertTriangle } from 'lucide-react'
import { Modal } from './Modal'
import { Button } from './Button'
import { MESSAGES } from '@/constants/messages'

export interface ConfirmDialogProps {
  isOpen:        boolean
  onClose:       () => void
  onConfirm:     () => void | Promise<void>
  title:         string
  message:       string
  confirmLabel?: string
  cancelLabel?:  string
  isDestructive?:boolean
  isLoading?:    boolean
}

export function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = MESSAGES.common.confirm,
  cancelLabel  = MESSAGES.common.cancel,
  isDestructive = false,
  isLoading = false,
}: ConfirmDialogProps) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      maxWidth="sm"
      footer={
        <>
          <Button variant="outline" size="sm" onClick={onClose} disabled={isLoading}>
            {cancelLabel}
          </Button>
          <Button
            variant={isDestructive ? 'danger' : 'primary'}
            size="sm"
            onClick={onConfirm}
            isLoading={isLoading}
          >
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="flex items-start gap-3">
        {isDestructive && (
          <div className="w-9 h-9 rounded-full bg-red-50 text-red-600 flex items-center justify-center shrink-0 border border-red-200">
            <AlertTriangle className="w-5 h-5" />
          </div>
        )}
        <div>
          <h4 className="text-sm font-bold text-slate-900 mb-1">{title}</h4>
          <p className="text-xs text-slate-600 leading-relaxed">{message}</p>
        </div>
      </div>
    </Modal>
  )
}
