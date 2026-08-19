/**
 * All copy and label constants for the Executive Dashboard.
 * Zero hardcode policy — everything is defined here in English.
 */
export const DASHBOARD_COPY = {
  header: {
    title:              'Executive Spend Pulse',
    subtitle:           'Real-time financial cockpit and operational action items.',
    refreshButton:      'Refresh Data',
    lastUpdated:        'Last updated:',
  },
  metrics: {
    totalBudget: {
      label:            'Total Allocated Budget',
      sublabel:         'Fiscal Year 2026',
    },
    budgetUtilization: {
      label:            'Budget Utilization',
      sublabel:         'Spent + Reserved',
    },
    pendingApprovals: {
      label:            'Pending Approvals',
      sublabel:         'Awaiting manager sign-off',
    },
    duePayments: {
      label:            'Due Invoices Ready for Batch',
      sublabel:         'Approved for settlement',
    },
    discrepancyHolds: {
      label:            'Invoices on Hold',
      sublabel:         '3-Way match variance',
    },
  },
  actions: {
    sectionTitle:       '60-Second Action Items',
    sectionSubtitle:    'Urgent exceptions, pending approvals, and payment authorizations.',
    emptyTitle:         'Zero Pending Exceptions',
    emptyDesc:          'All requisitions are approved, invoices reconciled, and payment batches dispatched.',
    viewAction:         'Review',
    approveAction:      'Authorize',
    resolveAction:      'Inspect Variance',
  },
  budgetTable: {
    title:              'Cost Center Utilization Ledger',
    subtitle:           'Real-time allocated vs. reserved vs. spent breakdown.',
    colCostCenter:      'Cost Center',
    colAllocated:       'Allocated',
    colReserved:        'Reserved',
    colSpent:           'Spent',
    colAvailable:       'Available',
    colUtilization:     'Utilization',
    emptyText:          'No budget pools configured for this legal entity.',
  },
  pendingTable: {
    title:              'Approval Queue Priority List',
    subtitle:           'Requests requiring your direct signature.',
    colPrNumber:        'PR Number',
    colTitle:           'Title / Justification',
    colRequester:       'Requester',
    colCostCenter:      'Cost Center',
    colAmount:          'Amount',
    colAction:          'Action',
    emptyText:          'No pending requests in your approval queue.',
    quickApprove:       'Approve',
    quickReject:        'Reject',
  },
  requisitions: {
    title:              'My Active Requisitions',
    subtitle:           'Track live status of your procurement requests.',
    createCTA:          '+ New Purchase Request',
    colPrNumber:        'PR Number',
    colTitle:           'Title',
    colCostCenter:      'Cost Center',
    colAmount:          'Estimated Amount',
    colStatus:          'Status',
    colDate:            'Created Date',
    emptyText:          'You have not submitted any purchase requests yet.',
  },
  auditFeed: {
    title:              'Recent Audit Trail Events',
    subtitle:           'Immutable transactional ledger activity.',
    emptyText:          'No recent audit log entries recorded.',
  },
} as const
