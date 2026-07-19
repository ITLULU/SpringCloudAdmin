import request from '@/utils/request'

export interface RoleQuery {
  roleName?: string
  status?: number
  pageNum: number
  pageSize: number
}

export interface RoleInfo {
  id?: number
  roleName: string
  roleKey: string
  sort: number
  status: number
  remark?: string
  menuIds?: number[]
  createTime?: string
}

const mockRoles: RoleInfo[] = [
  { id: 1, roleName: '超级管理员', roleKey: 'admin', sort: 1, status: 1, remark: '超级管理员', createTime: '2025-01-01 00:00:00' },
  { id: 2, roleName: '普通用户', roleKey: 'user', sort: 2, status: 1, remark: '普通用户', createTime: '2025-01-02 00:00:00' },
  { id: 3, roleName: '访客', roleKey: 'guest', sort: 3, status: 0, remark: '访客角色', createTime: '2025-01-03 00:00:00' }
]

export function getRoleList(params: RoleQuery) {
  // return request.get('/system/role/list', { params })
  return new Promise((resolve) => {
    setTimeout(() => {
      let list = [...mockRoles]
      if (params.roleName) {
        list = list.filter((r) => r.roleName.includes(params.roleName!))
      }
      if (params.status !== undefined) {
        list = list.filter((r) => r.status === params.status)
      }
      const total = list.length
      const start = (params.pageNum - 1) * params.pageSize
      const rows = list.slice(start, start + params.pageSize)
      resolve({ code: 200, msg: 'success', data: { total, rows } })
    }, 300)
  })
}

export function addRole(data: RoleInfo) {
  return request.post('/system/role', data)
}

export function updateRole(data: RoleInfo) {
  return request.put('/system/role', data)
}

export function deleteRole(id: number) {
  return request.delete(`/system/role/${id}`)
}
