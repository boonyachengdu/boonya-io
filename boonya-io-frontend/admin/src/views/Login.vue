<template>
  <div class="login-page">
    <!-- 左侧品牌展示区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <div class="logo-icon">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="6" y="6" width="52" height="52" rx="12" stroke="currentColor" stroke-width="2.5" opacity="0.4"/>
              <circle cx="32" cy="32" r="14" stroke="currentColor" stroke-width="2.5"/>
              <circle cx="32" cy="32" r="5" fill="currentColor"/>
              <path d="M32 6v8M32 50v8M6 32h8M50 32h8" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
              <path d="M14 14l5 5M50 14l-5 5M14 50l5-5M50 50l-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
            </svg>
          </div>
          <h1 class="brand-title">Boonya IoT</h1>
        </div>
        <p class="brand-subtitle">企业级物联网管理平台</p>

        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>设备全生命周期管理</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>实时监控与智能告警</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>TDengine 时序数据分析</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>OTA 远程升级与能碳管理</span>
          </div>
        </div>

        <div class="brand-footer">
          <span>© 2024 Boonya Lab · 让物联网更简单</span>
        </div>
      </div>

      <!-- 装饰光效 -->
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
      <div class="grid-pattern"></div>
    </div>

    <!-- 右侧登录表单区 -->
    <div class="login-form-wrapper">
      <div class="login-form-container">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-desc">请登录您的管理员账户</p>
        </div>

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          label-position="top"
          class="login-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
              class="custom-input"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              size="large"
              class="custom-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              size="large"
              class="login-btn"
              @click="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>默认账户：admin / admin123</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm)
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error) {
        console.error('Login error:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-page {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* ===== 左侧品牌区 ===== */
.login-brand {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a1929 0%, #0d2847 50%, #103a5c 100%);
  overflow: hidden;
}

.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
  padding: 40px;
  max-width: 480px;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  color: #00d4ff;
  filter: drop-shadow(0 0 12px rgba(0, 212, 255, 0.4));
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #ffffff 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 48px;
  letter-spacing: 1px;
}

.brand-features {
  text-align: left;
  display: inline-flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 60px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.8);
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 10px rgba(0, 212, 255, 0.6);
  flex-shrink: 0;
}

.brand-footer {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
}

/* 装饰光效 */
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.glow-1 {
  width: 400px;
  height: 400px;
  background: #00d4ff;
  top: -100px;
  right: -100px;
}

.glow-2 {
  width: 300px;
  height: 300px;
  background: #0ea5e9;
  bottom: -80px;
  left: -60px;
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 212, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 255, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
  z-index: 1;
}

/* ===== 右侧表单区 ===== */
.login-form-wrapper {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f9fc;
  position: relative;
}

.login-form-container {
  width: 380px;
  padding: 40px;
}

.form-header {
  margin-bottom: 40px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: #0a1929;
  margin-bottom: 8px;
}

.form-desc {
  font-size: 14px;
  color: #8492a6;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border: none;
  background: linear-gradient(135deg, #0d2847 0%, #103a5c 100%);
  box-shadow: 0 4px 16px rgba(13, 40, 71, 0.3);
  transition: all 0.3s;
}

.login-btn:hover {
  background: linear-gradient(135deg, #103a5c 0%, #155278 100%);
  box-shadow: 0 6px 20px rgba(13, 40, 71, 0.4);
  transform: translateY(-1px);
}

.form-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
  font-size: 13px;
  color: #a0a8b3;
}

/* 自定义输入框样式 */
:deep(.custom-input .el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 14px;
  box-shadow: 0 0 0 1px #e0e4ea;
  transition: all 0.3s;
}

:deep(.custom-input .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #0d2847;
}

:deep(.custom-input.el-input__wrapper.is-focus),
:deep(.custom-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(0, 212, 255, 0.3) !important;
}

:deep(.custom-input .el-input__inner) {
  height: 42px;
  font-size: 15px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #475669;
}

/* 响应式：小屏隐藏左侧品牌区 */
@media (max-width: 900px) {
  .login-brand {
    display: none;
  }
  .login-form-wrapper {
    width: 100%;
  }
}
</style>
