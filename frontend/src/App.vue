<template>
  <div class="app-shell">
    <el-container>
      <el-header class="app-header">
        <div class="logo">
          <el-icon :size="24"><TrendCharts /></el-icon>
          <span>DeepSeek-R1 智能投资决策系统</span>
          <el-tag size="small" type="success" effect="dark" style="margin-left:12px">毕业设计版</el-tag>
        </div>
        <div style="display:flex;align-items:center;gap:16px">
          <el-menu
            mode="horizontal"
            :default-active="route.path"
            router
            background-color="transparent"
            class="header-menu"
          >
            <el-menu-item index="/">策略看板</el-menu-item>
            <el-menu-item index="/reports">报告管理</el-menu-item>
            <el-menu-item index="/portfolios">历史方案</el-menu-item>
          </el-menu>
          <div v-if="auth.isLoggedIn" class="user-area">
            <el-dropdown @command="handleUserCmd">
              <span class="user-tag">
                <el-icon><User /></el-icon>
                {{ auth.username }}
                <el-tag v-if="auth.isAdmin" size="small" type="warning" effect="plain" style="margin-left:4px">管理员</el-tag>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <el-button v-else size="small" type="primary" @click="$router.push('/login')">登录</el-button>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

function handleUserCmd(cmd) {
  if (cmd === 'logout') {
    auth.clearAuth()
    router.push('/')
  }
}
</script>

<style>
:root {
  --bg: #f5f7fa;
  --card-bg: #ffffff;
  --text: #1a1a2e;
  --text-muted: #6b7280;
  --accent: #409eff;
  --accent-strong: #1d6fa5;
  --success: #67c23a;
  --warning: #e6a23c;
  --danger: #f56c6c;
  --border: #e4e7ed;
  --radius: 12px;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg);
  color: var(--text);
  -webkit-font-smoothing: antialiased;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  padding: 0 24px;
  height: 56px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-weight: 700;
  font-size: 1.05rem;
  white-space: nowrap;
}

.header-menu {
  border-bottom: none !important;
}

.header-menu .el-menu-item {
  color: rgba(255,255,255,0.75) !important;
  font-weight: 500;
}

.header-menu .el-menu-item:hover,
.header-menu .el-menu-item.is-active {
  color: #fff !important;
  background: rgba(255,255,255,0.1) !important;
}

.el-main {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.card {
  background: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  padding: 20px;
}

.user-area { cursor: pointer; }
.user-tag { color: rgba(255,255,255,0.85); display: flex; align-items: center; gap: 4px; font-size: 0.9rem; }
.user-tag:hover { color: #fff; }
</style>
