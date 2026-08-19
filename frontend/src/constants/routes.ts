/**
 * All application route path constants.
 * Never use raw path strings in navigate() or <Link to=...> — always use ROUTES.
 */
export const ROUTES = {
  login:      '/login',
  home:       '/',
  dashboard:  '/dashboard',

  requisitions: {
    root:     '/requisitions',
    new:      '/requisitions/new',
    detail:   (id: string) => `/requisitions/${id}`,
  },

  approvals: {
    root:     '/approvals',
  },

  purchasing: {
    root:     '/purchasing',
    new:      '/purchasing/new',
    vendors:  '/purchasing/vendors',
    orderDetail: (id: string) => `/purchasing/orders/${id}`,
  },

  receiving: {
    root:     '/receiving',
    new:      '/receiving/new',
    history:  '/receiving/history',
  },

  matching: {
    root:     '/matching',
    new:      '/matching/new',
    detail:   (id: string) => `/matching/${id}`,
  },

  payments: {
    root:     '/payments',
    runDetail:(id: string) => `/payments/runs/${id}`,
  },

  budgets: {
    root:     '/budgets',
    detail:   (id: string) => `/budgets/${id}`,
  },

  organization: {
    root:     '/organization',
  },

  audit: {
    root:     '/audit',
  },

  catalog: {
    root:     '/admin/catalog',
  },
} as const
