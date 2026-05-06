<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="28"><TrendCharts /></el-icon>
        <h2>DeepSeek-R1 智能投资系统</h2>
        <p>请登录以管理报告和查看历史方案</p>
      </div>
      <el-tabs v-model="tab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="form" :rules="loginRules" ref="loginRef" size="large" @submit.prevent="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-button type="primary" :loading="loading" native-type="submit" style="width:100%">
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form>
          <p v-if="loginError" class="error-text">{{ loginError }}</p>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="form" :rules="regRules" ref="regRef" size="large" @submit.prevent="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名（至少3位）" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="form.email" placeholder="邮箱（选填）" prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-button type="success" :loading="loading" native-type="submit" style="width:100%">
              {{ loading ? '注册中...' : '注 册' }}
            </el-button>
          </el-form>
          <p v-if="loginError" class="error-text">{{ loginError }}</p>
        </el-tab-pane>
      </el-tabs>
      <p class="hint">分析功能无需登录即可使用，登录后可管理报告及查看历史方案</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const tab = ref('login')
const loading = ref(false)
const loginError = ref('')
const loginRef = ref(null)
const regRef = ref(null)

const form = reactive({ username: '', password: '', email: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名至少3位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  loginError.value = ''
  try {
    await loginRef.value.validate()
  } catch { return }
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    router.push('/')
  } catch (e) {
    loginError.value = e.response?.data?.error || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  loginError.value = ''
  try {
    await regRef.value.validate()
  } catch { return }
  loading.value = true
  try {
    await auth.register(form.username, form.password, form.email)
    router.push('/')
  } catch (e) {
    loginError.value = e.response?.data?.error || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex; justify-content: center; align-items: center;
  min-height: calc(100vh - 120px);
}
.login-card {
  width: 420px; background: #fff; border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08); padding: 40px;
}
.login-header { text-align: center; margin-bottom: 24px; }
.login-header h2 { font-size: 1.2rem; margin: 8px 0 4px; }
.login-header p { color: #6b7280; font-size: 0.85rem; }
.login-tabs { margin-top: 8px; }
.error-text { color: #f56c6c; font-size: 0.85rem; margin-top: 8px; text-align: center; }
.hint { text-align: center; color: #9ca3af; font-size: 0.75rem; margin-top: 16px; }
</style>
