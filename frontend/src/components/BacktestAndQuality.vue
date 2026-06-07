<template>
  <el-row :gutter="16" style="margin-top:20px">
    <el-col :span="12">
      <div class="info-card">
        <h3>回测指标</h3>
        <el-row :gutter="8">
          <el-col :span="8" v-for="m in metrics" :key="m.label">
            <div class="metric"><div class="metric-label">{{ m.label }}</div><div class="metric-value">{{ m.value }}</div></div>
          </el-col>
        </el-row>
      </div>
    </el-col>
    <el-col :span="12">
      <div class="info-card">
        <h3>数据质量诊断</h3>
        <div class="metric"><div class="metric-label">数据质量评分</div><div class="metric-value">{{ fmt(result.dataQuality?.qualityScore, 3) }}</div></div>
        <div v-if="result.dataQuality?.warnings?.length" style="margin-top:8px">
          <el-alert v-for="w in result.dataQuality.warnings" :key="w" :title="w" type="warning" :closable="false" style="margin-bottom:4px"/>
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script setup>
import { computed } from 'vue'
import { fmt, pct } from '@/utils/format'

const props = defineProps({
  result: { type: Object, default: null },
})

const metrics = computed(() => {
  const m = props.result?.backtest?.metrics
  if (!m) return []
  return [
    { label: '累计收益', value: pct(m.cumulativeReturn) },
    { label: '夏普比率', value: fmt(m.sharpe, 3) },
    { label: '最大回撤', value: pct(m.maxDrawdown) },
    { label: '日胜率', value: pct(m.dailyWinRate) },
    { label: '年化收益', value: pct(m.annualizedReturn) },
    { label: '索提诺比率', value: fmt(m.sortino, 3) },
    { label: 'VaR(95%)', value: pct(m.var95) },
    { label: 'CVaR(95%)', value: pct(m.cvar95) },
    { label: '回测天数', value: m.days },
  ]
})
</script>

<style scoped>
.info-card {
  background: var(--card-bg, #fafaf9);
  border: 1px solid var(--card-border, #e7e5e0);
  border-radius: var(--radius, 10px);
  padding: 24px;
  box-shadow: 0 1px 0 var(--card-border, #e7e5e0), 0 2px 8px rgba(0,0,0,0.04);
  transition: box-shadow 320ms cubic-bezier(0.4,0,0.2,1);
}
.info-card:hover {
  box-shadow: 0 2px 0 var(--card-border, #e7e5e0), 0 4px 20px rgba(0,0,0,0.08);
}
.info-card h3 {
  font-family: var(--font-display, serif);
  font-size: 0.95rem;
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: 0.03em;
  color: var(--text, #1c1c1e);
}
.metric { padding: 8px 4px; }
.metric-label { font-size: 0.68rem; text-transform: uppercase; color: var(--text-muted, #7c7c82); letter-spacing: 0.06em; font-weight: 600; }
.metric-value { font-family: var(--font-num, monospace); font-size: 1.1rem; font-weight: 700; color: var(--text, #1c1c1e); }
</style>
