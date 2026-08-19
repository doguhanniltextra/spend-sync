/**
 * English copy constants for the Public Landing Page.
 * Strictly adheres to FE-FUND Zero Hard-Code policy.
 * Only lists real, concrete capabilities implemented in the codebase.
 */
export const LANDING_COPY = {
  nav: {
    brand:        'SpendSync',
    features:     'Features',
    lifecycle:    'P2P Lifecycle',
    security:     'Architecture & Security',
    signIn:       'Sign In',
    getStarted:   'Get Started',
  },
  hero: {
    badge:        'Enterprise Spend & P2P Management',
    titleLine1:   'Take Full Control of',
    titleHighlight:'Enterprise Spend',
    titleLine2:   'in Real-Time',
    subtitle:     'Eliminate unbudgeted spending, automate 3-way invoice reconciliation, and streamline payment releases with deterministic precision.',
    ctaPrimary:   'Sign In to Workspace',
    ctaSecondary: 'Explore P2P Lifecycle',
  },
  previewCard: {
    title:        'Procure-to-Pay Processing Pipeline',
    statusBadge:  'LIVE EXECUTION',
    step1:        { label: 'PR-2026-0004', desc: 'Budget Checked' },
    step2:        { label: 'PO-2026-0002', desc: 'Auto-Issued' },
    step3:        { label: 'GR-2026-0001', desc: 'Dock Inspected' },
    step4:        { label: 'INV-2026-0099', desc: '3-Way Matched' },
    step5:        { label: 'PAY-RUN-001', desc: 'Batch Dispatched' },
    amount:       '125,000.00 TRY',
    timeSaved:    'Automated pipeline execution',
  },
  pillars: {
    badge:        'Core Capabilities',
    heading:      'Built for High-Velocity Finance Operations',
    subheading:   'Engineered with strict pessimistic locking, multi-tenant isolation, and deterministic transaction workflows.',
    items: [
      {
        id:          'atomic-budget',
        title:       'Atomic Budget Engine',
        description: 'Pessimistic concurrency guards prevent overspending. Budgets are locked and committed at the exact millisecond of approval.',
        badge:       'Overspend Protection',
      },
      {
        id:          'three-way-match',
        title:       'Automated 3-Way Match',
        description: 'Line-by-line variance diffing across Purchase Order, Goods Receipt, and Supplier Invoice with configurable tolerance rules.',
        badge:       'Variance Inspection',
      },
      {
        id:          'executive-pulse',
        title:       '60-Second Executive Pulse',
        description: 'Role-based operational dashboard for CFOs, Approvers, and Requisitioners. Actionable items with zero cognitive clutter.',
        badge:       'Role-Based Visibility',
      },
    ],
  },
  lifecycle: {
    badge:        'Workflow Architecture',
    heading:      'The 5-Stage Procure-to-Pay Lifecycle',
    subheading:   'From initial employee requisition to payment batch release — structured and auditable.',
    steps: [
      {
        number: '01',
        title:  'Requisition & Budget Check',
        desc:   'Employees create PRs with dynamic line items. Department budget availability is validated in real-time before submission.',
      },
      {
        number: '02',
        title:  'Delegated Authority Approvals',
        desc:   'Multi-tier DoA approval chains automatically route requests based on monetary threshold and cost center assignment.',
      },
      {
        number: '03',
        title:  'Purchase Order & Dock Receiving',
        desc:   'Approved PRs convert to POs. Warehouse teams log dock receipts, inspect damaged goods, and record partial deliveries.',
      },
      {
        number: '04',
        title:  '3-Way Match & Variance Control',
        desc:   'Invoices are mathematically verified against POs and GRs. Matching records pass to payment; discrepancies enter hold status.',
      },
      {
        number: '05',
        title:  'Payment Release & Outbox Relay',
        desc:   'Approved invoices are bundled into payment batches and formatted as pain.001 XML files with transactional event logs.',
      },
    ],
  },
  compliance: {
    badge:    'Architecture & Governance',
    heading:  'Enterprise Governance & Auditability',
    items: [
      { title: 'Pain.001 XML Generation', desc: 'Outputs standard pain.001 XML payment batch files for bank transfer integration' },
      { title: 'Immutable Audit Trail', desc: 'Append-only chronological event log tracking every financial state change' },
      { title: 'Multi-Tenant RBAC', desc: 'Strict data isolation across legal entities, cost centers, and user roles' },
      { title: 'Segregation of Duties', desc: 'Double-spending protection, maker-checker authorization, and approval delegation' },
    ],
  },
  ctaSection: {
    heading:    'Ready to Access Your SpendSync Workspace?',
    subheading: 'Sign in with your enterprise credentials or contact your system administrator for access.',
    buttonText: 'Sign In to Workspace',
  },
  footer: {
    copyright:  '© 2026 SpendSync Inc. Enterprise Spend Management.',
    privacy:    'Privacy',
    terms:      'Terms',
    security:   'Architecture',
  },
} as const
