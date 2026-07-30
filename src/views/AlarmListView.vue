<template>
<div class="alv-wrap">
  <!-- 顶部统计栏 — 紧凑四列 -->
  <div class="alv-stats-row">
    <div class="als-card" v-for="b in bars" :key="b.lv" :class="{ac:filter===b.lv, 'ac-danger': b.lv==='1', 'ac-warn': b.lv==='2', 'ac-info': b.lv==='3'}" @click="filter=b.lv">
      <div class="als-top">
        <span class="als-dot" :style="{background:b.color,boxShadow:`0 0 8px ${b.color}`}"></span>
        <span class="als-lbl">{{ b.label }}</span>
      </div>
      <div class="als-val" :style="{color:b.color}">{{ b.cnt }}</div>
      <div class="als-bar-track"><span class="als-bar-fill" :style="{width:barPct(b.cnt)+'%',background:b.color}"></span></div>
    </div>
  </div>

  <!-- 主表格区 -->
  <div class="cd alv-table-panel">
    <div class="cd-h">
      <span class="cd-t">报警列表 · {{ store.selectedUnit.name }}（{{ filtered.length }} 条）</span>
      <span v-if="deviceFilter" class="cd-alarm-filter-tag">
        已筛选: {{ store.deviceById(parseInt(deviceFilter))?.name }}
        <span class="cd-clear-x" @click="deviceFilter=''">&times;</span>
      </span>
      <div class="alv-actions">
        <el-button size="small" @click="batchConfirm">批量确认</el-button>
        <el-button size="small" @click="batchResolve">批量完成</el-button>
        <el-button size="small" type="primary" @click="showSimulate">模拟新报警</el-button>
      </div>
    </div>
    <div class="alv-tb-wrap">
    <el-table :data="paged" borderless :row-class-name="rowCls" class="alv-tb" @row-click="r => cur = r">
      <el-table-column width="72"><template #default="{row}"><el-tag :type="row.l===1?'danger':row.l===2?'warning':'info'" size="small">{{ row.l===1?'一级':row.l===2?'二级':'智能' }}</el-tag></template></el-table-column>
      <el-table-column prop="time" label="时间" width="145">
        <template #default="{row}">{{ fmt(row.time) }}</template>
      </el-table-column>
      <el-table-column prop="unit" label="机组" width="70"><template #default="{row}">{{ unitName(row.unit) }}</template></el-table-column>
      <el-table-column prop="dept" label="专业" width="65" />
      <el-table-column prop="device" label="设备" width="115" />
      <el-table-column prop="desc" label="描述" />
      <el-table-column prop="point" label="测点" width="130" />
      <el-table-column label="报警值" width="85"><template #default="{row}"><span class="alv-val-cell" :class="row.l===1?'vd':row.l===2?'vw':'vi'">{{row.val}}</span></template></el-table-column>
      <el-table-column label="状态" width="78"><template #default="{row}"><el-tag :type="row.st==='unhandled'?'danger':row.st==='confirmed'?'warning':'success'" size="small">{{ stTxt(row.st) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{row}">
          <div class="alv-op-btns">
            <button v-if="row.st==='unhandled'" class="aob aob-pri" @click.stop="act(row,'confirmed')">确认</button>
            <button v-if="row.st==='unhandled'" class="aob" @click.stop="act(row,'suppressed')">抑制</button>
            <button v-if="row.st==='confirmed'" class="aob aob-suc" @click.stop="act(row,'resolved')">完成</button>
            <button v-if="row.st==='resolved'" class="aob" @click.stop="act(row,'unhandled')">重开</button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    </div>
    <div class="alv-pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="filtered.length"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        small
      />
    </div>
  </div>

  <!-- 操作指导面板 — 仅选中时显示 -->
  <transition name="fade-slide">
    <div v-if="cur" class="guide">
      <div class="gt">操作指导 · {{cur.desc}}</div>
      <div class="guide-grid">
        <div class="gs">
          <div class="gsh"><i class="gsh-ic"></i>报警详情</div>
          <div class="gs-body">
            <p><span class="gs-k">测点</span><span class="gs-v point">{{cur.point}}</span><span class="gs-k">当前值</span><span class="gs-v val">{{cur.val}}</span><span class="gs-k">类型</span><span class="gs-v">{{cur.type}}</span></p>
            <p><span class="gs-k">机组</span><span class="gs-v">{{ unitName(cur.unit) }}</span><span class="gs-k">专业</span><span class="gs-v dep">{{cur.dept}}</span><span class="gs-k">设备</span><span class="gs-v dev">{{cur.device}}</span></p>
            <p><span class="gs-k">状态</span><span class="gs-v st" :class="cur.st">{{ stTxt(cur.st) }}</span><span class="gs-k">报警时间</span><span class="gs-v tm">{{ fmt(cur.time) }}</span></p>
          </div>
        </div>
        <div class="gs gs-advice">
          <div class="gsh"><i class="gsh-ic gsh-ic-w"></i>处置建议</div>
          <div class="gs-body">
            <p v-for="(line,i) in adviceLines(cur)" :key="i" class="advice-line"><span class="advice-num">{{ i+1 }}</span>{{ line }}</p>
          </div>
        </div>
      </div>
    </div>
  </transition>

  <!-- 模拟对话框 -->
  <el-dialog v-model="simulateDlg" title="模拟新报警" width="400" class="sim-dlg">
    <div class="sim-body">选择一个设备触发模拟报警：</div>
    <el-select v-model="simDev" style="width:100%" filterable>
      <el-option v-for="d in store.unitDevices(store.selectedUnitId).filter(d => d.health < 90)" :key="d.id" :value="d.id" :label="`${d.name}（${d.dept} · 健康度 ${d.health.toFixed(1)}）`" />
    </el-select>
    <template #footer>
      <el-button @click="simulateDlg=false">取消</el-button>
      <el-button type="primary" @click="doSimulate">触发报警</el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'
import { useRoute } from 'vue-router'

const store = useDataStore()
const filter = ref('all')
const route = useRoute()
const deviceFilter = ref('')
const cur = ref(null)
onMounted(() => { if (route.query.deviceId) deviceFilter.value = String(route.query.deviceId) })

const filtered = computed(() => {
  const uid = store.selectedUnitId
  let arr = store.alarms.filter(a => a.unit === uid)
  if (filter.value !== 'all') arr = arr.filter(a => a.l === parseInt(filter.value))
  if (deviceFilter.value) {
    const dn = store.deviceById(parseInt(deviceFilter.value))?.name
    if (dn) arr = arr.filter(a => a.device === dn)
  }
  return arr
})

// 分页
const page = ref(1)
const pageSize = ref(10)
const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})
watch([filter, deviceFilter, () => store.selectedUnitId], () => { page.value = 1 })
watch(() => filtered.value.length, (n) => {
  const maxPage = Math.max(1, Math.ceil(n / pageSize.value))
  if (page.value > maxPage) page.value = maxPage
})

const totalAlarms = computed(() => store.unitAlarms(store.selectedUnitId).length)

const bars = computed(() => {
  const arr = store.unitAlarms(store.selectedUnitId)
  return [
    { lv: 'all', label: '全部报警', cnt: arr.length, color: '#3eaaff' },
    { lv: '1', label: '一级报警', cnt: arr.filter(a => a.l === 1).length, color: '#f87171' },
    { lv: '2', label: '二级报警', cnt: arr.filter(a => a.l === 2).length, color: '#fbbf24' },
    { lv: '3', label: '智能预警', cnt: arr.filter(a => a.l === 3).length, color: '#22d3ee' }
  ]
})

const barPct = (cnt) => {
  const t = totalAlarms.value || 1
  return Math.min(100, Math.round(cnt / t * 100))
}

const fmt = (t) => { const d = new Date(t); return d.toLocaleString('zh-CN', { hour12: false }) }
const filteredDeviceName = computed(() => deviceFilter.value ? store.deviceById(parseInt(deviceFilter.value))?.name || '' : '')
const rowCls = ({row}) => row.device === filteredDeviceName.value ? 'alarm-row-highlight' : ''
const stTxt = (st) => st === 'unhandled' ? '未处理' : st === 'confirmed' ? '已确认' : st === 'suppressed' ? '已抑制' : '已处理'
const unitName = (id) => store.units.find(u => u.id === id)?.name || id

const act = (row, st) => {
  store.setAlarmStatus(row.id, st)
  cur.value = { ...row, st }
  ElMessage.success(st === 'resolved' ? '已处理' : st === 'confirmed' ? '已确认' : st === 'suppressed' ? '已抑制（10分钟内同设备同类报警不再触发）' : '已重新打开')
}

const batchConfirm = async () => {
  const cnt = store.alarms.filter(a => a.unit === store.selectedUnitId && a.st === 'unhandled').length
  if (cnt === 0) { ElMessage.info('当前无未处理的报警'); return }
  try {
    await ElMessageBox.confirm(`确认批量确认 ${cnt} 条报警？已确认的报警将进入待处理阶段。`, '批量确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
    store.alarms.filter(a => a.unit === store.selectedUnitId && a.st === 'unhandled').forEach(a => store.setAlarmStatus(a.id, 'confirmed'))
    ElMessage.success(`已批量确认 ${cnt} 条报警`)
  } catch (e) {}
}
const batchResolve = async () => {
  const cnt = store.alarms.filter(a => a.unit === store.selectedUnitId && (a.st === 'unhandled' || a.st === 'confirmed')).length
  if (cnt === 0) { ElMessage.info('当前无待处理的报警'); return }
  try {
    await ElMessageBox.confirm(`确认批量完成 ${cnt} 条报警？完成后将标记为已处理。`, '批量完成', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
    store.alarms.filter(a => a.unit === store.selectedUnitId && (a.st === 'unhandled' || a.st === 'confirmed')).forEach(a => store.setAlarmStatus(a.id, 'resolved'))
    ElMessage.success(`已批量完成 ${cnt} 条报警`)
  } catch (e) {}
}

const simulateDlg = ref(false)
const simDev = ref('')
const showSimulate = () => {
  const devs = store.unitDevices(store.selectedUnitId).filter(d => d.health < 90)
  if (devs.length === 0) { ElMessage.warning('当前机组没有健康度 < 90 的设备'); return }
  simDev.value = devs[0].id
  simulateDlg.value = true
}
const doSimulate = () => {
  if (!simDev.value) return
  const d = store.devices.find(x => x.id === simDev.value)
  if (!d) return
  const badParam = Object.entries(d.params || {}).find(([k, v]) => v[0] >= v[1])
  const desc = badParam ? `${d.name} ${badParam[0]} 持续偏高预警` : `${d.name} 模拟故障报警`
  const point = badParam ? d.name + badParam[0] : d.name
  const val = badParam ? `${badParam[1][0]}${badParam[1][2]}` : '模拟值'
  const level = d.health < 70 ? 1 : 2
  store.triggerAlarm(simDev.value, level, desc, point, val)
  simulateDlg.value = false
  ElMessage.success('已触发 ' + d.name + ' 报警(' + (level === 1 ? '一级' : '二级') + ')')
}

const adviceLines = (r) => {
  const base = r.l === 1
    ? ['立即通知值班长和检修班组','启动应急预案','降低相关设备负荷至安全范围','准备备品备件','若参数持续恶化，执行紧急停机程序']
    : r.l === 2
      ? ['现场巡检确认报警真实性','分析异常原因并记录','通知相关专业负责人关注','若升级为一级报警，按一级响应程序处置']
      : ['关注报警趋势变化','记录异常数据，纳入交接班报告','分析报警产生原因，判断是否为误报','若频繁出现，建议安排专项检查']
  return base
}
</script>

<style scoped>
/* ================================================================
   报警列表页 — 专业工业风格 v2
   统一: 深蓝底 / cyan边框 / 玻璃态卡片 / 无空区
   ================================================================ */
.alv-wrap { display:flex; flex-direction:column; gap:12px; height:100%; overflow:hidden; }

/* ========== 顶部统计栏 — 四列等宽卡片 ========== */
.alv-stats-row {
  flex-shrink:0;
  display:grid;
  grid-template-columns: repeat(4, 1fr);
  gap:12px;
}
.als-card {
  position:relative;
  display:flex; flex-direction:column; gap:6px;
  padding:14px 16px;
  background:linear-gradient(180deg, rgba(4,10,20,0.385), rgba(3,8,16,0.42));
  border:1px solid rgba(62,170,255,0.12);
  border-radius:5px;
  cursor:pointer;
  transition:all 0.25s cubic-bezier(0.4,0,0.2,1);
  overflow:hidden;
}
.als-card::before {
  content:''; position:absolute; top:0; left:0; right:0; height:2px;
  opacity:0.7; transition:opacity 0.25s;
}
.als-card.ac::before { background:var(--card-ac, #3eaaff); }
.als-card.ac-danger { --card-ac:#f87171; border-color:rgba(248,113,113,0.25); background:linear-gradient(180deg, rgba(248,80,80,0.06), rgba(3,8,16,0.42)); }
.als-card.ac-warn { --card-ac:#fbbf24; border-color:rgba(251,191,36,0.25); background:linear-gradient(180deg, rgba(251,180,36,0.05), rgba(3,8,16,0.42)); }
.als-card.ac-info { --card-ac:#22d3ee; border-color:rgba(34,211,238,0.25); background:linear-gradient(180deg, rgba(34,200,238,0.04), rgba(3,8,16,0.42)); }
.als-card:hover { transform:translateY(-2px); box-shadow:0 6px 20px rgba(0,10,30,0.4); }
.als-card:not(.ac):hover { border-color:rgba(62,170,255,0.3); }
.als-card:not(.ac)::before { background:#3eaaff; }

.als-top { display:flex; align-items:center; gap:8px; }
.als-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.als-lbl { font-size:11px; color:#8fb0cf; letter-spacing:0.5px; font-weight:500; }
.als-val { font-size:30px; font-weight:700; font-family:"SF Mono","Consolas",monospace; line-height:1; text-shadow:0 2px 8px rgba(0,0,0,0.3); }
.als-bar-track { height:4px; background:rgba(62,170,255,0.08); border-radius:2px; overflow:hidden; margin-top:2px; }
.als-bar-fill { display:block; height:100%; border-radius:2px; transition:width 0.6s ease; box-shadow:0 0 6px currentColor; }

/* ========== 表格面板 — 深色工业风覆盖 ========== */
.alv-table-panel { flex:1; min-height:0; display:flex; flex-direction:column; padding:16px !important; }
.alv-tb-wrap { flex:1; min-height:0; overflow:auto; }
.alv-tb { width:100%; }

/* 覆盖 Element Plus 表格默认白底 → 深色工业风 */
:deep(.alv-tb .el-table) {
  background-color: transparent;
  color: #d4e4f0;
  --el-table-border-color: rgba(62,170,255,0.12);
  --el-table-header-bg-color: rgba(208,224,244,0.94);
  --el-table-row-hover-bg-color: rgba(62,170,255,0.1);
  --el-table-current-row-bg-color: rgba(62,170,255,0.15);
  --el-fill-color-blank: transparent;
}
:deep(.alv-tb .el-table th.el-table__cell) {
  background: linear-gradient(180deg, rgba(226,238,250,0.96), rgba(208,224,244,0.94)) !important;
  color: #16498c;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
  border-bottom: 2px solid #16498c !important;
  text-shadow: 0 1px 0 rgba(255,255,255,0.6);
  position: relative;
}
:deep(.alv-tb .el-table th.el-table__cell::before) {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; height: 2px;
  background: #2b6cb0;
}
:deep(.alv-tb .el-table td.el-table__cell) {
  background: linear-gradient(90deg, rgba(7,14,25,0.385), rgba(5,11,21,0.315)) !important;
  border-bottom: 1px solid rgba(62,170,255,0.08) !important;
  color: #dce8f2;
  font-size: 13px;
  transition: background 0.2s;
}
:deep(.alv-tb .el-table tr:hover > td.el-table__cell) {
  background: linear-gradient(90deg, rgba(25,60,110,0.55), rgba(18,45,85,0.45)) !important;
}
:deep(.alv-tb .el-table--border .el-table__cell) {
  border-right: 1px solid rgba(62,170,255,0.06);
}
.alv-op-btns { display:flex; gap:4px; flex-wrap:wrap; }

/* ========== 分页器 — 深色工业风 ========== */
.alv-pager { flex-shrink:0; display:flex; justify-content:flex-end; padding:12px 4px 2px; }
:deep(.alv-pager .el-pagination) {
  --el-pagination-bg-color: rgba(10,25,50,0.6);
  --el-pagination-text-color: #a8c4dc;
  --el-pagination-button-color: #a8c4dc;
  --el-pagination-button-disabled-bg-color: rgba(8,18,32,0.4);
  --el-pagination-hover-color: #3eaaff;
}
:deep(.alv-pager .el-pager li) {
  background: rgba(10,25,50,0.6);
  border: 1px solid rgba(62,170,255,0.15);
  color: #a8c4dc;
}
:deep(.alv-pager .el-pager li.is-active) {
  background: rgba(40,110,190,0.35);
  border-color: #3eaaff;
  color: #e0f0ff;
  box-shadow: 0 0 8px rgba(62,170,255,0.3);
}
:deep(.alv-pager .btn-prev), :deep(.alv-pager .btn-next) {
  background: rgba(10,25,50,0.6);
  border: 1px solid rgba(62,170,255,0.15);
  color: #a8c4dc;
}
:deep(.alv-pager .el-pagination__total),
:deep(.alv-pager .el-pagination__jump) { color: #8fb0cf; }
:deep(.alv-pager .el-input__wrapper) {
  background: rgba(10,25,50,0.6);
  box-shadow: 0 0 0 1px rgba(62,170,255,0.15) inset;
}
:deep(.alv-pager .el-input__inner) { color: #c8e0f4; }

/* 自定义操作按钮 */
.aob {
  padding:3px 10px; font-size:11px; font-weight:600;
  background:rgba(10,25,50,0.6);
  border:1px solid rgba(62,170,255,0.22);
  border-radius:3px; color:#b8d4f0;
  cursor:pointer; transition:all 0.15s;
}
.aob:hover { border-color:rgba(62,170,255,0.5); color:#e0f0ff; background:rgba(20,55,100,0.45); }
.aob-pri { border-color:rgba(62,170,255,0.5); color:#c0e4ff; background:rgba(40,110,190,0.2); }
.aob-pri:hover { background:rgba(50,130,230,0.3); box-shadow:0 0 10px rgba(62,170,255,0.25); }
.aob-suc { border-color:rgba(52,211,153,0.4); color:#8ef0c8; background:rgba(34,170,130,0.15); }
.aob-suc:hover { background:rgba(34,200,155,0.22); }

/* 报警值高亮 */
.alv-val-cell { font-weight:700; font-family:"SF Mono","Consolas",monospace; font-size:13px; }
.alv-val-cell.vd { color:#ff9090; text-shadow:0 0 8px rgba(248,113,113,0.4); }
.alv-val-cell.vw { color:#ffd666; text-shadow:0 0 8px rgba(251,191,36,0.35); }
.alv-val-cell.vi { color:#5ee8f8; }

/* 行高亮 — 深色底上的青色渐变高亮 */
:deep(.alarm-row-highlight) > td {
  background: linear-gradient(90deg, rgba(62,170,255,0.22), rgba(40,110,200,0.12)) !important;
}

/* 表格内 tag 提亮 */
:deep(.alv-tb .el-tag) { font-weight:600; font-size:11px; border-radius:3px; }
:deep(.alv-tb .el-tag--danger) { background:rgba(248,80,80,0.18); border-color:rgba(248,113,113,0.4); color:#ffaaaa; }
:deep(.alv-tb .el-tag--warning) { background:rgba(251,180,36,0.15); border-color:rgba(251,191,36,0.35); color:#ffd666; }
:deep(.alv-tb .el-tag--info) { background:rgba(34,180,238,0.14); border-color:rgba(34,211,238,0.3); color:#7ee8f8; }
:deep(.alv-tb .el-tag--success) { background:rgba(34,180,140,0.14); border-color:rgba(52,211,153,0.3); color:#7ee8c4; }

/* ========== 操作指导面板 ========== */
.guide {
  flex-shrink:0;
  max-height:200px;
  overflow:auto;
  background:linear-gradient(180deg, rgba(10,26,50,0.55), rgba(3,8,16,0.42));
  border:1px solid rgba(62,170,255,0.12);
  border-radius:6px;
  padding:16px;
  animation:guideIn 0.3s ease;
}
@keyframes guideIn { from{opacity:0;transform:translateY(-8px);} to{opacity:1;transform:none;} }

.gt {
  font-size:14px; font-weight:600; color:#fbbf24;
  margin-bottom:14px; padding-bottom:8px;
  border-bottom:1px dashed rgba(62,170,255,0.15);
  display:flex; align-items:center; gap:8px;
}
.gt::before {
  content:'▶'; font-size:11px; color:#fbbf24;
  filter:drop-shadow(0 0 4px rgba(251,191,36,0.5));
}

.guide-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
.gs {
  background:linear-gradient(180deg, rgba(4,9,16,0.49), rgba(3,7,13,0.525));
  border:1px solid rgba(62,170,255,0.08);
  border-radius:5px;
  padding:14px;
}
.gsh {
  font-size:12px; font-weight:600; color:#3eaaff;
  margin-bottom:10px; display:flex; align-items:center; gap:6px;
}
.gsh-ic {
  width:0; height:0;
  border-left:7px solid #3eaaff;
  border-top:4px solid transparent;
  border-bottom:4px solid transparent;
  filter:drop-shadow(0 0 3px rgba(62,170,255,0.5));
}
.gsh-ic-w { border-left-color:#fbbf24; filter:drop-shadow(0 0 3px rgba(251,191,36,0.5)); }

.gs-body p { font-size:12px; color:#8fb0cf; line-height:1.8; margin-bottom:4px; display:flex; flex-wrap:wrap; gap:4px 12px; align-items:center; }
.gs-k { font-size:11px; color:#8fb0cf; min-width:42px; }
.gs-v { color:#e2e8f0; font-weight:500; }
.gs-v.point { color:#fbbf24; font-family:"SF Mono","Consolas",monospace; }
.gs-v.val { color:#f87171; font-weight:700; font-size:13px; }
.gs-v.dev { color:#a8d4ff; }
.gs-v.dep { color:#c4b5fd; }
.gs-v.st { padding:1px 8px; border-radius:3px; font-size:11px; font-weight:500; }
.gs-v.st.unhandled { background:rgba(248,113,113,0.12); color:#f87171; border:1px solid rgba(248,113,113,0.25); }
.gs-v.st.confirmed { background:rgba(251,191,36,0.12); color:#fbbf24; border:1px solid rgba(251,191,36,0.25); }
.gs-v.st.resolved, .gs-v.st.suppressed { background:rgba(52,211,153,0.12); color:#34d399; border:1px solid rgba(52,211,153,0.25); }
.gs-v.tm { font-family:"SF Mono","Consolas",monospace; color:#5fb3ff; }

/* 处置建议列表 */
.gs-advice .gs-body { display:flex; flex-direction:column; gap:6px; }
.advice-line {
  display:flex; gap:8px; align-items:flex-start;
  font-size:12px; color:#a8c4dc; line-height:1.6;
  padding:6px 10px;
  background:rgba(4,10,20,0.28);
  border-radius:4px;
  border-left:2px solid rgba(62,170,255,0.2);
}
.advice-num {
  width:18px; height:18px; border-radius:50%;
  background:rgba(62,170,255,0.15); color:#5fb3ff;
  font-size:11px; font-weight:700;
  display:inline-flex; align-items:center; justify-content:center;
  flex-shrink:0; margin-top:1px;
}

/* 筛选tag */
.cd-alarm-filter-tag { display:inline-flex; align-items:center; gap:6px; padding:3px 10px; background:rgba(62,170,255,0.12); border:1px solid rgba(62,170,255,0.25); border-radius:3px; font-size:11px; color:#5fb3ff; }
.cd-clear-x { cursor:pointer; color:#f87171; font-weight:700; padding:0 6px; border-radius:2px; transition:all 0.15s; }
.cd-clear-x:hover { background:rgba(248,113,113,0.2); }

.alv-actions { display:flex; gap:6px; margin-left:auto; }

/* 过渡动画 */
.fade-slide-enter-active { transition:all 0.3s ease; }
.fade-slide-leave-active { transition:all 0.2s ease; }
.fade-slide-enter-from { opacity:0; transform:translateY(-10px); }
.fade-slide-leave-to { opacity:0; transform:translateY(-10px); }

/* 响应式 */
@media(max-width:1000px){
  .alv-stats-row { grid-template-columns:repeat(2, 1fr); }
  .guide-grid { grid-template-columns:1fr; }
}
</style>
