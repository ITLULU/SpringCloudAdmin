<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" style="margin-top: 16px">
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleKey" label="权限标识" width="150" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @change="fetchList"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="editForm.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="权限标识" prop="roleKey">
          <el-input v-model="editForm.roleKey" placeholder="请输入权限标识" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="editForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-tree
            ref="menuTreeRef"
            :data="menuTreeData"
            show-checkbox
            node-key="id"
            :default-checked-keys="editForm.menuIds"
            :props="{ label: 'menuName', children: 'children' }"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getRoleList } from '@/api/role'
import type { RoleInfo } from '@/api/role'
import { getMenuList } from '@/api/menu'
import type { MenuInfo } from '@/api/menu'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const tableData = ref<RoleInfo[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editFormRef = ref<FormInstance>()
const menuTreeRef = ref()
const menuTreeData = ref<MenuInfo[]>([])

const queryParams = reactive({
  roleName: '',
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

const editForm = reactive<RoleInfo>({
  roleName: '',
  roleKey: '',
  sort: 0,
  status: 1,
  remark: '',
  menuIds: []
})

const editRules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限标识', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getRoleList(queryParams)
    tableData.value = res.data.rows
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function fetchMenuTree() {
  const res: any = await getMenuList()
  menuTreeData.value = res.data
}

function handleSearch() {
  queryParams.pageNum = 1
  fetchList()
}

function handleReset() {
  queryParams.roleName = ''
  queryParams.status = undefined
  handleSearch()
}

function handleAdd() {
  dialogTitle.value = '新增角色'
  Object.assign(editForm, { id: undefined, roleName: '', roleKey: '', sort: 0, status: 1, remark: '', menuIds: [] })
  dialogVisible.value = true
}

function handleEdit(row: RoleInfo) {
  dialogTitle.value = '编辑角色'
  Object.assign(editForm, { ...row })
  dialogVisible.value = true
}

function handleDelete(row: RoleInfo) {
  ElMessageBox.confirm(`确定要删除角色 "${row.roleName}" 吗？`, '提示', {
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    fetchList()
  }).catch(() => {})
}

async function handleSubmit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  // 获取选中的菜单 ID
  if (menuTreeRef.value) {
    editForm.menuIds = [
      ...menuTreeRef.value.getCheckedKeys(),
      ...menuTreeRef.value.getHalfCheckedKeys()
    ]
  }
  ElMessage.success(editForm.id ? '编辑成功' : '新增成功')
  dialogVisible.value = false
  fetchList()
}

onMounted(() => {
  fetchList()
  fetchMenuTree()
})
</script>

<style lang="scss" scoped>
.search-card {
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.table-toolbar {
  margin-bottom: 16px;
}
</style>
