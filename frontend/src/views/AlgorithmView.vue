<template>
  <div class="algo">
    <el-alert
      :closable="false"
      type="info"
      show-icon
      title="工业多变量时序建模算法服务"
      description="上传测点 CSV 或生成仿真数据，依次进行数据概览、清洗、ARX 多变量建模与残差预警检测。计算在后端 FastAPI 算法引擎完成。"
    />

    <!-- 数据来源 -->
    <el-card class="blk" shadow="never">
      <template #header><span class="bh">① 数据来源</span></template>
      <el-upload
        class="up"
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept=".csv"
        :on-change="onFileChange"
      >
        <el-icon class="up-ic"><UploadFilled /></el-icon>
        <div class="up-t">将 CSV 拖到此处，或<em>点击选择</em></div>
        <div class="up-s">需要包含 time 列与若干测点列（如 xxx.PV）</div>
      </el-upload>
      <div class="row">
        <el-button :loading="genLoading" @click="generateSim">生成仿真数据（7天）</el-button>
        <el-button type="primary" :disabled="!pendingFile" :loading="upLoading" @click="upload">上传并分析</el-button>
        <span v-if="fileId" class="fid">当前数据 ID：<code>{{ fileId }}</code></span>
      </div>
    </el-card>

    <!-- 数据概览 -->
    <el-card v-if="profile" class="blk" shadow="never">
      <template #header><span class="bh">② 数据概览</span></template>
      <div class="metrics">
        <div class="m"><span>总行数</span><b>{{ profile.total_rows }}</b></div>
        <div class="m"><span>测点数</span><b>{{ profile.variable_count }}</b></div>
        <div class="m"><span>采样周期(s)</span><b>{{ profile.sample_interval_seconds }}</b></div>
        <div class="m"><span>时间范围</span><b class="sm">{{ profile.time_start }} ~ {{ profile.time_end }}</b></div>
      </div>
      <el-table :data="profile.variables" size="small" max-height="320" class="tb">
        <el-table-column prop="name" label="测点" min-width="140" />
        <el-table-column prop="var_type" label="类型" width="90" />
        <el-table-column label="均值" width="90"><template #default="{row}">{{ fmt(row.mean) }}</template></el-table-column>
        <el-table-column label="标准差" width="90"><template #default="{row}">{{ fmt(row.std) }}</template></el-table-column>
        <el-table-column label="缺失率" width="90"><template #default="{row}">{{ (row.missing_rate*100).toFixed(1) }}%</template></el-table-column>
        <el-table-column label="可建模" width="80">
          <template #default="{row}"><el-tag :type="row.suitable_for_modeling ? 'success' : 'info'" size="small">{{ row.suitable_for_modeling ? '是' : '否' }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 数据清洗 -->
    <el-card v-if="fileId" class="blk" shadow="never">
      <template #header><span class="bh">③ 数据清洗</span></template>
      <div class="row">
        <el-select v-model="cleanCfg.resample_interval" size="small" style="width:150px">
          <el-option label="1min" value="1min" /><el-option label="5min" value="5min" /><el-option label="10min" value="10min" />
        </el-select>
        <el-select v-model="cleanCfg.missing_strategy" size="small" style="width:150px">
          <el-option label="插值" value="interpolate" /><el-option label="前向填充" value="ffill" />
        </el-select>
        <el-button type="primary" :loading="cleanLoading" @click="runClean">执行清洗</el-button>
        <span v-if="cleanId" class="fid">clean_id：<code>{{ cleanId }}</code></span>
      </div>
      <div v-if="cleanReport" class="metrics">
        <div class="m"><span>异常点</span><b>{{ cleanReport.anomaly_count }}</b></div>
        <div class="m"><span>缺失(前)</span><b>{{ cleanReport.missing_before }}</b></div>
        <div class="m"><span>缺失(后)</span><b>{{ cleanReport.missing_after }}</b></div>
        <div class="m"><span>清洗后行数</span><b>{{ cleanReport.rows_after_clean ?? cleanReport.rows_after }}</b></div>
      </div>
    </el-card>

    <!-- ARX 建模 -->
    <el-card v-if="fileId" class="blk" shadow="never">
      <template #header><span class="bh">④ ARX 多变量建模（闭环寻优）</span></template>
      <div class="row">
        <el-input v-model="target" size="small" placeholder="目标变量，如 FC101.PV" style="width:240px" />
        <el-button type="primary" :loading="modelLoading" @click="runModel">开始建模</el-button>
      </div>
      <div v-if="modelResult" class="metrics">
        <div class="m"><span>训练拟合度</span><b :class="ok(modelResult.train_fit)">{{ modelResult.train_fit }}%</b></div>
        <div class="m"><span>验证拟合度</span><b :class="ok(modelResult.test_fit)">{{ modelResult.test_fit }}%</b></div>
        <div class="m"><span>目标函数</span><b>{{ fmt(modelResult.objective) }}</b></div>
        <div class="m"><span>选择输入数</span><b>{{ (modelResult.selected_inputs||[]).length }}</b></div>
      </div>
      <div v-if="modelResult" class="sel-in">
        <span class="lbl">选中输入变量：</span>
        <el-tag v-for="s in (modelResult.selected_inputs||[])" :key="s" size="small" class="it">{{ s }}</el-tag>
      </div>
    </el-card>

    <!-- 残差预警 -->
    <el-card v-if="fileId" class="blk" shadow="never">
      <template #header><span class="bh">⑤ 残差预警检测</span></template>
      <div class="row">
        <el-input v-model="alarmTag" size="small" placeholder="测点名称，如 ICS_HC_AS130.AV" style="width:240px" />
        <el-input v-model="alarmVal" type="number" size="small" placeholder="当前值" style="width:140px" />
        <el-button type="primary" :loading="alarmLoading" @click="runAlarm">检测</el-button>
      </div>
      <el-alert
        v-if="alarmResult"
        :type="alarmResult.is_alarm ? 'error' : 'success'"
        :closable="false"
        :title="alarmResult.is_alarm ? '🔴 报警' : '🟢 正常'"
        :description="alarmResult.alarm_message || '当前测点残差在阈值内'"
      />
      <div v-if="alarmResult" class="metrics">
        <div class="m"><span>当前值</span><b>{{ fmt(alarmResult.current_value) }}</b></div>
        <div class="m"><span>预测值</span><b>{{ fmt(alarmResult.predicted_value) }}</b></div>
        <div class="m"><span>残差</span><b>{{ fmt(alarmResult.residual) }}</b></div>
        <div class="m"><span>级别</span><b>{{ alarmResult.alarm_level || '-' }}</b></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { algoApi } from '@/api/algorithm'

const pendingFile = ref(null)
const fileId = ref('')
const cleanId = ref('')
const profile = ref(null)
const cleanReport = ref(null)
const modelResult = ref(null)
const alarmResult = ref(null)

const target = ref('')
const alarmTag = ref('')
const alarmVal = ref('5.5')

const upLoading = ref(false)
const genLoading = ref(false)
const cleanLoading = ref(false)
const modelLoading = ref(false)
const alarmLoading = ref(false)

const cleanCfg = ref({ resample_interval: '1min', missing_strategy: 'interpolate' })

const fmt = (v) => (v == null || Number.isNaN(v)) ? '-' : (typeof v === 'number' ? (Math.abs(v) > 1e4 || (v % 1 !== 0) ? v.toFixed(4) : v) : v)
const ok = (v) => (typeof v === 'number' && v >= 80 ? 'good' : 'warn')

const onFileChange = (file) => {
  if (file && file.raw) { pendingFile.value = file.raw; ElMessage.success(`已选择 ${file.name}`) }
}

const upload = async () => {
  if (!pendingFile.value) return
  upLoading.value = true
  try {
    const res = await algoApi.uploadCsv(pendingFile.value)
    fileId.value = res.file_id
    target.value = (res.columns || [])[0] || ''
    alarmTag.value = target.value
    await loadProfile()
    ElMessage.success(`上传成功：${res.rows} 行 / ${(res.columns||[]).length} 列`)
  } catch (e) {
    ElMessage.error('上传失败：' + (e.response?.data?.detail || e.message))
  } finally { upLoading.value = false }
}

const generateSim = async () => {
  genLoading.value = true
  try {
    const res = await algoApi.generateSimulation({ duration_days: 7, tags: ['FC101', 'FC102'] })
    fileId.value = res.file_id
    target.value = (res.columns || []).find(c => c.endsWith('.PV')) || (res.columns || [])[0] || ''
    alarmTag.value = target.value
    await loadProfile()
    ElMessage.success('仿真数据已生成')
  } catch (e) {
    ElMessage.error('生成失败：' + (e.response?.data?.detail || e.message))
  } finally { genLoading.value = false }
}

const loadProfile = async () => {
  profile.value = await algoApi.profile(fileId.value)
  target.value = target.value || (profile.value.variables?.[0]?.name || '')
  alarmTag.value = alarmTag.value || target.value
}

const runClean = async () => {
  cleanLoading.value = true; cleanReport.value = null; cleanId.value = ''
  try {
    const res = await algoApi.clean(fileId.value, cleanCfg.value)
    cleanId.value = res.clean_id
    cleanReport.value = res.report
    ElMessage.success(`清洗完成：${res.clean_id}`)
  } catch (e) {
    ElMessage.error('清洗失败：' + (e.response?.data?.detail || e.message))
  } finally { cleanLoading.value = false }
}

const runModel = async () => {
  const cid = cleanId.value || fileId.value
  if (!target.value) { ElMessage.warning('请填写目标变量'); return }
  modelLoading.value = true; modelResult.value = null
  try {
    modelResult.value = await algoApi.arxModeling(cid, { target: target.value, optimize_preprocessing: true })
    ElMessage.success('建模完成')
  } catch (e) {
    ElMessage.error('建模失败：' + (e.response?.data?.detail || e.message))
  } finally { modelLoading.value = false }
}

const runAlarm = async () => {
  const cid = cleanId.value || fileId.value
  if (!alarmTag.value) { ElMessage.warning('请填写测点名称'); return }
  alarmLoading.value = true; alarmResult.value = null
  try {
    alarmResult.value = await algoApi.residualCheck(cid, { tag_name: alarmTag.value, current_value: Number(alarmVal.value), lookback_hours: 24 })
  } catch (e) {
    ElMessage.error('检测失败：' + (e.response?.data?.detail || e.message))
  } finally { alarmLoading.value = false }
}
</script>

<style scoped>
.algo { display: flex; flex-direction: column; gap: 14px; }
.blk { background: #161d2a; border: 0.5px solid #1e293b; color: #e2e8f0; }
.blk :deep(.el-card__header) { border-bottom: 0.5px solid #1e293b; padding: 12px 16px; }
.bh { font-size: 13px; font-weight: 600; color: #e2e8f0; }
.up { width: 100%; }
.up :deep(.el-upload-dragger) { background: #0d1117; border: 1px dashed #334155; }
.up-ic { font-size: 40px; color: #3b82f6; }
.up-t { color: #cbd5e1; margin-top: 8px; } .up-t em { color: #3b82f6; font-style: normal; }
.up-s { color: #64748b; font-size: 12px; margin-top: 4px; }
.row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-top: 12px; }
.fid { color: #94a3b8; font-size: 12px; } .fid code { color: #22d3ee; }
.metrics { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 12px; }
.m { background: #0d1117; border: 0.5px solid #1e293b; border-radius: 8px; padding: 10px 14px; min-width: 120px; }
.m span { display: block; font-size: 11px; color: #64748b; margin-bottom: 4px; }
.m b { font-size: 16px; color: #e2e8f0; } .m b.sm { font-size: 12px; }
.m b.good { color: #22c55e; } .m b.warn { color: #f59e0b; }
.tb { margin-top: 12px; background: #0d1117; }
.sel-in { margin-top: 12px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.sel-in .lbl { font-size: 12px; color: #94a3b8; }
.it { background: rgba(59,130,246,0.12); color: #3b82f6; border: none; }
</style>
