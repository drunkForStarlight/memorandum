<template>
  <LoginView v-if="!user" @logged-in="loadMe" />
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">M</div>
        <div>
          <strong>备忘助手</strong>
          <span>{{ user.displayName }}</span>
        </div>
      </div>

      <el-menu :default-active="view" class="nav-menu" @select="selectView">
        <el-menu-item index="today"><el-icon><Calendar /></el-icon><span>今日</span></el-menu-item>
        <el-menu-item index="unscheduled"><el-icon><Box /></el-icon><span>待安排</span></el-menu-item>
        <el-menu-item index="plan"><el-icon><List /></el-icon><span>计划</span></el-menu-item>
        <el-menu-item index="records"><el-icon><Collection /></el-icon><span>记录</span></el-menu-item>
        <el-menu-item index="review"><el-icon><Notebook /></el-icon><span>回顾</span></el-menu-item>
        <el-menu-item index="settings"><el-icon><Setting /></el-icon><span>设置</span></el-menu-item>
      </el-menu>

      <div class="sidebar-actions">
        <el-button :icon="Download" @click="downloadCsv">导出 CSV</el-button>
        <el-button :icon="SwitchButton" plain @click="logout">退出</el-button>
      </div>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageHint }}</p>
        </div>
        <el-button v-if="view !== 'review' && view !== 'settings'" :icon="EditPen" @click="openCreate">高级新建</el-button>
        <el-button v-else-if="view === 'review'" type="primary" :icon="DocumentAdd" @click="generateReport">生成本周周报</el-button>
      </header>

      <el-skeleton v-if="loading" :rows="8" animated />
      <ActionCenter
        v-else-if="view === 'today'"
        :tasks="tasks"
        @created="afterCreated"
        @detail="openDetail"
        @edit="openEdit"
        @complete="completeTask"
        @cancel="cancelTask"
      />
      <TaskFlowView
        v-else-if="view === 'unscheduled'"
        mode="unscheduled"
        :tasks="tasks"
        @created="afterCreated"
        @detail="openDetail"
        @edit="openEdit"
        @complete="completeTask"
        @cancel="cancelTask"
      />
      <TaskFlowView
        v-else-if="view === 'plan'"
        mode="plan"
        :tasks="tasks"
        @created="afterCreated"
        @detail="openDetail"
        @edit="openEdit"
        @complete="completeTask"
        @cancel="cancelTask"
      />
      <RecordTimeline
        v-else-if="view === 'records'"
        :tasks="tasks"
        @detail="openDetail"
        @edit="openEdit"
        @complete="completeTask"
        @cancel="cancelTask"
        @export="downloadCsv"
      />
      <ReviewView v-else-if="view === 'review'" ref="reviewView" :tasks="tasks" />
      <SettingsView v-else />
    </main>

    <TaskEditor
      v-model="editorVisible"
      :task="editingTask"
      @saved="afterSaved"
    />
    <TaskDetail
      v-model="detailVisible"
      :task-id="detailTaskId"
      @changed="afterSaved"
      @edit="openEdit"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Box, Calendar, Collection, DocumentAdd, Download, EditPen, List, Notebook, Setting, SwitchButton } from '@element-plus/icons-vue'
import { api, ensureCsrf } from './api/client'
import LoginView from './views/LoginView.vue'
import ActionCenter from './views/ActionCenter.vue'
import TaskFlowView from './views/TaskFlowView.vue'
import RecordTimeline from './views/RecordTimeline.vue'
import ReviewView from './views/ReviewView.vue'
import SettingsView from './views/SettingsView.vue'
import TaskEditor from './components/TaskEditor.vue'
import TaskDetail from './components/TaskDetail.vue'

const user = ref(null)
const view = ref('today')
const tasks = ref([])
const loading = ref(false)
const editorVisible = ref(false)
const detailVisible = ref(false)
const editingTask = ref(null)
const detailTaskId = ref(null)
const reviewView = ref(null)

const titles = {
  today: ['今日', '现在需要关注的事项'],
  unscheduled: ['待安排', '还没设置时间的事项'],
  plan: ['计划', '按时间推进未完成事项'],
  records: ['记录', '过去完成的事项和处理痕迹'],
  review: ['回顾', '周报、统计和阶段复盘'],
  settings: ['设置', '邮件发送、提醒和周报配置']
}
const pageTitle = computed(() => titles[view.value]?.[0] || '备忘助手')
const pageHint = computed(() => titles[view.value]?.[1] || '')

onMounted(async () => {
  await ensureCsrf()
  await loadMe()
})

async function loadMe() {
  try {
    const { data } = await api.get('/auth/me')
    user.value = data
    await loadTasks()
  } catch {
    user.value = null
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const { data } = await api.get('/tasks', { params: { view: 'all' } })
    tasks.value = data
  } finally {
    loading.value = false
  }
}

function selectView(next) {
  view.value = next
}

function openCreate() {
  editingTask.value = null
  editorVisible.value = true
}

function openEdit(task) {
  editingTask.value = task
  detailVisible.value = false
  editorVisible.value = true
}

function openDetail(task) {
  detailTaskId.value = task.id
  detailVisible.value = true
}

async function afterCreated(task) {
  await loadTasks()
  if (view.value === 'records' || view.value === 'review') {
    view.value = 'unscheduled'
    return
  }
  if (view.value === 'today' && !isToday(task?.dueAt)) {
    view.value = task?.dueAt ? 'plan' : 'unscheduled'
  }
}

async function afterSaved() {
  editorVisible.value = false
  await loadTasks()
}

async function completeTask(task) {
  const { value } = await ElMessageBox.prompt('可以补充一段完成总结', `完成：${task.title}`, {
    inputType: 'textarea',
    confirmButtonText: '完成',
    cancelButtonText: '取消',
    inputPlaceholder: '这件事怎么处理的？可以留空'
  }).catch(() => ({ value: null }))
  if (value === null) return
  await api.post(`/tasks/${task.id}/complete`, { summary: value })
  ElMessage.success('已完成并归档')
  await loadTasks()
}

async function cancelTask(task) {
  await ElMessageBox.confirm(`确认取消「${task.title}」？`, '取消事项', { type: 'warning' })
  await api.post(`/tasks/${task.id}/cancel`)
  ElMessage.success('已取消')
  await loadTasks()
}

async function generateReport() {
  await reviewView.value?.generate()
}

function downloadCsv() {
  window.location.href = '/api/tasks/export.csv'
}

async function logout() {
  await api.post('/auth/logout')
  user.value = null
}

function isToday(value) {
  if (!value) return false
  const date = new Date(value)
  const now = new Date()
  return date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
}
</script>
