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
}
