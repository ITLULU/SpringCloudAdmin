import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, getRefreshToken, setToken, clearAllTokens } from './auth'
import router from '@/router'

/**
 * Axios 请求封装
 * <p>
 * 功能：
 * - 请求拦截器：自动携带 AccessToken
 * - 响应拦截器：
 *   1. 滑动窗口模式：从响应头读取新Token并更新本地存储
 *   2. 401 错误：尝试用 RefreshToken 刷新，失败则跳转登录页
 * </p>
 */

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/**
 * 是否正在刷新Token（防止并发请求重复刷新）
 */
let isRefreshing = false

/**
 * 等待刷新完成的请求队列
 */
let pendingRequests: Array<(token: string) => void> = []

// 请求拦截器 - 自动携带 Token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // === 滑动窗口续期：从响应头读取新Token ===
    // 后端在 Token 即将过期时，自动签发新Token并通过响应头返回
    const renewed = response.headers['x-token-renewed']
    if (renewed === 'true') {
      const newToken = response.headers['authorization']
      if (newToken && newToken.startsWith('Bearer ')) {
        const tokenValue = newToken.substring(7)
        setToken(tokenValue)
        console.debug('[Token续期] 滑动窗口已自动续期')
      }
    }

    // === 业务响应处理 ===
    const res = response.data
    if (res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        handleUnauthorized()
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  async (error) => {
    const status = error.response?.status

    if (status === 401) {
      // === RefreshToken 模式：尝试自动刷新 ===
      const refreshToken = getRefreshToken()
      if (refreshToken && !isRefreshing) {
        isRefreshing = true
        try {
          // 调用刷新接口
          const res = await axios.post('/api/auth/refresh', { refreshToken })
          if (res.data?.code === 200 && res.data?.data?.accessToken) {
            const newAccessToken = res.data.data.accessToken
            setToken(newAccessToken)
            console.debug('[Token续期] RefreshToken模式刷新成功')

            // 通知队列中的请求使用新Token重试
            pendingRequests.forEach(cb => cb(newAccessToken))
            pendingRequests = []

            // 重试当前失败的请求
            const config = error.config
            config.headers.Authorization = `Bearer ${newAccessToken}`
            return service(config)
          }
        } catch {
          // 刷新失败，清除Token并跳转登录
          console.warn('[Token续期] 刷新失败，需要重新登录')
        } finally {
          isRefreshing = false
        }
      }

      // 刷新失败或没有 RefreshToken → 跳转登录
      handleUnauthorized()
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

/**
 * 处理未授权（401）情况
 */
function handleUnauthorized(): void {
  clearAllTokens()
  router.push('/login')
  ElMessage.error('登录已过期，请重新登录')
}

export default service
