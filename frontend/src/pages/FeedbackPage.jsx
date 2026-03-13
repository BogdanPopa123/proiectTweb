import React, { useState } from 'react'
import { feedbackApi } from '../services/api'
import { useAuth } from '../context/AuthContext'

const CATEGORIES = [
  { value: 'GENERAL', label: 'General Feedback' },
  { value: 'BUG_REPORT', label: 'Bug Report' },
  { value: 'FEATURE_REQUEST', label: 'Feature Request' },
  { value: 'SCRAPER_ISSUE', label: 'Scraper / Data Issue' },
  { value: 'PRICE_ERROR', label: 'Price Error' },
]

const RATINGS = [1, 2, 3, 4, 5]
const RATING_LABELS = { 1: 'Very Poor', 2: 'Poor', 3: 'Average', 4: 'Good', 5: 'Excellent' }

export default function FeedbackPage() {
  const { user } = useAuth()
  const [form, setForm] = useState({
    category: '',
    rating: null,
    subscribeNewsletter: false,
    message: '',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)

  const validate = () => {
    const e = {}
    if (!form.category) e.category = 'Please select a category'
    if (!form.rating) e.rating = 'Please select a rating'
    if (!form.message || form.message.trim().length < 10) e.message = 'Message must be at least 10 characters'
    return e
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const v = validate()
    if (Object.keys(v).length > 0) { setErrors(v); return }
    setErrors({})
    setLoading(true)
    try {
      await feedbackApi.submit(form)
      setSuccess(true)
      setForm({ category: '', rating: null, subscribeNewsletter: false, message: '' })
    } catch (err) {
      setErrors({ general: err.response?.data?.message || 'Submission failed. Please try again.' })
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <div className="max-w-lg mx-auto mt-12">
        <div className="card text-center">
          <div className="text-5xl mb-4">🎉</div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Thank you for your feedback!</h2>
          <p className="text-gray-600 mb-4">Your input helps us improve PriceCompare.</p>
          <button onClick={() => setSuccess(false)} className="btn btn-primary">Send Another</button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-lg mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Share Your Feedback</h1>
        <p className="text-gray-600 text-sm mt-1">Help us improve PriceCompare with your thoughts.</p>
        {!user && <p className="text-sm text-blue-600 mt-1">You're submitting as a guest.</p>}
      </div>

      <div className="card">
        {errors.general && (
          <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded">{errors.general}</div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* SELECT — Category */}
          <div>
            <label className="label">Category *</label>
            <select
              value={form.category}
              onChange={e => { setForm({...form, category: e.target.value}); setErrors({...errors, category: ''}) }}
              className={`input ${errors.category ? 'border-red-400' : ''}`}
            >
              <option value="">-- Select a category --</option>
              {CATEGORIES.map(c => (
                <option key={c.value} value={c.value}>{c.label}</option>
              ))}
            </select>
            {errors.category && <p className="text-xs text-red-600 mt-1">{errors.category}</p>}
          </div>

          {/* RADIO — Rating */}
          <div>
            <label className="label">Rating *</label>
            <div className="flex gap-3 flex-wrap">
              {RATINGS.map(r => (
                <label key={r} className="flex flex-col items-center cursor-pointer group">
                  <input
                    type="radio"
                    name="rating"
                    value={r}
                    checked={form.rating === r}
                    onChange={() => { setForm({...form, rating: r}); setErrors({...errors, rating: ''}) }}
                    className="sr-only"
                  />
                  <div className={`h-10 w-10 rounded-full border-2 flex items-center justify-center text-lg transition-all
                    ${form.rating === r
                      ? 'border-blue-500 bg-blue-50 text-blue-600 scale-110'
                      : 'border-gray-200 hover:border-blue-300 text-gray-400'}`}>
                    {r === 1 ? '😞' : r === 2 ? '😕' : r === 3 ? '😐' : r === 4 ? '😊' : '😁'}
                  </div>
                  <span className={`text-xs mt-1 ${form.rating === r ? 'text-blue-600 font-medium' : 'text-gray-400'}`}>
                    {r}
                  </span>
                </label>
              ))}
            </div>
            {form.rating && (
              <p className="text-sm text-gray-600 mt-1">{RATING_LABELS[form.rating]}</p>
            )}
            {errors.rating && <p className="text-xs text-red-600 mt-1">{errors.rating}</p>}
          </div>

          {/* TEXTAREA — Message */}
          <div>
            <label className="label">Your Feedback *</label>
            <textarea
              value={form.message}
              onChange={e => { setForm({...form, message: e.target.value}); setErrors({...errors, message: ''}) }}
              rows={5}
              placeholder="Tell us what you think, report a bug, or suggest a feature..."
              className={`input resize-none ${errors.message ? 'border-red-400' : ''}`}
            />
            <div className="flex justify-between mt-1">
              {errors.message
                ? <p className="text-xs text-red-600">{errors.message}</p>
                : <span />}
              <span className="text-xs text-gray-400">{form.message.length}/2000</span>
            </div>
          </div>

          {/* CHECKBOX — Newsletter */}
          <div className="flex items-start gap-3 bg-gray-50 p-4 rounded-lg">
            <input
              type="checkbox"
              id="newsletter"
              checked={form.subscribeNewsletter}
              onChange={e => setForm({...form, subscribeNewsletter: e.target.checked})}
              className="mt-0.5 h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
            />
            <div>
              <label htmlFor="newsletter" className="text-sm font-medium text-gray-700 cursor-pointer">
                Subscribe to price alerts newsletter
              </label>
              <p className="text-xs text-gray-500 mt-0.5">
                Get weekly summaries of the best deals and price drops.
              </p>
            </div>
          </div>

          <button type="submit" className="btn btn-primary w-full" disabled={loading}>
            {loading ? 'Submitting...' : 'Submit Feedback'}
          </button>
        </form>
      </div>
    </div>
  )
}
