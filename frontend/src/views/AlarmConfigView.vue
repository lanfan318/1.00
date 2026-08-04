<template>
<div class="ac-page">
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
      <div class="ac-scan"></div>
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
      <div class="ac-top-glow"></div>
      <div class="ac-config-h">
        <span>{{ form.id ? '编辑报警配置' : '新增报警配置' }}</span>
        <span class="ac-config-tips" v-if="!form.id">带 <em class="req">*</em> 为必填项</span>
      </div>

      <!-- 点项配置 + 报警配置合并到一个 section -->
      <div class="ac-section ac-section-main">
        <div class="ac-section-h">
          <span class="ac-num">1</span>基础配置
        </div>
        <!-- 第 1 行：点项 + 平台点描述 + 报警点描述 -->
        <div class="ac-row ac-row-3">
          <div class="ac-form">
            <label><em class="req">*</em>点项</label>
            <el-select v-model="form.pointKey" placeholder="请选择测点" filterable>
              <el-option v-for="d in store.devices" :key="d.id" :value="d.id" :label="d.name + '（' + uName(d.unit) + ' · ' + d.dept + '） / ' + Object.keys(d.params)[0]" />
            </el-select>
          </div>
          <div class="ac-form">
            <label><em class="req">*</em>平台点描述</label>
            <el-input v-model="form.pointDesc" placeholder="选择测点后自动填充" />
          </div>
          <div class="ac-form">
            <label><em class="req">*</em>报警点描述</label>
            <el-input v-model="form.alarmDesc" placeholder="请输入报警点描述" />
          </div>
        </div>
        <div style="text-align:right;margin-top:2px">
          <el-button type="primary" size="small" @click="pickPoint"><el-icon><Aim /></el-icon>选择测点</el-button>
        </div>
        <!-- 第 2 行：所属机组 + 所属专业 + 设备 -->
        <div class="ac-row ac-row-3" style="margin-top:8px">
          <div class="ac-form">
            <label><em class="req">*</em>所属机组</label>
            <el-select v-model="form.unit" placeholder="请选择所属机组">
              <el-option v-for="u in store.units" :key="u.id" :value="u.id" :label="u.name" />
            </el-select>
          </div>
          <div class="ac-form">
            <label><em class="req">*</em>所属专业</label>
            <el-select v-model="form.dept" placeholder="请选择所属专业">
              <el-option value="锅炉" label="锅炉" />
              <el-option value="汽轮机" label="汽轮机" />
              <el-option value="电气" label="电气" />
              <el-option value="热工" label="热工" />
              <el-option value="辅网" label="辅网" />
            </el-select>
          </div>
          <div class="ac-form">
            <label>设备</label>
            <el-select v-model="form.device" placeholder="请选择设备" filterable>
              <el-option v-for="d in unitDevices" :key="d.id" :value="d.name" :label="d.name + '（' + d.dept + '）'" />
            </el-select>
          </div>
        </div>
        <!-- 第 3 行：报警类型 + 延迟时长 + 触发次数 -->
        <div class="ac-row ac-row-3" style="margin-top:4px">
          <div class="ac-form">
            <label><em class="req">*</em>报警类型</label>
            <el-select v-model="form.type" placeholder="请选择报警类型">
              <el-option value="阈值报警" label="阈值报警" />
              <el-option value="智能预警" label="智能预警" />
              <el-option value="趋势报警" label="趋势报警" />
              <el-option value="开关量变位" label="开关量变位" />
            </el-select>
          </div>
          <div class="ac-form">
            <label><em class="req">*</em>延迟时长(s)</label>
            <el-input-number v-model="form.delay" :min="0" :max="3600" />
          </div>
          <div class="ac-form">
            <label>触发次数</label>
            <el-input-number v-model="form.triggers" :min="1" :max="100" />
          </div>
        </div>
        <!-- 第 4 行：通知方式 + 启用 + 空列 -->
        <div class="ac-row ac-row-3" style="margin-top:4px">
          <div class="ac-form ac-form-wide">
            <label>通知方式</label>
            <el-select v-model="form.channels" multiple placeholder="选择通知方式">
              <el-option v-for="ch in store.channels" :key="ch.key" :value="ch.key" :label="ch.label" />
            </el-select>
          </div>
          <div class="ac-form ac-form-enable">
            <label>启用</label>
            <div><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" /></div>
          </div>
          <div class="ac-form"></div>
        </div>
      </div>

      <!-- 报警规则 -->
      <div class="ac-section ac-section-rule">
        <div class="ac-section-h">
          <span><span class="ac-num">2</span>报警规则</span>
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
        <div class="ac-rules-list">
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
        </div>
        <div class="ac-add-rule" @click="addRule">
          <el-icon><Plus /></el-icon> 添加规则
        </div>
      </div>

      <!-- 通知渠道说明（已删除，与基础配置的通知方式字段重复） -->

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
/* ════════════════════════════════════════
   报警规则配置 — 工业科技风 v2
   ════════════════════════════════════════ */

/* 页面根容器 — 一屏显示 */
.ac-page { display:flex; flex-direction:column; height:100%; overflow:hidden; background-color: #020305; }

/* 页面背景纹理 */
.pg-h { flex-shrink:0; display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; position: relative; }
.pg-h::after { content:''; position:absolute; bottom:-8px; left:0; right:0; height:1px; background:linear-gradient(90deg,transparent,rgba(62,170,255,0.3),transparent); }
.pg-h-right { display: flex; gap: 10px; align-items: center; }
h2 { font-size: 18px; font-weight: 700; color: #e0f0ff; letter-spacing: 1px; text-shadow: 0 0 12px rgba(62,170,255,0.25); }

.ac-layout { display: grid; grid-template-columns: 360px 1fr; gap: 14px; flex:1; min-height:0; }

/* ═══ 左侧规则列表 — 科技风卡片 ═══ */
.ac-left {
  background: linear-gradient(180deg, rgba(5,11,21,0.56), rgba(3,7,14,0.525));
  border: 1px solid rgba(62,170,255,0.18);
  border-radius: 8px;
  padding: 14px; display: flex; flex-direction: column;
  box-shadow: 0 4px 24px rgba(0,10,30,0.35), inset 0 1px 0 rgba(62,170,255,0.06);
  position: relative; overflow: hidden; min-height: 0;
}
/* 左侧装饰角标 */
.ac-left::before, .ac-left::after {
  content:''; position:absolute; width:18px; height:18px;
  border: 2px solid #3eaaff; pointer-events:none;
  box-shadow: 0 0 8px rgba(62,170,255,0.4);
}
.ac-left::before { top:-1px; left:-1px; border-right:none; border-bottom:none; border-radius: 6px 0 0 0; }
.ac-left::after { bottom:-1px; right:-1px; border-left:none; border-top:none; border-radius: 0 0 6px 0; }
/* 扫描线 */
.ac-left .ac-scan {
  position:absolute; top:0; left:0; right:0; height:1.5px;
  background:linear-gradient(90deg,transparent,rgba(62,170,255,0.4),transparent);
  animation: acScan 4s linear infinite; pointer-events:none;
}
@keyframes acScan { 0%{top:0} 100%{top:100%} }

.ac-left-h {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 10px 14px;
  border-bottom: 1px solid rgba(62,170,255,0.15);
  font-size: 14px; color: #d4ecff; font-weight: 600; letter-spacing: 0.5px;
  position: relative;
}
.ac-left-h::before {
  content:''; position:absolute; bottom:-1px; left:0; width:60px; height:2px;
  background:#3eaaff; box-shadow:0 0 8px rgba(62,170,255,0.5);
}
.ac-list { flex: 1; overflow-y: auto; margin-top: 10px; }
.ac-list::-webkit-scrollbar { width: 3px; }
.ac-list::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.25); border-radius: 2px; }

.ac-list-i {
  padding: 11px 12px; border-radius: 6px; cursor: pointer;
  margin-bottom: 7px; border: 1px solid transparent;
  background: linear-gradient(180deg, rgba(5,12,21,0.42), rgba(3,8,15,0.385));
  transition: all 0.25s; position: relative; overflow: hidden;
}
.ac-list-i::before {
  content:''; position:absolute; left:0; top:0; bottom:0; width:2.5px;
  background:transparent; transition:background 0.25s;
}
.ac-list-i:hover {
  border-color: rgba(62,170,255,0.2);
  background: linear-gradient(180deg, rgba(7,15,26,0.455), rgba(4,10,19,0.42));
  transform: translateX(2px);
}
.ac-list-i:hover::before { background: rgba(62,170,255,0.4); }
.ac-list-i.on {
  border-color: rgba(62,170,255,0.35);
  background: linear-gradient(180deg, rgba(20,55,90,0.5), rgba(14,35,60,0.45));
  box-shadow: 0 0 18px rgba(62,170,255,0.1), inset 0 0 20px rgba(62,170,255,0.04);
}
.ac-list-i.on::before { background: #3eaaff; box-shadow: 0 0 8px rgba(62,170,255,0.6); }

.ac-list-row1 { display: flex; justify-content: space-between; align-items: center; font-size: 12.5px; gap: 6px; }
.ac-list-name { color: #e8f0f8; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0; }
.ac-list-row2 { display: flex; justify-content: space-between; align-items: center; font-size: 11px; color: #9fb6cf; margin-top: 6px; }
.ac-list-row3 { margin-top: 5px; display: flex; flex-wrap: wrap; gap: 3px; }
.ac-empty { text-align: center; padding: 36px; color: #6a8caa; font-size: 12px; }

/* ═══ 右侧配置区 — 科技风主体 ═══ */
.ac-right {
  background: linear-gradient(180deg, rgba(10,26,50,0.7), rgba(3,8,16,0.455));
  border: 1px solid rgba(62,170,255,0.2);
  border-radius: 8px; padding: 14px 16px;
  display: flex; flex-direction: column; gap: 14px;
  overflow: auto; position: relative; min-height: 0; min-width: 0;
  box-shadow: 0 6px 32px rgba(0,10,30,0.4), inset 0 1px 0 rgba(62,170,255,0.06);
}
.ac-right :deep(.el-row) {
  display: flex !important;
  flex-wrap: wrap !important;
  margin: 0 -7px !important;
}
/* 通用行布局：3 列等分（点项配置 + 报警配置合并后） */
.ac-row {
  display: grid !important;
  grid-template-columns: repeat(3, minmax(0, 1fr)) !important;
  gap: 10px !important;
  width: 100% !important;
}
.ac-row.ac-row-3 > .ac-form { min-width: 0; }
/* 通知方式那一行特殊：通知方式占 2 列，启用占 1 列 */
.ac-row.ac-form-wide { grid-column: span 2; }
.ac-row .ac-form-wide { grid-column: span 2; min-width: 0; }
.ac-row .ac-form-enable { grid-column: span 1; min-width: 0; }
/* 四角装饰 */
.ac-right::before, .ac-right::after {
  content:''; position:absolute; width:16px; height:16px;
  border: 2px solid #3eaaff; pointer-events:none;
  box-shadow: 0 0 8px rgba(62,170,255,0.5);
}
.ac-right::before { top:-1px; left:-1px; border-right:none; border-bottom:none; }
.ac-right::after { bottom:-1px; right:-1px; border-left:none; border-top:none; }
/* 顶部发光线 */
.ac-right .ac-top-glow {
  position:absolute; top:0; left:50%; transform:translateX(-50%);
  width:200px; height:2px; background:linear-gradient(90deg,transparent,#3eaaff,transparent);
  box-shadow: 0 0 12px rgba(62,170,255,0.5);
}

.ac-config-h {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 18px 14px 22px;
  background: linear-gradient(90deg, rgba(62,170,255,0.2), rgba(62,170,255,0.03));
  border-left: 4px solid #3eaaff;
  font-size: 17px; color: #eaf4ff; font-weight: 700; letter-spacing: 1px;
  margin: -18px -18px 12px;
  box-shadow: -4px 0 20px -3px rgba(62,170,255,0.3);
  position: relative;
}
.ac-config-h::after {
  content:''; position:absolute; bottom:0; left:40px; right:0;
  height:1px; background:linear-gradient(90deg,#3eaaff,transparent);
}
.ac-config-tips { font-size: 11px; color: #7aa0c0; font-weight: 400; }
.req { color: #ef4444; font-style: normal; }

/* ═══ 分组区块 — 科技风 ═══ */
.ac-section {
  background: linear-gradient(180deg, rgba(4,11,21,0.42), rgba(3,8,16,0.35));
  border: 1px solid rgba(62,170,255,0.15);
  border-radius: 6px; padding: 12px 14px; transition: all 0.25s;
  position: relative; overflow: hidden;
}
.ac-section:hover { border-color: rgba(62,170,255,0.28); box-shadow: 0 0 16px rgba(62,170,255,0.06); }

/* 报警规则 section：flex column，标题+表头固定，列表滚动，添加按钮固定底 */
.ac-section-rule {
  flex: 1 !important;
  min-height: 260px !important;
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden !important;
}
.ac-section-rule .ac-section-h,
.ac-rules-thead { flex-shrink: 0; }
.ac-add-rule { flex-shrink: 0; }
/* 区块顶部微光条 */
.ac-section::before {
  content:''; position:absolute; top:0; left:16px; right:16px; height:1px;
  background:linear-gradient(90deg,transparent,rgba(62,170,255,0.2),transparent);
}

.ac-section-h {
  display: flex; justify-content: space-between; align-items: center;
  gap: 16px;
  font-size: 14.5px; color: #d4ecff; font-weight: 600;
  margin-bottom: 14px; padding: 0 12px;
  border-left: 3px solid #3eaaff; border-right: 3px solid #22d3ee;
  text-shadow: 0 0 8px rgba(62,170,255,0.15);
}
.ac-section-h > *:first-child { display: flex; align-items: center; gap: 12px; }
.ac-num {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px;
  background: linear-gradient(135deg, #3eaaff, #22d3ee);
  color: #fff; border-radius: 5px;
  font-size: 14px; font-weight: 800;
  box-shadow: 0 0 12px rgba(62,170,255,0.5), 0 2px 4px rgba(0,0,0,0.25);
}

.ac-form { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.ac-form label { font-size: 11.5px; color: #cbd5e1; font-weight: 500; letter-spacing: 0.3px; }
.ac-form .el-select, .ac-form .el-input, .ac-form .el-input-number, .ac-form .el-radio-group { width: 100% !important; }
.ac-form .el-input-number .el-input { width: 100%; }

/* ═══ 规则表格 — 科技风 ═══ */
.ac-rules-thead, .ac-rule-row {
  display: flex; gap: 8px; padding: 9px 6px; align-items: center;
}
.ac-rules-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}
.ac-rules-list::-webkit-scrollbar { width: 4px; }
.ac-rules-list::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.3); border-radius: 2px; }
.ac-rules-thead {
  background: linear-gradient(90deg, rgba(62,170,255,0.12), rgba(62,170,255,0.03));
  border-radius: 5px; margin-bottom: 6px;
  border: 1px solid rgba(62,170,255,0.08);
}
.ac-th { font-size: 11.5px; color: #a8c8e4; text-align: center; font-weight: 600; letter-spacing: 0.3px; }
.ac-td { font-size: 12px; }
.ac-rule-row {
  border-bottom: 0.5px dashed rgba(62,170,255,0.12); transition: all 0.15s;
  border-radius: 3px;
}
.ac-rule-row:hover { background: rgba(62,170,255,0.05); }
.ac-add-rule {
  text-align: center; padding: 12px;
  background: linear-gradient(180deg, rgba(62,170,255,0.08), rgba(62,170,255,0.02));
  border: 1px dashed rgba(62,170,255,0.35); border-radius: 5px;
  color: #5fb3ff; cursor: pointer; font-size: 13px; margin-top: 8px;
  transition: all 0.25s; position: relative; overflow: hidden;
}
.ac-add-rule:hover {
  background: rgba(62,170,255,0.12);
  border-color: #3eaaff;
  box-shadow: 0 0 16px rgba(62,170,255,0.15), inset 0 0 12px rgba(62,170,255,0.05);
  color: #a8d4ff;
}
.ac-add-rule::before {
  content:''; position:absolute; top:0; left:-100%; width:60%; height:100%;
  background:linear-gradient(90deg,transparent,rgba(62,170,255,0.1),transparent);
  transition:left 0.5s;
}
.ac-add-rule:hover::before { left:150%; }

/* ═══ 通知渠道 — 科技风卡片 ═══ */
.ac-channels {
  background: linear-gradient(180deg, rgba(3,8,15,0.56), rgba(2,5,11,0.525));
  border: 1px solid rgba(62,170,255,0.15);
  border-radius: 8px; padding: 14px 16px;
  position: relative;
}
.ac-channels::before {
  content:''; position:absolute; top:0; left:20px; right:20px; height:1.5px;
  background:linear-gradient(90deg,transparent,rgba(62,170,255,0.3),transparent);
}
.ac-ch-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
.ac-ch-card {
  display: flex; gap: 10px; padding: 12px;
  background: linear-gradient(180deg, rgba(7,12,21,0.56), rgba(4,8,16,0.525));
  border: 1px solid rgba(62,170,255,0.12);
  border-radius: 6px; transition: all 0.2s;
  position: relative; overflow: hidden;
}
.ac-ch-card::before {
  content:''; position:absolute; left:0; top:0; bottom:0; width:3px;
  background:linear-gradient(180deg,#3eaaff,rgba(62,170,255,0.2));
}
.ac-ch-card:hover { border-color: rgba(62,170,255,0.3); transform: translateY(-1px); box-shadow: 0 4px 16px rgba(0,0,0,0.2); }
.ac-ch-ic { font-size: 22px; line-height: 1; width: 28px; text-align: center; filter: drop-shadow(0 0 4px currentColor); }
.ac-ch-lb { font-size: 12px; color: #e2e8f0; font-weight: 600; }
.ac-ch-key { color: #6a8caa; font-size: 10.5px; font-weight: 400; margin-left: 4px; }
.ac-ch-desc { font-size: 11px; color: #8fb0cf; margin-top: 3px; line-height: 1.4; }

.ac-footer {
  display: flex; justify-content: flex-end; gap: 10px; padding-top: 10px;
  margin-top: 4px;
  position: relative;
}
.ac-footer::before {
  content:''; position:absolute; top:0; left:0; right:0; height:1px;
  background:linear-gradient(90deg,transparent,rgba(62,170,255,0.15),transparent);
}

/* Element Plus 深度覆盖 — 工业风 */
.ac-right :deep(.el-input__wrapper),
.ac-right :deep(.el-select__wrapper) {
  background: rgba(3,8,15,0.42) !important;
  border: 1px solid rgba(62,170,255,0.15) !important;
  box-shadow: none !important;
  transition: all 0.2s;
}
.ac-right :deep(.el-input__wrapper:hover),
.ac-right :deep(.el-select__wrapper:hover) {
  border-color: rgba(62,170,255,0.35) !important;
}
.ac-right :deep(.el-input__wrapper.is-focus),
.ac-right :deep(.el-select__wrapper.is-focused) {
  border-color: #3eaaff !important;
  box-shadow: 0 0 12px rgba(62,170,255,0.15) !important;
}
.ac-right :deep(.el-input__inner) { color: #e2e8f0 !important; }
.ac-right :deep(.el-input__inner::placeholder) { color: #5a7894 !important; }
.ac-right :deep(.el-textarea__inner) {
  background: rgba(3,8,15,0.42) !important;
  color: #e2e8f0 !important;
  border: 1px solid rgba(62,170,255,0.15) !important;
  box-shadow: none !important;
}
.ac-right :deep(.el-button--primary) {
  background: linear-gradient(135deg, #2563eb, #1d4ed8) !important;
  border: none !important;
  box-shadow: 0 2px 12px rgba(37,99,235,0.3) !important;
}
.ac-right :deep(.el-button--primary:hover) {
  box-shadow: 0 4px 20px rgba(37,99,235,0.45) !important;
  transform: translateY(-1px);
}
.ac-right :deep(.el-radio-button__inner) {
  background: rgba(3,8,15,0.42) !important;
  border-color: rgba(62,170,255,0.2) !important;
  color: #a0bed8 !important;
}
.ac-right :deep(.el-radio-button__original:checked + .el-radio-button__inner) {
  background: rgba(62,170,255,0.2) !important;
  border-color: #3eaaff !important;
  color: #e0f0ff !important;
  box-shadow: 0 0 8px rgba(62,170,255,0.2) !important;
}
</style>

<!-- 非 scoped 覆盖块：纯黑 + CRT 虚线方格 -->
<style>
/* 左侧规则列表 + 右侧配置区 + 分组区块 纯黑+CRT网格 */
.ac-page .ac-left,
.ac-page .ac-right,
.ac-page .ac-section {
  background-color: #000000 !important;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px) !important;
  border-color: rgba(90,166,196,0.12) !important;
  box-shadow: none !important;
}
/* 标题栏/头部 不透明深蓝挡网格 */
.ac-page .ac-left-h,
.ac-page .ac-config-h,
.ac-page .ac-section-h {
  background-color: #06121c !important;
  background-image: none !important;
}
/* 规则表头 */
.ac-page .ac-rules-thead {
  background-color: #06121c !important;
  background-image: none !important;
}
</style>
