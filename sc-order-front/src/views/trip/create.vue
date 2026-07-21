<template>
  <div class="trip-create-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="crumb-link" @click="router.back()">返回</span>
      <van-icon name="arrow" size="12" />
      <span class="crumb-current">创建行程</span>
    </div>

    <div class="form-card">
      <h2 class="form-title">办理入住</h2>
      <p class="form-desc">填写入住信息，0元即可入住</p>

      <div class="form-fields">
        <div class="field-item">
          <label class="field-label">入住酒店</label>
          <div class="field-value hotel-field">
            <van-icon name="hotel-o" />
            <span>{{ hotelName || '加载中...' }}</span>
          </div>
        </div>

        <div class="field-item">
          <label class="field-label">入住日期</label>
          <div class="field-input" @click="showCheckInPicker = true">
            <span :class="{ placeholder: !checkInDate }">
              {{ checkInDate || '请选择入住日期' }}
            </span>
            <van-icon name="calendar-o" />
          </div>
        </div>

        <div class="field-item">
          <label class="field-label">离店日期</label>
          <div class="field-input" @click="showCheckOutPicker = true">
            <span :class="{ placeholder: !checkOutDate }">
              {{ checkOutDate || '请选择离店日期' }}
            </span>
            <van-icon name="calendar-o" />
          </div>
        </div>
      </div>

      <div class="tips">
        <van-icon name="info-o" />
        <span>入住期间可浏览并下单购买酒店商品，行程时间段不可重叠</span>
      </div>

      <van-button
        type="primary"
        block
        round
        :loading="submitting"
        :disabled="!checkInDate || !checkOutDate"
        @click="handleSubmit"
        class="submit-btn"
      >
        确认入住
      </van-button>
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
  } catch (e: any) {
    console.error('获取酒店信息失败', e)
  }
})

function formatDate(values: string[]): string {
  return `${values[0]}-${values[1].padStart(2, '0')}-${values[2].padStart(2, '0')}`
}

function onCheckInConfirm({ selectedValues }: any) {
  checkInDate.value = formatDate(selectedValues)
  showCheckInPicker.value = false
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
  } catch (e: any) {
    console.error('创建行程失败', e)
  } finally {
    submitting.value = false
  }
}
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

.form-card {
  max-width: 520px;
  margin: 0 auto;
  background: var(--card-bg);
  border-radius: var(--radius);
  padding: 32px;
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
}

.form-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.form-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 28px;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 20px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.field-value {
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
}

.hotel-field {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--primary);
  font-weight: 500;
}

.field-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border: 1.5px solid var(--border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  transition: border-color 0.2s;
}

.field-input:hover {
  border-color: var(--primary-light);
}

.field-input .placeholder {
  color: var(--text-muted);
}

.field-input :deep(.van-icon) {
  color: var(--text-muted);
}

.tips {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 14px;
  font-size: 12px;
  color: #b45309;
  background: #fffbeb;
  border-radius: 8px;
  margin-bottom: 24px;
}

.submit-btn {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border: none;
}
</style>
