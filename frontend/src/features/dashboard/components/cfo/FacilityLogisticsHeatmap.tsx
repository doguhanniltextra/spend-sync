import { useState } from 'react'
import { MapPin, Truck } from 'lucide-react'
import { formatCurrency } from '@/utils/currency'

interface FacilityNode {
  id:          string
  code:        string
  name:        string
  city:        string
  country:     string
  type:        'OFFICE' | 'WAREHOUSE' | 'DATA_CENTER'
  activeSpend: number
  inboundShipments: number
  status:      'OPTIMAL' | 'CONGESTED' | 'NORMAL'
  coordinates: { x: number; y: number } // Percentage position on map
}

const FACILITIES: FacilityNode[] = [
  {
    id:          'f1',
    code:        'FAC-01',
    name:        'Maslak Financial Center HQ',
    city:        'Istanbul',
    country:     'TR',
    type:        'OFFICE',
    activeSpend: 1450000,
    inboundShipments: 8,
    status:      'NORMAL',
    coordinates: { x: 38, y: 35 },
  },
  {
    id:          'f2',
    code:        'FAC-02',
    name:        'Gebze R&D Logistics & Tech Center',
    city:        'Kocaeli / Gebze',
    country:     'TR',
    type:        'WAREHOUSE',
    activeSpend: 4850000,
    inboundShipments: 24,
    status:      'CONGESTED',
    coordinates: { x: 44, y: 40 },
  },
  {
    id:          'f3',
    code:        'FAC-03',
    name:        'Ankara Enterprise & Gov Office',
    city:        'Ankara',
    country:     'TR',
    type:        'OFFICE',
    activeSpend: 620000,
    inboundShipments: 3,
    status:      'OPTIMAL',
    coordinates: { x: 62, y: 52 },
  },
  {
    id:          'f4',
    code:        'FAC-UK-01',
    name:        'London Tech Solutions Hub',
    city:        'London',
    country:     'GB',
    type:        'DATA_CENTER',
    activeSpend: 2100000,
    inboundShipments: 11,
    status:      'OPTIMAL',
    coordinates: { x: 18, y: 22 },
  },
]

export function FacilityLogisticsHeatmap() {
  const [selectedFacility, setSelectedFacility] = useState<FacilityNode>(FACILITIES[1])

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs">
      <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center">
            <Truck className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">Geographic Logistics & Facility Spend Radar</h3>
            <p className="text-[11px] text-slate-500">Live dock inbound freight volume and location spend density</p>
          </div>
        </div>
        <span className="text-[11px] font-mono font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
          {FACILITIES.length} Operating Sites
        </span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 items-center">
        {/* Visual Map Canvas with Radar Ping Points */}
        <div className="lg:col-span-2 relative bg-slate-950 rounded-xl h-56 overflow-hidden border border-slate-800 flex items-center justify-center">
          {/* Subtle Grid Network Background */}
          <div
            className="absolute inset-0 opacity-20"
            style={{
              backgroundImage: 'radial-gradient(#94a3b8 1px, transparent 1px)',
              backgroundSize: '20px 20px',
            }}
          />

          {/* Regional Vector Outlines (Abstract Stylized Europe/Turkey Geography) */}
          <svg className="absolute inset-0 w-full h-full opacity-30" viewBox="0 0 500 250">
            <path
              d="M50,80 Q100,50 180,70 T300,120 T420,150"
              fill="none"
              stroke="#38bdf8"
              strokeWidth="1.5"
              strokeDasharray="4 4"
            />
            <path
              d="M100,60 L200,90 L220,110 L280,100 L320,130 L400,140"
              fill="none"
              stroke="#64748b"
              strokeWidth="1"
            />
          </svg>

          {/* Map Node Hotspots */}
          {FACILITIES.map((f) => {
            const isSelected = selectedFacility.id === f.id
            return (
              <div
                key={f.id}
                onClick={() => setSelectedFacility(f)}
                style={{ left: `${f.coordinates.x}%`, top: `${f.coordinates.y}%` }}
                className="absolute transform -translate-x-1/2 -translate-y-1/2 cursor-pointer group"
              >
                {/* Pulsing Radar Ring */}
                <span className="animate-ping absolute inline-flex h-7 w-7 rounded-full bg-sky-400 opacity-40" />

                {/* Hotspot Icon */}
                <div
                  className={`relative w-6 h-6 rounded-full flex items-center justify-center transition-all ${
                    isSelected
                      ? 'bg-amber-400 text-slate-950 scale-125 ring-4 ring-amber-400/30'
                      : 'bg-sky-500 text-white hover:scale-110'
                  }`}
                >
                  <MapPin className="w-3.5 h-3.5" />
                </div>

                {/* Floating Tag */}
                <div className="absolute left-1/2 -translate-x-1/2 top-7 bg-slate-900/90 text-white px-2 py-0.5 rounded text-[9px] font-mono whitespace-nowrap opacity-80 group-hover:opacity-100 border border-slate-700">
                  {f.code} • {f.city}
                </div>
              </div>
            )
          })}
        </div>

        {/* Selected Facility Inspector Card */}
        <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-3 text-xs">
          <div className="flex items-center justify-between border-b border-slate-200 pb-2">
            <span className="font-mono font-bold text-slate-900 text-xs bg-slate-200/80 px-2 py-0.5 rounded">
              {selectedFacility.code}
            </span>
            <span
              className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                selectedFacility.status === 'CONGESTED'
                  ? 'bg-amber-100 text-amber-800'
                  : 'bg-emerald-100 text-emerald-800'
              }`}
            >
              {selectedFacility.status} DOCK
            </span>
          </div>

          <div>
            <strong className="text-slate-900 text-xs font-bold block">{selectedFacility.name}</strong>
            <span className="text-[11px] text-slate-500">{selectedFacility.city}, {selectedFacility.country}</span>
          </div>

          <div className="space-y-2 pt-1 border-t border-slate-200/60">
            <div className="flex items-center justify-between">
              <span className="text-slate-500 text-[11px]">Cumulative Active Spend:</span>
              <span className="font-mono font-bold text-slate-900">
                {formatCurrency(selectedFacility.activeSpend)}
              </span>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-slate-500 text-[11px]">Pending Dock Freight:</span>
              <span className="font-mono font-bold text-slate-900">
                {selectedFacility.inboundShipments} Shipments
              </span>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-slate-500 text-[11px]">Facility Classification:</span>
              <span className="font-semibold text-slate-700 text-[11px]">
                {selectedFacility.type}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
