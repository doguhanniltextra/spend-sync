import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/useAuthStore'
import { ROUTES } from '@/constants/routes'
import { LandingNavbar } from './components/LandingNavbar'
import { HeroSection } from './components/HeroSection'
import { ValuePillarsSection } from './components/ValuePillarsSection'
import { P2PLifecycleStepper } from './components/P2PLifecycleStepper'
import { SecurityComplianceBanner } from './components/SecurityComplianceBanner'
import { LandingFooter } from './components/LandingFooter'

export default function LandingPage() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  // If already logged in, redirect directly to dashboard
  if (isAuthenticated) {
    return <Navigate to={ROUTES.dashboard} replace />
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col selection:bg-brand-500 selection:text-white">
      <LandingNavbar />
      <main className="flex-1">
        <HeroSection />
        <ValuePillarsSection />
        <P2PLifecycleStepper />
        <SecurityComplianceBanner />
      </main>
      <LandingFooter />
    </div>
  )
}
