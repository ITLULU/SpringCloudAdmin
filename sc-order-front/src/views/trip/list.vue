<template>
  <div class="trip-list-page">
    <div class="page-header">
      <h1 class="page-title">我的行程</h1>
      <p class="page-subtitle">管理您的酒店入住行程</p>
    </div>

    <div v-if="!loading && !trips.length" class="empty-state">
      <van-icon name="orders-o" size="48" color="#d1d5db" />
      <p>暂无行程记录</p>
      <van-button size="small" type="primary" round @click="router.push('/hotel/list')">去看看酒店</van-button>
    </div>

    <div v-else class="trip-grid">
      <div v-for="item in trips" :key="item.trip.id" class="trip-card">
        <div class="card-top">
          <div class="hotel-info">
            <van-icon name="hotel-o" size="18" />
            <span class="hotel-name">{{ item.hotel?.name || '未知酒店' }}</span>
          </div>
          <van-tag round :type="item.trip.status === 1 ? 'success' : 'default'">
            {{ item.trip.status === 1 ? '入住中' : '已取消' }}
          </van-tag>
        </div>
        <div class="date-range">
          <div class="date-item">
            <span class="date-label">入住</span>
            <span class="date-value">{{ item.trip.checkInDate }}</span>
          </div>
          <div class="date-divider">
            <span>至</span>
          </div>
          <div class="date-item">
            <span class="date-label">离店</span>
            <span class="date-value">{{ item.trip.checkOutDate }}</span>
          </div>
        </div>
        <div class="card-actions">
          <van-button
            v-if="item.trip.status === 1"
            size="small"
            round
            type="primary"
            @click="goProducts(item)"
          >
            浏览商品
          </van-button>
          <van-button
            size="small"
            round
            plain
            type="primary"
            @click="goOrders(item.trip.id)"
          >
            查看订单
          </van-button>
          <van-button
            v-if="item.trip.status === 1"
            size="small"
            round
            plain
            @click="handleCancel(item.trip)"
          >
            取消行程
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const trips = ref<any[]>([])
const loading = ref(false)

onMounted(() => fetchTrips())

async function fetchTrips() {
  loading.value = true
  try {
    const res: any = await request.get('/hotel/trip/my')
    trips.value = res.data || []
  } catch (e: any) {
    console.error('获取行程列表失败', e)
  } finally {
    loading.value = false
  }
}

function goProducts(item: any) {
  router.push({
    path: '/product/list',
    query: { hotelId: item.trip.hotelId, checkedIn: '1', tripId: item.trip.id }
  })
}

function goOrders(tripId: string) {
  router.push({ path: '/order/list', query: { tripId } })
}

async function handleCancel(trip: any) {
  try {
    await showConfirmDialog({ title: '确认取消', message: '确定要取消该行程吗？' })
    await request.put(`/hotel/trip/cancel/${trip.id}`)
    showToast('已取消')
    fetchTrips()
  } catch (e: any) {
    if (e !== 'cancel') {
      console.error('取消行程失败', e)
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

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--text-muted);
}

.empty-state p {
  margin: 12px 0 16px;
  font-size: 14px;
}

.trip-grid {
  display: grid;
  gap: 16px;
}

.trip-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
  transition: box-shadow 0.2s;
}

.trip-card:hover {
  box-shadow: var(--shadow);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.hotel-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--primary);
}

.hotel-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.date-range {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 10px;
  margin-bottom: 16px;
}

.date-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.date-label {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.date-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.date-divider {
  font-size: 12px;
  color: var(--text-muted);
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
