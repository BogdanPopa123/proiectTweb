import React from 'react'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, ReferenceLine
} from 'recharts'

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('ro-RO', { month: 'short', day: 'numeric' })
}

const formatPrice = (value) => `${value.toFixed(2)} RON`

export default function PriceHistoryChart({ history, currency = 'RON' }) {
  if (!history || history.length === 0) {
    return (
      <div className="flex items-center justify-center h-40 text-gray-400 text-sm">
        No price history available
      </div>
    )
  }

  const data = [...history]
    .sort((a, b) => new Date(a.recordedAt) - new Date(b.recordedAt))
    .map((h) => ({
      date: formatDate(h.recordedAt),
      price: parseFloat(h.price),
      full: h.recordedAt,
    }))

  const prices = data.map((d) => d.price)
  const minPrice = Math.min(...prices)
  const maxPrice = Math.max(...prices)
  const currentPrice = prices[prices.length - 1]

  return (
    <div>
      <div className="flex gap-6 mb-4 text-sm">
        <div>
          <span className="text-gray-500">Current: </span>
          <span className="font-semibold text-blue-600">{formatPrice(currentPrice)}</span>
        </div>
        <div>
          <span className="text-gray-500">Lowest: </span>
          <span className="font-semibold text-green-600">{formatPrice(minPrice)}</span>
        </div>
        <div>
          <span className="text-gray-500">Highest: </span>
          <span className="font-semibold text-red-500">{formatPrice(maxPrice)}</span>
        </div>
      </div>
      <ResponsiveContainer width="100%" height={220}>
        <LineChart data={data} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis dataKey="date" tick={{ fontSize: 11 }} />
          <YAxis
            tick={{ fontSize: 11 }}
            tickFormatter={(v) => `${v} RON`}
            domain={['auto', 'auto']}
          />
          <Tooltip formatter={(value) => [formatPrice(value), 'Price']} />
          {minPrice !== maxPrice && (
            <ReferenceLine y={minPrice} stroke="#16a34a" strokeDasharray="4 4"
              label={{ value: 'Lowest', fill: '#16a34a', fontSize: 10 }} />
          )}
          <Line
            type="monotone"
            dataKey="price"
            stroke="#2563eb"
            strokeWidth={2}
            dot={{ r: 3, fill: '#2563eb' }}
            activeDot={{ r: 5 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
