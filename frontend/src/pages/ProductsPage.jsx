import React, { useState, useEffect, useCallback } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { productsApi, storesApi } from '../services/api'
import { useAuth } from '../context/AuthContext'
import Pagination from '../components/Pagination'
import ConfirmDialog from '../components/ConfirmDialog'

const CATEGORY_OPTIONS = ['', 'Laptop', 'Phone', 'Electronics']

export default function ProductsPage() {
  const { isAdmin } = useAuth()
  const navigate = useNavigate()
  const [products, setProducts] = useState([])
  const [stores, setStores] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editProduct, setEditProduct] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const [form, setForm] = useState({ name: '', productUrl: '', imageUrl: '', category: '', storeId: '', currentPrice: '', currency: 'RON', active: true })
  const [formError, setFormError] = useState('')
  const [formLoading, setFormLoading] = useState(false)

  const loadProducts = useCallback(async () => {
    setLoading(true)
    try {
      const res = await productsApi.getAll({ page, size: 15, search: search || undefined })
      setProducts(res.data.content)
      setTotalPages(res.data.totalPages)
    } catch {
      setError('Failed to load products')
    } finally {
      setLoading(false)
    }
  }, [page, search])

  useEffect(() => { loadProducts() }, [loadProducts])
  useEffect(() => {
    storesApi.getActive().then(r => setStores(r.data)).catch(() => {})
  }, [])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(0)
    setSearch(searchInput)
  }

  const openCreate = () => {
    setEditProduct(null)
    setForm({ name: '', productUrl: '', imageUrl: '', category: '', storeId: '', currentPrice: '', currency: 'RON', active: true })
    setFormError('')
    setShowForm(true)
  }

  const openEdit = (p) => {
    setEditProduct(p)
    setForm({
      name: p.name, productUrl: p.productUrl, imageUrl: p.imageUrl || '',
      category: p.category || '', storeId: p.storeId || '',
      currentPrice: p.currentPrice || '', currency: p.currency || 'RON', active: p.active
    })
    setFormError('')
    setShowForm(true)
  }

  const handleFormSubmit = async (e) => {
    e.preventDefault()
    setFormError('')
    setFormLoading(true)
    try {
      const payload = { ...form, currentPrice: form.currentPrice ? parseFloat(form.currentPrice) : null }
      if (editProduct) {
        await productsApi.update(editProduct.id, payload)
      } else {
        await productsApi.create(payload)
      }
      setShowForm(false)
      loadProducts()
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to save product')
    } finally {
      setFormLoading(false)
    }
  }

  const handleDelete = async () => {
    setDeleteLoading(true)
    try {
      await productsApi.delete(deleteTarget.id)
      setDeleteTarget(null)
      loadProducts()
    } catch {
      setDeleteTarget(null)
    } finally {
      setDeleteLoading(false)
    }
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Products</h1>
        <div className="flex gap-3">
          <form onSubmit={handleSearch} className="flex gap-2">
            <input
              type="text" value={searchInput} onChange={e => setSearchInput(e.target.value)}
              placeholder="Search products..." className="input w-48"
            />
            <button type="submit" className="btn btn-secondary">Search</button>
            {search && <button type="button" onClick={() => { setSearch(''); setSearchInput(''); setPage(0) }} className="btn btn-secondary">Clear</button>}
          </form>
          {isAdmin && (
            <button onClick={openCreate} className="btn btn-primary">+ Add Product</button>
          )}
        </div>
      </div>

      {error && <div className="p-3 bg-red-50 text-red-700 rounded-md mb-4">{error}</div>}

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="table-header">Product</th>
                <th className="table-header">Store</th>
                <th className="table-header">Category</th>
                <th className="table-header">Price</th>
                <th className="table-header">Status</th>
                {isAdmin && <th className="table-header">Actions</th>}
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loading ? (
                <tr><td colSpan={isAdmin ? 6 : 5} className="table-cell text-center py-8 text-gray-400">Loading...</td></tr>
              ) : products.length === 0 ? (
                <tr><td colSpan={isAdmin ? 6 : 5} className="table-cell text-center py-8 text-gray-400">No products found</td></tr>
              ) : products.map((product) => (
                <tr key={product.id} className="hover:bg-gray-50">
                  <td className="table-cell">
                    <div className="flex items-center gap-3">
                      {product.imageUrl && (
                        <img src={product.imageUrl} alt="" className="h-10 w-10 object-cover rounded" onError={e => e.target.style.display='none'} />
                      )}
                      <div>
                        <Link to={`/products/${product.id}`} className="font-medium text-blue-600 hover:underline line-clamp-1">
                          {product.name}
                        </Link>
                        <a href={product.productUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-gray-400 hover:underline">
                          View on site ↗
                        </a>
                      </div>
                    </div>
                  </td>
                  <td className="table-cell">{product.storeName || '—'}</td>
                  <td className="table-cell">
                    {product.category ? <span className="badge-blue">{product.category}</span> : '—'}
                  </td>
                  <td className="table-cell font-semibold">
                    {product.currentPrice ? `${parseFloat(product.currentPrice).toFixed(2)} ${product.currency}` : '—'}
                  </td>
                  <td className="table-cell">
                    <span className={product.active ? 'badge-green' : 'badge-red'}>
                      {product.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {isAdmin && (
                    <td className="table-cell">
                      <div className="flex gap-2">
                        <button onClick={() => openEdit(product)} className="btn btn-secondary btn-sm">Edit</button>
                        <button onClick={() => setDeleteTarget(product)} className="btn btn-danger btn-sm">Delete</button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />

      {/* Add/Edit Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black bg-opacity-40" onClick={() => setShowForm(false)} />
          <div className="relative bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 p-6 z-10 max-h-screen overflow-y-auto">
            <h2 className="text-lg font-semibold mb-4">{editProduct ? 'Edit Product' : 'Add Product'}</h2>
            {formError && <div className="mb-3 p-2 bg-red-50 text-red-700 text-sm rounded">{formError}</div>}
            <form onSubmit={handleFormSubmit} className="space-y-3">
              <div>
                <label className="label">Name *</label>
                <input value={form.name} onChange={e => setForm({...form, name: e.target.value})} className="input" required />
              </div>
              <div>
                <label className="label">Product URL *</label>
                <input value={form.productUrl} onChange={e => setForm({...form, productUrl: e.target.value})} className="input" required />
              </div>
              <div>
                <label className="label">Image URL</label>
                <input value={form.imageUrl} onChange={e => setForm({...form, imageUrl: e.target.value})} className="input" />
              </div>
              <div>
                <label className="label">Store *</label>
                <select value={form.storeId} onChange={e => setForm({...form, storeId: e.target.value})} className="input" required>
                  <option value="">Select store...</option>
                  {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">Category</label>
                  <select value={form.category} onChange={e => setForm({...form, category: e.target.value})} className="input">
                    {CATEGORY_OPTIONS.map(c => <option key={c} value={c}>{c || 'All categories'}</option>)}
                  </select>
                </div>
                <div>
                  <label className="label">Price (RON)</label>
                  <input type="number" step="0.01" min="0" value={form.currentPrice} onChange={e => setForm({...form, currentPrice: e.target.value})} className="input" />
                </div>
              </div>
              <div className="flex items-center gap-2">
                <input type="checkbox" id="active" checked={form.active} onChange={e => setForm({...form, active: e.target.checked})} />
                <label htmlFor="active" className="text-sm text-gray-700">Active</label>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowForm(false)} className="btn btn-secondary flex-1">Cancel</button>
                <button type="submit" className="btn btn-primary flex-1" disabled={formLoading}>
                  {formLoading ? 'Saving...' : 'Save'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete Product"
        message={`Are you sure you want to delete "${deleteTarget?.name}"?`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteLoading}
      />
    </div>
  )
}
