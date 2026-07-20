<template>
  <div class="product-detail-page">
    <van-loading v-if="pageLoading" class="page-loading" />

    <template v-else-if="product">
      <!-- 面包屑 -->
      <div class="breadcrumb">
        <span class="crumb-link" @click="router.push('/hotel/list')">酒店</span>
        <van-icon name="arrow" size="12" />
        <span class="crumb-link" @click="router.push({ path: '/product/list', query: { hotelId } })">商品</span>
        <van-icon name="arrow" size="12" />
        <span class="crumb-current">{{ product.name }}</span>
      </div>

      <div class="detail-layout">
        <!-- 左侧：商品图片 -->
        <div class="product-image">
          <van-image :src="product.coverImage" width="100%" height="320" fit="cover" radius="12">
            <template #error>
              <div class="img-placeholder">
                <van-icon name="photo-o" size="48" />
              </div>
            </template>
          </van-image>
        </div>

        <!-- 右侧：商品信息 -->
        <div class="product-info">
          <h1 class="product-name">{{ product.name }}</h1>
          <div class="price-row">
            <span class="price">免费</span>
            <van-tag round type="success">0元购</van-tag>
          </div>
          <p class="product-desc" v-if="product.description">{{ product.description }}</p>

          <!-- 规格选择 -->
          <div class="spec-section">
            <h3 class="section-title">选择规格</h3>
            <div class="spec-list">
              <div
                v-for="spec in specs"
                :key="spec.id"
                class="spec-card"
                :class="{ selected: selectedSpec?.id === spec.id, disabled: spec.stock <= 0 }"
                @click="selectSpec(spec)"
              >
                <div class="spec-name">{{ spec.specName }}</div>
                <div class="spec-value">{{ spec.specValue }}</div>
                <div class="spec-stock">
                  <template v-if="spec.stock > 0">
                    剩余 <strong>{{ spec.stock }}</strong> 件
                  </template>
                  <van-tag v-else type="danger" round>已售罄</van-tag>
                </div>
              </div>
            </div>
          </div>

          <!-- 数量选择 -->
          <div class="quantity-row" v-if="selectedSpec">
            <span>购买数量</span>
            <van-stepper v-model="quantity" :max="selectedSpec.stock" min="1" theme="round" />
          </div>

          <!-- 操作按钮 -->
          <div class="action-area">
            <van-button
              type="primary"
              round
              block
              :disabled="!canOrder"
              :loading="orderLoading"
              @click="handleOrder"
              class="order-btn"
            >
              {{ !checkedIn ? '请先入住后再下单' : '立即下单（0元）' }}
            </van-button>
          </div>
        </div>
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

const canOrder = computed(() => checkedIn && selectedSpec.value && selectedSpec.value.stock > 0)

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

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  background: var(--card-bg);
  border-radius: var(--radius);
  padding: 24px;
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
}

.img-placeholder {
  width: 100%;
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4ff 0%, #faf5ff 100%);
  color: var(--primary-light);
  border-radius: 12px;
}

.product-info {
  display: flex;
  flex-direction: column;
}

.product-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 10px;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.price {
  font-size: 24px;
  font-weight: 700;
  color: #ef4444;
}

.product-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0 0 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.spec-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 20px;
}

.spec-card {
  padding: 12px;
  border: 1.5px solid var(--border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.spec-card:hover:not(.disabled) {
  border-color: var(--primary-light);
}

.spec-card.selected {
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.04);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.12);
}

.spec-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spec-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.spec-value {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.spec-stock {
  font-size: 12px;
  color: var(--text-muted);
}

.spec-stock strong {
  color: var(--primary);
}

.quantity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-top: 1px solid var(--border);
  margin-bottom: 20px;
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.action-area {
  margin-top: auto;
}

.order-btn {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border: none;
}

@media (max-width: 640px) {
  .detail-layout {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
