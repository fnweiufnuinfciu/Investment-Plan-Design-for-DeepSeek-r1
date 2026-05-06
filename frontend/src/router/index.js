import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'

const routes = [
  { path: '/', name: 'Dashboard', component: Dashboard },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/reports', name: 'Reports', component: () => import('@/views/ReportManage.vue'), meta: { requiresAuth: true } },
  { path: '/portfolios', name: 'Portfolios', component: () => import('@/views/PortfolioHistory.vue'), meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
