<template>
  <div class="history-page">
    <div class="card">
      <div class="card-header">
        <h2>历史方案</h2>
        <el-button size="small" @click="loadData">刷新</el-button>
      </div>

      <template v-if="!hasData">
        <div class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 80 80" fill="none"><circle cx="40" cy="40" r="26" stroke="currentColor" stroke-width="1.2"/><line x1="40" y1="20" x2="40" y2="60" stroke="currentColor" stroke-width="1" opacity="0.25"/><line x1="20" y1="40" x2="60" y2="40" stroke="currentColor" stroke-width="1" opacity="0.25"/><circle cx="40" cy="40" r="5" fill="currentColor" opacity="0.5"/></svg>
          </div>
          <h3>暂无历史方案记录</h3>
          <p>在「策略看板」生成投资方案后，分析结果将自动保存到这里</p>
          <el-button size="small" type="primary" @click="$router.push('/')">前往策略看板</el-button>
        </div>
      </template>

      <template v-else>
        <el-table :data="groupedHistory" stripe size="small">
          <el-table-column prop="planId" label="方案编号" width="200"/>
          <el-table-column prop="createdAt" label="生成时间" width="180"/>
          <el-table-column label="持仓" min-width="300">
            <template #default="{row}">
              <el-tag v-for="p in row.positions" :key="p.ticker" size="small" style="margin-right:6px"
                :type="p.side==='LONG'?'success':'danger'">
                {{ p.ticker }}({{ p.side==='LONG'?'多':'空' }}) {{ (p.targetWeight*100).toFixed(1) }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{row}">
              <el-button size="small" @click="toggleDetail(row)">{{ row._expanded ? '收起' : '详情' }}</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Expanded detail -->
        <div v-if="expandedPlan" class="card" style="margin-top:16px">
          <h3>{{ expandedPlan.planId }} - 详细记录</h3>
          <el-table :data="expandedPlan.positions" size="small" stripe>
            <el-table-column prop="ticker" label="代码"/>
            <el-table-column prop="side" label="方向"/>
            <el-table-column prop="recommendation" label="评级"/>
            <el-table-column prop="recommendationScore" label="评级分"/>
            <el-table-column prop="confidence" label="置信度"><template #default="{row}">{{ (row.confidence||0).toFixed(3) }}</template></el-table-column>
            <el-table-column prop="signalScore" label="信号值"><template #default="{row}">{{ (row.signalScore||0).toFixed(4) }}</template></el-table-column>
            <el-table-column prop="targetWeight" label="权重"><template #default="{row}">{{ (row.targetWeight*100).toFixed(2) }}%</template></el-table-column>
          </el-table>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getHistory } from '@/api'

const historyList = ref([])
const expandedPlan = ref(null)

const hasData = computed(() => historyList.value.length > 0)

const groupedHistory = computed(() => {
  const map = new Map()
  for (const h of historyList.value) {
    if (!h.planId) continue
    if (!map.has(h.planId)) {
      map.set(h.planId, { planId: h.planId, createdAt: h.createdAt, positions: [], _expanded: false })
    }
    map.get(h.planId).positions.push(h)
  }
  return Array.from(map.values())
})

function toggleDetail(row) {
  row._expanded = !row._expanded
  expandedPlan.value = row._expanded ? row : null
}

async function loadData() {
  try { const r = await getHistory(); historyList.value = r.data } catch (_) { historyList.value = [] }
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; padding-bottom: 12px; border-bottom: 1px solid var(--border, #e4e4e0); }
.card-header h2 { font-family: var(--font-display, serif); font-size: 1rem; font-weight: 700; letter-spacing: 0.03em; }
h3 { font-family: var(--font-display, serif); font-size: 0.95rem; font-weight: 700; margin-bottom: 10px; letter-spacing: 0.03em; }

/* Empty state */
.empty-state {
  text-align: center;
  padding: 56px 24px 44px;
  animation: fadeIn 0.5s ease both;
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.empty-icon {
  width: 64px; height: 64px;
  margin: 0 auto 16px;
  color: var(--accent, #c8a45c);
  opacity: 0.55;
}
.empty-state h3 {
  font-family: var(--font-display, serif);
  font-size: 1.05rem;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text, #1c1c1e);
}
.empty-state p {
  color: var(--text-muted, #7c7c82);
  font-size: 0.85rem;
  margin-bottom: 16px;
}
</style>
