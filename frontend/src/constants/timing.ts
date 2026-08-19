/**
 * All duration/timing constants in milliseconds.
 * Never write raw `ms` numbers in code — always use TIMING.
 */
export const TIMING = {
  toast: {
    /** Duration before auto-dismiss for success/info toasts */
    autoDismiss:       4_000,
    /** Duration before auto-dismiss for error toasts (longer to read) */
    errorDismiss:      6_000,
  },
  query: {
    /** TanStack Query: data considered fresh for this duration */
    staleTime:         60_000,
    /** TanStack Query: inactive cache held for this duration */
    gcTime:            5 * 60_000,
    /** Dashboard & queue polling interval */
    dashboardRefresh:  30_000,
    /** Queue background refetch interval */
    refetchInterval:   30_000,
  },
  animation: {
    fast:   150,   // hover, focus ring
    normal: 250,   // sidebar collapse, modal open
    slow:   400,   // drawer slide
  },
  debounce: {
    /** Search input debounce delay */
    search: 300,
  },
  kpi: {
    /** KPI count-up animation duration */
    countUp: 800,
    /** Progress bar fill animation duration */
    progressFill: 600,
  },
} as const
