import React from 'react'

export default function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null

  const pages = []
  const start = Math.max(0, page - 2)
  const end = Math.min(totalPages - 1, page + 2)

  for (let i = start; i <= end; i++) pages.push(i)

  return (
    <div className="flex items-center justify-center gap-1 mt-6">
      <button
        onClick={() => onPageChange(0)}
        disabled={page === 0}
        className="btn btn-secondary btn-sm disabled:opacity-40"
      >«</button>
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        className="btn btn-secondary btn-sm disabled:opacity-40"
      >‹</button>

      {start > 0 && (
        <>
          <button onClick={() => onPageChange(0)} className="btn btn-secondary btn-sm">1</button>
          {start > 1 && <span className="px-2 text-gray-500">…</span>}
        </>
      )}

      {pages.map((p) => (
        <button
          key={p}
          onClick={() => onPageChange(p)}
          className={`btn btn-sm ${p === page ? 'btn-primary' : 'btn-secondary'}`}
        >
          {p + 1}
        </button>
      ))}

      {end < totalPages - 1 && (
        <>
          {end < totalPages - 2 && <span className="px-2 text-gray-500">…</span>}
          <button onClick={() => onPageChange(totalPages - 1)} className="btn btn-secondary btn-sm">
            {totalPages}
          </button>
        </>
      )}

      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        className="btn btn-secondary btn-sm disabled:opacity-40"
      >›</button>
      <button
        onClick={() => onPageChange(totalPages - 1)}
        disabled={page >= totalPages - 1}
        className="btn btn-secondary btn-sm disabled:opacity-40"
      >»</button>
    </div>
  )
}
