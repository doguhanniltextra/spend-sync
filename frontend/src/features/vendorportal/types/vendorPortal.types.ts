export interface VendorAuthResponse {
  accessToken: string;
  refreshToken: string;
  vendorId: string;
  vendorName: string;
  tenantId: string;
  vendorUserId: string;
  email: string;
  fullName: string;
  role: 'VENDOR_ADMIN' | 'VENDOR_USER';
}

export interface VendorUser {
  id: string;
  vendorId: string;
  vendorName: string;
  tenantId: string;
  email: string;
  fullName: string;
  role: 'VENDOR_ADMIN' | 'VENDOR_USER';
  phoneNumber?: string;
  isPrimaryContact?: boolean;
}

export interface AcceptInviteRequest {
  invitationToken: string;
  password: string;
  fullName: string;
  phoneNumber?: string;
  taxOffice?: string;
  iban?: string;
  bankName?: string;
  address?: string;
  city?: string;
}

export interface VendorProfileResponse {
  id: string;
  name: string;
  taxNumber: string;
  taxOffice?: string;
  bankName?: string;
  maskedIban: string;
  address?: string;
  city?: string;
  country?: string;
  orderEmail?: string;
  phoneNumber?: string;
  category?: string;
  tier?: string;
  status: string;
  isEinvoiceRegistered: boolean;
  currentUser: VendorUser;
}

export interface BankChangeRequestPayload {
  newBankName: string;
  newIban: string;
  reason: string;
  documentRef?: string;
}

export interface BankChangeRequestResponse {
  id: string;
  vendorId: string;
  vendorName: string;
  requestedByUserId: string;
  requestedByUserName: string;
  oldIbanMasked: string;
  newIbanMasked: string;
  newBankName: string;
  reason: string;
  documentRef?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewNotes?: string;
  createdAt: string;
}

export interface VendorOrderLineItem {
  id: string;
  itemDescription: string;
  sku?: string;
  category?: string;
  unitOfMeasure?: string;
  quantityOrdered: number;
  quantityReceived: number;
  unitPrice: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
}

export interface VendorOrderResponse {
  id: string;
  poNumber: string;
  revisionNumber: number;
  status: 'DRAFT' | 'ISSUED' | 'ACKNOWLEDGED' | 'IN_TRANSIT' | 'PARTIALLY_DELIVERED' | 'RECEIVED' | 'CANCELLED';
  legalEntityId: string;
  legalEntityName: string;
  deliveryFacilityName: string;
  deliveryAddress: string;
  paymentTerms: string;
  incoterms: string;
  currency: string;
  totalAmount: number;
  issuedAt?: string;
  notes?: string;
  acknowledgmentStatus?: 'PENDING' | 'ACCEPTED' | 'REVISED_DATE_PROPOSED' | 'REJECTED';
  acknowledgedAt?: string;
  vendorPromisedDeliveryDate?: string;
  vendorAcknowledgmentNotes?: string;
  lineItems: VendorOrderLineItem[];
}

export interface AcknowledgePoRequest {
  status: 'ACCEPTED' | 'REVISED_DATE_PROPOSED' | 'REJECTED';
  promisedDeliveryDate?: string;
  vendorNotes?: string;
}

export interface AsnLineItemPayload {
  poLineItemId: string;
  quantityShipped: number;
  lotBatchNumber?: string;
  serialNumbers?: string[];
}

export interface DispatchAsnRequest {
  waybillNumber: string;
  carrierCompany: string;
  trackingNumber?: string;
  vehiclePlate?: string;
  driverFullName?: string;
  driverNationalId?: string;
  estimatedArrivalDate: string;
  notes?: string;
  items: AsnLineItemPayload[];
}

export interface VendorAsnResponse {
  id: string;
  shipmentNumber: string;
  waybillNumber: string;
  carrierCompany: string;
  trackingNumber?: string;
  vehiclePlate?: string;
  driverFullName?: string;
  driverNationalIdMasked?: string;
  estimatedArrivalDate: string;
  status: 'DISPATCHED' | 'DELIVERED';
  createdAt: string;
}

export interface VendorInvoiceLineItem {
  id: string;
  itemDescription: string;
  quantity: number;
  unitPrice: number;
  subtotalAmount: number;
  taxRate: number;
  taxAmount: number;
  withholdingTaxRate?: number;
  withholdingTaxAmount?: number;
  totalAmount: number;
}

export interface VendorInvoiceResponse {
  id: string;
  invoiceNumber: string;
  ettn: string;
  invoiceDate: string;
  dueDate: string;
  purchaseOrderId?: string;
  poNumber?: string;
  currency: string;
  subtotalAmount: number;
  taxAmount: number;
  withholdingTaxAmount: number;
  totalAmount: number;
  payableAmount: number;
  status: 'SUBMITTED' | 'UNDER_REVIEW' | 'HOLD' | 'APPROVED_FOR_PAYMENT' | 'PAID' | 'REJECTED';
  matchType?: 'TWO_WAY' | 'THREE_WAY' | 'NON_PO';
  matchStatus?: 'PENDING_MATCH' | 'AUTO_MATCHED' | 'MANUALLY_MATCHED' | 'DISCREPANCY';
  discrepancyReason?: string;
  rawUblXml?: string;
  createdAt: string;
  lineItems?: VendorInvoiceLineItem[];
}

export interface PoFlipInvoiceRequest {
  invoiceNumber: string;
  ettn?: string;
  invoiceDate: string;
  taxWithholdingCode?: string; // 601, 608, 627, etc.
  notes?: string;
}

export interface InvoicePaymentTimelineStep {
  step: 'SUBMITTED' | 'MATCHED' | 'APPROVED_FOR_PAYMENT' | 'PAID';
  title: string;
  description: string;
  completed: boolean;
  timestamp?: string;
}

export interface InvoicePaymentStatusResponse {
  invoiceId: string;
  invoiceNumber: string;
  currentStatus: string;
  matchStatus: string;
  dueDate: string;
  payableAmount: number;
  currency: string;
  maskedPayoutIban: string;
  timeline: InvoicePaymentTimelineStep[];
}

export interface DynamicDiscountOfferResponse {
  invoiceId: string;
  invoiceNumber: string;
  originalDueDate: string;
  acceleratedPaymentDate: string;
  originalPayableAmount: number;
  discountPercentage: number;
  discountAmount: number;
  netAcceleratedAmount: number;
  currency: string;
  offerExpiresAt: string;
  status: 'AVAILABLE' | 'ACCEPTED' | 'EXPIRED';
}

export interface AcceptDiscountRequest {
  discountPercentage?: number;
}

export interface StatementItem {
  date: string;
  type: 'INVOICE' | 'PAYMENT' | 'CREDIT_MEMO';
  documentNumber: string;
  description: string;
  currency: string;
  debitAmount: number;  // Paid to vendor
  creditAmount: number; // Invoiced by vendor
  runningBalance: number;
}

export interface StatementOfAccountsResponse {
  vendorId: string;
  vendorName: string;
  startDate: string;
  endDate: string;
  openingBalance: number;
  totalInvoiced: number;
  totalPaid: number;
  closingBalance: number;
  currency: string;
  items: StatementItem[];
}

export interface ReconciliationSummaryResponse {
  vendorId: string;
  vendorName: string;
  periodYear: number;
  periodMonth: number;
  invoiceCount: number;
  totalAmount: number;
  currency: string;
  status: 'PENDING' | 'APPROVED' | 'DISPUTED';
  vendorApprovedAt?: string;
  signedChecksum?: string;
  vendorNotes?: string;
}

export interface ApproveReconciliationRequest {
  periodYear: number;
  periodMonth: number;
  vendorNotes?: string;
}

export interface CatalogProposalRequest {
  sku: string;
  name: string;
  description?: string;
  category: string;
  unitOfMeasure: string;
  unitPrice: number;
  currency: string;
  taxRate: number;
  leadTimeDays: number;
  moq: number;
  notes?: string;
}
