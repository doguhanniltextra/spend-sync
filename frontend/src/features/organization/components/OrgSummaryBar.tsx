import { Building2, Layers, Truck, Users, ShieldCheck } from 'lucide-react'
import { ORG_COPY } from '../constants/organizationCopy'

interface OrgSummaryBarProps {
  entityCount:     number
  costCenterCount: number
  facilityCount:   number
  userCount:       number
  doaLimitCount:   number
}

export function OrgSummaryBar({
  entityCount,
  costCenterCount,
  facilityCount,
  userCount,
  doaLimitCount,
}: OrgSummaryBarProps) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
      {/* 1. Legal Entities */}
      <div className="bg-white rounded-lg p-3.5 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
            {ORG_COPY.kpi.entities}
          </span>
          <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center">
            <Building2 className="w-3.5 h-3.5" />
          </div>
        </div>
        <div className="mt-1.5 text-lg font-bold text-slate-900">{entityCount} Entities</div>
        <p className="text-[10px] text-slate-400 mt-0.5">{ORG_COPY.kpi.entitiesSub}</p>
      </div>

      {/* 2. Cost Centers */}
      <div className="bg-white rounded-lg p-3.5 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
            {ORG_COPY.kpi.costCenters}
          </span>
          <div className="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-700 flex items-center justify-center">
            <Layers className="w-3.5 h-3.5" />
          </div>
        </div>
        <div className="mt-1.5 text-lg font-bold text-slate-900">{costCenterCount} Centers</div>
        <p className="text-[10px] text-slate-400 mt-0.5">{ORG_COPY.kpi.costCentersSub}</p>
      </div>

      {/* 3. Facilities */}
      <div className="bg-white rounded-lg p-3.5 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
            {ORG_COPY.kpi.facilities}
          </span>
          <div className="w-7 h-7 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center">
            <Truck className="w-3.5 h-3.5" />
          </div>
        </div>
        <div className="mt-1.5 text-lg font-bold text-slate-900">{facilityCount} Sites</div>
        <p className="text-[10px] text-slate-400 mt-0.5">{ORG_COPY.kpi.facilitiesSub}</p>
      </div>

      {/* 4. Active Users */}
      <div className="bg-white rounded-lg p-3.5 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
            {ORG_COPY.kpi.users}
          </span>
          <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
            <Users className="w-3.5 h-3.5" />
          </div>
        </div>
        <div className="mt-1.5 text-lg font-bold text-slate-900">{userCount} Users</div>
        <p className="text-[10px] text-slate-400 mt-0.5">{ORG_COPY.kpi.usersSub}</p>
      </div>

      {/* 5. DoA Limits */}
      <div className="bg-white rounded-lg p-3.5 border border-slate-200 shadow-2xs">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
            {ORG_COPY.kpi.doaTiers}
          </span>
          <div className="w-7 h-7 rounded-lg bg-slate-900 text-white flex items-center justify-center">
            <ShieldCheck className="w-3.5 h-3.5" />
          </div>
        </div>
        <div className="mt-1.5 text-lg font-bold text-slate-900">{doaLimitCount} Tiers</div>
        <p className="text-[10px] text-slate-400 mt-0.5">{ORG_COPY.kpi.doaTiersSub}</p>
      </div>
    </div>
  )
}
