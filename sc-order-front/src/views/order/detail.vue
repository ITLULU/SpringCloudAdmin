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
          <span class="value">{{ hotel?.name }}</span>
        </div>
        <div class="info-row">
          <span class="label">下单时间</span>
          <span class="value">{{ formatTime(order.createdTime) }}</span>
        </div>
      </div>

      <!-- 商品明细 -->
      <div class="items-card">
        <h3 class="section-title">商品明细</h3>
        <div v-for="item in items" :key="item.id" class="item-row">
          <div class="item-left">
            <span class="item-name">{{ item.productName }}</span>
            <van-tag plain round>{{ item.specName }}</van-tag>
          </div>
          <div class="item-right">
            <span class="item-qty">x{{ item.quantity }}</span>
            <span class="item-price">{{ item.price === '0.00' || item.price === 0 ? '免费' : `¥${item.price}` }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-section" v-if="order.status === 1">
        <van-button type="danger" plain round block @click="handleCancel" class="cancel-btn">
          取消订单（归还库存）
        </van-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const orderId = route.params.id as string

const order = ref<any>(null)
const items = ref<any[]>([])
const hotel = ref<any>(null)
const pageLoading = ref(true)

onMounted(async () => {
  try {
    const res: any = await request.get(`/hotel/order/${orderId}`)
    order.value = res.data?.order
    items.value = res.data?.items || []
    hotel.value = res.data?.hotel
  } catch {
    // handled
  } finally {
    pageLoading.value = false
  }
})

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
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
  } catch {
    // cancelled or error
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
}

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
}

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
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
}

.item-row:last-child {
  border-bottom: none;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-qty {
  font-size: 13px;
  color: var(--text-muted);
}

.item-price {
  font-size: 14px;
  color: #ef4444;
  font-weight: 600;
}

.action-section {
  margin-top: 24px;
}

.cancel-btn {
  height: 44px;
  font-weight: 500;
}
</style>
