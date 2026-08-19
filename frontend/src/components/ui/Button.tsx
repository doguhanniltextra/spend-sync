import React, { forwardRef } from 'react'
import { Loader2 } from 'lucide-react'
import { cn } from '@/utils/cn'

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
export type ButtonSize = 'sm' | 'md' | 'lg'

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?:   ButtonVariant
  size?:      ButtonSize
  isLoading?: boolean
  leftIcon?:  React.ReactNode
  rightIcon?: React.ReactNode
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      disabled,
      children,
      ...props
    },
    ref
  ) => {
    const variantStyles: Record<ButtonVariant, string> = {
      primary:
        'bg-slate-900 text-white hover:bg-slate-800 focus-visible:ring-slate-900 shadow-2xs border border-transparent',
      secondary:
        'bg-slate-100 text-slate-900 hover:bg-slate-200 focus-visible:ring-slate-400 border border-slate-200 shadow-2xs',
      outline:
        'bg-white text-slate-700 hover:bg-slate-50 border border-slate-300 focus-visible:ring-slate-400 shadow-2xs',
      ghost:
        'bg-transparent text-slate-700 hover:bg-slate-100 focus-visible:ring-slate-400',
      danger:
        'bg-red-600 text-white hover:bg-red-700 focus-visible:ring-red-600 shadow-2xs border border-transparent',
    }

    const sizeStyles: Record<ButtonSize, string> = {
      sm: 'h-8 px-2.5 text-xs rounded-md gap-1.5',
      md: 'h-9 px-3.5 text-sm rounded-lg gap-2',
      lg: 'h-11 px-5 text-base rounded-lg gap-2.5',
    }

    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        className={cn(
          'inline-flex items-center justify-center font-semibold transition-all select-none',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
          'disabled:opacity-50 disabled:pointer-events-none active:scale-[0.98]',
          variantStyles[variant],
          sizeStyles[size],
          className
        )}
        {...props}
      >
        {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin shrink-0" />}
        {!isLoading && leftIcon && <span className="shrink-0">{leftIcon}</span>}
        {children}
        {!isLoading && rightIcon && <span className="shrink-0">{rightIcon}</span>}
      </button>
    )
  }
)

Button.displayName = 'Button'
