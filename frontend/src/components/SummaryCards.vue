<template>
  <el-row :gutter="16" style="margin-top:20px">
    <el-col :span="4" v-for="c in cards" :key="c.title">
      <div class="stat-card">
        <div class="stat-title">{{ c.title }}</div>
        <div class="stat-value">{{ c.value }}</div>
      </div>
    </el-col>
  </el-row>
</template>

<script setup>
import { computed } from 'vue'
import { fmt } from '@/utils/format'

const props = defineProps({
  summary: { type: Object, default: null },
})

const cards = computed(() => {
  const s = props.summary
  if (!s) return []
  return [
    { title: '候选池', value: s.universeSize },
    { title: '持仓数', value: s.selectedPositions },
    { title: '多/空', value: `${s.longPositions}/${s.shortPositions}` },
    { title: '总敞口', value: fmt(s.grossExposure, 4) },
    { title: '净敞口', value: fmt(s.netExposure, 4) },
    { title: '已过滤', value: s.filteredOut },
  ]
})
</script>

<style scoped>
.stat-card {
  background: var(--card-bg, #fafaf9);
  border: 1px solid var(--card-border, #e7e5e0);
  border-radius: var(--radius, 10px);
  padding: 18px 14px;
  text-align: center;
  box-shadow: 0 1px 0 var(--card-border, #e7e5e0), 0 2px 8px rgba(0,0,0,0.04);
  position: relative;
  overflow: hidden;
  transition: all 320ms cubic-bezier(0.4,0,0.2,1);
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 12px; right: 12px;
  height: 2px;
  border-radius: 0 0 3px 3px;
  background: linear-gradient(90deg, transparent, var(--accent, #c8a45c) 20%, var(--accent, #c8a45c) 80%, transparent);
  opacity: 0;
  transition: opacity 320ms ease;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}
.stat-card:hover::before { opacity: 1; }
.stat-title {
  font-size: 0.7rem;
  text-transform: uppercase;
  color: var(--text-muted, #7c7c82);
  letter-spacing: 0.08em;
  font-weight: 600;
}
.stat-value {
  font-family: var(--font-num, monospace);
  font-size: 1.5rem;
  font-weight: 700;
  margin-top: 6px;
  color: var(--text, #1c1c1e);
}
</style>
