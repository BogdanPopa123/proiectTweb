import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Defined outside the parent component to prevent remounting on every render
function Field({ name, label, type = 'text', placeholder, value, onChange, error }) {
  return (
    <div>
      <label className="label">{label}</label>
      <input
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        className={`input ${error ? 'border-red-400 focus:ring-red-400' : ''}`}
        placeholder={placeholder}
        required
      />
      {error && (
        <p className="mt-1 text-xs text-red-600">{error}</p>
      )}
    </div>
  )
}

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    fullName: '', username: '', email: '', password: '', confirmPassword: ''
  })
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
    setFieldErrors({ ...fieldErrors, [e.target.name]: '' })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setFieldErrors({})

    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match' })
      return
    }

    setLoading(true)
    try {
      await register(form)
      navigate('/login', { state: { message: 'Account created! Please sign in.' } })
    } catch (err) {
      const data = err.response?.data
      if (data?.errors) {
        setFieldErrors(data.errors)
      } else {
        setError(data?.message || 'Registration failed. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-md mx-auto mt-8">
      <div className="card">
        <div className="text-center mb-6">
          <span className="text-4xl">💰</span>
          <h1 className="mt-2 text-2xl font-bold text-gray-900">Create account</h1>
          <p className="mt-1 text-sm text-gray-500">Join PriceCompare today</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <Field name="fullName" label="Full Name" placeholder="John Doe" value={form.fullName} onChange={handleChange} error={fieldErrors.fullName} />
          <Field name="username" label="Username" placeholder="johndoe" value={form.username} onChange={handleChange} error={fieldErrors.username} />
          <Field name="email" label="Email Address" type="email" placeholder="john@example.com" value={form.email} onChange={handleChange} error={fieldErrors.email} />
          <Field name="password" label="Password" type="password" placeholder="Min 6 characters" value={form.password} onChange={handleChange} error={fieldErrors.password} />
          <Field name="confirmPassword" label="Confirm Password" type="password" placeholder="Repeat password" value={form.confirmPassword} onChange={handleChange} error={fieldErrors.confirmPassword} />

          <button type="submit" className="btn btn-primary w-full" disabled={loading}>
            {loading ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-600 hover:underline font-medium">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
