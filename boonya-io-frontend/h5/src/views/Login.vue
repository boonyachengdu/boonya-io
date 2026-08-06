<template>
  <div class="login-page">
    <div class="login-header">
      <h1>Boonya IoT</h1>
      <p>智能设备管理平台</p>
    </div>
    
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="username"
          name="username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请填写用户名' }]"
        />
        <van-field
          v-model="password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请填写密码' }]"
        />
      </van-cell-group>
      
      <div style="margin: 16px;">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          登录
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { login } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const username = ref('admin')
const password = ref('admin123')

const onSubmit = async () => {
  loading.value = true
  try {
    const data = await login({
      username: username.value,
      password: password.value,
    })
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    showToast('登录成功')
    router.push('/')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 16px;
}

.login-header {
  text-align: center;
  color: #fff;
  margin-bottom: 40px;
}

.login-header h1 {
  font-size: 32px;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  opacity: 0.9;
}
</style>
