<template>
  <div class="trip-create-page">
    <van-nav-bar title="创建行程" left-arrow @click-left="router.back()" fixed placeholder />

    <div class="form-section">
      <van-cell-group inset>
        <van-field
          v-model="hotelName"
          label="酒店"
          readonly
          :loading="hotelLoading"
        />
        <van-field
          v-model="checkInDate"
          is-link
          readonly
          label="入住日期"
          placeholder="请选择入住日期"
          @click="showCheckInPicker = true"
        />
        <van-field
          v-model="checkOutDate"
          is-link
          readonly
          label="离店日期"
          placeholder="请选择离店日期"
          @click="showCheckOutPicker = true"
        />
      </van-cell-group>

      <div class="tips">
        <van-icon name="info-o" />
        <span>0元入住，入住期间可浏览并下单购买酒店商品</span>
      </div>

      <div class="submit-section">
        <van-button
          type="primary"
          block
          round
          :loading="submitting"
          :disabled="!checkInDate || !checkOutDate"
          @click="handleSubmit"
        >
          确认入住
        </van-button>
      </div>
    </div>

    <!-- 日期选择器 -->
    <van-popup v-model:show="showCheckInPicker" position="bottom" round>
      <van-date-picker
        title="选择入住日期"
        :min-date="minDate"
        :max-date="maxDate"
        @confirm="onCheckInConfirm"
        @cancel="showCheckInPicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showCheckOutPicker" position="bottom" round>
      <van-date-picker
        title="选择离店日期"
        :min-date="checkInDate ? new Date(checkInDate) : minDate"
        :max-date="maxDate"
        @confirm="onCheckOutConfirm"
        @cancel="showCheckOutPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const hotelId = route.query.hotelId as string

const hotelName = ref('')
const hotelLoading = ref(true)
const checkInDate = ref('')
const checkOutDate = ref('')
const showCheckInPicker = ref(false)
const showCheckOutPicker = ref(false)
const submitting = ref(false)

const today = new Date()
const minDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())
const maxDate = new Date(today.getFullYear() + 1, 11, 31)

onMounted(async () => {
  try {
    const res: any = await request.get(`/hotel/${hotelId}`)
    hotelName.value = res.data?.name || ''
  } catch {
    // handled
  } finally {
    hotelLoading.value = false
  }
})

function formatDate(values: string[]): string {
  return `${values[0]}-${values[1].padStart(2, '0')}-${values[2].padStart(2, '0')}`
}

function onCheckInConfirm({ selectedValues }: any) {
  checkInDate.value = formatDate(selectedValues)
  showCheckInPicker.value = false
  // 如果离店日期早于入住日期，清空
  if (checkOutDate.value && checkOutDate.value <= checkInDate.value) {
    checkOutDate.value = ''
  }
}

function onCheckOutConfirm({ selectedValues }: any) {
  checkOutDate.value = formatDate(selectedValues)
  showCheckOutPicker.value = false
}

async function handleSubmit() {
  if (!checkInDate.value || !checkOutDate.value) return
  if (checkOutDate.value <= checkInDate.value) {
    showToast('离店日期必须晚于入住日期')
    return
  }

  submitting.value = true
  try {
    await request.post('/hotel/trip', {
      hotelId,
      checkInDate: checkInDate.value,
      checkOutDate: checkOutDate.value
    })
    showToast('入住成功')
    router.replace('/trip/list')
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-section {
  padding-top: 12px;
}

.tips {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 24px;
  font-size: 12px;
  color: #ed6a0c;
}

.submit-section {
  padding: 24px 16px;
}
</style>
