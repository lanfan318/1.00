<template>
<div class="dm-page">
  <el-row :gutter="14">
    <!-- 左侧设备树 -->
    <el-col :span="6">
      <div class="cd dm-tree">
        <div class="cd-t"><span class="ut-ic">▸</span>设备树 · {{ store.selectedUnit.name }}
          <div class="dm-tree-ops">
            <el-button size="small" type="primary" @click="showAddUnit">+ 机组</el-button>
            <el-button size="small" @click="showAddDev">+ 设备</el-button>
          </div>
        </div>
        <div class="dm-tree-body">
          <div v-for="u in store.units" :key="u.id" class="unit-block">
            <div class="tu" @click="selectUnit(u.id)" :class="{ac:store.selectedUnitId===u.id}">
              <span class="tu-dot" :style="{background: u.id===store.selectedUnitId?'#3eaaff':'#8fb0cf', boxShadow:'0 0 6px '+(u.id===store.selectedUnitId?'#3eaaff':'transparent')}"></span>
              <span class="tu-name">{{ u.name }} · {{ u.type }}</span>
              <span class="tu-cap">{{ u.capacity }}MW</span>
              <span class="tu-del" @click.stop="delUnit(u.id)" v-if="store.units.length>1">×</span>
            </div>
            <div v-for="d in store.unitDevices(u.id)" :key="d.id" class="td" :class="{ac:store.selectedDevice===d.id}" @click="selectDev(d.id)">
              <span class="td-name">{{ d.name }}</span>
              <span class="td-h" :style="{color:d.health>=90?'#34d399':d.health>=80?'#fbbf24':'#ef4444'}">{{ d.health.toFixed(0) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-col>

    <!-- 右侧详情 -->
    <el-col :span="18">
      <div class="dm-right">
      <div class="cd dm-info">
        <div class="cd-t"><span class="ut-ic">▸</span>设备信息
          <div class="dm-info-ops">
            <el-button size="small" type="primary" @click="showBatchEdit" :disabled="!cdev">批量修改</el-button>
            <el-button size="small" type="danger" @click="delCurDev" :disabled="!cdev">删除设备</el-button>
          </div>
        </div>
        <div v-if="cdev" class="info-grid">
          <div class="info-i"><span class="info-k">名称</span><span class="info-v">{{ cdev.name }}</span></div>
          <div class="info-i"><span class="info-k">型号</span><span class="info-v">{{ cdev.model }}</span></div>
          <div class="info-i"><span class="info-k">所属机组</span><span class="info-v">{{ unitName }}</span></div>
          <div class="info-i"><span class="info-k">专业</span><span class="info-v">{{ cdev.dept }}</span></div>
          <div class="info-i"><span class="info-k">健康度</span><span class="info-v" :style="{color:cdev.health>=90?'#34d399':cdev.health>=80?'#fbbf24':'#ef4444'}">{{ cdev.health.toFixed(1) }}</span></div>
          <div class="info-i"><span class="info-k">运行状态</span><span class="info-v" :style="{color:cdev.health>=90?'#34d399':cdev.health>=80?'#fbbf24':'#ef4444'}">{{ cdev.health>=90?'运行中':cdev.health>=80?'需关注':'预警处置' }}</span></div>
        </div>
        <div v-else class="empty">请选择左侧设备</div>
      </div>

      <div class="cd dm-meas">
        <div class="cd-t"><span class="ut-ic">▸</span>测点参数<span class="dm-meas-tip">（点击"应用"调整阈值模拟报警）</span></div>
        <el-table v-if="cdev" :data="measureRows" borderless>
          <el-table-column prop="key" label="测点" width="120" />
          <el-table-column prop="value" label="当前值" width="110" />
          <el-table-column prop="unit" label="单位" width="90" />
          <el-table-column label="阈值上限" width="110"><template #default="{row}">{{ row.limit }}</template></el-table-column>
          <el-table-column label="状态" width="90"><template #default="{row}">
            <span class="dm-st" :class="row.ok?'ok':'warn'"><span class="dm-st-dot"></span>{{ row.ok?'正常':'超限' }}</span>
          </template></el-table-column>
          <el-table-column label="操作" min-width="180">
            <template #default="{row}">
              <el-input-number v-model="row.value" :min="0" :step="row.step" size="small" :controls="false" style="width:80px" />
              <el-button link type="primary" size="small" @click="updateParam(row.key, row.value)">应用</el-button>
              <el-button link type="warning" size="small" @click="triggerAlarm(row)">触发报警</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty">请先选择设备</div>
      </div>

      <div v-if="result" class="in"><div class="in-t"><span class="in-ic">✓</span>参数修改预测</div><div class="in-b" v-html="result"></div></div>

      <!-- 设备健康趋势 -->
      <div class="cd dm-trend">
        <div class="cd-t"><span class="ut-ic">▸</span>设备健康趋势</div>
        <div ref="trendChart" class="dm-trend-chart"></div>
      </div>

      <!-- 近期报警记录 -->
      <div class="cd dm-alarms">
        <div class="cd-t"><span class="ut-ic">▸</span>近期报警记录<span class="dm-alarm-count">{{ deviceAlarms.length }} 条</span></div>
        <div v-if="deviceAlarms.length" class="dm-alarm-list">
          <div v-for="(a, i) in deviceAlarms" :key="i" class="dm-alarm-i" :class="'lv'+a.l">
            <span class="dm-alarm-lv">{{ a.l === 1 ? '一级' : a.l === 2 ? '二级' : '预警' }}</span>
            <span class="dm-alarm-desc">{{ a.desc }}</span>
            <span class="dm-alarm-time">{{ a.time }}</span>
            <span class="dm-alarm-st" :class="a.st">{{ a.st === 'resolved' ? '已处理' : a.st === 'confirmed' ? '已确认' : '未处理' }}</span>
          </div>
        </div>
        <div v-else class="empty">该设备暂无报警记录</div>
      </div>
      </div>
    </el-col>
  </el-row>

  <!-- 添加机组 -->
  <el-dialog v-model="unitDlg" title="添加机组" width="400">
    <div class="fg"><label>机组名称</label><el-input v-model="uf.n" placeholder="例：#2 机组"/></div>
    <div class="fg"><label>机组类型</label><el-select v-model="uf.type" style="width:100%"><el-option value="火电机组"/><el-option value="燃气机组"/><el-option value="联合循环"/></el-select></div>
    <div class="fg"><label>额定容量 (MW)</label><el-input-number v-model="uf.cap" :min="100" :max="1000" style="width:100%"/></div>
    <template #footer><el-button @click="unitDlg=false">取消</el-button><el-button type="primary" @click="saveUnit">添加</el-button></template>
  </el-dialog>

  <!-- 添加设备 -->
  <el-dialog v-model="devDlg" title="添加设备" width="400">
    <div class="fg"><label>所属机组</label><el-select v-model="df.unit" style="width:100%"><el-option v-for="u in store.units" :key="u.id" :value="u.id" :label="u.name"/></el-select></div>
    <div class="fg"><label>专业</label><el-select v-model="df.dept" style="width:100%"><el-option value="锅炉"/><el-option value="汽轮机"/><el-option value="电气"/><el-option value="热工"/><el-option value="辅网"/></el-select></div>
    <div class="fg"><label>设备名称</label><el-input v-model="df.n" placeholder="例：E送风机"/></div>
    <div class="fg"><label>设备型号</label><el-input v-model="df.model" placeholder="例：YF-2024E"/></div>
    <template #footer><el-button @click="devDlg=false">取消</el-button><el-button type="primary" @click="saveDev">添加</el-button></template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'

const store = useDataStore()

const cdev = computed(() => store.devices.find(d => d.id === store.selectedDevice))
const unitName = computed(() => store.units.find(u => u.id === store.selectedDevice?.split('-')[0])?.name || '-')
const result = ref('')
const unitDlg = ref(false), devDlg = ref(false)
const trendChart = ref(null)
let tChart = null
const uf = reactive({ n: '', type: '火电机组', cap: 600 })
const df = reactive({ unit: 'U1', dept: '锅炉', n: '', model: '' })

const measureRows = ref([])
const refreshRows = () => {
  if (!cdev.value) { measureRows.value = []; return }
  measureRows.value = Object.entries(cdev.value.params).map(([k, v]) => ({
    key: k, value: v[0], limit: v[1], unit: v[2], ok: v[0] < v[1], step: v[2] === 'rpm' ? 50 : 1
  }))
}
watch(cdev, refreshRows, { immediate: true })

// 近期报警记录
const deviceAlarms = computed(() => {
  if (!cdev.value) return []
  return store.alarms.filter(a => a.device === cdev.value.name).slice(0, 8).map(a => ({
    ...a,
    time: a.time || new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }))
})

// 健康趋势图
const initTrendChart = () => {
  if (tChart) tChart.dispose()
  if (!trendChart.value || !cdev.value) return
  tChart = echarts.init(trendChart.value)
  const h = cdev.value.health
  const data = Array.from({ length: 24 }, (_, i) => Math.max(60, Math.min(100, h + (Math.random() - 0.5) * 12 - i * 0.15)))
  const labels = Array.from({ length: 24 }, (_, i) => `${23 - i}h前`).reverse()
  tChart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 42, right: 14, top: 16, bottom: 22 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(8,20,40,0.92)', borderColor: 'rgba(62,170,255,0.3)', textStyle: { color: '#c8e4ff', fontSize: 11 }, formatter: '{b}: <strong>{value}</strong>' },
    xAxis: { type: 'category', data: labels, axisLabel: { color: '#9fb6cf', fontSize: 10 }, axisLine: { lineStyle: { color: 'rgba(62,170,255,0.12)' } }, axisTick: { show: false } },
    yAxis: { type: 'value', min: 50, max: 100, axisLabel: { color: '#9fb6cf', fontSize: 10, formatter: '{value}%' }, splitLine: { lineStyle: { color: 'rgba(62,170,255,0.08)' } }, axisLine: { show: false } },
    series: [{
      type: 'line', data, smooth: true, symbol: 'circle', symbolSize: 4,
      lineStyle: { width: 2.5, color: '#3eaaff', shadowBlur: 10, shadowColor: '#3eaaff' },
      itemStyle: { color: '#3eaaff', borderColor: '#061224', borderWidth: 1.5 },
      areaStyle: { opacity: 0.5, color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(62,170,255,0.35)' }, { offset: 1, color: 'rgba(62,170,255,0.02)' }]) },
      markLine: {
        silent: true,
        data: [
          { yAxis: 90, lineStyle: { color: '#34d399', type: 'dashed', width: 1 }, label: { show: true, formatter: '健康', fontSize: 9, color: '#34d399' } },
          { yAxis: 80, lineStyle: { color: '#fbbf24', type: 'dashed', width: 1 }, label: { show: true, formatter: '关注', fontSize: 9, color: '#fbbf24' } }
        ]
      }
    }]
  })
}
watch(cdev, () => nextTick(initTrendChart), { immediate: true })
onMounted(() => nextTick(initTrendChart))
onUnmounted(() => tChart?.dispose())

const selectUnit = (id) => { store.selectedUnitId = id }
const selectDev = (id) => { store.selectedDevice = id; result.value = '' }

const updateParam = (k, v) => {
  if (!cdev.value) return
  const oldH = cdev.value.health
  const oldVal = cdev.value.params[k][0]
  store.updateDeviceParam(cdev.value.id, k, v)
  const newH = cdev.value.health
  const ok = v < cdev.value.params[k][1]
  result.value = `<strong>${cdev.value.name}</strong> · ${k} 从 <strong>${oldVal}</strong> 改为 <strong style="color:${ok?'#34d399':'#ef4444'}">${v}${cdev.value.params[k][2]}</strong>。健康度：<strong>${oldH.toFixed(1)} → ${newH.toFixed(1)}</strong>。${ok?'参数在安全范围内。':'⚠️ 超过阈值，建议立即处置。'}`
  refreshRows()
}

const triggerAlarm = (row) => {
  if (!cdev.value) return
  const ok = row.value < row.limit
  const level = !ok ? 1 : 3
  const desc = ok ? `${cdev.value.name} ${row.key} 持续偏高预警` : `${cdev.value.name} ${row.key} 严重超限！立即处置！`
  store.triggerAlarm(cdev.value.id, level, desc, row.key, row.value + row.unit)
  ElMessage.success(`已为 ${cdev.value.name} 触发${level===1?'一级':'智能预警'}报警`)
}

const showAddUnit = () => { Object.assign(uf, { n: '', type: '火电机组', cap: 600 }); unitDlg.value = true }
const showBatchEdit = () => {
  ElMessage.info('请使用下表每行的"应用"按钮单独修改每个测点参数')
}
const saveUnit = () => {
  if (!uf.n.trim()) { ElMessage.warning('请输入机组名称'); return }
  store.addUnit({ name: uf.n, type: uf.type, capacity: uf.cap })
  unitDlg.value = false
  ElMessage.success('已添加机组')
}
const delUnit = async (u) => {
  try {
    await ElMessageBox.confirm(`确认删除 ${u.name}？该机组下所有设备也会被删除。`, '危险操作', { type: 'warning' })
    store.delUnit(u.id)
    ElMessage.success('已删除机组')
  } catch (e) {}
}
const showAddDev = () => { Object.assign(df, { unit: store.selectedUnitId, dept: '锅炉', n: '', model: '' }); devDlg.value = true }
const saveDev = () => {
  if (!df.n.trim()) { ElMessage.warning('请输入设备名称'); return }
  store.addDevice(df)
  devDlg.value = false
  ElMessage.success('已添加设备')
}
const delCurDev = async () => {
  if (!cdev.value) return
  try {
    await ElMessageBox.confirm(`确认删除设备「${cdev.value.name}」？`, '危险操作', { type: 'warning' })
    store.delDevice(cdev.value.id)
    store.selectedDevice = null
    ElMessage.success('已删除设备')
  } catch (e) {}
}

if (!store.selectedDevice && store.devices.length) store.selectedDevice = store.devices[0].id
</script>

<style scoped>
.dm-page { height: 100%; display: flex; flex-direction: column; }
.ut-ic { color: #3eaaff; font-weight: 700; margin-right: 2px; }

/* 设备树 */
.dm-tree { height: calc(100vh - 110px); display: flex; flex-direction: column; }
.dm-tree .cd-t { flex-shrink: 0; }
.dm-tree-ops { margin-left: auto; display: flex; gap: 6px; }
.dm-tree-body { flex: 1; overflow-y: auto; padding-top: 4px; }
.unit-block { margin-bottom: 10px; }
.tu { font-weight: 500; padding: 9px 10px; border-radius: 5px; cursor: pointer; font-size: 13px; display: flex; align-items: center; gap: 8px; position: relative; transition: 0.15s; border: 1px solid transparent; }
.tu:hover { background: rgba(62,170,255,0.06); }
.tu.ac { background: rgba(62,170,255,0.1); color: #3eaaff; border-color: rgba(62,170,255,0.2); }
.tu-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.tu-name { flex: 1; }
.tu-cap { font-size: 11px; color: #8fb0cf; font-family: "SF Mono","Consolas",monospace; }
.tu-del { color: #8fb0cf; padding: 0 6px; border-radius: 3px; }
.tu-del:hover { color: #ef4444; background: rgba(239,68,68,0.1); }
.td { padding: 7px 10px 7px 26px; cursor: pointer; font-size: 12px; color: #8fb0cf; border-radius: 5px; transition: 0.15s; display: flex; align-items: center; justify-content: space-between; margin-bottom: 2px; }
.td:hover { background: rgba(62,170,255,0.06); color: #c8e4ff; }
.td.ac { background: rgba(62,170,255,0.12); color: #3eaaff; }
.td-h { font-family: "SF Mono","Consolas",monospace; font-weight: 600; font-size: 11px; }

/* 设备信息 */
.dm-right { display: flex; flex-direction: column; gap: 14px; height: calc(100vh - 110px); }
.dm-info-ops { margin-left: auto; display: flex; gap: 6px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.info-i { display: flex; justify-content: space-between; align-items: center; background: linear-gradient(180deg, rgba(10,24,42,0.55), rgba(6,16,30,0.6)); border: 1px solid rgba(62,170,255,0.1); border-radius: 6px; padding: 10px 14px; }
.info-k { font-size: 12px; color: #8fb0cf; }
.info-v { font-size: 15px; color: #e2e8f0; font-weight: 600; }

/* 测点 */
.dm-meas-tip { font-size: 11px; color: #8fb0cf; margin-left: 8px; font-weight: 400; }
.dm-st { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; font-weight: 500; }
.dm-st-dot { width: 6px; height: 6px; border-radius: 50%; }
.dm-st.ok { color: #34d399; } .dm-st.ok .dm-st-dot { background: #34d399; box-shadow: 0 0 5px #34d399; }
.dm-st.warn { color: #fbbf24; } .dm-st.warn .dm-st-dot { background: #fbbf24; box-shadow: 0 0 5px #fbbf24; }

.empty { padding: 40px; text-align: center; color: #8fb0cf; font-size: 13px; }

.in { background: linear-gradient(180deg, rgba(10,24,42,0.55), rgba(6,16,30,0.6)); border: 1px solid rgba(62,170,255,0.15); border-left: 3px solid #34d399; border-radius: 6px; padding: 14px; margin-top: 14px; }
.in-t { font-size: 13px; font-weight: 600; color: #34d399; margin-bottom: 8px; display: flex; align-items: center; gap: 6px; }
.in-ic { width: 18px; height: 18px; border-radius: 50%; background: rgba(52,211,153,0.15); border: 1px solid rgba(52,211,153,0.4); display: flex; align-items: center; justify-content: center; font-size: 11px; }
.in-b { font-size: 12px; color: #cbd5e1; line-height: 1.8; }

.fg { margin-bottom: 12px; }
.fg label { display: block; font-size: 12px; color: #9fb6cf; margin-bottom: 4px; }

/* 健康趋势 */
.dm-trend { flex: 1; min-height: 180px; display: flex; flex-direction: column; }
.dm-trend-chart { flex: 1; min-height: 150px; }

/* 近期报警 */
.dm-alarms { flex-shrink: 0; max-height: 220px; display: flex; flex-direction: column; }
.dm-alarm-count { font-size: 11px; color: #9fb6cf; margin-left: 8px; font-weight: 400; }
.dm-alarm-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 6px; padding-top: 8px; }
.dm-alarm-i { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 5px; background: linear-gradient(180deg, rgba(10,24,42,0.5), rgba(6,16,30,0.55)); border: 1px solid rgba(62,170,255,0.08); transition: 0.15s; }
.dm-alarm-i:hover { border-color: rgba(62,170,255,0.2); }
.dm-alarm-lv { font-size: 10px; font-weight: 700; padding: 2px 7px; border-radius: 3px; flex-shrink: 0; }
.dm-alarm-i.lv1 .dm-alarm-lv { background: rgba(239,68,68,0.2); color: #f87171; border: 1px solid rgba(239,68,68,0.3); }
.dm-alarm-i.lv2 .dm-alarm-lv { background: rgba(245,158,11,0.2); color: #fbbf24; border: 1px solid rgba(245,158,11,0.3); }
.dm-alarm-i.lv3 .dm-alarm-lv { background: rgba(34,211,238,0.15); color: #22d3ee; border: 1px solid rgba(34,211,238,0.25); }
.dm-alarm-desc { font-size: 12px; color: #e2e8f0; flex: 1; }
.dm-alarm-time { font-size: 10px; color: #8fb0cf; font-family: "SF Mono","Consolas",monospace; }
.dm-alarm-st { font-size: 10px; font-weight: 600; padding: 1px 7px; border-radius: 3px; flex-shrink: 0; }
.dm-alarm-st.resolved { background: rgba(52,211,153,0.15); color: #34d399; }
.dm-alarm-st.confirmed { background: rgba(245,158,11,0.15); color: #fbbf24; }
.dm-alarm-st.unhandled { background: rgba(239,68,68,0.15); color: #f87171; }
</style>
