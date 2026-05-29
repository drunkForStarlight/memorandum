<template>
  <el-drawer :model-value="modelValue" :title="task ? '编辑事项' : '高级新建'" size="520px" @close="$emit('update:modelValue', false)">
    <el-form :model="form" label-position="top" class="editor-form">
      <el-form-item label="事项">
        <el-input
          v-model="form.title"
          class="editor-title-input"
          maxlength="120"
          show-word-limit
          placeholder="输入要做的事"
          @keyup.enter="save"
        />
      </el-form-item>

      <el-form-item label="截止">
        <div class="option-stack">
          <el-radio-group v-model="duePreset" @change="applyDuePreset">
            <el-radio-button label="NONE">无截止</el-radio-button>
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
            @change="applyReminderPreset"
          />
        </div>
      </el-form-item>

      <el-form-item label="提醒">
        <div class="option-stack">
          <el-radio-group v-model="reminderPreset" @change="applyReminderPreset">
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
          />
        </div>
      </el-form-item>

      <el-form-item label="分类">
        <div class="category-row">
          <el-radio-group v-model="form.category" @change="customCategory = ''">
            <el-radio-button v-for="item in categories" :key="item" :label="item">{{ item }}</el-radio-button>
          </el-radio-group>
          <el-input
            v-model="customCategory"
            class="category-custom-input"
            placeholder="其他分类"
            clearable
            @input="form.category = customCategory"
          />
        </div>
      </el-form-item>

      <el-form-item label="优先级">
        <el-radio-group v-model="form.priority">
          <el-radio-button label="LOW">低</el-radio-button>
          <el-radio-button label="MEDIUM">中</el-radio-button>
          <el-radio-button label="HIGH">高</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="task" label="状态">
        <el-select v-model="form.status">
          <el-option label="待办" value="TODO" />
          <el-option label="进行中" value="DOING" />
          <el-option label="已完成" value="DONE" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>
      </el-form-item>

      <el-collapse v-model="openPanels" class="description-collapse">
        <el-collapse-item title="描述" name="description">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="补充背景、处理要求或备注" />
        </el-collapse-item>
      </el-collapse>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'

const props = defineProps({
  modelValue: Boolean,
  task: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])
const categories = ['工作', '生活', '学习', '健康']
const saving = ref(false)
const duePreset = ref('NONE')
const reminderPreset = ref('NONE')
const customCategory = ref('')
const openPanels = ref([])
const form = reactive({
  title: '',
  description: '',
  category: '工作',
  priority: 'MEDIUM',
  status: 'TODO',
  dueAt: null,
  remindAt: null
})

watch(() => props.modelValue, visible => {
  if (!visible) return
  const category = props.task?.category || '工作'
  Object.assign(form, {
    title: props.task?.title || '',
    description: props.task?.description || '',
    category,
    priority: props.task?.priority || 'MEDIUM',
    status: props.task?.status || 'TODO',
    dueAt: props.task?.dueAt || null,
    remindAt: props.task?.remindAt || null
  })
  customCategory.value = categories.includes(category) ? '' : category
  duePreset.value = form.dueAt ? 'CUSTOM' : 'NONE'
  reminderPreset.value = form.remindAt ? 'CUSTOM' : 'NONE'
  openPanels.value = form.description ? ['description'] : []
})

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('先写下要做的事')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      title: form.title.trim(),
      status: props.task ? form.status : 'TODO'
    }
    if (props.task?.id) {
      await api.put(`/tasks/${props.task.id}`, payload)
    } else {
      await api.post('/tasks', payload)
    }
    ElMessage.success('已保存')
    emit('saved')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
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
  if (preset === 'NONE') {
    return null
  }
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

