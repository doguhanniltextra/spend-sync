import { LANDING_COPY } from '../constants/landingCopy'

export function P2PLifecycleStepper() {
  return (
    <section id="lifecycle" className="py-16 bg-white border-b border-slate-200">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="max-w-2xl mb-12">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">
            {LANDING_COPY.lifecycle.badge}
          </p>
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
            {LANDING_COPY.lifecycle.heading}
          </h2>
          <p className="mt-2 text-sm text-slate-600">
            {LANDING_COPY.lifecycle.subheading}
          </p>
        </div>

        {/* 5 Stages Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          {LANDING_COPY.lifecycle.steps.map((step) => (
            <div
              key={step.number}
              className="bg-slate-50 rounded-lg p-5 border border-slate-200 hover:bg-white hover:border-slate-300 transition-colors flex flex-col justify-between"
            >
              <div>
                <span className="text-lg font-mono font-bold text-slate-900 mb-3 block">
                  {step.number}
                </span>
                <h3 className="text-sm font-bold text-slate-900 mb-1.5">
                  {step.title}
                </h3>
                <p className="text-xs text-slate-600 leading-relaxed">
                  {step.desc}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
