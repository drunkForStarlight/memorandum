<template>
  <el-drawer :model-value="modelValue" title="事项详情" size="520px" @close="$emit('update:modelValue', false)">
    <div v-if="detail" class="detail-stack">
      <section>
        <div class="detail-heading">
          <h2>{{ detail.task.title }}</h2>
          <el-button :icon="Edit" circle title="编辑" @click="$emit('edit', detail.task)" />
        </div>
        <p>{{ detail.task.description || '无描述' }}</p>
        <div class="detail-meta">
          <el-tag>{{ detail.task.category || '未分类' }}</el-tag>
          <el-tag type="primary">{{ detail.task.priority }}</el-tag>
          <el-tag type="success" v-if="detail.task.status === 'DONE'">已完成</el-tag>
          <el-tag v-else>{{ detail.task.status }}</el-tag>
        </div>
      </section>

      <section>
        <h3>追加处理记录</h3>
        <el-input v-model="logContent" type="textarea" :rows="3" placeholder="记录处理过程、阻塞点或结果" />
        <el-button type="primary" :icon="Plus" :loading="saving" @click="addLog">追加</el-button>
      </section>

      <section>
        <h3>处理时间线</h3>
        <el-timeline>
          <el-timeline-item v-for="log in detail.logs" :key="log.id" :timestamp="formatTime(log.createdAt)">
            {{ log.content }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="detail.logs.length === 0" description="暂无处理记录" />
      </section>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Plus } from '@element-plus/icons-vue'
import { api } from '../api/client'

const props = defineProps({
  modelValue: Boolean,
  taskId: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue', 'changed', 'edit'])
const detail = ref(null)
const logContent = ref('')
const saving = ref(false)

watch([() => props.modelValue, () => props.taskId], async ([visible]) => {
  if (visible && props.taskId) {
    await load()
  }
})

async function load() {
  const { data } = await api.get(`/tasks/${props.taskId}`)
  detail.value = data
}

async function addLog() {
  if (!logContent.value.trim()) {
    ElMessage.warning('处理记录不能为空')
    return
  }
  saving.value = true
  try {
    await api.post(`/tasks/${props.taskId}/logs`, { content: logContent.value })
    logContent.value = ''
    ElMessage.success('已追加')
    await load()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

function formatTime(value) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}
</script>

