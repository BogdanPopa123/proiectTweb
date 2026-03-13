import React, { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { alertsApi } from '../services/api'
import Pagination from '../components/Pagination'
import ConfirmDialog from '../components/ConfirmDialog'

export default function AlertsPage() {
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const [toggleLoading, setToggleLoading] = useState(null)

  const loadAlerts = useCallback(async () => {
    setLoading(true)
    try {
      const res = await alertsApi.getAll({ page, size: 15 })
      setAlerts(res.data.content)
      setTotalPages(res.data.totalPages)
    } catch {/* silent */} finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => { loadAlerts() }, [loadAlerts])

  const handleToggle = async (alert) => {
    setToggleLoading(alert.id)
    try {
      await alertsApi.update(alert.id, {
        productId: alert.productId,
        enabled: !alert.enabled,
        targetPrice: alert.targetPrice,
      })
      loadAlerts()
    } catch {/* silent */} finally {
      setToggleLoading(null)
    }
  }

  const handleDelete = async () => {
    setDeleteLoading(true)
    try {
      await alertsApi.delete(deleteTarget.id)
      setDeleteTarget(null)
      loadAlerts()
    } catch {
      setDeleteTarget(null)
    } finally {
      setDeleteLoading(false)
    }
  }

  const formatDate = (d) => d ? new Date(d).toLocaleDateString('ro-RO', { year: 'numeric', month: 'short', day: 'numeric' }) : '—'
  const formatPrice = (p) => p ? `${parseFloat(p).toFixed(2)} RON` : 'Any drop'

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Price Alerts</h1>
          <p className="text-gray-500 text-sm mt-1">
            Get notified by email when your tracked products drop in price.
          </p>
        </div>
        <Link to="/products" className="btn btn-secondary">+ Track a Product</Link>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="table-header">Product</th>
                <th className="table-header">Store</th>
                <th className="table-header">Current Price</th>
                <th className="table-header">Target Price</th>
                <th className="table-header">Status</th>
                <th className="table-header">Created</th>
                <th className="table-header">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loading ? (
                <tr><td colSpan={7} className="table-cell text-center py-8 text-gray-400">Loading...</td></tr>
              ) : alerts.length === 0 ? (
                <tr>
                  <td colSpan={7} className="table-cell text-center py-12">
                    <div className="text-gray-400">
                      <div className="text-4xl mb-2">🔔</div>
                      <p className="font-medium">No alerts yet</p>
                      <p className="text-sm mt-1">
                        Browse <Link to="/products" className="text-blue-600 hover:underline">products</Link> and click "Alert me" to track price drops.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : alerts.map(alert => (
                <tr key={alert.id} className="hover:bg-gray-50">
                  <td className="table-cell">
                    <div className="flex items-center gap-2">
                      {alert.productImageUrl && (
                        <img src={alert.productImageUrl} alt="" className="h-8 w-8 object-cover rounded" onError={e => e.target.style.display='none'} />
                      )}
                      <Link to={`/products/${alert.productId}`} className="text-blue-600 hover:underline font-medium text-sm line-clamp-1">
                        {alert.productName}
                      </Link>
                    </div>
                  </td>
                  <td className="table-cell text-sm">{alert.storeName || '—'}</td>
                  <td className="table-cell font-semibold text-blue-600">{formatPrice(alert.currentPrice)}</td>
                  <td className="table-cell text-sm text-gray-700">{formatPrice(alert.targetPrice)}</td>
                  <td className="table-cell">
                    <div className="flex items-center gap-2">
                      <span className={alert.enabled ? 'badge-green' : 'badge-gray'}>
                        {alert.enabled ? 'Active' : 'Paused'}
                      </span>
                      {alert.triggeredAt && (
                        <span className="badge-blue text-xs">Triggered</span>
                      )}
                    </div>
                  </td>
                  <td className="table-cell text-sm text-gray-500">{formatDate(alert.createdAt)}</td>
                  <td className="table-cell">
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleToggle(alert)}
                        disabled={toggleLoading === alert.id}
                        className="btn btn-secondary btn-sm"
                      >
                        {alert.enabled ? 'Pause' : 'Resume'}
                      </button>
                      <button onClick={() => setDeleteTarget(alert)} className="btn btn-danger btn-sm">Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete Alert"
        message={`Remove price alert for "${deleteTarget?.productName}"?`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteLoading}
      />
    </div>
  )
}
