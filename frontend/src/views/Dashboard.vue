<template>
  <div class="dashboard">
    <!-- Input Panel -->
    <el-row :gutter="20">
      <el-col :span="14">
        <div class="card">
          <div class="card-header">
            <h2>输入与参数</h2>
            <div class="header-actions">
              <el-button size="small" @click="loadSample">加载示例</el-button>
              <el-button size="small" @click="clearResult">清空结果</el-button>
            </div>
          </div>

          <el-tabs v-model="store.activeTab" class="input-tabs">
            <el-tab-pane label="预计算数据（JSON）" name="json">
              <el-input
                v-model="jsonInput"
                type="textarea"
                :rows="10"
                placeholder='[{"ticker":"AAPL","report_date":"2026-04-15","deepseek":{"recommendation":"Bullish","confidence":0.82},...}]'
              />
            </el-tab-pane>
            <el-tab-pane label="原始报告文本（API）" name="text">
              <!-- Metadata fields -->
              <el-form :model="textForm" label-width="95px" size="small">
                <el-row :gutter="10">
                  <el-col :span="6">
                    <el-form-item label="股票代码">
                      <el-input v-model="textForm.ticker" placeholder="如 600519"/>
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="报告日期">
                      <el-input v-model="textForm.reportDate" type="date" placeholder="2026-04-24"/>
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="来源">
                      <el-input v-model="textForm.source" placeholder="如 华鑫证券"/>
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="行业">
                      <el-input v-model="textForm.sector" placeholder="如 白酒"/>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="10">
                  <el-col :span="8">
                    <el-form-item label="分析师评级">
                      <el-select v-model="textForm.analystRecommendation" style="width:100%" placeholder="可选">
                        <el-option label="Very Bullish" value="Very Bullish"/>
                        <el-option label="Bullish" value="Bullish"/>
                        <el-option label="Neutral" value="Neutral"/>
                        <el-option label="Bearish" value="Bearish"/>
                        <el-option label="Very Bearish" value="Very Bearish"/>
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="客观占比">
                      <el-input-number v-model="textForm.objectiveRatio" :min="0" :max="1" :step="0.01" controls-position="right" style="width:100%"/>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="主观占比">
                      <el-input-number v-model="textForm.subjectiveRatio" :min="0" :max="1" :step="0.01" controls-position="right" style="width:100%"/>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="10">
                  <el-col :span="8">
                    <el-form-item label="20日波动率">
                      <el-input-number v-model="textForm.volatility20d" :min="0" :max="2" :step="0.01" controls-position="right" style="width:100%"/>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="60日预期收益">
                      <el-input-number v-model="textForm.futureAr60d" :min="-1" :max="2" :step="0.01" controls-position="right" style="width:100%" placeholder="可选"/>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="置信度">
                      <el-input-number v-model="textForm.confidence" :min="0" :max="1" :step="0.01" controls-position="right" style="width:100%" placeholder="可选(API自动覆盖)"/>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item label="报告原文">
                  <el-input
                    v-model="textForm.reportText"
                    type="textarea"
                    :rows="8"
                    placeholder="直接粘贴研报全文即可，无需转义引号或换行。&#10;&#10;示例：华鑫证券有限责任公司孙山山近期对贵州茅台进行研究并发布了研究报告...给予贵州茅台买入评级。贵州茅台(600519) 事件 2026年4月24日..."
                  />
                </el-form-item>
                <div style="display:flex;align-items:center;gap:10px">
                  <el-button size="small" type="primary" @click="addTextReport">添加到列表</el-button>
                  <el-button size="small" @click="loadTextSample">加载示例</el-button>
                  <el-tag v-if="apiOk" type="success" size="small">API 已配置</el-tag>
                  <el-tag v-else type="danger" size="small">API 未配置</el-tag>
                  <span v-if="textReports.length" style="font-size:0.85rem;color:#6b7280;margin-left:auto">
                    已添加 <b>{{ textReports.length }}</b> 条报告
                  </span>
                </div>
              </el-form>

              <!-- Report list -->
              <div v-if="textReports.length" style="margin-top:10px">
                <el-table :data="textReports" size="small" max-height="180">
                  <el-table-column prop="ticker" label="代码" width="80"/>
                  <el-table-column prop="reportDate" label="日期" width="100"/>
                  <el-table-column prop="source" label="来源" width="90"/>
                  <el-table-column prop="sector" label="行业" width="70"/>
                  <el-table-column prop="analystRecommendation" label="评级" width="110"/>
                  <el-table-column label="报告摘要" min-width="180">
                    <template #default="{row}">{{ (row.reportText||'').substring(0,50) }}...</template>
                  </el-table-column>
                  <el-table-column label="操作" width="60" fixed="right">
                    <template #default="{ $index }">
                      <el-button size="small" type="danger" @click="textReports.splice($index,1)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>

      <!-- Parameters -->
      <el-col :span="10">
        <div class="card">
          <h3 style="margin-bottom:12px;">策略参数</h3>
          <el-form :model="settings" label-width="110px" size="small">
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="资金">
                  <el-input-number v-model="settings.capital" :min="10000" :step="100000" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="模式">
                  <el-select v-model="settings.mode" style="width:100%">
                    <el-option label="多空组合 (Long/Short)" value="long_short"/>
                    <el-option label="仅做多 (Long Only)" value="long_only"/>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最大持仓数">
                  <el-input-number v-model="settings.maxPositions" :min="1" :max="50" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="单票上限">
                  <el-input-number v-model="settings.maxPositionWeight" :min="0.01" :max="1" :step="0.01" :precision="2" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最低信心">
                  <el-input-number v-model="settings.minConfidence" :min="0" :max="1" :step="0.01" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最低客观占比">
                  <el-input-number v-model="settings.minObjectiveRatio" :min="0" :max="1" :step="0.01" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="持有天数">
                  <el-input-number v-model="settings.holdDays" :min="1" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="再平衡周期">
                  <el-input-number v-model="settings.rebalanceDays" :min="1" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="止损">
                  <el-input-number v-model="settings.stopLossPct" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="止盈">
                  <el-input-number v-model="settings.takeProfitPct" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-button type="primary" :loading="store.loading" @click="runAnalysis" style="width:100%;margin-top:8px">
              {{ store.loading ? '正在分析...' : '生成全量分析' }}
            </el-button>
          </el-form>
          <p class="status-text" :class="{ error: store.error }">{{ store.statusText }}</p>
        </div>
      </el-col>
    </el-row>


    <!-- Results Section -->
    <template v-if="store.result">
      <!-- Summary Cards -->
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="4" v-for="c in summaryCards" :key="c.title">
          <div class="stat-card">
            <div class="stat-title">{{ c.title }}</div>
            <div class="stat-value">{{ c.value }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- Charts Row 1: Weight Pie + Long/Short Bars -->
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="12">
          <div class="card"><h3>持仓权重分布</h3><v-chart :option="weightPieOption" style="height:300px" autoresize /></div>
        </el-col>
        <el-col :span="12">
          <div class="card"><h3>信号值对比</h3><v-chart :option="signalBarOption" style="height:300px" autoresize /></div>
        </el-col>
      </el-row>

      <!-- Charts Row 2: Backtest Curve + Risk Radar -->
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="12">
          <div class="card"><h3>回测收益曲线（估算）</h3><v-chart :option="backtestCurveOption" style="height:300px" autoresize /></div>
        </el-col>
        <el-col :span="12">
          <div class="card"><h3>压力测试雷达图</h3><v-chart :option="stressRadarOption" style="height:300px" autoresize /></div>
        </el-col>
      </el-row>

      <!-- Positions Table -->
      <div class="card" style="margin-top:20px">
        <div class="card-header"><h2>持仓明细</h2></div>
        <el-table :data="store.result.positions" stripe size="small" max-height="400">
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

      <!-- Backtest + Data Quality -->
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="12">
          <div class="card"><h3>回测指标</h3>
            <el-row :gutter="8">
              <el-col :span="8" v-for="m in backtestMetrics" :key="m.label">
                <div class="metric"><div class="metric-label">{{ m.label }}</div><div class="metric-value">{{ m.value }}</div></div>
              </el-col>
            </el-row>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="card"><h3>数据质量诊断</h3>
            <div class="metric"><div class="metric-label">数据质量评分</div><div class="metric-value">{{ fmt(store.result.dataQuality?.qualityScore, 3) }}</div></div>
            <div v-if="store.result.dataQuality?.warnings?.length" style="margin-top:8px">
              <el-alert v-for="w in store.result.dataQuality.warnings" :key="w" :title="w" type="warning" :closable="false" style="margin-bottom:4px"/>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- Stress + Sensitivity -->
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="12">
          <div class="card"><h3>压力测试</h3>
            <el-table :data="store.result.stressTest?.scenarios || []" size="small">
              <el-table-column prop="scenario" label="压力情景"/>
              <el-table-column prop="assumedMarketMove" label="假定市场波动"><template #default="{row}">{{ pct(row.assumedMarketMove) }}</template></el-table-column>
              <el-table-column prop="portfolioPnl" label="组合盈亏($)"><template #default="{row}">{{ fmt(row.portfolioPnl, 2) }}</template></el-table-column>
              <el-table-column prop="portfolioReturn" label="组合收益率"><template #default="{row}">{{ pct(row.portfolioReturn) }}</template></el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="card"><h3>可解释性（Top 贡献）</h3>
            <el-table :data="store.result.explainability?.topSignalContributors || []" size="small">
              <el-table-column prop="ticker" label="代码"/><el-table-column prop="side" label="方向">
                <template #default="{row}"><el-tag :type="row.side==='LONG'?'success':'danger'" size="small">{{ row.side==='LONG'?'做多':'做空' }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="contributionScore" label="贡献度"><template #default="{row}">{{ fmt(row.contributionScore, 4) }}</template></el-table-column>
              <el-table-column prop="signalScore" label="信号值"><template #default="{row}">{{ fmt(row.signalScore, 4) }}</template></el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>

      <!-- Strategy Comparison -->
      <div class="card" style="margin-top:20px">
        <div class="card-header">
          <h2>策略对比</h2>
          <el-button size="small" type="primary" :loading="comparing" @click="runComparison">生成对比</el-button>
        </div>
        <template v-if="comparisonData">
          <el-row :gutter="16">
            <el-col :span="16">
              <el-table :data="comparisonData.strategies" size="small" stripe>
                <el-table-column prop="strategyLabel" label="策略"/>
                <el-table-column prop="positionCount" label="持仓数"/>
                <el-table-column prop="longCount" label="做多"/>
                <el-table-column prop="shortCount" label="做空"/>
                <el-table-column prop="grossExposure" label="总敞口"><template #default="{row}">{{ fmt(row.grossExposure, 4) }}</template></el-table-column>
                <el-table-column prop="netExposure" label="净敞口"><template #default="{row}">{{ fmt(row.netExposure, 4) }}</template></el-table-column>
                <el-table-column prop="expectedReturn60d" label="预期收益"><template #default="{row}">{{ pct(row.expectedReturn60d) }}</template></el-table-column>
                <el-table-column prop="maxDrawdown" label="最大回撤"><template #default="{row}">{{ pct(row.maxDrawdown) }}</template></el-table-column>
              </el-table>
              <el-alert v-if="comparisonData.best" type="success" :closable="false" style="margin-top:12px">
                最优策略：<strong>{{ comparisonData.best }}</strong> — {{ comparisonData.reason }}
              </el-alert>
            </el-col>
            <el-col :span="8">
              <v-chart :option="comparisonBarOption" style="height:260px" autoresize />
            </el-col>
          </el-row>
        </template>
      </div>

      <!-- Downloads -->
      <div style="margin-top:16px;display:flex;gap:10px;justify-content:flex-end">
        <el-button @click="downloadJson">下载 JSON</el-button>
        <el-button type="primary" @click="downloadMd">下载 Markdown</el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getHealth, getDefaults, generatePlan, generatePlanFromTexts, compareStrategies } from '@/api'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, BarChart, LineChart, RadarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, BarChart, LineChart, RadarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const store = useAppStore()
const jsonInput = ref('')
const apiOk = ref(false)

// Text tab — individual form fields
const textForm = ref({
  ticker: '', reportDate: '', source: '', sector: '',
  analystRecommendation: '', objectiveRatio: 0.65, subjectiveRatio: 0.35,
  volatility20d: 0.25, futureAr60d: 0, confidence: 0, reportText: '',
})
const textReports = ref([])

function resetTextForm() {
  textForm.value = {
    ticker: '', reportDate: '', source: '', sector: '',
    analystRecommendation: '', objectiveRatio: 0.65, subjectiveRatio: 0.35,
    volatility20d: 0.25, futureAr60d: 0, confidence: 0, reportText: '',
  }
}

function addTextReport() {
  if (!textForm.value.ticker || !textForm.value.reportText) {
    store.setStatus('请至少填写股票代码和报告原文。')
    return
  }
  textReports.value.push({
    ticker: textForm.value.ticker,
    reportDate: textForm.value.reportDate || new Date().toISOString().split('T')[0],
    source: textForm.value.source || '',
    sector: textForm.value.sector || '',
    analystRecommendation: textForm.value.analystRecommendation || '',
    confidence: textForm.value.confidence || null,
    objectiveRatio: textForm.value.objectiveRatio,
    subjectiveRatio: textForm.value.subjectiveRatio,
    volatility20d: textForm.value.volatility20d,
    futureAr60d: textForm.value.futureAr60d || 0,
    reportText: textForm.value.reportText,
  })
  resetTextForm()
  store.setStatus(`报告已添加，当前列表共 ${textReports.value.length} 条。`)
}
const comparing = ref(false)
const comparisonData = ref(null)

const settings = ref({
  capital: 1000000, mode: 'long_short', maxPositions: 12,
  maxPositionWeight: 0.15, minConfidence: 0.55, minObjectiveRatio: 0.45,
  holdDays: 60, rebalanceDays: 20, stopLossPct: 0.08, takeProfitPct: 0.20,
})

const summaryCards = computed(() => {
  const s = store.result?.summary
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

const backtestMetrics = computed(() => {
  const m = store.result?.backtest?.metrics
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

// ── ECharts Options ──

const weightPieOption = computed(() => {
  const pos = store.result?.positions || []
  const data = pos.map(p => ({ name: p.ticker, value: Math.abs(p.targetWeight) }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { fontSize: 12 } },
    series: [{ type: 'pie', radius: ['45%','75%'], center: ['40%','50%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data,
    }],
  }
})

const signalBarOption = computed(() => {
  const pos = store.result?.positions || []
  const names = pos.map(p => p.ticker)
  const signals = pos.map(p => p.signalScore)
  const colors = pos.map(p => p.signalScore >= 0 ? '#67c23a' : '#f56c6c')
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: names },
    yAxis: { type: 'value', name: '信号值' },
    series: [{ type: 'bar', data: signals.map((v,i) => ({ value: v, itemStyle: { color: colors[i] } })),
      barMaxWidth: 40 }],
  }
})

const backtestCurveOption = computed(() => {
  const simulated = store.result?.backtest?.cumulativeCurve || []
  const expected = store.result?.backtest?.expectedCurve || []
  const days = simulated.length - 1
  if (days < 0) return {}
  const xData = Array.from({length: days + 1}, (_, i) => `D${i}`)
  return {
    tooltip: { trigger: 'axis', formatter: p => `${p[0].axisValue}<br/>${p[0].seriesName}: ${p[0].value.toFixed(4)}%` },
    legend: { data: ['模拟路径','预期路径'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: xData, name: '交易日' },
    yAxis: { type: 'value', name: '累计收益(%)', axisLabel: { formatter: '{value}%' } },
    series: [
      { name: '模拟路径', type: 'line', data: simulated, smooth: true,
        lineStyle: { color: '#409eff', width: 2 },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{offset:0,color:'rgba(64,158,255,0.3)'},{offset:1,color:'rgba(64,158,255,0.05)'}] } } },
      { name: '预期路径', type: 'line', data: expected, smooth: true,
        lineStyle: { color: '#67c23a', width: 2, type: 'dashed' } },
    ],
  }
})

const stressRadarOption = computed(() => {
  const scenarios = store.result?.stressTest?.scenarios || []
  if (!scenarios.length) return {}
  const maxAbs = Math.max(...scenarios.map(s => Math.abs(s.portfolioReturn)), 0.005) * 1.3
  const indicator = scenarios.map(s => ({ name: s.scenario.replace(/\(.*\)/,'').trim(), max: maxAbs }))
  const data = scenarios.map(s => s.portfolioReturn)
  return {
    tooltip: {},
    radar: { indicator, center: ['50%','55%'], radius: '65%' },
    series: [{ type: 'radar', data: [{ value: data, name: '组合收益率', areaStyle: { opacity: 0.2 },
      lineStyle: { color: '#e6a23c' }, itemStyle: { color: '#e6a23c' } }] }],
  }
})

const comparisonBarOption = computed(() => {
  if (!comparisonData.value) return {}
  const names = comparisonData.value.strategies.map(s => s.strategyLabel)
  const rets = comparisonData.value.strategies.map(s => +(s.expectedReturn60d * 100).toFixed(2))
  const dd = comparisonData.value.strategies.map(s => +(s.maxDrawdown * 100).toFixed(2))
  return {
    tooltip: { trigger: 'axis', formatter: p => {
      let s = p[0].name
      p.forEach(item => { s += `<br/>${item.marker} ${item.seriesName}: ${item.value.toFixed(2)}%` })
      return s
    }},
    legend: { data: ['预期收益(%)','最大回撤(%)'], bottom: 0 },
    grid: { left: '3%', right: '8%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: names },
    yAxis: [
      { type: 'value', name: '预期收益(%)', axisLabel: { formatter: '{value}%' } },
      { type: 'value', name: '最大回撤(%)', inverse: true, axisLabel: { formatter: '{value}%' } },
    ],
    series: [
      { name: '预期收益(%)', type: 'bar', data: rets, barMaxWidth: 30, yAxisIndex: 0,
        itemStyle: { color: '#67c23a' }, label: { show: true, position: 'top', formatter: '{c}%' } },
      { name: '最大回撤(%)', type: 'bar', data: dd, barMaxWidth: 30, yAxisIndex: 1,
        itemStyle: { color: '#f56c6c' }, label: { show: true, position: 'bottom', formatter: '{c}%' } },
    ],
  }
})

// ── Actions ──

async function runComparison() {
  comparing.value = true
  try {
    const records = JSON.parse(jsonInput.value || '[]')
    if (!records.length) throw new Error('请先加载示例或输入数据')
    const resp = await compareStrategies({ records, settings: settings.value })
    comparisonData.value = resp.data
    store.setStatus('策略对比完成。')
  } catch (e) {
    store.setStatus(`对比失败: ${e.response?.data?.error || e.message}`)
  } finally {
    comparing.value = false
  }
}

const NOTE_MAP = {
  high_objective_content: '高客观含量',
  high_model_confidence: '高模型置信度',
  high_volatility_risk: '高波动风险',
}
function noteLabel(key) {
  return NOTE_MAP[key] || key
}
function fmt(v, d) {
  if (v === null || v === undefined || isNaN(v)) return '-'
  return Number(v).toFixed(d)
}
function pct(v) {
  if (v === null || v === undefined || isNaN(v)) return '-'
  return (Number(v) * 100).toFixed(2) + '%'
}

async function runAnalysis() {
  store.setLoading(true)
  store.setError(null)
  store.setStatus('正在分析...')

  try {
    let resp
    if (store.activeTab === 'json') {
      const records = JSON.parse(jsonInput.value || '[]')
      if (!records.length) throw new Error('输入数据为空')
      resp = await generatePlan({ records, settings: settings.value })
    } else {
      if (!textReports.value.length) throw new Error('请先在「原始报告文本」Tab 中添加至少一条报告')
      resp = await generatePlanFromTexts({ records: textReports.value, settings: settings.value })
    }
    store.setResult(resp.data)
    store.setStatus(`完成：输入 ${resp.data.diagnostics?.inputRecords} 条，持仓 ${resp.data.diagnostics?.positionCount} 条`)
  } catch (e) {
    store.setError(e)
    store.setStatus(`错误: ${e.response?.data?.error || e.message}`)
  } finally {
    store.setLoading(false)
  }
}

async function loadSample() {
  try {
    const resp = await getDefaults()
    if (resp.data?.defaults) Object.assign(settings.value, resp.data.defaults)
    // Load a pre-built sample
    jsonInput.value = JSON.stringify(SAMPLE_DATA, null, 2)
    store.setStatus('已加载示例数据。')
  } catch (e) {
    store.setStatus('加载示例失败。')
  }
}

const REPORT_TEXTS = {
  AAPL: "Apple Inc. Q1 FY2026 earnings report: Revenue reached $125B, beating consensus estimates by 4%. " +
    "Services segment revenue grew 18% YoY to $28B, driven by App Store and Apple Music subscriptions. " +
    "iPhone revenue remained flat at $55B, with higher ASP offsetting unit declines. " +
    "Gross margins expanded 120bps to 46.2%, above management guidance. " +
    "The company authorized an additional $90B share buyback program. " +
    "Greater China revenue declined 8% YoY to $20.8B amid competitive pressure from Huawei. " +
    "EU Digital Markets Act compliance costs estimated at $500M annually. " +
    "Wearables revenue up 5% YoY. Forward P/E of 28x reflects premium valuation. " +
    "Cash position of $165B provides significant strategic flexibility.",
  MSFT: "Microsoft Corporation Q3 FY2026 analysis: Total revenue of $68B, up 15% YoY, driven by Azure cloud growth. " +
    "Azure revenue accelerated to 33% YoY growth, outpacing AWS and GCP. " +
    "Microsoft 365 commercial seats grew 8%, with E5 premium mix improving ARPU. " +
    "AI Copilot adoption reached 60% of Fortune 500 enterprises, contributing $3.2B incremental revenue. " +
    "Operating margins expanded 50bps to 48.5%. Gaming revenue declined 4% due to hardware cycle maturity. " +
    "LinkedIn revenue grew 10% YoY. Capital expenditures of $14B reflect AI infrastructure investment. " +
    "Commercial cloud backlog of $260B provides multi-year visibility. " +
    "Net cash of $55B. Forward P/E 32x reflects AI growth premium.",
  TSLA: "Tesla Inc. Q1 2026 report: Total revenue of $28B missed consensus by 3%. " +
    "Automotive gross margin declined to 16.2% from 18.5% year-over-year, reflecting price cuts in China and Europe. " +
    "Operating margin compressed to 7.1% from 11.4%. Cybertruck production ramp progressing slower than initial guidance, " +
    "with 45K units delivered in Q1. Energy storage deployed 12GWh, up 90% YoY, with Megapack backlog extending into 2027. " +
    "FSD take rate declined to 12% from 18% globally. China market share dropped from 8.5% to 6.2%. " +
    "Competition from BYD and Xpeng intensifying across price segments. " +
    "Berlin and Austin factories operating at 65% utilization. Cash reserves of $25B. " +
    "Forward P/E of 58x remains elevated relative to auto peers.",
  NVDA: "NVIDIA Corporation Q1 FY2027 analysis: Revenue of $35B exceeded guidance by 8%, driven by Blackwell GPU demand. " +
    "Data Center segment revenue grew 200% YoY to $28B, as hyperscale customers accelerate AI infrastructure buildout. " +
    "Gaming revenue of $3.5B, up 25% YoY on RTX 50 series refresh cycle. " +
    "Gross margins reached 78.4%, expanding 300bps sequentially on product mix shift to higher-end configurations. " +
    "Enterprise AI adoption driving demand for DGX systems and Omniverse platform. " +
    "Supply constraints on HBM3e memory expected to ease in H2 2026. " +
    "Announced $25B share repurchase authorization. Forward P/E of 35x on consensus estimates. " +
    "Competitive moat remains wide with CUDA ecosystem locked in across 4M+ developers. " +
    "Automotive revenue of $400M, up 15% YoY. Main risk: potential export controls tightening.",
  META: "Meta Platforms Inc. Q1 2026 report: Revenue of $45B, up 18% YoY, driven by AI-powered ad targeting improvements. " +
    "Family daily active people reached 3.4B, up 6% YoY. Ad impressions increased 12% with average price per ad up 5%. " +
    "Reality Labs operating loss of $5.2B, with revenue of $800M from Quest 4 and Ray-Ban Meta smart glasses. " +
    "Operating margins of 42% on improved cost discipline. AI content recommendation drove 8% increase in Reels engagement. " +
    "WhatsApp Business monetization reached $2B annual run rate. Capital expenditures of $12B focused on AI training clusters. " +
    "Threads MAU of 350M. Cash reserves of $65B. Forward P/E of 22x reasonable relative to growth rate. " +
    "Main risks: TikTok regulatory uncertainty creates competitive opening but also regulatory precedent risk.",
  INTC: "Intel Corporation Q1 2026 analysis: Revenue of $14.2B, flat YoY, missing expectations by 2%. " +
    "Data Center revenue declined 8% YoY as Gaudi 3 AI accelerator failed to gain meaningful traction against NVIDIA. " +
    "Client Computing revenue of $7.2B, down 3% YoY on PC market weakness. " +
    "Foundry Services (IFS) revenue of $400M, with external customer pipeline of $5B but slow conversion. " +
    "Gross margins of 38.5%, well below historical 55%+ levels. Operating loss of $800M reflects heavy R&D investment. " +
    "18A process node delayed to late 2026, extending timeline for competitive parity with TSMC. " +
    "Announced $10B cost reduction program including 5% workforce reduction. " +
    "Dividend suspended to preserve cash for foundry investments. Net debt position of $8B. " +
    "Forward P/E NM. Main risk: sustained market share loss in both client and data center segments.",
}

function loadTextSample() {
  textReports.value = SAMPLE_DATA.map(r => ({
    ticker: r.ticker,
    reportDate: r.reportDate,
    source: '',
    sector: '',
    analystRecommendation: r.analystRecommendation,
    confidence: null,
    objectiveRatio: r.objectiveRatio,
    subjectiveRatio: r.subjectiveRatio,
    volatility20d: r.volatility20d,
    futureAr60d: r.futureAr60d,
    reportText: REPORT_TEXTS[r.ticker] || `Analyst report for ${r.ticker}. Recommendation: ${r.analystRecommendation}.`,
  }))
  store.setStatus('已生成6份模拟研报，点击「生成全量分析」将依次调用 DeepSeek API 分析（预计30-60秒）。')
}

function clearResult() {
  store.result = null
  store.setStatus('等待输入数据。')
}

function downloadJson() {
  const blob = new Blob([JSON.stringify(store.result, null, 2)], { type: 'application/json' })
  download(blob, 'investment_analysis.json')
}

function downloadMd() {
  const blob = new Blob([store.result?.markdown || ''], { type: 'text/markdown' })
  download(blob, 'investment_plan.md')
}

function download(blob, name) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = name
  a.click(); URL.revokeObjectURL(url)
}

onMounted(async () => {
  try {
    const h = await getHealth()
    apiOk.value = h.data?.api_configured
    const d = await getDefaults()
    if (d.data?.defaults) Object.assign(settings.value, d.data.defaults)
  } catch (_) {}
})

// Sample data matching SpringBoot ReportRequest DTO (flat structure)
const SAMPLE_DATA = [
  { ticker:"AAPL", reportDate:"2026-04-15", analystRecommendation:"Bullish",
    confidence:0.72, objectiveRatio:0.68, subjectiveRatio:0.22, volatility20d:0.24, futureAr60d:0.0784 },
  { ticker:"MSFT", reportDate:"2026-04-15", analystRecommendation:"Very Bullish",
    confidence:0.85, objectiveRatio:0.72, subjectiveRatio:0.18, volatility20d:0.21, futureAr60d:0.0912 },
  { ticker:"TSLA", reportDate:"2026-04-13", analystRecommendation:"Bearish",
    confidence:0.65, objectiveRatio:0.45, subjectiveRatio:0.48, volatility20d:0.52, futureAr60d:-0.0345 },
  { ticker:"NVDA", reportDate:"2026-04-15", analystRecommendation:"Very Bullish",
    confidence:0.88, objectiveRatio:0.75, subjectiveRatio:0.15, volatility20d:0.38, futureAr60d:0.1240 },
  { ticker:"META", reportDate:"2026-04-12", analystRecommendation:"Bullish",
    confidence:0.70, objectiveRatio:0.55, subjectiveRatio:0.35, volatility20d:0.29, futureAr60d:0.0567 },
  { ticker:"INTC", reportDate:"2026-04-10", analystRecommendation:"Bearish",
    confidence:0.58, objectiveRatio:0.40, subjectiveRatio:0.50, volatility20d:0.44, futureAr60d:-0.0210 },
]
</script>

<style scoped>
.dashboard { animation: fadeIn 0.4s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.card-header h2 { font-size: 1.05rem; font-weight: 700; }
.header-actions { display: flex; gap: 8px; }

.input-tabs { margin-top: 8px; }

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.stat-title { font-size: 0.75rem; text-transform: uppercase; color: #6b7280; letter-spacing: 0.05em; }
.stat-value { font-size: 1.4rem; font-weight: 700; margin-top: 6px; }

.metric { padding: 8px; }
.metric-label { font-size: 0.7rem; text-transform: uppercase; color: #6b7280; }
.metric-value { font-size: 1.05rem; font-weight: 600; }

.status-text { margin-top: 8px; font-size: 0.85rem; color: #6b7280; }
.status-text.error { color: #f56c6c; }

h3 { font-size: 0.95rem; font-weight: 600; margin-bottom: 10px; }
</style>
