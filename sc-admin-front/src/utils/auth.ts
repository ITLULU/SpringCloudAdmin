/**
 * Token 管理工具
 * <p>
 * 支持两种Token：
 * - accessToken: 用于API访问认证
 * - refreshToken: 用于刷新accessToken（仅refresh-token模式使用）
 * </p>
 */

const TOKEN_KEY = 'admin_token'
const REFRESH_TOKEN_KEY = 'admin_refresh_token'

/**
 * 获取 AccessToken
 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置 AccessToken
 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 移除 AccessToken
 */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 获取 RefreshToken
 */
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

/**
 * 设置 RefreshToken
 */
export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

/**
 * 移除 RefreshToken
 */
export function removeRefreshToken(): void {
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

/**
 * 清除所有Token（退出登录时调用）
 */
export function clearAllTokens(): void {
  removeToken()
  removeRefreshToken()
}
