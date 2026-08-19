import { Package, FolderTree, AlertTriangle, XCircle } from 'lucide-react'
import { useCatalogHealth } from '../../hooks/useCategoryTree'
import { CATALOG_COPY } from '../../constants/catalogCopy'

export function CatalogHealthCards() {
  const { data: health, isLoading } = useCatalogHealth()

  const cards = [
    {
      title: CATALOG_COPY.admin.health.totalItems,
      value: health?.totalActiveItems ?? 0,
      sub: CATALOG_COPY.admin.health.totalItemsSub,
      icon: Package,
      iconColor: 'text-indigo-600',
      bgColor: 'bg-indigo-50/70 border-indigo-100',
    },
    {
      title: CATALOG_COPY.admin.health.categories,
      value: health?.totalCategories ?? 0,
      sub: CATALOG_COPY.admin.health.categoriesSub,
      icon: FolderTree,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50/70 border-blue-100',
    },
    {
      title: CATALOG_COPY.admin.health.expiring30,
      value: health?.expiringIn30DaysCount ?? 0,
      sub: CATALOG_COPY.admin.health.expiring30Sub,
      icon: AlertTriangle,
      iconColor: 'text-amber-600',
      bgColor: 'bg-amber-50/70 border-amber-200',
      alert: (health?.expiringIn30DaysCount ?? 0) > 0,
    },
    {
      title: CATALOG_COPY.admin.health.expired,
      value: health?.expiredItemsCount ?? 0,
      sub: CATALOG_COPY.admin.health.expiredSub,
      icon: XCircle,
      iconColor: 'text-red-600',
      bgColor: 'bg-red-50/70 border-red-200',
    },
  ]

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card, index) => {
        const Icon = card.icon
        return (
          <div
            key={index}
            className="bg-white border border-slate-200 rounded-xl p-4 shadow-2xs hover:shadow-sm transition-shadow flex items-start justify-between"
          >
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                {card.title}
              </p>
              <h3 className="text-2xl font-bold text-slate-900 font-mono">
                {isLoading ? '...' : card.value}
              </h3>
              <p className="text-[11px] text-slate-500 mt-1">
                {card.sub}
              </p>
            </div>
            <div className={`p-2.5 rounded-xl border ${card.bgColor}`}>
              <Icon className={`w-5 h-5 ${card.iconColor}`} />
            </div>
          </div>
        )
      })}
    </div>
  )
}
