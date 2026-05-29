<template>
  <div class="flow-page">
    <QuickTaskCreate v-if="mode !== 'records'" @created="$emit('created', $event)" />

    <template v-if="mode === 'unscheduled'">
      <TaskSection title="待安排事项" :tasks="unscheduled" empty="没有待安排事项" v-bind="events" />
    </template>

    <template v-else>
      <TaskSection title="逾期" :tasks="overdue" empty="没有逾期事项" tone="danger" v-bind="events" />
      <TaskSection title="今天" :tasks="today" empty="今天没有截止事项" v-bind="events" />
      <TaskSection title="未来" :tasks="future" empty="暂无未来计划" v-bind="events" />
      <TaskSection title="待安排" :tasks="noDate" empty="没有待安排事项" v-bind="events" />
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import QuickTaskCreate from '../components/QuickTaskCreate.vue'
import TaskSection from './TaskSection.vue'

const props = defineProps({
  tasks: { type: Array, default: () => [] },
  mode: { type: String, default: 'plan' }
})
const emit = defineEmits(['created', 'detail', 'edit', 'complete', 'cancel'])
const events = {
  onDetail: task => emit('detail', task),
  onEdit: task => emit('edit', task),
  onComplete: task => emit('complete', task),
  onCancel: task => emit('cancel', task)
}
const active = computed(() => props.tasks.filter(task => task.status === 'TODO' || task.status === 'DOING'))
const unscheduled = computed(() => active.value.filter(task => !task.dueAt && !task.remindAt))
const overdue = computed(() => active.value.filter(task => task.dueAt && new Date(task.dueAt) < new Date()))
const today = computed(() => active.value.filter(task => isToday(task.dueAt)))
const future = computed(() => active.value.filter(task => task.dueAt && new Date(task.dueAt) >= startOfTomorrow()))
const noDate = computed(() => active.value.filter(task => !task.dueAt))

function isToday(value) {
  if (!value) return false
  const date = new Date(value)
  const now = new Date()
  return date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
}

function startOfTomorrow() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  date.setHours(0, 0, 0, 0)
  return date
}
</script>
