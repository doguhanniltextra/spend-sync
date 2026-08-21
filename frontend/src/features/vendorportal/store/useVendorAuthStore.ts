import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { VendorAuthResponse, VendorUser } from '../types/vendorPortal.types'

interface VendorAuthState {
  vendorUser: VendorUser | null
  vendorId: string | null
  vendorName: string | null
  tenantId: string | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean

  setAuth: (response: VendorAuthResponse) => void
  logout: () => void
  isVendorAdmin: () => boolean
}

export const useVendorAuthStore = create<VendorAuthState>()(
  persist(
    (set, get) => ({
      vendorUser: null,
      vendorId: null,
      vendorName: null,
      tenantId: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      setAuth: (response: VendorAuthResponse) => {
        set({
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
          vendorId: response.vendorId,
          vendorName: response.vendorName,
          tenantId: response.tenantId,
          isAuthenticated: true,
          vendorUser: {
            id: response.vendorUserId,
            vendorId: response.vendorId,
            vendorName: response.vendorName,
            tenantId: response.tenantId,
            email: response.email,
            fullName: response.fullName,
            role: response.role,
          },
        })
      },

      logout: () => {
        set({
          vendorUser: null,
          vendorId: null,
          vendorName: null,
          tenantId: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        })
      },

      isVendorAdmin: () => {
        const { vendorUser } = get()
        return vendorUser?.role === 'VENDOR_ADMIN'
      },
    }),
    {
      name: 'spendsync_vendor_auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        vendorUser: state.vendorUser,
        vendorId: state.vendorId,
        vendorName: state.vendorName,
        tenantId: state.tenantId,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)
