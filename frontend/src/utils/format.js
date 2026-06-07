/**
 * 共享格式化工具
 */
export function fmt(v, d) {
  if (v === null || v === undefined || isNaN(v)) return '-'
  return Number(v).toFixed(d)
}

export function pct(v) {
  if (v === null || v === undefined || isNaN(v)) return '-'
  return (Number(v) * 100).toFixed(2) + '%'
}

export function noteLabel(key) {
  const map = {
    high_objective_content: '高客观含量',
    high_model_confidence: '高模型置信度',
    high_volatility_risk: '高波动风险',
  }
  return map[key] || key
}
