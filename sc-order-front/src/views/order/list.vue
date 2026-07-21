<template>
  <div class="order-list-page">
    <div class="page-header">
      <h1 class="page-title">我的订单</h1>
      <p class="page-subtitle">查看所有购买记录</p>
    </div>

    <van-loading v-if="loading" class="page-loading" />

    <div v-else-if="!orders.length" class="empty-state">
      <van-icon name="bag-o" size="56" color="#d1d5db" />
      <p class="empty-text">暂无订单记录</p>
      <van-button size="small" type="primary" round @click="router.push('/hotel/list')">去逛逛</van-button>
    </div>

    <div v-else class="order-list">
      <div
        v-for="item in orders"
        :key="item.order.id"
        class="order-card"
        @click="router.push(`/order/${item.order.id}`)"
      >
        <!-- 卡片头部：酒店 + 状态 -->
        <div class="card-top">
          <div class="hotel-info">
            <van-icon name="hotel-o" size="16" />
            <span class="hotel-name">{{ item.hotelName }}</span>
          </div>
          <van-tag
            round
            size="medium"
            :color="item.order.status === 1 ? '#10b981' : '#9ca3af'"
            text-color="#fff"
          >
            {{ item.order.status === 1 ? '已完成' : '已取消' }}
          </van-tag>
        </div>

        <!-- 商品列表 -->
        <div class="goods-section">
          <div v-for="(goods, idx) in item.items" :key="idx" class="goods-item">
            <div class="goods-icon">
              <van-icon name="gift-o" size="20" />
            </div>
            <div class="goods-info">
              <span class="goods-name">{{ goods.productName }}</span>
              <span class="goods-spec">{{ goods.specName }}</span>
            </div>
            <div class="goods-right">
              <span class="goods-price">{{ formatPrice(goods.price) }}</span>
              <span class="goods-qty">x{{ goods.quantity }}</span>
            </div>
          </div>
        </div>

        <!-- 底部：时间 + 合计 -->
        <div class="card-bottom">
          <span class="order-time">{{ formatTime(item.order.createdTime) }}</span>
          <div class="order-total">
            <span class="total-label">共{{ totalQty(item.items) }}件 合计</span>
            <span class="total-amount">¥{{ formatAmount(item.totalAmount) }}</span>
          </div>
        </div>

        <!-- 操作栏 -->
        <div class="card-actions">
          <van-button size="small" plain round type="primary" @click.stop="router.push(`/order/${item.order.id}`)">
            查看详情
          </van-button>
          <van-button
            v-if="item.order.status === 1"
            size="small"
            plain
            round
            type="danger"
            @click.stop="handleCancel(item.order.id)"
          >
            取消订单
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
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
    //console.log('[订单列表] 响应数据:', res)
    // 兼容两种数据格式：丰富格式 {order,items,hotelName} 和平铺格式 {id,status,items,...}
    orders.value = (res.data || []).map((item: any) => {
      if (item.order) return item
      return {
        order: item,
        items: item.items || [],
        hotelName: '酒店',
        totalAmount: item.totalAmount
      }
    })
    //console.log('[订单列表] 订单数量:', orders.value.length, '首条:', orders.value[0])
  } catch (e: any) {
    console.error('获取订单列表失败', e)
  } finally {
    loading.value = false
  }
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatPrice(price: any) {
  if (!price || price === '0.00' || price === 0) return '免费'
  return `¥${Number(price).toFixed(2)}`
}

function formatAmount(amount: any) {
  if (!amount) return '0.00'
  return Number(amount).toFixed(2)
}

function totalQty(items: any[]) {
  return items.reduce((sum: number, i: any) => sum + (i.quantity || 0), 0)
}

async function handleCancel(orderId: string) {
  try {
    await showConfirmDialog({ title: '确认取消', message: '取消订单后库存将自动归还，确定要取消吗？' })
    await request.put(`/hotel/order/cancel/${orderId}`)
    showToast('订单已取消，库存已归还')
    fetchOrders()
  } catch (e: any) {
    if (e !== 'cancel') {
      console.error('取消订单失败', e)
    }
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
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 80px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
}

.empty-text {
  margin: 16px 0;
  font-size: 15px;
  color: var(--text-muted);
}

/* 订单列表 */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  padding: 0;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
  overflow: hidden;
  transition: all 0.2s;
}

.order-card:hover {
  box-shadow: var(--shadow);
  border-color: rgba(99, 102, 241, 0.15);
}

/* 卡片顶部 */
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
}

.hotel-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hotel-info :deep(.van-icon) {
  color: var(--primary);
}

.hotel-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 商品区域 */
.goods-section {
  padding: 0 20px;
  border-top: 1px solid var(--border);
}

.goods-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.goods-item:last-child {
  border-bottom: none;
}

.goods-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ede9fe, #e0e7ff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.goods-icon :deep(.van-icon) {
  color: var(--primary);
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.goods-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-spec {
  font-size: 12px;
  color: var(--text-muted);
}

.goods-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
}

.goods-price {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.goods-qty {
  font-size: 12px;
  color: var(--text-muted);
}

/* 底部 */
.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fafbfc;
  border-top: 1px solid var(--border);
}

.order-time {
  font-size: 12px;
  color: var(--text-muted);
}

.order-total {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.total-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.total-amount {
  font-size: 16px;
  font-weight: 700;
  color: #ef4444;
}

/* 操作栏 */
.card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--border);
}
</style>
