import { Mail, Phone, MapPin, CreditCard } from 'lucide-react'
import { Drawer } from '@/components/ui/Drawer'
import { Button } from '@/components/ui/Button'
import { VendorTierBadge, VendorStatusBadge, EInvoiceBadge } from './VendorStatusBadge'
import { useCreateVendor } from '../hooks/useVendors'
import { usePurchaseOrders } from '../hooks/usePurchaseOrders'
import { StatusBadge } from '@/components/ui/Badge'
import { CurrencyDisplay } from '@/components/ui/MoneyInput'
import type { VendorResponse, VendorStatus } from '@/types/purchasing.types'
import { formatDate } from '@/utils/date'

interface VendorDetailDrawerProps {
  vendor:  VendorResponse | null
  isOpen:  boolean
  onClose: () => void
}

export function VendorDetailDrawer({ vendor, isOpen, onClose }: VendorDetailDrawerProps) {
  const { updateStatus, isUpdatingStatus } = useCreateVendor()
  const { orders } = usePurchaseOrders('ALL', vendor?.id)

  if (!vendor) return null

  const handleStatusChange = async (newStatus: VendorStatus) => {
    await updateStatus({ id: vendor.id, payload: { status: newStatus } })
  }

  const totalVendorSpend = orders.reduce((acc, o) => acc + (o.totalAmount || 0), 0)

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={vendor.name}
      subtitle={`Tax ID / VKN: ${vendor.taxNumber} • ${vendor.taxOffice}`}
      size="lg"
      footer={
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-500 font-medium">Status:</span>
            <select
              value={vendor.status}
              disabled={isUpdatingStatus}
              onChange={(e) => handleStatusChange(e.target.value as VendorStatus)}
              className="text-xs font-semibold bg-white border border-slate-300 rounded px-2.5 py-1 text-slate-900 focus:ring-2 focus:ring-slate-900 cursor-pointer"
            >
              <option value="ACTIVE">ACTIVE (Approved for POs)</option>
              <option value="ON_HOLD">ON_HOLD (Suspended)</option>
              <option value="BLOCKED">BLOCKED (Blacklisted)</option>
            </select>
          </div>

          <Button variant="outline" size="sm" onClick={onClose}>
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-6 text-xs text-slate-700">
        {/* Tier & E-Invoice Overview Banner */}
        <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <VendorTierBadge tier={vendor.tier} />
            <EInvoiceBadge isRegistered={vendor.isEInvoiceRegistered} />
          </div>
          <VendorStatusBadge status={vendor.status} />
        </div>

        {/* Commercial & Contact Profile */}
        <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
          <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
            Commercial & Contact Profile
          </h4>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <span className="text-slate-400 block text-[10px]">Spend Category:</span>
              <span className="font-medium text-slate-900">{vendor.category.replace(/_/g, ' ')}</span>
            </div>
            <div>
              <span className="text-slate-400 block text-[10px]">Default Payment Terms:</span>
              <span className="font-mono font-bold text-slate-900">{vendor.paymentTerms}</span>
            </div>
            <div>
              <span className="text-slate-400 block text-[10px]">Order Dispatch Email:</span>
              <a href={`mailto:${vendor.orderEmail}`} className="text-blue-600 font-medium hover:underline flex items-center gap-1 mt-0.5">
                <Mail className="w-3 h-3" />
                {vendor.orderEmail}
              </a>
            </div>
            <div>
              <span className="text-slate-400 block text-[10px]">Direct Phone:</span>
              <span className="text-slate-800 flex items-center gap-1 mt-0.5">
                <Phone className="w-3 h-3 text-slate-400" />
                {vendor.phoneNumber || '—'}
              </span>
            </div>
            <div className="col-span-2 pt-2 border-t border-slate-100">
              <span className="text-slate-400 block text-[10px]">Registered HQ Address:</span>
              <span className="text-slate-800 flex items-start gap-1 mt-0.5">
                <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                {vendor.address}, {vendor.city} / {vendor.country}
              </span>
            </div>
          </div>
        </div>

        {/* Banking & Settlement Box */}
        <div className="bg-white p-4 rounded-lg border border-slate-200 space-y-3">
          <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
            <CreditCard className="w-3.5 h-3.5 text-slate-500" />
            Banking & Wire Transfer Information
          </h4>
          <div className="p-3 bg-slate-50 rounded border border-slate-200/80 space-y-1 font-mono">
            <div className="flex justify-between">
              <span className="text-slate-500 font-sans">Bank:</span>
              <strong className="text-slate-900">{vendor.bankName || 'Garanti BBVA'}</strong>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500 font-sans">IBAN:</span>
              <strong className="text-slate-900 font-mono tracking-wider">{vendor.iban || 'TR00 0000 0000 0000 0000 0000 00'}</strong>
            </div>
          </div>
        </div>

        {/* Historical Orders with this Supplier */}
        <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
          <div className="px-4 py-3 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
            <h4 className="font-bold text-slate-900 uppercase tracking-wider text-[11px]">
              Purchase Orders ({orders.length})
            </h4>
            <div className="text-right">
              <span className="text-[10px] text-slate-500 block">Total Spend:</span>
              <CurrencyDisplay amount={totalVendorSpend} className="font-bold text-slate-900" />
            </div>
          </div>

          {orders.length === 0 ? (
            <div className="p-4 text-center text-slate-400 text-xs italic">
              No purchase orders issued to this vendor yet.
            </div>
          ) : (
            <div className="divide-y divide-slate-100 font-sans">
              {orders.map((po) => (
                <div key={po.id} className="p-3 flex items-center justify-between hover:bg-slate-50/50">
                  <div>
                    <span className="font-mono font-bold text-slate-900 block">{po.poNumber}</span>
                    <span className="text-[10px] text-slate-500">{formatDate(po.issuedAt ?? po.createdAt)}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <CurrencyDisplay amount={po.totalAmount} currency={po.currency as any} className="font-mono font-semibold text-slate-900" />
                    <StatusBadge status={po.status} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </Drawer>
  )
}
