# Admin 管理后台前端实现计划

## 技术栈

| 技术 | 用途 |
|------|------|
| Vue 3 (Composition API) | 核心框架 |
| Vite | 构建工具 |
| TypeScript | 类型安全 |
| Element Plus | UI 组件库 |
| Vue Router 4 | 路由管理 |
| Pinia | 状态管理 |
| Axios | HTTP 请求 |
| SCSS | 样式 |

## 目标目录结构

```
sc-admin-front/
├── public/
├── src/
│   ├── api/                    # API 接口定义
│   │   ├── user.ts
│   │   ├── role.ts
│   │   └── menu.ts
│   ├── assets/                 # 静态资源 (logo等)
│   ├── components/             # 通用组件
│   │   └── Pagination.vue
│   ├── layout/                 # 布局组件
│   │   ├── index.vue           # 主布局 (侧边栏+头部+内容区)
│   │   ├── Sidebar.vue         # 左侧菜单
│   │   ├── Header.vue          # 顶部导航
│   │   └── TagsView.vue       # 标签页导航
│   ├── router/
│   │   └── index.ts            # 路由配置
│   ├── stores/
│   │   ├── user.ts             # 用户状态
│   │   └── app.ts              # 应用状态 (侧边栏折叠等)
│   ├── styles/
│   │   ├── variables.scss      # 主题变量
│   │   └── index.scss          # 全局样式
│   ├── utils/
│   │   ├── request.ts          # Axios 封装 (token拦截/统一错误处理)
│   │   └── auth.ts             # Token 管理
│   ├── views/
│   │   ├── login/index.vue     # 登录页
│   │   ├── dashboard/index.vue # 仪表盘首页
│   │   ├── system/
│   │   │   ├── user/index.vue  # 用户管理
│   │   │   ├── role/index.vue  # 角色管理
│   │   │   └── menu/index.vue  # 菜单管理
│   │   └── error/404.vue       # 404页面
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## 任务分解

### Task 1: 项目初始化与依赖安装
- 在 `sc-admin-front` 目录执行 `npm create vite@latest . -- --template vue-ts`
- 安装核心依赖: `element-plus`, `vue-router`, `pinia`, `axios`, `sass`, `@element-plus/icons-vue`
- 配置 `vite.config.ts`: 代理后端 API (`/api -> http://192.168.235.128:8080`)、Element Plus 自动导入、SCSS 变量
- 配置 `tsconfig.json` 路径别名 `@` -> `src/`

### Task 2: 全局样式与主题变量
- `src/styles/variables.scss`: 侧边栏宽度(210px/64px)、主题色(蓝色系 #409EFF)、头部高度(50px)
- `src/styles/index.scss`: 全局 reset、布局基础样式、滚动条美化
- Element Plus 暗色/亮色变量适配

### Task 3: 工具层封装
- `src/utils/auth.ts`: Token 的 get/set/remove (localStorage)
- `src/utils/request.ts`: Axios 实例封装
  - 请求拦截器: 自动附加 Authorization header
  - 响应拦截器: 统一错误提示 (ElMessage)、401 跳转登录、response data 解包

### Task 4: 状态管理 (Pinia)
- `src/stores/user.ts`: 用户信息 store (login action / logout action / getUserInfo / token 管理)
- `src/stores/app.ts`: 应用状态 store (sidebar collapsed、active menu)

### Task 5: 路由配置
- `src/router/index.ts`:
  - 公开路由: `/login`, `/404`
  - 需认证路由 (layout 嵌套):
    - `/dashboard` - 仪表盘
    - `/system/user` - 用户管理
    - `/system/role` - 角色管理
    - `/system/menu` - 菜单管理
  - 路由守卫: 未登录跳转 `/login`、已登录拦截 `/login`
  - 动态路由占位 (后续从后端获取菜单权限)

### Task 6: 布局组件
- `src/layout/index.vue`: 经典三栏布局 (sidebar + header + main content)
- `src/layout/Sidebar.vue`: Element Plus `el-menu` 递归菜单，支持折叠，带图标
- `src/layout/Header.vue`: 折叠按钮、面包屑、右侧用户信息+退出
- `src/layout/TagsView.vue`: 标签页导航 (打开的页面以 tab 形式展示，支持关闭)

### Task 7: 登录页
- `src/views/login/index.vue`:
  - 居中卡片式登录表单 (用户名+密码+验证码)
  - Element Plus 表单验证
  - 调用登录 API -> 存储 Token -> 跳转 Dashboard
  - 背景渐变色或图案

### Task 8: 仪表盘首页
- `src/views/dashboard/index.vue`:
  - 顶部统计卡片 (4个: 用户数/角色数/菜单数/在线用户)
  - 快捷操作区
  - 占位信息 (后续接入真实数据)

### Task 9: API 接口定义
- `src/api/user.ts`: 用户 CRUD (列表/新增/编辑/删除/重置密码)
- `src/api/role.ts`: 角色 CRUD
- `src/api/menu.ts`: 菜单 CRUD (树形结构)
- 使用 Mock 数据先行 (后续对接后端 API)

### Task 10: 用户管理页面
- `src/views/system/user/index.vue`:
  - 搜索栏 (用户名/状态/手机号)
  - 操作按钮 (新增/删除/导出)
  - 数据表格 (el-table): 分页、排序
  - 新增/编辑对话框 (el-dialog + el-form)
  - 删除确认

### Task 11: 角色管理页面
- `src/views/system/role/index.vue`:
  - 搜索栏 (角色名/状态)
  - 数据表格 + 分页
  - 新增/编辑对话框 (含菜单权限树 el-tree)

### Task 12: 菜单管理页面
- `src/views/system/menu/index.vue`:
  - 树形表格 (el-table + tree): 展示菜单层级
  - 新增/编辑对话框 (菜单类型: 目录/菜单/按钮、图标选择、权限标识)

### Task 13: 通用组件与收尾
- `src/components/Pagination.vue`: 分页组件封装
- `404.vue`: 404 页面
- 最终验证: `npm run dev` 启动，确认所有页面正常渲染和路由跳转

## 关键实现细节

**布局配色 (参考若依风格):**
- 侧边栏: 深色背景 `#304156`，激活项蓝色高亮
- 顶部栏: 白色背景，高度 50px
- 内容区: 浅灰 `#f0f2f5`，带 padding

**Axios 请求规范:**
- baseURL: `/api` (通过 Vite proxy 转发到后端)
- Token: 存储在 localStorage，请求头 `Authorization: Bearer {token}`
- 统一响应格式: `{ code: 200, msg: "success", data: {...} }`

**Mock 策略:**
- 第一阶段 API 层使用本地 Mock 数据，确保前端功能可独立运行
- 后续对接后端时只需替换 API 地址，无需改动业务代码
