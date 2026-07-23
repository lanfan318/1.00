<template>
<div class="ca-page">
  <!-- 顶部：设备选择 + 标题 -->
  <div class="ca-head">
    <div class="ca-head-l">
      <span class="ca-bar"></span>
      <h2>设备工况分析</h2>
      <span class="ca-sub">实时运行参数 · 健康度评估 · AI 工况诊断</span>
    </div>
    <el-select v-model="selectedId" style="width:300px" filterable>
      <el-option v-for="d in devs" :key="d.id" :value="d.id" :label="d.name + '（' + uName(d.unit) + ' · ' + d.dept + '）健康度' + d.health.toFixed(1)" />
    </el-select>
  </div>

  <!-- KPI 指标条 -->
  <div class="ca-kpis">
    <div class="ca-kpi" :style="{'--kac': cur.health>=90?'#34d399':cur.health>=80?'#fbbf24':'#ef4444'}">
      <div class="ca-kpi-ic">◈</div>
      <div class="ca-kpi-body">
        <div class="ca-kpi-l">设备健康度</div>
        <div class="ca-kpi-v">{{ cur.health.toFixed(1) }}<span class="ca-kpi-u">/100</span></div>
      </div>
      <div class="ca-kpi-ring" :style="{background: 'conic-gradient(var(--kac) ' + (cur.health*3.6) + 'deg, rgba(62,170,255,0.08) 0deg)'}"></div>
    </div>
    <div class="ca-kpi" style="--kac:#3eaaff">
      <div class="ca-kpi-ic">⚡</div>
      <div class="ca-kpi-body">
        <div class="ca-kpi-l">出力 / 负荷</div>
        <div class="ca-kpi-v" style="color:#3eaaff">{{ cur.output || '-' }}</div>
      </div>
    </div>
    <div class="ca-kpi" :style="{'--kac': cur.health>=80?'#34d399':'#fbbf24'}">
      <div class="ca-kpi-ic">◉</div>
      <div class="ca-kpi-body">
        <div class="ca-kpi-l">运行状态</div>
        <div class="ca-kpi-v" style="font-size:18px" :style="{color:cur.health>=80?'#34d399':'#fbbf24'}">{{ cur.health>=80?'正常运行':'告警处置' }}</div>
      </div>
    </div>
    <div class="ca-kpi" style="--kac:#22d3ee">
      <div class="ca-kpi-ic">⬢</div>
      <div class="ca-kpi-body">
        <div class="ca-kpi-l">所属机组 / 专业</div>
        <div class="ca-kpi-v" style="color:#22d3ee;font-size:18px">{{ unitName }}</div>
        <div style="color:#9fb6cf;font-size:11px;margin-top:2px">{{ cur.dept }}</div>
      </div>
    </div>
  </div>

  <!-- 图表区 -->
  <div class="ca-charts">
    <div class="cd ca-card">
      <div class="cd-t"><span class="ut-ic">▸</span>关键参数趋势</div>
      <div ref="ct" class="ca-chart"></div>
    </div>
    <div class="cd ca-card">
      <div class="cd-t"><span class="ut-ic">▸</span>运行效率仪表盘</div>
      <div ref="cg" class="ca-chart"></div>
    </div>
  </div>

  <!-- 测点实时值 -->
  <div class="cd ca-meas">
    <div class="cd-t"><span class="ut-ic">▸</span>测点实时值</div>
    <div class="ca-meas-grid">
      <div v-for="(v, k) in cur.params" :key="k" class="ca-meas-i" :class="{over: v[0] >= v[1]}">
        <div class="ca-mi-top">
          <span class="ca-mi-name">{{ k }}</span>
          <span class="ca-mi-dot" :style="{background: v[0] >= v[1] ? '#ef4444' : '#34d399'}"></span>
        </div>
        <div class="ca-mi-val" :style="{color: v[0] >= v[1] ? '#ef4444' : '#e2e8f0'}">{{ v[0] }}<span class="ca-mi-u">{{ v[2] }}</span></div>
        <div class="ca-mi-bar"><span :style="{width: Math.min(100, (v[0]/v[1])*100) + '%', background: v[0] >= v[1] ? 'linear-gradient(90deg,#f87171,#ef4444)' : 'linear-gradient(90deg,#3eaaff,#22d3ee)'}"></span></div>
        <div class="ca-mi-lim">阈值 {{ v[1] }} {{ v[2] }}</div>
      </div>
    </div>
  </div>

  <!-- AI 工况分析 -->
  <div class="cd ca-ai">
    <div class="cd-t"><span class="ut-ic">▸</span>AI 工况分析</div>
    <div class="ai">
      <p><span class="tg tg-i">健康度</span> 当前设备健康度为 <strong :style="{color:cur.health>=90?'#34d399':cur.health>=80?'#fbbf24':'#ef4444'}">{{ cur.health.toFixed(1) }}</strong>，{{ cur.health >= 90 ? '运行状态良好，无需特别处理' : cur.health >= 80 ? '需关注，建议加强巡检' : '需立即安排检修' }}。</p>
      <p v-for="ins in aiInsights" :key="ins.key"><span class="tg" :class="ins.cls">{{ ins.key }}</span> {{ ins.text }}</p>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const ct = ref(null), cg = ref(null)
let tCh, gCh

const devs = computed(() => store.unitDevices(store.selectedUnitId))
const selectedId = ref(store.devices[0]?.id || '')
watch(() => store.devices, () => {
  if (!selectedId.value && store.devices[0]) selectedId.value = store.devices[0].id
}, { deep: true })

const cur = computed(() => store.devices.find(d => d.id === selectedId.value) || store.devices[0] || { health: 0, params: {}, name: '无设备', dept: '-', unit: '-' })
const unitName = computed(() => store.units.find(u => u.id === cur.value?.unit)?.name || '-')
const uName = (uid) => store.units.find(u => u.id === uid)?.name || uid

const aiInsights = computed(() => {
  const d = cur.value
  if (!d) return []
  const out = []
  for (const [k, v] of Object.entries(d.params || {})) {
    if (v[0] >= v[1]) {
      out.push({ key: k, cls: 'tg-w', text: `当前 ${v[0]}${v[2]} 已超阈值 ${v[1]}${v[2]}，建议立即处置。` })
    } else if (v[0] > v[1] * 0.9) {
      out.push({ key: k, cls: 'tg-i', text: `当前 ${v[0]}${v[2]} 接近阈值 (90% 警戒线)，持续关注。` })
    }
  }
  if (out.length === 0) out.push({ key: '综合', cls: 'tg-ok', text: '各测点运行平稳，状态良好。' })
  return out
})

// 3D 风格折线工厂
const lineOpt = (name, color, data) => ({
  name, type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
  showSymbol: true,
  lineStyle: {
    width: 2.5, color,
    shadowBlur: 14, shadowColor: color, shadowOffsetY: 6
  },
  itemStyle: { color, borderColor: '#061224', borderWidth: 1.5, shadowBlur: 8, shadowColor: color },
  emphasis: { focus: 'series' },
  areaStyle: {
    opacity: 0.9,
    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: hexA(color, 0.40) },
      { offset: 1, color: hexA(color, 0.02) }
    ])
  },
  data
})

// 颜色转 rgba 工具
function hexA(c, a) {
  if (c.startsWith('#')) {
    const n = parseInt(c.slice(1), 16)
    const r = (n >> 16) & 255, g = (n >> 8) & 255, b = n & 255
    return `rgba(${r},${g},${b},${a})`
  }
  return c
}

const initCharts = () => {
  if (tCh) tCh.dispose(); if (gCh) gCh.dispose()
  const mk = (color, base, amp) => Array.from({ length: 20 }, (_, i) => +(base + Math.sin(i / 2) * amp + (Math.random() - 0.5) * amp * 0.4).toFixed(1))

  const tData = mk('#fbbf24', cur.value?.params?.温度?.[0] || 60, 4)
  const vData = mk('#3eaaff', cur.value?.params?.振动?.[0] || 3, 1.2)

  tCh = echarts.init(ct.value)
  tCh.setOption({
    backgroundColor: 'transparent',
    grid: { left: 46, right: 18, top: 36, bottom: 28 },
    legend: { textStyle: { color: '#9fb6cf', fontSize: 12 }, top: 0, right: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 4 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(8,20,40,0.92)', borderColor: 'rgba(62,170,255,0.3)', textStyle: { color: '#c8e4ff' } },
    xAxis: {
      type: 'category', boundaryGap: false,
      data: Array.from({ length: 20 }, (_, i) => i * 3 + 's'),
      axisLabel: { color: '#9fb6cf', fontSize: 12 },
      axisLine: { lineStyle: { color: 'rgba(62,170,255,0.12)' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#9fb6cf' },
      splitLine: { lineStyle: { color: 'rgba(62,170,255,0.08)' } },
      axisLine: { show: false }, axisTick: { show: false }
    },
    series: [
      lineOpt('温度', '#fbbf24', tData),
      lineOpt('振动', '#3eaaff', vData)
    ]
  })

  const eff = cur.value?.health >= 80 ? 80 + (cur.value.health - 80) * 0.5 : cur.value?.health * 0.8
  const effRounded = parseFloat(eff.toFixed(1))
  const effColor = effRounded >= 90 ? '#34d399' : effRounded >= 80 ? '#fbbf24' : '#ef4444'
  gCh = echarts.init(cg.value)
  gCh.setOption({
    backgroundColor: 'transparent',
    series: [{
      type: 'gauge', min: 0, max: 100, radius: '82%', center: ['50%', '56%'],
      startAngle: 220, endAngle: -40,
      progress: { show: true, width: 14, roundCap: true, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: hexA(effColor, 0.5) }, { offset: 1, color: effColor }]), shadowBlur: 16, shadowColor: effColor } },
      axisLine: { lineStyle: { width: 14, color: [[0.6, 'rgba(239,68,68,0.25)'], [0.85, 'rgba(245,158,11,0.25)'], [1, 'rgba(52,211,153,0.25)']] } },
      axisTick: { distance: -18, length: 4, lineStyle: { color: 'rgba(62,170,255,0.3)' } },
      splitLine: { distance: -20, length: 10, lineStyle: { color: 'rgba(62,170,255,0.4)' } },
      axisLabel: { distance: -2, color: '#9fb6cf', fontSize: 11 },
      pointer: { show: true, length: '52%', width: 3.5, itemStyle: { color: effColor, shadowBlur: 8, shadowColor: effColor } },
      anchor: { show: true, size: 12, itemStyle: { color: effColor, borderColor: '#061224', borderWidth: 2 } },
      title: { color: '#9fb6cf', fontSize: 12, offsetCenter: [0, '82%'] },
      detail: { valueAnimation: true, fontSize: 34, fontFamily: '"Orbitron","SF Mono","Consolas",monospace', color: effColor, offsetCenter: [0, '38%'], formatter: '{value}%', textShadowColor: effColor, textShadowBlur: 16 },
      data: [{ value: effRounded, name: '运行效率' }]
    }]
  })
}

watch(selectedId, () => nextTick(initCharts))
watch(() => cur.value?.id, () => nextTick(initCharts))
onMounted(() => nextTick(initCharts))
onUnmounted(() => { tCh?.dispose(); gCh?.dispose() })
</script>

<style scoped>
.ca-page { display: flex; flex-direction: column; gap: 14px; min-height: 100%; }
.ca-head { display: flex; justify-content: space-between; align-items: center; }
.ca-head-l { display: flex; align-items: center; gap: 12px; }
.ca-bar { width: 4px; height: 22px; background: linear-gradient(180deg, #3eaaff, #22d3ee); border-radius: 2px; box-shadow: 0 0 10px rgba(62,170,255,0.5); }
.ca-head h2 { font-size: 17px; font-weight: 600; color: #d4ecff; letter-spacing: 0.5px; }
.ca-sub { font-size: 11px; color: #8fb0cf; }

/* KPI */
.ca-kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.ca-kpi {
  position: relative; display: flex; align-items: center; gap: 14px;
  padding: 16px 18px; overflow: hidden;
  background: linear-gradient(135deg, rgba(12,30,52,0.65) 0%, rgba(8,20,38,0.7) 100%);
  border: 1px solid rgba(62,170,255,0.15); border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.3), 0 4px 16px rgba(0,10,30,0.4), inset 0 1px 0 rgba(62,170,255,0.06);
}
.ca-kpi::before { content:''; position:absolute; left:0; top:0; bottom:0; width:3px; background: var(--kac); box-shadow: 0 0 12px var(--kac); }
.ca-kpi-ic { font-size: 22px; color: var(--kac); filter: drop-shadow(0 0 6px var(--kac)); width: 30px; text-align: center; }
.ca-kpi-body { flex: 1; min-width: 0; }
.ca-kpi-l { font-size: 12px; color: #9fb6cf; letter-spacing: 0.5px; font-weight: 500; }
.ca-kpi-v { font-size: 32px; font-weight: 800; font-family: "SF Mono","Consolas","Orbitron",monospace; line-height: 1.15; color: #fff; text-shadow: 0 0 12px rgba(62,170,255,0.35), 0 2px 4px rgba(0,0,0,0.4); }
.ca-kpi-u { font-size: 12px; color: #8fb0cf; margin-left: 2px; font-weight: 400; }
.ca-kpi-ring { width: 34px; height: 34px; border-radius: 50%; -webkit-mask: radial-gradient(transparent 52%, #000 53%); mask: radial-gradient(transparent 52%, #000 53%); flex-shrink: 0; }

/* 图表 */
.ca-charts { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.ca-card { height: 280px; display: flex; flex-direction: column; }
.ca-card :deep(.cd-t) { flex-shrink: 0; }
.ca-chart { flex: 1; min-height: 0; }
.ut-ic { color: #3eaaff; font-weight: 700; margin-right: 2px; }

/* 测点 */
.ca-meas-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; }
.ca-meas-i { background: linear-gradient(180deg, rgba(10,24,42,0.55), rgba(6,16,30,0.6)); border: 1px solid rgba(62,170,255,0.13); border-radius: 6px; padding: 10px 12px; transition: 0.2s; }
.ca-meas-i:hover { border-color: rgba(62,170,255,0.3); transform: translateY(-1px); }
.ca-meas-i.over { border-color: rgba(239,68,68,0.35); }
.ca-mi-top { display: flex; justify-content: space-between; align-items: center; }
.ca-mi-name { font-size: 13px; color: #d4ecff; font-weight: 600; }
.ca-mi-dot { width: 7px; height: 7px; border-radius: 50%; box-shadow: 0 0 6px currentColor; }
.ca-mi-val { font-size: 26px; font-weight: 800; font-family: "SF Mono","Consolas","Orbitron",monospace; margin: 4px 0 6px; text-shadow: 0 0 8px currentColor; }
.ca-mi-u { font-size: 12px; color: #9fb6cf; margin-left: 2px; font-weight: 400; }
.ca-mi-bar { height: 5px; border-radius: 3px; background: rgba(62,170,255,0.12); overflow: hidden; }
.ca-mi-bar span { display: block; height: 100%; border-radius: 3px; transition: width 0.6s ease; box-shadow: 0 0 6px currentColor; }
.ca-mi-lim { font-size: 11px; color: #9fb6cf; margin-top: 4px; }

/* AI */
.ai p { line-height: 1.8; font-size: 13px; color: #cbd5e1; margin-bottom: 6px; }
.tg { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; margin-right: 6px; }
.tg-i { background: rgba(59, 130, 246, 0.12); color: #3eaaff; }
.tg-w { background: rgba(245, 158, 11, 0.12); color: #fbbf24; }
.tg-ok { background: rgba(34, 197, 94, 0.12); color: #34d399; }
</style>
