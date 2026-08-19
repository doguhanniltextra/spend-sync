import { format, formatDistanceToNow, parseISO, isValid } from 'date-fns'

/**
 * Format an ISO date string to a human-readable date in English.
 * Output: "19 Aug 2026"
 */
export function formatDate(isoString: string | null | undefined): string {
  if (!isoString) return '—'
  const parsed = parseISO(isoString)
  if (!isValid(parsed)) return '—'
  return format(parsed, 'd MMM yyyy')
}

/**
 * Format an ISO date string to date + time in English.
 * Output: "19 Aug 2026 14:30"
 */
export function formatDateTime(isoString: string | null | undefined): string {
  if (!isoString) return '—'
  const parsed = parseISO(isoString)
  if (!isValid(parsed)) return '—'
  return format(parsed, 'd MMM yyyy HH:mm')
}

/**
 * Relative time label in English.
 * Output: "2 hours ago", "3 days ago"
 */
export function formatTimeAgo(isoString: string | null | undefined): string {
  if (!isoString) return '—'
  const parsed = parseISO(isoString)
  if (!isValid(parsed)) return '—'
  return formatDistanceToNow(parsed, { addSuffix: true })
}

/** Alias for formatTimeAgo */
export const fromNow = formatTimeAgo

/**
 * Format ISO string to fiscal period label in English.
 * Output: "August 2026"
 */
export function formatMonth(isoString: string | null | undefined): string {
  if (!isoString) return '—'
  const parsed = parseISO(isoString)
  if (!isValid(parsed)) return '—'
  return format(parsed, 'MMMM yyyy')
}
