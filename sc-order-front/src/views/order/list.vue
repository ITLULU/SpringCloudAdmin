<template>
  <div class="order-list-page">
    <div class="page-header">
      <h1 class="page-title">我的订单</h1>
      <p class="page-subtitle">查看所有购买记录</p>
    </div>

    <div v-if="!loading && !orders.length" class="empty-state">
      <van-icon name="bag-o" size="48" color="#d1d5db" />
      <p>暂无订单记录</p>
    </div>

    <div v-else class="order-grid">
      <div
        v-for="item in orders"
        :key="item.order.id"
        class="order-card"
        @click="router.push(`/order/${item.order.id}`)"
      >
        <div class="card-header">
          <div class="hotel-tag">
            <van-icon name="hotel-o" size="14" />
            <span>{{ item.hotelName }}</span>
          </div>
          <van-tag round :type="item.order.status === 1 ? 'success' : 'default'">
            {{ item.order.status === 1 ? '已完成' : '已取消' }}
          </van-tag>
        </div>

        <div class="order-items">
          <div v-for="orderItem in item.items" :key="orderItem.id" class="item-row">
            <span class="item-name">{{ orderItem.productName }}</span>
            <van-tag plain size="medium">{{ orderItem.specName }}</van-tag>
            <span class="item-qty">x{{ orderItem.quantity }}</span>
          </div>
        </div>

        <div class="card-footer">
          <span class="order-time">{{ formatTime(item.order.createdTime) }}</span>
          <span class="view-detail">查看详情 →</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const orders = ref<any[]>([])
const loading = ref(false)

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try {
    const tripId = route.query.tripId as string
    const params: any = {}
    if (tripId) params.tripId = tripId
    const res: any = await request.get('/hotel/order/my', { params })
    orders.value = res.data || []
  } catch (e: any) {
    console.error('获取订单列表失败', e)
  } finally {
    loading.value = false
  }
}

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
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
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--text-muted);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

.order-grid {
  display: grid;
  gap: 16px;
}

.order-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.2s;
}

.order-card:hover {
  box-shadow: var(--shadow);
  border-color: rgba(99, 102, 241, 0.2);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.hotel-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.hotel-tag :deep(.van-icon) {
  color: var(--primary);
}

.order-items {
  padding: 12px 0;
  border-top: 1px solid var(--border);
}

.item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.item-name {
  font-weight: 500;
  color: var(--text-primary);
}

.item-qty {
  margin-left: auto;
  color: var(--text-muted);
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.order-time {
  font-size: 12px;
  color: var(--text-muted);
}

.view-detail {
  font-size: 12px;
  color: var(--primary);
  font-weight: 500;
}
</style>
