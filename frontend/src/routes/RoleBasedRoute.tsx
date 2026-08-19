import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/useAuthStore'
import { ROUTES } from '@/constants/routes'
import { MESSAGES } from '@/constants/messages'

interface RoleBasedRouteProps {
  /** Permission string required to access this route (from PERMISSIONS constants). */
  permission: string
  children:   React.ReactNode
}

/**
 * RBAC access guard.
 * Renders children if the current user has the required permission.
 * Otherwise shows a 403 Forbidden screen.
 */
export function RoleBasedRoute({ permission, children }: RoleBasedRouteProps) {
  const hasPermission  = useAuthStore((s) => s.hasPermission)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.login} replace />
  }

  if (!hasPermission(permission)) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3 text-center">
        <span className="text-5xl">🔒</span>
        <h2 className="text-xl font-semibold text-slate-800">Access Denied</h2>
        <p className="text-sm text-slate-500 max-w-xs">{MESSAGES.common.forbidden}</p>
      </div>
    )
  }

  return <>{children}</>
}
