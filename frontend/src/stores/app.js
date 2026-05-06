import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const result = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const statusText = ref('等待输入数据。')
  const activeTab = ref('json')

  const defaults = ref({
    capital: 1000000,
    mode: 'long_short',
    maxPositions: 12,
    maxPositionWeight: 0.15,
    minConfidence: 0.55,
    minObjectiveRatio: 0.45,
    holdDays: 60,
    rebalanceDays: 20,
    stopLossPct: 0.08,
    takeProfitPct: 0.20,
  })

  function setResult(data) {
    result.value = data
    if (data?.plan) {
      statusText.value = `完成：持仓 ${data.diagnostics?.positionCount || 0} 条`
    }
  }

  function setLoading(v) { loading.value = v }
  function setError(e) { error.value = e?.message || String(e) }
  function setStatus(t) { statusText.value = t }

  return {
    result, loading, error, statusText, activeTab, defaults,
    setResult, setLoading, setError, setStatus,
  }
})
