<template>
  <section class="flow-section" :class="tone">
    <header class="section-head">
      <h2>{{ title }}</h2>
      <span>{{ tasks.length }}</span>
    </header>
    <div v-if="tasks.length" class="task-list">
      <TaskListItem
        v-for="task in tasks"
        :key="task.id"
        :task="task"
        @detail="$emit('detail', $event)"
        @edit="$emit('edit', $event)"
        @complete="$emit('complete', $event)"
        @cancel="$emit('cancel', $event)"
      />
    </div>
    <el-empty v-else :description="empty" />
  </section>
</template>

<script setup>
import TaskListItem from '../components/TaskListItem.vue'

defineProps({
  title: { type: String, required: true },
  tasks: { type: Array, default: () => [] },
  empty: { type: String, default: '暂无事项' },
  tone: { type: String, default: '' }
})
defineEmits(['detail', 'edit', 'complete', 'cancel'])
</script>

