<template>
  <div class="profile-page">
    <div class="page-header">
      <h1 class="page-title">个人中心</h1>
    </div>

    <!-- 用户卡片 -->
    <div class="user-card" @click="!isLoggedIn && router.push('/login')">
      <div class="user-avatar">
        <van-icon name="manager-o" size="28" />
      </div>
      <div class="user-info">
        <h3 class="user-name">{{ isLoggedIn ? username : '点击登录' }}</h3>
        <p class="user-desc">{{ isLoggedIn ? '欢迎回来' : '登录后可享受完整服务' }}</p>
      </div>
    </div>

    <!-- 功能菜单 -->
    <div class="menu-card">
      <div class="menu-item" @click="goPage('/trip/list')">
        <div class="menu-icon trip">
          <van-icon name="orders-o" size="20" />
        </div>
        <span class="menu-text">我的行程</span>
        <van-icon name="arrow" size="14" class="menu-arrow" />
      </div>
      <div class="menu-item" @click="goPage('/order/list')">
        <div class="menu-icon order">
          <van-icon name="bag-o" size="20" />
        </div>
        <span class="menu-text">我的订单</span>
        <van-icon name="arrow" size="14" class="menu-arrow" />
      </div>
      <div class="menu-item" @click="router.push('/hotel/list')">
        <div class="menu-icon hotel">
          <van-icon name="hotel-o" size="20" />
        </div>
        <span class="menu-text">酒店列表</span>
        <van-icon name="arrow" size="14" class="menu-arrow" />
      </div>
    </div>

    <!-- 退出登录 -->
    <div class="logout-section" v-if="isLoggedIn">
      <van-button plain type="danger" round block @click="handleLogout" class="logout-btn">
        退出登录
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'

const router = useRouter()

const isLoggedIn = computed(() => !!localStorage.getItem('token'))
const username = computed(() => localStorage.getItem('username') || '')

function goPage(path: string) {
  if (!isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: path } })
    return
  }
  router.push(path)
}

async function handleLogout() {
  try {
    await showConfirmDialog({ title: '提示', message: '确定要退出登录吗？' })
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    showToast('已退出')
    router.replace('/login')
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: var(--radius);
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.2s;
}

.user-card:hover {
  transform: translateY(-2px);
}

.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 2px;
}

.user-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.menu-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
  overflow: hidden;
  margin-bottom: 24px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:hover {
  background: #f8fafc;
}

.menu-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.menu-icon.trip {
  background: linear-gradient(135deg, #6366f1, #818cf8);
}

.menu-icon.order {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
}

.menu-icon.hotel {
  background: linear-gradient(135deg, #10b981, #34d399);
}

.menu-text {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.menu-arrow {
  color: var(--text-muted);
}

.logout-section {
  padding: 0;
}

.logout-btn {
  height: 44px;
  font-weight: 500;
}
</style>
