import { Shield, Clock } from 'lucide-react'
import type { AuditLogResponse } from '@/types/audit.types'
import { fromNow, formatDateTime } from '@/utils/date'
import { DASHBOARD_COPY } from '../constants/dashboardCopy'

interface AuditActivityFeedProps {
  logs: AuditLogResponse[]
}

export function AuditActivityFeed({ logs }: AuditActivityFeedProps) {
  const recentLogs = logs.slice(0, 6)

  return (
    <div className="bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
      <div className="px-5 py-4 border-b border-slate-200 bg-white flex items-center justify-between">
        <div>
          <h3 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
            <Shield className="w-4 h-4 text-slate-700" />
            {DASHBOARD_COPY.auditFeed.title}
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            {DASHBOARD_COPY.auditFeed.subtitle}
          </p>
        </div>
      </div>

      {recentLogs.length === 0 ? (
        <div className="p-8 text-center text-xs text-slate-500">
          {DASHBOARD_COPY.auditFeed.emptyText}
        </div>
      ) : (
        <div className="divide-y divide-slate-100">
          {recentLogs.map((log) => (
            <div key={log.id} className="p-4 hover:bg-slate-50/70 transition-colors flex items-start justify-between gap-3 text-xs">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono font-semibold text-slate-900 uppercase">
                    {log.action}
                  </span>
                  <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 font-mono">
                    {log.entityType}
                  </span>
                </div>
                <p className="text-slate-600 mt-0.5">
                  Actor: <span className="font-medium text-slate-900">{log.actorEmail}</span> ({log.actorRole})
                </p>
                {log.decisionNote && (
                  <p className="text-[11px] text-slate-500 italic mt-0.5">
                    "{log.decisionNote}"
                  </p>
                )}
              </div>
              <div className="text-right shrink-0">
                <span className="text-[11px] text-slate-500 flex items-center justify-end gap-1" title={formatDateTime(log.createdAt)}>
                  <Clock className="w-3 h-3" />
                  {fromNow(log.createdAt)}
                </span>
                <span className="text-[10px] font-mono text-slate-400 block mt-0.5">
                  {log.correlationId.slice(0, 8)}...
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
