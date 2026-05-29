<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-title">
        <div class="brand-mark">M</div>
        <div>
          <h1>个人备忘助手</h1>
          <p>登录后查看待办、提醒和完成记录</p>
        </div>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent="login">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" class="full-button" :loading="loading" @click="login">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, ensureCsrf } from '../api/client'

const emit = defineEmits(['logged-in'])
const form = reactive({ username: 'admin', password: 'admin123' })
const loading = ref(false)

async function login() {
  loading.value = true
  try {
    await ensureCsrf()
    const body = new URLSearchParams()
    body.set('username', form.username)
    body.set('password', form.password)
    await api.post('/auth/login', body, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
    await ensureCsrf()
    ElMessage.success('登录成功')
    emit('logged-in')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>
