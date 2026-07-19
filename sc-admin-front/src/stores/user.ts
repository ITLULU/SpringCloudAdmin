import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, setRefreshToken, clearAllTokens } from '@/utils/auth'
import request from '@/utils/request'

/**
 * 用户信息接口
 */
interface UserInfo {
  username: string
  roles: string[]
  permissions: string[]
}

/**
 * 登录响应数据
 */
interface LoginResult {
  username: string
  token?: string
  accessToken?: string
  refreshToken?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)

  /**
   * 用户登录
   * <p>
   * 根据后端续期模式，返回数据格式不同：
   * - mode=none: { token, username }
   * - mode=refresh-token: { accessToken, refreshToken, username }
   * - mode=sliding-window: { token, username }
   * </p>
   */
  async function login(username: string, password: string): Promise<void> {
    const res = await request.post<LoginResult>('/auth/login', { username, password })
    const data = res.data

    // 保存 Token（兼容两种返回格式）
    const accessToken = data.accessToken || data.token
    if (accessToken) {
      token.value = accessToken
      setToken(accessToken)
    }

    // RefreshToken 模式：保存 refreshToken
    if (data.refreshToken) {
      setRefreshToken(data.refreshToken)
    }
  }

  /**
   * 获取当前用户信息（含角色和权限）
   */
  async function getUserInfo(): Promise<UserInfo> {
    const res = await request.get<UserInfo>('/auth/info')
    userInfo.value = res.data
    return res.data
  }

  /**
   * 退出登录
   */
  function logout(): void {
    token.value = null
    userInfo.value = null
    clearAllTokens()
  }

  return {
    token,
    userInfo,
    login,
    getUserInfo,
    logout
  }
})
