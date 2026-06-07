<template>
  <div class="table-card" style="margin-top:20px">
    <div class="table-header"><h2>持仓明细</h2></div>
    <el-table :data="positions" stripe size="small" max-height="400">
      <el-table-column prop="ticker" label="代码" width="100"/>
      <el-table-column prop="side" label="方向" width="80">
        <template #default="{row}">
          <el-tag :type="row.side==='LONG'?'success':'danger'" size="small">{{ row.side==='LONG'?'做多':'做空' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="recommendationScore" label="评级分" width="65"/>
      <el-table-column prop="confidence" label="置信度" width="80">
        <template #default="{row}">{{ fmt(row.confidence, 3) }}</template>
      </el-table-column>
      <el-table-column prop="objectiveRatio" label="客观占比" width="80">
        <template #default="{row}">{{ fmt(row.objectiveRatio, 3) }}</template>
      </el-table-column>
      <el-table-column prop="targetWeight" label="权重" width="90">
        <template #default="{row}">{{ fmt(row.targetWeight, 4) }}</template>
      </el-table-column>
      <el-table-column prop="targetDollar" label="金额($)" width="110">
        <template #default="{row}">{{ fmt(row.targetDollar, 2) }}</template>
      </el-table-column>
      <el-table-column prop="signalScore" label="信号值" width="100">
        <template #default="{row}">{{ fmt(row.signalScore, 4) }}</template>
      </el-table-column>
      <el-table-column prop="notes" label="标签" min-width="180">
        <template #default="{row}">
          <el-tag v-for="n in (row.notes||[])" :key="n" size="small" style="margin-right:4px">{{ noteLabel(n) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { fmt, noteLabel } from '@/utils/format'

defineProps({
  positions: { type: Array, default: () => [] },
})
</script>

<style scoped>
.table-card {
  background: var(--card-bg, #fafaf9);
  border: 1px solid var(--card-border, #e7e5e0);
  border-radius: var(--radius, 10px);
  padding: 24px;
  box-shadow: 0 1px 0 var(--card-border, #e7e5e0), 0 2px 8px rgba(0,0,0,0.04);
  transition: box-shadow 320ms cubic-bezier(0.4,0,0.2,1);
}
.table-card:hover {
  box-shadow: 0 2px 0 var(--card-border, #e7e5e0), 0 4px 20px rgba(0,0,0,0.08);
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border, #e4e4e0);
}
.table-header h2 {
  font-family: var(--font-display, serif);
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.03em;
}
</style>
