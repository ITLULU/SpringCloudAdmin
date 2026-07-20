<template>
  <div class="product-detail-page">
    <van-nav-bar title="商品详情" left-arrow @click-left="router.back()" fixed placeholder />

    <van-loading v-if="pageLoading" class="page-loading" />

    <template v-else-if="product">
      <!-- 商品图片 -->
      <div class="product-image">
        <van-image :src="product.coverImage" width="100%" height="240" fit="cover">
          <template #error>
            <div class="img-placeholder">
              <van-icon name="photo-o" size="48" />
            </div>
          </template>
        </van-image>
      </div>

      <!-- 商品基本信息 -->
      <div class="product-info-card">
        <h2 class="product-name">{{ product.name }}</h2>
        <div class="product-price-row">
          <span class="price">免费</span>
          <van-tag type="success">0元</van-tag>
        </div>
        <p class="product-desc" v-if="product.description">{{ product.description }}</p>
      </div>

      <!-- 规格选择 -->
      <div class="spec-section">
        <h3>选择规格</h3>
        <div class="spec-list">
          <div
            v-for="spec in specs"
            :key="spec.id"
            class="spec-item"
            :class="{ selected: selectedSpec?.id === spec.id, disabled: spec.stock <= 0 }"
            @click="selectSpec(spec)"
          >
            <div class="spec-name">{{ spec.specName }}</div>
            <div class="spec-value">{{ spec.specValue }}</div>
            <div class="spec-stock">
              <template v-if="spec.stock > 0">
                库存: {{ spec.stock }}
              </template>
              <template v-else>
                <van-tag type="danger">已售罄</van-tag>
              </template>
            </div>
          </div>
        </div>
      </div>

      <!-- 数量选择 -->
      <div class="quantity-section" v-if="selectedSpec">
        <span>购买数量</span>
        <van-stepper v-model="quantity" :max="selectedSpec.stock" min="1" />
      </div>

      <!-- 底部操作栏 -->
      <div class="bottom-bar">
        <van-button
          type="primary"
          round
          block
          :disabled="!canOrder"
          :loading="orderLoading"
          @click="handleOrder"
        >
          {{ !isLoggedIn ? '请先登录' : !checkedIn ? '请先入住后下单' : '立即下单（0元）' }}
        </van-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const productId = route.params.id as string
const hotelId = route.query.hotelId as string
const checkedIn = route.query.checkedIn === '1'
const tripId = route.query.tripId as string

const product = ref<any>(null)
const specs = ref<any[]>([])
const selectedSpec = ref<any>(null)
const quantity = ref(1)
const pageLoading = ref(true)
const orderLoading = ref(false)

const isLoggedIn = computed(() => !!localStorage.getItem('token'))
const canOrder = computed(() => isLoggedIn.value && checkedIn && selectedSpec.value && selectedSpec.value.stock > 0)

onMounted(async () => {
  try {
    const res: any = await request.get(`/hotel/product/${productId}`)
    product.value = res.data?.product
    specs.value = res.data?.specs || []
  } catch {
    // handled
  } finally {
    pageLoading.value = false
  }
})

function selectSpec(spec: any) {
  if (spec.stock <= 0) return
  selectedSpec.value = spec
  quantity.value = 1
}

async function handleOrder() {
  if (!canOrder.value) return
  orderLoading.value = true
  try {
    await request.post('/hotel/order', {
      hotelId,
      tripId,
      items: [{
        productId: productId,
        specId: selectedSpec.value.id,
        quantity: quantity.value
      }]
    })
    showToast('下单成功')
    router.push('/order/list')
  } catch {
    // handled
  } finally {
    orderLoading.value = false
  }
}
</script>

<style scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 80px;
}

.img-placeholder {
  width: 100%;
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #ccc;
}

.product-info-card {
  padding: 16px;
  background: #fff;
}

.product-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px;
}

.product-price-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.price {
  font-size: 22px;
  font-weight: 700;
  color: #e53935;
}

.product-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.spec-section {
  margin-top: 8px;
  padding: 16px;
  background: #fff;
}

.spec-section h3 {
  font-size: 15px;
  margin: 0 0 12px;
}

.spec-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spec-item {
  flex: 0 0 calc(50% - 5px);
  padding: 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.spec-item.selected {
  border-color: #1989fa;
  background: #ecf5ff;
}

.spec-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spec-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.spec-value {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.spec-stock {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.quantity-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  margin-top: 8px;
  background: #fff;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
}
</style>
