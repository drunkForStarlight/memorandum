<template>
  <section class="content-panel report-layout">
    <div class="report-list">
      <el-table :data="reports" v-loading="loading" height="calc(100vh - 178px)" empty-text="暂无周报">
        <el-table-column prop="subject" label="周报" min-width="240">
          <template #default="{ row }">
            <button class="link-button" @click="selected = row">{{ row.subject }}</button>
            <div class="muted-line">{{ row.weekStart }} 至 {{ row.weekEnd }}</div>
          </template>
        </el-table-column>
        <el-table-column label="发送" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.sentAt" type="success">已发送</el-tag>
            <el-button v-else :icon="Promotion" circle title="发送" @click="send(row)" />
          </template>
        </el-table-column>
      </el-table>
    </div>
    <article class="report-preview">
      <pre v-if="selected">{{ selected.content }}</pre>
      <el-empty v-else description="选择一份周报预览" />
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import { api } from '../api/client'

const reports = ref([])
const selected = ref(null)
const loading = ref(false)

onMounted(load)
defineExpose({ generate })

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/reports/weekly')
    reports.value = data
    selected.value = selected.value || data[0] || null
  } finally {
    loading.value = false
  }
}

async function generate() {
  const { data } = await api.post('/reports/weekly/generate')
  ElMessage.success('已生成周报')
  await load()
  selected.value = data
}

async function send(row) {
  const { data } = await api.post(`/reports/weekly/${row.id}/send`)
  ElMessage.success('已提交发送')
  selected.value = data
  await load()
}
</script>

