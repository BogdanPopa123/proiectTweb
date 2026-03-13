import React, { useState, useEffect, useCallback } from 'react'
import { storesApi } from '../services/api'
import { useAuth } from '../context/AuthContext'
import Pagination from '../components/Pagination'
import ConfirmDialog from '../components/ConfirmDialog'

export default function StoresPage() {
  const { isAdmin } = useAuth()
  const [stores, setStores] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editStore, setEditStore] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const [form, setForm] = useState({ name: '', baseUrl: '', logoUrl: '', active: true })
  const [formError, setFormError] = useState('')
  const [formLoading, setFormLoading] = useState(false)

  const loadStores = useCallback(async () => {
    setLoading(true)
    try {
      const res = await storesApi.getAll({ page, size: 15, search: search || undefined })
      setStores(res.data.content)
      setTotalPages(res.data.totalPages)
    } catch {
      /* silent */
    } finally {
      setLoading(false)
    }
  }, [page, search])

  useEffect(() => { loadStores() }, [loadStores])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(0)
    setSearch(searchInput)
  }

  const openCreate = () => {
    setEditStore(null)
    setForm({ name: '', baseUrl: '', logoUrl: '', active: true })
    setFormError('')
    setShowForm(true)
  }

  const openEdit = (s) => {
    setEditStore(s)
    setForm({ name: s.name, baseUrl: s.baseUrl, logoUrl: s.logoUrl || '', active: s.active })
    setFormError('')
    setShowForm(true)
  }

  const handleFormSubmit = async (e) => {
    e.preventDefault()
    setFormError('')
    setFormLoading(true)
    try {
      if (editStore) {
        await storesApi.update(editStore.id, form)
      } else {
        await storesApi.create(form)
      }
      setShowForm(false)
      loadStores()
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to save store')
    } finally {
      setFormLoading(false)
    }
  }

  const handleDelete = async () => {
    setDeleteLoading(true)
    try {
      await storesApi.delete(deleteTarget.id)
      setDeleteTarget(null)
      loadStores()
    } catch {
      setDeleteTarget(null)
    } finally {
      setDeleteLoading(false)
    }
  }

  const formatDate = (d) => d ? new Date(d).toLocaleDateString('ro-RO') : '—'

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Stores</h1>
        <div className="flex gap-3">
          <form onSubmit={handleSearch} className="flex gap-2">
            <input type="text" value={searchInput} onChange={e => setSearchInput(e.target.value)}
              placeholder="Search stores..." className="input w-44" />
            <button type="submit" className="btn btn-secondary">Search</button>
            {search && <button type="button" onClick={() => { setSearch(''); setSearchInput(''); setPage(0) }} className="btn btn-secondary">Clear</button>}
          </form>
          {isAdmin && <button onClick={openCreate} className="btn btn-primary">+ Add Store</button>}
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="table-header">Logo</th>
                <th className="table-header">Store Name</th>
                <th className="table-header">Base URL</th>
                <th className="table-header">Status</th>
                <th className="table-header">Created</th>
                {isAdmin && <th className="table-header">Actions</th>}
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loading ? (
                <tr><td colSpan={isAdmin ? 6 : 5} className="table-cell text-center py-8 text-gray-400">Loading...</td></tr>
              ) : stores.length === 0 ? (
                <tr><td colSpan={isAdmin ? 6 : 5} className="table-cell text-center py-8 text-gray-400">No stores found</td></tr>
              ) : stores.map(store => (
                <tr key={store.id} className="hover:bg-gray-50">
                  <td className="table-cell">
                    {store.logoUrl
                      ? <img src={store.logoUrl} alt="" className="h-8 w-8 object-contain" onError={e => e.target.style.display='none'} />
                      : <div className="h-8 w-8 bg-gray-100 rounded flex items-center justify-center text-gray-400 text-xs">N/A</div>
                    }
                  </td>
                  <td className="table-cell font-medium">{store.name}</td>
                  <td className="table-cell">
                    <a href={store.baseUrl} target="_blank" rel="noopener noreferrer"
                      className="text-blue-600 hover:underline text-sm truncate max-w-xs block">
                      {store.baseUrl}
                    </a>
                  </td>
                  <td className="table-cell">
                    <span className={store.active ? 'badge-green' : 'badge-red'}>
                      {store.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="table-cell text-gray-500 text-sm">{formatDate(store.createdAt)}</td>
                  {isAdmin && (
                    <td className="table-cell">
                      <div className="flex gap-2">
                        <button onClick={() => openEdit(store)} className="btn btn-secondary btn-sm">Edit</button>
                        <button onClick={() => setDeleteTarget(store)} className="btn btn-danger btn-sm">Delete</button>
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

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black bg-opacity-40" onClick={() => setShowForm(false)} />
          <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6 z-10">
            <h2 className="text-lg font-semibold mb-4">{editStore ? 'Edit Store' : 'Add Store'}</h2>
            {formError && <div className="mb-3 p-2 bg-red-50 text-red-700 text-sm rounded">{formError}</div>}
            <form onSubmit={handleFormSubmit} className="space-y-3">
              <div>
                <label className="label">Name *</label>
                <input value={form.name} onChange={e => setForm({...form, name: e.target.value})} className="input" required />
              </div>
              <div>
                <label className="label">Base URL *</label>
                <input value={form.baseUrl} onChange={e => setForm({...form, baseUrl: e.target.value})} className="input" placeholder="https://example.ro" required />
              </div>
              <div>
                <label className="label">Logo URL</label>
                <input value={form.logoUrl} onChange={e => setForm({...form, logoUrl: e.target.value})} className="input" placeholder="https://..." />
              </div>
              <div className="flex items-center gap-2">
                <input type="checkbox" id="storeActive" checked={form.active} onChange={e => setForm({...form, active: e.target.checked})} />
                <label htmlFor="storeActive" className="text-sm text-gray-700">Active (enable scraping)</label>
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
        title="Delete Store"
        message={`Delete "${deleteTarget?.name}"? All associated products will also be deleted.`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteLoading}
      />
    </div>
  )
}
