<template>
  <div class="login-page">
    <div class="login-header">
      <h1>酒店商城</h1>
      <p>欢迎登录</p>
    </div>
    <van-form @submit="handleLogin" class="login-form">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>
      <div class="login-actions">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          登 录
        </van-button>
        <div class="login-footer">
          还没有账号？
          <span class="link" @click="router.push('/login?mode=register')">去注册</span>
        </div>
      </div>
    </van-form>
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
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0 20px;
}

.login-header {
  text-align: center;
  color: #fff;
  margin-bottom: 40px;
}

.login-header h1 {
  font-size: 28px;
  margin: 0 0 8px;
}

.login-header p {
  font-size: 14px;
  opacity: 0.8;
}

.login-form {
  background: #fff;
  border-radius: 12px;
  padding: 24px 16px;
}

.login-actions {
  padding: 24px 16px 0;
}

.login-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #999;
}

.link {
  color: #1989fa;
  cursor: pointer;
}
</style>
