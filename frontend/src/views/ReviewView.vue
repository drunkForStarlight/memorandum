<template>
  <div class="flow-page">
    <div class="summary-strip">
      <div class="summary-tile success">
        <strong>{{ completedThisWeek }}</strong>
        <span>本周完成</span>
      </div>
      <div class="summary-tile">
        <strong>{{ activeCount }}</strong>
        <span>未完成</span>
      </div>
      <div class="summary-tile danger">
        <strong>{{ overdueCount }}</strong>
        <span>逾期</span>
      </div>
    </div>
    <ReportView ref="reportView" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import ReportView from './ReportView.vue'

const props = defineProps({
  tasks: { type: Array, default: () => [] }
})
const reportView = ref(null)
defineExpose({ generate: () => reportView.value?.generate() })

const active = computed(() => props.tasks.filter(task => task.status === 'TODO' || task.status === 'DOING'))
const activeCount = computed(() => active.value.length)
const overdueCount = computed(() => active.value.filter(task => task.dueAt && new Date(task.dueAt) < new Date()).length)
const completedThisWeek = computed(() => props.tasks.filter(task => {
  if (task.status !== 'DONE' || !task.completedAt) return false
  const completed = new Date(task.completedAt)
  const start = startOfWeek()
  return completed >= start
}).length)

function startOfWeek() {
  const date = new Date()
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  date.setHours(0, 0, 0, 0)
  return date
}
</script>

