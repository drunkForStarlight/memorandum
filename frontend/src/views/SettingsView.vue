<template>
  <div class="settings-page">
    <section class="settings-panel">
      <header class="section-head">
        <h2>邮件发送</h2>
        <span>{{ form.passwordSet ? '已保存密码' : '未保存密码' }}</span>
      </header>

      <el-form :model="form" label-position="top" class="settings-form">
        <div class="form-grid">
          <el-form-item label="SMTP 服务器">
            <el-input v-model="form.host" placeholder="smtp.qq.com" />
          </el-form-item>
          <el-form-item label="端口">
            <el-input-number v-model="form.port" :min="1" :max="65535" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="SMTP 用户名">
            <el-input v-model="form.username" placeholder="name@example.com" />
          </el-form-item>
          <el-form-item label="发件人">
            <el-input v-model="form.from" placeholder="name@example.com" />
          </el-form-item>
        </div>

        <el-form-item label="SMTP 密码 / 授权码">
          <div class="password-row">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="留空则不修改已保存的授权码"
            />
            <el-checkbox v-model="form.clearPassword">清空已保存密码</el-checkbox>
          </div>
        </el-form-item>

        <div class="toggle-row">
          <el-checkbox v-model="form.auth">需要认证</el-checkbox>
          <el-checkbox v-model="form.starttls">启用 STARTTLS</el-checkbox>
        </div>
      </el-form>
    </section>

    <section class="settings-panel">
      <header class="section-head">
        <h2>提醒与周报</h2>
        <span>页面保存后立即生效</span>
      </header>

      <el-form :model="form" label-position="top" class="settings-form">
        <el-form-item label="提醒收件人">
          <el-input v-model="form.reminderRecipients" placeholder="me@example.com，多人用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="周报收件人">
          <el-input v-model="form.weeklyRecipients" placeholder="me@example.com，多人用英文逗号分隔" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="自动发送周报">
            <el-switch v-model="form.weeklyEnabled" active-text="开启" inactive-text="关闭" />
          </el-form-item>
          <el-form-item label="周报时间">
            <div class="weekly-time-row">
              <el-select v-model="form.weeklyDay">
                <el-option v-for="day in weekDays" :key="day.value" :label="day.label" :value="day.value" />
              </el-select>
              <el-time-picker v-model="weeklyTimeValue" format="HH:mm" value-format="HH:mm" />
            </div>
          </el-form-item>
        </div>
      </el-form>
    </section>

    <section class="settings-panel">
      <header class="section-head">
        <h2>测试</h2>
        <span>保存配置后再发送测试</span>
      </header>
      <div class="test-row">
        <el-input v-model="testRecipient" placeholder="测试收件人邮箱" />
        <el-button :loading="testing" @click="sendTest">发送测试邮件</el-button>
      </div>
    </section>

    <div class="settings-actions">
      <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'

const saving = ref(false)
const testing = ref(false)
const testRecipient = ref('')
const weekDays = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' }
]
const form = reactive({
  host: '',
  port: 465,
  username: '',
  password: '',
  passwordSet: false,
  clearPassword: false,
  auth: true,
  starttls: false,
  from: '',
  reminderRecipients: '',
  weeklyRecipients: '',
  weeklyEnabled: true,
  weeklyDay: 5,
  weeklyTime: '18:00'
})

const weeklyTimeValue = computed({
  get: () => form.weeklyTime,
  set: value => {
    form.weeklyTime = value || '18:00'
  }
})

onMounted(load)

async function load() {
  const { data } = await api.get('/settings/mail')
  Object.assign(form, {
    ...data,
    password: '',
    clearPassword: false
  })
  testRecipient.value = data.reminderRecipients?.split(',')[0]?.trim() || data.weeklyRecipients?.split(',')[0]?.trim() || ''
}

async function save() {
  saving.value = true
  try {
    const { data } = await api.put('/settings/mail', {
      host: form.host,
      port: form.port,
      username: form.username,
      password: form.password,
      clearPassword: form.clearPassword,
      auth: form.auth,
      starttls: form.starttls,
      from: form.from,
      reminderRecipients: form.reminderRecipients,
      weeklyRecipients: form.weeklyRecipients,
      weeklyEnabled: form.weeklyEnabled,
      weeklyDay: form.weeklyDay,
      weeklyTime: form.weeklyTime
    })
    Object.assign(form, {
      ...data,
      password: '',
      clearPassword: false
    })
    ElMessage.success('邮件设置已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function sendTest() {
  if (!testRecipient.value.trim()) {
    ElMessage.warning('先填写测试收件人')
    return
  }
  testing.value = true
  try {
    const { data } = await api.post('/settings/mail/test', { recipient: testRecipient.value.trim() })
    if (data.ok) {
      ElMessage.success(data.message)
    } else {
      ElMessage.error(data.message)
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    testing.value = false
  }
}
</script>

