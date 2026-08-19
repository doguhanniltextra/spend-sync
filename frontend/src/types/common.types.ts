/**
 * Common domain type primitives shared across all feature modules.
 * Import from here — never redefine in feature types.
 */

export type UUID = string

export type ISODateString = string   // "2026-08-19T14:30:00Z"

export type CurrencyCode = 'TRY' | 'USD' | 'EUR' | 'GBP'

/**
 * Money is always a paired (amount + currency).
 * Never store a bare `number` for financial amounts.
 */
export interface Money {
  amount:   number
  currency: CurrencyCode
}

/** Generic paginated API response wrapper */
export interface PagedResponse<T> {
  content:          T[]
  totalElements:    number
  totalPages:       number
  number:           number   // current page index (0-based)
  size:             number
  first:            boolean
  last:             boolean
}

/** Generic API error shape from Spring Boot */
export interface ApiError {
  timestamp: ISODateString
  status:    number
  error:     string
  message:   string
  path:      string
}
