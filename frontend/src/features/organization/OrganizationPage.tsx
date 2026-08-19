import { useState } from 'react'
import { Building2, Layers, Truck, Users, ShieldCheck } from 'lucide-react'
import { OrgSummaryBar } from './components/OrgSummaryBar'
import { LegalEntitiesTab } from './components/LegalEntitiesTab'
import { CostCentersTab } from './components/CostCentersTab'
import { FacilitiesTab } from './components/FacilitiesTab'
import { UserDirectoryTab } from './components/UserDirectoryTab'
import { DoAMatrixTab } from './components/DoAMatrixTab'
import { useLegalEntities } from './hooks/useLegalEntities'
import { useCostCenters } from './hooks/useCostCenters'
import { useFacilities } from './hooks/useFacilities'
import { useUserManagement } from './hooks/useUserManagement'
import { useDoAMatrix } from './hooks/useDoAMatrix'
import { ORG_COPY } from './constants/organizationCopy'

export default function OrganizationPage() {
  const [activeTab, setActiveTab] = useState<'ENTITIES' | 'COST_CENTERS' | 'FACILITIES' | 'USERS' | 'DOA'>('ENTITIES')

  const { legalEntities } = useLegalEntities()
  const { costCenters } = useCostCenters()
  const { facilities } = useFacilities()
  const { users } = useUserManagement()
  const { approvalLimits } = useDoAMatrix()

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-20">
      {/* Header */}
      <div className="border-b border-slate-200 pb-5">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
          {ORG_COPY.header.title}
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          {ORG_COPY.header.subtitle}
        </p>
      </div>

      {/* KPI Metrics Summary Bar */}
      <OrgSummaryBar
        entityCount={legalEntities.length}
        costCenterCount={costCenters.length}
        facilityCount={facilities.length}
        userCount={users.length}
        doaLimitCount={approvalLimits.length}
      />

      {/* 5-Tab Navigation Bar */}
      <div className="flex border-b border-slate-200 gap-6 overflow-x-auto">
        <button
          type="button"
          onClick={() => setActiveTab('ENTITIES')}
          className={`pb-3 font-bold text-xs flex items-center gap-1.5 transition-colors border-b-2 whitespace-nowrap ${
            activeTab === 'ENTITIES'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          <Building2 className="w-3.5 h-3.5" />
          {ORG_COPY.tabs.legalEntities} ({legalEntities.length})
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('COST_CENTERS')}
          className={`pb-3 font-bold text-xs flex items-center gap-1.5 transition-colors border-b-2 whitespace-nowrap ${
            activeTab === 'COST_CENTERS'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          <Layers className="w-3.5 h-3.5" />
          {ORG_COPY.tabs.costCenters} ({costCenters.length})
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('FACILITIES')}
          className={`pb-3 font-bold text-xs flex items-center gap-1.5 transition-colors border-b-2 whitespace-nowrap ${
            activeTab === 'FACILITIES'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          <Truck className="w-3.5 h-3.5" />
          {ORG_COPY.tabs.facilities} ({facilities.length})
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('USERS')}
          className={`pb-3 font-bold text-xs flex items-center gap-1.5 transition-colors border-b-2 whitespace-nowrap ${
            activeTab === 'USERS'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          <Users className="w-3.5 h-3.5" />
          {ORG_COPY.tabs.users} ({users.length})
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('DOA')}
          className={`pb-3 font-bold text-xs flex items-center gap-1.5 transition-colors border-b-2 whitespace-nowrap ${
            activeTab === 'DOA'
              ? 'border-slate-900 text-slate-900'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          }`}
        >
          <ShieldCheck className="w-3.5 h-3.5" />
          {ORG_COPY.tabs.doaMatrix} ({approvalLimits.length})
        </button>
      </div>

      {/* Tab Panels */}
      {activeTab === 'ENTITIES' && <LegalEntitiesTab />}
      {activeTab === 'COST_CENTERS' && <CostCentersTab />}
      {activeTab === 'FACILITIES' && <FacilitiesTab />}
      {activeTab === 'USERS' && <UserDirectoryTab />}
      {activeTab === 'DOA' && <DoAMatrixTab />}
    </div>
  )
}
