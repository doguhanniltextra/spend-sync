import { apiClient } from '@/services/apiClient'
import { ENDPOINTS } from '@/constants/endpoints'
import type {
  LoginRequest,
  AuthTokenResponse,
  RefreshTokenRequest,
} from '@/types/auth.types'

/**
 * Authentication API service.
 * All auth HTTP operations live here — never call apiClient directly from components.
 */
export const authApi = {
  login: (dto: LoginRequest): Promise<AuthTokenResponse> =>
    apiClient.post<AuthTokenResponse>(ENDPOINTS.auth.login, dto).then((r) => r.data),

  refresh: (dto: RefreshTokenRequest): Promise<AuthTokenResponse> =>
    apiClient.post<AuthTokenResponse>(ENDPOINTS.auth.refresh, dto).then((r) => r.data),

  logout: (dto?: RefreshTokenRequest): Promise<void> =>
    apiClient.post(ENDPOINTS.auth.logout, dto).then(() => undefined),
} as const
