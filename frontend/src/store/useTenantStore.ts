import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { UUID, CurrencyCode } from '@/types/common.types'

interface TenantState {
  tenantId:          UUID | null
  activeLegalEntity: UUID | null
  baseCurrency:      CurrencyCode
  companyName:       string | null

  setTenant:          (tenantId: UUID, companyName?: string) => void
  setActiveLegalEntity: (legalEntityId: UUID) => void
  setBaseCurrency:    (currency: CurrencyCode) => void
  clearTenant:        () => void
}

export const useTenantStore = create<TenantState>()(
  persist(
    (set) => ({
      tenantId:          null,
      activeLegalEntity: null,
      baseCurrency:      'TRY',
      companyName:       null,

      setTenant: (tenantId, companyName) => set({ tenantId, companyName: companyName ?? null }),

      setActiveLegalEntity: (legalEntityId) => set({ activeLegalEntity: legalEntityId }),

      setBaseCurrency: (currency) => set({ baseCurrency: currency }),

      clearTenant: () => set({
        tenantId:          null,
        activeLegalEntity: null,
        baseCurrency:      'TRY',
        companyName:       null,
      }),
    }),
    {
      name:    'spendsync_tenant',
      storage: createJSONStorage(() => localStorage),
    }
  )
)
