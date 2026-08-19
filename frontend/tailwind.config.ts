import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './index.html',
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        // Corporate Enterprise Palette: Deep Navy & Professional Slate Blue
        brand: {
          50:  '#f0f7ff',
          100: '#e0effe',
          200: '#bae0fd',
          300: '#7cc7fb',
          400: '#38a8f8',
          500: '#0e87eb',
          600: '#0267c7', // Primary Action Blue (clean & professional)
          700: '#0252a1',
          800: '#064684',
          900: '#0b3b6f', // Deep Enterprise Navy
          950: '#07264a',
        },
        // Clean high-contrast surfaces
        surface: {
          DEFAULT: '#f8fafc',
          card:    '#ffffff',
          sidebar: '#ffffff',
          hover:   '#f1f5f9',
          border:  '#e2e8f0',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      borderRadius: {
        'lg': '0.5rem',
        'xl': '0.75rem',
        '2xl': '1rem',
      },
      animation: {
        'fade-in':     'fadeIn 150ms ease-out',
        'fade-out':    'fadeOut 150ms ease-in',
        'slide-up':    'slideUp 200ms ease-out',
        'slide-right': 'slideInRight 280ms ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        fadeOut: {
          '0%': { opacity: '1' },
          '100%': { opacity: '0' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideInRight: {
          '0%': { opacity: '0', transform: 'translateX(16px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
      },
    },
  },
  plugins: [],
}

export default config
