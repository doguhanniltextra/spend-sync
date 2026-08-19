import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Copy, Check, KeyRound } from 'lucide-react'
import type { LegalEntityResponse, RequisitionerLinkResponse } from '@/types/organization.types'
import { useToast } from '@/components/feedback/Toast'

interface RequisitionerLinkModalProps {
  isOpen:        boolean
  onClose:       () => void
  legalEntities: LegalEntityResponse[]
  onGenerate:    (payload: { targetLegalEntityId: string; expirationDays?: number }) => Promise<RequisitionerLinkResponse>
}

export function RequisitionerLinkModal({
  isOpen,
  onClose,
  legalEntities,
  onGenerate,
}: RequisitionerLinkModalProps) {
  const toast = useToast()
  const [legalEntityId, setLegalEntityId] = useState(legalEntities[0]?.id || '')
  const [expirationDays, setExpirationDays] = useState(7)
  const [isGenerating, setIsGenerating] = useState(false)
  const [generatedResult, setGeneratedResult] = useState<RequisitionerLinkResponse | null>(null)
  const [copied, setCopied] = useState(false)

  const handleGenerate = async () => {
    if (!legalEntityId) return
    try {
      setIsGenerating(true)
      const res = await onGenerate({ targetLegalEntityId: legalEntityId, expirationDays })
      setGeneratedResult(res)
    } catch {
      // Handled in hook
    } finally {
      setIsGenerating(false)
    }
  }

  const handleCopy = () => {
    if (!generatedResult) return
    const fullUrl = `${window.location.origin}/register?token=${generatedResult.linkToken}`
    navigator.clipboard.writeText(fullUrl)
    setCopied(true)
    toast.success('Registration link copied to clipboard!')
    setTimeout(() => setCopied(false), 2500)
  }

  const entityOptions = [
    { value: '', label: 'Select Target Legal Entity...' },
    ...legalEntities.map((e) => ({ value: e.id, label: `${e.name} (${e.companyCode})` })),
  ]

  return (
    <Modal
      isOpen={isOpen}
      onClose={() => {
        setGeneratedResult(null)
        onClose()
      }}
      title="Generate Requisitioner Passkey Link"
      description="Create a self-service onboarding link for departmental staff to submit purchase requests."
      maxWidth="md"
      footer={
        <div className="flex items-center justify-end gap-2 w-full">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setGeneratedResult(null)
              onClose()
            }}
          >
            Close
          </Button>
          {!generatedResult && (
            <Button
              size="sm"
              onClick={handleGenerate}
              isLoading={isGenerating}
              disabled={!legalEntityId}
              className="bg-slate-900 text-white"
            >
              Generate Passkey Link
            </Button>
          )}
        </div>
      }
    >
      <div className="space-y-4 text-xs">
        {!generatedResult ? (
          <>
            <Select
              label="Assigned Legal Entity"
              value={legalEntityId}
              onChange={(e) => setLegalEntityId(e.target.value)}
              options={entityOptions}
              required
            />

            <Input
              label="Link Validity (Days)"
              type="number"
              value={expirationDays}
              onChange={(e) => setExpirationDays(Number(e.target.value))}
              placeholder="7"
            />
          </>
        ) : (
          <div className="space-y-3 bg-slate-50 p-4 rounded-lg border border-slate-200">
            <div className="flex items-center gap-2 text-emerald-700 font-bold">
              <KeyRound className="w-4 h-4" />
              <span>Passkey Generated Successfully</span>
            </div>

            <p className="text-slate-600 text-[11px]">
              Share this link with your team. Anyone with this link can self-register as a Requisitioner:
            </p>

            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={`${window.location.origin}/register?token=${generatedResult.linkToken}`}
                className="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 font-mono text-[11px] text-slate-800 focus:outline-none"
              />
              <Button
                size="sm"
                variant="outline"
                onClick={handleCopy}
                leftIcon={copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
              >
                {copied ? 'Copied' : 'Copy'}
              </Button>
            </div>

            <div className="text-[10px] text-slate-400">
              Expires on: <strong>{new Date(generatedResult.expiresAt).toLocaleDateString()}</strong>
            </div>
          </div>
        )}
      </div>
    </Modal>
  )
}
