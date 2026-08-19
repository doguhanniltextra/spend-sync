import type { UUID, ISODateString } from './common.types'

export type RoleType =
  | 'ROOT_USER'
  | 'APPROVER'
  | 'REQUISITIONER'
  | 'PROCUREMENT'
  | 'AP_SPECIALIST'
  | 'ACCOUNT_USER'
  | 'FACILITY_USER'
  | string

export type FacilityType = 'OFFICE' | 'WAREHOUSE' | 'PLANT' | 'RETAIL' | 'DATA_CENTER'

export interface LegalEntityResponse {
  id:                UUID
  tenantId:          UUID
  name:              string
  companyCode:       string
  taxNumber:         string
  taxOffice:         string | null
  baseCurrency:      string
  registeredAddress: string
  country:           string
  isActive:          boolean
  createdAt:         ISODateString
}

export interface CreateLegalEntityRequest {
  name:              string
  companyCode:       string
  taxNumber:         string
  taxOffice?:        string
  baseCurrency:      string
  registeredAddress: string
  country:           string
}

export interface UpdateLegalEntityRequest {
  name:              string
  companyCode:       string
  taxNumber:         string
  taxOffice?:        string
  baseCurrency:      string
  registeredAddress: string
  country:           string
}

export interface CostCenterResponse {
  id:              UUID
  tenantId:        UUID
  legalEntityId:   UUID
  legalEntityName: string
  code:            string
  name:            string
  managerUserId:   UUID | null
  managerFullName: string | null
  isActive:        boolean
  createdAt:       ISODateString
}

export interface CreateCostCenterRequest {
  legalEntityId: UUID
  code:          string
  name:          string
  managerUserId?:UUID
}

export interface UpdateCostCenterRequest {
  code:          string
  name:          string
  managerUserId?:UUID
}

export interface FacilityResponse {
  id:              UUID
  tenantId:        UUID
  legalEntityId:   UUID
  legalEntityName: string
  name:            string
  facilityCode:    string
  facilityType:    FacilityType
  shippingAddress: string
  contactPerson:   string | null
  contactPhone:    string | null
  isActive:        boolean
  createdAt:       ISODateString
}

export interface CreateFacilityRequest {
  legalEntityId:   UUID
  name:            string
  facilityCode:    string
  facilityType:    FacilityType
  shippingAddress: string
  contactPerson?:  string
  contactPhone?:   string
}

export interface UpdateFacilityRequest {
  name:            string
  facilityCode:    string
  facilityType:    FacilityType
  shippingAddress: string
  contactPerson?:  string
  contactPhone?:   string
}

export interface UserResponse {
  id:                UUID
  email:             string
  firstName:         string
  lastName:          string
  fullName:          string
  jobTitle:          string | null
  phoneNumber:       string | null
  country:           string | null
  timezone:          string | null
  preferredLanguage: string | null
  isActive:          boolean
  isEmailVerified:   boolean
  roles:             RoleType[]
  createdAt:         ISODateString
}

export interface UpdateUserRolesRequest {
  roles: RoleType[]
}

export interface UpdateUserLegalEntitiesRequest {
  legalEntityIds: UUID[]
}

export interface SubAccountInvitationResponse {
  id:                  UUID
  email:               string
  targetLegalEntityId: UUID
  targetRoles:         RoleType[]
  token:               string
  expiresAt:           ISODateString
  createdAt:           ISODateString
}

export interface InviteSubAccountRequest {
  email:               string
  targetLegalEntityId: UUID
  targetRoles:         RoleType[]
  expirationHours?:    number
}

export interface GenerateRequisitionerLinkRequest {
  targetLegalEntityId: UUID
  expirationDays?:     number
}

export interface RequisitionerLinkResponse {
  targetLegalEntityId: UUID
  linkToken:           string
  registrationUrl:     string
  expiresAt:           ISODateString
}

export interface ApprovalLimitResponse {
  id:              UUID
  tenantId:        UUID
  userId:          UUID
  userFullName:    string
  userEmail:       string
  legalEntityId:   UUID
  legalEntityName: string
  costCenterId:    UUID | null
  costCenterName:  string | null
  approvalLevel:   number
  minAmount:       number
  maxAmount:       number | null
  isUnlimited:     boolean
  currency:        string
  isActive:        boolean
  createdAt:       ISODateString
  updatedAt:       ISODateString
}

export interface SetApprovalLimitRequest {
  userId:         UUID
  legalEntityId:  UUID
  costCenterId?:  UUID
  approvalLevel:  number
  minAmount?:     number
  maxAmount?:     number
  currency:       string
}
