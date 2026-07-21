import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { showToast, closeToast } from 'vant'
import { nextTick } from 'vue'
import router from '@/router'

// 封装安全的 toast 显示，确保 DOM 就绪
function safeToast(message: string) {
  nextTick(() => {
    closeToast()
    showToast({
      message,
      position: 'top',
      duration: 3000
    })
  })
}

const service: AxiosInstance = axios.create({
  baseURL: '/api/web/api',
  timeout: 15000
})

// 请求拦截器 - 自动携带 Token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: any) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    // console.log('[Response]', response.config.url, 'status:', response.status, 'body:', res)
    // 检查服务端返回的业务状态码
    if (res && typeof res === 'object' && 'code' in res && res.code !== 200 && res.code !== '200') {
      const msg = res.msg || '请求失败'
      console.warn('[API Error]', response.config.url, '→', msg)
      safeToast(msg)
      if (res.code === 401 || res.code === '401') {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        router.push('/login')
      }
      return Promise.reject(new Error(msg))
    }
    return res
  },
  (error: any) => {
    const status = error.response?.status
    const data = error.response?.data
    // console.log('[HttpError]', error.config?.url, 'status:', status, 'data:', data)
    // 优先显示服务端返回的错误信息
    const serverMsg = data?.msg || data?.message
    console.warn('[HTTP Error]', error.config?.url, '→', status, serverMsg || error.message)
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      router.push('/login')
      safeToast(serverMsg || '登录已过期，请重新登录')
    } else if (status === 403) {
      safeToast(serverMsg || '无权限访问')
    } else {
      safeToast(serverMsg || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
