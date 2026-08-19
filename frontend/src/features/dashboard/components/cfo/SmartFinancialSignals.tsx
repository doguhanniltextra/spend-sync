import { Sparkles, AlertTriangle, ShieldCheck, Zap } from 'lucide-react'

export function SmartFinancialSignals() {
  return (
    <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 text-white rounded-xl p-5 shadow-sm border border-slate-700">
      <div className="flex items-center justify-between border-b border-slate-700/60 pb-3 mb-3.5">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-amber-400/20 text-amber-300 flex items-center justify-center">
            <Sparkles className="w-3.5 h-3.5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              Autonomous CFO Financial Signals
              <span className="text-[9px] uppercase tracking-wider bg-indigo-500/30 text-indigo-200 border border-indigo-400/30 px-1.5 py-0.5 rounded font-mono">
                AI / Real-time
              </span>
            </h3>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
        {/* Signal 1 */}
        <div className="bg-white/5 border border-white/10 p-3 rounded-lg flex items-start gap-2.5">
          <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
          <div>
            <strong className="text-amber-200 block font-semibold text-xs">
              Cloud Ops Velocity Alert
            </strong>
            <p className="text-[11px] text-slate-300 leading-tight mt-0.5">
              `CC-300` has reached <strong>%66</strong> of annual allocation. Estimated exhaustion by October 2026.
            </p>
          </div>
        </div>

        {/* Signal 2 */}
        <div className="bg-white/5 border border-white/10 p-3 rounded-lg flex items-start gap-2.5">
          <Zap className="w-4 h-4 text-sky-400 shrink-0 mt-0.5" />
          <div>
            <strong className="text-sky-200 block font-semibold text-xs">
              Upcoming Liquidity Demand
            </strong>
            <p className="text-[11px] text-slate-300 leading-tight mt-0.5">
              <strong>173.000 TRY</strong> in commercial POs in transit. Treasury liquidity buffer required for next cycle.
            </p>
          </div>
        </div>

        {/* Signal 3 */}
        <div className="bg-white/5 border border-white/10 p-3 rounded-lg flex items-start gap-2.5">
          <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <div>
            <strong className="text-emerald-200 block font-semibold text-xs">
              3-Way Match Integrity
            </strong>
            <p className="text-[11px] text-slate-300 leading-tight mt-0.5">
              <strong>%100</strong> of settled invoices match verified PO & Goods Receipt lines. Zero price leak detected.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
