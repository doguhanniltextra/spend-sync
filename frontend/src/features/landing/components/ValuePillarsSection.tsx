import { Lock, Cpu, Activity } from 'lucide-react'
import { LANDING_COPY } from '../constants/landingCopy'

const PILLAR_ICONS = {
  'atomic-budget': Lock,
  'three-way-match': Cpu,
  'executive-pulse': Activity,
}

export function ValuePillarsSection() {
  return (
    <section id="features" className="py-16 bg-slate-50 border-b border-slate-200">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="max-w-2xl mb-12">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">
            {LANDING_COPY.pillars.badge}
          </p>
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
            {LANDING_COPY.pillars.heading}
          </h2>
          <p className="mt-2 text-sm text-slate-600">
            {LANDING_COPY.pillars.subheading}
          </p>
        </div>

        {/* 3 Pillar Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {LANDING_COPY.pillars.items.map((pillar) => {
            const Icon = PILLAR_ICONS[pillar.id as keyof typeof PILLAR_ICONS] ?? Activity

            return (
              <div
                key={pillar.id}
                className="bg-white rounded-lg p-6 border border-slate-200 shadow-2xs hover:border-slate-300 transition-colors"
              >
                <div className="w-10 h-10 rounded-md bg-slate-100 text-slate-800 flex items-center justify-center mb-5 border border-slate-200">
                  <Icon className="w-5 h-5" />
                </div>

                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-base font-bold text-slate-900">
                    {pillar.title}
                  </h3>
                </div>

                <p className="text-xs text-slate-600 leading-relaxed mb-4">
                  {pillar.description}
                </p>

                <span className="inline-block text-[10px] font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
                  {pillar.badge}
                </span>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
