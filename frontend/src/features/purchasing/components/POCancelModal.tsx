import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Textarea } from '@/components/ui/Textarea'
import { PURCHASING_COPY } from '../constants/purchasingCopy'

interface POCancelModalProps {
  isOpen:    boolean
  poNumber:  string
  isLoading: boolean
  onClose:   () => void
  onConfirm: (reason: string) => Promise<void>
}

export function POCancelModal({
  isOpen,
  poNumber,
  isLoading,
  onClose,
  onConfirm,
}: POCancelModalProps) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (reason.trim().length < 5) {
      setError(PURCHASING_COPY.modals.cancelReasonMinError)
      return
    }
    setError(null)
    await onConfirm(reason.trim())
    setReason('')
    onClose()
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`${PURCHASING_COPY.modals.cancelTitle} — ${poNumber}`}
      description={PURCHASING_COPY.modals.cancelDesc}
      maxWidth="sm"
      footer={
        <>
          <Button type="button" variant="outline" size="sm" onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button
            type="button"
            variant="danger"
            size="sm"
            onClick={handleSubmit}
            isLoading={isLoading}
          >
            Confirm Cancellation
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit}>
        <Textarea
          label={PURCHASING_COPY.modals.cancelReasonLabel}
          placeholder={PURCHASING_COPY.modals.cancelReasonPlaceholder}
          value={reason}
          onChange={(e) => {
            setReason(e.target.value)
            if (error) setError(null)
          }}
          error={error ?? undefined}
          rows={3}
          required
        />
      </form>
    </Modal>
  )
}
