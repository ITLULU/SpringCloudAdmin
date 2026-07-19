import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
// import request from '@/utils/request'

interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)

  // 登录
  async function login(username: string, password: string): Promise<void> {
    // Mock 登录 - 后续替换为真实 API
    if (username && password) {
      const mockToken = 'mock-token-' + Date.now()
      token.value = mockToken
      setToken(mockToken)
      // 实际项目中使用:
      // const res = await request.post('/auth/login', { username, password })
      // token.value = res.data.token
      // setToken(res.data.token)
    } else {
      throw new Error('用户名和密码不能为空')
    }
  }

  // 获取用户信息
  async function getUserInfo(): Promise<UserInfo> {
    if (!userInfo.value) {
      // Mock 数据
      userInfo.value = {
        id: 1,
        username: 'admin',
        nickname: '管理员',
        avatar: '',
        roles: ['admin']
      }
      // 实际项目中使用:
      // const res = await request.get('/auth/info')
      // userInfo.value = res.data
    }
    return userInfo.value!
  }

  // 退出登录
  function logout(): void {
    token.value = null
    userInfo.value = null
    removeToken()
  }

  return {
    token,
    userInfo,
    login,
    getUserInfo,
    logout
  }
})
