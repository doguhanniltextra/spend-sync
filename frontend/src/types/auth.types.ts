import type { UUID, ISODateString } from './common.types'

/** POST /auth/login request body — mirrors backend LoginRequest record */
export interface LoginRequest {
  email:    string
  password: string
}

/** POST /auth/refresh request body */
export interface RefreshTokenRequest {
  refreshToken: string
}

/**
 * POST /auth/login response — mirrors backend AuthTokenResponse record.
 * roles is an array of role name strings e.g. ["ROLE_APPROVER", "ROLE_AP_CLERK"]
 */
export interface AuthTokenResponse {
  accessToken:      string
  refreshToken:     string
  expiresInSeconds: number
  tokenType:        string
  userId:           UUID
  email:            string
  fullName:         string
  tenantId:         UUID
  roles:            string[]
}

/** Hydrated user profile stored in Zustand auth store */
export interface AuthUser {
  id:       UUID
  email:    string
  fullName: string
  tenantId: UUID
  roles:    string[]
}

/** GET /auth/users/{id} response */
export interface UserResponse {
  id:                UUID
  email:             string
  firstName:         string
  lastName:          string
  fullName:          string
  jobTitle:          string | null
  phoneNumber:       string | null
  country:           string | null
  timezone:          string | null
  preferredLanguage: string | null
  isActive:          boolean
  isEmailVerified:   boolean
  roles:             string[]
  createdAt:         ISODateString
}
