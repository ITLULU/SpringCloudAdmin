import request from '@/utils/request'

export interface MenuInfo {
  id?: number
  parentId: number
  menuName: string
  menuType: string // M: 目录, C: 菜单, F: 按钮
  path?: string
  component?: string
  icon?: string
  permission?: string
  sort: number
  status: number
  visible?: boolean
  children?: MenuInfo[]
  createTime?: string
}

const mockMenus: MenuInfo[] = [
  {
    id: 1, parentId: 0, menuName: '系统管理', menuType: 'M', path: '/system', icon: 'Setting', sort: 1, status: 1, visible: true,
    children: [
      { id: 11, parentId: 1, menuName: '用户管理', menuType: 'C', path: 'user', component: 'system/user/index', icon: 'User', sort: 1, status: 1, permission: 'system:user:list', createTime: '2025-01-01 00:00:00' },
      { id: 12, parentId: 1, menuName: '角色管理', menuType: 'C', path: 'role', component: 'system/role/index', icon: 'UserFilled', sort: 2, status: 1, permission: 'system:role:list', createTime: '2025-01-01 00:00:00' },
      { id: 13, parentId: 1, menuName: '菜单管理', menuType: 'C', path: 'menu', component: 'system/menu/index', icon: 'Menu', sort: 3, status: 1, permission: 'system:menu:list', createTime: '2025-01-01 00:00:00' }
    ]
  },
  {
    id: 2, parentId: 0, menuName: '系统监控', menuType: 'M', path: '/monitor', icon: 'Monitor', sort: 2, status: 1, visible: true,
    children: [
      { id: 21, parentId: 2, menuName: '在线用户', menuType: 'C', path: 'online', component: 'monitor/online/index', icon: 'View', sort: 1, status: 1, permission: 'monitor:online:list', createTime: '2025-01-01 00:00:00' }
    ]
  }
]

export function getMenuList() {
  // return request.get('/system/menu/list')
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: 'success', data: mockMenus })
    }, 300)
  })
}

export function addMenu(data: MenuInfo) {
  return request.post('/system/menu', data)
}

export function updateMenu(data: MenuInfo) {
  return request.put('/system/menu', data)
}

export function deleteMenu(id: number) {
  return request.delete(`/system/menu/${id}`)
}
