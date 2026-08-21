import { useStatementOfAccounts, useVendorInvoices, useInvoicePaymentStatus } from '../hooks/useVendorPortalQueries'
import { EarlyDiscountOfferCard } from '../components/finance/EarlyDiscountOfferCard'
import { VendorPaymentTimeline } from '../components/finance/VendorPaymentTimeline'
import { StatementOfAccountsTable } from '../components/finance/StatementOfAccountsTable'

export function VendorFinancePage() {
  const { data: invoices = [] } = useVendorInvoices()
  const { data: soa, isLoading: soaLoading } = useStatementOfAccounts()

  // Find invoice approved for payment or submitted to show live progression
  const activeInvoice = invoices.find(
    (inv) => inv.status === 'APPROVED_FOR_PAYMENT' || inv.status === 'SUBMITTED' || inv.status === 'PAID'
  )

  const { data: paymentStatus } = useInvoicePaymentStatus(
    activeInvoice?.id || '',
    Boolean(activeInvoice)
  )

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Page Title */}
      <div>
        <h2 className="text-2xl font-bold text-slate-900 tracking-tight">
          Financial Hub & Cash Flow Management
        </h2>
        <p className="text-sm text-slate-500 mt-1">
          Monitor real-time accounts payable status, lock in accelerated early payment discounts, and view statement of accounts.
        </p>
      </div>

      {/* 1. Dynamic Discount Offer Card (If Available) */}
      {activeInvoice && activeInvoice.status === 'APPROVED_FOR_PAYMENT' && (
        <EarlyDiscountOfferCard
          invoiceId={activeInvoice.id}
          invoiceNumber={activeInvoice.invoiceNumber}
          originalAmount={activeInvoice.payableAmount}
          originalDueDate={activeInvoice.dueDate}
          discountPercentage={2}
          discountAmount={activeInvoice.payableAmount * 0.02}
          netPayoutAmount={activeInvoice.payableAmount * 0.98}
          currency={activeInvoice.currency}
          acceleratedDate={new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]}
        />
      )}

      {/* 2. Live AP Payment Stepper */}
      {paymentStatus && (
        <VendorPaymentTimeline
          invoiceNumber={paymentStatus.invoiceNumber}
          payableAmount={paymentStatus.payableAmount}
          currency={paymentStatus.currency}
          dueDate={paymentStatus.dueDate}
          maskedPayoutIban={paymentStatus.maskedPayoutIban}
          timeline={paymentStatus.timeline}
        />
      )}

      {/* 3. Statement of Accounts (Cari Hesap Ekstresi) */}
      {soaLoading ? (
        <div className="p-8 text-center text-slate-500 bg-white rounded-2xl border border-slate-200">
          <div className="w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          Loading Statement of Accounts...
        </div>
      ) : soa ? (
        <StatementOfAccountsTable soa={soa} />
      ) : null}
    </div>
  )
}
export default VendorFinancePage
