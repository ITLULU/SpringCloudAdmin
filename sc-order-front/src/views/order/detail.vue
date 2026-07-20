<template>
  <div class="order-detail-page">
    <van-nav-bar title="订单详情" left-arrow @click-left="router.back()" fixed placeholder />

    <van-loading v-if="pageLoading" class="page-loading" />

    <template v-else-if="order">
      <!-- 订单状态 -->
      <div class="status-card" :class="{ cancelled: order.status === 0 }">
        <van-icon :name="order.status === 1 ? 'passed' : 'close'" size="36" />
        <span>{{ order.status === 1 ? '已完成' : '已取消' }}</span>
      </div>

      <!-- 酒店信息 -->
      <van-cell-group inset class="info-group">
        <van-cell title="酒店" :value="hotel?.name" />
        <van-cell title="订单号" :value="order.id" />
        <van-cell title="下单时间" :value="formatTime(order.createdTime)" />
      </van-cell-group>

      <!-- 商品明细 -->
      <div class="items-section">
        <h3>商品明细</h3>
        <div v-for="item in items" :key="item.id" class="item-row">
          <div class="item-info">
            <span class="item-name">{{ item.productName }}</span>
            <van-tag plain>{{ item.specName }}</van-tag>
          </div>
          <div class="item-right">
            <span class="item-qty">x{{ item.quantity }}</span>
            <span class="item-price">{{ item.price === '0.00' || item.price === 0 ? '免费' : `¥${item.price}` }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-section" v-if="order.status === 1">
        <van-button type="danger" plain block round @click="handleCancel">
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

.status-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px;
  background: linear-gradient(135deg, #4caf50, #66bb6a);
  color: #fff;
  font-size: 16px;
  font-weight: 500;
}

.status-card.cancelled {
  background: linear-gradient(135deg, #9e9e9e, #bdbdbd);
}

.info-group {
  margin-top: 12px;
}

.items-section {
  margin: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
}

.items-section h3 {
  font-size: 15px;
  margin: 0 0 12px;
}

.item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.item-row:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-name {
  font-size: 14px;
  color: #333;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-qty {
  font-size: 13px;
  color: #666;
}

.item-price {
  font-size: 14px;
  color: #e53935;
  font-weight: 500;
}

.action-section {
  padding: 24px 16px;
}
</style>
