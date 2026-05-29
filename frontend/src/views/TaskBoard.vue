<template>
  <section class="content-panel">
    <el-table :data="tasks" v-loading="loading" height="calc(100vh - 334px)" empty-text="暂无事项">
      <el-table-column prop="title" label="事项" min-width="220">
        <template #default="{ row }">
          <button class="link-button" @click="$emit('detail', row)">{{ row.title }}</button>
          <div class="muted-line">{{ row.description || '无描述' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="120">
        <template #default="{ row }">{{ row.category || '未分类' }}</template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="110">
        <template #default="{ row }">
          <el-tag :type="priorityType(row.priority)" effect="plain">{{ priorityText(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="dueAt" label="截止时间" width="180">
        <template #default="{ row }">{{ formatTime(row.dueAt) }}</template>
      </el-table-column>
      <el-table-column prop="remindAt" label="提醒时间" width="180">
        <template #default="{ row }">{{ formatTime(row.remindAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button :icon="View" circle title="详情" @click="$emit('detail', row)" />
          <el-button :icon="Edit" circle title="编辑" @click="$emit('edit', row)" />
          <el-button v-if="row.status !== 'DONE'" :icon="Check" circle type="success" title="完成" @click="$emit('complete', row)" />
          <el-button v-if="row.status !== 'DONE' && row.status !== 'CANCELED'" :icon="Close" circle type="warning" title="取消" @click="$emit('cancel', row)" />
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { Check, Close, Edit, View } from '@element-plus/icons-vue'

defineProps({
  tasks: { type: Array, default: () => [] },
  loading: Boolean
})
defineEmits(['refresh', 'detail', 'edit', 'complete', 'cancel'])

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function priorityText(value) {
  return { LOW: '低', MEDIUM: '中', HIGH: '高' }[value] || value
}

function priorityType(value) {
  return { LOW: 'info', MEDIUM: 'primary', HIGH: 'danger' }[value] || 'info'
}

function statusText(value) {
  return { TODO: '待办', DOING: '进行中', DONE: '已完成', CANCELED: '已取消' }[value] || value
}

function statusType(value) {
  return { TODO: 'info', DOING: 'primary', DONE: 'success', CANCELED: 'warning' }[value] || 'info'
}
</script>
