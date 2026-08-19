import React, { forwardRef } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from '@/utils/cn'

export interface SelectOption {
  value: string
  label: string
}

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?:      string
  error?:      string
  helperText?: string
  options:     SelectOption[]
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, label, error, helperText, options, id, required, disabled, ...props }, ref) => {
    const selectId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined)

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label
            htmlFor={selectId}
            className="block text-xs font-semibold uppercase tracking-wider text-slate-700"
          >
            {label}
            {required && <span className="text-red-500 ml-0.5">*</span>}
          </label>
        )}

        <div className="relative rounded-lg">
          <select
            id={selectId}
            ref={ref}
            disabled={disabled}
            className={cn(
              'block w-full text-sm bg-slate-50 border rounded-lg text-slate-900',
              'py-2 pl-3 pr-9 transition-all appearance-none cursor-pointer',
              'focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900 focus:border-slate-900',
              'disabled:bg-slate-100 disabled:text-slate-400 disabled:cursor-not-allowed',
              error
                ? 'border-red-300 focus:ring-red-500 focus:border-red-500 bg-red-50/10'
                : 'border-slate-200',
              className
            )}
            {...props}
          >
            {options.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

          <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none text-slate-400">
            <ChevronDown className="w-4 h-4" />
          </div>
        </div>

        {error && <p className="text-xs text-red-600 font-medium">{error}</p>}
        {!error && helperText && <p className="text-xs text-slate-500">{helperText}</p>}
      </div>
    )
  }
)

Select.displayName = 'Select'
