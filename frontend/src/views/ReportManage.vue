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
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-header h2 { font-size: 1.05rem; font-weight: 700; }
.header-actions { display: flex; gap: 8px; }
</style>
