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
            <span class="ac-list-name">{{ uName(r.unit) }} · {{ r.device }} - {{ r.point }}</span>
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
                <el-option v-for="d in store.devices" :key="d.id" :value="d.id" :label="d.name + '（' + uName(d.unit) + ' · ' + d.dept + '） / ' + Object.keys(d.params)[0]" />
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
                <el-option value="辅网" label="辅网" />
              </el-select>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="ac-form">
              <label>设备</label>
              <el-select v-model="form.device" placeholder="请选择设备" filterable style="width:100%">
                <el-option v-for="d in unitDevices" :key="d.id" :value="d.name" :label="d.name + '（' + d.dept + '）'" />
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
const uName = (uid) => store.units.find(u => u.id === uid)?.name || uid

const selectRule = (r) => {
  current.value = r
  form.value = {
    id: r.id,
    pointKey: r.id, pointDesc: r.device + '-' + r.point,
    alarmDesc: r.device + r.point + ' 异常',
    unit: r.unit || 'U1', dept: store.devices.find(d => d.name === r.device)?.dept || '锅炉', device: r.device,
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
  if (!form.value.device) { ElMessage.warning('请选择设备'); return }
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
.pg-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.pg-h-right { display: flex; gap: 10px; align-items: center; }
h2 { font-size: 17px; font-weight: 700; color: #d4ecff; letter-spacing: 0.5px; }

.ac-layout { display: grid; grid-template-columns: 360px 1fr; gap: 14px; min-height: calc(100vh - 180px); }

/* 左侧列表 */
.ac-left { background: linear-gradient(180deg, rgba(12,26,46,0.7), rgba(8,18,34,0.65)); border: 1px solid rgba(62,170,255,0.15); border-radius: 8px; padding: 14px; display: flex; flex-direction: column; box-shadow: 0 2px 12px rgba(0,10,30,0.25); }
.ac-left-h { display: flex; justify-content: space-between; align-items: center; padding: 6px 6px 12px; border-bottom: 1px solid rgba(62,170,255,0.15); font-size: 14px; color: #d4ecff; font-weight: 600; letter-spacing: 0.5px; }
.ac-list { flex: 1; overflow-y: auto; margin-top: 10px; }
.ac-list-i { padding: 12px; border-radius: 6px; cursor: pointer; margin-bottom: 8px; border: 1px solid transparent; background: linear-gradient(180deg, rgba(10,24,42,0.55), rgba(6,16,30,0.5)); transition: all 0.2s; }
.ac-list-i:hover { border-color: rgba(62,170,255,0.2); background: linear-gradient(180deg, rgba(12,28,50,0.6), rgba(8,20,38,0.55)); }
.ac-list-i.on { border-color: #3eaaff; background: linear-gradient(180deg, rgba(20,50,85,0.4), rgba(12,30,52,0.35)); box-shadow: 0 0 16px rgba(62,170,255,0.12), inset 0 0 20px rgba(62,170,255,0.03); }
.ac-list-row1 { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.ac-list-name { color: #e2e8f0; font-weight: 600; }
.ac-list-row2 { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #9fb6cf; margin-top: 6px; }
.ac-list-row3 { margin-top: 4px; }
.ac-empty { text-align: center; padding: 30px; color: #8fb0cf; font-size: 12px; }

/* 右侧配置 */
.ac-right { background: linear-gradient(180deg, rgba(10,26,50,0.6), rgba(6,16,32,0.55)); border: 1px solid rgba(62,170,255,0.18); border-radius: 8px; padding: 18px; display: flex; flex-direction: column; gap: 14px; overflow-y: auto; position: relative; box-shadow: 0 4px 24px rgba(0,10,30,0.3), inset 0 1px 0 rgba(62,170,255,0.05); }
.ac-right::before, .ac-right::after { content:''; position:absolute; width:14px; height:14px; border:1.5px solid #3eaaff; box-shadow:0 0 6px rgba(62,170,255,0.45); pointer-events:none; }
.ac-right::before { top:-1px; left:-1px; border-right:none; border-bottom:none; }
.ac-right::after { bottom:-1px; right:-1px; border-left:none; border-top:none; }
.ac-config-h { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px 12px 20px; background: linear-gradient(90deg, rgba(62,170,255,0.18), rgba(62,170,255,0.03)); border-left: 4px solid #3eaaff; font-size: 17px; color: #eaf4ff; font-weight: 700; letter-spacing: 1px; margin: -18px -18px 10px; box-shadow: -4px 0 16px -3px rgba(62,170,255,0.25); }
.ac-config-tips { font-size: 11px; color: #8fb0cf; font-weight: 400; }
.req { color: #ef4444; font-style: normal; }

.ac-section { background: linear-gradient(180deg, rgba(8,22,42,0.5), rgba(6,16,32,0.45)); border: 1px solid rgba(62,170,255,0.12); border-radius: 6px; padding: 16px; transition: all 0.2s; }
.ac-section:hover { border-color: rgba(62,170,255,0.2); }
.ac-section-h { display: flex; justify-content: space-between; align-items: center; font-size: 15px; color: #d4ecff; font-weight: 600; margin-bottom: 14px; padding-left: 12px; border-left: 3px solid #3eaaff; }
.ac-num { display: inline-block; width: 20px; height: 20px; background: linear-gradient(135deg, #3eaaff, #22d3ee); color: #fff; border-radius: 5px; text-align: center; line-height: 20px; font-size: 12px; font-weight: 800; margin-right: 8px; box-shadow: 0 0 10px rgba(62,170,255,0.35); }

.ac-form { display: flex; flex-direction: column; gap: 5px; }
.ac-form label { font-size: 13px; color: #cbd5e1; font-weight: 500; }

/* 规则列表 */
.ac-rules-thead, .ac-rule-row { display: flex; gap: 8px; padding: 10px 6px; align-items: center; }
.ac-rules-thead { background: linear-gradient(90deg, rgba(62,170,255,0.1), rgba(62,170,255,0.03)); border-radius: 5px; margin-bottom: 6px; }
.ac-th { font-size: 12px; color: #d4ecff; text-align: center; font-weight: 600; }
.ac-td { font-size: 12px; }
.ac-rule-row { border-bottom: 0.5px dashed rgba(62,170,255,0.15); transition: background 0.15s; }
.ac-rule-row:hover { background: rgba(62,170,255,0.04); border-radius: 4px; }
.ac-add-rule { text-align: center; padding: 12px; background: linear-gradient(180deg, rgba(62,170,255,0.08), rgba(62,170,255,0.03)); border: 1px dashed #3eaaff; border-radius: 5px; color: #3eaaff; cursor: pointer; font-size: 13px; margin-top: 8px; transition: all 0.2s; }
.ac-add-rule:hover { background: rgba(62,170,255,0.12); box-shadow: 0 0 12px rgba(62,170,255,0.15); }

/* 通知渠道 */
.ac-channels { background: #061224; border-radius: 8px; padding: 12px 14px; }
.ac-ch-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
.ac-ch-card { display: flex; gap: 8px; padding: 10px; background: #111827; border-radius: 6px; }
.ac-ch-ic { font-size: 20px; }
.ac-ch-lb { font-size: 12px; color: #e2e8f0; font-weight: 500; }
.ac-ch-key { color: #8fb0cf; font-size: 11px; font-weight: 400; }
.ac-ch-desc { font-size: 11px; color: #8fb0cf; margin-top: 2px; }

.ac-footer { display: flex; justify-content: flex-end; gap: 8px; padding-top: 6px; }
</style>
