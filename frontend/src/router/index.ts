import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { 
      requiresGuest: true,
      title: 'Accesso - P-Cal'
    }
  },
  {
    path: '/register',
    name: 'Register', 
    component: () => import('../views/RegisterView.vue'),
    meta: { 
      requiresGuest: true,
      title: 'Registrazione - P-Cal'
    }
  },
  {
    path: '/',
    name: 'Calendar',
    component: () => import('../views/CalendarView.vue'),
    meta: { 
      requiresAuth: true,
      title: 'Calendario - P-Cal'
    }
  },
  {
    path: '/calendar',
    redirect: '/'
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../components/Auth/UserProfile.vue'),
    meta: { 
      requiresAuth: true,
      title: 'Profilo Utente - P-Cal'
    }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/SettingsView.vue'),
    meta: { 
      requiresAuth: true,
      title: 'Impostazioni - P-Cal'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

// Navigation guards
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  
  // Set page title
  if (to.meta.title) {
    document.title = to.meta.title as string
  }
  
  // Initialize auth state if not already done
  if (!authStore.isInitialized) {
    console.log('🚀 Router: Initializing auth before navigation to', to.path)
    await authStore.initializeAuth()
    // Give a small delay for reactive updates to complete
    await new Promise(resolve => setTimeout(resolve, 10))
    console.log('✅ Router: Auth initialized, isAuthenticated:', authStore.isAuthenticated)
  }
  
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresGuest = to.matched.some(record => record.meta.requiresGuest)
  
  if (requiresAuth && !authStore.isAuthenticated) {
    console.log('🔒 Router: Auth required but not authenticated, redirecting to login')
    // Redirect to login if authentication is required but user is not authenticated
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (requiresGuest && authStore.isAuthenticated) {
    console.log('👤 Router: Guest route but user is authenticated, redirecting to calendar')
    // Redirect to calendar if guest route is accessed by authenticated user
    next({ name: 'Calendar' })
  } else {
    console.log('✅ Router: Navigation allowed to', to.path)
    next()
  }
})

export default router