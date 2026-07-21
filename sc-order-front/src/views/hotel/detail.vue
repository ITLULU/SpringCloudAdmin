<template>
  <div class="hotel-detail-page">
    <van-loading v-if="pageLoading" class="page-loading" />

    <template v-else-if="hotel">
      <!-- 面包屑导航 -->
      <div class="breadcrumb">
        <span class="crumb-link" @click="router.push('/hotel/list')">酒店列表</span>
        <van-icon name="arrow" size="12" />
        <span class="crumb-current">{{ hotel.name }}</span>
      </div>

      <!-- 酒店主卡片 -->
      <div class="hotel-hero">
        <div class="hero-image">
          <van-image :src="hotel.logo" width="100%" height="280" fit="cover" radius="12">
            <template #error>
              <div class="hero-placeholder">
                <van-icon name="hotel-o" size="56" />
                <span>{{ hotel.name }}</span>
              </div>
            </template>
          </van-image>
        </div>
        <div class="hero-info">
          <div class="hero-top">
            <h1 class="hotel-name">{{ hotel.name }}</h1>
            <van-tag round type="primary">{{ hotel.brand }}</van-tag>
          </div>
          <p class="hotel-desc" v-if="hotel.description">{{ hotel.description }}</p>
          <div class="info-list">
            <div class="info-item">
              <van-icon name="location-o" />
              <span>{{ hotel.address }}</span>
            </div>
            <div class="info-item">
              <van-icon name="phone-o" />
              <a :href="`tel:${hotel.phone}`" class="phone-link">{{ hotel.phone }}</a>
            </div>
          </div>
          <div class="hero-actions">
            <van-button type="primary" round icon="calendar-o" @click="goCreateTrip" class="action-btn primary-btn">
              立即入住（0元）
            </van-button>
            <van-button plain round icon="shopping-cart-o" @click="goProducts" class="action-btn">
              浏览商品
            </van-button>
          </div>
        </div>
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
  } catch (e: any) {
    console.error('获取酒店详情失败', e)
  } finally {
    pageLoading.value = false
  }
})

function goCreateTrip() {
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

.hotel-hero {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  background: var(--card-bg);
  border-radius: var(--radius);
  padding: 24px;
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
}

.hero-placeholder {
  width: 100%;
  height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0e7ff 0%, #ede9fe 100%);
  color: var(--primary);
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  gap: 8px;
}

.hero-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.hotel-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.3px;
}

.hotel-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0 0 20px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.info-item :deep(.van-icon) {
  color: var(--primary);
  font-size: 16px;
}

.phone-link {
  color: var(--primary);
  text-decoration: none;
}

.phone-link:hover {
  text-decoration: underline;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  flex: 1;
  height: 42px;
  font-weight: 500;
  border-color: var(--primary);
  color: var(--primary);
}

.primary-btn {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border: none;
}

@media (max-width: 640px) {
  .hotel-hero {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
