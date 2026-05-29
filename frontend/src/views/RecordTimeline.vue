<template>
  <div class="flow-page">
    <section class="record-toolbar">
      <el-input v-model="keyword" placeholder="搜索完成记录" clearable />
      <el-button :icon="Download" @click="$emit('export')">导出 CSV</el-button>
    </section>

    <section class="flow-section">
      <header class="section-head">
        <h2>完成记录</h2>
        <span>{{ filtered.length }}</span>
      </header>
      <div v-if="filtered.length" class="timeline-list">
        <TaskListItem
          v-for="task in filtered"
          :key="task.id"
          :task="task"
          @detail="$emit('detail', $event)"
          @edit="$emit('edit', $event)"
          @complete="$emit('complete', $event)"
          @cancel="$emit('cancel', $event)"
        />
      </div>
      <el-empty v-else description="暂无完成记录" />
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import TaskListItem from '../components/TaskListItem.vue'

const props = defineProps({
  tasks: { type: Array, default: () => [] }
})
defineEmits(['detail', 'edit', 'complete', 'cancel', 'export'])
const keyword = ref('')
const completed = computed(() => props.tasks
  .filter(task => task.status === 'DONE')
  .sort((a, b) => new Date(b.completedAt || b.updatedAt) - new Date(a.completedAt || a.updatedAt)))
const filtered = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return completed.value
  return completed.value.filter(task => [task.title, task.description, task.category]
    .some(value => (value || '').toLowerCase().includes(key)))
})
</script>

