import type { UUID, ISODateString } from './common.types'
import type { Incoterms } from './purchasing.types'
import type { WorkflowStatus } from '@/constants/workflow'

export type GoodsReceiptStatus = 'COMPLETED' | 'CANCELLED'

export interface CreateGRLineItemRequest {
  purchaseOrderLineItemId: UUID
  receivedQuantity:        number
  acceptedQuantity:        number
  rejectedQuantity?:       number
  rejectionReason?:        string
  notes?:                  string
}

export interface CreateGoodsReceiptRequest {
  purchaseOrderId:     UUID
  waybillNumber:       string
  waybillDate:         string // YYYY-MM-DD
  deliveryFacilityId?: UUID
  notes?:              string
  lineItems:           CreateGRLineItemRequest[]
}

export interface GRLineItemResponse {
  id:                      UUID
  purchaseOrderLineItemId: UUID
  itemDescription:         string
  receivedQuantity:        number
  acceptedQuantity:        number
  rejectedQuantity:        number
  rejectionReason:         string | null
  notes:                   string | null
}

export interface GoodsReceiptResponse {
  id:                   UUID
  receiptNumber:        string
  purchaseOrderId:      UUID
  poNumber:             string
  vendorId:             UUID
  vendorName:           string
  deliveryFacilityId:   UUID
  deliveryFacilityName: string
  waybillNumber:        string
  waybillDate:          string
  receivedByUserId:     UUID
  receivedByUserName:   string
  status:               GoodsReceiptStatus
  notes:                string | null
  lineItems:            GRLineItemResponse[]
  createdAt:            ISODateString
}

export interface PendingPOForReceivingResponse {
  id:                   UUID
  poNumber:             string
  vendorId:             UUID
  vendorName:           string
  deliveryFacilityId:   UUID
  deliveryFacilityName: string
  status:               WorkflowStatus
  incoterms:            Incoterms
  totalAmount:          number
  currency:             string
  lineItemCount:        number
  issuedAt:             ISODateString | null
}
