<template>
  <div class="layout">
    <router-view />
    
    <!-- 底部导航 -->
    <van-tabbar v-model="active" route>
      <van-tabbar-item icon="wap-home" to="/devices">设备</van-tabbar-item>
      <van-tabbar-item icon="chart-trending" to="/analytics">分析</van-tabbar-item>
      <van-tabbar-item icon="user" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const active = ref(0)
</script>

<style scoped>
.layout {
  min-height: 100vh;
  padding-bottom: 50px;
}
</style>
