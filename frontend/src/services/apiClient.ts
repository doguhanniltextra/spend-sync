import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ENV } from '@/config/env'
import { useAuthStore } from '@/store/useAuthStore'
import { useTenantStore } from '@/store/useTenantStore'

/**
 * Singleton Axios instance.
 * - Injects Bearer token from auth store on every request.
 * - Injects X-Tenant-Id header from tenant store on every request.
 * - On 401, clears auth state and redirects to /login.
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
    const accessToken = useAuthStore.getState().accessToken
    const tenantId = useTenantStore.getState().tenantId

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId
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
      useAuthStore.getState().logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
