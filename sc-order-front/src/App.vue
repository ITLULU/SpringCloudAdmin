<template>
  <!-- 登录页全屏，不显示顶部导航 -->
  <router-view v-if="isLoginPage" />
  <div v-else class="app-layout">
    <!-- 顶部导航栏 -->
    <header class="app-header">
      <div class="header-inner">
        <div class="header-left" @click="router.push('/hotel/list')">
          <van-icon name="hotel-o" size="22" />
          <span class="logo-text">酒店商城</span>
        </div>
        <div class="header-right">
          <div class="nav-btn" title="我的行程" @click="router.push('/trip/list')">
            <van-icon name="orders-o" size="20" />
          </div>
          <div class="nav-btn" title="我的订单" @click="router.push('/order/list')">
            <van-icon name="bag-o" size="20" />
          </div>
          <div class="avatar" title="个人中心" @click="router.push('/user/profile')">
            <van-icon name="manager-o" size="18" />
          </div>
        </div>
      </div>
    </header>
    <!-- 内容区域 -->
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'

const router = useRouter()
const route = useRoute()
const isLoginPage = computed(() => route.path === '/login')
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  background: #f0f2f5;
  color: #333;
  -webkit-font-smoothing: antialiased;
}

:root {
  --primary: #6366f1;
  --primary-light: #818cf8;
  --primary-dark: #4f46e5;
  --bg: #f0f2f5;
  --card-bg: #ffffff;
  --text-primary: #1f2937;
  --text-secondary: #6b7280;
  --text-muted: #9ca3af;
  --border: #e5e7eb;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.05);
  --radius: 12px;
  --max-width: 960px;
}
</style>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}

.header-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--primary);
  font-weight: 600;
}

.logo-text {
  font-size: 18px;
  letter-spacing: -0.5px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-btn:hover {
  background: rgba(99, 102, 241, 0.08);
}

.nav-btn :deep(.van-icon) {
  color: var(--text-secondary);
  transition: color 0.2s;
}

.nav-btn:hover :deep(.van-icon) {
  color: var(--primary);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s;
}

.avatar:hover {
  transform: scale(1.1);
}

.avatar :deep(.van-icon) {
  color: #fff !important;
}

.app-main {
  flex: 1;
  max-width: var(--max-width);
  width: 100%;
  margin: 0 auto;
  padding: 24px;
}
</style>
