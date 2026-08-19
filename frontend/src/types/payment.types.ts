import type { UUID, ISODateString } from './common.types'

export type PaymentMethod =
  | 'BANK_TRANSFER_EFT'
  | 'ISO_20022_PAIN_001'
  | 'SWIFT_WIRE'

export type PaymentBatchStatus =
  | 'DRAFT'
  | 'APPROVED'
  | 'DISPATCHED'
  | 'CANCELLED'

export interface DueInvoiceResponse {
  id:              UUID
  invoiceNumber:   string
  ettn:            string
  invoiceDate:     string // YYYY-MM-DD
  vendorId:        UUID
  vendorName:      string
  vendorIban:      string | null
  legalEntityId:   UUID
  legalEntityName: string
  currency:        string
  totalAmount:     number
  matchStatus:     string
  status:          string
}

export interface PaymentBatchItemResponse {
  id:                UUID
  supplierInvoiceId: UUID
  invoiceNumber:     string
  vendorId:          UUID
  vendorName:        string
  vendorIban:        string | null
  amount:            number
  discountAmount:    number
  netPayableAmount:  number
  status:            string
  createdAt:         ISODateString
}

export interface PaymentBatchResponse {
  id:                UUID
  batchNumber:       string
  legalEntityId:     UUID
  legalEntityName:   string
  paymentMethod:     PaymentMethod
  totalAmount:       number
  currency:          string
  itemCount:         number
  status:            PaymentBatchStatus
  createdByUserId:   UUID
  createdByUserName: string
  approvedByUserId:  UUID | null
  approvedByUserName:string | null
  approvedAt:        ISODateString | null
  xmlPayload:        string | null
  idempotencyKey:    string
  items:             PaymentBatchItemResponse[]
  createdAt:         ISODateString
}

export interface CreatePaymentBatchRequest {
  legalEntityId:   UUID
  paymentMethod?:  PaymentMethod
  idempotencyKey?: string
  invoiceIds:      UUID[]
}

export interface ApprovePaymentBatchRequest {
  approvalNote?: string
}
