package com.enterprise.spendsync.shared.config;

/**
 * Centralized REST API endpoints constants for SpendSync.
 * Prevents hardcoding URI paths across controllers and security configurations.
 */
public final class Endpoints {

    private Endpoints() {
        // Prevent instantiation
    }

    public static final String API_V1 = "/api/v1";

    public static final class Auth {
        public static final String BASE = API_V1 + "/auth";
        public static final String REGISTER_USER = "/register-user";
        public static final String USERS_BY_ID = "/users/{id}";
        public static final String LOGIN = "/login";
        public static final String REFRESH = "/refresh";
        public static final String LOGOUT = "/logout";

        // Sub-Account & Onboarding Endpoints
        public static final String SUBACCOUNT_INVITE_DETAILS = "/invitations/subaccount/{token}";
        public static final String ACCEPT_SUBACCOUNT_INVITE = "/accept-subaccount-invite";

        // Requisitioner Multi-Use Endpoints
        public static final String REQUISITIONER_INVITE_DETAILS = "/invitations/requisitioner/{token}";
        public static final String JOIN_AS_REQUISITIONER = "/join-as-requisitioner";
    }

    public static final class Organization {
        public static final String BASE = API_V1 + "/organization";
        public static final String CREATE_COMPANY = "/create-company";
        public static final String CURRENT_CONTEXT = "/current-context";

        public static final String LEGAL_ENTITIES = "/legal-entities";
        public static final String LEGAL_ENTITY_BY_ID = "/legal-entities/{id}";
        public static final String LEGAL_ENTITY_STATUS = "/legal-entities/{id}/status";

        public static final String FACILITIES = "/facilities";
        public static final String FACILITY_BY_ID = "/facilities/{id}";
        public static final String FACILITY_STATUS = "/facilities/{id}/status";

        public static final String COST_CENTERS = "/cost-centers";
        public static final String COST_CENTER_BY_ID = "/cost-centers/{id}";
        public static final String COST_CENTER_STATUS = "/cost-centers/{id}/status";

        public static final String USERS = "/users";
        public static final String USER_BY_ID = "/users/{id}";
        public static final String USER_ROLES = "/users/{id}/roles";
        public static final String USER_LEGAL_ENTITIES = "/users/{id}/legal-entities";
        public static final String USER_STATUS = "/users/{id}/status";

        // Invitation Management
        public static final String INVITE_SUBACCOUNT = "/users/invite-subaccount";
        public static final String GENERATE_REQUISITIONER_LINK = "/users/generate-requisitioner-link";
        public static final String INVITATIONS = "/invitations";
        public static final String INVITATION_BY_ID = "/invitations/{id}";
    }

    public static final class Budget {
        public static final String BASE = API_V1 + "/budget";
        public static final String POOLS = "/pools";
        public static final String POOL_BY_ID = "/pools/{id}";
        public static final String POOL_STATUS = "/pools/{id}/status";
        public static final String POOL_ADJUST = "/pools/{id}/adjust";
        public static final String POOL_TRANSACTIONS = "/pools/{id}/transactions";
        public static final String TRANSFERS = "/transfers";
        public static final String SUMMARY = "/summary";
    }

    public static final class Requisition {
        public static final String BASE = API_V1 + "/requisitions";
        public static final String MY_REQUISITIONS = "/my-requisitions";
        public static final String REQUISITION_BY_ID = "/{id}";
        public static final String PENDING_APPROVALS = "/pending-approvals";
        public static final String APPROVE = "/{id}/approve";
        public static final String REJECT = "/{id}/reject";
        public static final String CANCEL = "/{id}/cancel";

        public static final String APPROVAL_LIMITS = "/approval-limits";
        public static final String APPROVAL_LIMIT_BY_ID = "/approval-limits/{id}";
        public static final String APPROVAL_LIMIT_STATUS = "/approval-limits/{id}/status";
        public static final String EFFECTIVE_LIMIT = "/approval-limits/effective";
    }

    public static final class Purchasing {
        public static final String BASE = API_V1 + "/purchasing";

        // Vendor Management
        public static final String VENDORS_BASE = BASE + "/vendors";
        public static final String VENDOR_BY_ID = "/{id}";
        public static final String VENDOR_STATUS = "/{id}/status";
        public static final String VENDOR_INVITE = "/invite";
        public static final String BANK_CHANGE_REQUESTS = "/bank-change-requests";
        public static final String BANK_CHANGE_APPROVE = "/bank-change-requests/{id}/approve";
        public static final String BANK_CHANGE_REJECT = "/bank-change-requests/{id}/reject";

        // Purchase Orders
        public static final String ORDERS_BASE = BASE + "/orders";
        public static final String ORDER_BY_ID = "/{id}";
        public static final String ORDER_ISSUE = "/{id}/issue";
        public static final String ORDER_REVISE = "/{id}/revise";
        public static final String ORDER_REVISIONS = "/{id}/revisions";
        public static final String ORDER_CANCEL = "/{id}/cancel";
    }

    public static final class Audit {
        public static final String BASE = API_V1 + "/audit";
        public static final String LOGS = "/logs";
        public static final String TIMELINE = "/logs/timeline/{entityType}/{entityId}";
        public static final String CORRELATION = "/logs/correlation/{correlationId}";
        public static final String VIOLATIONS = "/violations";
    }

    public static final class Receiving {
        public static final String BASE = API_V1 + "/receiving";
        public static final String RECEIPTS = "/receipts";
        public static final String RECEIPT_BY_ID = "/receipts/{id}";
        public static final String RECEIPTS_BY_PO = "/receipts/by-po/{poId}";
        public static final String PENDING_ORDERS = "/orders/pending";
    }

    public static final class Matching {
        public static final String BASE = API_V1 + "/matching";
        public static final String INVOICES = "/invoices";
        public static final String INVOICE_BY_ID = "/invoices/{id}";
        public static final String INVOICES_BY_PO = "/invoices/by-po/{poId}";
        public static final String OVERRIDE = "/invoices/{id}/override";
        public static final String REJECT = "/invoices/{id}/reject";
    }

    public static final class Payment {
        public static final String BASE = API_V1 + "/payments";
        public static final String DUE_INVOICES = "/invoices/due";
        public static final String BATCHES = "/batches";
        public static final String BATCH_BY_ID = "/batches/{id}";
        public static final String APPROVE_BATCH = "/batches/{id}/approve";
        public static final String CANCEL_BATCH = "/batches/{id}/cancel";
    }

    public static final class Intelligence {
        public static final String BASE = API_V1 + "/intelligence";
        public static final String PULSE = "/pulse";
        public static final String ASK = "/ask";
        public static final String BUDGET_RUNWAY = "/budget-runway";
        public static final String SAVINGS_OPPORTUNITIES = "/savings-opportunities";
        public static final String WHAT_IF_SIMULATE = "/what-if-simulate";
    }

    public static final class Catalog {
        public static final String BASE = API_V1 + "/catalog";
        public static final String SEARCH = "/search";
        public static final String CATEGORIES = "/categories";
        public static final String ITEM_BY_ID = "/items/{id}";
        public static final String AUTOFILL = "/items/{id}/autofill";
        public static final String HEALTH = "/health";
    }

    public static final class AdminCatalog {
        public static final String BASE = API_V1 + "/admin/catalog";
        public static final String ITEMS = "/items";
        public static final String ITEM_BY_ID = "/items/{id}";
        public static final String CATEGORIES = "/categories";
        public static final String IMPORT = "/import";
        public static final String EXPORT = "/export";
    }

    public static final class Analytics {
        public static final String BASE = API_V1 + "/analytics";
        public static final String CFO_DECK = "/cfo-deck";
    }

    public static final class VendorPortal {
        public static final String BASE = API_V1 + "/vendor-portal";
        public static final String AUTH_BASE = BASE + "/auth";
        public static final String INVITE_DETAILS = "/invite/{token}";
        public static final String ACCEPT_INVITE = "/accept-invite";
        public static final String LOGIN = "/login";
        public static final String PROFILE_BASE = BASE + "/profile";
        public static final String BANK_CHANGE_REQUEST = "/bank-change-request";

        // Purchase Orders & ASN / e-Waybill
        public static final String ORDERS_BASE = BASE + "/orders";
        public static final String ORDER_BY_ID = "/{id}";
        public static final String ORDER_ACKNOWLEDGE = "/{id}/acknowledge";
        public static final String ORDER_DISPATCH = "/{id}/dispatch";
        public static final String ORDER_ASNS = "/{id}/asns";

        // Electronic Invoicing (PO-Flip, UBL-TR XML & HTML Render)
        public static final String INVOICES_BASE = BASE + "/invoices";
        public static final String INVOICE_BY_ID = "/{id}";
        public static final String INVOICE_PO_FLIP = "/po-flip/{poId}";
        public static final String INVOICE_UPLOAD_UBL = "/upload-ubl";
        public static final String INVOICE_HTML = "/{id}/html";
        public static final String INVOICE_PAYMENT_STATUS = "/{id}/payment-status";
        public static final String INVOICE_ACCEPT_EARLY_DISCOUNT = "/{id}/accept-early-discount";

        // Vendor Finance, SOA & BA-BS e-Reconciliation
        public static final String FINANCE_BASE = BASE + "/finance";
        public static final String EARLY_PAY_OFFERS = "/early-payment-offers";
        public static final String SOA = "/statement-of-accounts";
        public static final String RECONCILIATION = "/reconciliation";
        public static final String RECONCILIATION_APPROVE = "/reconciliation/approve";

        // Vendor Catalog Proposals
        public static final String CATALOG_BASE = BASE + "/catalog";
        public static final String CATALOG_PROPOSALS = "/proposals";
    }

    public static final class Notification {
        public static final String BASE = API_V1 + "/notifications";
        public static final String NOTIFICATION_BY_ID = "/{id}";
        public static final String MARK_READ = "/{id}/read";
        public static final String MARK_ALL_READ = "/read-all";
        public static final String UNREAD_COUNT = "/count/unread";
        public static final String PREFERENCES = "/preferences";
    }
}
