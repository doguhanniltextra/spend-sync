import { Clock } from 'lucide-react'

export default function ComingSoonPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[50vh] text-center p-6 bg-white border border-slate-200 rounded-xl shadow-sm">
      <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center text-slate-500 mb-4">
        <Clock className="w-6 h-6" />
      </div>
      <h2 className="text-lg font-semibold text-slate-900 mb-1">Module Under Construction</h2>
      <p className="text-sm text-slate-500 max-w-sm">
        This module is scheduled for implementation in upcoming frontend phases.
      </p>
    </div>
  )
}
