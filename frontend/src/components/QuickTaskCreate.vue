<template>
  <section class="quick-capture">
    <div class="capture-line">
      <el-input
        v-model="form.title"
        size="large"
        placeholder="记一件事"
        maxlength="120"
        clearable
        @keyup.enter="create"
      />
      <el-button :icon="Operation" size="large" @click="expanded = !expanded" />
      <el-button type="primary" size="large" :icon="Plus" :loading="saving" @click="create">创建</el-button>
    </div>

    <div v-if="expanded" class="capture-options">
      <div class="quick-row">
        <span class="quick-label">截止</span>
        <el-radio-group v-model="duePreset" size="small" @change="applyDuePreset">
          <el-radio-button label="NONE">无</el-radio-button>
          <el-radio-button label="TODAY">今天</el-radio-button>
          <el-radio-button label="TOMORROW">明天</el-radio-button>
          <el-radio-button label="THIS_WEEK">本周</el-radio-button>
          <el-radio-button label="CUSTOM">自定义</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-if="duePreset === 'CUSTOM'"
          v-model="form.dueAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          size="small"
          @change="applyReminderPreset"
        />
      </div>

      <div class="quick-row">
        <span class="quick-label">提醒</span>
        <el-radio-group v-model="reminderPreset" size="small" @change="applyReminderPreset">
          <el-radio-button label="NONE">不提醒</el-radio-button>
          <el-radio-button label="AT_DUE" :disabled="!form.dueAt">到期时</el-radio-button>
          <el-radio-button label="ONE_HOUR" :disabled="!form.dueAt">提前1小时</el-radio-button>
          <el-radio-button label="ONE_DAY" :disabled="!form.dueAt">提前1天</el-radio-button>
          <el-radio-button label="CUSTOM">自定义</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-if="reminderPreset === 'CUSTOM'"
          v-model="form.remindAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          size="small"
        />
      </div>

      <div class="quick-row">
        <span class="quick-label">分类</span>
        <el-radio-group v-model="form.category" size="small" @change="customCategory = ''">
          <el-radio-button label="">无</el-radio-button>
          <el-radio-button v-for="item in categories" :key="item" :label="item">{{ item }}</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="customCategory"
          class="quick-category-input"
          size="small"
          placeholder="其他"
          clearable
          @input="form.category = customCategory"
        />
        <span class="quick-label">优先级</span>
        <el-radio-group v-model="form.priority" size="small">
          <el-radio-button label="MEDIUM">普通</el-radio-button>
          <el-radio-button label="HIGH">高</el-radio-button>
          <el-radio-button label="LOW">低</el-radio-button>
        </el-radio-group>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Operation, Plus } from '@element-plus/icons-vue'
import { api } from '../api/client'

const emit = defineEmits(['created'])
const categories = ['工作', '生活', '学习', '健康']
const saving = ref(false)
const expanded = ref(false)
const duePreset = ref('NONE')
const reminderPreset = ref('NONE')
const customCategory = ref('')
const form = reactive({
  title: '',
  category: '',
  priority: 'MEDIUM',
  dueAt: null,
  remindAt: null
})

async function create() {
  if (!form.title.trim()) {
    ElMessage.warning('先写下要做的事')
    return
  }
  saving.value = true
  try {
    const { data } = await api.post('/tasks', {
      title: form.title.trim(),
      description: '',
      category: form.category || '',
      priority: form.priority,
      status: 'TODO',
      dueAt: form.dueAt,
      remindAt: form.remindAt
    })
    ElMessage.success('已创建')
    reset()
    emit('created', data)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

function reset() {
  form.title = ''
  form.dueAt = null
  form.remindAt = null
  form.category = ''
  form.priority = 'MEDIUM'
  duePreset.value = 'NONE'
  reminderPreset.value = 'NONE'
  customCategory.value = ''
}

function applyDuePreset() {
  if (duePreset.value === 'CUSTOM') {
    applyReminderPreset()
    return
  }
  form.dueAt = presetDueTime(duePreset.value)
  applyReminderPreset()
}

function applyReminderPreset() {
  if (!form.dueAt && reminderPreset.value !== 'CUSTOM') {
    reminderPreset.value = 'NONE'
  }
  if (reminderPreset.value === 'CUSTOM') {
    return
  }
  if (reminderPreset.value === 'NONE' || !form.dueAt) {
    form.remindAt = null
    return
  }
  const due = new Date(form.dueAt)
  if (reminderPreset.value === 'AT_DUE') {
    form.remindAt = form.dueAt
  } else if (reminderPreset.value === 'ONE_HOUR') {
    form.remindAt = formatDateTime(new Date(due.getTime() - 60 * 60 * 1000))
  } else if (reminderPreset.value === 'ONE_DAY') {
    form.remindAt = formatDateTime(new Date(due.getTime() - 24 * 60 * 60 * 1000))
  }
}

function presetDueTime(preset) {
  if (preset === 'NONE') return null
  const now = new Date()
  const date = new Date(now)
  if (preset === 'TODAY') {
    date.setHours(18, 0, 0, 0)
    if (date <= now) {
      date.setTime(now.getTime() + 60 * 60 * 1000)
      date.setMinutes(0, 0, 0)
    }
  } else if (preset === 'TOMORROW') {
    date.setDate(date.getDate() + 1)
    date.setHours(18, 0, 0, 0)
  } else if (preset === 'THIS_WEEK') {
    const day = date.getDay() || 7
    const daysUntilFriday = Math.max(0, 5 - day)
    date.setDate(date.getDate() + daysUntilFriday)
    date.setHours(18, 0, 0, 0)
    if (date <= now) {
      date.setDate(date.getDate() + 2)
      date.setHours(18, 0, 0, 0)
    }
  }
  return formatDateTime(date)
}

function formatDateTime(date) {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
</script>
