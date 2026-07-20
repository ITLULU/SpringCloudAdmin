<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">
          <van-icon name="hotel-o" size="40" color="#667eea" />
        </div>
        <h1>酒店商城</h1>
        <p>欢迎登录</p>
      </div>
      <van-form @submit="handleLogin" class="login-form">
        <van-field
          v-model="form.username"
          name="username"
          placeholder="请输入用户名"
          left-icon="user-o"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          placeholder="请输入密码"
          left-icon="lock"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
        <div class="login-actions">
          <van-button round block type="primary" native-type="submit" :loading="loading" color="linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
            登 录
          </van-button>
          <div class="login-footer">
            还没有账号？
            <span class="link" @click="router.push('/login?mode=register')">去注册</span>
          </div>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  loading.value = true
  try {
    const res: any = await request.post('/auth/login', {
      username: form.username,
      password: form.password
    })
    const data = res.data
    const token = data.token || data.accessToken
    if (token) {
      localStorage.setItem('token', token)
      localStorage.setItem('username', form.username)
      showToast('登录成功')
      const redirect = (route.query.redirect as string) || '/hotel/list'
      router.replace(redirect)
    }
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 360px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.login-header {
  text-align: center;
  padding: 32px 24px 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
}

.logo-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.logo-icon :deep(.van-icon) {
  color: #fff !important;
}

.login-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px;
}

.login-header p {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.login-form {
  padding: 24px 20px 28px;
}

.login-form :deep(.van-field) {
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 16px;
  padding: 4px 12px;
}

.login-actions {
  margin-top: 8px;
}

.login-footer {
  text-align: center;
  margin-top: 18px;
  font-size: 13px;
  color: #999;
}

.link {
  color: #667eea;
  cursor: pointer;
  font-weight: 500;
}
</style>
