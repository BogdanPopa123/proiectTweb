import React, { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, logout, isAdmin } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navLinkClass = ({ isActive }) =>
    `px-3 py-2 rounded-md text-sm font-medium transition-colors ${
      isActive
        ? 'bg-blue-700 text-white'
        : 'text-blue-100 hover:bg-blue-700 hover:text-white'
    }`

  return (
    <nav className="bg-blue-600 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/products" className="flex items-center gap-2">
            <span className="text-2xl">💰</span>
            <span className="text-white font-bold text-lg tracking-tight">PriceCompare</span>
          </Link>

          {/* Desktop Nav */}
          <div className="hidden md:flex items-center gap-1">
            <NavLink to="/products" className={navLinkClass}>Products</NavLink>
            <NavLink to="/stores" className={navLinkClass}>Stores</NavLink>
            <NavLink to="/visual-search" className={navLinkClass}>Visual Search</NavLink>
            {user && <NavLink to="/alerts" className={navLinkClass}>My Alerts</NavLink>}
            <NavLink to="/feedback" className={navLinkClass}>Feedback</NavLink>
          </div>

          {/* Auth buttons */}
          <div className="hidden md:flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-3">
                <span className="text-blue-100 text-sm">
                  {isAdmin && <span className="badge-blue mr-1">ADMIN</span>}
                  {user.username}
                </span>
                <button onClick={handleLogout} className="btn btn-sm bg-blue-700 text-white hover:bg-blue-800 border-blue-500">
                  Logout
                </button>
              </div>
            ) : (
              <>
                <Link to="/login" className="btn btn-sm bg-transparent text-blue-100 border border-blue-400 hover:bg-blue-700">Login</Link>
                <Link to="/register" className="btn btn-sm bg-white text-blue-600 hover:bg-blue-50">Register</Link>
              </>
            )}
          </div>

          {/* Mobile menu button */}
          <button
            className="md:hidden text-white"
            onClick={() => setMenuOpen(!menuOpen)}
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              {menuOpen
                ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              }
            </svg>
          </button>
        </div>

        {/* Mobile menu */}
        {menuOpen && (
          <div className="md:hidden pb-3 space-y-1">
            <NavLink to="/products" className={navLinkClass} onClick={() => setMenuOpen(false)}>Products</NavLink>
            <NavLink to="/stores" className={navLinkClass} onClick={() => setMenuOpen(false)}>Stores</NavLink>
            <NavLink to="/visual-search" className={navLinkClass} onClick={() => setMenuOpen(false)}>Visual Search</NavLink>
            {user && <NavLink to="/alerts" className={navLinkClass} onClick={() => setMenuOpen(false)}>My Alerts</NavLink>}
            <NavLink to="/feedback" className={navLinkClass} onClick={() => setMenuOpen(false)}>Feedback</NavLink>
            {user ? (
              <button onClick={handleLogout} className="w-full text-left px-3 py-2 text-sm text-blue-100 hover:bg-blue-700 rounded-md">
                Logout ({user.username})
              </button>
            ) : (
              <>
                <Link to="/login" className="block px-3 py-2 text-sm text-blue-100" onClick={() => setMenuOpen(false)}>Login</Link>
                <Link to="/register" className="block px-3 py-2 text-sm text-blue-100" onClick={() => setMenuOpen(false)}>Register</Link>
              </>
            )}
          </div>
        )}
      </div>
    </nav>
  )
}
