import React, { forwardRef } from 'react'
import { cn } from '@/utils/cn'

export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?:      string
  error?:      string
  helperText?: string
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, helperText, id, required, disabled, ...props }, ref) => {
    const textareaId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined)

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label
            htmlFor={textareaId}
            className="block text-xs font-semibold uppercase tracking-wider text-slate-700"
          >
            {label}
            {required && <span className="text-red-500 ml-0.5">*</span>}
          </label>
        )}

        <textarea
          id={textareaId}
          ref={ref}
          disabled={disabled}
          className={cn(
            'block w-full text-sm bg-slate-50 border rounded-lg text-slate-900 placeholder:text-slate-400',
            'py-2 px-3 transition-all min-h-[80px]',
            'focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-900 focus:border-slate-900',
            'disabled:bg-slate-100 disabled:text-slate-400 disabled:cursor-not-allowed',
            error
              ? 'border-red-300 focus:ring-red-500 focus:border-red-500 bg-red-50/10'
              : 'border-slate-200',
            className
          )}
          {...props}
        />

        {error && <p className="text-xs text-red-600 font-medium">{error}</p>}
        {!error && helperText && <p className="text-xs text-slate-500">{helperText}</p>}
      </div>
    )
  }
)

Textarea.displayName = 'Textarea'
