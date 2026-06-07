<template>
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
            <el-option label="多空组合" value="long_short"/>
            <el-option label="仅做多" value="long_only"/>
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
    <el-button type="primary" :loading="loading" @click="$emit('run')" style="width:100%;margin-top:8px">
      {{ loading ? '正在分析...' : '生成全量分析' }}
    </el-button>
  </el-form>
</template>

<script setup>
defineProps({
  settings: { type: Object, required: true },
  loading: { type: Boolean, default: false },
})
defineEmits(['run'])
</script>
