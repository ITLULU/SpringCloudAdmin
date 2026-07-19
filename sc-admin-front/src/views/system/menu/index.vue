<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd(0)">
          <el-icon><Plus /></el-icon>新增根菜单
        </el-button>
      </div>

      <el-table
        v-if="refreshTable"
        :data="tableData"
        v-loading="loading"
        row-key="id"
        border
        :default-expand-all="true"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="icon" label="图标" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="menuType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.menuType === 'M'" type="primary" size="small">目录</el-tag>
            <el-tag v-else-if="row.menuType === 'C'" type="success" size="small">菜单</el-tag>
            <el-tag v-else type="info" size="small">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permission" label="权限标识" min-width="160" />
        <el-table-column prop="path" label="路由路径" width="140" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleAdd(row.id)">新增</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="editForm.parentId"
            :data="menuTreeOptions"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            check-strictly
            placeholder="选择上级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="editForm.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="editForm.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="图标" v-if="editForm.menuType !== 'F'">
          <el-input v-model="editForm.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-form-item label="路由路径" v-if="editForm.menuType !== 'F'">
          <el-input v-model="editForm.path" placeholder="请输入路由路径" />
        </el-form-item>
        <el-form-item label="组件路径" v-if="editForm.menuType === 'C'">
          <el-input v-model="editForm.component" placeholder="请输入组件路径" />
        </el-form-item>
        <el-form-item label="权限标识" v-if="editForm.menuType !== 'M'">
          <el-input v-model="editForm.permission" placeholder="如 system:user:list" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMenuList } from '@/api/menu'
import type { MenuInfo } from '@/api/menu'

const loading = ref(false)
const tableData = ref<MenuInfo[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editFormRef = ref<FormInstance>()
const refreshTable = ref(true)
const menuTreeOptions = ref<MenuInfo[]>([])

const editForm = reactive<MenuInfo>({
  parentId: 0,
  menuName: '',
  menuType: 'M',
  path: '',
  icon: '',
  component: '',
  permission: '',
  sort: 0,
  status: 1
})

const editRules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getMenuList()
    tableData.value = res.data
    menuTreeOptions.value = [{ id: 0, menuName: '顶级菜单', parentId: 0, sort: 0, status: 1, children: res.data } as MenuInfo]
  } finally {
    loading.value = false
  }
}

function handleAdd(parentId: number) {
  dialogTitle.value = '新增菜单'
  Object.assign(editForm, { id: undefined, parentId, menuName: '', menuType: 'M', path: '', icon: '', component: '', permission: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

function handleEdit(row: MenuInfo) {
  dialogTitle.value = '编辑菜单'
  Object.assign(editForm, { ...row })
  dialogVisible.value = true
}

function handleDelete(row: MenuInfo) {
  ElMessageBox.confirm(`确定要删除菜单 "${row.menuName}" 吗？`, '提示', {
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    refreshTable.value = false
    nextTick(() => {
      refreshTable.value = true
      fetchList()
    })
  }).catch(() => {})
}

async function handleSubmit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  ElMessage.success(editForm.id ? '编辑成功' : '新增成功')
  dialogVisible.value = false
  refreshTable.value = false
  await nextTick()
  refreshTable.value = true
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.table-toolbar {
  margin-bottom: 16px;
}
</style>
