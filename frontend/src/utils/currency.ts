import type { CurrencyCode } from '@/types/common.types'

/**
 * Format a monetary amount with its ISO currency code.
 * Output: "10.000,00 TRY"  (always [amount] [ISO_CODE])
 *
 * Per FE-00 design contract:
 *  - ISO code suffix, not prefix symbol
 *  - Turkish locale formatting for TRY
 *  - 2 decimal places always
 *  - Negative values: (15.000,00 TRY)
 */
export function formatCurrency(amount: number, currency: CurrencyCode = 'TRY'): string {
  const locale = currency === 'TRY' ? 'tr-TR' : 'en-US'
  const abs    = Math.abs(amount)

  const formatted = new Intl.NumberFormat(locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(abs)

  const withCode = `${formatted} ${currency}`
  return amount < 0 ? `(${withCode})` : withCode
}

/**
 * Format a raw number with thousand separators (no currency code).
 * Used for quantity fields and non-monetary numbers.
 */
export function formatNumber(value: number, decimals = 0): string {
  return new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value)
}
