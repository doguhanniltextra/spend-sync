import { clsx } from 'clsx'
import type { BudgetPoolResponse } from '@/types/budget.types'
import { formatCurrency } from '@/utils/currency'
import { THRESHOLDS } from '@/constants/thresholds'
import { DASHBOARD_COPY } from '../constants/dashboardCopy'

interface BudgetUtilizationListProps {
  pools: BudgetPoolResponse[]
}

export function BudgetUtilizationList({ pools }: BudgetUtilizationListProps) {
  return (
    <div className="bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
      <div className="px-5 py-4 border-b border-slate-200 bg-white">
        <h3 className="text-sm font-bold text-slate-900">
          {DASHBOARD_COPY.budgetTable.title}
        </h3>
        <p className="text-xs text-slate-500 mt-0.5">
          {DASHBOARD_COPY.budgetTable.subtitle}
        </p>
      </div>

      {pools.length === 0 ? (
        <div className="p-8 text-center text-xs text-slate-500">
          {DASHBOARD_COPY.budgetTable.emptyText}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-5 py-3">{DASHBOARD_COPY.budgetTable.colCostCenter}</th>
                <th className="px-4 py-3 text-right">{DASHBOARD_COPY.budgetTable.colAllocated}</th>
                <th className="px-4 py-3 text-right">{DASHBOARD_COPY.budgetTable.colReserved}</th>
                <th className="px-4 py-3 text-right">{DASHBOARD_COPY.budgetTable.colSpent}</th>
                <th className="px-4 py-3 text-right">{DASHBOARD_COPY.budgetTable.colAvailable}</th>
                <th className="px-5 py-3 text-center">{DASHBOARD_COPY.budgetTable.colUtilization}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white font-mono">
              {pools.map((pool) => {
                const committed = (pool.spentAmount || 0) + (pool.reservedAmount || 0)
                const percent = pool.allocatedAmount > 0
                  ? Math.min(Math.round((committed / pool.allocatedAmount) * 100), 100)
                  : 0

                const isCritical = percent >= THRESHOLDS.budget.criticalPercent
                const isWarning  = percent >= THRESHOLDS.budget.warningPercent && !isCritical

                return (
                  <tr key={pool.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-5 py-3.5 font-sans font-medium text-slate-900">
                      <div>{pool.costCenterName}</div>
                      <span className="text-[10px] font-mono text-slate-400">
                        {pool.costCenterCode} • {pool.legalEntityName}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right text-slate-700">
                      {formatCurrency(pool.allocatedAmount, pool.currency as any)}
                    </td>
                    <td className="px-4 py-3.5 text-right text-amber-700">
                      {formatCurrency(pool.reservedAmount, pool.currency as any)}
                    </td>
                    <td className="px-4 py-3.5 text-right text-slate-900 font-semibold">
                      {formatCurrency(pool.spentAmount, pool.currency as any)}
                    </td>
                    <td className="px-4 py-3.5 text-right text-emerald-700 font-semibold">
                      {formatCurrency(pool.availableAmount, pool.currency as any)}
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2 justify-center">
                        <div className="w-24 bg-slate-100 rounded-full h-2 overflow-hidden">
                          <div
                            style={{ width: `${percent}%` }}
                            className={clsx(
                              'h-full rounded-full transition-all duration-500',
                              isCritical
                                ? 'bg-red-600'
                                : isWarning
                                ? 'bg-amber-500'
                                : 'bg-slate-800'
                            )}
                          />
                        </div>
                        <span
                          className={clsx(
                            'text-[11px] font-semibold min-w-[2.5rem] text-right',
                            isCritical ? 'text-red-700' : isWarning ? 'text-amber-700' : 'text-slate-700'
                          )}
                        >
                          %{percent}
                        </span>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
