import { CheckCircle2, Clock, XCircle, MinusCircle } from 'lucide-react'
import type { ApprovalStepResponse, ApprovalStepStatus } from '@/types/requisition.types'
import { formatDateTime } from '@/utils/date'
import { cn } from '@/utils/cn'

interface ApprovalChainTimelineProps {
  steps: ApprovalStepResponse[]
}

export function ApprovalChainTimeline({ steps }: ApprovalChainTimelineProps) {
  if (!steps || steps.length === 0) {
    return (
      <div className="p-4 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-500 text-center">
        No formal approval steps generated. Self-approved within limit.
      </div>
    )
  }

  const sortedSteps = [...steps].sort((a, b) => a.stepOrder - b.stepOrder)

  const statusIcons: Record<ApprovalStepStatus, React.ReactNode> = {
    APPROVED: <CheckCircle2 className="w-4 h-4 text-emerald-600" />,
    PENDING:  <Clock className="w-4 h-4 text-amber-600 animate-pulse" />,
    REJECTED: <XCircle className="w-4 h-4 text-red-600" />,
    BYPASSED: <MinusCircle className="w-4 h-4 text-slate-400" />,
  }

  const statusColors: Record<ApprovalStepStatus, string> = {
    APPROVED: 'border-emerald-500 bg-emerald-50 text-emerald-800',
    PENDING:  'border-amber-500 bg-amber-50 text-amber-800',
    REJECTED: 'border-red-500 bg-red-50 text-red-800',
    BYPASSED: 'border-slate-300 bg-slate-50 text-slate-600',
  }

  return (
    <div className="space-y-3">
      {sortedSteps.map((step, idx) => {
        const isLast = idx === sortedSteps.length - 1

        return (
          <div key={step.id} className="relative flex items-start gap-3">
            {/* Step Icon */}
            <div className="mt-0.5 z-10 shrink-0 bg-white rounded-full">
              {statusIcons[step.status]}
            </div>

            {/* Connecting line */}
            {!isLast && (
              <div className="absolute left-[7px] top-5 bottom-[-14px] w-0.5 bg-slate-200" />
            )}

            {/* Step Card */}
            <div
              className={cn(
                'flex-1 p-3 rounded-lg border text-xs shadow-2xs transition-colors',
                statusColors[step.status]
              )}
            >
              <div className="flex items-center justify-between font-semibold">
                <span>
                  Step {step.stepOrder}: Level {step.approvalLevel} Authority
                </span>
                <span className="text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-white/70">
                  {step.status}
                </span>
              </div>

              <div className="mt-1 text-slate-700">
                <span>Approver: </span>
                <strong className="text-slate-900">{step.approverName}</strong>{' '}
                <span className="text-slate-500">({step.approverEmail})</span>
              </div>

              {step.decisionNote && (
                <p className="mt-1.5 text-[11px] text-slate-600 italic bg-white/50 p-1.5 rounded">
                  "{step.decisionNote}"
                </p>
              )}

              {step.decidedAt && (
                <p className="mt-1 text-[10px] text-slate-500">
                  Decided: {formatDateTime(step.decidedAt)}
                </p>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}
