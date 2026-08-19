import { useNavigate } from 'react-router-dom'
import { Shield, FileCode2, Users2, Scale, ArrowRight } from 'lucide-react'
import { ROUTES } from '@/constants/routes'
import { LANDING_COPY } from '../constants/landingCopy'

const COMPLIANCE_ICONS = [FileCode2, Shield, Users2, Scale]

export function SecurityComplianceBanner() {
  const navigate = useNavigate()

  return (
    <section id="security" className="py-16 bg-slate-900 text-white">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mb-12">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2 block">
            {LANDING_COPY.compliance.badge}
          </span>
          <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
            {LANDING_COPY.compliance.heading}
          </h2>
        </div>

        {/* 4 Standards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-12">
          {LANDING_COPY.compliance.items.map((item, idx) => {
            const Icon = COMPLIANCE_ICONS[idx] ?? Shield

            return (
              <div
                key={item.title}
                className="bg-slate-800/80 rounded-lg p-5 border border-slate-700 hover:border-slate-600 transition-colors"
              >
                <div className="w-8 h-8 rounded bg-slate-700 text-slate-200 flex items-center justify-center mb-3">
                  <Icon className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-semibold text-white mb-1">
                  {item.title}
                </h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  {item.desc}
                </p>
              </div>
            )
          })}
        </div>

        {/* Bottom Callout Banner */}
        <div className="bg-slate-800 rounded-xl p-6 sm:p-8 border border-slate-700 flex flex-col sm:flex-row items-center justify-between gap-6">
          <div>
            <h3 className="text-lg font-bold text-white mb-1">
              {LANDING_COPY.ctaSection.heading}
            </h3>
            <p className="text-slate-400 text-xs sm:text-sm max-w-lg">
              {LANDING_COPY.ctaSection.subheading}
            </p>
          </div>
          <button
            onClick={() => navigate(ROUTES.login)}
            type="button"
            className="shrink-0 inline-flex items-center gap-2 px-5 py-2.5 text-sm font-semibold text-slate-900 bg-white hover:bg-slate-100 rounded-lg transition-colors shadow-sm"
          >
            {LANDING_COPY.ctaSection.buttonText}
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </section>
  )
}
