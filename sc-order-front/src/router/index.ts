import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/hotel/list'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/hotel/list',
    name: 'HotelList',
    component: () => import('@/views/hotel/list.vue'),
    meta: { title: '酒店列表' }
  },
  {
    path: '/hotel/:id',
    name: 'HotelDetail',
    component: () => import('@/views/hotel/detail.vue'),
    meta: { title: '酒店详情' }
  },
  {
    path: '/product/list',
    name: 'ProductList',
    component: () => import('@/views/product/list.vue'),
    meta: { title: '商品列表' }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/product/detail.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/trip/list',
    name: 'TripList',
    component: () => import('@/views/trip/list.vue'),
    meta: { title: '我的行程', requireAuth: true }
  },
  {
    path: '/trip/create',
    name: 'TripCreate',
    component: () => import('@/views/trip/create.vue'),
    meta: { title: '创建行程', requireAuth: true }
  },
  {
    path: '/order/list',
    name: 'OrderList',
    component: () => import('@/views/order/list.vue'),
    meta: { title: '我的订单', requireAuth: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/order/detail.vue'),
    meta: { title: '订单详情', requireAuth: true }
  },
  {
    path: '/user/profile',
    name: 'Profile',
    component: () => import('@/views/user/profile.vue'),
    meta: { title: '个人中心', requireAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 所有页面均需登录（登录页除外）
router.beforeEach((to: RouteLocationNormalized, _from: RouteLocationNormalized, next: any) => {
  document.title = (to.meta.title as string) || '酒店商城'
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    // 已登录用户访问登录页，直接跳转首页
    if (token) {
      next('/hotel/list')
    } else {
      next()
    }
    return
  }
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
