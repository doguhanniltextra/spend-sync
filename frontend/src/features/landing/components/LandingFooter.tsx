import { Link } from 'react-router-dom'
import { ROUTES } from '@/constants/routes'
import { LANDING_COPY } from '../constants/landingCopy'

export function LandingFooter() {
  return (
    <footer className="bg-slate-950 text-slate-400 py-10 border-t border-slate-800 text-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md bg-brand-600 flex items-center justify-center text-white font-bold text-[10px]">
            SS
          </div>
          <p>{LANDING_COPY.footer.copyright}</p>
        </div>

        <div className="flex items-center gap-6">
          <Link to={ROUTES.home} className="hover:text-white transition-colors">
            {LANDING_COPY.footer.privacy}
          </Link>
          <Link to={ROUTES.home} className="hover:text-white transition-colors">
            {LANDING_COPY.footer.terms}
          </Link>
          <Link to={ROUTES.home} className="hover:text-white transition-colors">
            {LANDING_COPY.footer.security}
          </Link>
        </div>
      </div>
    </footer>
  )
}
