import type { UUID, ISODateString } from './common.types'
import type { WorkflowStatus } from '@/constants/workflow'

export type ApprovalStepStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'BYPASSED'

export interface CreateLineItemRequest {
  itemDescription:       string
  itemCategory:          string
  quantity:              number
  unitOfMeasure:         string
  unitPrice:             number
  estimatedDeliveryDate?: string
}

export interface CreateRequisitionRequest {
  legalEntityId:      UUID
  costCenterId:       UUID
  deliveryFacilityId: UUID
  title:              string
  justification:      string
  currency:           string
  lineItems:          CreateLineItemRequest[]
}

export interface LineItemResponse {
  id:                    UUID
  lineNumber:            number
  itemDescription:       string
  itemCategory:          string
  quantity:              number
  unitOfMeasure:         string
  unitPrice:             number
  lineTotal:             number
  estimatedDeliveryDate: string | null
}

export interface ApprovalStepResponse {
  id:            UUID
  stepOrder:     number
  approverId:    UUID
  approverName:  string
  approverEmail: string
  approvalLevel: number
  status:        ApprovalStepStatus
  decisionNote:  string | null
  decidedAt:     ISODateString | null
}

export interface CrossAssignmentWarning {
  warningMessage: string
  assignedCostCenterCode?: string
  targetCostCenterCode?: string
}

export interface RequisitionSummaryResponse {
  id:                   UUID
  prNumber:             string
  title:                string
  costCenterId:         UUID
  costCenterCode:       string
  costCenterName:       string
  requesterId:          UUID
  requesterName:        string
  totalEstimatedAmount: number
  currency:             string
  status:               WorkflowStatus
  createdAt:            ISODateString
}

export interface RequisitionDetailResponse {
  id:                     UUID
  requisitionNumber:      string
  prNumber?:              string
  requisitionerId:        UUID
  requisitionerName:      string
  requesterName?:         string
  requisitionerEmail:     string
  legalEntityId:          UUID
  legalEntityName:        string
  costCenterId:           UUID
  costCenterName:         string
  costCenterCode:         string
  deliveryFacilityId:     UUID
  deliveryFacilityName:   string
  budgetPoolId:           UUID | null
  status:                 WorkflowStatus
  totalAmount:            number
  totalEstimatedAmount?:  number
  currency:               string
  title:                  string
  justification:          string
  rejectionReason:        string | null
  crossAssignmentWarning: CrossAssignmentWarning | null
  lineItems:              LineItemResponse[]
  approvalSteps:          ApprovalStepResponse[]
  createdAt:              ISODateString
  approvedAt:             ISODateString | null
}
