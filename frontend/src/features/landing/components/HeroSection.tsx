import { useNavigate } from 'react-router-dom'
import { ArrowRight, CheckCircle2, ShieldCheck, ChevronRight } from 'lucide-react'
import { ROUTES } from '@/constants/routes'
import { LANDING_COPY } from '../constants/landingCopy'

export function HeroSection() {
  const navigate = useNavigate()

  return (
    <section className="pt-14 pb-16 md:pt-20 md:pb-24 bg-white border-b border-slate-200">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto">
          {/* Top Pill */}
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-md bg-slate-100 border border-slate-200 text-slate-700 text-xs font-medium tracking-wide mb-6">
            <span className="w-1.5 h-1.5 rounded-full bg-brand-600" />
            {LANDING_COPY.hero.badge}
          </div>

          {/* Heading - Clean, authoritative typography */}
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-slate-900 tracking-tight leading-tight">
            {LANDING_COPY.hero.titleLine1}{' '}
            <span className="text-brand-700">
              {LANDING_COPY.hero.titleHighlight}
            </span>{' '}
            {LANDING_COPY.hero.titleLine2}
          </h1>

          {/* Subtitle */}
          <p className="mt-4 text-base sm:text-lg text-slate-600 leading-relaxed max-w-2xl mx-auto">
            {LANDING_COPY.hero.subtitle}
          </p>

          {/* CTAs */}
          <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
            <button
              onClick={() => navigate(ROUTES.login)}
              type="button"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 text-sm font-semibold text-white bg-slate-900 hover:bg-slate-800 rounded-lg transition-colors shadow-sm"
            >
              {LANDING_COPY.hero.ctaPrimary}
              <ArrowRight className="w-4 h-4" />
            </button>
            <a
              href="#lifecycle"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-1.5 px-5 py-2.5 text-sm font-medium text-slate-700 bg-white hover:bg-slate-50 border border-slate-300 rounded-lg transition-colors shadow-2xs"
            >
              {LANDING_COPY.hero.ctaSecondary}
              <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
            </a>
          </div>
        </div>

        {/* Corporate Pipeline Preview Card */}
        <div className="mt-12 max-w-4xl mx-auto">
          <div className="bg-slate-900 rounded-xl border border-slate-800 shadow-xl overflow-hidden text-slate-100">
            {/* Header bar */}
            <div className="px-5 py-3 border-b border-slate-800 flex items-center justify-between bg-slate-950">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-slate-700" />
                <div className="w-2.5 h-2.5 rounded-full bg-slate-700" />
                <div className="w-2.5 h-2.5 rounded-full bg-slate-700" />
                <span className="ml-2 text-xs font-mono text-slate-400 font-medium">
                  {LANDING_COPY.previewCard.title}
                </span>
              </div>
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-[11px] font-mono font-medium text-emerald-400 bg-emerald-950/60 border border-emerald-800/60">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                {LANDING_COPY.previewCard.statusBadge}
              </span>
            </div>

            {/* Pipeline Step Grid */}
            <div className="p-5 grid grid-cols-2 sm:grid-cols-5 gap-2.5 bg-slate-900">
              <div className="bg-slate-800/80 p-3 rounded border border-slate-700/80">
                <p className="text-[10px] font-mono text-slate-400 uppercase">1. Requisition</p>
                <p className="text-xs font-mono font-bold text-white mt-0.5">{LANDING_COPY.previewCard.step1.label}</p>
                <p className="text-[10px] text-emerald-400 mt-1 font-sans">✓ {LANDING_COPY.previewCard.step1.desc}</p>
              </div>

              <div className="bg-slate-800/80 p-3 rounded border border-slate-700/80">
                <p className="text-[10px] font-mono text-slate-400 uppercase">2. Order</p>
                <p className="text-xs font-mono font-bold text-white mt-0.5">{LANDING_COPY.previewCard.step2.label}</p>
                <p className="text-[10px] text-sky-400 mt-1 font-sans">✓ {LANDING_COPY.previewCard.step2.desc}</p>
              </div>

              <div className="bg-slate-800/80 p-3 rounded border border-slate-700/80">
                <p className="text-[10px] font-mono text-slate-400 uppercase">3. Receiving</p>
                <p className="text-xs font-mono font-bold text-white mt-0.5">{LANDING_COPY.previewCard.step3.label}</p>
                <p className="text-[10px] text-sky-400 mt-1 font-sans">✓ {LANDING_COPY.previewCard.step3.desc}</p>
              </div>

              <div className="bg-slate-800/80 p-3 rounded border border-slate-700/80">
                <p className="text-[10px] font-mono text-slate-400 uppercase">4. Match</p>
                <p className="text-xs font-mono font-bold text-white mt-0.5">{LANDING_COPY.previewCard.step4.label}</p>
                <p className="text-[10px] text-emerald-400 mt-1 font-sans">✓ {LANDING_COPY.previewCard.step4.desc}</p>
              </div>

              <div className="bg-slate-800 p-3 rounded border border-slate-600 col-span-2 sm:col-span-1">
                <p className="text-[10px] font-mono text-brand-300 uppercase">5. Settlement</p>
                <p className="text-xs font-mono font-bold text-white mt-0.5">{LANDING_COPY.previewCard.step5.label}</p>
                <p className="text-[10px] text-brand-300 mt-1 font-sans font-medium">✓ {LANDING_COPY.previewCard.step5.desc}</p>
              </div>
            </div>

            {/* Bottom Metric bar */}
            <div className="px-5 py-3 bg-slate-950 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs">
              <div className="flex items-center gap-2 text-slate-400">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                <span>Double-Spending Protection</span>
                <span className="text-slate-700">•</span>
                <ShieldCheck className="w-3.5 h-3.5 text-sky-400 shrink-0" />
                <span>Audited Transaction Flow</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-slate-400">{LANDING_COPY.previewCard.timeSaved}</span>
                <span className="font-mono font-bold text-white">
                  {LANDING_COPY.previewCard.amount}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
