import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setAuth(t, u, r) {
    token.value = t
    username.value = u
    role.value = r
    localStorage.setItem('token', t)
    localStorage.setItem('username', u)
    localStorage.setItem('role', r)
  }

  function clearAuth() {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  async function login(username_, password) {
    const res = await api.post('/auth/login', { username: username_, password })
    const d = res.data
    setAuth(d.token, d.username, d.role)
    return d
  }

  async function register(username_, password, email) {
    const res = await api.post('/auth/register', { username: username_, password, email })
    const d = res.data
    setAuth(d.token, d.username, d.role)
    return d
  }

  async function fetchMe() {
    try {
      const res = await api.get('/auth/me')
      const d = res.data
      setAuth(token.value, d.username, d.role)
    } catch {
      clearAuth()
    }
  }

  return { token, username, role, isLoggedIn, isAdmin, setAuth, clearAuth, login, register, fetchMe }
})
