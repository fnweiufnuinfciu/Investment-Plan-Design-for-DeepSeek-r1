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
/* ── Design: "Midnight Trading Desk" ── */
:root {
  --bg: #0f1119;
  --bg-elevated: #161821;
  --card-bg: #fafaf9;
  --card-border: #e7e5e0;
  --header-bg: #0b0d14;
  --text: #1c1c1e;
  --text-muted: #7c7c82;
  --text-inverse: #e8e8ec;
  --accent: #c8a45c;
  --accent-hover: #dbb86e;
  --accent-soft: rgba(200,164,92,0.12);
  --accent-blue: #5b8def;
  --success: #4a9b6d;
  --warning: #d4a853;
  --danger: #c4554d;
  --border: #e4e4e0;
  --border-dark: #252836;
  --radius: 10px;
  --radius-sm: 6px;
  --font-display: 'Georgia', 'Noto Serif SC', 'STSong', serif;
  --font-body: 'PingFang SC', 'Microsoft YaHei', 'Hiragino Sans GB', sans-serif;
  --font-num: 'SF Mono', 'JetBrains Mono', 'Consolas', 'Courier New', monospace;
  --shadow-card: 0 1px 0 var(--card-border), 0 2px 8px rgba(0,0,0,0.04);
  --shadow-elevated: 0 2px 0 var(--card-border), 0 4px 20px rgba(0,0,0,0.08);
  --transition-fast: 160ms ease;
  --transition-smooth: 320ms cubic-bezier(0.4,0,0.2,1);
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: var(--font-body);
  background: var(--bg);
  color: var(--text);
  -webkit-font-smoothing: antialiased;
  background-image:
    radial-gradient(ellipse at 20% 20%, rgba(200,164,92,0.04) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 60%, rgba(91,141,239,0.03) 0%, transparent 50%);
  background-attachment: fixed;
}

/* ── Scrollbar ── */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #2a2d3a; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #3d4050; }

/* ── Header ── */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--header-bg);
  padding: 0 28px;
  height: 56px;
  border-bottom: 1px solid #1e2030;
  position: relative;
}
.app-header::after {
  content: '';
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(200,164,92,0.3) 20%, rgba(200,164,92,0.3) 80%, transparent);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e8e8ec;
  font-weight: 700;
  font-size: 1rem;
  letter-spacing: 0.02em;
  white-space: nowrap;
}
.logo .el-icon { color: var(--accent); }

.header-menu {
  border-bottom: none !important;
  background: transparent !important;
}
.header-menu .el-menu-item {
  color: rgba(232,232,236,0.65) !important;
  font-weight: 500;
  font-size: 0.9rem;
  letter-spacing: 0.03em;
  border-bottom: 2px solid transparent !important;
  transition: color var(--transition-fast), border-color var(--transition-fast);
}
.header-menu .el-menu-item:hover {
  color: #e8e8ec !important;
  background: transparent !important;
  border-bottom-color: rgba(200,164,92,0.5) !important;
}
.header-menu .el-menu-item.is-active {
  color: var(--accent) !important;
  background: transparent !important;
  border-bottom-color: var(--accent) !important;
}

.el-main {
  padding: 28px 28px 48px;
  max-width: 1440px;
  margin: 0 auto;
}

/* ── Override Element Plus buttons ── */
.el-button--primary {
  --el-button-bg-color: var(--accent);
  --el-button-border-color: var(--accent);
  --el-button-hover-bg-color: var(--accent-hover);
  --el-button-hover-border-color: var(--accent-hover);
  --el-button-active-bg-color: #b8933e;
  --el-button-active-border-color: #b8933e;
  font-weight: 600;
  letter-spacing: 0.03em;
}
.el-button {
  transition: all var(--transition-fast);
}

/* ── Override tags ── */
.el-tag--success {
  background: rgba(74,155,109,0.12);
  border-color: rgba(74,155,109,0.3);
  color: var(--success);
}
.el-tag--danger {
  background: rgba(196,85,77,0.12);
  border-color: rgba(196,85,77,0.3);
  color: var(--danger);
}
.el-tag--warning {
  background: rgba(212,168,83,0.12);
  border-color: rgba(212,168,83,0.3);
  color: var(--warning);
}

/* ── Override tabs ── */
.el-tabs__item.is-active { color: var(--accent) !important; }
.el-tabs__active-bar { background-color: var(--accent) !important; }
.el-tabs__item:hover { color: var(--accent) !important; }

/* ── Override form focus ── */
.el-input__wrapper.is-focus,
.el-textarea__inner:focus {
  box-shadow: 0 0 0 1px var(--accent) inset !important;
}
.el-select .el-input.is-focus .el-input__wrapper {
  box-shadow: 0 0 0 1px var(--accent) inset !important;
}

/* ── Card (global utility) ── */
.card {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  padding: 24px;
  transition: box-shadow var(--transition-smooth);
}
.card:hover {
  box-shadow: var(--shadow-elevated);
}

/* ── Page entrance animation ── */
@keyframes pageIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.router-view-enter-active { animation: pageIn 0.4s ease both; }

/* ── Override tables ── */
.el-table {
  --el-table-border-color: var(--border, #e4e4e0);
  --el-table-header-bg-color: #f5f4f1;
  --el-table-row-hover-bg-color: rgba(200,164,92,0.04);
  font-size: 0.85rem;
}
.el-table th.el-table__cell {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 700;
  color: var(--text-muted, #7c7c82);
}
.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background-color: #fafaf8;
}

/* ── Override alert ── */
.el-alert--success {
  background: rgba(74,155,109,0.08);
  border: 1px solid rgba(74,155,109,0.2);
}
.el-alert--warning {
  background: rgba(212,168,83,0.08);
  border: 1px solid rgba(212,168,83,0.2);
}

/* ── Override input number ── */
.el-input-number .el-input__wrapper {
  border-color: var(--border, #e4e4e0);
}

/* ── Override select dropdown ── */
.el-select-dropdown__item.is-selected {
  color: var(--accent, #c8a45c);
  font-weight: 600;
}

.user-area { cursor: pointer; }
.user-tag { color: rgba(232,232,236,0.85); display: flex; align-items: center; gap: 6px; font-size: 0.85rem; }
.user-tag:hover { color: #e8e8ec; }
</style>
