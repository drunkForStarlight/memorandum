<template>
  <div class="flow-page">
    <QuickTaskCreate @created="$emit('created', $event)" />

    <div class="summary-strip">
      <div class="summary-tile">
        <strong>{{ dueToday.length }}</strong>
        <span>今天</span>
      </div>
      <div class="summary-tile danger">
        <strong>{{ overdue.length }}</strong>
        <span>逾期</span>
      </div>
      <div class="summary-tile">
        <strong>{{ nextReminders.length }}</strong>
        <span>提醒</span>
      </div>
      <div class="summary-tile success">
        <strong>{{ recentCompleted.length }}</strong>
        <span>最近完成</span>
      </div>
    </div>

    <TaskSection title="今天要处理" :tasks="dueToday" empty="今天没有截止事项" v-bind="events" />
    <TaskSection title="已逾期" :tasks="overdue" empty="没有逾期事项" tone="danger" v-bind="events" />
    <TaskSection title="接下来提醒" :tasks="nextReminders" empty="暂无提醒" v-bind="events" />
    <TaskSection title="最近完成" :tasks="recentCompleted" empty="暂无完成记录" readonly v-bind="events" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import QuickTaskCreate from '../components/QuickTaskCreate.vue'
import TaskSection from './TaskSection.vue'

const props = defineProps({
  tasks: { type: Array, default: () => [] }
})
const emit = defineEmits(['created', 'detail', 'edit', 'complete', 'cancel'])
const events = {
  onDetail: task => emit('detail', task),
  onEdit: task => emit('edit', task),
  onComplete: task => emit('complete', task),
  onCancel: task => emit('cancel', task)
}

const active = computed(() => props.tasks.filter(task => task.status === 'TODO' || task.status === 'DOING'))
const completed = computed(() => props.tasks.filter(task => task.status === 'DONE'))
const overdue = computed(() => active.value.filter(task => task.dueAt && new Date(task.dueAt) < new Date()).slice(0, 6))
const dueToday = computed(() => active.value.filter(task => isToday(task.dueAt)).slice(0, 8))
const nextReminders = computed(() => active.value
  .filter(task => task.remindAt)
  .sort((a, b) => new Date(a.remindAt) - new Date(b.remindAt))
  .slice(0, 6))
const recentCompleted = computed(() => completed.value
  .sort((a, b) => new Date(b.completedAt || b.updatedAt) - new Date(a.completedAt || a.updatedAt))
  .slice(0, 5))

function isToday(value) {
  if (!value) return false
  const date = new Date(value)
  const now = new Date()
  return date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
}
</script>

