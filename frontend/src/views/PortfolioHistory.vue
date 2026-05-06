<template>
  <div class="history-page">
    <div class="card">
      <div class="card-header">
        <h2>历史方案</h2>
        <el-button size="small" @click="loadData">刷新</el-button>
      </div>

      <template v-if="!hasData">
        <el-empty description="暂无历史方案记录">
          <p style="color:#6b7280;font-size:0.85rem">在「策略看板」生成方案后，可保存到历史记录中查看对比</p>
        </el-empty>
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
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-header h2 { font-size: 1.05rem; font-weight: 700; }
h3 { font-size: 0.95rem; font-weight: 600; margin-bottom: 10px; }
</style>
