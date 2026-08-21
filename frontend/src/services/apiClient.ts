import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ENV } from '@/config/env'
import { useAuthStore } from '@/store/useAuthStore'
import { useTenantStore } from '@/store/useTenantStore'
import { useVendorAuthStore } from '@/features/vendorportal/store/useVendorAuthStore'

/**
 * Singleton Axios instance.
 * - Injects Bearer token from auth store (or vendor auth store for vendor endpoints).
 * - Injects X-Tenant-Id header from tenant store on every request.
 * - On 401, clears appropriate auth state and redirects.
 */
export const apiClient = axios.create({
  baseURL: `${ENV.apiBaseUrl}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ─── Request Interceptor ───────────────────────────────────────────────────────

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const isVendorRequest =
      config.url?.startsWith('/vendor-portal') ||
      window.location.pathname.startsWith('/vendor')

    if (isVendorRequest) {
      const vendorToken = useVendorAuthStore.getState().accessToken
      const vendorTenantId = useVendorAuthStore.getState().tenantId

      if (vendorToken) {
        config.headers.Authorization = `Bearer ${vendorToken}`
      }
      if (vendorTenantId) {
        config.headers['X-Tenant-Id'] = vendorTenantId
      }
    } else {
      const accessToken = useAuthStore.getState().accessToken
      const tenantId = useTenantStore.getState().tenantId || useAuthStore.getState().user?.tenantId

      if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`
      }
      if (tenantId) {
        config.headers['X-Tenant-Id'] = tenantId
      }
    }

    return config
  },
  (error) => Promise.reject(error)
)

// ─── Response Interceptor ─────────────────────────────────────────────────────

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      const isVendor = window.location.pathname.startsWith('/vendor')
      if (isVendor) {
        useVendorAuthStore.getState().logout()
        if (window.location.pathname !== '/vendor/login' && !window.location.pathname.startsWith('/vendor/invite')) {
          window.location.href = '/vendor/login'
        }
      } else {
        useAuthStore.getState().logout()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  }
)

