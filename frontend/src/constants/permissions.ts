/**
 * RBAC permission string constants.
 * Never compare role/permission strings inline — always use PERMISSIONS.
 * Maps to backend RoleType enum and GrantedAuthority values.
 */
export const PERMISSIONS = {
  requisition: {
    create:   'PR_CREATE',
    readOwn:  'PR_READ_OWN',
    readAll:  'PR_READ_ALL',
    approve:  'PR_APPROVE',
    cancel:   'PR_CANCEL',
  },
  purchasing: {
    readPO:        'PO_READ',
    createPO:      'PO_CREATE',
    issuePO:       'PO_ISSUE',
    cancelPO:      'PO_CANCEL',
    manageVendors: 'VENDOR_MANAGE',
  },
  receiving: {
    create:   'GR_CREATE',
    read:     'GR_READ',
  },
  matching: {
    createInvoice: 'INVOICE_CREATE',
    evaluate:      'MATCH_EVALUATE',
    override:      'MATCH_OVERRIDE',
  },
  payment: {
    release:  'PAYMENT_RELEASE',
    read:     'PAYMENT_READ',
  },
  budget: {
    read:     'BUDGET_READ',
    manage:   'BUDGET_MANAGE',
  },
  organization: {
    manage:   'ORG_MANAGE',
    read:     'ORG_READ',
  },
  audit: {
    read:     'AUDIT_READ',
  },
} as const

/**
 * Backend role types — maps to RoleType.java enum.
 */
export const ROLES = {
  rootUser:     'ROLE_ROOT_USER',
  approver:     'ROLE_APPROVER',
  apClerk:      'ROLE_AP_CLERK',
  requisitioner:'ROLE_REQUISITIONER',
  auditor:      'ROLE_AUDITOR',
} as const

export type RoleType = typeof ROLES[keyof typeof ROLES]
