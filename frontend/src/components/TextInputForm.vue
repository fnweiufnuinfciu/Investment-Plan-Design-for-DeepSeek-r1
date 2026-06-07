<template>
  <div>
    <el-form :model="textForm" label-width="95px" size="small">
      <el-row :gutter="10">
        <el-col :span="6">
          <el-form-item label="股票代码">
            <el-input v-model="textForm.ticker" placeholder="如 600519"/>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="报告日期">
            <el-input v-model="textForm.reportDate" type="date"/>
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
              <el-option v-for="r in RATINGS" :key="r" :label="r" :value="r"/>
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
            <el-input-number v-model="textForm.confidence" :min="0" :max="1" :step="0.01" controls-position="right" style="width:100%" placeholder="可选"/>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="报告原文">
        <el-input v-model="textForm.reportText" type="textarea" :rows="8"
          placeholder="直接粘贴研报全文即可。示例：华鑫证券有限责任公司孙山山近期对贵州茅台进行研究..."
        />
      </el-form-item>
      <div style="display:flex;align-items:center;gap:10px">
        <el-button size="small" type="primary" @click="$emit('add')">添加到列表</el-button>
        <el-button size="small" @click="$emit('loadSample')">加载示例</el-button>
        <el-tag v-if="apiOk" type="success" size="small">API 已配置</el-tag>
        <el-tag v-else type="danger" size="small">API 未配置</el-tag>
        <span v-if="textReports.length" style="font-size:0.85rem;color:#6b7280;margin-left:auto">
          已添加 <b>{{ textReports.length }}</b> 条报告
        </span>
      </div>
    </el-form>

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
            <el-button size="small" type="danger" @click="$emit('remove', $index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
const RATINGS = ['Very Bullish','Bullish','Neutral','Bearish','Very Bearish']
defineProps({
  textForm: { type: Object, required: true },
  textReports: { type: Array, required: true },
  apiOk: { type: Boolean, default: false },
})
defineEmits(['add', 'remove', 'loadSample'])
</script>
