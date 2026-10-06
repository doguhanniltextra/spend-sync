import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { AuthUser, AuthTokenResponse } from '@/types/auth.types'
import { useTenantStore } from './useTenantStore'

interface AuthState {
  user:         AuthUser | null
  accessToken:  string | null
  refreshToken: string | null
  isAuthenticated: boolean

  /** Called after successful login response */
  setAuth: (response: AuthTokenResponse) => void
  /** Clears all auth state — called on logout or 401 */
  logout:  () => void
  /** Check if user has a specific role string */
  hasRole: (role: string) => boolean
  /** Check if user has a specific permission string */
  hasPermission: (permission: string) => boolean
}

const ROLE_PERMISSIONS: Record<string, string[]> = {
  ROOT_USER: [
    'ORG_MANAGE', 'USER_MANAGE', 'INVITATION_CREATE', 'BUDGET_READ', 'BUDGET_MANAGE',
    'PR_CREATE', 'PR_READ_ALL', 'PR_READ_OWN', 'PR_APPROVE', 'PR_REJECT', 'PR_CANCEL',
    'PO_CREATE', 'PO_READ', 'PO_UPDATE', 'PO_ISSUE', 'PO_CANCEL', 'VENDOR_MANAGE',
    'GR_CREATE', 'GR_READ', 'INVOICE_CREATE', 'INVOICE_READ', 'MATCH_EVALUATE', 'MATCH_OVERRIDE',
    'PAYMENT_RELEASE', 'PAYMENT_READ', 'AUDIT_READ',
  ],
  APPROVER: [
    'PR_READ_ALL', 'PR_READ_OWN', 'PR_APPROVE', 'PR_REJECT', 'BUDGET_READ',
  ],
  REQUISITIONER: [
    'PR_CREATE', 'PR_READ_OWN',
  ],
  PROCUREMENT: [
    'PR_READ_ALL', 'PR_READ_OWN', 'PO_CREATE', 'PO_READ', 'PO_UPDATE', 'PO_ISSUE', 'PO_CANCEL',
    'VENDOR_MANAGE', 'GR_CREATE', 'GR_READ', 'MATCH_EVALUATE',
  ],
  AP_SPECIALIST: [
    'INVOICE_CREATE', 'INVOICE_READ', 'MATCH_EVALUATE', 'PAYMENT_RELEASE', 'PAYMENT_READ',
    'PO_READ', 'GR_READ',
  ],
  ACCOUNT_USER: [
    'BUDGET_READ', 'BUDGET_MANAGE', 'PR_READ_ALL', 'PO_READ', 'GR_READ',
    'INVOICE_READ', 'PAYMENT_RELEASE', 'PAYMENT_READ', 'AUDIT_READ',
  ],
  FACILITY_USER: [
    'GR_CREATE', 'GR_READ', 'PO_READ',
  ],
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user:            null,
      accessToken:     null,
      refreshToken:    null,
      isAuthenticated: false,

      setAuth: (response: AuthTokenResponse) => {
        let roles: string[] = []
        if (Array.isArray(response?.roles)) {
          roles = response.roles
        } else if (response?.roles && typeof (response.roles as any)[Symbol.iterator] === 'function') {
          roles = Array.from(response.roles)
        }

        const userId = response?.userId || (response as any)?.id
        const tenantId = response?.tenantId || (response as any)?.tenant_id
        const fullName = response?.fullName || (response as any)?.name || 'User'
        const email = response?.email || ''

        set({
          accessToken:     response?.accessToken || (response as any)?.access_token || null,
          refreshToken:    response?.refreshToken || (response as any)?.refresh_token || null,
          isAuthenticated: true,
          user: {
            id:       userId,
            email,
            fullName,
            tenantId,
            roles,
          },
        })
        if (tenantId) {
          useTenantStore.getState().setTenant(tenantId)
        }
      },

      logout: () => {
        set({
          user:            null,
          accessToken:     null,
          refreshToken:    null,
          isAuthenticated: false,
        })
        useTenantStore.getState().clearTenant()
      },

      hasRole: (role: string) => {
        const { user } = get()
        if (!user || !Array.isArray(user.roles)) return false
        const normalizedRole = role.replace(/^ROLE_/, '')
        return user.roles.some(
          (r) =>
            r === role ||
            r === normalizedRole ||
            r === `ROLE_${normalizedRole}` ||
            r === 'ROOT_USER' ||
            r === 'ROLE_ROOT_USER'
        )
      },

      /**
       * Permission check:
       * 1. ROOT_USER / ADMIN has all permissions.
       * 2. Evaluates role-to-permission mapping from backend RolePermissionRegistry.
       */
      hasPermission: (permission: string) => {
        const { user } = get()
        if (!user || !Array.isArray(user.roles)) return false

        // Superusers have full access
        const isSuperUser = user.roles.some(
          (r) =>
            r === 'ROOT_USER' ||
            r === 'ROLE_ROOT_USER' ||
            r === 'ADMIN' ||
            r === 'ROLE_ADMIN' ||
            r === 'CFO' ||
            r === 'ROLE_CFO'
        )
        if (isSuperUser) return true

        const normalizedPerm = permission.replace(/^(PERM_|ROLE_)/, '')

        return user.roles.some((r) => {
          const roleKey = r.replace(/^ROLE_/, '')
          if (roleKey === normalizedPerm || r === permission) return true
          const perms = ROLE_PERMISSIONS[roleKey] || []
          return perms.includes(normalizedPerm) || perms.includes(`PERM_${normalizedPerm}`)
        })
      },
    }),
    {
      name:    'spendsync_auth',
      storage: createJSONStorage(() => localStorage),
      // Only persist tokens and user — not function references
      partialize: (state) => ({
        user:         state.user,
        accessToken:  state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)
