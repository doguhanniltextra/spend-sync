/**
 * Workflow status constants and their visual configuration.
 * Open/Closed Principle: adding a new status only requires a new entry here.
 * Badge and other components never need to be modified.
 */
import {
  Clock,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Ban,
  CreditCard,
  PackageCheck,
  Zap,
  PackageOpen,
  type LucideIcon,
} from 'lucide-react'

// ─── Status value constants ────────────────────────────────────────────────────

export const WORKFLOW_STATUS = {
  DRAFT:               'DRAFT',
  PENDING_APPROVAL:    'PENDING_APPROVAL',
  APPROVED:            'APPROVED',
  REJECTED:            'REJECTED',
  CANCELLED:           'CANCELLED',
  ISSUED:              'ISSUED',
  FULFILLED:           'FULFILLED',
  PARTIALLY_RECEIVED:  'PARTIALLY_RECEIVED',
  AUTO_MATCHED:        'AUTO_MATCHED',
  DISCREPANCY_HOLD:    'DISCREPANCY_HOLD',
  APPROVED_FOR_PAYMENT:'APPROVED_FOR_PAYMENT',
  PAID:                'PAID',
  SETTLED:             'SETTLED',
  DISPATCHED:          'DISPATCHED',
  FAILED:              'FAILED',
  PENDING:             'PENDING',
  SENT:                'SENT',
} as const

export type WorkflowStatus = typeof WORKFLOW_STATUS[keyof typeof WORKFLOW_STATUS]

// ─── Visual config per status ──────────────────────────────────────────────────

export interface StatusConfig {
  label:     string
  className: string    // Tailwind badge classes (bg + text + border)
  icon:      LucideIcon
}

export const STATUS_CONFIG: Record<string, StatusConfig> = {
  [WORKFLOW_STATUS.DRAFT]: {
    label:     'Draft',
    className: 'bg-slate-100 text-slate-600 border border-slate-300',
    icon:      Clock,
  },
  [WORKFLOW_STATUS.PENDING_APPROVAL]: {
    label:     'Pending Approval',
    className: 'bg-amber-50 text-amber-700 border border-amber-300',
    icon:      Clock,
  },
  [WORKFLOW_STATUS.PENDING]: {
    label:     'Pending',
    className: 'bg-amber-50 text-amber-700 border border-amber-300',
    icon:      Clock,
  },
  [WORKFLOW_STATUS.APPROVED]: {
    label:     'Approved',
    className: 'bg-emerald-50 text-emerald-700 border border-emerald-300',
    icon:      CheckCircle,
  },
  [WORKFLOW_STATUS.AUTO_MATCHED]: {
    label:     'Auto Matched',
    className: 'bg-emerald-50 text-emerald-700 border border-emerald-300',
    icon:      Zap,
  },
  [WORKFLOW_STATUS.APPROVED_FOR_PAYMENT]: {
    label:     'Ready for Payment',
    className: 'bg-emerald-50 text-emerald-700 border border-emerald-300',
    icon:      CheckCircle,
  },
  [WORKFLOW_STATUS.REJECTED]: {
    label:     'Rejected',
    className: 'bg-red-50 text-red-700 border border-red-300',
    icon:      XCircle,
  },
  [WORKFLOW_STATUS.CANCELLED]: {
    label:     'Cancelled',
    className: 'bg-slate-50 text-slate-400 border border-slate-200',
    icon:      Ban,
  },
  [WORKFLOW_STATUS.DISCREPANCY_HOLD]: {
    label:     'Discrepancy Hold',
    className: 'bg-orange-50 text-orange-700 border border-orange-300',
    icon:      AlertTriangle,
  },
  [WORKFLOW_STATUS.ISSUED]: {
    label:     'Issued',
    className: 'bg-blue-50 text-blue-700 border border-blue-300',
    icon:      CheckCircle,
  },
  [WORKFLOW_STATUS.FULFILLED]: {
    label:     'Fulfilled',
    className: 'bg-blue-50 text-blue-700 border border-blue-300',
    icon:      PackageCheck,
  },
  [WORKFLOW_STATUS.PARTIALLY_RECEIVED]: {
    label:     'Partially Received',
    className: 'bg-violet-50 text-violet-700 border border-violet-300',
    icon:      PackageOpen,
  },
  [WORKFLOW_STATUS.PAID]: {
    label:     'Paid',
    className: 'bg-blue-50 text-blue-700 border border-blue-300',
    icon:      CreditCard,
  },
  [WORKFLOW_STATUS.SETTLED]: {
    label:     'Settled',
    className: 'bg-blue-50 text-blue-700 border border-blue-300',
    icon:      CreditCard,
  },
  [WORKFLOW_STATUS.DISPATCHED]: {
    label:     'Dispatched',
    className: 'bg-blue-50 text-blue-700 border border-blue-300',
    icon:      CreditCard,
  },
  [WORKFLOW_STATUS.SENT]: {
    label:     'Sent',
    className: 'bg-emerald-50 text-emerald-700 border border-emerald-300',
    icon:      CheckCircle,
  },
  [WORKFLOW_STATUS.FAILED]: {
    label:     'Failed',
    className: 'bg-red-50 text-red-700 border border-red-300',
    icon:      XCircle,
  },
}

/** Returns status config; falls back to a generic grey badge for unknown statuses. */
export function getStatusConfig(status: string): StatusConfig {
  return STATUS_CONFIG[status] ?? {
    label:     status,
    className: 'bg-slate-100 text-slate-600 border border-slate-300',
    icon:      Clock,
  }
}
