/**
 * Business rule threshold constants.
 * All numeric boundary values live here — never hardcoded in component logic.
 */
export const THRESHOLDS = {
  budget: {
    /** Show yellow warning progress bar above this utilization % */
    warningPercent:  80,
    /** Show red critical progress bar above this utilization % */
    criticalPercent: 95,
  },
  approval: {
    /** Requisitions above this amount (TRY) require second-level approval */
    highValueAmount: 50_000,
    /** Days waiting in approval queue before showing SLA breach warning */
    urgentAgingDays: 3,
  },
  matching: {
    /** 3-Way match: acceptable variance as a percentage of invoice total */
    tolerancePercent: 1,
    /** 3-Way match: absolute tolerance floor in currency units */
    toleranceFixed:   100,
  },
  payment: {
    /** Maximum number of invoices in a single payment batch */
    batchMaxInvoices: 50,
  },
  table: {
    /** Default rows per page for all paginated tables */
    defaultPageSize: 20,
    /** Available page size options */
    pageSizeOptions: [10, 20, 50, 100] as const,
  },
} as const
