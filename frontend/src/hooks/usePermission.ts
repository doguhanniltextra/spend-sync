import { useAuthStore } from '@/store/useAuthStore'

/**
 * Cross-cutting RBAC hook.
 * Returns a function that checks if the current user has a given permission string.
 *
 * Usage:
 *   const { can } = usePermission()
 *   if (can(PERMISSIONS.requisition.approve)) { ... }
 */
export function usePermission() {
  const hasPermission = useAuthStore((s) => s.hasPermission)
  const hasRole       = useAuthStore((s) => s.hasRole)

  return { can: hasPermission, hasRole }
}
