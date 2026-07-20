<template>
<div>
  <div class="pg-h">
    <h2>报警规则配置</h2>
    <div class="pg-h-right">
      <el-input v-model="kw" placeholder="搜索设备/测点" clearable style="width:200px" />
      <el-button type="primary" @click="showAdd"><el-icon><Plus /></el-icon>新增报警配置</el-button>
    </div>
  </div>

  <div class="ac-layout">
    <!-- 左侧：规则列表 -->
    <div class="ac-left">
      <div class="ac-left-h">
        <span>规则列表</span>
        <el-tag size="small">{{ store.alarmRules.length }} 条</el-tag>
      </div>
      <div class="ac-list">
        <div v-for="r in filtered" :key="r.id"
             class="ac-list-i" :class="{on: current?.id === r.id}"
             @click="selectRule(r)">
          <div class="ac-list-row1">
            <span class="ac-list-name">{{ r.device }} - {{ r.point }}</span>
            <el-tag :type="r.level===1?'danger':r.level===2?'warning':'info'" size="small">{{ lvlTxt(r.level) }}</el-tag>
          </div>
          <div class="ac-list-row2">
            <span>触发：{{ r.cond }} {{ r.val }}<span v-if="r.delay"> · 延迟{{ r.delay }}s</span></span>
            <el-switch v-model="r.enabled" size="small" @click.stop />
          </div>
          <div class="ac-list-row3">
            <el-tag v-for="c in r.channels" :key="c" size="small" effect="plain" style="margin-right:2px">{{ chLabel(c) }}</el-tag>
          </div>
        </div>
        <div v-if="!filtered.length" class="ac-empty">未找到匹配的规则</div>
      </div>
    </div>

    <!-- 右侧：配置详情 -->
    <div class="ac-right">
      <div class="ac-config-h">
        <span>{{ form.id ? '编辑报警配置' : '新增报警配置' }}</span>
        <span class="ac-config-tips" v-if="!form.id">带 <em class="req">*</em> 为必填项</span>
      </div>

      <!-- 点项配置 -->
      <div class="ac-section">
        <div class="ac-section-h"><span class="ac-num">1</span>点项配置</div>
        <el-row :gutter="14">
          <el-col :span="8">
            <div class="ac-form">
              <label><em class="req">*</em>点项</label>
              <el-select v-model="form.pointKey" placeholder="请选择测点" filterable style="width:100%">
                <el-option v-for="d in store.devices" :key="d.id" :value="d.id" :label="d.name + ' / ' + Object.keys(d.params)[0]" />
              </el-select>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="ac-form">
              <label><em class="req">*</em>平台点描述</label>
              <el-input v-model="form.pointDesc" placeholder="选择测点后自动填充" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="ac-form">
              <label><em class="req">*</em>报警点描述</label>
              <el-input v-model="form.alarmDesc" placeholder="请输入报警点描述" />
            </div>
          </el-col>
        </el-row>
        <div style="text-align:right;margin-top:6px">
          <el-button type="primary" @click="pickPoint"><el-icon><Aim /></el-icon>选择测点</el-button>
        </div>
      </div>

      <!-- 报警配置 -->
      <div class="ac-section">
        <div class="ac-section-h"><span class="ac-num">2</span>报警配置</div>
        <el-row :gutter="14">
          <el-col :span="6">
            <div class="ac-form">
              <label><em class="req">*</em>所属机组</label>
              <el-select v-model="form.unit" placeholder="请选择所属机组" style="width:100%">
                <el-option v-for="u in store.units" :key="u.id" :value="u.id" :label="u.name" />
              </el-select>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="ac-form">
              <label><em class="req">*</em>所属专业</label>
              <el-select v-model="form.dept" placeholder="请选择所属专业" style="width:100%">
                <el-option value="锅炉" label="锅炉" />
                <el-option value="汽轮机" label="汽轮机" />
                <el-option value="电气" label="电气" />
                <el-option value="热工" label="热工" />
              </el-select>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="ac-form">
              <label>系统/设备</label>
              <el-select v-model="form.device" placeholder="请选择系统/设备" filterable style="width:100%">
                <el-option v-for="d in unitDevices" :key="d.id" :value="d.name" :label="d.name" />
              </el-select>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="ac-form">
              <label><em class="req">*</em>报警类型</label>
              <el-select v-model="form.type" placeholder="请选择报警类型" style="width:100%">
                <el-option value="阈值报警" label="阈值报警" />
                <el-option value="智能预警" label="智能预警" />
                <el-option value="趋势报警" label="趋势报警" />
                <el-option value="开关量变位" label="开关量变位" />
              </el-select>
            </div>
          </el-col>
        </el-row>
        <el-row :gutter="14" style="margin-top:10px">
          <el-col :span="6">
            <div class="ac-form">
              <label><em class="req">*</em>延迟时长(s)</label>
              <el-input-number v-model="form.delay" :min="0" :max="3600" style="width:100%" />
            </div>
          </el-col>
          <el-col :span="6">
            <div class="ac-form">
              <label>触发次数</label>
              <el-input-number v-model="form.triggers" :min="1" :max="100" style="width:100%" />
            </div>
          </el-col>
          <el-col :span="6">
            <div class="ac-form">
              <label>通知方式</label>
              <el-select v-model="form.channels" multiple placeholder="选择通知方式" style="width:100%">
                <el-option v-for="ch in store.channels" :key="ch.key" :value="ch.key" :label="ch.label" />
              </el-select>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="ac-form">
              <label>启用</label>
              <div><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" /></div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 报警规则 -->
      <div class="ac-section">
        <div class="ac-section-h">
          <span><span class="ac-num">3</span>报警规则</span>
          <el-radio-group v-model="form.mode" size="small">
            <el-radio-button value="analog">模拟量</el-radio-button>
            <el-radio-button value="switch">开关量</el-radio-button>
          </el-radio-group>
        </div>
        <div class="ac-rules-thead">
          <div class="ac-th" style="flex:0 0 30px">序号</div>
          <div class="ac-th" style="flex:1.2">报警等级</div>
          <div class="ac-th" style="flex:1.2">规则类型</div>
          <div class="ac-th" style="flex:0 0 90px">死区</div>
          <div class="ac-th" style="flex:2">值</div>
          <div class="ac-th" style="flex:0 0 50px">操作</div>
        </div>
        <div v-for="(r, i) in form.rules" :key="i" class="ac-rule-row">
          <div class="ac-td" style="flex:0 0 30px">{{ i + 1 }}</div>
          <div class="ac-td" style="flex:1.2">
            <el-select v-model="r.level" size="small" style="width:100%">
              <el-option :value="1" label="一级报警" />
              <el-option :value="2" label="二级报警" />
              <el-option :value="3" label="智能预警" />
            </el-select>
          </div>
          <div class="ac-td" style="flex:1.2">
            <el-select v-model="r.cond" size="small" style="width:100%">
              <el-option v-if="form.mode==='analog'" value=">" label="> 大于" />
              <el-option v-if="form.mode==='analog'" value="<" label="< 小于" />
              <el-option v-if="form.mode==='analog'" value=">=" label="≥ 大于等于" />
              <el-option v-if="form.mode==='analog'" value="<=" label="≤ 小于等于" />
              <el-option v-if="form.mode==='analog'" value="==" label="= 等于" />
              <el-option v-if="form.mode==='analog'" value="trend" label="趋势异常" />
              <el-option v-if="form.mode==='switch'" value="==1" label="= 1 (合)" />
              <el-option v-if="form.mode==='switch'" value="==0" label="= 0 (开)" />
            </el-select>
          </div>
          <div class="ac-td" style="flex:0 0 90px">
            <el-input-number v-model="r.deadband" size="small" :min="0" :precision="2" :step="0.1" style="width:100%" />
          </div>
          <div class="ac-td" style="flex:2;display:flex;gap:6px;align-items:center">
            <el-radio-group v-model="r.valMode" size="small">
              <el-radio-button value="manual">手动输入</el-radio-button>
              <el-radio-button value="point">选择测点</el-radio-button>
            </el-radio-group>
            <el-input v-if="r.valMode==='manual'" v-model="r.val" size="small" placeholder="请输入" style="flex:1" />
            <el-select v-else v-model="r.val" size="small" placeholder="请选择" style="flex:1">
              <el-option v-for="d in store.devices" :key="d.id" :value="d.id" :label="d.name" />
            </el-select>
          </div>
          <div class="ac-td" style="flex:0 0 50px;text-align:center">
            <el-button link type="danger" size="small" @click="delRule(i)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="ac-add-rule" @click="addRule">
          <el-icon><Plus /></el-icon> 添加规则
        </div>
      </div>

      <!-- 通知渠道说明 -->
      <div class="ac-channels" v-if="form.channels.length">
        <div class="ac-section-h">已选通知渠道</div>
        <div class="ac-ch-grid">
          <div v-for="ch in selectedChannels" :key="ch.key" class="ac-ch-card">
            <span class="ac-ch-ic">{{ ch.icon }}</span>
            <div>
              <div class="ac-ch-lb">{{ ch.label }} <span class="ac-ch-key">({{ ch.key }})</span></div>
              <div class="ac-ch-desc">{{ ch.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="ac-footer">
        <el-button @click="resetForm">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Aim, Delete } from '@element-plus/icons-vue'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const kw = ref('')
const current = ref(null)

const emptyForm = () => ({
  id: null, pointKey: '', pointDesc: '', alarmDesc: '',
  unit: 'U1', dept: '锅炉', device: '', type: '阈值报警',
  delay: 0, triggers: 1, channels: ['站内'], enabled: true,
  mode: 'analog',
  rules: [
    { level: 2, cond: '>', deadband: 0, valMode: 'manual', val: '' }
  ]
})

const form = ref(emptyForm())

const filtered = computed(() => {
  const k = kw.value
  if (!k) return store.alarmRules
  return store.alarmRules.filter(r => r.device.includes(k) || r.point.includes(k))
})

const unitDevices = computed(() => store.unitDevices(form.value.unit))

const selectedChannels = computed(() =>
  store.channels.filter(c => form.value.channels.includes(c.key))
)

const lvlTxt = (l) => l === 1 ? '一级' : l === 2 ? '二级' : '智能预警'
const chLabel = (key) => store.channels.find(c => c.key === key)?.label || key

const selectRule = (r) => {
  current.value = r
  form.value = {
    id: r.id,
    pointKey: r.id, pointDesc: r.device + '-' + r.point,
    alarmDesc: r.device + r.point + ' 异常',
    unit: r.unit || 'U1', dept: '锅炉', device: r.device,
    type: '阈值报警', delay: r.delay || 0, triggers: 1,
    channels: r.channels || ['站内'], enabled: r.enabled,
    mode: 'analog',
    rules: [{ level: r.level, cond: r.cond, deadband: 0, valMode: 'manual', val: r.val }]
  }
}

const showAdd = () => {
  current.value = null
  form.value = emptyForm()
}

const pickPoint = () => {
  ElMessage.info('请在左侧点项下拉中选择测点')
}

const addRule = () => {
  form.value.rules.push({ level: 2, cond: '>', deadband: 0, valMode: 'manual', val: '' })
}

const delRule = (i) => {
  if (form.value.rules.length <= 1) {
    ElMessage.warning('至少需要保留一条规则')
    return
  }
  form.value.rules.splice(i, 1)
}

const save = () => {
  if (!form.value.device) { ElMessage.warning('请选择系统/设备'); return }
  if (!form.value.unit) { ElMessage.warning('请选择机组'); return }
  if (form.value.rules.some(r => r.val === '' || r.val === null)) {
    ElMessage.warning('请填写所有规则的值')
    return
  }
  // 同步到 store：每条规则生成一条
  if (form.value.id) {
    // 编辑模式：删除原规则，按 form 重新添加
    store.delRule(form.value.id)
  }
  form.value.rules.forEach(r => {
    store.addRule({
      unit: form.value.unit,
      device: form.value.device,
      point: form.value.alarmDesc || (form.value.device + '-' + form.value.pointDesc),
      level: r.level,
      cond: r.cond,
      val: typeof r.val === 'string' ? parseFloat(r.val) : r.val,
      channels: form.value.channels,
      enabled: form.value.enabled,
      delay: form.value.delay
    })
  })
  ElMessage.success('保存成功，共 ' + form.value.rules.length + ' 条规则')
  showAdd()
}

const resetForm = () => {
  current.value = null
  form.value = emptyForm()
}

watch(() => form.value.unit, () => {
  if (form.value.device && !unitDevices.value.find(d => d.name === form.value.device)) {
    form.value.device = ''
  }
})
</script>

<style scoped>
.pg-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.pg-h-right { display: flex; gap: 10px; align-items: center; }
h2 { font-size: 16px; font-weight: 500; }

.ac-layout { display: grid; grid-template-columns: 360px 1fr; gap: 14px; min-height: calc(100vh - 180px); }

/* 左侧列表 */
.ac-left { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 12px; display: flex; flex-direction: column; }
.ac-left-h { display: flex; justify-content: space-between; align-items: center; padding: 4px 4px 10px; border-bottom: 0.5px solid #1e293b; font-size: 13px; color: #94a3b8; font-weight: 500; }
.ac-list { flex: 1; overflow-y: auto; margin-top: 8px; }
.ac-list-i { padding: 10px; border-radius: 6px; cursor: pointer; margin-bottom: 6px; border: 0.5px solid transparent; background: #0a0e17; }
.ac-list-i:hover { border-color: #1e293b; }
.ac-list-i.on { border-color: #3b82f6; background: rgba(59,130,246,0.08); }
.ac-list-row1 { display: flex; justify-content: space-between; align-items: center; font-size: 12px; }
.ac-list-name { color: #e2e8f0; font-weight: 500; }
.ac-list-row2 { display: flex; justify-content: space-between; align-items: center; font-size: 11px; color: #94a3b8; margin-top: 4px; }
.ac-list-row3 { margin-top: 4px; }
.ac-empty { text-align: center; padding: 30px; color: #64748b; font-size: 12px; }

/* 右侧配置 */
.ac-right { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 18px; display: flex; flex-direction: column; gap: 14px; overflow-y: auto; }
.ac-config-h { display: flex; justify-content: space-between; align-items: center; padding-bottom: 6px; border-bottom: 0.5px solid #1e293b; font-size: 14px; color: #e2e8f0; font-weight: 500; }
.ac-config-tips { font-size: 11px; color: #94a3b8; font-weight: 400; }
.req { color: #ef4444; font-style: normal; }

.ac-section { background: #0a0e17; border-radius: 8px; padding: 12px 14px; }
.ac-section-h { display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: #e2e8f0; font-weight: 500; margin-bottom: 10px; }
.ac-num { display: inline-block; width: 18px; height: 18px; background: #3b82f6; color: #fff; border-radius: 4px; text-align: center; line-height: 18px; font-size: 11px; margin-right: 6px; }

.ac-form { display: flex; flex-direction: column; gap: 4px; }
.ac-form label { font-size: 12px; color: #cbd5e1; }

/* 规则列表 */
.ac-rules-thead, .ac-rule-row { display: flex; gap: 8px; padding: 8px 4px; align-items: center; }
.ac-rules-thead { background: rgba(59,130,246,0.06); border-radius: 4px; margin-bottom: 4px; }
.ac-th { font-size: 12px; color: #94a3b8; text-align: center; }
.ac-td { font-size: 12px; }
.ac-rule-row { border-bottom: 0.5px dashed #1e293b; }
.ac-add-rule { text-align: center; padding: 10px; background: rgba(59,130,246,0.06); border: 0.5px dashed #3b82f6; border-radius: 4px; color: #3b82f6; cursor: pointer; font-size: 12px; margin-top: 6px; }
.ac-add-rule:hover { background: rgba(59,130,246,0.12); }

/* 通知渠道 */
.ac-channels { background: #0a0e17; border-radius: 8px; padding: 12px 14px; }
.ac-ch-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
.ac-ch-card { display: flex; gap: 8px; padding: 10px; background: #111827; border-radius: 6px; }
.ac-ch-ic { font-size: 20px; }
.ac-ch-lb { font-size: 12px; color: #e2e8f0; font-weight: 500; }
.ac-ch-key { color: #64748b; font-size: 10px; font-weight: 400; }
.ac-ch-desc { font-size: 11px; color: #94a3b8; margin-top: 2px; }

.ac-footer { display: flex; justify-content: flex-end; gap: 8px; padding-top: 6px; }
</style>
