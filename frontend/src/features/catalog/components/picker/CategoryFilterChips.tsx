import { clsx } from 'clsx'
import type { CatalogCategoryDto } from '../../types/catalog.types'
import { CATALOG_COPY } from '../../constants/catalogCopy'

interface CategoryFilterChipsProps {
  categories: CatalogCategoryDto[]
  selectedCategoryId?: string
  onSelectCategory: (id?: string) => void
}

export function CategoryFilterChips({
  categories,
  selectedCategoryId,
  onSelectCategory,
}: CategoryFilterChipsProps) {
  // Flatten tree for convenient pill selection
  const flatCategories: { id: string; name: string; fullPath: string; count: number }[] = []

  const flatten = (cats: CatalogCategoryDto[]) => {
    for (const c of cats) {
      flatCategories.push({
        id: c.id,
        name: c.name,
        fullPath: c.fullPath,
        count: c.itemCount,
      })
      if (c.children?.length) {
        flatten(c.children)
      }
    }
  }

  flatten(categories)

  return (
    <div className="flex items-center gap-1.5 overflow-x-auto pb-1.5 scrollbar-thin">
      <button
        type="button"
        onClick={() => onSelectCategory(undefined)}
        className={clsx(
          'px-3 py-1 text-xs font-medium rounded-full transition-all whitespace-nowrap',
          !selectedCategoryId
            ? 'bg-slate-900 text-white shadow-xs'
            : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
        )}
      >
        {CATALOG_COPY.picker.allCategories}
      </button>

      {flatCategories.map((cat) => {
        const isSelected = selectedCategoryId === cat.id
        return (
          <button
            key={cat.id}
            type="button"
            onClick={() => onSelectCategory(isSelected ? undefined : cat.id)}
            className={clsx(
              'px-3 py-1 text-xs font-medium rounded-full transition-all whitespace-nowrap flex items-center gap-1.5',
              isSelected
                ? 'bg-slate-900 text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            )}
            title={cat.fullPath}
          >
            <span>{cat.name}</span>
            {cat.count > 0 && (
              <span
                className={clsx(
                  'px-1.5 py-0.2 rounded-full text-[10px] font-mono',
                  isSelected ? 'bg-slate-700 text-white' : 'bg-slate-200 text-slate-700'
                )}
              >
                {cat.count}
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}
