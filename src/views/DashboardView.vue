<template>
<div>
  <!-- 机组选择板块（卡片式，可点击切换） -->
  <div class="unit-panel">
    <div class="up-header">
      <div class="up-title">⚡ 机组选择</div>
      <div class="up-hint">点击切换不同机组查看监控数据</div>
    </div>
    <div class="up-cards">
      <div v-for="u in store.units" :key="u.id" class="up-card" :class="{on: store.selectedUnitId===u.id}" @click="switchUnit(u.id)">
        <div class="up-c1">
          <div class="up-name">{{ u.name }}</div>
          <div class="up-type">{{ u.type }} · {{ u.capacity }}MW</div>
          <span v-if="store.selectedUnitId===u.id" class="up-badge">✓ 当前选中</span>
        </div>
        <div class="up-c2">
          <div class="up-metric">
            <span class="m-label">设备</span>
            <strong class="m-value">{{ store.unitDevices(u.id).length }}</strong>
          </div>
          <div class="up-metric">
            <span class="m-label">报警</span>
            <strong class="m-value danger">{{ store.unitAlarms(u.id).length }}</strong>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 专业切换：锅炉 / 汽轮 / 全部 -->
  <div class="dept-switch">
    <span class="dept-lbl">监控专业：</span>
    <el-radio-group v-model="deptMode" size="small">
      <el-radio-button value="all">全部</el-radio-button>
      <el-radio-button value="锅炉">🔥 锅炉</el-radio-button>
      <el-radio-button value="汽轮机">⚙ 汽轮机</el-radio-button>
    </el-radio-group>
    <span class="dept-hint">点击切换查看不同专业的设备</span>
  </div>

  <!-- 机组实时概览 + 排放指标 -->
  <el-row :gutter="14" style="margin-bottom:14px">
    <el-col :span="12"><div class="cd">
      <div class="cd-h"><span class="cd-t"><span class="sd ok"></span>{{ store.selectedUnit.name }} 机组实时概览</span>
        <span class="tg" :class="overviewStatus==='需关注'?'tg-w':'tg-ok'">{{ overviewStatus }}</span>
      </div>
      <el-row :gutter="10">
        <el-col :span="6"><div class="mc"><div class="ml">负荷</div><div class="mv in" id="m-load">-</div><div class="ms">MW</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">主汽压力</div><div class="mv ok" id="m-press">-</div><div class="ms">MPa</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">主汽温度</div><div class="mv ok" id="m-temp">-</div><div class="ms">℃</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">煤耗</div><div class="mv in" id="m-coal">-</div><div class="ms">g/kWh</div></div></el-col>
      </el-row>
    </div></el-col>
    <el-col :span="12"><div class="cd">
      <div class="cd-h"><span class="cd-t">环保排放指标</span><span class="tg tg-ok">达标</span></div>
      <el-row :gutter="10">
        <el-col :span="6"><div class="mc"><div class="ml">NOx</div><div class="mv ok" id="m-nox">-</div><div class="ms">mg/m³</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">SO2</div><div class="mv ok" id="m-so2">-</div><div class="ms">mg/m³</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">粉尘</div><div class="mv ok" id="m-dust">-</div><div class="ms">mg/m³</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">达标率</div><div class="mv in" id="m-em">-</div><div class="ms">%</div></div></el-col>
      </el-row>
    </div></el-col>
  </el-row>

  <!-- 设备健康度 -->
  <el-row :gutter="14" style="margin-bottom:14px">
    <el-col v-if="showBoiler" :span="showTurbine?12:24"><div class="cd"><div class="cd-t"><span class="sd ok"></span>锅炉设备健康度（{{ store.selectedUnit.name }}）</div>
      <div v-for="d in boiler" :key="d.id" class="hb-row">
        <span class="hb-n">{{ d.name }}</span>
        <div class="hb-b"><div class="hb-f" :class="cls(d.health)" :style="{width:d.health+'%'}"></div></div>
        <span class="hb-v" :style="{color:col(d.health)}">{{ d.health.toFixed(1) }}</span>
      </div>
    </div></el-col>
    <el-col v-if="showTurbine" :span="showBoiler?12:24"><div class="cd"><div class="cd-t"><span class="sd ok"></span>汽轮机设备健康度（{{ store.selectedUnit.name }}）</div>
      <div v-for="d in turbine" :key="d.id" class="hb-row">
        <span class="hb-n">{{ d.name }}</span>
        <div class="hb-b"><div class="hb-f" :class="cls(d.health)" :style="{width:d.health+'%'}"></div></div>
        <span class="hb-v" :style="{color:col(d.health)}">{{ d.health.toFixed(1) }}</span>
      </div>
    </div></el-col>
  </el-row>

  <!-- 三个趋势图 -->
  <el-row :gutter="14" style="margin-bottom:14px">
    <el-col :span="8"><div class="cd"><div class="cd-t">负荷趋势</div><div ref="ch1" style="height:200px"></div></div></el-col>
    <el-col :span="8"><div class="cd"><div class="cd-t">主汽温度趋势</div><div ref="ch2" style="height:200px"></div></div></el-col>
    <el-col :span="8"><div class="cd"><div class="cd-t">自动/联锁投入率</div><div ref="ch3" style="height:200px"></div></div></el-col>
  </el-row>

  <!-- AI 洞察 -->
  <div class="insight">
    <div class="in-t">AI 实时运行洞察 · {{ store.selectedUnit.name }}</div>
    <div class="in-b" id="dash-insight">分析中...</div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()

const deptMode = ref('all')
const boiler = computed(() => {
  const arr = store.unitDevices(store.selectedUnitId).filter(d => d.dept === '锅炉')
  return deptMode.value === '汽轮机' ? [] : arr
})
const turbine = computed(() => {
  const arr = store.unitDevices(store.selectedUnitId).filter(d => d.dept === '汽轮机')
  return deptMode.value === '锅炉' ? [] : arr
})
const showBoiler = computed(() => deptMode.value !== '汽轮机')
const showTurbine = computed(() => deptMode.value !== '锅炉')

const overviewStatus = computed(() => {
  const warn = store.unitDevices(store.selectedUnitId).filter(d => d.health < 85).length
  return warn > 0 ? '需关注' : '运行正常'
})

const cls = (h) => h >= 90 ? 'ok' : h >= 80 ? 'wn' : 'dg'
const col = (h) => h >= 90 ? '#22c55e' : h >= 80 ? '#f59e0b' : '#ef4444'

const ch1 = ref(null), ch2 = ref(null), ch3 = ref(null)
let c1, c2, c3, iv

const tsArr = Array.from({ length: 30 }, (_, i) => {
  const d = new Date(); d.setMinutes(d.getMinutes() - 29 + i); return d.toTimeString().slice(0, 5)
})

const initCharts = () => {
  if (c1) c1.dispose(); if (c2) c2.dispose(); if (c3) c3.dispose()
  c1 = echarts.init(ch1.value)
  c1.setOption({
    grid: { left: 36, right: 14, top: 12, bottom: 22 },
    xAxis: { type: 'category', data: tsArr, axisLabel: { color: '#64748b', fontSize: 8, interval: 5 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'value', name: 'MW', axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [{ type: 'line', smooth: true, data: Array.from({ length: 30 }, () => store.selectedUnit.base.load + Math.random() * 20 - 10), lineStyle: { color: '#3b82f6', width: 1.5 }, areaStyle: { color: 'rgba(59,130,246,0.08)' }, symbol: 'none' }]
  })
  c2 = echarts.init(ch2.value)
  c2.setOption({
    grid: { left: 36, right: 14, top: 12, bottom: 22 },
    xAxis: { type: 'category', data: tsArr, axisLabel: { color: '#64748b', fontSize: 8, interval: 5 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'value', name: '℃', axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [
      { type: 'line', smooth: true, data: Array.from({ length: 30 }, () => store.selectedUnit.base.temp + Math.random() * 8), lineStyle: { color: '#f59e0b', width: 1.5 }, areaStyle: { color: 'rgba(245,158,11,0.06)' }, symbol: 'none' },
      { type: 'line', data: Array(30).fill(545), lineStyle: { color: '#ef4444', width: 1, type: 'dashed' }, symbol: 'none' }
    ]
  })
  c3 = echarts.init(ch3.value)
  c3.setOption({
    grid: { left: 36, right: 14, top: 12, bottom: 22 },
    xAxis: { type: 'category', data: tsArr, axisLabel: { color: '#64748b', fontSize: 8, interval: 5 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'value', name: '%', min: 95, axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [
      { type: 'line', smooth: true, data: Array.from({ length: 30 }, () => 98.5 + Math.random()), lineStyle: { color: '#22c55e', width: 1.5 }, symbol: 'none' },
      { type: 'line', smooth: true, data: Array.from({ length: 30 }, () => 97.2 + Math.random()), lineStyle: { color: '#06b6d4', width: 1.5 }, symbol: 'none' }
    ]
  })
}

const updateMetrics = () => {
  const u = store.selectedUnit
  document.getElementById('m-load').textContent = (u.base.load + Math.random() * 30 - 15).toFixed(1)
  document.getElementById('m-press').textContent = u.base.press.toFixed(2)
  document.getElementById('m-temp').textContent = (u.base.temp + Math.random() * 8).toFixed(1)
  document.getElementById('m-coal').textContent = (u.base.coal + Math.random() * 8).toFixed(1)
  document.getElementById('m-nox').textContent = (u.base.nox + Math.random() * 15).toFixed(1)
  document.getElementById('m-so2').textContent = (u.base.so2 + Math.random() * 5).toFixed(1)
  document.getElementById('m-dust').textContent = (u.base.dust + Math.random() * 3).toFixed(1)
  document.getElementById('m-em').textContent = (99 + Math.random()).toFixed(1)
  updateInsight()
}

const updateInsight = () => {
  const devs = store.unitDevices(store.selectedUnitId)
  const warns = devs.filter(d => d.health < 85)
  const alarms = store.unitAlarms(store.selectedUnitId).filter(a => a.st === 'unhandled')
  let text = ''
  if (warns.length > 0) {
    text += `<span style="color:#f59e0b">设备健康度：</span>${warns.map(d => `${d.name} ${d.health.toFixed(1)}`).join('、')} 偏低需关注。`
  } else {
    text += `<span style="color:#22c55e">设备健康度：</span>所有设备均在 85 以上，状态良好。`
  }
  if (alarms.length > 0) {
    text += ` <span style="color:#ef444e">未处理报警：</span>${alarms.length} 条，${alarms.filter(a => a.l === 1).length} 条一级需立即响应。`
  } else {
    text += ` <span style="color:#22c55e">报警状态：</span>无未处理。`
  }
  const t = parseFloat(document.getElementById('m-temp').textContent)
  if (t > 545) text += ` <span style="color:#f59e0b">温度预警：</span>主汽温度 ${t}°C 接近上限 545°C，建议调整减温水量。`
  document.getElementById('dash-insight').innerHTML = text
}

const onUnitChange = () => {
  nextTick(() => { initCharts(); updateMetrics() })
}
const switchUnit = (id) => {
  store.selectedUnitId = id
  // 切换机组时自动重置专业视图为"全部"
  deptMode.value = 'all'
  nextTick(() => { initCharts(); updateMetrics() })
}

watch(() => store.selectedUnitId, onUnitChange)
onMounted(() => { nextTick(() => { initCharts(); updateMetrics() }) })
iv = setInterval(updateMetrics, 3000)
onUnmounted(() => { clearInterval(iv); c1?.dispose(); c2?.dispose(); c3?.dispose() })
</script>

<style scoped>
/* 机组选择板块（卡片式） */
.unit-panel { background: linear-gradient(135deg, rgba(59,130,246,0.05), rgba(99,102,241,0.02)); border: 0.5px solid rgba(59,130,246,0.2); border-radius: 12px; padding: 14px 16px; margin-bottom: 14px; }
.up-header { display: flex; align-items: center; margin-bottom: 12px; }
.up-title { font-size: 14px; font-weight: 600; color: #3b82f6; }
.up-hint { font-size: 11px; color: #64748b; margin-left: 12px; }
.up-cards { display: flex; gap: 12px; flex-wrap: wrap; }
.up-card { display: flex; align-items: stretch; gap: 0; padding: 0; background: #0a0e17; border: 1.5px solid #1e293b; border-radius: 10px; cursor: pointer; transition: 0.15s; min-width: 360px; position: relative; overflow: hidden; }
.up-card:hover { border-color: #3b82f6; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(59,130,246,0.2); }
.up-card.on { border-color: #3b82f6; box-shadow: 0 0 16px rgba(59,130,246,0.3); }
.up-card.on::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: #3b82f6; }
.up-c1 { flex: 1; padding: 14px 16px; min-width: 160px; }
.up-name { font-size: 15px; font-weight: 600; color: #e2e8f0; margin-bottom: 4px; }
.up-type { font-size: 11px; color: #94a3b8; margin-bottom: 6px; }
.up-badge { display: inline-block; font-size: 10px; padding: 2px 8px; border-radius: 3px; background: rgba(59,130,246,0.15); color: #3b82f6; }
.up-c2 { display: flex; gap: 0; border-left: 0.5px solid #1e293b; }
.up-metric { display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 70px; padding: 14px 18px; }
.up-metric + .up-metric { border-left: 0.5px solid #1e293b; }
.up-metric .m-label { font-size: 10px; color: #64748b; margin-bottom: 6px; line-height: 1; white-space: nowrap; }
.up-metric .m-value { font-size: 22px; color: #3b82f6; font-weight: 600; line-height: 1; }
.up-metric .m-value.danger { color: #ef4444; }
.dept-switch { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; padding: 8px 14px; background: #0a0e17; border-radius: 8px; }
.dept-lbl { font-size: 12px; color: #94a3b8; }
.dept-hint { font-size: 11px; color: #64748b; margin-left: auto; }
:deep(.dept-switch .el-radio-button__inner) { background: #111827; border-color: #1e293b; color: #94a3b8; }
:deep(.dept-switch .el-radio-button.is-active .el-radio-button__inner) { background: rgba(59,130,246,0.15); border-color: #3b82f6; color: #3b82f6; box-shadow: none; }
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-h { display: flex; justify-content: space-between; margin-bottom: 12px; }
.cd-t { font-size: 13px; font-weight: 500; }
.sd { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 6px; }
.sd.ok { background: #22c55e; }
.tg { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; }
.tg-ok { background: rgba(34,197,94,0.12); color: #22c55e; }
.tg-w { background: rgba(245,158,11,0.12); color: #f59e0b; }
.mc { background: #0a0e17; border-radius: 6px; padding: 14px; text-align: center; }
.ml { font-size: 11px; color: #94a3b8; margin-bottom: 4px; }
.mv { font-size: 24px; font-weight: 600; }
.ms { font-size: 10px; color: #64748b; margin-top: 2px; }
.hb-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; font-size: 12px; }
.hb-n { width: 90px; color: #94a3b8; }
.hb-b { flex: 1; height: 5px; background: #1e293b; border-radius: 3px; overflow: hidden; }
.hb-f { height: 100%; border-radius: 3px; transition: width 0.6s; }
.hb-f.ok { background: #22c55e; } .hb-f.wn { background: #f59e0b; } .hb-f.dg { background: #ef4444; }
.hb-v { width: 36px; text-align: right; font-weight: 500; }
.insight { background: #0a0e17; border: 0.5px solid #1e293b; border-radius: 10px; padding: 14px; }
.in-t { font-size: 12px; font-weight: 500; color: #3b82f6; margin-bottom: 6px; }
.in-b { font-size: 12px; color: #94a3b8; line-height: 1.7; }
</style>
