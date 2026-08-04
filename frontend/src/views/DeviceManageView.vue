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
        <div class="cd-t"><span class="ut-ic">▸</span>测点参数<span class="dm-meas-tip">（点击"修改"调整当前值/阈值，"触发"模拟报警）</span></div>
        <div v-if="cdev" class="dm-meas-wrap">
          <table class="meas-tb">
            <thead>
              <tr>
                <th>测点</th><th>当前值</th><th>单位</th><th>阈值上限</th><th>状态</th><th style="width:170px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, ri) in measureRows" :key="ri" :class="{warn: !row.ok}">
                <td class="meas-k">{{ row.key }}</td>
                <td class="meas-v" :style="{color: row.ok ? '#3eaaff' : '#f87171'}">{{ row.value }}<span class="meas-u-sm">{{ row.unit }}</span></td>
                <td class="meas-unit">{{ row.unit }}</td>
                <td class="meas-limit">{{ row.limit }}</td>
                <td><span class="dm-st" :class="row.ok?'ok':'warn'"><span class="dm-st-dot"></span>{{ row.ok?'正常':'超限' }}</span></td>
                <td class="meas-op">
                  <button class="meas-btn edit" @click="openParamEdit(row)">修改</button>
                  <button class="meas-btn alarm" @click="triggerAlarm(row)">触发</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
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
        <div class="cd-t"><span class="ut-ic">▸</span>近期报警记录<span class="dm-alarm-count">共 {{ deviceAlarms.length }} 条</span></div>
        <div v-if="pagedAlarms.length" class="dm-alarm-list">
          <div v-for="(a, i) in pagedAlarms" :key="i" class="dm-alarm-i" :class="'lv'+a.l">
            <span class="dm-alarm-lv">{{ a.l === 1 ? '一级' : a.l === 2 ? '二级' : '预警' }}</span>
            <span class="dm-alarm-desc">{{ a.desc }}</span>
            <span class="dm-alarm-time">{{ a.time }}</span>
            <span class="dm-alarm-st" :class="a.st">{{ a.st === 'resolved' ? '已处理' : a.st === 'confirmed' ? '已确认' : '未处理' }}</span>
          </div>
        </div>
        <div v-else class="empty">该设备暂无报警记录</div>
        <div v-if="deviceAlarms.length > pageSize" class="dm-pager">
          <el-pagination
            v-model:current-page="alarmPage"
            :page-size="pageSize"
            :total="deviceAlarms.length"
            layout="prev, pager, next, jumper, total"
            small
            background
          />
        </div>
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
<!-- 测点参数编辑 -->
  <el-dialog v-model="paramDlg" :title="paramEditTarget ? `修改测点参数 · ${paramEditTarget.key} (${cdev?.name})` : '修改测点参数'" width="560">
    <div v-if="paramEditTarget" class="param-edit-form">
      <div class="pe-row">
        <label>测点名称</label>
        <div class="pe-val">{{ paramEditTarget.key }}</div>
      </div>
      <div class="pe-row">
        <label>当前值</label>
        <el-input-number v-model="paramEditTarget.value" :min="0" :step="paramEditTarget.step" :precision="2" style="width:100%"/>
        <span class="pe-unit">{{ paramEditTarget.unit }}</span>
      </div>
      <div class="pe-row">
        <label>阈值上限</label>
        <el-input-number v-model="paramEditTarget.limit" :min="0" :step="paramEditTarget.step" :precision="2" style="width:100%"/>
        <span class="pe-unit">{{ paramEditTarget.unit }}</span>
      </div>
      <div class="pe-preview">
        <span>预览：</span>
        <span :style="{color: paramEditTarget.value < paramEditTarget.limit ? '#34d399' : '#ef4444', fontWeight: 700}">
          {{ paramEditTarget.value }}{{ paramEditTarget.unit }} / 阈值 {{ paramEditTarget.limit }}{{ paramEditTarget.unit }}
        </span>
        <span :class="['pe-st', paramEditTarget.value < paramEditTarget.limit ? 'ok' : 'warn']">
          {{ paramEditTarget.value < paramEditTarget.limit ? '正常' : '超限' }}
        </span>
      </div>
    </div>
    <template #footer>
      <el-button @click="paramDlg=false">取消</el-button>
      <el-button type="primary" @click="saveParamEdit">保存应用</el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from '@/utils/echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'

const store = useDataStore()

const cdev = computed(() => store.devices.find(d => d.id === store.selectedDevice))
const unitName = computed(() => store.units.find(u => u.id === store.selectedDevice?.split('-')[0])?.name || '-')
const result = ref('')
const unitDlg = ref(false), devDlg = ref(false), paramDlg = ref(false)
const paramEditTarget = ref(null)
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

// 报警记录分页
const alarmPage = ref(1)
const pageSize = 4
const pagedAlarms = computed(() => {
  const start = (alarmPage.value - 1) * pageSize
  return deviceAlarms.value.slice(start, start + pageSize)
})

// 健康趋势图
const initTrendChart = () => {
  if (tChart) tChart.dispose()
  if (!trendChart.value || !cdev.value) return
  tChart = echarts.init(trendChart.value)
  const h = cdev.value.health
  const data = Array.from({ length: 24 }, (_, i) => Math.max(60, Math.min(100, h + (Math.random() - 0.5) * 12 - i * 0.15)))
  const labels = Array.from({ length: 24 }, (_, i) => `${23 - i}h`).reverse()
  tChart.setOption({
    backgroundColor: '#000000',
    grid: { left: 42, right: 14, top: 20, bottom: 30 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(0,0,0,0.88)', borderColor: 'rgba(90,166,196,0.25)', textStyle: { color: '#c8e4ff', fontSize: 11 }, formatter: '{b}: <strong>{value}%</strong>' },
    xAxis: {
      type: 'category', data: labels,
      axisLabel: { color: '#7a9cc0', fontSize: 9, interval: 2, rotate: 30, fontFamily: '"SF Mono","Consolas",monospace' },
      axisLine: { lineStyle: { color: 'rgba(90,166,196,0.12)' } },
      axisTick: { show: false },
      splitLine: { show: true, lineStyle: { color: 'rgba(90,166,196,0.08)', type: 'dashed' } }
    },
    yAxis: {
      type: 'value', min: 50, max: 100,
      axisLabel: { color: '#9fb6cf', fontSize: 10, formatter: '{value}%' },
      splitLine: { lineStyle: { color: 'rgba(90,166,196,0.08)', type: 'dashed' } },
      axisLine: { show: false }
    },
    series: [{
      type: 'line', data, smooth: true, symbol: 'circle', symbolSize: 4,
      lineStyle: { width: 2.5, color: '#3eaaff', shadowBlur: 10, shadowColor: '#3eaaff' },
      itemStyle: { color: '#3eaaff', borderColor: '#000000', borderWidth: 1.5 },
      areaStyle: { opacity: 0.25, color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(62,170,255,0.25)' }, { offset: 1, color: 'rgba(62,170,255,0.02)' }]) },
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
const rz = () => tChart?.resize()
onMounted(() => { nextTick(initTrendChart); window.addEventListener('resize', rz) })
onUnmounted(() => { window.removeEventListener('resize', rz); tChart?.dispose() })

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

// 打开测点编辑对话框
const openParamEdit = (row) => {
  // 深拷贝一份防止实时双向绑定到原数据
  paramEditTarget.value = { ...row }
  paramDlg.value = true
}

// 保存测点编辑
const saveParamEdit = () => {
  if (!paramEditTarget.value || !cdev.value) return
  const k = paramEditTarget.value.key
  const newVal = Number(paramEditTarget.value.value)
  const newLimit = Number(paramEditTarget.value.limit)
  // 通过 store 更新当前值和阈值上限（limit 写在 v[1]）
  cdev.value.params[k][0] = newVal
  cdev.value.params[k][1] = newLimit
  // 触发 store 健康度重新计算
  store.updateDeviceParam(cdev.value.id, k, newVal)
  result.value = `<strong>${cdev.value.name}</strong> · ${k} 当前值更新为 <strong>${newVal}${paramEditTarget.value.unit}</strong>，阈值上限更新为 <strong>${newLimit}${paramEditTarget.value.unit}</strong>。`
  paramDlg.value = false
  refreshRows()
  ElMessage.success(`已保存 ${k} 参数修改`)
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
.dm-page { height: 100%; display: flex; flex-direction: column;
  /* 根容器纯黑，消除间隙藏青透显 */
  background-color: #000000;
}
.dm-page :deep(.el-row) { flex: 1; min-height: 0; }
.dm-page :deep(.el-col) { height: 100%; }
.ut-ic { color: #3eaaff; font-weight: 700; margin-right: 2px; }

/* 测点参数编辑对话框 */
.param-edit-form { display: flex; flex-direction: column; gap: 14px; padding: 6px 0; }
.pe-row { display: flex; align-items: center; gap: 12px; }
.pe-row label { width: 80px; font-size: 12.5px; color: #8fb0cf; flex-shrink: 0; }
.pe-val { flex: 1; font-size: 13px; color: #e2e8f0; font-weight: 600; }
.pe-unit { font-size: 11px; color: #8fb0cf; min-width: 32px; }
.pe-preview { display: flex; align-items: center; gap: 10px; padding: 10px 12px; background: rgba(62,170,255,0.05); border: 1px solid rgba(62,170,255,0.15); border-radius: 4px; font-size: 12px; color: #8fb0cf; }
.pe-st { padding: 2px 8px; border-radius: 3px; font-size: 11px; margin-left: auto; }
.pe-st.ok { background: rgba(52,211,153,0.15); color: #34d399; border: 1px solid rgba(52,211,153,0.3); }
.pe-st.warn { background: rgba(239,68,68,0.15); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); }

/* 设备树 */
.dm-tree { height: 100%; display: flex; flex-direction: column; }
.dm-tree .cd-t { flex-shrink: 0; }
.dm-tree-ops { margin-left: auto; display: flex; gap: 6px; }
.dm-tree-body { flex: 1; overflow-y: auto; padding-top: 4px; }
.dm-tree-body::-webkit-scrollbar { width: 3px; }
.dm-tree-body::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.25); border-radius: 2px; }
.unit-block { margin-bottom: 8px; }
.tu { font-weight: 500; padding: 8px 10px; border-radius: 5px; cursor: pointer; font-size: 12.5px; display: flex; align-items: center; gap: 8px; position: relative; transition: 0.15s; border: 1px solid transparent; }
.tu:hover { background: rgba(62,170,255,0.06); }
.tu.ac { background: rgba(62,170,255,0.1); color: #3eaaff; border-color: rgba(62,170,255,0.2); }
.tu-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.tu-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tu-cap { font-size: 10.5px; color: #8fb0cf; font-family: "SF Mono","Consolas",monospace; flex-shrink: 0; }
.tu-del { color: #8fb0cf; padding: 0 6px; border-radius: 3px; flex-shrink: 0; }
.tu-del:hover { color: #ef4444; background: rgba(239,68,68,0.1); }
.td { padding: 6px 10px 6px 24px; cursor: pointer; font-size: 11.5px; color: #8fb0cf; border-radius: 4px; transition: 0.15s; display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5px; }
.td:hover { background: rgba(62,170,255,0.06); color: #c8e4ff; }
.td.ac { background: rgba(62,170,255,0.12); color: #3eaaff; }
.td-h { font-family: "SF Mono","Consolas",monospace; font-weight: 600; font-size: 11px; }

/* 设备信息 */
.dm-right { display: flex; flex-direction: column; gap: 12px; height: 100%; overflow-y: auto; padding-right: 4px;
  /* 右侧区域纯黑底 + CRT 虚线方格 */
  background-color: #000000;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px);
}
.dm-right::-webkit-scrollbar { width: 3px; }
.dm-right::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.dm-info { flex-shrink: 0; }
.dm-info-ops { margin-left: auto; display: flex; gap: 6px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.info-i { display: flex; justify-content: space-between; align-items: center; background: rgba(0,0,0,0.35); border: 1px solid rgba(90,166,196,0.1); border-radius: 5px; padding: 9px 12px; transition: all 0.2s; }
.info-i:hover { border-color: rgba(62,170,255,0.2); }
.info-k { font-size: 11.5px; color: #8fb0cf; }
.info-v { font-size: 14px; color: #e2e8f0; font-weight: 600; }

/* 测点参数 */
.dm-meas { flex-shrink: 0; min-height: 180px; display: flex; flex-direction: column; }
.dm-meas-tip { font-size: 11px; color: #8fb0cf; margin-left: 8px; font-weight: 400; }
.dm-st { display: inline-flex; align-items: center; gap: 5px; font-size: 11.5px; font-weight: 500; }
.dm-st-dot { width: 6px; height: 6px; border-radius: 50%; }
.dm-st.ok { color: #34d399; } .dm-st.ok .dm-st-dot { background: #34d399; box-shadow: 0 0 5px #34d399; }
.dm-st.warn { color: #f87171; } .dm-st.warn .dm-st-dot { background: #f87171; box-shadow: 0 0 5px #f87171; }

.dm-meas-wrap { overflow-x: auto; border-radius: 4px; border: 1px solid rgba(62,170,255,0.1); }
.dm-meas-wrap::-webkit-scrollbar { height: 4px; }
.dm-meas-wrap::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }

.meas-tb { width: 100%; border-collapse: collapse; font-size: 12px; }
.meas-tb th {
  background: rgba(0,0,0,0.42);
  color: #a8c8e4;
  font-weight: 600;
  font-size: 11.5px;
  padding: 9px 10px;
  text-align: left;
  white-space: nowrap;
  border-bottom: 1.5px solid rgba(62,170,255,0.2);
  position: sticky;
  top: 0;
  z-index: 1;
}
.meas-tb td {
  padding: 8px 10px;
  border-bottom: 1px solid rgba(62,170,255,0.07);
  color: #c8e4ff;
  vertical-align: middle;
}
.meas-tb tbody tr {
  transition: background 0.15s;
}
.meas-tb tbody tr:hover {
  background: rgba(20,45,75,0.35);
}
.meas-tb tbody tr.warn {
  background: rgba(120,30,30,0.12);
}
.meas-tb tbody tr.warn:hover {
  background: rgba(140,35,35,0.2);
}

/* 列样式 */
.meas-k { font-weight: 600; color: #d4ecff; font-size: 12px; letter-spacing: 0.3px; }
.meas-v { font-family: "SF Mono","Consolas",monospace; font-weight: 700; font-size: 14px; }
.meas-u-sm { font-size: 10px; color: #7a98b4; margin-left: 3px; font-weight: 400; }
.meas-unit { color: #8fb0cf; font-size: 11px; }
.meas-limit { font-family: "SF Mono","Consolas",monospace; color: #fbbf24; font-weight: 500; font-size: 12px; }

/* 操作列 */
.meas-op { display: flex; align-items: center; gap: 6px; flex-wrap: nowrap; }
.meas-input {
  width: 68px;
  padding: 3px 6px;
  border: 1px solid rgba(62,170,255,0.25);
  border-radius: 4px;
  background: rgba(0,0,0,0.42);
  color: #e2e8f0;
  font-size: 12px;
  font-family: "SF Mono","Consolas",monospace;
  outline: none;
  transition: all 0.2s;
  text-align: center;
}
.meas-input:focus {
  border-color: #3eaaff;
  box-shadow: 0 0 8px rgba(62,170,255,0.25);
}
.meas-btn {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}
.meas-btn.apply {
  background: rgba(56,161,105,0.15);
  color: #34d399;
  border-color: rgba(52,211,153,0.3);
}
.meas-btn.apply:hover {
  background: rgba(56,161,105,0.28);
  box-shadow: 0 0 8px rgba(52,211,153,0.2);
}
.meas-btn.edit {
  background: rgba(62,170,255,0.15);
  color: #5fb3ff;
  border-color: rgba(62,170,255,0.35);
}
.meas-btn.edit:hover {
  background: rgba(62,170,255,0.3);
  box-shadow: 0 0 8px rgba(62,170,255,0.25);
}
.meas-btn.alarm {
  background: rgba(220,80,60,0.12);
  color: #f87171;
  border-color: rgba(248,113,113,0.3);
}
.meas-btn.alarm:hover {
  background: rgba(220,80,60,0.24);
  box-shadow: 0 0 8px rgba(248,113,113,0.2);
}

.empty { padding: 28px; text-align: center; color: #7a98b4; font-size: 12.5px; }

.in { background: rgba(0,0,0,0.35); border: 1px solid rgba(90,166,196,0.15); border-left: 3px solid #34d399; border-radius: 5px; padding: 12px; margin-top: 10px; }
.in-t { font-size: 12.5px; font-weight: 600; color: #34d399; margin-bottom: 6px; display: flex; align-items: center; gap: 6px; }
.in-ic { width: 17px; height: 17px; border-radius: 50%; background: rgba(52,211,153,0.15); border: 1px solid rgba(52,211,153,0.4); display: flex; align-items: center; justify-content: center; font-size: 10.5px; }
.in-b { font-size: 11.5px; color: #cbd5e1; line-height: 1.7; }

.fg { margin-bottom: 12px; }
.fg label { display: block; font-size: 11.5px; color: #9fb6cf; margin-bottom: 4px; }

/* 健康趋势 — 压缩高度确保报警记录可见 */
.dm-trend { flex: 0 0 auto; height: 200px; min-height: 0; max-height: 220px; display: flex; flex-direction: column; }
.dm-trend-chart { flex: 1; min-height: 140px; }

/* 近期报警 — 确保可见 */
.dm-alarms { flex: 0 0 auto; min-height: 260px; max-height: 320px; display: flex; flex-direction: column; }
.dm-pager { display: flex; justify-content: flex-end; padding: 8px 6px 0; flex-shrink: 0; }
.dm-alarm-count { font-size: 10.5px; color: #8fb0cf; margin-left: 8px; font-weight: 400; }
.dm-alarm-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 5px; padding-top: 6px; }
.dm-alarm-list::-webkit-scrollbar { width: 2.5px; }
.dm-alarm-list::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.dm-alarm-i { display: flex; align-items: center; gap: 8px; padding: 7px 10px; border-radius: 4px; background: rgba(0,0,0,0.28); border: 1px solid rgba(90,166,196,0.08); transition: 0.15s; }
.dm-alarm-i:hover { border-color: rgba(62,170,255,0.22); transform: translateX(2px); }
.dm-alarm-lv { font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 3px; flex-shrink: 0; }
.dm-alarm-i.lv1 .dm-alarm-lv { background: rgba(239,68,68,0.2); color: #f87171; border: 1px solid rgba(239,68,68,0.3); }
.dm-alarm-i.lv2 .dm-alarm-lv { background: rgba(245,158,11,0.2); color: #fbbf24; border: 1px solid rgba(245,158,11,0.3); }
.dm-alarm-i.lv3 .dm-alarm-lv { background: rgba(34,211,238,0.15); color: #22d3ee; border: 1px solid rgba(34,211,238,0.25); }
.dm-alarm-desc { font-size: 11.5px; color: #e2e8f0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dm-alarm-time { font-size: 10px; color: #7a98b4; font-family: "SF Mono","Consolas",monospace; flex-shrink: 0; }
.dm-alarm-st { font-size: 9.5px; font-weight: 600; padding: 1px 6px; border-radius: 3px; flex-shrink: 0; }
.dm-alarm-st.resolved { background: rgba(52,211,153,0.15); color: #34d399; }
.dm-alarm-st.confirmed { background: rgba(245,158,11,0.15); color: #fbbf24; }
.dm-alarm-st.unhandled { background: rgba(239,68,68,0.15); color: #f87171; }

/* ═══ 右侧四面板纯黑+CRT虚线方格（压制全局 .cd !important，设备树保留深蓝，标题栏不动） ═══ */
.dm-page .dm-right .cd:not(.dm-tree) {
  background-color: #000000 !important;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px) !important;
  border-color: rgba(90,166,196,0.12) !important;
  box-shadow: none !important;
}
/* 标题栏不透明深蓝底，盖住下方网格（外观与原来一致，不改色） */
.dm-page .dm-right .cd:not(.dm-tree) .cd-t {
  background-color: #050f16;
}

/* ═══ 通用面板样式 .cd — 科技风统一 ═══ */
.cd {
  background: linear-gradient(180deg, rgba(5,12,22,0.455), rgba(3,8,15,0.42));
  border: 1px solid rgba(62,170,255,0.13);
  border-radius: 6px;
  position: relative;
  overflow: hidden;
  transition: border-color 0.2s;
}
.cd:hover { border-color: rgba(62,170,255,0.22); }
/* 面板头部 */
.cd-t {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 14px; font-size: 13px; font-weight: 600;
  color: #d4ecff; letter-spacing: 0.3px;
  background: linear-gradient(90deg, rgba(62,170,255,0.12), rgba(62,170,255,0.02));
  border-left: 3px solid #3eaaff;
  margin: -1px -1px 10px;
  box-shadow: -3px 0 10px -2px rgba(62,170,255,0.12);
}
</style>

<!-- ═══ 非scoped覆盖：压制全局 .cd !important 深蓝锁色 ═══ -->
<style>
/* 右侧四面板内容区纯黑 + CRT 虚线方格 */
.dm-page .dm-right > .cd:not(.dm-tree) {
  background-color: #04060a !important;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.12) 0px, rgba(90,166,196,0.12) 1px, transparent 1px, transparent 28px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.12) 0px, rgba(90,166,196,0.12) 1px, transparent 1px, transparent 28px) !important;
  border-color: rgba(90,166,196,0.16) !important;
  box-shadow: none !important;
}
/* 标题栏不透明深蓝底，挡住网格 */
.dm-page .dm-right > .cd:not(.dm-tree) > .cd-t {
  background-color: #06121c !important;
  background-image: none !important;
}
</style>
