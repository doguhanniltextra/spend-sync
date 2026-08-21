import { Printer } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { useVendorInvoiceHtml } from '../../hooks/useVendorPortalQueries'

interface Props {
  invoiceId: string
  invoiceNumber: string
  isOpen: boolean
  onClose: () => void
}

export function VendorInvoiceHtmlModal({ invoiceId, invoiceNumber, isOpen, onClose }: Props) {
  const { data: htmlContent, isLoading, error } = useVendorInvoiceHtml(invoiceId, isOpen)

  const handlePrint = () => {
    const iframe = document.getElementById('invoice-html-frame') as HTMLIFrameElement
    if (iframe?.contentWindow) {
      iframe.contentWindow.focus()
      iframe.contentWindow.print()
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Official GİB e-Fatura Render: ${invoiceNumber}`}>
      <div className="space-y-4">
        {/* Toolbar */}
        <div className="flex items-center justify-between p-3 rounded-xl bg-slate-100 border border-slate-200">
          <span className="text-xs font-semibold text-slate-700">
            Standard GİB UBL-TR 1.2 XSLT Template
          </span>
          <button
            onClick={handlePrint}
            className="px-3.5 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-xs shadow flex items-center gap-1.5 transition"
          >
            <Printer className="w-3.5 h-3.5" />
            <span>Print / Save as PDF</span>
          </button>
        </div>

        {/* HTML Render Frame */}
        <div className="w-full h-[550px] border border-slate-300 rounded-xl overflow-hidden bg-white shadow-inner">
          {isLoading ? (
            <div className="flex items-center justify-center h-full text-slate-400 text-sm">
              <div className="w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full animate-spin mr-2" />
              Rendering GİB e-Invoice HTML...
            </div>
          ) : error ? (
            <div className="flex items-center justify-center h-full text-rose-500 text-xs">
              Failed to load invoice HTML template.
            </div>
          ) : (
            <iframe
              id="invoice-html-frame"
              srcDoc={htmlContent}
              title={`Invoice-${invoiceNumber}`}
              className="w-full h-full border-0"
            />
          )}
        </div>
      </div>
    </Modal>
  )
}
