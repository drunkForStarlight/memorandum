<template>
  <article class="task-row" :class="{ done: task.status === 'DONE' }">
    <button class="task-main" @click="$emit('detail', task)">
      <span class="task-title">{{ task.title }}</span>
      <span class="task-meta">
        <span>{{ task.category || '无分类' }}</span>
        <span v-if="task.dueAt">截止 {{ formatTime(task.dueAt) }}</span>
        <span v-if="task.remindAt">提醒 {{ formatTime(task.remindAt) }}</span>
        <span v-if="task.completedAt">完成 {{ formatTime(task.completedAt) }}</span>
      </span>
    </button>
    <el-tag :type="priorityType(task.priority)" effect="plain">{{ priorityText(task.priority) }}</el-tag>
    <div class="task-actions">
      <el-button :icon="View" circle title="详情" @click="$emit('detail', task)" />
      <el-button :icon="Edit" circle title="编辑" @click="$emit('edit', task)" />
      <el-button v-if="task.status !== 'DONE'" :icon="Check" circle type="success" title="完成" @click="$emit('complete', task)" />
      <el-button v-if="task.status !== 'DONE' && task.status !== 'CANCELED'" :icon="Close" circle type="warning" title="取消" @click="$emit('cancel', task)" />
    </div>
  </article>
</template>

<script setup>
import { Check, Close, Edit, View } from '@element-plus/icons-vue'

defineProps({
  task: { type: Object, required: true }
})
defineEmits(['detail', 'edit', 'complete', 'cancel'])

function formatTime(value) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

function priorityText(value) {
  return { LOW: '低', MEDIUM: '普通', HIGH: '高' }[value] || value
}

function priorityType(value) {
  return { LOW: 'info', MEDIUM: 'primary', HIGH: 'danger' }[value] || 'info'
}
</script>

