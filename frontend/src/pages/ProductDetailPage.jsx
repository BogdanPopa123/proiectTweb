import React, { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { productsApi, priceHistoryApi, alertsApi, productGroupsApi } from '../services/api'
import { useAuth } from '../context/AuthContext'
import PriceHistoryChart from '../components/PriceHistoryChart'

export default function ProductDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const [product, setProduct] = useState(null)
  const [history, setHistory] = useState([])
  const [group, setGroup] = useState(null)
  const [loading, setLoading] = useState(true)
  const [alertForm, setAlertForm] = useState({ targetPrice: '' })
  const [alertMsg, setAlertMsg] = useState('')
  const [alertLoading, setAlertLoading] = useState(false)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [prodRes, histRes] = await Promise.all([
          productsApi.getById(id),
          priceHistoryApi.getByProduct(id),
        ])
        setProduct(prodRes.data)
        setHistory(histRes.data)

        // Try loading product group for cross-site comparison
        try {
          const groupsRes = await productGroupsApi.getAll({ page: 0, size: 100 })
          const matchingGroup = groupsRes.data.content?.find(g =>
            g.products?.some(p => p.id === parseInt(id))
          )
          if (matchingGroup) setGroup(matchingGroup)
        } catch {}
      } catch {
        setProduct(null)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  const handleCreateAlert = async (e) => {
    e.preventDefault()
    setAlertMsg('')
    setAlertLoading(true)
    try {
      await alertsApi.create({
        productId: parseInt(id),
        targetPrice: alertForm.targetPrice ? parseFloat(alertForm.targetPrice) : null,
      })
      setAlertMsg('Alert created! You\'ll be notified when the price drops.')
      setAlertForm({ targetPrice: '' })
    } catch (err) {
      setAlertMsg(err.response?.data?.message || 'Failed to create alert')
    } finally {
      setAlertLoading(false)
    }
  }

  if (loading) return <div className="flex justify-center py-20"><div className="animate-spin h-8 w-8 border-b-2 border-blue-600 rounded-full" /></div>
  if (!product) return <div className="text-center py-20 text-gray-500">Product not found</div>

  const minPrice = history.length > 0 ? Math.min(...history.map(h => parseFloat(h.price))) : null
  const savings = product.currentPrice && minPrice && parseFloat(product.currentPrice) > minPrice
    ? (parseFloat(product.currentPrice) - minPrice).toFixed(2) : null

  return (
    <div className="max-w-4xl mx-auto">
      <Link to="/products" className="text-blue-600 hover:underline text-sm mb-4 inline-block">← Back to Products</Link>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        {/* Product Image */}
        <div className="card flex items-center justify-center p-4 bg-white min-h-64">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} className="max-h-64 object-contain" />
          ) : (
            <div className="text-gray-300 text-6xl">📦</div>
          )}
        </div>

        {/* Product Info */}
        <div className="card">
          <div className="mb-2">
            {product.category && <span className="badge-blue mb-2">{product.category}</span>}
            <span className={`badge ml-2 ${product.active ? 'badge-green' : 'badge-red'}`}>
              {product.active ? 'Available' : 'Unavailable'}
            </span>
          </div>
          <h1 className="text-xl font-bold text-gray-900 mb-2">{product.name}</h1>

          <div className="text-3xl font-bold text-blue-600 mb-1">
            {product.currentPrice ? `${parseFloat(product.currentPrice).toFixed(2)} ${product.currency}` : 'N/A'}
          </div>

          {minPrice && savings && (
            <p className="text-sm text-green-600 mb-3">
              Historical low: {minPrice.toFixed(2)} RON
              <span className="ml-2 badge-green">Save {savings} RON now!</span>
            </p>
          )}

          <div className="text-sm text-gray-600 space-y-1 mb-4">
            <p><span className="font-medium">Store:</span> {product.storeName}</p>
            {product.description && <p className="text-gray-500 text-xs mt-2">{product.description}</p>}
          </div>

          <a
            href={product.productUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-primary w-full mb-3"
          >
            View on {product.storeName} ↗
          </a>

          {user ? (
            <div className="border-t pt-4">
              <h3 className="font-medium text-gray-900 mb-2">Set Price Alert</h3>
              {alertMsg && (
                <div className={`mb-2 p-2 rounded text-sm ${alertMsg.includes('created') ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
                  {alertMsg}
                </div>
              )}
              <form onSubmit={handleCreateAlert} className="flex gap-2">
                <input
                  type="number" step="0.01" min="0"
                  value={alertForm.targetPrice}
                  onChange={e => setAlertForm({ targetPrice: e.target.value })}
                  placeholder="Target price (optional)"
                  className="input flex-1"
                />
                <button type="submit" className="btn btn-secondary" disabled={alertLoading}>
                  {alertLoading ? '...' : 'Alert me'}
                </button>
              </form>
              <p className="text-xs text-gray-400 mt-1">Leave empty to be notified on any price drop</p>
            </div>
          ) : (
            <p className="text-sm text-gray-500 border-t pt-3">
              <Link to="/login" className="text-blue-600 hover:underline">Sign in</Link> to set price alerts
            </p>
          )}
        </div>
      </div>

      {/* Price History Chart */}
      <div className="card mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Price History</h2>
        <PriceHistoryChart history={history} />
      </div>

      {/* Cross-site price comparison */}
      {group && group.products && group.products.length > 1 && (
        <div className="card">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Compare Prices Across Stores</h2>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="table-header">Store</th>
                  <th className="table-header">Product</th>
                  <th className="table-header">Price</th>
                  <th className="table-header">Link</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {[...group.products]
                  .sort((a, b) => (a.currentPrice || 999999) - (b.currentPrice || 999999))
                  .map((p, idx) => (
                    <tr key={p.id} className={p.id === parseInt(id) ? 'bg-blue-50' : ''}>
                      <td className="table-cell font-medium">{p.storeName}</td>
                      <td className="table-cell text-sm">{p.name}</td>
                      <td className="table-cell font-bold text-blue-600">
                        {p.currentPrice ? `${parseFloat(p.currentPrice).toFixed(2)} ${p.currency}` : '—'}
                        {idx === 0 && <span className="ml-2 badge-green">Best Price</span>}
                      </td>
                      <td className="table-cell">
                        <Link to={`/products/${p.id}`} className="text-blue-600 hover:underline text-sm">Details</Link>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
