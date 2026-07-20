<template>
  <div class="hotel-list-page">
    <!-- 页面标题 + 搜索 -->
    <div class="page-header">
      <h1 class="page-title">探索酒店</h1>
      <p class="page-subtitle">发现优质酒店，享受美好旅程</p>
      <div class="search-box">
        <van-icon name="search" size="18" class="search-icon" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索酒店名称 / 品牌 / 地址"
          class="search-input"
          @keyup.enter="fetchHotels"
        />
      </div>
    </div>

    <!-- 酒店网格列表 -->
    <div class="hotel-grid" v-if="hotels.length">
      <div
        v-for="hotel in hotels"
        :key="hotel.id"
        class="hotel-card"
        @click="router.push(`/hotel/${hotel.id}`)"
      >
        <div class="card-image">
          <van-image :src="hotel.logo" width="100%" height="160" fit="cover" radius="8 8 0 0">
            <template #error>
              <div class="img-placeholder">
                <van-icon name="hotel-o" size="36" />
                <span>{{ hotel.brand || 'HOTEL' }}</span>
              </div>
            </template>
          </van-image>
        </div>
        <div class="card-body">
          <div class="card-top">
            <h3 class="hotel-name">{{ hotel.name }}</h3>
            <van-tag type="primary" round plain size="medium">{{ hotel.brand || '品牌酒店' }}</van-tag>
          </div>
          <div class="hotel-address">
            <van-icon name="location-o" size="12" />
            <span>{{ hotel.address }}</span>
          </div>
          <div class="card-footer">
            <span class="view-btn">查看详情</span>
            <van-icon name="arrow" size="14" />
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-else-if="!loading">
      <van-icon name="hotel-o" size="48" color="#d1d5db" />
      <p>暂无酒店信息</p>
    </div>

    <!-- 加载更多 -->
    <div class="load-more" v-if="loading">
      <van-loading size="24" color="var(--primary)" />
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const keyword = ref('')
const hotels = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const pageNum = ref(1)

onMounted(() => fetchHotels())

async function fetchHotels() {
  loading.value = true
  pageNum.value = 1
  try {
    const res: any = await request.get('/hotel/list', {
      params: { pageNum: 1, pageSize: 20, keyword: keyword.value }
    })
    hotels.value = res.data?.records || []
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-header {
  text-align: center;
  padding: 20px 0 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 24px;
}

.search-box {
  max-width: 480px;
  margin: 0 auto;
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 16px;
  color: var(--text-muted);
}

.search-input {
  width: 100%;
  height: 44px;
  padding: 0 16px 0 42px;
  border: 1.5px solid var(--border);
  border-radius: 22px;
  font-size: 14px;
  background: var(--card-bg);
  outline: none;
  transition: all 0.2s;
  color: var(--text-primary);
}

.search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-input::placeholder {
  color: var(--text-muted);
}

.hotel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.hotel-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.25s ease;
}

.hotel-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: rgba(99, 102, 241, 0.3);
}

.img-placeholder {
  width: 100%;
  height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0e7ff 0%, #ede9fe 100%);
  color: var(--primary);
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
}

.card-body {
  padding: 16px;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.hotel-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hotel-address {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.view-btn {
  font-size: 13px;
  color: var(--primary);
  font-weight: 500;
}

.card-footer :deep(.van-icon) {
  color: var(--primary);
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

.load-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
