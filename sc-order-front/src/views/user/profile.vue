<template>
  <div class="profile-page">
    <van-nav-bar title="个人中心" fixed placeholder />

    <!-- 用户头部 -->
    <div class="user-header" v-if="isLoggedIn">
      <van-icon name="manager-o" size="48" color="#fff" />
      <div class="user-name">{{ username }}</div>
    </div>
    <div class="user-header" v-else @click="router.push('/login')">
      <van-icon name="manager-o" size="48" color="#fff" />
      <div class="user-name">点击登录</div>
    </div>

    <!-- 功能菜单 -->
    <van-cell-group inset class="menu-group">
      <van-cell icon="orders-o" title="我的行程" is-link @click="goPage('/trip/list')" />
      <van-cell icon="bag-o" title="我的订单" is-link @click="goPage('/order/list')" />
      <van-cell icon="hotel-o" title="酒店列表" is-link @click="router.push('/hotel/list')" />
    </van-cell-group>

    <!-- 退出登录 -->
    <div class="logout-section" v-if="isLoggedIn">
      <van-button type="danger" plain block round @click="handleLogout">
        退出登录
      </van-button>
    </div>

    <!-- 底部TabBar -->
    <van-tabbar v-model="activeTab" route fixed placeholder>
      <van-tabbar-item icon="hotel-o" to="/hotel/list">酒店</van-tabbar-item>
      <van-tabbar-item icon="orders-o" to="/trip/list">行程</van-tabbar-item>
      <van-tabbar-item icon="bag-o" to="/order/list">订单</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/user/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'

const router = useRouter()
const activeTab = ref(3)

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
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.user-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.user-name {
  font-size: 18px;
  color: #fff;
  font-weight: 500;
}

.menu-group {
  margin-top: 12px;
}

.logout-section {
  padding: 32px 16px;
}
</style>
