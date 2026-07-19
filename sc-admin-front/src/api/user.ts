import request from '@/utils/request'

// 用户列表查询参数
export interface UserQuery {
  username?: string
  phone?: string
  status?: number
  pageNum: number
  pageSize: number
}

// 用户信息
export interface UserInfo {
  id?: number
  username: string
  nickname: string
  email: string
  phone: string
  status: number
  roleId?: number
  createTime?: string
}

// Mock 数据
const mockUsers: UserInfo[] = [
  { id: 1, username: 'admin', nickname: '管理员', email: 'admin@example.com', phone: '13800138000', status: 1, roleId: 1, createTime: '2025-01-01 00:00:00' },
  { id: 2, username: 'zhangsan', nickname: '张三', email: 'zhangsan@example.com', phone: '13800138001', status: 1, roleId: 2, createTime: '2025-01-02 00:00:00' },
  { id: 3, username: 'lisi', nickname: '李四', email: 'lisi@example.com', phone: '13800138002', status: 0, roleId: 2, createTime: '2025-01-03 00:00:00' },
  { id: 4, username: 'wangwu', nickname: '王五', email: 'wangwu@example.com', phone: '13800138003', status: 1, roleId: 3, createTime: '2025-01-04 00:00:00' },
  { id: 5, username: 'zhaoliu', nickname: '赵六', email: 'zhaoliu@example.com', phone: '13800138004', status: 1, roleId: 3, createTime: '2025-01-05 00:00:00' }
]

// 获取用户列表 (Mock)
export function getUserList(params: UserQuery) {
  // return request.get('/system/user/list', { params })
  return new Promise((resolve) => {
    setTimeout(() => {
      let list = [...mockUsers]
      if (params.username) {
        list = list.filter((u) => u.username.includes(params.username!))
      }
      if (params.phone) {
        list = list.filter((u) => u.phone.includes(params.phone!))
      }
      if (params.status !== undefined) {
        list = list.filter((u) => u.status === params.status)
      }
      const total = list.length
      const start = (params.pageNum - 1) * params.pageSize
      const rows = list.slice(start, start + params.pageSize)
      resolve({ code: 200, msg: 'success', data: { total, rows } })
    }, 300)
  })
}

// 新增用户
export function addUser(data: UserInfo) {
  return request.post('/system/user', data)
}

// 修改用户
export function updateUser(data: UserInfo) {
  return request.put('/system/user', data)
}

// 删除用户
export function deleteUser(id: number) {
  return request.delete(`/system/user/${id}`)
}

// 重置密码
export function resetPassword(id: number, password: string) {
  return request.put('/system/user/resetPwd', { id, password })
}
