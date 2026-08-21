import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type {
  VendorAuthResponse,
  AcceptInviteRequest,
  VendorProfileResponse,
  BankChangeRequestPayload,
  BankChangeRequestResponse,
  VendorOrderResponse,
  AcknowledgePoRequest,
  DispatchAsnRequest,
  VendorAsnResponse,
  VendorInvoiceResponse,
  PoFlipInvoiceRequest,
  InvoicePaymentStatusResponse,
  AcceptDiscountRequest,
  StatementOfAccountsResponse,
  ReconciliationSummaryResponse,
  ApproveReconciliationRequest,
  CatalogProposalRequest,
} from '../types/vendorPortal.types'

export const vendorPortalApi = {
  // ─── Auth ───────────────────────────────────────────────────────────────────
  acceptInvite: async (payload: AcceptInviteRequest): Promise<VendorAuthResponse> => {
    const res = await apiClient.post<VendorAuthResponse>(ENDPOINTS.vendorPortal.acceptInvite, payload)
    return res.data
  },

  login: async (email: string, password: string): Promise<VendorAuthResponse> => {
    const res = await apiClient.post<VendorAuthResponse>(ENDPOINTS.vendorPortal.login, { email, password })
    return res.data
  },

  getProfile: async (): Promise<VendorProfileResponse> => {
    const res = await apiClient.get<VendorProfileResponse>(ENDPOINTS.vendorPortal.profile)
    return res.data
  },

  requestBankChange: async (payload: BankChangeRequestPayload): Promise<BankChangeRequestResponse> => {
    const res = await apiClient.post<BankChangeRequestResponse>(ENDPOINTS.vendorPortal.bankChangeRequest, payload)
    return res.data
  },

  // ─── Orders ─────────────────────────────────────────────────────────────────
  getOrders: async (status?: string): Promise<VendorOrderResponse[]> => {
    const res = await apiClient.get<VendorOrderResponse[]>(ENDPOINTS.vendorPortal.orders, {
      params: status ? { status } : undefined,
    })
    return res.data
  },

  getOrderById: async (id: string): Promise<VendorOrderResponse> => {
    const res = await apiClient.get<VendorOrderResponse>(ENDPOINTS.vendorPortal.orderById(id))
    return res.data
  },

  acknowledgeOrder: async (id: string, payload: AcknowledgePoRequest): Promise<VendorOrderResponse> => {
    const res = await apiClient.post<VendorOrderResponse>(ENDPOINTS.vendorPortal.acknowledgeOrder(id), payload)
    return res.data
  },

  dispatchAsn: async (id: string, payload: DispatchAsnRequest): Promise<VendorAsnResponse> => {
    const res = await apiClient.post<VendorAsnResponse>(ENDPOINTS.vendorPortal.dispatchAsn(id), payload)
    return res.data
  },

  // ─── Invoices ───────────────────────────────────────────────────────────────
  getInvoices: async (status?: string): Promise<VendorInvoiceResponse[]> => {
    const res = await apiClient.get<VendorInvoiceResponse[]>(ENDPOINTS.vendorPortal.invoices, {
      params: status ? { status } : undefined,
    })
    return res.data
  },

  getInvoiceById: async (id: string): Promise<VendorInvoiceResponse> => {
    const res = await apiClient.get<VendorInvoiceResponse>(ENDPOINTS.vendorPortal.invoiceById(id))
    return res.data
  },

  poFlipInvoice: async (poId: string, payload: PoFlipInvoiceRequest): Promise<VendorInvoiceResponse> => {
    const res = await apiClient.post<VendorInvoiceResponse>(ENDPOINTS.vendorPortal.poFlipInvoice(poId), payload)
    return res.data
  },

  uploadUblXml: async (xmlContent: string): Promise<VendorInvoiceResponse> => {
    const res = await apiClient.post<VendorInvoiceResponse>(ENDPOINTS.vendorPortal.ublUpload, xmlContent, {
      headers: { 'Content-Type': 'application/xml' },
    })
    return res.data
  },

  getInvoiceHtml: async (id: string): Promise<string> => {
    const res = await apiClient.get<string>(ENDPOINTS.vendorPortal.invoiceHtml(id), {
      headers: { Accept: 'text/html' },
      responseType: 'text',
    })
    return res.data
  },

  getPaymentStatusTimeline: async (id: string): Promise<InvoicePaymentStatusResponse> => {
    const res = await apiClient.get<InvoicePaymentStatusResponse>(ENDPOINTS.vendorPortal.paymentStatus(id))
    return res.data
  },

  acceptEarlyDiscount: async (id: string, payload: AcceptDiscountRequest): Promise<{ status: string; message: string; netAcceleratedAmount: number; scheduledPaymentDate: string }> => {
    const res = await apiClient.post(ENDPOINTS.vendorPortal.acceptEarlyDiscount(id), payload)
    return res.data
  },

  // ─── Finance, Catalog & Reconciliation ──────────────────────────────────────
  getStatementOfAccounts: async (startDate?: string, endDate?: string): Promise<StatementOfAccountsResponse> => {
    const res = await apiClient.get<StatementOfAccountsResponse>(ENDPOINTS.vendorPortal.statementOfAccounts, {
      params: { startDate, endDate },
    })
    return res.data
  },

  getReconciliationSummary: async (year?: number, month?: number): Promise<ReconciliationSummaryResponse> => {
    const res = await apiClient.get<ReconciliationSummaryResponse>(ENDPOINTS.vendorPortal.reconciliationSummary, {
      params: { year, month },
    })
    return res.data
  },

  approveReconciliation: async (payload: ApproveReconciliationRequest): Promise<ReconciliationSummaryResponse> => {
    const res = await apiClient.post<ReconciliationSummaryResponse>(ENDPOINTS.vendorPortal.approveReconciliation, payload)
    return res.data
  },

  proposeCatalogItem: async (payload: CatalogProposalRequest): Promise<{ id: string; status: string }> => {
    const res = await apiClient.post(ENDPOINTS.vendorPortal.proposeCatalogItem, payload)
    return res.data
  },

  // ─── Buyer Side Vendor Management ───────────────────────────────────────────
  inviteVendor: async (payload: { companyName: string; taxNumber: string; email: string }): Promise<{ invitationToken: string; message: string; email: string }> => {
    const res = await apiClient.post(ENDPOINTS.purchasing.inviteVendor, payload)
    return res.data
  },

  getBankChangeRequests: async (): Promise<BankChangeRequestResponse[]> => {
    const res = await apiClient.get<BankChangeRequestResponse[]>(ENDPOINTS.purchasing.bankChangeRequests)
    return res.data
  },

  approveBankChange: async (id: string, notes?: string): Promise<BankChangeRequestResponse> => {
    const res = await apiClient.post<BankChangeRequestResponse>(ENDPOINTS.purchasing.approveBankChange(id), { notes })
    return res.data
  },

  rejectBankChange: async (id: string, notes?: string): Promise<BankChangeRequestResponse> => {
    const res = await apiClient.post<BankChangeRequestResponse>(ENDPOINTS.purchasing.rejectBankChange(id), { notes })
    return res.data
  },
}
