import type { UUID, ISODateString } from './common.types'
import type { WorkflowStatus } from '@/constants/workflow'

export type Incoterms =
  | 'DAP'
  | 'DDP'
  | 'EXW'
  | 'FOB'
  | 'CIF'
  | 'CPT'

export type PaymentTerms =
  | 'IMMEDIATE'
  | 'NET_15'
  | 'NET_30'
  | 'NET_45'
  | 'NET_60'
  | 'NET_90'
  | 'CASH_IN_ADVANCE'

export type VendorCategory =
  | 'IT_HARDWARE'
  | 'SOFTWARE_SAAS'
  | 'OFFICE_SUPPLIES'
  | 'FACILITY_SERVICES'
  | 'LOGISTICS_TRANSPORT'
  | 'CONSULTING_PROFESSIONAL'

export type VendorTier = 'TIER_1_STRATEGIC' | 'TIER_2_PREFERRED' | 'TIER_3_STANDARD'

export type VendorStatus = 'ACTIVE' | 'BLOCKED' | 'INACTIVE'

export interface VendorResponse {
  id:                  UUID
  name:                string
  taxNumber:           string
  taxOffice:           string
  category:            VendorCategory
  tier:                VendorTier
  isEInvoiceRegistered:boolean
  orderEmail:          string
  phoneNumber:         string
  address:             string
  city:                string
  country:             string
  paymentTerms:        PaymentTerms
  bankName:            string
  iban:                string
  status:              VendorStatus
  createdAt:           ISODateString
  updatedAt:           ISODateString
}

export interface CreateVendorRequest {
  name:                string
  taxNumber:           string
  taxOffice:           string
  category:            VendorCategory
  tier:                VendorTier
  isEInvoiceRegistered:boolean
  orderEmail:          string
  phoneNumber?:        string
  address:             string
  city?:               string
  country:             string
  paymentTerms:        PaymentTerms
  bankName?:           string
  iban?:               string
}

export interface POLineItemRequest {
  itemDescription:       string
  itemCategory:          string
  quantity:              number
  unitOfMeasure:         string
  unitPrice:             number
  estimatedDeliveryDate?: string
}

export interface POLineItemResponse {
  id:                    UUID
  lineNumber:            number
  itemDescription:       string
  itemCategory:          string
  quantity:              number
  unitOfMeasure:         string
  unitPrice:             number
  lineTotal:             number
  receivedQuantity?:     number
  overDeliveryTolerancePct?: number
  underDeliveryTolerancePct?: number
  estimatedDeliveryDate: string | null
}

export interface PORevisionResponse {
  id:             UUID
  revisionNumber: number
  previousAmount: number
  newAmount:      number
  currency:       string
  revisionReason: string
  revisedByUserName: string
  createdAt:      ISODateString
}

export interface CreatePurchaseOrderRequest {
  requisitionId?:     UUID
  legalEntityId:      UUID
  costCenterId:       UUID
  deliveryFacilityId: UUID
  vendorId:           UUID
  incoterms:          Incoterms
  paymentTerms:       PaymentTerms
  currency?:          string
  notes?:             string
  lineItems:          POLineItemRequest[]
}

export interface PurchaseOrderSummaryResponse {
  id:                   UUID
  poNumber:             string
  revisionNumber:       number
  requisitionNumber?:   string | null
  legalEntityName:      string
  costCenterName:       string
  deliveryFacilityName: string
  vendorId:             UUID
  vendorName:           string
  vendorTaxNumber:      string
  status:               WorkflowStatus
  incoterms:            Incoterms
  currency:             string
  totalAmount:          number
  issuedAt:             ISODateString | null
  createdAt:            ISODateString
}

export interface PurchaseOrderDetailResponse {
  id:                   UUID
  poNumber:             string
  revisionNumber:       number
  requisitionId?:       UUID | null
  requisitionNumber?:   string | null
  legalEntityId:        UUID
  legalEntityName:      string
  costCenterId:         UUID
  costCenterName:       string
  deliveryFacilityId:   UUID
  deliveryFacilityName: string
  vendorId:             UUID
  vendorName:           string
  vendorTaxNumber:      string
  vendorOrderEmail:     string
  status:               WorkflowStatus
  incoterms:            Incoterms
  currency:             string
  totalAmount:          number
  paymentTerms:         PaymentTerms
  notes?:               string | null
  issuedAt:             ISODateString | null
  createdByUserId:      UUID
  createdByUserName:    string
  lineItems:            POLineItemResponse[]
  revisions:            PORevisionResponse[]
  crossAssignmentWarning?: { warningMessage: string } | null
  createdAt:            ISODateString
  updatedAt:            ISODateString
}
