import { Outlet, Navigate, useLocation } from 'react-router-dom'
import { VendorSidebar } from './VendorSidebar'
import { VendorHeader } from './VendorHeader'
import { useVendorAuthStore } from '../../store/useVendorAuthStore'
import { ROUTES } from '@/constants/routes'

export function VendorLayout() {
  const { isAuthenticated } = useVendorAuthStore()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.vendor.login} state={{ from: location }} replace />
  }

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-slate-50 font-sans antialiased text-slate-900">
      <VendorSidebar />
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <VendorHeader />
        <main className="flex-1 overflow-y-auto p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
export default VendorLayout
