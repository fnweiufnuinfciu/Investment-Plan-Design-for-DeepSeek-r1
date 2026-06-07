<template>
  <template v-if="result">
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="12">
        <div class="chart-card"><h3>持仓权重分布</h3><v-chart :option="chartOptions.weightPieOption" style="height:300px" autoresize /></div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card"><h3>信号值对比</h3><v-chart :option="chartOptions.signalBarOption" style="height:300px" autoresize /></div>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="12">
        <div class="chart-card"><h3>回测收益曲线（估算）</h3><v-chart :option="chartOptions.backtestCurveOption" style="height:300px" autoresize /></div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card"><h3>压力测试雷达图</h3><v-chart :option="chartOptions.stressRadarOption" style="height:300px" autoresize /></div>
      </el-col>
    </el-row>
  </template>
</template>

<script setup>
import VChart from 'vue-echarts'

defineProps({
  result: { type: Object, default: null },
  chartOptions: { type: Object, required: true },
})
</script>

<style scoped>
.chart-card {
  background: var(--card-bg, #fafaf9);
  border: 1px solid var(--card-border, #e7e5e0);
  border-radius: var(--radius, 10px);
  padding: 24px;
  box-shadow: 0 1px 0 var(--card-border, #e7e5e0), 0 2px 8px rgba(0,0,0,0.04);
  transition: box-shadow 320ms cubic-bezier(0.4,0,0.2,1);
}
.chart-card:hover {
  box-shadow: 0 2px 0 var(--card-border, #e7e5e0), 0 4px 20px rgba(0,0,0,0.08);
}
.chart-card h3 {
  font-family: var(--font-display, serif);
  font-size: 0.95rem;
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: 0.03em;
  color: var(--text, #1c1c1e);
}
</style>
