import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Textarea } from '@/components/ui/Textarea'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import type { RequisitionDetailResponse } from '@/types/requisition.types'
import { APPROVAL_COPY } from '../constants/approvalCopy'

export type DecisionMode = 'APPROVE' | 'REJECT'

interface ApprovalDecisionModalProps {
  isOpen:      boolean
  mode:        DecisionMode
  requisition: RequisitionDetailResponse | null
  isLoading:   boolean
  onClose:     () => void
  onConfirm:   (noteOrReason: string) => Promise<void>
}

export function ApprovalDecisionModal({
  isOpen,
  mode,
  requisition,
  isLoading,
  onClose,
  onConfirm,
}: ApprovalDecisionModalProps) {
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)

  if (!requisition) return null

  const isApprove = mode === 'APPROVE'
  const title = isApprove
    ? APPROVAL_COPY.modals.approveTitle
    : APPROVAL_COPY.modals.rejectTitle

  const description = isApprove
    ? APPROVAL_COPY.modals.approveDesc
    : APPROVAL_COPY.modals.rejectDesc

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!isApprove && text.trim().length < 5) {
      setError(APPROVAL_COPY.modals.rejectionReasonMinError)
      return
    }
    setError(null)
    await onConfirm(text.trim())
    setText('')
    onClose()
  }

  const prNumber = requisition.requisitionNumber ?? requisition.prNumber
  const requester = requisition.requisitionerName ?? requisition.requesterName
  const amount = requisition.totalAmount ?? requisition.totalEstimatedAmount ?? 0

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      description={description}
      maxWidth="md"
      footer={
        <>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onClose}
            disabled={isLoading}
          >
            Cancel
          </Button>

          <Button
            type="button"
            variant={isApprove ? 'primary' : 'danger'}
            size="sm"
            onClick={handleSubmit}
            isLoading={isLoading}
          >
            {isApprove
              ? APPROVAL_COPY.modals.confirmApproveCTA
              : APPROVAL_COPY.modals.confirmRejectCTA}
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4 text-xs">
        {/* Requisition Snapshot */}
        <div className="bg-slate-50 p-3.5 rounded-lg border border-slate-200 grid grid-cols-3 gap-3 font-mono">
          <div>
            <span className="text-[10px] text-slate-400 font-sans block">PR Number:</span>
            <strong className="text-slate-900">{prNumber}</strong>
          </div>

          <div>
            <span className="text-[10px] text-slate-400 font-sans block">Requester:</span>
            <span className="text-slate-800 font-sans font-medium">{requester}</span>
          </div>

          <div className="text-right">
            <span className="text-[10px] text-slate-400 font-sans block">Total Amount:</span>
            <CurrencyDisplay
              amount={amount}
              currency={requisition.currency as any}
              className="text-sm font-bold text-slate-900"
            />
          </div>
        </div>

        {/* Input Field */}
        <div>
          <Textarea
            label={
              isApprove
                ? APPROVAL_COPY.modals.decisionNoteLabel
                : APPROVAL_COPY.modals.rejectionReasonLabel
            }
            placeholder={
              isApprove
                ? APPROVAL_COPY.modals.decisionNotePlaceholder
                : APPROVAL_COPY.modals.rejectionReasonPlaceholder
            }
            value={text}
            onChange={(e) => {
              setText(e.target.value)
              if (error) setError(null)
            }}
            error={error ?? undefined}
            rows={3}
            required={!isApprove}
          />
        </div>
      </form>
    </Modal>
  )
}
