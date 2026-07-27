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

  <!-- 趋势分析控制条：自定义时段 + 多参数叠加 + AI 解读 -->
  <div class="ca-ctrls">
    <div class="ca-ctrl">
      <label>时间范围</label>
      <el-radio-group v-model="trRange" size="small">
        <el-radio-button value="1h">近1h</el-radio-button>
        <el-radio-button value="6h">近6h</el-radio-button>
        <el-radio-button value="24h">近24h</el-radio-button>
        <el-radio-button value="7d">近7d</el-radio-button>
        <el-radio-button value="custom">自定义</el-radio-button>
      </el-radio-group>
      <el-date-picker v-if="trRange==='custom'" v-model="trCustom" type="datetimerange" size="small"
        style="width:260px" range-separator="~" start-placeholder="开始" end-placeholder="结束" />
    </div>
    <div class="ca-ctrl" style="flex:1;min-width:240px">
      <label>叠加参数 <span class="ca-ctrl-tip">（可多选对比）</span></label>
      <el-select v-model="selParams" multiple collapse-tags size="small" style="width:100%" placeholder="选择叠加参数">
        <el-option v-for="p in allParams" :key="p" :value="p" :label="p" />
      </el-select>
    </div>
    <el-button type="primary" size="small" class="ca-ai-btn" @click="genAiTrend">
      <el-icon><MagicStick /></el-icon> AI 解读趋势
    </el-button>
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
      <div class="cd-t"><span class="ut-ic">▸</span>关键参数趋势
        <span class="ca-trend-tools">
          <span class="ca-drill-hint" v-if="!drillPoint">点击曲线可钻取明细 ▾</span>
          <el-button size="small" class="ca-ai-trend-btn" @click="genAiTrend"><el-icon><MagicStick /></el-icon>AI 解读</el-button>
        </span>
      </div>
      <div ref="ct" class="ca-chart"></div>
      <!-- 数据钻取明细 -->
      <div v-if="drillPoint" class="ca-drill">
        <div class="ca-drill-h">
          <span class="ca-drill-t">数据钻取 · {{ drillPoint.param }}</span>
          <el-button text size="small" @click="drillPoint=null"><el-icon><Close /></el-icon></el-button>
        </div>
        <div class="ca-drill-b">
          <div class="ca-dr"><span>时刻</span><b>{{ drillPoint.time }}</b></div>
          <div class="ca-dr"><span>数值</span><b :style="{color:drillPoint.over?'#ef4444':'#3eaaff'}">{{ drillPoint.value }} {{ drillPoint.unit }}</b></div>
          <div class="ca-dr"><span>阈值</span><b>{{ drillPoint.limit }} {{ drillPoint.unit }}</b></div>
          <div class="ca-dr ca-dr-alarm" v-if="drillPoint.alarm">
            <span>关联告警</span>
            <b style="color:#f87171">{{ drillPoint.alarm.desc }}</b>
            <el-button link type="primary" size="small" @click="goAlarm">去处置</el-button>
          </div>
          <div class="ca-dr" v-else><span>关联告警</span><b style="color:#34d399">无</b></div>
        </div>
      </div>
      <!-- AI 趋势解读 -->
      <div v-if="aiTrend" class="ca-ai-trend">
        <div class="ca-ai-trend-h"><el-icon><MagicStick /></el-icon> AI 趋势解读
          <el-button text size="small" @click="aiTrend=''"><el-icon><Close /></el-icon></el-button>
        </div>
        <div class="ca-ai-trend-b" v-html="aiTrend"></div>
      </div>
    </div>
    <div class="cd ca-card ca-gauge-card">
      <div class="cd-t"><span class="ut-ic">▸</span>运行效率仪表盘</div>
      <div class="gauge-scene">
        <!-- 左上：告警数徽章 -->
        <div class="gs-badge gs-badge--alarm">
          <span class="gsb-label">告警</span>
          <span class="gsb-val">{{ alarmCount }}</span>
        </div>
        <!-- 左下：关注/异常数徽章 -->
        <div class="gs-badge gs-badge--warn">
          <span class="gsb-label">关注</span>
          <span class="gsb-val">{{ warnCount }}</span>
        </div>
        <!-- 右侧：正常数徽章 -->
        <div class="gs-badge gs-badge--ok">
          <span class="gsb-label">正常</span>
          <span class="gsb-val">{{ okCount }}</span>
        </div>
        <!-- 右上角：单位标识 -->
        <div class="gs-unit">
          <span class="gsu-icon">⏻</span>
          <span class="gsu-text">{{ cur.name || '设备' }}</span>
        </div>
        <!-- 顶部装饰弧线 -->
        <svg class="gs-top-arc" viewBox="0 0 200 20" preserveAspectRatio="none">
          <path d="M0,20 Q100,-10 200,20" fill="none" stroke="rgba(62,170,255,0.15)" stroke-width="1" />
          <path d="M30,16 Q100,2 170,16" fill="none" stroke="rgba(62,170,255,0.25)" stroke-width="0.5" />
        </svg>
        <!-- 底部装饰 -->
        <div class="gs-bottom-bar"></div>
        <!-- ECharts 仪表盘 -->
        <div ref="cg" class="ca-chart"></div>
      </div>
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
import { useRouter } from 'vue-router'

const store = useDataStore()
const router = useRouter()
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

// ============ 趋势分析：时段 + 多参数叠加 + 钻取 + AI ============
const trRange = ref('24h')
const trCustom = ref(null)
// 当前设备全部参数名
const allParams = computed(() => Object.keys(cur.value?.params || {}))
// 默认叠加前 2 个参数
const selParams = ref([])
const syncSelParams = () => {
  const ks = Object.keys(cur.value?.params || {})
  selParams.value = ks.slice(0, 2)
}
watch(cur, () => { syncSelParams() }, { immediate: true })

const drillPoint = ref(null)
const aiTrend = ref('')

// 时段 → 点数 + 时间标签
const rangeMeta = (r) => {
  if (r === '1h') return { n: 60, stepMin: 1, fmt: (i) => `${String(Math.floor(i / 60)).padStart(2,'0')}:${String(i % 60).padStart(2,'0')}` }
  if (r === '6h') return { n: 36, stepMin: 10, fmt: (i) => `${String(Math.floor(i / 6)).padStart(2,'0')}:${String((i % 6) * 10).padStart(2,'0')}` }
  if (r === '24h') return { n: 24, stepMin: 60, fmt: (i) => `${String(i).padStart(2,'0')}:00` }
  if (r === '7d') return { n: 28, stepMin: 360, fmt: (i) => `D${Math.floor(i / 4) + 1} ${String((i % 4) * 6).padStart(2,'0')}:00` }
  return { n: 24, stepMin: 60, fmt: (i) => `${String(i).padStart(2,'0')}:00` }
}

// 仪表盘周围徽章数据
const alarmCount = computed(() => {
  const d = cur.value
  if (!d || !d.params) return 0
  return Object.values(d.params).filter(v => v[0] >= v[1]).length
})
const warnCount = computed(() => {
  const d = cur.value
  if (!d || !d.params) return 0
  return Object.values(d.params).filter(v => v[0] < v[1] && v[0] > v[1] * 0.9).length
})
const okCount = computed(() => {
  const d = cur.value
  if (!d || !d.params) return 0
  return Object.values(d.params).filter(v => v[0] <= v[1] * 0.9).length
})

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
  itemStyle: { color, borderColor: '#000000', borderWidth: 1.5, shadowBlur: 8, shadowColor: color },
  emphasis: { focus: 'series' },
  areaStyle: {
    opacity: 0.25,
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
  const meta = rangeMeta(trRange.value)
  const mk = (base, amp) => Array.from({ length: meta.n }, (_, i) => {
    const t = i / (meta.n - 1)
    return +(base + Math.sin(i / 2.5) * amp + (t > 0.6 ? (t - 0.6) * amp * 1.6 : 0) + (Math.random() - 0.5) * amp * 0.3).toFixed(1)
  })
  const palette = ['#fbbf24', '#3eaaff', '#34d399', '#22d3ee', '#a78bfa', '#f87171']
  const xData = Array.from({ length: meta.n }, (_, i) => meta.fmt(i))

  tCh = echarts.init(ct.value)
  const trendSeries = selParams.value.map((p, idx) => {
    const pv = cur.value?.params?.[p] || [50, 80, '']
    return lineOpt(p, palette[idx % palette.length], mk(pv[0], pv[1] ? pv[1] * 0.06 : 3))
  })
  if (!trendSeries.length) trendSeries.push(lineOpt('无参数', '#5fb3ff', mk(50, 2)))

  tCh.setOption({
    backgroundColor: '#000000',
    grid: { left: 46, right: 18, top: 36, bottom: 28 },
    legend: { textStyle: { color: '#9fb6cf', fontSize: 12 }, top: 0, right: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 4 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(8,20,40,0.92)', borderColor: 'rgba(62,170,255,0.3)', textStyle: { color: '#c8e4ff' } },
    xAxis: {
      type: 'category', boundaryGap: false,
      data: xData,
      axisLabel: { color: '#9fb6cf', fontSize: 12 },
      axisLine: { lineStyle: { color: 'rgba(90,166,196,0.12)' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#9fb6cf' },
      splitLine: { lineStyle: { color: 'rgba(90,166,196,0.10)', type: 'dashed' } },
      axisLine: { show: false }, axisTick: { show: false }
    },
    series: trendSeries
  })

  // 数据钻取：点击曲线点 → 关联告警明细
  tCh.off('click')
  tCh.on('click', (params) => {
    if (!params.value && params.value !== 0) return
    const p = params.seriesName
    const pv = cur.value?.params?.[p]
    const unit = pv?.[2] || ''
    const limit = pv?.[1]
    const over = pv && params.value >= limit
    const alarm = store.unitAlarms(cur.value?.unit).find(a => a.device === cur.value?.name && (a.point === p || a.desc.includes(p)))
    drillPoint.value = {
      param: p, time: params.name, value: params.value, unit, limit,
      over: !!over, alarm: alarm || null
    }
  })

  const eff = cur.value?.health >= 80 ? 80 + (cur.value.health - 80) * 0.5 : cur.value?.health * 0.8
  const effRounded = parseFloat(eff.toFixed(1))
  const effColor = effRounded >= 90 ? '#22c55e' : effRounded >= 80 ? '#f59e0b' : '#ef4444'
  gCh = echarts.init(cg.value)
  gCh.setOption({
    backgroundColor: '#000000',
    series: [
      // —— 最外层装饰环（密集刻度线 + 发光弧） ——
      {
        type: 'gauge', min: 0, max: 100, radius: '100%', center: ['50%', '52%'],
        startAngle: 220, endAngle: -40,
        progress: { show: false },
        axisLine: { lineStyle: { width: 1.5, color: [[1, 'rgba(62,170,255,0.18)']] } },
        axisTick: {
          show: true, distance: -4, length: 6,
          lineStyle: { color: 'rgba(62,170,255,0.28)', width: 1 }
        },
        splitLine: { show: false },
        axisLabel: { show: false }, pointer: { show: false },
        anchor: { show: false }, detail: { show: false }, title: { show: false },
        silent: true
      },
      // —— 次外层细光环 ——
      {
        type: 'gauge', min: 0, max: 100, radius: '93%', center: ['50%', '52%'],
        startAngle: 220, endAngle: -40,
        progress: { show: false },
        axisLine: { lineStyle: { width: 1, color: [[1, 'rgba(62,170,255,0.10)']] } },
        axisTick: { show: false }, splitLine: { show: false },
        axisLabel: { show: false }, pointer: { show: false },
        anchor: { show: false }, detail: { show: false }, title: { show: false },
        silent: true
      },
      // —— 主仪表盘 ——
      {
        type: 'gauge', min: 0, max: 100, radius: '82%', center: ['50%', '52%'],
        startAngle: 220, endAngle: -40,
        // 进度弧：渐变 + 三层发光（参考矩阵边框规格）
        progress: {
          show: true, width: 12, roundCap: true,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: hexA(effColor, 0.6) },
              { offset: 0.5, color: hexA(effColor, 0.85) },
              { offset: 1, color: effColor }
            ]),
            shadowBlur: 20,
            shadowColor: effColor,
            shadowOffsetY: 0
          }
        },
        // 背景轨道：三段色带 + 微妙渐变
        axisLine: {
          lineStyle: {
            width: 12,
            color: [
              [0.6, new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: 'rgba(239,68,68,0.35)' }, { offset: 1, color: 'rgba(239,68,68,0.18)' }])],
              [0.85, new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: 'rgba(245,158,11,0.32)' }, { offset: 1, color: 'rgba(245,158,11,0.16)' }])],
              [1, new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: 'rgba(34,197,94,0.30)' }, { offset: 1, color: 'rgba(34,197,94,0.14)' }])]
            ]
          }
        },
        // 精细刻度：短刻度更密
        axisTick: {
          distance: -16, length: 5, lineStyle: { color: 'rgba(62,170,255,0.35)', width: 1 }
        },
        // 长刻度线：带微发光
        splitLine: {
          distance: -20, length: 12,
          lineStyle: { color: 'rgba(62,170,255,0.45)', width: 1.5, shadowBlur: 4, shadowColor: 'rgba(62,170,255,0.25)' }
        },
        // 数字标签：精致字号
        axisLabel: {
          distance: -4, color: '#8fb8db', fontSize: 10,
          fontFamily: '"SF Mono","Consolas",monospace',
          textShadowBlur: 2, textShadowColor: 'rgba(62,170,255,0.3)'
        },
        // 指针：渐变色 + 发光阴影
        pointer: {
          show: true, length: '55%', width: 4,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#ffffff' },
              { offset: 0.3, color: hexA(effColor, 0.9) },
              { offset: 1, color: effColor }
            ]),
            shadowBlur: 14, shadowColor: effColor,
            shadowOffsetY: 2
          }
        },
        // 锚点：中心圆 + 外发光
        anchor: {
          show: true, size: 10,
          itemStyle: {
            color: effColor,
            borderColor: '#000000',
            borderWidth: 2.5,
            shadowBlur: 12, shadowColor: effColor
          }
        },
        title: {
          color: '#7a9cbe', fontSize: 11, offsetCenter: [0, '82%'],
          fontFamily: '"SF Mono","Consolas",monospace', letterSpacing: '2px'
        },
        // 中心数值：大字 + 强发光（参考设计核心视觉焦点）
        detail: {
          valueAnimation: true,
          fontSize: 38,
          fontWeight: 800,
          fontFamily: '"Orbitron","SF Mono","Consolas",monospace',
          color: effColor,
          offsetCenter: [0, '36%'],
          formatter: '{value}%',
          textShadowColor: effColor,
          textShadowBlur: 24,
          textShadowOffsetY: 2
        },
        data: [{ value: effRounded, name: '运行效率' }]
      }
    ]
  })
}

// AI 趋势解读：结合选中参数 + 时段 + 阈值
const rangeLabel = (r) => ({ '1h': '近1小时', '6h': '近6小时', '24h': '近24小时', '7d': '近7天', custom: '自定义时段' }[r] || r)
const genAiTrend = () => {
  const meta = rangeMeta(trRange.value)
  const rows = selParams.value.map(p => {
    const pv = cur.value?.params?.[p] || [0, 0, '']
    const over = pv[0] >= pv[1]
    const trend = (Math.random() > 0.5 ? '上升' : '平稳')
    return `<div class="ai-mt"><span>${p}</span><strong style="color:${over ? '#ef4444' : '#c8e4ff'}">${pv[0]} ${pv[2]} / 阈值 ${pv[1]}${pv[2]}</strong></div>
<div class="ai-mt"><span>趋势</span><strong style="color:${over ? '#f87171' : '#34d399'}">${over ? '超阈·' + trend : trend}</strong></div>`
  }).join('')
  aiTrend.value = `<p style="margin:0 0 6px;color:#9fb6cf;font-size:12px">基于 <b style="color:#3eaaff">${cur.value.name}</b> 在 <b style="color:#3eaaff">${meta.n} 个采样点（${trRange.value === 'custom' ? '自定义时段' : rangeLabel(trRange.value)}）</b> 的时序分析：</p>${rows}
<p style="margin:8px 0 0;color:#cbd5e1;font-size:12px;line-height:1.7">AI 研判：<b style="color:#3eaaff">${selParams.value.join('、')}</b> 整体${selParams.value.some(p => cur.value.params?.[p]?.[0] >= cur.value.params?.[p]?.[1]) ? '存在超阈参数，建议提前介入并核对关联告警。' : '处于安全区，但需关注末端爬升斜率，建议维持当前巡检频次。'}</p>`
}
const goAlarm = () => router.push({ path: '/alarm-config', query: { device: cur.value?.name } })

watch(selectedId, () => nextTick(initCharts))
watch(() => cur.value?.id, () => nextTick(initCharts))
watch([selParams, trRange], () => { drillPoint.value = null; nextTick(initCharts) })
onMounted(() => nextTick(initCharts))
onUnmounted(() => { tCh?.dispose(); gCh?.dispose() })
</script>

<style scoped>
.ca-page { display: flex; flex-direction: column; gap: 6px; height: 100%; overflow: hidden;
  background-color: #04060a; }
.ca-head { display: flex; justify-content: space-between; align-items: center; flex-shrink: 0; }
.ca-head-l { display: flex; align-items: center; gap: 10px; }
.ca-bar { width: 4px; height: 18px; background: linear-gradient(180deg, #3eaaff, #22d3ee); border-radius: 2px; box-shadow: 0 0 8px rgba(62,170,255,0.5); }
.ca-head h2 { font-size: 15px; font-weight: 600; color: #d4ecff; letter-spacing: 0.5px; }
.ca-sub { font-size: 11px; color: #8fb0cf; }

/* KPI — 紧凑 */
.ca-kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; flex-shrink: 0; }
.ca-kpi {
  position: relative; display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; overflow: hidden;
  background: rgba(4,6,10,0.85);
  border: 1px solid rgba(90,166,196,0.15); border-radius: 5px;
  box-shadow: none;
}
.ca-kpi::before { content:''; position:absolute; left:0; top:0; bottom:0; width:3px; background: var(--kac); box-shadow: 0 0 8px var(--kac); }
.ca-kpi-ic { font-size: 18px; color: var(--kac); filter: drop-shadow(0 0 4px var(--kac)); width: 24px; text-align: center; }
.ca-kpi-body { flex: 1; min-width: 0; }
.ca-kpi-l { font-size: 10px; color: #9fb6cf; letter-spacing: 0.3px; font-weight: 500; }
.ca-kpi-v { font-size: 22px; font-weight: 800; font-family: "SF Mono","Consolas","Orbitron",monospace; line-height: 1.1; color: #fff; text-shadow: 0 0 8px rgba(62,170,255,0.3), 0 2px 4px rgba(0,0,0,0.4); }
.ca-kpi-u { font-size: 10px; color: #8fb0cf; margin-left: 2px; font-weight: 400; }
.ca-kpi-ring { width: 28px; height: 28px; border-radius: 50%; -webkit-mask: radial-gradient(transparent 52%, #000 53%); mask: radial-gradient(transparent 52%, #000 53%); flex-shrink: 0; }

/* 图表 — 去掉固定高度，让 flex 自然分配 */
.ca-charts { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; flex: 1; min-height: 0; }
.ca-card {
  display: flex;
  flex-direction: column;
  position: relative;
  min-height: 0;
  background: rgba(4,6,10,0.9);
  border: 1px solid rgba(90,166,196,0.12);
}
/* 仪表盘卡片特别增强 */
.ca-gauge-card { overflow: hidden; }
.ca-gauge-card::after {
  content: '';
  position: absolute;
  width: 14px; height: 14px;
  top: 6px; right: 6px;
  border-top: 2px solid rgba(90,166,196,0.35);
  border-right: 2px solid rgba(90,166,196,0.2);
  pointer-events: none;
}

/* ====== 仪表盘场景：表盘 + 徽章 + 装饰 ====== */
.gauge-scene {
  position: relative;
  flex: 1; min-height: 0;
  width: 100%;
}
/* 仪表盘场景内的 ECharts 容器：填满可用空间 */
.gauge-scene > .ca-chart {
  position: absolute;
  inset: 0;
  width: 100% !important;
  height: 100% !important;
}

/* 状态徽章 */
.gs-badge {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 48px; height: 48px;
  border-radius: 10px;
  clip-path: polygon(30% 0%, 70% 0%, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0% 70%, 0% 30%);
  z-index: 2;
}
.gs-badge--alarm {
  top: 18%; left: 6%;
  background: linear-gradient(135deg, rgba(239,68,68,0.2), rgba(185,28,28,0.12));
  border: 1.5px solid rgba(239,68,68,0.45);
  box-shadow: 0 0 12px rgba(239,68,68,0.2), inset 0 0 8px rgba(239,68,68,0.08);
}
.gs-badge--warn {
  bottom: 12%; left: 8%;
  background: linear-gradient(135deg, rgba(245,158,11,0.18), rgba(180,83,9,0.10));
  border: 1.5px solid rgba(245,158,11,0.40);
  box-shadow: 0 0 12px rgba(245,158,11,0.15), inset 0 0 8px rgba(245,158,11,0.06);
}
.gs-badge--ok {
  bottom: 16%; right: 6%;
  background: linear-gradient(135deg, rgba(34,197,94,0.15), rgba(21,128,61,0.08));
  border: 1.5px solid rgba(34,197,94,0.35);
  box-shadow: 0 0 12px rgba(34,197,94,0.12), inset 0 0 8px rgba(34,197,94,0.05);
}
.gsb-label {
  font-size: 9px;
  letter-spacing: 1px;
  text-transform: uppercase;
}
.gsb-val {
  font-size: 20px;
  font-weight: 800;
  font-family: '"Orbitron","SF Mono","Consolas",monospace';
  line-height: 1.1;
}
.gs-badge--alarm .gsb-label { color: #fca5a5; }
.gs-badge--alarm .gsb-val   { color: #ef4444; text-shadow: 0 0 10px rgba(239,68,68,0.5); }
.gs-badge--warn .gsb-label { color: #fcd34d; }
.gs-badge--warn .gsb-val   { color: #f59e0b; text-shadow: 0 0 10px rgba(245,158,11,0.4); }
.gs-badge--ok .gsb-label   { color: #86efac; }
.gs-badge--ok .gsb-val     { color: #22c55e; text-shadow: 0 0 10px rgba(34,197,94,0.4); }

/* 右上角设备名标识 */
.gs-unit {
  position: absolute;
  top: 14%; right: 5%;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 2;
}
.gsu-icon { font-size: 13px; color: #3eaaff; filter: drop-shadow(0 0 4px rgba(62,170,255,0.5)); }
.gsu-text {
  font-size: 11px;
  color: #9fb6cf;
  font-family: '"SF Mono","Consolas",monospace';
  letter-spacing: 0.5px;
  text-shadow: 0 0 6px rgba(62,170,255,0.2);
}

/* 顶部装饰弧线 SVG */
.gs-top-arc {
  position: absolute;
  top: -2px; left: 0;
  width: 100%; height: 22px;
  pointer-events: none;
  z-index: 1;
}

/* 底部装饰条 */
.gs-bottom-bar {
  position: absolute;
  bottom: 0; left: 50%;
  transform: translateX(-50%);
  width: 60%; height: 3px;
  background: linear-gradient(90deg, transparent, rgba(62,170,255,0.25), rgba(62,170,255,0.35), rgba(62,170,255,0.25), transparent);
  border-radius: 2px;
  box-shadow: 0 0 8px rgba(62,170,255,0.15);
  z-index: 1;
}
.ca-card :deep(.cd-t) {
  flex-shrink: 0;
  font-size: 12px;
  letter-spacing: 1px;
  text-shadow: 0 0 8px rgba(62,170,255,0.3);
}
.ca-chart { flex: 1; min-height: 0; }
.ut-ic { color: #3eaaff; font-weight: 700; margin-right: 2px; }

/* 测点 — 紧凑 */
.ca-meas-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 6px; }
.ca-meas { flex-shrink: 0; max-height: 24%; overflow: auto; }
.ca-meas-i { background: rgba(4,6,10,0.7); border: 1px solid rgba(90,166,196,0.12); border-radius: 5px; padding: 6px 10px; transition: 0.2s; }
.ca-meas-i:hover { border-color: rgba(62,170,255,0.3); transform: translateY(-1px); }
.ca-meas-i.over { border-color: rgba(239,68,68,0.35); }
.ca-mi-top { display: flex; justify-content: space-between; align-items: center; }
.ca-mi-name { font-size: 11px; color: #d4ecff; font-weight: 600; }
.ca-mi-dot { width: 6px; height: 6px; border-radius: 50%; box-shadow: 0 0 5px currentColor; }
.ca-mi-val { font-size: 20px; font-weight: 800; font-family: "SF Mono","Consolas","Orbitron",monospace; margin: 2px 0 4px; text-shadow: 0 0 6px currentColor; }
.ca-mi-u { font-size: 10px; color: #9fb6cf; margin-left: 2px; font-weight: 400; }
.ca-mi-bar { height: 4px; border-radius: 2px; background: rgba(62,170,255,0.12); overflow: hidden; }
.ca-mi-bar span { display: block; height: 100%; border-radius: 2px; transition: width 0.6s ease; box-shadow: 0 0 4px currentColor; }
.ca-mi-lim { font-size: 10px; color: #9fb6cf; margin-top: 2px; }

/* AI — 紧凑 */
.ai p { line-height: 1.7; font-size: 12px; color: #cbd5e1; margin-bottom: 4px; }
.ca-ai { flex-shrink: 0; max-height: 90px; overflow: auto; }
.tg { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; margin-right: 6px; }
.tg-i { background: rgba(59, 130, 246, 0.12); color: #3eaaff; }
.tg-w { background: rgba(245, 158, 11, 0.12); color: #fbbf24; }
.tg-ok { background: rgba(34, 197, 94, 0.12); color: #34d399; }

/* 趋势控制条 — 紧凑 */
.ca-ctrls { flex-shrink: 0; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; padding: 5px 10px; background: rgba(4,6,10,0.85); border: 1px solid rgba(90,166,196,0.1); }
.ca-ctrl { display: flex; align-items: center; gap: 4px; }
.ca-ctrl label { font-size: 10px; color: #9fb6cf; }
.ca-ctrl-tip { color: #5a7894; font-size: 10px; font-weight: 400; }
.ca-ai-btn { background: linear-gradient(135deg, #3eaaff, #22d3ee); border: none; height: 24px; font-size: 11px; }

/* 趋势卡片：工具 + 钻取 + AI */
.ca-trend-tools { margin-left: auto; display: flex; align-items: center; gap: 10px; font-size: 11px; }
.ca-drill-hint { color: #5a7894; }
.ca-ai-trend-btn { color: #3eaaff !important; border-color: rgba(62,170,255,0.3) !important; }
.ca-chart { flex: 1; min-height: 0; position: relative; }
.ca-drill { position: absolute; top: 40px; right: 12px; width: 240px; background: rgba(8,18,38,0.96); border: 1px solid rgba(62,170,255,0.3); border-radius: 8px; padding: 12px 14px; box-shadow: 0 8px 30px rgba(0,0,0,0.5); z-index: 5; backdrop-filter: blur(4px); }
.ca-drill-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.ca-drill-t { font-size: 12px; font-weight: 600; color: #3eaaff; }
.ca-drill-b { display: flex; flex-direction: column; gap: 6px; }
.ca-dr { display: flex; justify-content: space-between; align-items: center; gap: 8px; font-size: 12px; color: #9fb6cf; }
.ca-dr b { font-family: monospace; color: #e2e8f0; font-weight: 600; }
.ca-dr-alarm { background: rgba(248,113,113,0.06); border-radius: 4px; padding: 6px 8px; align-items: flex-start; }
.ca-ai-trend { position: absolute; left: 10px; right: 10px; bottom: 10px; background: rgba(8,18,38,0.95); border: 1px solid rgba(62,170,255,0.25); border-radius: 8px; padding: 10px 14px; z-index: 5; max-height: 60%; overflow-y: auto; }
.ca-ai-trend-h { display: flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600; color: #3eaaff; margin-bottom: 6px; }
.ca-ai-trend-h .el-icon { color: #a78bfa; }
.ca-ai-trend-b { font-size: 12px; color: #cbd5e1; line-height: 1.7; }
.ca-ai-trend-b :deep(.ai-mt) { display: flex; justify-content: space-between; padding: 2px 0; }
.ca-ai-trend-b :deep(.ai-mt span) { color: #8fb0cf; }
.ca-ai-trend-b :deep(.ai-mt strong) { color: #c8e4ff; }
</style>

<!-- ═══ 非scoped覆盖：压制全局 .cd !important 深蓝锁色 ═══ -->
<style>
/* 四个面板内容区纯黑 + CRT 虚线方格 */
.ca-page > .cd {
  background-color: #04060a !important;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.12) 0px, rgba(90,166,196,0.12) 1px, transparent 1px, transparent 28px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.12) 0px, rgba(90,166,196,0.12) 1px, transparent 1px, transparent 28px) !important;
  border-color: rgba(90,166,196,0.16) !important;
  box-shadow: none !important;
}
/* 标题栏不透明深蓝底，挡住网格 */
.ca-page > .cd > .cd-t {
  background-color: #06121c !important;
  background-image: none !important;
}
</style>
