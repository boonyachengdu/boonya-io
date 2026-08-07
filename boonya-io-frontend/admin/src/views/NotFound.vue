<template>
  <div class="not-found">
    <div class="content">
      <div class="code">404</div>
      <div class="desc">抱歉，您访问的页面不存在</div>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
}

.content {
  text-align: center;
}

.code {
  font-size: 96px;
  font-weight: bold;
  color: #409eff;
  line-height: 1;
}

.desc {
  font-size: 16px;
  color: #999;
  margin: 16px 0 24px;
}
</style>
