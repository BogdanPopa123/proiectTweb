import React, { useState, useRef } from 'react'
import { Link } from 'react-router-dom'
import { visualSearchApi } from '../services/api'

const TABS = [
  { id: 'ai', label: '🤖 AI Search', description: 'Gemini identifies the product → scrapes all stores live' },
  { id: 'phash', label: '🔍 Visual Match', description: 'pHash similarity against locally stored products' },
]

const storeColors = {
  'eMAG':  'bg-yellow-100 text-yellow-800 border-yellow-200',
  'Altex': 'bg-blue-100 text-blue-800 border-blue-200',
  'Cel.ro':'bg-green-100 text-green-800 border-green-200',
}

export default function VisualSearchPage() {
  const [activeTab, setActiveTab] = useState('ai')
  const [preview, setPreview] = useState(null)
  const [file, setFile] = useState(null)
  const [results, setResults] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [dragging, setDragging] = useState(false)
  const fileRef = useRef()

  const handleFile = (f) => {
    if (!f || !f.type.startsWith('image/')) {
      setError('Please select an image file')
      return
    }
    setError('')
    setFile(f)
    setResults(null)
    const reader = new FileReader()
    reader.onload = (e) => setPreview(e.target.result)
    reader.readAsDataURL(f)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    setDragging(false)
    const f = e.dataTransfer.files[0]
    if (f) handleFile(f)
  }

  const handleTabChange = (tab) => {
    setActiveTab(tab)
    setResults(null)
    setError('')
  }

  const handleSearch = async () => {
    if (!file) return
    setLoading(true)
    setError('')
    setResults(null)
    try {
      const formData = new FormData()
      formData.append('image', file)
      const res = activeTab === 'ai'
        ? await visualSearchApi.aiSearch(formData)
        : await visualSearchApi.search(formData)
      setResults({ type: activeTab, data: res.data })
    } catch (err) {
      setError(err.response?.data?.message || 'Search failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const getSimilarityColor = (score) => {
    if (score >= 0.9) return 'badge-green'
    if (score >= 0.7) return 'badge-blue'
    return 'badge-gray'
  }

  return (
    <div className="max-w-5xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Visual Product Search</h1>
        <p className="mt-1 text-gray-600">
          Upload a product image to find it across Romanian online stores with live price comparison.
        </p>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 border-b border-gray-200">
        {TABS.map(tab => (
          <button
            key={tab.id}
            onClick={() => handleTabChange(tab.id)}
            className={`px-4 py-2 text-sm font-medium rounded-t-lg border-b-2 transition-colors
              ${activeTab === tab.id
                ? 'border-blue-600 text-blue-600 bg-blue-50'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* ── Upload panel ── */}
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-1">Upload Image</h2>
          <p className="text-xs text-gray-500 mb-4">
            {TABS.find(t => t.id === activeTab)?.description}
          </p>

          <div
            className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors cursor-pointer
              ${dragging ? 'border-blue-400 bg-blue-50' : 'border-gray-300 hover:border-blue-400'}`}
            onClick={() => fileRef.current?.click()}
            onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
            onDragLeave={() => setDragging(false)}
            onDrop={handleDrop}
          >
            {preview ? (
              <img src={preview} alt="Preview" className="max-h-48 mx-auto object-contain rounded" />
            ) : (
              <div className="text-gray-400">
                <div className="text-4xl mb-2">📷</div>
                <p className="text-sm font-medium">Drag & drop or click to upload</p>
                <p className="text-xs mt-1">JPEG, PNG, WebP, GIF — max 10MB</p>
              </div>
            )}
          </div>

          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => handleFile(e.target.files?.[0])}
          />

          {file && (
            <p className="mt-2 text-xs text-gray-500 truncate">Selected: {file.name}</p>
          )}

          {error && (
            <div className="mt-3 p-2 bg-red-50 text-red-700 text-sm rounded">{error}</div>
          )}

          <button
            onClick={handleSearch}
            disabled={!file || loading}
            className="btn btn-primary w-full mt-4 disabled:opacity-50"
          >
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
                {activeTab === 'ai' ? 'Identifying & scraping...' : 'Searching...'}
              </span>
            ) : activeTab === 'ai' ? '🤖 AI Search' : '🔍 Find Similar Products'}
          </button>

          {/* How it works */}
          <div className={`mt-4 p-3 rounded-md ${activeTab === 'ai' ? 'bg-purple-50' : 'bg-blue-50'}`}>
            <h3 className={`text-xs font-medium mb-1 ${activeTab === 'ai' ? 'text-purple-800' : 'text-blue-800'}`}>
              How it works
            </h3>
            {activeTab === 'ai' ? (
              <ol className="text-xs text-purple-700 space-y-1 list-decimal list-inside">
                <li>Gemini Vision AI identifies the product from your image</li>
                <li>Uses the product name to search eMAG, Altex & Cel.ro live</li>
                <li>Returns fresh prices from all 3 stores for comparison</li>
              </ol>
            ) : (
              <p className="text-xs text-blue-700">
                Uses DCT-based perceptual hashing (pHash) to find visually similar products
                stored in the local database. Lower hamming distance = more similar.
              </p>
            )}
          </div>
        </div>

        {/* ── Results panel ── */}
        <div>
          {results === null && !loading && (
            <div className="card flex flex-col items-center justify-center h-64 text-gray-400">
              <div className="text-5xl mb-3">{activeTab === 'ai' ? '🤖' : '🔍'}</div>
              <p className="text-sm">Upload an image to start searching</p>
            </div>
          )}

          {loading && (
            <div className="card flex flex-col items-center justify-center h-64">
              <div className="animate-spin h-8 w-8 border-b-2 border-blue-600 rounded-full mb-4" />
              <p className="text-sm text-gray-600 font-medium">
                {activeTab === 'ai' ? 'Asking Gemini AI...' : 'Computing image hash...'}
              </p>
              {activeTab === 'ai' && (
                <p className="text-xs text-gray-400 mt-1">This may take 5–10 seconds</p>
              )}
            </div>
          )}

          {/* AI results */}
          {results?.type === 'ai' && !loading && (
            <div className="space-y-4">
              {/* Identified product banner */}
              <div className="card bg-purple-50 border border-purple-200 p-4">
                <p className="text-xs text-purple-600 font-medium uppercase tracking-wide mb-1">
                  Gemini identified
                </p>
                <p className="text-lg font-bold text-purple-900">{results.data.identifiedProduct}</p>
                <p className="text-xs text-purple-600 mt-1">
                  {results.data.totalResults} result{results.data.totalResults !== 1 ? 's' : ''} found
                  across {Object.keys(results.data.resultsByStore).length} store{Object.keys(results.data.resultsByStore).length !== 1 ? 's' : ''}
                </p>
              </div>

              {results.data.totalResults === 0 ? (
                <div className="card text-center py-8 text-gray-400">
                  <div className="text-4xl mb-2">😕</div>
                  <p className="text-sm">No results found across stores</p>
                  <p className="text-xs mt-1">The stores may not carry this product right now</p>
                </div>
              ) : (
                Object.entries(results.data.resultsByStore).map(([storeName, storeResults]) => (
                  <div key={storeName} className="card p-0 overflow-hidden">
                    <div className={`px-4 py-2 border-b flex items-center justify-between ${storeColors[storeName] || 'bg-gray-100 text-gray-700 border-gray-200'}`}>
                      <span className="font-semibold text-sm">{storeName}</span>
                      <span className="text-xs opacity-75">{storeResults.length} result{storeResults.length !== 1 ? 's' : ''}</span>
                    </div>
                    <div className="divide-y divide-gray-100">
                      {storeResults.map((product, idx) => (
                        <div key={idx} className="flex gap-3 p-3 hover:bg-gray-50 transition-colors">
                          {product.imageUrl && (
                            <img
                              src={product.imageUrl}
                              alt={product.name}
                              className="h-14 w-14 object-contain rounded flex-shrink-0 bg-white border border-gray-100 p-1"
                              onError={e => { e.target.style.display = 'none' }}
                            />
                          )}
                          <div className="flex-1 min-w-0">
                            <a
                              href={product.productUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-sm font-medium text-blue-600 hover:underline line-clamp-2"
                            >
                              {product.name}
                            </a>
                            {product.price && (
                              <p className="text-sm font-bold text-gray-900 mt-0.5">
                                {product.price} <span className="font-normal text-gray-500">{product.currency}</span>
                              </p>
                            )}
                          </div>
                          <a
                            href={product.productUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-xs bg-blue-600 text-white px-2 py-1 rounded hover:bg-blue-700 self-center flex-shrink-0"
                          >
                            View
                          </a>
                        </div>
                      ))}
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* pHash results */}
          {results?.type === 'phash' && !loading && (
            <div>
              <h2 className="font-semibold text-gray-900 mb-3">
                Found {results.data.totalResults} similar product{results.data.totalResults !== 1 ? 's' : ''}
              </h2>

              {results.data.totalResults === 0 ? (
                <div className="card text-center py-8 text-gray-400">
                  <div className="text-4xl mb-2">😕</div>
                  <p className="text-sm">No visually similar products found in the local database</p>
                  <p className="text-xs mt-1">Try the 🤖 AI Search tab for live results</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {results.data.results.map((result) => (
                    <div key={result.product.id} className="card flex gap-4 p-4">
                      {result.product.imageUrl && (
                        <img
                          src={result.product.imageUrl}
                          alt={result.product.name}
                          className="h-16 w-16 object-cover rounded flex-shrink-0"
                          onError={e => { e.target.style.display = 'none' }}
                        />
                      )}
                      <div className="flex-1 min-w-0">
                        <Link to={`/products/${result.product.id}`} className="font-medium text-blue-600 hover:underline line-clamp-2 text-sm">
                          {result.product.name}
                        </Link>
                        <p className="text-xs text-gray-500">{result.product.storeName}</p>
                        <div className="flex items-center gap-2 mt-1">
                          <span className="text-blue-600 font-semibold text-sm">
                            {result.product.currentPrice
                              ? `${parseFloat(result.product.currentPrice).toFixed(2)} ${result.product.currency}`
                              : 'N/A'}
                          </span>
                          <span className={getSimilarityColor(result.similarityScore)}>
                            {Math.round(result.similarityScore * 100)}% similar
                          </span>
                          <span className="badge-gray text-xs">Δ{result.hammingDistance} bits</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
