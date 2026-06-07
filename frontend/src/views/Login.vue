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
      <div class="guest-zone">
        <el-divider>或</el-divider>
        <el-button size="large" @click="$router.push('/')" style="width:100%">
          跳过登录，直接体验
        </el-button>
      </div>
      <p class="hint">示例账号：admin / admin123 | 不登录可直接使用策略分析功能</p>
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
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 120px);
  background:
    radial-gradient(ellipse at 30% 20%, rgba(200,164,92,0.06) 0%, transparent 55%),
    radial-gradient(ellipse at 70% 70%, rgba(91,141,239,0.05) 0%, transparent 50%);
}
.login-card {
  width: 440px;
  background: var(--card-bg, #fafaf9);
  border: 1px solid var(--card-border, #e7e5e0);
  border-radius: 16px;
  box-shadow:
    0 2px 0 var(--card-border, #e7e5e0),
    0 8px 40px rgba(0,0,0,0.12);
  padding: 44px 40px 36px;
  animation: cardUp 0.6s cubic-bezier(0.16,1,0.3,1) both;
}
@keyframes cardUp {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-header { text-align: center; margin-bottom: 28px; }
.login-header .el-icon {
  color: var(--accent, #c8a45c);
  margin-bottom: 8px;
}
.login-header h2 {
  font-family: var(--font-display, serif);
  font-size: 1.25rem;
  margin: 8px 0 6px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--text, #1c1c1e);
}
.login-header p {
  color: var(--text-muted, #7c7c82);
  font-size: 0.85rem;
}

.login-tabs { margin-top: 8px; }

.error-text {
  color: var(--danger, #c4554d);
  font-size: 0.85rem;
  margin-top: 10px;
  text-align: center;
}

.hint {
  text-align: center;
  color: var(--text-muted, #7c7c82);
  font-size: 0.75rem;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--border, #e4e4e0);
  letter-spacing: 0.02em;
}
.guest-zone { margin-top: 20px; }
</style>
