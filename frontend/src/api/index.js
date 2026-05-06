import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 120000,
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor — attach token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor — handle 401 redirect
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
    }
    const msg = err.response?.data?.error || err.message || 'Network error'
    console.error('[API Error]', msg)
    return Promise.reject(err)
  }
)

// Health & defaults
export function getHealth() { return api.get('/health') }
export function getDefaults() { return api.get('/defaults') }

// Analysis
export function analyzeReport(payload) { return api.post('/analyze-report', payload) }
export function generatePlan(payload) { return api.post('/plan', payload) }
export function generatePlanFromTexts(payload) { return api.post('/plan-from-texts', payload) }

// Auth
export function login(payload) { return api.post('/auth/login', payload) }
export function register(payload) { return api.post('/auth/register', payload) }
export function getMe() { return api.get('/auth/me') }

// Reports CRUD
export function getReports() { return api.get('/reports') }
export function getReport(id) { return api.get(`/reports/${id}`) }
export function getReportsByTicker(ticker) { return api.get(`/reports/ticker/${ticker}`) }
export function createReport(payload) { return api.post('/reports', payload) }
export function updateReport(id, payload) { return api.put(`/reports/${id}`, payload) }
export function deleteReport(id) { return api.delete(`/reports/${id}`) }
export function deleteReportsBatch(ids) { return api.delete('/reports/batch', { data: ids }) }

// History
export function getHistory() { return api.get('/history') }
export function getHistoryByPlan(planId) { return api.get(`/history/plan/${planId}`) }
export function saveHistory(records) { return api.post('/history/save', records) }

// Strategy comparison
export function compareStrategies(payload) { return api.post('/compare-strategies', payload) }

export default api
