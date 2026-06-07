<template>
  <div class="table-card" style="margin-top:20px">
    <div class="table-header">
      <h2>策略对比</h2>
      <el-button size="small" type="primary" :loading="comparing" @click="$emit('run')">生成对比</el-button>
    </div>
    <template v-if="comparisonData">
      <el-row :gutter="16">
        <el-col :span="16">
          <el-table :data="comparisonData.strategies" size="small" stripe>
            <el-table-column prop="strategyLabel" label="策略"/>
            <el-table-column prop="positionCount" label="持仓数"/>
            <el-table-column prop="longCount" label="做多"/>
            <el-table-column prop="shortCount" label="做空"/>
            <el-table-column prop="grossExposure" label="总敞口">
              <template #default="{row}">{{ fmt(row.grossExposure, 4) }}</template>
            </el-table-column>
            <el-table-column prop="netExposure" label="净敞口">
              <template #default="{row}">{{ fmt(row.netExposure, 4) }}</template>
            </el-table-column>
            <el-table-column prop="expectedReturn60d" label="预期收益">
              <template #default="{row}">{{ pct(row.expectedReturn60d) }}</template>
            </el-table-column>
            <el-table-column prop="maxDrawdown" label="最大回撤">
              <template #default="{row}">{{ pct(row.maxDrawdown) }}</template>
            </el-table-column>
          </el-table>
          <el-alert v-if="comparisonData.best" type="success" :closable="false" style="margin-top:12px">
            最优策略：<strong>{{ comparisonData.best }}</strong> — {{ comparisonData.reason }}
          </el-alert>
        </el-col>
        <el-col :span="8">
          <v-chart :option="chartOptions.comparisonBarOption" style="height:260px" autoresize />
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import VChart from 'vue-echarts'
import { fmt, pct } from '@/utils/format'

defineProps({
  comparisonData: { type: Object, default: null },
  comparing: { type: Boolean, default: false },
  chartOptions: { type: Object, required: true },
})
defineEmits(['run'])
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
