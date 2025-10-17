import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { i18nGlobal } from '../i18n'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: {
      requiresGuest: true,
      titleKey: 'router.titles.login'
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: {
      requiresGuest: true,
      titleKey: 'router.titles.register'
    }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('../views/ResetPasswordView.vue'),
    meta: {
      requiresGuest: true,
      titleKey: 'router.titles.resetPassword'
    }
  },
  {
    path: '/verify-email',
    name: 'VerifyEmail',
    component: () => import('../views/EmailVerificationView.vue'),
    meta: {
      requiresGuest: true,
      titleKey: 'router.titles.verifyEmail'
    }
  },
  {
    path: '/',
    name: 'Calendar',
    component: () => import('../views/CalendarView.vue'),
    meta: {
      requiresAuth: true,
      titleKey: 'router.titles.calendar'
    }
  },
  {
    path: '/calendar',
    redirect: '/'
  },
  {
    path: '/tasks/:taskId',
    name: 'TaskDetail',
    redirect: (to) => {
      // Redirect to calendar with taskId as query parameter to open the detail modal
      return {
        name: 'Calendar',
        query: { taskId: to.params.taskId }
      }
    },
    meta: {
      requiresAuth: true
    }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../components/Auth/UserProfile.vue'),
    meta: {
      requiresAuth: true,
      titleKey: 'router.titles.profile'
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

  // Set page title from i18n
  if (to.meta.titleKey) {
    document.title = i18nGlobal.t(to.meta.titleKey as string)
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