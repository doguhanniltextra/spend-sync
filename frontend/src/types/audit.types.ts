import type { UUID, ISODateString } from './common.types'

export interface AuditLogResponse {
  id:            UUID
  correlationId: string
  action:        string
  complianceTag: string
  actorId:       UUID
  actorEmail:    string
  actorRole:     string
  ipAddress:     string
  entityType:    string
  entityId:      string
  amount:        number
  currency:      string
  fromStatus:    string
  toStatus:      string
  decisionNote:  string
  checksum:      string
  createdAt:     ISODateString
}
