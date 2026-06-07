<template>
  <div class="report-manage">
    <div class="card">
      <div class="card-header">
        <h2>研报管理</h2>
        <div class="header-actions">
          <el-button size="small" type="primary" @click="showAdd = true">新增报告</el-button>
          <el-button size="small" type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除 ({{ selected.length }})</el-button>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="reports.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 80 80" fill="none"><rect x="18" y="12" width="44" height="56" rx="3" stroke="currentColor" stroke-width="1.3"/><line x1="28" y1="28" x2="52" y2="28" stroke="currentColor" stroke-width="1" opacity="0.3"/><line x1="28" y1="36" x2="52" y2="36" stroke="currentColor" stroke-width="1" opacity="0.3"/><line x1="28" y1="44" x2="44" y2="44" stroke="currentColor" stroke-width="1" opacity="0.3"/><circle cx="56" cy="56" r="12" stroke="currentColor" stroke-width="1.2"/><line x1="50" y1="56" x2="62" y2="56" stroke="currentColor" stroke-width="1.2"/><line x1="56" y1="50" x2="56" y2="62" stroke="currentColor" stroke-width="1.2"/></svg>
        </div>
        <h3>暂无研报记录</h3>
        <p>手动添加或从「策略看板」批量导入分析结果</p>
        <el-button type="primary" size="small" @click="showAdd = true">添加第一份研报</el-button>
      </div>

      <template v-else>
      <el-table :data="reports" stripe size="small" @selection-change="s => selected = s" max-height="500">
        <el-table-column type="selection" width="40"/>
        <el-table-column prop="id" label="ID" width="60"/>
        <el-table-column prop="ticker" label="代码" width="90"/>
        <el-table-column prop="reportDate" label="日期" width="110"/>
        <el-table-column prop="source" label="来源" width="100"/>
        <el-table-column prop="sector" label="行业" width="90"/>
        <el-table-column prop="analystRecommendation" label="评级" width="110"/>
        <el-table-column prop="objectiveRatio" label="客观占比" width="90"><template #default="{row}">{{ (row.objectiveRatio||0).toFixed(2) }}</template></el-table-column>
        <el-table-column prop="subjectiveRatio" label="主观占比" width="90"><template #default="{row}">{{ (row.subjectiveRatio||0).toFixed(2) }}</template></el-table-column>
        <el-table-column prop="reportText" label="报告内容" min-width="200"><template #default="{row}">{{ (row.reportText||'').substring(0, 80) }}...</template></el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="editReport(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:12px;color:#6b7280;font-size:0.85rem">
        共 {{ reports.length }} 条记录
        <el-button size="small" style="margin-left:8px" @click="loadData">刷新</el-button>
      </div>
      </template>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog :title="editing ? '编辑报告' : '新增报告'" v-model="showAdd" width="640px" destroy-on-close>
      <el-form :model="form" label-width="90px" size="small">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="股票代码"><el-input v-model="form.ticker" placeholder="AAPL"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="报告日期"><el-input v-model="form.reportDate" type="date"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="来源"><el-input v-model="form.source" placeholder="券商/机构名"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="行业"><el-input v-model="form.sector" placeholder="科技/金融/医药"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="分析师评级">
            <el-select v-model="form.analystRecommendation" style="width:100%">
              <el-option v-for="r in ['Very Bullish','Bullish','Neutral','Bearish','Very Bearish']" :key="r" :label="r" :value="r"/>
            </el-select>
          </el-form-item></el-col>
          <el-col :span="6"><el-form-item label="客观占比"><el-input-number v-model="form.objectiveRatio" :min="0" :max="1" :step="0.01" controls-position="right"/></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="主观占比"><el-input-number v-model="form.subjectiveRatio" :min="0" :max="1" :step="0.01" controls-position="right"/></el-form-item></el-col>
        </el-row>
        <el-form-item label="报告内容"><el-input v-model="form.reportText" type="textarea" :rows="6" placeholder="研报正文..."/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="saveReport">{{ editing ? '保存修改' : '添加' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getReports, createReport, updateReport, deleteReport, deleteReportsBatch } from '@/api'

const reports = ref([])
const selected = ref([])
const showAdd = ref(false)
const editing = ref(false)
const editId = ref(null)

const form = ref({ ticker:'', reportDate:'', source:'', sector:'', analystRecommendation:'Neutral', objectiveRatio:0.5, subjectiveRatio:0.5, reportText:'' })

function resetForm() {
  form.value = { ticker:'', reportDate:'', source:'', sector:'', analystRecommendation:'Neutral', objectiveRatio:0.5, subjectiveRatio:0.5, reportText:'' }
  editing.value = false; editId.value = null
}

function editReport(row) {
  editing.value = true; editId.value = row.id; showAdd.value = true
  form.value = { ...row, reportDate: row.reportDate || '' }
}

async function saveReport() {
  try {
    const payload = { ...form.value, id: undefined }
    if (editing.value) { await updateReport(editId.value, payload) }
    else { await createReport(payload) }
    showAdd.value = false; resetForm(); await loadData()
  } catch (e) {
    alert('保存失败: ' + (e.response?.data?.error || e.message))
  }
}

async function handleDelete(id) {
  try { await deleteReport(id); await loadData() }
  catch (e) { alert('删除失败: ' + (e.response?.data?.error || e.message)) }
}

async function handleBatchDelete() {
  try {
    await deleteReportsBatch(selected.value.map(s => s.id))
    selected.value = []; await loadData()
  } catch (e) { alert('批量删除失败: ' + (e.response?.data?.error || e.message)) }
}

async function loadData() {
  try { const r = await getReports(); reports.value = r.data } catch (_) { reports.value = [] }
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; padding-bottom: 12px; border-bottom: 1px solid var(--border, #e4e4e0); }
.card-header h2 { font-family: var(--font-display, serif); font-size: 1rem; font-weight: 700; letter-spacing: 0.03em; }
.header-actions { display: flex; gap: 8px; }

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
  opacity: 0.6;
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
