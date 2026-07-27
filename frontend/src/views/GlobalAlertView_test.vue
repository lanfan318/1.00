<template>
<div class="bs">
  <!-- ===== 顶栏：标题 + 时钟 + 面板切换 ===== -->
  <div class="bs-head">
    <div class="bs-head-l">
      <div class="bs-title">火电运行全域告警监控大屏</div>
      <div class="bs-sub">{{ store.selectedUnit.name }} · {{ store.selectedUnit.type }} · {{ store.selectedUnit.capacity }}MW</div>
    </div>
    <div class="bs-head-r">
      <!-- 面板切换按钮组 -->
      <div class="panel-switch">
        <button class="ps-btn" :class="{active: activePanel==='monitor'}" @click="activePanel='monitor'">
          <span class="ps-icon">◈</span> 设备监控
        </button>
        <button class="ps-btn" :class="{active: activePanel==='analysis'}" @click="activePanel='analysis'">
          <span class="ps-icon">◉</span> 智能分析
        </button>
      </div>
      <div class="bs-clock">{{ clock }}</div>
    </div>
  </div>

  <!-- 机组选择（紧凑横条） -->
  <div class="bs-unit-bar">
    <span class="bs-ub-lbl">⚡</span>
    <div v-for="u in store.units" :key="u.id"
         class="bs-ub-card" :class="{on: store.selectedUnitId===u.id}"
         @click="switchUnit(u.id)">
      <span class="bs-ub-name">{{ u.name }}</span>
      <span class="bs-ub-dev">{{ store.unitDevices(u.id).length }} 设备</span>
      <span class="bs-ub-alarm" :class="{warn: store.unitAlarms(u.id).length>0}">{{ store.unitAlarms(u.id).length }}</span>
    </div>
  </div>

  <!-- ==================== 面板 A：设备监控 ==================== -->
  <div v-show="activePanel==='monitor'" class="panel-monitor">

    <!-- KPI 指标条 -->
    <div class="kpi-bar">
      <div class="kpi-item" v-for="(k,i) in kpiList" :key="i">
        <div class="kpi-icon" :style="{background: k.bg}">{{ k.icon }}</div>
        <div class="kpi-body">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value" :style="{color: k.color}">{{ k.value }}</div>
        </div>
      </div>
    </div>

    <!-- 核心区域：蜂窝矩阵（居中大块） -->
    <div class="matrix-zone">
      <div class="mz-header">
        <div class="mz-title">
          <span class="mz-icon">⬡</span> 设备健康度矩阵
          <span class="mz-sub">{{ store.selectedUnit.name }} · {{ unitDevices.length }} 台设备</span>
        </div>
        <div class="mz-legend">
          <span class="lz"><i class="lz-dot ok"></i> 健康 ≥90</span>
          <span class="lz"><i class="lz-dot warn"></i> 关注 80~90</span>
          <span class="lz"><i class="lz-dot danger"></i> 异常 &lt;80</span>
        </div>
      </div>

      <!-- 蜂窝网格容器 -->
      <div class="honeycomb-wrap">
        <div class="honeycomb" :style="{width: hcWidth + 'px'}">
          <div v-for="(cell, idx) in honeycombCells" :key="cell.device?.id || idx"
               class="hc-cell"
               :class="[cell.healthClass, cell.empty ? 'empty' : '']"
               :style="cell.style"
               @click="cell.device && goDevice(cell.device)">
            <template v-if="!cell.empty">
              <!-- 设备图标（按类型） -->
              <div class="hc-icon-wrap" :class="'type-' + cell.deviceType">
                <svg class="hc-svg" viewBox="0 0 40 40" v-html="deviceIconSvg(cell.device)"></svg>
              </div>
              <!-- 设备名 -->
              <div class="hc-name" :title="cell.device.name">{{ shortName(cell.device.name) }}</div>
              <!-- 健康度数值 -->
              <div class="hc-health" :class="cell.healthClass">{{ cell.device.health.toFixed(1) }}</div>
              <!-- 健康度环 -->
              <svg class="hc-ring" viewBox="0 0 36 36">
                <circle cx="18" cy="18" r="15.5" fill="none" :stroke="ringColor(cell.device.health)" stroke-width="3" :stroke-dasharray="cell.device.health + ' ' + (100-cell.device.health)" stroke-linecap="round" transform="rotate(-90 18 18)" />
              </svg>
            </template>
            <template v-else>
              <div class="hc-empty">+</div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 趋势图区域：上下排布 -->
    <div class="trend-zone">
      <!-- 实时数据趋势图 -->
      <div class="tz-card">
        <div class="tz-header">
          <span class="tz-title">📡 设备实时参数趋势</span>
          <span class="tz-badge live">LIVE</span>
        </div>
        <div ref="realtimeRef" class="tz-chart"></div>
      </div>
      <!-- 24小时告警趋势图 -->
      <div class="tz-card">
        <div class="tz-header">
          <span class="tz-title">📈 近 24 小时告警趋势</span>
          <div class="tz-legends">
            <span class="tz-l"><i style="background:#ef4444"></i>一级</span>
            <span class="tz-l"><i style="background:#f59e0b"></i>二级</span>
            <span class="tz-l"><i style="background:#06b6d4"></i>智能预警</span>
          </div>
        </div>
        <div ref="alarmTrendRef" class="tz-chart"></div>
      </div>
    </div>
  </div>

  <!-- ==================== 面板 B：智能分析 ==================== -->
  <div v-show="activePanel==='analysis'" class="panel-analysis">
    <div class="pa-grid">
      <!-- 左列：重点关注设备 TOP5 -->
      <div class="pa-card pa-wide">
        <div class="pa-header"><span class="pa-icon">⚠️</span> 重点关注设备 TOP5</div>
        <table class="pa-table">
          <thead><tr><th>设备</th><th>专业</th><th>当前问题</th><th>健康度</th><th>建议措施</th></tr></thead>
          <tbody>
            <tr v-for="d in topWarn" :key="d.id" :class="d.health<70?'row-danger':d.health<80?'row-warn':''">
              <td class="td-name">
                <span class="td-type-icon" :class="'type-'+deviceTypeKey(d)">{{ typeIcon(deviceTypeKey(d)) }}</span>
                {{ d.name }}
              </td>
              <td>{{ d.dept }}</td>
              <td :class="d.health<70?'text-danger':'text-warn'">{{ d.problem }}</td>
              <td><span class="health-badge" :class="d.health>=90?'ok':d.health>=80?'warn':'danger'">{{ d.health.toFixed(1) }}</span></td>
              <td class="td-suggest">{{ d.suggest }}</td>
            </tr>
            <tr v-if="topWarn.length===0"><td colspan="5" class="empty-row">✅ 所有设备运行正常</td></tr>
          </tbody>
        </table>
      </div>

      <!-- 中列：AI 智能洞察 + 告警分布 -->
      <div class="pa-col">
        <div class="pa-card">
          <div class="pa-header"><span class="pa-icon">🤖</span> AI 智能洞察</div>
          <div class="ai-insight">
            <div v-for="(ins, i) in insights" :key="i" class="ai-card" :class="ins.type">
              <div class="ai-left">
                <div class="ai-icon-ring" :class="ins.type">{{ ins.icon }}</div>
              </div>
              <div class="ai-right">
                <div class="ai-title">{{ ins.title }}</div>
                <div class="ai-content">{{ ins.content }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="pa-card">
          <div class="pa-header"><span class="pa-icon">📊</span> 告警专业分布</div>
          <div ref="profRef" class="pa-chart-sm"></div>
        </div>
      </div>

      <!-- 右列：最近告警 -->
      <div class="pa-card">
        <div class="pa-header"><span class="pa-icon">🔔</span> 最近告警 <span class="pa-count">{{ recent.length }}</span></div>
        <div class="alarm-timeline">
          <div v-for="a in recent" :key="a.id" class="at-item" :class="a.st">
            <div class="at-dot-wrap">
              <div class="at-dot" :class="'l'+a.l"></div>
              <div class="at-line"></div>
            </div>
            <div class="at-body">
              <div class="at-head">
                <span class="at-dev">{{ a.device }}</span>
                <span class="at-time">{{ fmt(a.time) }}</span>
                <span class="at-tag" :class="'st-'+a.st">{{ stTxt(a.st) }}</span>
              </div>
              <div class="at-desc">{{ a.desc }}</div>
              <div class="at-meta">
                <span class="at-point">{{ a.point }}: {{ a.val }}</span>
                <span class="at-dept">{{ a.dept }}</span>
              </div>
            </div>
          </div>
          <div v-if="recent.length===0" class="at-empty">暂无告警记录</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const router = useRouter()

// ---- 状态 ----
const activePanel = ref('monitor')   // monitor | analysis
const clock = ref('')
let iv

// 图表 refs
const realtimeRef = ref(null)
const alarmTrendRef = ref(null)
const profRef = ref(null)
let rtCh, atCh, pCh

// ---- 数据计算 ----
const unitDevices = computed(() => store.unitDevices(store.selectedUnitId))
const unitAlarms = computed(() => store.unitAlarms(store.selectedUnitId))

const cnt = (l) => unitAlarms.value.filter(a => a.l === l).length
const avgHealth = computed(() => {
  const ds = unitDevices.value
  if (!ds.length) return 0
  return (ds.reduce((s, d) => s + d.health, 0) / ds.length).toFixed(1)
})

// KPI 列表
const kpiList = computed(() => [
  { icon: '🔴', label: '总告警', value: unitAlarms.value.length, color: '#3b82f6', bg: 'rgba(239,68,68,0.1)' },
  { icon: '⛔', label: '一级报警', value: cnt(1), color: '#ef4444', bg: 'rgba(239,68,68,0.12)' },
  { icon: '⚠️', label: '二级报警', value: cnt(2), color: '#f59e0b', bg: 'rgba(245,158,11,0.12)' },
  { icon: '💡', label: '智能预警', value: cnt(3), color: '#06b6d4', bg: 'rgba(6,182,212,0.12)' },
  { icon: '⏳', label: '未处理', value: unitAlarms.value.filter(a=>a.st==='unhandled').length, color: '#ef4444', bg: 'rgba(239,68,68,0.08)' },
  { icon: '❤️', label: '设备健康度', value: avgHealth.value + '%', color: Number(avgHealth.value)>=90?'#22c55e':Number(avgHealth.value)>=80?'#f59e0b':'#ef4444', bg: 'rgba(34,197,94,0.08)' }
])

// ---- 蜂窝矩阵 ----
// 蜂窝布局：奇数行偏移半个格子，每行数量交替
const HC_CELL_W = 110   // 六边形宽度
const HC_CELL_H = 96    // 六边形高度（含间距）
const HC_GAP_X = 6
const HC_GAP_Y = 8

const honeycombCells = computed(() => {
  const devices = unitDevices.value
  const cells = []
  // 计算需要的行列数：尽量接近正方形
  const total = Math.max(devices.length, 16) // 至少 16 个位置
  const cols = 7 // 每行最多 7 个
  let idx = 0
  for (let row = 0; row < 5; row++) {
    const count = row % 2 === 0 ? cols : cols - 1 // 奇数行少一个
    const offsetX = row % 2 === 0 ? 0 : (HC_CELL_W + HC_GAP_X) / 2
    for (let col = 0; col < count; col++) {
      const dev = idx < devices.length ? devices[idx] : null
      const health = dev ? dev.health : 0
      cells.push({
        device: dev,
        empty: !dev,
        healthClass: health >= 90 ? 'ok' : health >= 80 ? 'warn' : health > 0 ? 'danger' : '',
        deviceType: dev ? deviceTypeKey(dev) : '',
        style: {
          left: `${col * (HC_CELL_W + HC_GAP_X) + offsetX}px`,
          top: `${row * (HC_CELL_H * 0.85 + HC_GAP_Y)}px`
        }
      })
      idx++
    }
  }
  return cells
})

const hcWidth = computed(() => {
  const maxCols = 7
  return maxCols * (HC_CELL_W + HC_GAP_X) + (HC_CELL_W + HC_GAP_X) / 2 + 20
})

// 设备类型分类
function deviceTypeKey(d) {
  const n = d.name || ''
  if (n.includes('风机')) return 'fan'
  if (n.includes('磨煤机')) return 'mill'
  if (n.includes('泵')) return 'pump'
  if (n.includes('润滑油') || n.includes('油')) return 'oil'
  if (n.includes('凝汽器')) return 'condenser'
  if (n.includes('加热器')) return 'heater'
  if (n.includes('除氧器')) return 'deox'
  return 'generic'
}

function typeIcon(key) {
  const map = { fan: '🌀', mill: '⚙️', pump: '💧', oil: '🛢️', condenser: '❄️', heater: '🔥', deox: '💨', generic: '⬡' }
  return map[key] || '⬡'
}

// 设备 SVG 图标（科技感）
function deviceIconSvg(dev) {
  const t = deviceTypeKey(dev)
  const svgs = {
    fan: `<path d="M20 6 L28 14 L28 26 L20 34 L12 26 L12 14 Z M20 10 L15 18 L20 22 L25 18 Z" fill="none" stroke="currentColor" stroke-width="1.5"/><circle cx="20" cy="18" r="4" fill="none" stroke="currentColor" stroke-width="1"/><line x1="20" y1="6" x2="20" y2="10" stroke="currentColor" stroke-width="1.5"/><line x1="20" y1="30" x2="20" y2="34" stroke="currentColor" stroke-width="1.5"/>`,
    mill: `<circle cx="20" cy="20" r="12" fill="none" stroke="currentColor" stroke-width="1.5"/><line x1="20" y1="8" x2="20" y2="32" stroke="currentColor" stroke-width="1.5"/><line x1="8" y1="20" x2="32" y2="20" stroke="currentColor" stroke-width="1.5"/><circle cx="20" cy="20" r="4" fill="currentColor" opacity="0.3"/>`,
    pump: `<path d="M12 24 Q12 14 20 14 Q28 14 28 24 L28 30 L12 30 Z" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M16 24 L16 30 M24 24 L24 30" stroke="currentColor" stroke-width="1.5"/><path d="M20 10 L20 14 M17 11 L23 11" stroke="currentColor" stroke-width="1.5"/>`,
    oil: `<rect x="12" y="12" width="16" height="18" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><line x1="16" y1="18" x2="24" y2="18" stroke="currentColor" stroke-width="1"/><line x1="16" y1="22" x2="24" y2="22" stroke="currentColor" stroke-width="1"/><line x1="16" y1="26" x2="20" y2="26" stroke="currentColor" stroke-width="1"/><circle cx="27" cy="10" r="3" fill="none" stroke="currentColor" stroke-width="1"/>`,
    condenser: `<rect x="10" y="14" width="20" height="16" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><line x1="14" y1="18" x2="26" y2="18" stroke="currentColor" stroke-width="1"/><line x1="14" y1="22" x2="26" y2="22" stroke="currentColor" stroke-width="1"/><line x1="14" y1="26" x2="26" y2="26" stroke="currentColor" stroke-width="1"/><path d="M16 10 Q20 6 24 10" fill="none" stroke="currentColor" stroke-width="1.5"/>`,
    heater: `<path d="M10 28 L10 16 Q10 10 16 10 L24 10 Q30 10 30 16 L30 28" fill="none" stroke="currentColor" stroke-width="1.5"/><line x1="10" y1="20" x2="30" y2="20" stroke="currentColor" stroke-width="1" stroke-dasharray="2 2"/><path d="M16 6 L20 10 L24 6" fill="none" stroke="currentColor" stroke-width="1.5"/>`,
    deox: `<ellipse cx="20" cy="22" rx="12" ry="8" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M8 22 L8 14 Q8 8 20 8 Q32 8 32 14 L32 22" fill="none" stroke="currentColor" stroke-width="1.5"/><line x1="20" y1="8" x2="20" y2="4" stroke="currentColor" stroke-width="1.5"/><circle cx="20" cy="3" r="2" fill="currentColor" opacity="0.5"/>`,
    generic: `<polygon points="20,6 32,14 32,26 20,34 8,26 8,14" fill="none" stroke="currentColor" stroke-width="1.5"/><circle cx="20" cy="20" r="5" fill="none" stroke="currentColor" stroke-width="1"/>`
  }
  return svgs[t] || svgs.generic
}

function ringColor(h) {
  return h >= 90 ? '#22c55e' : h >= 80 ? '#f59e0b' : '#ef4444'
}

function shortName(n) {
  if (n.length <= 5) return n
  return n.replace(/[ABCD]/g, '').slice(0, 5)
}

// ---- 分析面板数据 ----
const fmt = (t) => { const d = new Date(t); return d.toLocaleTimeString('zh-CN', { hour12: false }).slice(0, 8) }
const stTxt = (st) => st === 'unhandled' ? '未处理' : st === 'confirmed' ? '已确认' : '已处理'

const recent = computed(() => unitAlarms.value.slice(0, 8))

const topWarn = computed(() => {
  const ds = unitDevices.value.filter(d => d.health < 90)
  return ds.sort((a, b) => a.health - b.health).slice(0, 5).map(d => ({
    ...d,
    problem: d.health < 70 ? '严重异常，建议立即停机检查' : d.health < 80 ? '健康度偏低，需安排检修' : '部分参数超限，需关注',
    suggest: d.dept === '汽轮机' ? '检查轴承润滑/振动情况' : d.name.includes('磨') ? '检查磨辊磨损/煤质' : '加强巡检频次'
  }))
})

const insights = computed(() => {
  const out = []
  const ds = unitDevices.value
  const lowH = ds.filter(d => d.health < 80)
  if (lowH.length) out.push({ type: 'danger', icon: '🚨', title: `设备健康度告警`, content: `${store.selectedUnit.name} 有 ${lowH.length} 台设备健康度低于 80：${lowH.map(d => d.name).join('、')}。建议立即启动预防性检修流程。` })
  const l1 = unitAlarms.value.filter(a => a.l === 1 && a.st === 'unhandled')
  if (l1.length) out.push({ type: 'danger', icon: '⚠️', title: '一级报警未处置', content: `${l1.length} 条一级报警处于未处理状态，超时将自动升级。` })
  const idfWarn = ds.find(d => d.name === 'A引风机' && d.health < 85)
  if (idfWarn) out.push({ type: 'warn', icon: '🌡️', title: 'A引风机温度预警', content: `轴承温度偏离正常值，温升速率加快。AI 建议：48h 内安排润滑油脂更换。` })
  const millWarn = ds.find(d => d.name.includes('磨煤机') && d.health < 85)
  if (millWarn) out.push({ type: 'warn', icon: '⚙️', title: '磨煤机振动预警', content: `磨辊磨损不均导致动不平衡。AI 建议：降低给煤量至 70%。` })
  const emCheck = unitAlarms.value.filter(a => a.point?.includes('NOx'))
  if (emCheck.length === 0) out.push({ type: 'info', icon: '✅', title: '环保排放达标', content: `NOx/SO2/粉尘均在国标限值 70% 以下。` })
  if (out.length === 0) out.push({ type: 'ok', icon: '✅', title: '系统状态正常', content: `${store.selectedUnit.name} 各设备运行平稳，暂未检测到需关注的告警。` })
  return out
})

// ---- 操作 ----
const switchUnit = (id) => {
  store.selectedUnitId = id
  nextTick(() => initCharts())
}
const goDevice = (d) => {
  store.selectedDevice = d.id
  router.push('/devices')
}

// ---- 图表初始化 ----
const initCharts = () => {
  // 清理旧图表
  if (rtCh) rtCh.dispose(); if (atCh) atCh.dispose(); if (pCh) pCh.dispose()

  // 实时数据趋势图
  if (realtimeRef.value) {
    rtCh = echarts.init(realtimeRef.value)
    const now = Date.now()
    const times = Array.from({ length: 30 }, (_, i) => {
      const d = new Date(now - (29 - i) * 2000)
      return d.toLocaleTimeString('zh-CN', { hour12: false }).slice(0, 8)
    })
    rtCh.setOption({
      grid: { left: 45, right: 15, top: 20, bottom: 28 },
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,20,35,0.95)', borderColor: '#1e293b', textStyle: { color: '#94a3b8', fontSize: 11 } },
      xAxis: { type: 'category', data: times, axisLabel: { color: '#64748b', fontSize: 11, interval: 4 }, axisLine: { lineStyle: { color: '#1e293b' } }, axisTick: { show: false } },
      yAxis: { type: 'value', axisLabel: { color: '#64748b', fontSize: 11, formatter: '{value}' }, splitLine: { lineStyle: { color: '#1e293b', type: 'dashed' } } },
      series: [
        { name: '主汽温度', type: 'line', smooth: true, data: Array.from({ length: 30 }, () => 535 + Math.random() * 12), lineStyle: { color: '#ef4444', width: 2 }, areaStyle: { color: 'rgba(239,68,68,0.08)' }, symbol: 'none', symbolSize: 4 },
        { name: '负荷', type: 'line', smooth: true, data: Array.from({ length: 30 }, () => 480 + Math.random() * 30), lineStyle: { color: '#3b82f6', width: 2 }, areaStyle: { color: 'rgba(59,130,246,0.06)' }, symbol: 'none' },
        { name: '主汽压力', type: 'line', smooth: true, data: Array.from({ length: 30 }, () => 16.4 + Math.random() * 0.6), lineStyle: { color: '#22c55e', width: 2 }, areaStyle: { color: 'rgba(34,197,94,0.05)' }, symbol: 'none' }
      ]
    })
  }

  // 24h 告警趋势图
  if (alarmTrendRef.value) {
    atCh = echarts.init(alarmTrendRef.value)
    atCh.setOption({
      grid: { left: 45, right: 15, top: 30, bottom: 28 },
      legend: { textStyle: { color: '#64748b', fontSize: 11 }, top: 2, right: 0, itemWidth: 12, itemHeight: 3 },
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,20,35,0.95)', borderColor: '#1e293b', textStyle: { color: '#94a3b8', fontSize: 11 } },
      xAxis: { type: 'category', data: Array.from({ length: 24 }, (_, i) => i + ':00'), axisLabel: { color: '#64748b', fontSize: 11, interval: 3 }, axisLine: { lineStyle: { color: '#1e293b' } }, axisTick: { show: false } },
      yAxis: { type: 'value', axisLabel: { color: '#64748b', fontSize: 11 }, splitLine: { lineStyle: { color: '#1e293b', type: 'dashed' } } },
      series: [
        { name: '一级', type: 'bar', stack: 'alarm', data: Array.from({ length: 24 }, () => Math.random() > 0.75 ? Math.floor(Math.random() * 3) : 0), itemStyle: { color: '#ef4444', borderRadius: [0, 0, 2, 2] }, barWidth: '50%' },
        { name: '二级', type: 'bar', stack: 'alarm', data: Array.from({ length: 24 }, () => Math.floor(Math.random() * 5)), itemStyle: { color: '#f59e0b', borderRadius: [0, 0, 2, 2] } },
        { name: '智能预警', type: 'line', smooth: true, data: Array.from({ length: 24 }, () => 4 + Math.random() * 8), lineStyle: { color: '#06b6d4', width: 2 }, areaStyle: { color: 'rgba(6,182,212,0.06)' }, symbol: 'none' }
      ]
    })
  }

  // 分布饼图（分析面板）
  if (profRef.value) {
    pCh = echarts.init(profRef.value)
    const profCnt = { '锅炉': 0, '汽轮机': 0, '辅网': 0 }
    unitAlarms.value.forEach(a => { profCnt[a.dept] = (profCnt[a.dept] || 0) + 1 })
    pCh.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{ type: 'pie', radius: ['42%', '70%'], center: ['50%', '52%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#0a0e17', borderWidth: 2, borderRadius: 4 },
        label: { color: '#94a3b8', fontSize: 11 },
        data: [
          { value: profCnt['锅炉'], name: '锅炉', itemStyle: { color: '#f59e0b' } },
          { value: profCnt['汽轮机'], name: '汽机', itemStyle: { color: '#3b82f6' } },
          { value: profCnt['辅网'], name: '辅网', itemStyle: { color: '#22c55e' } }
        ].filter(d => d.value > 0)
      }]
    })
  }
}

watch(() => store.selectedUnitId, () => nextTick(initCharts))
watch(activePanel, () => nextTick(() => {
  // 切换面板后重新渲染图表（解决 display:none 导致的尺寸问题）
  setTimeout(initCharts, 50)
}))

onMounted(() => {
  clock.value = new Date().toLocaleString('zh-CN', { hour12: false })
  iv = setInterval(() => { clock.value = new Date().toLocaleString('zh-CN', { hour12: false }) }, 1000)
  nextTick(initCharts)

  // resize 监听
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  clearInterval(iv)
  rtCh?.dispose(); atCh?.dispose(); pCh?.dispose()
  window.removeEventListener('resize', handleResize)
})
const handleResize = () => {
  rtCh?.resize(); atCh?.resize(); pCh?.resize()
}
</script>

<style scoped>
/* ========== 全局 ========== */
.bs { min-height: 100vh; padding: 12px 16px; background: linear-gradient(180deg, #040812 0%, #0a1020 50%, #060c18 100%); position: relative; overflow-x: hidden; }

/* 背景装饰 */
.bs::before {
  content: ''; position: absolute; inset: 0;
  background:
    radial-gradient(ellipse 800px 400px at 50% 0%, rgba(59,130,246,0.06) 0%, transparent 70%),
    radial-gradient(ellipse 600px 300px at 80% 100%, rgba(6,182,212,0.04) 0%, transparent 70%);
  pointer-events: none;
}
.bs::after {
  content: ''; position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(59,130,246,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59,130,246,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
}

/* ========== 顶栏 ========== */
.bs-head { display: flex; justify-content: space-between; align-items: flex-start; position: relative; z-index: 2; margin-bottom: 10px; padding: 0 4px; }
.bs-head-l { display: flex; flex-direction: column; gap: 2px; }
.bs-title { font-size: 22px; font-weight: 700; background: linear-gradient(90deg, #60a5fa, #38bdf8, #06b6d4); -webkit-background-clip: text; -webkit-text-fill-color: transparent; letter-spacing: 3px; text-shadow: none; }
.bs-sub { font-size: 11px; color: #64748b; letter-spacing: 1px; }
.bs-head-r { display: flex; align-items: center; gap: 16px; }

/* 面板切换按钮 */
.panel-switch { display: flex; gap: 2px; background: rgba(15,23,42,0.6); border: 0.5px solid rgba(59,130,246,0.2); border-radius: 8px; padding: 3px; }
.ps-btn { display: flex; align-items: center; gap: 6px; padding: 6px 16px; border: none; border-radius: 6px; font-size: 12px; cursor: pointer; transition: all 0.25s; background: transparent; color: #64748b; font-weight: 500; }
.ps-btn:hover { color: #94a3b8; background: rgba(59,130,246,0.06); }
.ps-btn.active { background: rgba(59,130,246,0.18); color: #60a5fa; box-shadow: 0 0 12px rgba(59,130,246,0.15), inset 0 0 0 0.5px rgba(96,165,250,0.3); }
.ps-icon { font-size: 13px; }

.bs-clock { color: #475569; font-family: 'SF Mono', 'Consolas', monospace; font-size: 12px; letter-spacing: 1px; }

/* ========== 机组选择条 ========== */
.bs-unit-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; padding: 8px 14px; background: rgba(15,23,42,0.5); border: 0.5px solid rgba(59,130,246,0.15); border-radius: 10px; position: relative; z-index: 2; backdrop-filter: blur(8px); }
.bs-ub-lbl { font-size: 16px; color: #f59e0b; }
.bs-ub-card { display: flex; align-items: center; gap: 8px; padding: 6px 14px; border-radius: 6px; cursor: pointer; transition: all 0.2s; border: 0.5px solid transparent; background: rgba(15,23,42,0.4); }
.bs-ub-card:hover { border-color: rgba(59,130,246,0.3); background: rgba(59,130,246,0.06); }
.bs-ub-card.on { border-color: rgba(59,130,246,0.5); background: rgba(59,130,246,0.12); box-shadow: 0 0 16px rgba(59,130,246,0.1); }
.bs-ub-name { font-size: 13px; font-weight: 600; color: #e2e8f0; }
.bs-ub-dev { font-size: 11px; color: #64748b; }
.bs-ub-alarm { font-size: 11px; font-weight: 700; color: #475569; min-width: 18px; text-align: center; }
.bs-ub-alarm.warn { color: #ef4444; text-shadow: 0 0 8px rgba(239,68,68,0.5); }

/* ========== KPI 指标条 ========== */
.kpi-bar { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; margin-bottom: 14px; position: relative; z-index: 2; }
.kpi-item { display: flex; align-items: center; gap: 10px; padding: 12px 14px; background: rgba(15,23,42,0.5); border: 0.5px solid rgba(59,130,246,0.12); border-radius: 10px; backdrop-filter: blur(8px); transition: all 0.25s; }
.kpi-item:hover { border-color: rgba(59,130,246,0.3); transform: translateY(-1px); box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
.kpi-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
.kpi-label { font-size: 11px; color: #64748b; margin-bottom: 2px; }
.kpi-value { font-size: 22px; font-weight: 700; font-family: 'DIN Alternate', 'Roboto Mono', monospace; }

/* ========== 蜂窝矩阵区域 ========== */
.matrix-zone { position: relative; z-index: 2; margin-bottom: 14px; background: rgba(15,23,42,0.35); border: 0.5px solid rgba(59,130,246,0.15); border-radius: 12px; padding: 16px 20px 20px; backdrop-filter: blur(8px); }
.mz-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.mz-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #e2e8f0; }
.mz-icon { font-size: 18px; color: #3b82f6; }
.mz-sub { font-size: 11px; color: #64748b; font-weight: 400; margin-left: 4px; }
.mz-legend { display: flex; gap: 16px; }
.lz { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #64748b; }
.lz-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.lz-dot.ok { background: #22c55e; box-shadow: 0 0 6px rgba(34,197,94,0.5); }
.lz-dot.warn { background: #f59e0b; box-shadow: 0 0 6px rgba(245,158,11,0.5); }
.lz-dot.danger { background: #ef4444; box-shadow: 0 0 6px rgba(239,68,68,0.5); animation: pulse-danger 1.5s infinite; }
@keyframes pulse-danger { 0%,100%{opacity:1} 50%{opacity:.5} }

/* 蜂窝容器 */
.honeycomb-wrap { overflow-x: auto; padding: 10px 0 20px; }
.honeycomb { position: relative; height: 420px; margin: 0 auto; }

/* 六边形单元格 */
.hc-cell {
  position: absolute;
  width: 110px; height: 96px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  clip-path: polygon(50% 0%, 93% 25%, 93% 75%, 50% 100%, 7% 75%, 7% 25%);
  background: rgba(15,23,42,0.7);
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 6px 8px;
  gap: 1px;
}
.hc-cell::before {
  content: ''; position: absolute; inset: 0;
  clip-path: polygon(50% 0%, 93% 25%, 93% 75%, 50% 100%, 7% 75%, 7% 25%);
  border-radius: 0;
  transition: all 0.3s;
}
.hc-cell:not(.empty):hover { transform: scale(1.08); z-index: 10; }

/* 健康态颜色 */
.hc-cell.ok { background: rgba(34,197,94,0.08); }
.hc-cell.ok::before { box-shadow: inset 0 0 0 1px rgba(34,197,94,0.35), 0 0 12px rgba(34,197,94,0.08); }
.hc-cell.ok:hover { background: rgba(34,197,94,0.16); box-shadow: 0 0 24px rgba(34,197,94,0.2); }

.hc-cell.warn { background: rgba(245,158,11,0.1); }
.hc-cell.warn::before { box-shadow: inset 0 0 0 1px rgba(245,158,11,0.35), 0 0 12px rgba(245,158,11,0.1); }
.hc-cell.warn:hover { background: rgba(245,158,11,0.18); box-shadow: 0 0 24px rgba(245,158,11,0.2); }

.hc-cell.danger { background: rgba(239,68,68,0.1); }
.hc-cell.danger::before { box-shadow: inset 0 0 0 1px rgba(239,68,68,0.4), 0 0 16px rgba(239,68,68,0.15); animation: hex-pulse 2s infinite; }
.hc-cell.danger:hover { background: rgba(239,68,68,0.18); box-shadow: 0 0 28px rgba(239,68,68,0.3); }
@keyframes hex-pulse { 0%,100%{box-shadow: inset 0 0 0 1px rgba(239,68,68,0.4), 0 0 12px rgba(239,68,68,0.1)} 50%{box-shadow: inset 0 0 0 1px rgba(239,68,68,0.6), 0 0 24px rgba(239,68,68,0.3)} }

.hc-cell.empty { background: rgba(30,41,59,0.3); cursor: default; }
.hc-cell.empty::before { box-shadow: inset 0 0 0 1px rgba(51,65,85,0.3); }

/* 设备图标 */
.hc-icon-wrap { width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-bottom: 2px; position: relative; }
.hc-icon-wrap.type-fan { background: linear-gradient(135deg, rgba(59,130,246,0.2), rgba(99,102,241,0.15)); color: #60a5fa; }
.hc-icon-wrap.type-mill { background: linear-gradient(135deg, rgba(245,158,11,0.2), rgba(234,179,8,0.15)); color: #fbbf24; }
.hc-icon-wrap.type-pump { background: linear-gradient(135deg, rgba(6,182,212,0.2), rgba(34,211,238,0.15)); color: #22d3ee; }
.hc-icon-wrap.type-oil { background: linear-gradient(135deg, rgba(168,85,247,0.2), rgba(139,92,246,0.15)); color: #a78bfa; }
.hc-icon-wrap.type-condenser { background: linear-gradient(135deg, rgba(34,197,94,0.2), rgba(16,185,129,0.15)); color: #34d399; }
.hc-icon-wrap.type-heater { background: linear-gradient(135deg, rgba(239,68,68,0.15), rgba(220,38,38,0.1)); color: #f87171; }
.hc-icon-wrap.type-deox { background: linear-gradient(135deg, rgba(148,163,184,0.2), rgba(100,116,139,0.15)); color: #94a3b8; }
.hc-icon-wrap.type-generic { background: linear-gradient(135deg, rgba(71,85,105,0.2), rgba(51,65,85,0.15)); color: #94a3b8; }

.hc-svg { width: 24px; height: 24px; color: currentColor; }

/* 设备名称 */
.hc-name { font-size: 9px; color: #94a3b8; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 90px; text-align: center; letter-spacing: 0.5px; }

/* 健康度数值 */
.hc-health { font-size: 15px; font-weight: 700; font-family: 'DIN Alternate', 'Roboto Mono', monospace; line-height: 1; }
.hc-health.ok { color: #22c55e; text-shadow: 0 0 8px rgba(34,197,94,0.4); }
.hc-health.warn { color: #fbbf24; text-shadow: 0 0 8px rgba(251,191,36,0.4); }
.hc-health.danger { color: #ef4444; text-shadow: 0 0 8px rgba(239,68,68,0.5); }

/* 健康度环 */
.hc-ring { position: absolute; bottom: 4px; right: 4px; width: 28px; height: 28px; transform: rotate(-90deg); opacity: 0.6; }

/* 空位 */
.hc-empty { font-size: 18px; color: #334155; }

/* ========== 趋势图区域（上下排） ========== */
.trend-zone { display: grid; grid-template-rows: 1fr 1fr; gap: 12px; position: relative; z-index: 2; min-height: 360px; }
.tz-card { background: rgba(15,23,42,0.45); border: 0.5px solid rgba(59,130,246,0.12); border-radius: 10px; padding: 12px 16px; backdrop-filter: blur(8px); }
.tz-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.tz-title { font-size: 12px; font-weight: 600; color: #94a3b8; }
.tz-badge { font-size: 9px; font-weight: 700; padding: 1px 6px; border-radius: 4px; letter-spacing: 1px; }
.tz-badge.live { background: rgba(239,68,68,0.15); color: #ef4444; animation: live-blink 2s infinite; }
@keyframes live-blink { 0%,100%{opacity:1} 50%{opacity:.4} }
.tz-legends { display: flex; gap: 10px; }
.tz-l { display: flex; align-items: center; gap: 4px; font-size: 9px; color: #64748b; }
.tz-l i { width: 10px; height: 3px; border-radius: 2px; display: inline-block; }
.tz-chart { height: calc(100% - 30px); min-height: 140px; }

/* ========== 分析面板 ========== */
.panel-analysis { position: relative; z-index: 2; }
.pa-grid { display: grid; grid-template-columns: 1.4fr 1fr 1fr; gap: 12px; }
.pa-card { background: rgba(15,23,42,0.45); border: 0.5px solid rgba(59,130,246,0.12); border-radius: 10px; padding: 14px; backdrop-filter: blur(8px); }
.pa-card.pa-wide { grid-column: span 1; }
.pa-col { display: flex; flex-direction: column; gap: 12px; }
.pa-header { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #e2e8f0; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 0.5px solid rgba(59,130,246,0.12); }
.pa-icon { font-size: 14px; }
.pa-count { font-size: 11px; font-weight: 600; color: #3b82f6; background: rgba(59,130,246,0.12); padding: 1px 8px; border-radius: 10px; margin-left: auto; }

/* 表格 */
.pa-table { width: 100%; border-collapse: collapse; font-size: 11px; }
.pa-table th { text-align: left; padding: 7px 8px; color: #475569; font-weight: 500; font-size: 11px; border-bottom: 0.5px solid rgba(59,130,246,0.12); text-transform: uppercase; letter-spacing: 0.5px; }
.pa-table td { padding: 8px; border-bottom: 0.5px solid rgba(30,41,59,0.6); color: #94a3b8; vertical-align: middle; }
.pa-table tr:hover td { background: rgba(59,130,246,0.04); }
.row-danger td { background: rgba(239,68,68,0.05); }
.row-warn td { background: rgba(245,158,11,0.04); }
.td-name { display: flex; align-items: center; gap: 6px; color: #e2e8f0; font-weight: 500; }
.td-type-icon { width: 22px; height: 22px; border-radius: 5px; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; flex-shrink: 0; }
.td-type-icon.type-fan { background: rgba(59,130,246,0.15); }
.td-type-icon.type-mill { background: rgba(245,158,11,0.15); }
.td-type-icon.type-pump { background: rgba(6,182,212,0.15); }
.text-danger { color: #ef4444 !important; }
.text-warn { color: #fbbf24 !important; }
.health-badge { display: inline-block; padding: 1px 8px; border-radius: 10px; font-size: 11px; font-weight: 600; font-family: 'DIN Alternate', monospace; }
.health-badge.ok { background: rgba(34,197,94,0.12); color: #22c55e; }
.health-badge.warn { background: rgba(245,158,11,0.12); color: #fbbf24; }
.health-badge.danger { background: rgba(239,68,68,0.12); color: #ef4444; }
.td-suggest { color: #64748b; font-size: 11px; }
.empty-row { text-align: center; color: #475569; padding: 20px !important; }

/* AI 洞察卡片 */
.ai-insight { display: flex; flex-direction: column; gap: 8px; max-height: 320px; overflow-y: auto; }
.ai-insight::-webkit-scrollbar { width: 3px; }
.ai-insight::-webkit-scrollbar-thumb { background: rgba(59,130,246,0.2); border-radius: 3px; }
.ai-card { display: flex; gap: 10px; padding: 10px 12px; border-radius: 8px; border-left: 3px solid; transition: all 0.2s; }
.ai-card:hover { transform: translateX(2px); }
.ai-card.danger { background: rgba(239,68,68,0.05); border-color: #ef4444; }
.ai-card.warn { background: rgba(245,158,11,0.05); border-color: #f59e0b; }
.ai-card.info { background: rgba(59,130,246,0.05); border-color: #3b82f6; }
.ai-card.ok { background: rgba(34,197,94,0.05); border-color: #22c55e; }
.ai-left { flex-shrink: 0; }
.ai-icon-ring { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 15px; }
.ai-icon-ring.danger { background: rgba(239,68,68,0.12); }
.ai-icon-ring.warn { background: rgba(245,158,11,0.12); }
.ai-icon-ring.info { background: rgba(59,130,246,0.12); }
.ai-icon-ring.ok { background: rgba(34,197,94,0.12); }
.ai-right { flex: 1; min-width: 0; }
.ai-title { font-size: 12px; font-weight: 600; color: #e2e8f0; margin-bottom: 3px; }
.ai-content { font-size: 11px; color: #94a3b8; line-height: 1.55; }

/* 小图表 */
.pa-chart-sm { height: 160px; }

/* 最近告警时间线 */
.alarm-timeline { max-height: 340px; overflow-y: auto; }
.alarm-timeline::-webkit-scrollbar { width: 3px; }
.alarm-timeline::-webkit-scrollbar-thumb { background: rgba(59,130,246,0.2); border-radius: 3px; }
.at-item { display: flex; gap: 10px; padding: 8px 0; position: relative; transition: background 0.15s; border-radius: 6px; }
.at-item:hover { background: rgba(59,130,246,0.03); }
.at-dot-wrap { display: flex; flex-direction: column; align-items: center; flex-shrink: 0; width: 16px; }
.at-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; position: relative; z-index: 1; }
.at-dot.l1 { background: #ef4444; box-shadow: 0 0 6px rgba(239,68,68,0.5); }
.at-dot.l2 { background: #f59e0b; box-shadow: 0 0 6px rgba(245,158,11,0.5); }
.at-dot.l3 { background: #06b6d4; box-shadow: 0 0 6px rgba(6,182,212,0.5); }
.at-line { width: 1px; flex: 1; background: rgba(51,65,85,0.5); margin-top: 2px; }
.at-body { flex: 1; min-width: 0; }
.at-head { display: flex; align-items: center; gap: 6px; margin-bottom: 3px; flex-wrap: wrap; }
.at-dev { font-size: 11px; font-weight: 600; color: #e2e8f0; }
.at-time { font-size: 11px; color: #475569; font-family: monospace; }
.at-tag { font-size: 9px; padding: 1px 6px; border-radius: 3px; font-weight: 500; }
.at-tag.st-unhandled { background: rgba(239,68,68,0.12); color: #ef4444; }
.at-tag.st-confirmed { background: rgba(245,158,11,0.12); color: #fbbf24; }
.at-tag.st-resolved { background: rgba(34,197,94,0.12); color: #22c55e; }
.at-desc { font-size: 11px; color: #cbd5e1; margin-bottom: 3px; line-height: 1.4; }
.at-meta { display: flex; gap: 10px; font-size: 9px; color: #475569; }
.at-point { color: #94a3b8; }
.at-dept { color: #64748b; }
.at-empty { text-align: center; color: #475569; padding: 30px 0; font-size: 12px; }

/* ========== 响应式适配 ========== */
@media (max-width: 1400px) {
  .pa-grid { grid-template-columns: 1fr 1fr; }
  .pa-card.pa-wide { grid-column: span 2; }
}
@media (max-width: 1000px) {
  .kpi-bar { grid-template-columns: repeat(3, 1fr); }
  .pa-grid { grid-template-columns: 1fr; }
  .pa-card.pa-wide { grid-column: span 1; }
}
</style>
