<template>
  <div class="order-detail-page">
    <van-loading v-if="pageLoading" class="page-loading" />

    <template v-else-if="order">
      <!-- 面包屑 -->
      <div class="breadcrumb">
        <span class="crumb-link" @click="router.back()">返回</span>
        <van-icon name="arrow" size="12" />
        <span class="crumb-current">订单详情</span>
      </div>

      <!-- 状态卡片 -->
      <div class="status-section" :class="{ cancelled: order.status === 0 }">
        <div class="status-icon">
          <van-icon :name="order.status === 1 ? 'passed' : 'close'" size="32" />
        </div>
        <div class="status-text">
          <h2>{{ order.status === 1 ? '订单已完成' : '订单已取消' }}</h2>
          <p>订单号：{{ order.id }}</p>
        </div>
      </div>

      <!-- 信息卡片 -->
      <div class="info-card">
        <div class="info-row">
          <span class="label">酒店</span>
          <span class="value">
            <van-icon name="hotel-o" size="14" style="margin-right: 4px; color: var(--primary)" />
            {{ hotelName }}
          </span>
        </div>
        <div class="info-row">
          <span class="label">下单时间</span>
          <span class="value">{{ formatTime(order.createdTime) }}</span>
        </div>
        <div class="info-row" v-if="order.tripId">
          <span class="label">关联行程</span>
          <span class="value">{{ order.tripId }}</span>
        </div>
      </div>

      <!-- 商品明细 -->
      <div class="items-card">
        <h3 class="section-title">商品明细</h3>
        <div v-for="(item, idx) in items" :key="idx" class="item-row">
          <div class="item-icon">
            <van-icon name="gift-o" size="18" />
          </div>
          <div class="item-info">
            <span class="item-name">{{ item.productName }}</span>
            <van-tag plain round size="small">{{ item.specName }}</van-tag>
          </div>
          <div class="item-right">
            <span class="item-qty">x{{ item.quantity }}</span>
            <span class="item-price">{{ formatPrice(item.price) }}</span>
          </div>
        </div>
        <!-- 合计 -->
        <div class="total-row">
          <span>共 {{ totalQty }} 件商品</span>
          <span class="total-amount">合计 ¥{{ formatAmount(totalAmount) }}</span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-section" v-if="order.status === 1">
        <van-button type="danger" plain round block @click="handleCancel" class="cancel-btn">
          取消订单（归还库存）
        </van-button>
      </div>
    </template>

    <!-- 无数据 -->
    <div v-else class="empty-state">
      <van-icon name="bag-o" size="48" color="#d1d5db" />
      <p>订单不存在或加载失败</p>
      <van-button size="small" type="primary" round @click="router.back()">返回</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const orderId = route.params.id as string

const order = ref<any>(null)
const items = ref<any[]>([])
const hotelName = ref('')
const totalAmount = ref(0)
const pageLoading = ref(true)

const totalQty = computed(() => items.value.reduce((sum, i) => sum + (i.quantity || 0), 0))

onMounted(async () => {
  try {
    const res: any = await request.get(`/hotel/order/${orderId}`)
    console.log('[订单详情] 响应数据:', res.data)
    const data = res.data

    if (data) {
      // 兼容两种格式
      if (data.order) {
        order.value = data.order
        items.value = data.items || []
        hotelName.value = data.hotelName || '酒店'
        totalAmount.value = data.totalAmount || 0
      } else {
        // 平铺格式
        order.value = data
        items.value = data.items || []
        hotelName.value = '酒店'
        totalAmount.value = data.totalAmount || 0
      }
    }
  } catch (e: any) {
    console.error('获取订单详情失败', e)
  } finally {
    pageLoading.value = false
  }
})

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

async function handleCancel() {
  try {
    await showConfirmDialog({
      title: '确认取消',
      message: '取消订单后库存将自动归还，确定要取消吗？'
    })
    await request.put(`/hotel/order/cancel/${orderId}`)
    showToast('订单已取消，库存已归还')
    order.value.status = 0
  } catch (e: any) {
    if (e !== 'cancel') {
      console.error('取消订单失败', e)
    }
  }
}
</script>

<style scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 80px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 20px;
}

.crumb-link {
  color: var(--primary);
  cursor: pointer;
}

.crumb-link:hover {
  text-decoration: underline;
}

/* 状态区域 */
.status-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(135deg, #10b981, #34d399);
  border-radius: var(--radius);
  color: #fff;
  margin-bottom: 20px;
}

.status-section.cancelled {
  background: linear-gradient(135deg, #6b7280, #9ca3af);
}

.status-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-text h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}

.status-text p {
  font-size: 12px;
  opacity: 0.85;
  margin: 0;
  word-break: break-all;
}

/* 信息卡片 */
.info-card,
.items-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  font-size: 13px;
  color: var(--text-muted);
}

.info-row .value {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
  display: flex;
  align-items: center;
}

/* 商品明细 */
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.item-row:last-of-type {
  border-bottom: none;
}

.item-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ede9fe, #e0e7ff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-icon :deep(.van-icon) {
  color: var(--primary);
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
}

.item-qty {
  font-size: 12px;
  color: var(--text-muted);
}

.item-price {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
}

/* 合计 */
.total-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  margin-top: 8px;
  border-top: 2px solid var(--border);
  font-size: 13px;
  color: var(--text-secondary);
}

.total-amount {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
}

/* 操作 */
.action-section {
  margin-top: 24px;
}

.cancel-btn {
  height: 44px;
  font-weight: 500;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
}

.empty-state p {
  margin: 16px 0;
  font-size: 15px;
}
</style>
