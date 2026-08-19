import { useState } from 'react'
import { Plus, FolderTree, UploadCloud, Download } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { CatalogHealthCards } from '../components/admin/CatalogHealthCards'
import { CatalogTable } from '../components/admin/CatalogTable'
import { CatalogItemDrawer } from '../components/admin/CatalogItemDrawer'
import { CatalogCsvImportModal } from '../components/admin/CatalogCsvImportModal'
import { CategoryManagerModal } from '../components/admin/CategoryManagerModal'
import { useCatalogAdmin } from '../hooks/useCatalogAdmin'
import { CATALOG_COPY } from '../constants/catalogCopy'
import type { CatalogItemResponse } from '../types/catalog.types'

export function CatalogManagementPage() {
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)
  const [itemToEdit, setItemToEdit] = useState<CatalogItemResponse | null>(null)
  const [isImportModalOpen, setIsImportModalOpen] = useState(false)
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false)

  const { exportCsv } = useCatalogAdmin()

  const handleOpenCreateDrawer = () => {
    setItemToEdit(null)
    setIsDrawerOpen(true)
  }

  const handleOpenEditDrawer = (item: CatalogItemResponse) => {
    setItemToEdit(item)
    setIsDrawerOpen(true)
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-slate-900">
              {CATALOG_COPY.admin.pageTitle}
            </h1>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            {CATALOG_COPY.admin.pageSubtitle}
          </p>
        </div>

        {/* Global Action Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setIsCategoryModalOpen(true)}
            leftIcon={<FolderTree className="w-4 h-4 text-blue-600" />}
          >
            {CATALOG_COPY.admin.btnManageCategories}
          </Button>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setIsImportModalOpen(true)}
            leftIcon={<UploadCloud className="w-4 h-4 text-indigo-600" />}
          >
            {CATALOG_COPY.admin.btnImport}
          </Button>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={exportCsv}
            leftIcon={<Download className="w-4 h-4 text-slate-600" />}
          >
            {CATALOG_COPY.admin.btnExport}
          </Button>

          <Button
            type="button"
            variant="primary"
            size="sm"
            onClick={handleOpenCreateDrawer}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            {CATALOG_COPY.admin.btnNewItem}
          </Button>
        </div>
      </div>

      {/* KPI Health Cards */}
      <CatalogHealthCards />

      {/* Main Catalog Table */}
      <CatalogTable onEditItem={handleOpenEditDrawer} />

      {/* Drawers & Modals */}
      <CatalogItemDrawer
        isOpen={isDrawerOpen}
        onClose={() => {
          setIsDrawerOpen(false)
          setItemToEdit(null)
        }}
        itemToEdit={itemToEdit}
      />

      <CatalogCsvImportModal
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
      />

      <CategoryManagerModal
        isOpen={isCategoryModalOpen}
        onClose={() => setIsCategoryModalOpen(false)}
      />
    </div>
  )
}
export default CatalogManagementPage
