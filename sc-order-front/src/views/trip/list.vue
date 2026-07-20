<template>
  <div class="trip-list-page">
    <van-nav-bar title="我的行程" fixed placeholder>
      <template #left>
        <van-icon name="arrow-left" size="20" @click="router.push('/hotel/list')" />
      </template>
    </van-nav-bar>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-empty v-if="!loading && !trips.length" description="暂无行程，去酒店详情页创建入住" />

      <div v-for="item in trips" :key="item.trip.id" class="trip-card">
        <div class="trip-header">
          <van-tag :type="item.trip.status === 1 ? 'success' : 'default'">
            {{ item.trip.status === 1 ? '已入住' : '已取消' }}
          </van-tag>
          <span class="trip-hotel-name">{{ item.hotel?.name || '未知酒店' }}</span>
        </div>
        <div class="trip-dates">
          <van-icon name="calendar-o" />
          <span>{{ item.trip.checkInDate }} 至 {{ item.trip.checkOutDate }}</span>
        </div>
        <div class="trip-actions">
          <van-button
            v-if="item.trip.status === 1"
            size="small"
            plain
            type="primary"
            @click="goProducts(item)"
          >
            浏览商品
          </van-button>
          <van-button
            v-if="item.trip.status === 1"
            size="small"
            plain
            @click="handleCancel(item.trip)"
          >
            取消行程
          </van-button>
          <van-button
            size="small"
            plain
            type="primary"
            @click="goOrders(item.trip.id)"
          >
            查看订单
          </van-button>
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
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const activeTab = ref(1)
const trips = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)

onMounted(() => fetchTrips())

async function fetchTrips() {
  loading.value = true
  try {
    const res: any = await request.get('/hotel/trip/my')
    trips.value = res.data || []
  } catch {
    // handled
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function onRefresh() {
  fetchTrips()
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
  } catch {
    // cancelled or error
  }
}
</script>

<style scoped>
.trip-card {
  margin: 8px 12px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.trip-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.trip-hotel-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.trip-dates {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
}

.trip-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
