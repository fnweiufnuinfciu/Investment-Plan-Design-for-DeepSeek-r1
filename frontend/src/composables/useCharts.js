import { computed, reactive } from 'vue'

/**
 * 图表配置 composable — 从 Dashboard 中提取 ECharts option 逻辑
 */
export function useCharts(result, comparisonData) {
  const weightPieOption = computed(() => {
    const pos = result.value?.positions || []
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
    const pos = result.value?.positions || []
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
    const simulated = result.value?.backtest?.cumulativeCurve || []
    const expected = result.value?.backtest?.expectedCurve || []
    const days = simulated.length - 1
    if (days < 0) return {}
    const xData = Array.from({length: days + 1}, (_, i) => `D${i}`)
    return {
      tooltip: { trigger: 'axis', formatter: p => `${p[0].axisValue}<br/>${p[0].seriesName}: ${Number(p[0].value).toFixed(4)}%` },
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
    const scenarios = result.value?.stressTest?.scenarios || []
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
      tooltip: { trigger: 'axis' },
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

  return reactive({ weightPieOption, signalBarOption, backtestCurveOption, stressRadarOption, comparisonBarOption })
}
