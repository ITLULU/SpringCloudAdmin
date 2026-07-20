<template>
  <div class="hotel-list-page">
    <van-nav-bar title="酒店列表" fixed placeholder>
      <template #right>
        <van-icon name="user-o" size="20" @click="router.push('/user/profile')" />
      </template>
    </van-nav-bar>

    <!-- 搜索栏 -->
    <van-search
      v-model="keyword"
      placeholder="搜索酒店名称/品牌/地址"
      @search="fetchHotels"
      @clear="fetchHotels"
    />

    <!-- 酒店列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="fetchHotels"
      >
        <div
          v-for="hotel in hotels"
          :key="hotel.id"
          class="hotel-card"
          @click="router.push(`/hotel/${hotel.id}`)"
        >
          <div class="hotel-logo">
            <van-image
              :src="hotel.logo || defaultHotelImg"
              width="80"
              height="80"
              fit="cover"
              radius="8"
            >
              <template #error>
                <div class="img-error">酒店</div>
              </template>
            </van-image>
          </div>
          <div class="hotel-info">
            <div class="hotel-name">{{ hotel.name }}</div>
            <div class="hotel-brand">
              <van-tag type="primary" plain>{{ hotel.brand || '品牌酒店' }}</van-tag>
            </div>
            <div class="hotel-address">{{ hotel.address }}</div>
          </div>
          <van-icon name="arrow" class="hotel-arrow" />
        </div>
      </van-list>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const activeTab = ref(0)
const keyword = ref('')
const hotels = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const defaultHotelImg = ''

async function fetchHotels() {
  loading.value = true
  try {
    const res: any = await request.get('/hotel/list', {
      params: { pageNum: pageNum.value, pageSize: 10, keyword: keyword.value }
    })
    const records = res.data?.records || []
    if (pageNum.value === 1) {
      hotels.value = records
    } else {
      hotels.value.push(...records)
    }
    finished.value = records.length < 10
    pageNum.value++
  } catch {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function onRefresh() {
  pageNum.value = 1
  finished.value = false
  fetchHotels()
}
</script>

<style scoped>
.hotel-card {
  display: flex;
  align-items: center;
  padding: 16px;
  margin: 8px 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.hotel-logo {
  flex-shrink: 0;
  margin-right: 12px;
}

.img-error {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  color: #999;
  border-radius: 8px;
  font-size: 12px;
}

.hotel-info {
  flex: 1;
  overflow: hidden;
}

.hotel-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.hotel-brand {
  margin-bottom: 4px;
}

.hotel-address {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hotel-arrow {
  flex-shrink: 0;
  color: #ccc;
  margin-left: 8px;
}
</style>
