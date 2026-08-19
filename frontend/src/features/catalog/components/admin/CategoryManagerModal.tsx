import { useState } from 'react'
import { X, FolderTree, Plus, Folder, CornerDownRight } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { useCategoryTree } from '../../hooks/useCategoryTree'
import { useCatalogAdmin } from '../../hooks/useCatalogAdmin'
import { CATALOG_COPY } from '../../constants/catalogCopy'
import type { CatalogCategoryDto } from '../../types/catalog.types'

interface CategoryManagerModalProps {
  isOpen: boolean
  onClose: () => void
}

export function CategoryManagerModal({ isOpen, onClose }: CategoryManagerModalProps) {
  const { data: categories = [], isLoading } = useCategoryTree()
  const { createCategory, isCreatingCategory } = useCatalogAdmin()

  const [parentId, setParentId] = useState<string>('')
  const [name, setName] = useState('')
  const [code, setCode] = useState('')
  const [description, setDescription] = useState('')

  if (!isOpen) return null

  // Flatten categories for parent dropdown
  const parentOptions: { value: string; label: string }[] = [
    { value: '', label: CATALOG_COPY.admin.categoryModal.parentPlaceholder },
  ]
  const flatten = (cats: CatalogCategoryDto[]) => {
    for (const c of cats) {
      parentOptions.push({ value: c.id, label: c.fullPath })
      if (c.children?.length) {
        flatten(c.children)
      }
    }
  }
  flatten(categories)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return

    await createCategory({
      parentId: parentId || undefined,
      code: code.trim() || undefined,
      name: name.trim(),
      description: description.trim() || undefined,
    })

    setName('')
    setCode('')
    setDescription('')
    setParentId('')
  }

  const renderCategoryNode = (cat: CatalogCategoryDto, depth = 0) => {
    return (
      <div key={cat.id} className="space-y-1">
        <div
          className={`flex items-center justify-between py-2 px-3 rounded-lg border transition-colors ${
            depth === 0
              ? 'bg-slate-50 border-slate-200 font-semibold text-slate-900'
              : 'bg-white border-slate-100 text-slate-700 ml-4'
          }`}
        >
          <div className="flex items-center gap-2">
            {depth > 0 ? (
              <CornerDownRight className="w-3.5 h-3.5 text-slate-400" />
            ) : (
              <Folder className="w-4 h-4 text-indigo-600" />
            )}
            <span className="text-xs">{cat.name}</span>
            <span className="font-mono text-[10px] text-slate-400 bg-slate-100 px-1.5 py-0.2 rounded">
              {cat.code}
            </span>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-[11px] font-mono text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
              {cat.itemCount} Items
            </span>
          </div>
        </div>

        {cat.children?.map((child) => renderCategoryNode(child, depth + 1))}
      </div>
    )
  }

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 sm:p-6 animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-3xl overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
              <FolderTree className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {CATALOG_COPY.admin.categoryModal.title}
              </h3>
              <p className="text-xs text-slate-500">
                {CATALOG_COPY.admin.categoryModal.subtitle}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200/60 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content: Grid with Tree and Form */}
        <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6 overflow-y-auto flex-1">
          {/* Left Column: Category Tree */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Configured Categories ({categories.length})
            </h4>
            <div className="bg-slate-50/50 border border-slate-200 rounded-xl p-3 max-h-80 overflow-y-auto space-y-2">
              {isLoading ? (
                <div className="text-xs text-slate-400 text-center py-6">Loading categories...</div>
              ) : categories.length === 0 ? (
                <div className="text-xs text-slate-400 text-center py-6">No categories defined yet.</div>
              ) : (
                categories.map((c) => renderCategoryNode(c))
              )}
            </div>
          </div>

          {/* Right Column: New Category Form */}
          <form onSubmit={handleSubmit} className="space-y-4 bg-slate-50/70 border border-slate-200 rounded-xl p-4">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
              <Plus className="w-4 h-4 text-indigo-600" />
              Add New Category
            </h4>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {CATALOG_COPY.admin.categoryModal.parentLabel}
              </label>
              <Select
                value={parentId}
                onChange={(e) => setParentId(e.target.value)}
                options={parentOptions}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {CATALOG_COPY.admin.categoryModal.nameLabel} <span className="text-red-500">*</span>
              </label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={CATALOG_COPY.admin.categoryModal.namePlaceholder}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {CATALOG_COPY.admin.categoryModal.codeLabel}
              </label>
              <Input
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder={CATALOG_COPY.admin.categoryModal.codePlaceholder}
                className="font-mono uppercase text-xs"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {CATALOG_COPY.admin.categoryModal.descLabel}
              </label>
              <Input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Optional category description..."
              />
            </div>

            <Button
              type="submit"
              variant="primary"
              className="w-full"
              isLoading={isCreatingCategory}
              leftIcon={<Plus className="w-4 h-4" />}
            >
              Create Category
            </Button>
          </form>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-200 bg-slate-50 flex items-center justify-end">
          <Button type="button" variant="outline" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </div>
  )
}
