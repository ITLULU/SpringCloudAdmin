<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">用户数</div>
              <div class="stat-value">5</div>
            </div>
            <el-icon class="stat-icon" style="color: #409EFF"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">角色数</div>
              <div class="stat-value">3</div>
            </div>
            <el-icon class="stat-icon" style="color: #67C23A"><UserFilled /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">菜单数</div>
              <div class="stat-value">7</div>
            </div>
            <el-icon class="stat-icon" style="color: #E6A23C"><Menu /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">在线用户</div>
              <div class="stat-value">1</div>
            </div>
            <el-icon class="stat-icon" style="color: #F56C6C"><View /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>快捷操作</span>
          </template>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-button type="primary" plain style="width: 100%" @click="$router.push('/system/user')">
                <el-icon><User /></el-icon>用户管理
              </el-button>
            </el-col>
            <el-col :span="8">
              <el-button type="success" plain style="width: 100%" @click="$router.push('/system/role')">
                <el-icon><UserFilled /></el-icon>角色管理
              </el-button>
            </el-col>
            <el-col :span="8">
              <el-button type="warning" plain style="width: 100%" @click="$router.push('/system/menu')">
                <el-icon><Menu /></el-icon>菜单管理
              </el-button>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>系统信息</span>
          </template>
          <div class="info-list">
            <div class="info-item">
              <span>系统名称</span>
              <span>Spring Cloud Admin</span>
            </div>
            <div class="info-item">
              <span>版本</span>
              <span>v1.0.0</span>
            </div>
            <div class="info-item">
              <span>框架</span>
              <span>Vue 3 + Element Plus</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { User, UserFilled, Menu, View } from '@element-plus/icons-vue'
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 0;
}

.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .stat-title {
    font-size: 14px;
    color: #999;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #333;
  }

  .stat-icon {
    font-size: 48px;
    opacity: 0.8;
  }
}

.info-list {
  .info-item {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    border-bottom: 1px solid #f0f0f0;
    font-size: 14px;

    &:last-child {
      border-bottom: none;
    }

    span:first-child {
      color: #999;
    }

    span:last-child {
      color: #333;
    }
  }
}
</style>
