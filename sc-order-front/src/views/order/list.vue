<template>
  <div class="order-list-page">
    <van-nav-bar title="我的订单" fixed placeholder>
      <template #left>
        <van-icon name="arrow-left" size="20" @click="router.push('/hotel/list')" />
      </template>
    </van-nav-bar>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-empty v-if="!loading && !orders.length" description="暂无订单" />

      <div
        v-for="item in orders"
        :key="item.order.id"
        class="order-card"
        @click="router.push(`/order/${item.order.id}`)"
      >
        <div class="order-header">
          <span class="order-hotel">{{ item.hotelName }}</span>
          <van-tag :type="item.order.status === 1 ? 'success' : 'default'">
            {{ item.order.status === 1 ? '已完成' : '已取消' }}
          </van-tag>
        </div>
        <div class="order-items">
          <div v-for="orderItem in item.items" :key="orderItem.id" class="order-item-row">
            <span>{{ orderItem.productName }}</span>
            <span class="spec-label">{{ orderItem.specName }}</span>
            <span>x{{ orderItem.quantity }}</span>
          </div>
        </div>
        <div class="order-footer">
          <span class="order-time">{{ formatTime(item.order.createdTime) }}</span>
          <van-icon name="arrow" />
        </div>
      </div>
    </van-pull-refresh>

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
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const activeTab = ref(2)
const orders = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try {
    const tripId = route.query.tripId as string
    const params: any = {}
    if (tripId) params.tripId = tripId
    const res: any = await request.get('/hotel/order/my', { params })
    orders.value = res.data || []
  } catch {
    // handled
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function onRefresh() {
  fetchOrders()
}

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped>
.order-card {
  margin: 8px 12px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.order-hotel {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.order-items {
  border-top: 1px solid #f5f5f5;
  padding-top: 8px;
}

.order-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
  padding: 4px 0;
}

.spec-label {
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.order-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
}

.order-time {
  font-size: 12px;
  color: #999;
}
</style>
