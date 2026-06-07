<template>
  <el-row :gutter="16" style="margin-top:20px">
    <el-col :span="12">
      <div class="info-card">
        <h3>压力测试</h3>
        <el-table :data="result.stressTest?.scenarios || []" size="small">
          <el-table-column prop="scenario" label="压力情景"/>
          <el-table-column prop="assumedMarketMove" label="假定市场波动">
            <template #default="{row}">{{ pct(row.assumedMarketMove) }}</template>
          </el-table-column>
          <el-table-column prop="portfolioPnl" label="组合盈亏($)">
            <template #default="{row}">{{ fmt(row.portfolioPnl, 2) }}</template>
          </el-table-column>
          <el-table-column prop="portfolioReturn" label="组合收益率">
            <template #default="{row}">{{ pct(row.portfolioReturn) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-col>
    <el-col :span="12">
      <div class="info-card">
        <h3>可解释性（Top 贡献）</h3>
        <el-table :data="result.explainability?.topSignalContributors || []" size="small">
          <el-table-column prop="ticker" label="代码"/>
          <el-table-column prop="side" label="方向">
            <template #default="{row}">
              <el-tag :type="row.side==='LONG'?'success':'danger'" size="small">{{ row.side==='LONG'?'做多':'做空' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="contributionScore" label="贡献度">
            <template #default="{row}">{{ fmt(row.contributionScore, 4) }}</template>
          </el-table-column>
          <el-table-column prop="signalScore" label="信号值">
            <template #default="{row}">{{ fmt(row.signalScore, 4) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-col>
  </el-row>
</template>

<script setup>
import { fmt, pct } from '@/utils/format'

defineProps({
  result: { type: Object, default: null },
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
</style>
