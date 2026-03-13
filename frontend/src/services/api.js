import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Handle 401 responses
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// ─── Auth ────────────────────────────────────────────────────────────────────
export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  me: () => api.get('/auth/me'),
}

// ─── Products ────────────────────────────────────────────────────────────────
export const productsApi = {
  getAll: (params) => api.get('/products', { params }),
  getById: (id) => api.get(`/products/${id}`),
  getByStore: (storeId, params) => api.get(`/products/store/${storeId}`, { params }),
  create: (data) => api.post('/products', data),
  update: (id, data) => api.put(`/products/${id}`, data),
  delete: (id) => api.delete(`/products/${id}`),
}

// ─── Stores ──────────────────────────────────────────────────────────────────
export const storesApi = {
  getAll: (params) => api.get('/stores', { params }),
  getActive: () => api.get('/stores/active'),
  getById: (id) => api.get(`/stores/${id}`),
  create: (data) => api.post('/stores', data),
  update: (id, data) => api.put(`/stores/${id}`, data),
  delete: (id) => api.delete(`/stores/${id}`),
}

// ─── Price History ───────────────────────────────────────────────────────────
export const priceHistoryApi = {
  getByProduct: (productId) => api.get(`/price-history/product/${productId}`),
  getSince: (productId, since) => api.get(`/price-history/product/${productId}/since`, { params: { since } }),
  delete: (id) => api.delete(`/price-history/${id}`),
}

// ─── Alerts ──────────────────────────────────────────────────────────────────
export const alertsApi = {
  getAll: (params) => api.get('/alerts', { params }),
  getById: (id) => api.get(`/alerts/${id}`),
  create: (data) => api.post('/alerts', data),
  update: (id, data) => api.put(`/alerts/${id}`, data),
  delete: (id) => api.delete(`/alerts/${id}`),
}

// ─── Feedback ────────────────────────────────────────────────────────────────
export const feedbackApi = {
  getAll: (params) => api.get('/feedback', { params }),
  getById: (id) => api.get(`/feedback/${id}`),
  submit: (data) => api.post('/feedback', data),
  delete: (id) => api.delete(`/feedback/${id}`),
}

// ─── Product Groups ──────────────────────────────────────────────────────────
export const productGroupsApi = {
  getAll: (params) => api.get('/product-groups', { params }),
  getById: (id) => api.get(`/product-groups/${id}`),
  create: (data) => api.post('/product-groups', data),
  update: (id, data) => api.put(`/product-groups/${id}`, data),
  delete: (id) => api.delete(`/product-groups/${id}`),
}

// ─── Visual Search ───────────────────────────────────────────────────────────
export const visualSearchApi = {
  search: (formData) => api.post('/visual-search', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  aiSearch: (formData) => api.post('/visual-search/ai-search', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
}

export default api
