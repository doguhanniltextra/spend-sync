import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { vendorPortalApi } from '../services/vendorPortalApi'
import type {
  AcknowledgePoRequest,
  DispatchAsnRequest,
  PoFlipInvoiceRequest,
  AcceptDiscountRequest,
  ApproveReconciliationRequest,
  CatalogProposalRequest,
  BankChangeRequestPayload,
} from '../types/vendorPortal.types'

export const VENDOR_QUERY_KEYS = {
  profile: ['vendor', 'profile'] as const,
  orders: (status?: string) => ['vendor', 'orders', status] as const,
  orderById: (id: string) => ['vendor', 'order', id] as const,
  invoices: (status?: string) => ['vendor', 'invoices', status] as const,
  invoiceById: (id: string) => ['vendor', 'invoice', id] as const,
  invoiceHtml: (id: string) => ['vendor', 'invoice-html', id] as const,
  paymentStatus: (id: string) => ['vendor', 'payment-status', id] as const,
  soa: (start?: string, end?: string) => ['vendor', 'soa', start, end] as const,
  reconciliation: (year?: number, month?: number) => ['vendor', 'reconciliation', year, month] as const,
  buyerBankChanges: ['buyer', 'bank-changes'] as const,
}

// ─── Queries ─────────────────────────────────────────────────────────────────

export function useVendorProfile() {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.profile,
    queryFn: () => vendorPortalApi.getProfile(),
  })
}

export function useVendorOrders(status?: string) {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.orders(status),
    queryFn: () => vendorPortalApi.getOrders(status),
  })
}

export function useVendorOrder(id: string) {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.orderById(id),
    queryFn: () => vendorPortalApi.getOrderById(id),
    enabled: Boolean(id),
  })
}

export function useVendorInvoices(status?: string) {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.invoices(status),
    queryFn: () => vendorPortalApi.getInvoices(status),
  })
}

export function useVendorInvoice(id: string) {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.invoiceById(id),
    queryFn: () => vendorPortalApi.getInvoiceById(id),
    enabled: Boolean(id),
  })
}

export function useVendorInvoiceHtml(id: string, enabled = true) {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.invoiceHtml(id),
    queryFn: () => vendorPortalApi.getInvoiceHtml(id),
    enabled: Boolean(id) && enabled,
  })
}

export function useInvoicePaymentStatus(id: string, enabled = true) {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.paymentStatus(id),
    queryFn: () => vendorPortalApi.getPaymentStatusTimeline(id),
    enabled: Boolean(id) && enabled,
  })
}

export function useStatementOfAccounts(startDate?: string, endDate?: string) {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.soa(startDate, endDate),
    queryFn: () => vendorPortalApi.getStatementOfAccounts(startDate, endDate),
  })
}

export function useReconciliationSummary(year?: number, month?: number) {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.reconciliation(year, month),
    queryFn: () => vendorPortalApi.getReconciliationSummary(year, month),
  })
}

export function useBuyerBankChangeRequests() {
  return useQuery({
    queryKey: VENDOR_QUERY_KEYS.buyerBankChanges,
    queryFn: () => vendorPortalApi.getBankChangeRequests(),
  })
}

// ─── Mutations ────────────────────────────────────────────────────────────────

export function useAcknowledgePo(orderId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: AcknowledgePoRequest) => vendorPortalApi.acknowledgeOrder(orderId, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vendor', 'orders'] })
      qc.invalidateQueries({ queryKey: VENDOR_QUERY_KEYS.orderById(orderId) })
    },
  })
}

export function useDispatchAsn(orderId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: DispatchAsnRequest) => vendorPortalApi.dispatchAsn(orderId, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vendor', 'orders'] })
      qc.invalidateQueries({ queryKey: VENDOR_QUERY_KEYS.orderById(orderId) })
    },
  })
}

export function usePoFlipInvoice() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ poId, payload }: { poId: string; payload: PoFlipInvoiceRequest }) =>
      vendorPortalApi.poFlipInvoice(poId, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vendor', 'invoices'] })
      qc.invalidateQueries({ queryKey: ['vendor', 'orders'] })
    },
  })
}

export function useAcceptEarlyDiscount(invoiceId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: AcceptDiscountRequest) => vendorPortalApi.acceptEarlyDiscount(invoiceId, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vendor', 'invoices'] })
      qc.invalidateQueries({ queryKey: VENDOR_QUERY_KEYS.paymentStatus(invoiceId) })
      qc.invalidateQueries({ queryKey: ['vendor', 'soa'] })
    },
  })
}

export function useApproveReconciliation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: ApproveReconciliationRequest) => vendorPortalApi.approveReconciliation(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vendor', 'reconciliation'] })
    },
  })
}

export function useRequestBankChange() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: BankChangeRequestPayload) => vendorPortalApi.requestBankChange(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: VENDOR_QUERY_KEYS.profile })
    },
  })
}

export function useProposeCatalogItem() {
  return useMutation({
    mutationFn: (payload: CatalogProposalRequest) => vendorPortalApi.proposeCatalogItem(payload),
  })
}
