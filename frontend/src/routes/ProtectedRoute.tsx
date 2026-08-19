import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/store/useAuthStore'
import { ROUTES } from '@/constants/routes'

/**
 * Redirects unauthenticated users to /login.
 * Wraps all protected route groups in AppRoutes.
 */
export function ProtectedRoute() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.login} replace />
  }

  return <Outlet />
}
