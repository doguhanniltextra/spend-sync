import React from 'react'
import type { CurrencyCode } from '@/types/common.types'
import { formatCurrency } from '@/utils/currency'
import { cn } from '@/utils/cn'

export interface CurrencyDisplayProps {
  amount:     number
  currency?:  CurrencyCode
  className?: string
  isBold?:    boolean
}

export function CurrencyDisplay({
  amount,
  currency = 'TRY',
  className,
  isBold = false,
}: CurrencyDisplayProps) {
  const formatted = formatCurrency(amount, currency)
  const isNegative = amount < 0

  return (
    <span
      className={cn(
        'font-mono tabular-nums inline-block',
        isBold ? 'font-bold' : 'font-medium',
        isNegative ? 'text-red-700' : 'text-slate-900',
        className
      )}
    >
      {formatted}
    </span>
  )
}

export interface MoneyInputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
  label?:     string
  value:      number
  currency?:  CurrencyCode
  error?:     string
  onChange:   (value: number) => void
}

export function MoneyInput({
  label,
  value,
  currency = 'TRY',
  error,
  onChange,
  disabled,
  className,
  required,
  ...props
}: MoneyInputProps) {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value.replace(/[^0-9.]/g, '')
    const parsed = parseFloat(raw)
    onChange(isNaN(parsed) ? 0 : parsed)
  }

  return (
    <div className="w-full space-y-1.5">
      {label && (
        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700">
          {label}
          {required && <span className="text-red-500 ml-0.5">*</span>}
        </label>
      )}

      <div className="relative rounded-lg">
        <input
          type="number"
          step="0.01"
          min="0"
          value={value === 0 ? '' : value}
          onChange={handleChange}
          disabled={disabled}
          placeholder="0.00"
          className={cn(
            'block w-full text-sm font-mono bg-slate-50 border rounded-lg text-slate-900 placeholder:text-slate-400',
            'py-2 pl-3 pr-16 text-right transition-all',
            'focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900 focus:border-slate-900',
            'disabled:bg-slate-100 disabled:text-slate-400 disabled:cursor-not-allowed',
            error ? 'border-red-300' : 'border-slate-200',
            className
          )}
          {...props}
        />
        <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none text-xs font-mono font-semibold text-slate-500">
          {currency}
        </div>
      </div>

      {error && <p className="text-xs text-red-600 font-medium">{error}</p>}
    </div>
  )
}
