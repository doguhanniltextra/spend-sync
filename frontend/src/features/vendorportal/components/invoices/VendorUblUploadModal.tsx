import { useState } from 'react'
import { UploadCloud, FileCode, CheckCircle2, AlertCircle } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { vendorPortalApi } from '../../services/vendorPortalApi'
import { useQueryClient } from '@tanstack/react-query'

interface Props {
  isOpen: boolean
  onClose: () => void
}

export function VendorUblUploadModal({ isOpen, onClose }: Props) {
  const qc = useQueryClient()
  const [xmlContent, setXmlContent] = useState('')
  const [fileName, setFileName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    setFileName(file.name)
    const reader = new FileReader()
    reader.onload = (event) => {
      setXmlContent(event.target?.result as string)
    }
    reader.readAsText(file)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!xmlContent) {
      setError('Please select or paste an official GİB UBL-TR XML invoice file.')
      return
    }

    try {
      setLoading(true)
      setError(null)
      await vendorPortalApi.uploadUblXml(xmlContent)
      qc.invalidateQueries({ queryKey: ['vendor', 'invoices'] })
      setSuccess(true)
      setTimeout(() => {
        setSuccess(false)
        onClose()
      }, 1500)
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to parse or submit UBL-TR XML invoice.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Upload GİB UBL-TR 1.2 XML e-Invoice">
      {success ? (
        <div className="p-8 text-center space-y-3">
          <div className="w-12 h-12 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-bold text-slate-900">UBL XML Successfully Processed!</h3>
          <p className="text-xs text-slate-500">
            e-Invoice parsed and submitted to the 3-Way matching engine.
          </p>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-6">
          <p className="text-xs text-slate-500">
            Upload your official signed e-Fatura / e-Arşiv XML file compliant with GİB UBL-TR 1.2 standard.
          </p>

          {error && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          {/* Drag & Drop File Upload Area */}
          <div className="border-2 border-dashed border-slate-300 hover:border-teal-500 rounded-2xl p-6 text-center transition bg-slate-50/50">
            <input
              type="file"
              accept=".xml"
              onChange={handleFileChange}
              id="xml-file-input"
              className="hidden"
            />
            <label htmlFor="xml-file-input" className="cursor-pointer block space-y-2">
              <div className="w-10 h-10 rounded-full bg-teal-100 text-teal-600 flex items-center justify-center mx-auto">
                <UploadCloud className="w-5 h-5" />
              </div>
              <p className="text-sm font-semibold text-slate-700">
                {fileName ? fileName : 'Click or drag GİB XML file here'}
              </p>
              <p className="text-[11px] text-slate-400">
                Accepts .xml files up to 10MB (UBL-TR 1.2 format)
              </p>
            </label>
          </div>

          {/* Direct XML Paste Area */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
              <FileCode className="w-4 h-4 text-slate-400" />
              Or Paste Raw UBL XML Content
            </label>
            <textarea
              rows={4}
              value={xmlContent}
              onChange={(e) => setXmlContent(e.target.value)}
              placeholder="<?xml version='1.0' encoding='UTF-8'?>&#10;<Invoice xmlns='urn:oasis:names:specification:ubl:schema:xsd:Invoice-2'..."
              className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 font-mono text-xs text-slate-800 focus:ring-2 focus:ring-teal-500 focus:outline-none"
            />
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-800 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || !xmlContent}
              className="px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-semibold text-sm shadow transition disabled:opacity-50"
            >
              {loading ? 'Validating XML...' : 'Submit XML e-Invoice'}
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}
