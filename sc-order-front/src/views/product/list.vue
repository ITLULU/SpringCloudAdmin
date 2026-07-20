<template>
  <div class="product-list-page">
    <van-nav-bar title="商品列表" left-arrow @click-left="router.back()" fixed placeholder />

    <!-- 入住状态提示 -->
    <div class="checkin-banner" :class="{ active: checkedIn }">
      <van-icon :name="checkedIn ? 'passed' : 'warning-o'" />
      <span>{{ checkedIn ? '已入住，可以下单购买商品' : '未入住，仅可浏览商品信息' }}</span>
    </div>

    <van-loading v-if="pageLoading" class="page-loading" />

    <van-empty v-else-if="!products.length" description="暂无商品" />

    <div v-else class="product-grid">
      <div
        v-for="product in products"
        :key="product.id"
        class="product-card"
        @click="router.push({ path: `/product/${product.id}`, query: { hotelId, checkedIn: checkedIn ? '1' : '0', tripId: currentTripId } })"
      >
        <van-image
          :src="product.coverImage"
          width="100%"
          height="120"
          fit="cover"
          radius="8 8 0 0"
        >
          <template #error>
            <div class="img-placeholder">
              <van-icon name="photo-o" size="32" />
            </div>
          </template>
        </van-image>
        <div class="product-info">
          <div class="product-name">{{ product.name }}</div>
          <div class="product-price">免费</div>
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
const hotelId = route.query.hotelId as string
const products = ref<any[]>([])
const pageLoading = ref(true)
const checkedIn = ref(false)
const currentTripId = ref('')

onMounted(async () => {
  try {
    // 加载商品列表
    const res: any = await request.get('/hotel/product/list', { params: { hotelId } })
    products.value = res.data || []

    // 检查入住状态
    const token = localStorage.getItem('token')
    if (token) {
      try {
        const tripRes: any = await request.get('/hotel/trip/active', { params: { hotelId } })
        checkedIn.value = tripRes.data?.checkedIn || false
        if (tripRes.data?.trip) {
          currentTripId.value = tripRes.data.trip.id
        }
      } catch {
        checkedIn.value = false
      }
    }
  } catch {
    // handled
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}

.checkin-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin: 8px 12px;
  border-radius: 8px;
  background: #fff7e6;
  color: #ed6a0c;
  font-size: 13px;
}

.checkin-banner.active {
  background: #e8f5e9;
  color: #4caf50;
}

.product-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 12px;
}

.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.img-placeholder {
  width: 100%;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #ccc;
}

.product-info {
  padding: 8px 10px;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 16px;
  color: #e53935;
  font-weight: 600;
  margin-top: 4px;
}
</style>
