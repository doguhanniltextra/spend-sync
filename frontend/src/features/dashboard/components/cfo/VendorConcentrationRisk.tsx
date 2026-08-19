import { ShieldAlert } from 'lucide-react'
import { formatCurrency } from '@/utils/currency'

interface VendorRisk {
  name:        string
  taxId:       string
  volume:      number
  sharePercent:number
  tier:        string
  riskLevel:   'HIGH' | 'MEDIUM' | 'LOW'
}

const TOP_VENDORS: VendorRisk[] = [
  {
    name:        'Amazon Web Services EMEA SARL',
    taxId:       'LU1200000000000000',
    volume:      4540000,
    sharePercent:38,
    tier:        'TIER_1_STRATEGIC',
    riskLevel:   'HIGH',
  },
  {
    name:        'Apple Bilgi Teknolojileri Tic. Ltd. Şti.',
    taxId:       '8844112233',
    volume:      3280000,
    sharePercent:27,
    tier:        'TIER_1_STRATEGIC',
    riskLevel:   'MEDIUM',
  },
  {
    name:        'Datadog Europe B.V.',
    taxId:       'NL8822991100',
    volume:      1420000,
    sharePercent:12,
    tier:        'TIER_2_PREFERRED',
    riskLevel:   'LOW',
  },
  {
    name:        'Vatan Bilgisayar San. ve Tic. A.Ş.',
    taxId:       '9911223344',
    volume:      890000,
    sharePercent:8,
    tier:        'TIER_3_STANDARD',
    riskLevel:   'LOW',
  },
]

export function VendorConcentrationRisk() {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs">
      <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-700 flex items-center justify-center">
            <ShieldAlert className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">Supplier Concentration & Pareto Risk</h3>
            <p className="text-[11px] text-slate-500">Procurement dependency ratio and counterparty risk scoring</p>
          </div>
        </div>
        <span className="text-[10px] font-bold text-amber-700 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded">
          Top 2 = %65 Total Spend
        </span>
      </div>

      <div className="space-y-3">
        {TOP_VENDORS.map((v) => (
          <div key={v.name} className="space-y-1.5 bg-slate-50/70 p-2.5 rounded-lg border border-slate-100">
            <div className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2 truncate">
                <strong className="text-slate-900 font-semibold truncate">{v.name}</strong>
                <span className="text-[10px] font-mono text-slate-400">({v.tier.replace('_', ' ')})</span>
              </div>
              <div className="flex items-center gap-2 font-mono shrink-0">
                <span className="font-bold text-slate-900">{formatCurrency(v.volume)}</span>
                <span className="text-slate-500 font-semibold">%{v.sharePercent}</span>
              </div>
            </div>

            {/* Progress Bar */}
            <div className="w-full bg-slate-200 h-2 rounded-full overflow-hidden flex">
              <div
                style={{ width: `${v.sharePercent}%` }}
                className={`h-full rounded-full ${
                  v.riskLevel === 'HIGH'
                    ? 'bg-purple-600'
                    : v.riskLevel === 'MEDIUM'
                    ? 'bg-blue-600'
                    : 'bg-slate-500'
                }`}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
