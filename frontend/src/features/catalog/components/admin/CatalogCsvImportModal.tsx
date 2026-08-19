import { useState, useRef } from 'react'
import { X, UploadCloud, FileText, CheckCircle2, AlertCircle, Download } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { useCatalogCsvImport } from '../../hooks/useCatalogCsvImport'
import { CATALOG_COPY } from '../../constants/catalogCopy'

interface CatalogCsvImportModalProps {
  isOpen: boolean
  onClose: () => void
}

export function CatalogCsvImportModal({ isOpen, onClose }: CatalogCsvImportModalProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [isDragOver, setIsDragOver] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const { importCsv, isImporting, result, reset } = useCatalogCsvImport()

  if (!isOpen) return null

  const handleClose = () => {
    reset()
    setSelectedFile(null)
    onClose()
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0])
    }
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragOver(false)
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0])
    }
  }

  const handleUpload = async () => {
    if (!selectedFile) return
    await importCsv(selectedFile)
  }

  const downloadSampleTemplate = () => {
    const sampleCsv = `item_code,name,description,category_path,vendor_name,unit_price,vat_rate,uom,contract_ref,valid_from,valid_until,is_preferred
IT-LAP-SAMPLE,"Dell Latitude 5540","Intel i7, 32GB RAM, 1TB SSD","IT / Hardware / Laptop","ABC Tech Solutions Inc.",1250,0.20,PIECE,SZL-2026-001,2026-01-01,2026-12-31,true
OF-PPR-SAMPLE,"A4 80gsm Copy Paper 500 Sheets","White standard 80gsm copy paper","Office / Stationery","",8.5,0.20,BOX,SZL-2026-002,2026-01-01,2026-06-30,false`

    const blob = new Blob([sampleCsv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'catalog_import_template.csv'
    document.body.appendChild(a)
    a.click()
    URL.revokeObjectURL(url)
    document.body.removeChild(a)
  }

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 sm:p-6 animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              {CATALOG_COPY.admin.importModal.title}
            </h3>
            <p className="text-xs text-slate-500">
              {CATALOG_COPY.admin.importModal.subtitle}
            </p>
          </div>

          <button
            type="button"
            onClick={handleClose}
            className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200/60 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-5 overflow-y-auto flex-1">
          {/* Download Template Bar */}
          <div className="bg-indigo-50/70 border border-indigo-100 rounded-xl p-3.5 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 text-indigo-900">
              <FileText className="w-4 h-4 text-indigo-600" />
              <span>Use the pre-formatted CSV template to ensure correct column matching.</span>
            </div>
            <button
              type="button"
              onClick={downloadSampleTemplate}
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 shrink-0 ml-2"
            >
              <Download className="w-3.5 h-3.5" />
              {CATALOG_COPY.admin.importModal.downloadTemplate}
            </button>
          </div>

          {/* Upload Area */}
          {!result && (
            <div
              onDragOver={(e) => {
                e.preventDefault()
                setIsDragOver(true)
              }}
              onDragLeave={() => setIsDragOver(false)}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all flex flex-col items-center justify-center gap-2.5 ${
                isDragOver
                  ? 'border-indigo-500 bg-indigo-50/50'
                  : selectedFile
                  ? 'border-emerald-400 bg-emerald-50/30'
                  : 'border-slate-300 hover:border-indigo-400 bg-slate-50/50'
              }`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".csv"
                onChange={handleFileChange}
                className="hidden"
              />

              <div className="p-3 rounded-full bg-white shadow-xs border border-slate-200 text-indigo-600">
                <UploadCloud className="w-6 h-6" />
              </div>

              {selectedFile ? (
                <div>
                  <p className="text-sm font-semibold text-emerald-800">
                    Selected File: {selectedFile.name}
                  </p>
                  <p className="text-xs text-slate-500 font-mono mt-0.5">
                    ({(selectedFile.size / 1024).toFixed(1)} KB) — Click to choose different file
                  </p>
                </div>
              ) : (
                <div>
                  <p className="text-sm font-semibold text-slate-800">
                    {CATALOG_COPY.admin.importModal.dragDropTitle}
                  </p>
                  <p className="text-xs text-slate-500 mt-1">
                    {CATALOG_COPY.admin.importModal.dragDropSub}
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Result View */}
          {result && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 flex items-center gap-3">
                  <CheckCircle2 className="w-6 h-6 text-emerald-600 shrink-0" />
                  <div>
                    <div className="text-xs text-emerald-700 font-medium">
                      {CATALOG_COPY.admin.importModal.successSummary}
                    </div>
                    <div className="text-xl font-bold text-emerald-900 font-mono">
                      {result.successCount}
                    </div>
                  </div>
                </div>

                <div className={`border rounded-xl p-4 flex items-center gap-3 ${
                  result.failureCount > 0
                    ? 'bg-red-50 border-red-200'
                    : 'bg-slate-50 border-slate-200'
                }`}>
                  <AlertCircle className={`w-6 h-6 shrink-0 ${
                    result.failureCount > 0 ? 'text-red-600' : 'text-slate-400'
                  }`} />
                  <div>
                    <div className="text-xs text-slate-600 font-medium">
                      {CATALOG_COPY.admin.importModal.errorSummary}
                    </div>
                    <div className="text-xl font-bold text-slate-900 font-mono">
                      {result.failureCount}
                    </div>
                  </div>
                </div>
              </div>

              {/* Error Table if any */}
              {result.errors.length > 0 && (
                <div className="border border-red-200 rounded-xl overflow-hidden">
                  <div className="bg-red-50/80 px-4 py-2 text-xs font-semibold text-red-800 border-b border-red-200">
                    Error Breakdown ({result.errors.length} Rows)
                  </div>
                  <div className="max-h-48 overflow-y-auto">
                    <table className="w-full text-left text-xs">
                      <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold">
                        <tr>
                          <th className="px-3 py-2 w-16">{CATALOG_COPY.admin.importModal.colRow}</th>
                          <th className="px-3 py-2 w-32">{CATALOG_COPY.admin.importModal.colItem}</th>
                          <th className="px-3 py-2">{CATALOG_COPY.admin.importModal.colError}</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 bg-white">
                        {result.errors.map((err, idx) => (
                          <tr key={idx} className="hover:bg-red-50/30">
                            <td className="px-3 py-2 font-mono text-slate-600">{err.rowNumber}</td>
                            <td className="px-3 py-2 font-mono font-medium text-slate-800">{err.itemCodeOrName}</td>
                            <td className="px-3 py-2 text-red-600">{err.errorMessage}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-200 bg-slate-50 flex items-center justify-end gap-3">
          <Button type="button" variant="outline" onClick={handleClose}>
            {result ? 'Close' : 'Cancel'}
          </Button>

          {!result && (
            <Button
              type="button"
              variant="primary"
              disabled={!selectedFile || isImporting}
              isLoading={isImporting}
              onClick={handleUpload}
              leftIcon={<UploadCloud className="w-4 h-4" />}
            >
              {CATALOG_COPY.admin.importModal.uploadCTA}
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
