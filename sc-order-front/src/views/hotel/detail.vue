<template>
  <div class="hotel-detail-page">
    <van-nav-bar title="酒店详情" left-arrow @click-left="router.back()" fixed placeholder />

    <van-loading v-if="pageLoading" class="page-loading" />

    <template v-else-if="hotel">
      <!-- 酒店头部信息 -->
      <div class="hotel-header">
        <van-image
          :src="hotel.logo"
          width="100%"
          height="200"
          fit="cover"
        >
          <template #error>
            <div class="header-placeholder">
              <van-icon name="hotel-o" size="48" color="#fff" />
              <span>{{ hotel.name }}</span>
            </div>
          </template>
        </van-image>
      </div>

      <div class="hotel-info-card">
        <h2 class="hotel-name">{{ hotel.name }}</h2>
        <van-tag type="primary" plain class="brand-tag">{{ hotel.brand }}</van-tag>
        <van-cell-group inset>
          <van-cell icon="location-o" title="地址" :value="hotel.address" />
          <van-cell icon="phone-o" title="电话" :value="hotel.phone" is-link :url="`tel:${hotel.phone}`" />
        </van-cell-group>
        <div class="hotel-desc" v-if="hotel.description">
          <h4>酒店简介</h4>
          <p>{{ hotel.description }}</p>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-section">
        <van-button
          type="primary"
          block
          round
          icon="calendar-o"
          @click="goCreateTrip"
        >
          立即入住（0元）
        </van-button>
        <van-button
          plain
          type="primary"
          block
          round
          icon="shopping-cart-o"
          class="mt-12"
          @click="goProducts"
        >
          浏览商品
        </van-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const hotel = ref<any>(null)
const pageLoading = ref(true)

onMounted(async () => {
  try {
    const res: any = await request.get(`/hotel/${route.params.id}`)
    hotel.value = res.data
  } catch {
    // handled
  } finally {
    pageLoading.value = false
  }
})

function goCreateTrip() {
  const token = localStorage.getItem('token')
  if (!token) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  router.push({ path: '/trip/create', query: { hotelId: route.params.id as string } })
}

function goProducts() {
  router.push({ path: '/product/list', query: { hotelId: route.params.id as string } })
}
</script>

<style scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 100px;
}

.header-placeholder {
  width: 100%;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 18px;
  gap: 8px;
}

.hotel-info-card {
  padding: 16px;
}

.hotel-name {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px;
}

.brand-tag {
  margin-bottom: 12px;
}

.hotel-desc {
  padding: 12px 16px;
}

.hotel-desc h4 {
  font-size: 14px;
  color: #333;
  margin: 0 0 8px;
}

.hotel-desc p {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.action-section {
  padding: 16px;
}

.mt-12 {
  margin-top: 12px;
}
</style>
