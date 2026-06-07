<template>
  <div class="dashboard">
    <!-- Step guide -->
    <div class="step-guide">
      <div class="step-track">
        <div
          v-for="(s, i) in steps"
          :key="s.key"
          class="step-dot"
          :class="{ done: currentStep > i, active: currentStep === i }"
          @click="scrollToSection(s.key)"
        >
          <span class="step-num">{{ currentStep > i ? '✓' : i + 1 }}</span>
          <span class="step-label">{{ s.label }}</span>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：输入面板 -->
      <el-col :span="14">
        <AppCard>
          <template #header>
            <h2>输入与参数</h2>
            <div class="header-actions">
              <el-button size="small" @click="loadSample">加载示例</el-button>
              <el-button size="small" @click="clearResult">清空结果</el-button>
            </div>
          </template>

          <el-tabs v-model="store.activeTab" class="input-tabs">
            <el-tab-pane label="预计算数据 (JSON)" name="json">
              <el-input
                v-model="jsonInput"
                type="textarea"
                :rows="10"
                placeholder='[{"ticker":"AAPL","report_date":"2026-04-15","deepseek":{"recommendation":"Bullish","confidence":0.82},...}]'
              />
            </el-tab-pane>

            <el-tab-pane label="原始报告文本 (API)" name="text">
              <TextInputForm
                v-model:text-form="textForm"
                :text-reports="textReports"
                :api-ok="apiOk"
                @add="addTextReport"
                @remove="i => textReports.splice(i, 1)"
                @load-sample="loadTextSample"
              />
            </el-tab-pane>
          </el-tabs>
        </AppCard>
      </el-col>

      <!-- 右侧：策略参数 -->
      <el-col :span="10">
        <AppCard>
          <h3 style="margin-bottom:12px;">策略参数</h3>
          <ParameterForm :settings="settings" :loading="store.loading" @run="runAnalysis" />
          <p class="status-text" :class="{ error: store.error }">{{ store.statusText }}</p>
        </AppCard>
      </el-col>
    </el-row>

    <!-- Loading skeleton -->
    <div v-if="store.loading" class="loading-zone">
      <div class="skeleton-card" v-for="n in 4" :key="n" :style="{ animationDelay: `${n * 0.1}s` }">
        <div class="sk-line sk-title"></div>
        <div class="sk-line sk-body"></div>
        <div class="sk-line sk-body short"></div>
        <div class="sk-block"></div>
      </div>
    </div>

    <!-- Empty state (no result yet) -->
    <div v-else-if="!store.result && !store.loading" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 80 80" fill="none"><rect x="15" y="25" width="50" height="38" rx="3" stroke="currentColor" stroke-width="1.5" fill="none"/><line x1="22" y1="34" x2="58" y2="34" stroke="currentColor" stroke-width="1" opacity="0.4"/><line x1="22" y1="40" x2="50" y2="40" stroke="currentColor" stroke-width="1" opacity="0.3"/><line x1="22" y1="46" x2="44" y2="46" stroke="currentColor" stroke-width="1" opacity="0.3"/><circle cx="58" cy="32" r="10" stroke="currentColor" stroke-width="1.2" fill="none"/><line x1="58" y1="28" x2="58" y2="36" stroke="currentColor" stroke-width="1.2"/><line x1="54" y1="32" x2="62" y2="32" stroke="currentColor" stroke-width="1.2"/></svg>
      </div>
      <h3>开始分析</h3>
      <p>加载示例数据或输入您的研究报告，生成投资组合方案</p>
      <div class="empty-actions">
        <el-button type="primary" @click="loadSample">加载示例数据</el-button>
        <el-button @click="store.activeTab = 'text'">输入研报文本</el-button>
      </div>
    </div>

    <!-- Results -->
    <div class="results-zone">
    <template v-if="store.result">
      <SummaryCards :summary="store.result.summary" />
      <ChartGrid :result="store.result" :chart-options="chartOptions" />
      <PositionsTable :positions="store.result.positions" />
      <BacktestAndQuality :result="store.result" />
      <StressAndExplain :result="store.result" />
      <StrategyComparison
        :comparison-data="comparisonData"
        :comparing="comparing"
        :chart-options="chartOptions"
        @run="runComparison"
      />
      <div style="margin-top:16px;display:flex;gap:10px;justify-content:flex-end">
        <el-button @click="downloadJson">下载 JSON</el-button>
        <el-button type="primary" @click="downloadMd">下载 Markdown</el-button>
      </div>
    </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getHealth, getDefaults, generatePlan, generatePlanFromTexts, compareStrategies } from '@/api'
import { useCharts } from '@/composables/useCharts'
import { SAMPLE_DATA, REPORT_TEXTS } from '@/constants/sampleData'
import AppCard from '@/components/AppCard.vue'
import TextInputForm from '@/components/TextInputForm.vue'
import ParameterForm from '@/components/ParameterForm.vue'
import SummaryCards from '@/components/SummaryCards.vue'
import ChartGrid from '@/components/ChartGrid.vue'
import PositionsTable from '@/components/PositionsTable.vue'
import BacktestAndQuality from '@/components/BacktestAndQuality.vue'
import StressAndExplain from '@/components/StressAndExplain.vue'
import StrategyComparison from '@/components/StrategyComparison.vue'

const store = useAppStore()
const jsonInput = ref('')
const apiOk = ref(false)
const comparing = ref(false)
const comparisonData = ref(null)
const chartOptions = useCharts(computed(() => store.result), comparisonData)

// Step guide
const steps = [
  { key: 'input', label: '输入报告' },
  { key: 'params', label: '配置参数' },
  { key: 'generate', label: '生成方案' },
  { key: 'results', label: '查看结果' },
]
const currentStep = computed(() => {
  if (store.result) return 4
  if (store.loading) return 3
  const hasReports = textReports.value.length > 0 || jsonInput.value.trim()
  if (hasReports) return 2
  return 1
})
function scrollToSection(key) {
  if (key === 'input') window.scrollTo({ top: 180, behavior: 'smooth' })
  else if (key === 'results' && store.result) {
    document.querySelector('.results-zone')?.scrollIntoView({ behavior: 'smooth' })
  }
}

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

const settings = ref({
  capital: 1000000, mode: 'long_short', maxPositions: 12,
  maxPositionWeight: 0.15, minConfidence: 0.55, minObjectiveRatio: 0.45,
  holdDays: 60, rebalanceDays: 20, stopLossPct: 0.08, takeProfitPct: 0.20,
})

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
    jsonInput.value = JSON.stringify(SAMPLE_DATA, null, 2)
    store.setStatus('已加载示例数据。')
  } catch (e) {
    store.setStatus('加载示例失败。')
  }
}

function loadTextSample() {
  textReports.value = SAMPLE_DATA.map(r => ({
    ticker: r.ticker, reportDate: r.reportDate, source: '', sector: '',
    analystRecommendation: r.analystRecommendation, confidence: null,
    objectiveRatio: r.objectiveRatio, subjectiveRatio: r.subjectiveRatio,
    volatility20d: r.volatility20d, futureAr60d: r.futureAr60d,
    reportText: REPORT_TEXTS[r.ticker] || `Analyst report for ${r.ticker}.`,
  }))
  store.setStatus('已生成6份模拟研报，点击「生成全量分析」将依次调用 DeepSeek API 分析。')
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
</script>

<style scoped>
.dashboard { animation: fadeIn 0.5s cubic-bezier(0.16,1,0.3,1) both; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }

/* ── Step Guide ── */
.step-guide {
  margin-bottom: 24px;
  padding: 16px 20px;
  background: var(--card-bg, #fafaf9);
  border: 1px solid var(--card-border, #e7e5e0);
  border-radius: var(--radius, 10px);
  box-shadow: 0 1px 0 var(--card-border, #e7e5e0), 0 2px 8px rgba(0,0,0,0.04);
}
.step-track {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  position: relative;
}
.step-track::before {
  content: '';
  position: absolute;
  top: 14px;
  left: calc(50% - 260px);
  right: calc(50% - 260px);
  height: 1px;
  background: var(--border, #e4e4e0);
  z-index: 0;
}
.step-dot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 0 32px;
  position: relative;
  z-index: 1;
  cursor: pointer;
  transition: all 0.28s cubic-bezier(0.4,0,0.2,1);
}
.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  font-weight: 700;
  background: #f3f3f0;
  border: 1.5px solid var(--border, #e4e4e0);
  color: var(--text-muted, #7c7c82);
  transition: all 0.28s ease;
}
.step-dot.done .step-num {
  background: var(--accent, #c8a45c);
  border-color: var(--accent, #c8a45c);
  color: #fff;
}
.step-dot.active .step-num {
  background: var(--header-bg, #0b0d14);
  border-color: var(--accent, #c8a45c);
  color: var(--accent, #c8a45c);
  box-shadow: 0 0 0 4px rgba(200,164,92,0.15);
}
.step-label {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-muted, #7c7c82);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  transition: color 0.28s ease;
}
.step-dot.done .step-label,
.step-dot.active .step-label { color: var(--accent, #c8a45c); }
.step-dot:hover .step-num { transform: scale(1.08); }

/* ── Loading Skeleton ── */
.loading-zone { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 20px; }
.skeleton-card {
  background: var(--card-bg, #fafaf9);
  border: 1px solid var(--card-border, #e7e5e0);
  border-radius: var(--radius, 10px);
  padding: 24px;
  animation: skPulse 1.6s ease-in-out infinite both;
}
@keyframes skPulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}
.sk-line {
  height: 12px;
  border-radius: 4px;
  background: linear-gradient(90deg, #e8e6e0, #d5d3cb, #e8e6e0);
  background-size: 200% 100%;
  animation: skShimmer 1.8s ease-in-out infinite;
  margin-bottom: 10px;
}
@keyframes skShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
.sk-title { width: 40%; height: 16px; }
.sk-body { width: 85%; }
.sk-body.short { width: 55%; }
.sk-block { height: 60px; border-radius: 6px; background: #f0eee8; margin-top: 12px; }

/* ── Empty State ── */
.empty-state {
  text-align: center;
  padding: 60px 24px 48px;
  animation: fadeIn 0.6s ease both;
}
.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  color: var(--accent, #c8a45c);
  opacity: 0.7;
}
.empty-state h3 {
  font-family: var(--font-display, serif);
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text, #1c1c1e);
  margin-bottom: 8px;
}
.empty-state p {
  color: var(--text-muted, #7c7c82);
  font-size: 0.88rem;
  margin-bottom: 20px;
}
.empty-actions { display: flex; gap: 10px; justify-content: center; }

/* ── Results ── */
.results-zone { animation: fadeIn 0.5s ease both; }

.header-actions { display: flex; gap: 8px; }
.input-tabs { margin-top: 8px; }
.status-text { margin-top: 10px; font-size: 0.85rem; color: var(--text-muted, #7c7c82); letter-spacing: 0.02em; }
.status-text.error { color: var(--danger, #c4554d); }
h3 { font-family: var(--font-display, serif); font-size: 0.95rem; font-weight: 700; margin-bottom: 10px; letter-spacing: 0.03em; }
</style>
