import type { UUID, ISODateString } from './common.types'
import type { WorkflowStatus } from '@/constants/workflow'

export interface SupplierInvoiceResponse {
  id:              UUID
  invoiceNumber:   string
  ettn:            string
  vendorId:        UUID
  vendorName:      string
  purchaseOrderId: UUID
  poNumber:        string
  invoiceDate:     ISODateString
  dueDate:         ISODateString
  subtotal:        number
  taxAmount:       number
  totalAmount:     number
  currency:        string
  status:          WorkflowStatus
  discrepancyReason?: string
  createdAt:       ISODateString
}
