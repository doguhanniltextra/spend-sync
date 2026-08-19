/**
 * Environment variable wrapper.
 * All import.meta.env access goes through here — never scattered across files.
 */
export const ENV = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL as string,
  appName:    import.meta.env.VITE_APP_NAME    as string,
  version:    import.meta.env.VITE_APP_VERSION as string,
  isDev:      import.meta.env.DEV              as boolean,
  isProd:     import.meta.env.PROD             as boolean,
} as const
