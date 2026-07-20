<template>
  <div class="product-list-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="crumb-link" @click="router.push('/hotel/list')">酒店列表</span>
      <van-icon name="arrow" size="12" />
      <span class="crumb-current">商品列表</span>
    </div>

    <!-- 入住状态提示 -->
    <div class="status-banner" :class="{ active: checkedIn }">
      <div class="banner-icon">
        <van-icon :name="checkedIn ? 'passed' : 'info-o'" size="18" />
      </div>
      <div class="banner-text">
        <span>{{ checkedIn ? '已入住，可以下单购买酒店商品' : '您尚未入住，仅可浏览商品信息' }}</span>
      </div>
    </div>

    <van-loading v-if="pageLoading" class="page-loading" />
    <div class="empty-state" v-else-if="!products.length">
      <van-icon name="shopping-cart-o" size="48" color="#d1d5db" />
      <p>该酒店暂无商品</p>
    </div>

    <div v-else class="product-grid">
      <div
        v-for="product in products"
        :key="product.id"
        class="product-card"
        @click="router.push({ path: `/product/${product.id}`, query: { hotelId, checkedIn: checkedIn ? '1' : '0', tripId: currentTripId } })"
      >
        <div class="card-image">
          <van-image :src="product.coverImage" width="100%" height="180" fit="cover" radius="10 10 0 0">
            <template #error>
              <div class="img-placeholder">
                <van-icon name="photo-o" size="32" />
              </div>
            </template>
          </van-image>
        </div>
        <div class="card-body">
          <h3 class="product-name">{{ product.name }}</h3>
          <div class="card-bottom">
            <span class="price-tag">免费</span>
            <span class="view-text">查看详情 →</span>
          </div>
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
    const res: any = await request.get('/hotel/product/list', { params: { hotelId } })
    products.value = res.data || []

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

.status-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-radius: var(--radius);
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
  font-size: 14px;
  margin-bottom: 24px;
}

.status-banner.active {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #065f46;
}

.banner-icon {
  flex-shrink: 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.product-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.25s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.img-placeholder {
  width: 100%;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4ff 0%, #faf5ff 100%);
  color: var(--primary-light);
}

.card-body {
  padding: 14px 16px;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price-tag {
  font-size: 16px;
  font-weight: 700;
  color: #ef4444;
}

.view-text {
  font-size: 12px;
  color: var(--primary);
  font-weight: 500;
}

.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 60px;
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
</style>
