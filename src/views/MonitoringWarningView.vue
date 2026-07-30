<template>
<div class="mw-page">
  <!-- ═══ 顶部一行：左=筛选控制，右=时间范围（左右齐平，两个独立 section 不合并）═══ -->
  <section class="mw-toprow">
    <!-- 左：筛选控制栏 -->
    <section class="mw-ctrls">
      <el-select v-model="selDevice" filterable placeholder="选择设备" size="small" class="mw-sel">
        <el-option v-for="d in devs" :key="d.id" :label="d.name" :value="d.id">
          <span>{{ d.name }}</span><span style="float:right;color:#5a8aaa;font-size:11px;">健康 {{ d.health }}</span>
        </el-option>
      </el-select>
      <el-select v-model="selPoint" placeholder="测点筛选" size="small" class="mw-sel">
        <el-option v-for="p in points" :key="p" :label="p" :value="p" />
      </el-select>
      <button class="mw-gen-btn" @click="genJudge">生成研判</button>
      <button class="mw-ai-btn" @click="showAiTrend = !showAiTrend">AI 解读趋势</button>
    </section>

    <!-- 右：时间范围选择栏（与智能监盘/工况分析同源） -->
    <section class="mw-timebar">
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
    </section>
  </section>

  <!-- ═══ KPI 4卡片横排 ═══ -->
  <section class="mw-kpis">
    <div class="mw-kpi" style="--kac:#22c55e">
      <div class="mw-kpi-body">
        <span class="mw-kpi-l">设备健康度</span>
        <span class="mw-kpi-v">{{ cur?.health ?? '--' }}<small class="mw-kpi-u">%</small></span>
      </div>
      <div class="mw-kpi-ring" :style="{'--p':cur?.health??0, '--rc': cur?.health>=90 ? '#22c55e' : cur?.health>=80 ? '#f59e0b' : '#ef4444'}"></div>
    </div>
    <div class="mw-kpi" style="--kac:#3eaaff">
      <div class="mw-kpi-body">
        <span class="mw-kpi-l">机组出力 / 负荷</span>
        <span class="mw-kpi-v">{{ loadVal }}<small class="mw-kpi-u">MW</small></span>
      </div>
    </div>
    <!-- 运行状态 KPI 已移除（用户要求"关注去点"） -->
    <div class="mw-kpi" style="--kac:#a78bfa">
      <div class="mw-kpi-body">
        <span class="mw-kpi-l">所属机组 · 专业</span>
        <span class="mw-kpi-v">#{{ store.selectedUnitId }} · {{ cur?.dept || '--' }}</span>
      </div>
    </div>
  </section>

  <!-- ══════════════════════════════════════
       主内容区 — 3×2 网格（严格对照红框标注）
       ══════════════════════════════════════ -->
  <!-- ═══ 主内容区：左=预警研判分析图 | 中=仪表盘+趋势图(更宽) | 右=告警统计+AI研判(合并) ═══ -->
  <section class="mw-grid">

    <!-- 左：预警研判里的分析图（独占整列） -->
    <div class="cd mw-card mw-g-cell warn-analysis">
      <i class="hud-tl"></i><i class="hud-tr"></i><i class="hud-bl"></i><i class="hud-br"></i>
      <div class="cd-t"><span>预警研判分析</span></div>
      <div class="wa-body">
        <div class="wa-cards">
          <div class="wa-item" v-for="w in warnTypes" :key="w.key">
            <div :data-spark="w.key" class="wa-spark"></div>
            <div class="wa-info">
              <div class="wa-name">{{ w.name }}<el-tag size="small" :type="w.level" effect="plain" class="wa-tag">{{ w.tag }}</el-tag></div>
              <div class="wa-rows">
                <div class="wa-r"><span>当前值</span><b :style="{color:w.c}">{{ w.cur }}</b></div>
                <div class="wa-r"><span>阈值</span><b>{{ w.limit }}</b></div>
                <div class="wa-r"><span>偏离</span><b :style="{color:w.c}">{{ w.dev }}</b></div>
                <div class="wa-r"><span>状态</span><b :style="{color:w.c}"><span class="wa-dot" :style="{background:w.c}"></span>{{ w.state }}</b></div>
              </div>
            </div>
          </div>
        </div>
        <div class="wa-cmp-section">
          <div class="wa-cmp-hd">
            <span>实时 · 预测 · 设定 关联研判</span>
            <span class="wa-leg"><i style="background:#3eaaff"></i>实时<i style="background:#a78bfa"></i>预测<i style="background:#fbbf24"></i>设定</span>
          </div>
          <div ref="cmpRef" class="wa-cmp-chart"></div>
          <div class="wa-cmp-note">
            <span class="wa-cmp-icon">ⓘ</span>
            当 <b style="color:#3eaaff">实时值</b> 持续偏离 <b style="color:#a78bfa">预测值</b> 形成残差累积，或变化速率突破设定带时，系统自动升级为对应类型预警。
          </div>
        </div>
      </div>
    </div>

    <!-- 中上：仪表盘 -->
    <div class="cd mw-card mw-g-cell gauge-cell">
      <i class="hud-tl"></i><i class="hud-tr"></i><i class="hud-bl"></i><i class="hud-br"></i>
      <div class="cd-t"><span>运行效率仪表盘</span></div>
      <div class="gauge-scene">
        <div class="gs-badge" :class="gaugeBadgeCls">{{ gaugeBadgeTxt }}</div>
        <div class="gs-unit"><span class="gsu-icon">⏻</span><span class="gsu-text">{{ cur?.name || '当前设备' }}</span></div>
        <svg class="gs-top-arc" viewBox="0 0 200 20" preserveAspectRatio="none"><path d="M0,20 Q100,-10 200,20" fill="none" stroke="rgba(62,170,255,0.15)" stroke-width="1"/><path d="M30,16 Q100,2 170,16" fill="none" stroke="rgba(62,170,255,0.25)" stroke-width="0.5"/></svg>
        <div class="gs-bottom-bar"></div>
        <div ref="gaugeRef" class="gauge-dom"></div>
      </div>
    </div>

    <!-- 右：预警研判 & 告警统计 & AI研判详情（合并面板，占整列） -->
    <div class="cd mw-card mw-g-cell ai-merged-cell">
      <i class="hud-tl"></i><i class="hud-tr"></i><i class="hud-bl"></i><i class="hud-br"></i>
      <div class="cd-t"><span>预警研判 & 告警统计</span>
        <span class="am-count" v-if="judges.length">{{ judges.length }} 条研判</span>
      </div>
      <div class="am-body">
        <div class="am-alarm">
          <div class="am-sec-hd">告警分级统计</div>
          <div class="am-alarm-row">
            <div class="am-alarm-item danger"><i class="am-num">{{ alarmCounts.l1 }}</i><b>一级告警</b><em>严重风险需立即处置</em></div>
            <div class="am-alarm-item warning"><i class="am-num">{{ alarmCounts.l2 }}</i><b>二级告警</b><em>关注级异常波动</em></div>
            <div class="am-alarm-item info"><i class="am-num">{{ alarmCounts.warn }}</i><b>预警数量</b><em>潜在偏离趋势预警</em></div>
          </div>
        </div>
        <div class="am-judge">
          <div class="am-sec-hd">AI 预警研判结论</div>
          <div class="am-judge-list">
            <template v-if="judges.length">
              <div v-for="(j,idx) in judges" :key="idx" class="am-j-item" :class="j.cls">
                <div class="am-j-top">
                  <span class="am-j-sev" :style="{background:j.c}">{{ j.sev }}</span>
                  <span class="am-j-title">{{ j.title }}</span>
                  <button class="am-j-resolve" @click="j.resolve=!j.resolve">{{ j.resolve ? '已处置 ✓' : '标记处置' }}</button>
                </div>
                <div class="am-j-desc">{{ j.body }}</div>
                <div class="am-j-meta">
                  <span>预计触发 <b :style="{color:j.c}">{{ j.eta }}</b></span>
                  <span class="am-j-src">来源：AI 预警研判引擎</span>
                </div>
              </div>
            </template>
            <div v-else class="am-j-empty">暂无研判结论，请点击「生成研判」。</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中下：趋势图 -->
    <div class="cd mw-card mw-g-cell trend-cell">
      <i class="hud-tl"></i><i class="hud-tr"></i><i class="hud-bl"></i><i class="hud-br"></i>
      <div class="cd-t">
        <span>关键参数实时趋势</span>
        <div class="mw-trend-tools">
          <span class="mw-leg"><i style="background:#3eaaff"></i>实时<i style="background:#a78bfa"></i>预测<i style="background:#fbbf24"></i>设定</span>
          <button class="mw-mini-btn" @click="showAiTrend=!showAiTrend">AI 趋势解读</button>
        </div>
      </div>
      <div ref="trendRef" class="mw-chart"></div>
      <transition name="fade"><div v-if="showAiTrend" class="mw-ai-panel">
        <div class="mw-ai-close" @click="showAiTrend=false">×</div>
        <h4>AI 趋势智能解读</h4>
        <div class="mw-ai-body" v-html="aiTrendHtml"></div>
      </div></transition>
    </div>

  </section>

</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from '@/utils/echarts'
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useDataStore()
const userStore = useUserStore()

const clock = ref('')
const iv = setInterval(() => {
  const d = new Date()
  clock.value = `${String(d.getMonth()+1).padStart(2,'0')}/${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`
}, 1000)

const logout = () => { userStore.logout(); router.push('/login') }

/* ── 子标签栏 ── */
const subTabs = [
  { k:'monitor', n:'实时监控' },
  { k:'resid', n:'残差预警' },
  { k:'fluct', n:'波动预警' },
  { k:'rate', n:'变化速率预警' },
  { k:'ai', n:'AI 趋势研判' }
]
const activeTab = ref('monitor')

/* ── 时间范围选择（与智能监盘/工况分析同源）── */
const trRange = ref('24h')
const trCustom = ref(null)
const rangeLabel = (r) => ({ '1h': '近1小时', '6h': '近6小时', '24h': '近24小时', '7d': '近7天', custom: '自定义时段' }[r] || r)

/* ── 设备与数据 ── */
const devs = computed(() => store.devices)
const selDevice = ref(store.devices.find(d => d.health < 90)?.id || store.devices[0]?.id)
const selPoint = ref('温度')
const cur = computed(() => store.devices.find(d => d.id === selDevice.value) || store.devices[0])
const points = computed(() => Object.keys(cur.value?.params || {}))
watch(cur, () => { if (!points.value.includes(selPoint.value)) selPoint.value = points.value[0] })

const loadVal = computed(() => {
  if (!cur.value?.params) return '--'
  const p = cur.value.params['负荷'] || cur.value.params['功率']
  return p ? p[0] : '--'
})
const runStatus = computed(() => {
  const h = cur.value?.health ?? 0
  return h >= 90 ? { txt:'正常运行', cl:'ok' } : h >= 80 ? { txt:'关注运行', cl:'warn' } : { txt:'异常告警', cl:'bad' }
})

/* ── 测点数据 ── */
const measData = computed(() => cur.value?.params || {})

/* ── 告警统计 ── */
const alarmCounts = computed(() => {
  const alarms = store.unitAlarms(cur.value?.unit)
  return { l1: alarms.filter(a=>a.l===1).length, l2: alarms.filter(a=>a.l===2).length, warn: alarms.length }
})

/* ── 仪表盘状态 ── */
const gaugeBadgeCls = computed(() => {
  const h = cur.value?.health ?? 0
  return h >= 90 ? '--ok' : h >= 80 ? '--warn' : '--alarm'
})
const gaugeBadgeTxt = computed(() => {
  const h = cur.value?.health ?? 0
  return h >= 90 ? '正常' : h >= 80 ? '关注' : '告警'
})

/* ═══ 预警研判核心数据逻辑 ═══ */

const series = computed(() => {
  const base = cur.value?.params?.[selPoint.value]?.[0] || 50
  const limit = cur.value?.params?.[selPoint.value]?.[1] || 80
  const labels = Array.from({ length: 24 }, (_, i) => (i * 3) + ':00')
  const real = [], pred = [], set = []
  for (let i = 0; i < 24; i++) {
    const t = i / 23
    const drift = Math.sin(i / 2.5) * base * 0.06 + (t > 0.6 ? (t - 0.6) * base * 0.5 : 0)
    const noise = (Math.random() - 0.5) * base * 0.03
    real.push(+(base + drift + noise).toFixed(1))
    pred.push(+(base + Math.sin(i / 2.5) * base * 0.05).toFixed(1))
    set.push(+(limit * (0.92 + 0.03 * Math.sin(i / 4))).toFixed(1))
  }
  return { labels, real, pred, set, limit }
})

const warnTypes = computed(() => {
  const s = series.value
  const resid = s.real.map((v, i) => +(v - s.pred[i]).toFixed(1))
  const residSum = resid.slice(-6).reduce((a, b) => a + Math.max(0, b), 0)
  let fluct = 0
  for (let i = 1; i < s.real.length; i++) fluct += Math.abs(s.real[i] - s.real[i - 1])
  fluct = +(fluct / (s.real.length - 1)).toFixed(2)
  const rate = +(((s.real[23] - s.real[20]) / 3) * 60).toFixed(1)
  return [
    { key: 'resid', name: '残差预警', ic: '∑', c: '#f87171', level: 'danger', tag: '一级', cur: '+' + residSum.toFixed(1), limit: '≤ 0', dev: '+' + residSum.toFixed(1), state: residSum > 2 ? '已触发' : '正常', data: resid },
    { key: 'fluct', name: '波动预警', ic: '∿', c: '#fbbf24', level: 'warning', tag: '二级', cur: fluct, limit: '≤ 1.5', dev: '+' + (fluct - 1.5).toFixed(2), state: fluct > 1.5 ? '已触发' : '正常', data: s.real },
    { key: 'rate', name: '变化速率预警', ic: '↗', c: '#22d3ee', level: 'info', tag: '预警', cur: rate + '/h', limit: '≤ 8/h', dev: (rate - 8 >= 0 ? '+' : '') + (rate - 8).toFixed(1), state: rate > 8 ? '已触发' : '正常', data: s.real.map((v, i) => i === 0 ? 0 : +((v - s.real[i - 1]) * 60).toFixed(1)) }
  ]
})

/* ── ECharts refs ── */
const trendRef = ref(null)
const gaugeRef = ref(null)
const cmpRef = ref(null)
let tCh, gCh, cmpCh
const sparkCharts = {}

/* ── AI 研判 ── */
const judges = ref([])
const showAiTrend = ref(false)
const aiTrendHtml = ref('')

function genJudge() {
  const wt = warnTypes.value
  const fired = wt.filter(w => w.state === '已触发')
  const arr = []
  if (fired.length) {
    fired.forEach(w => {
      arr.push({
        sev: w.tag, c: w.c, cls: w.level,
        title: w.name + '：' + (cur.value?.name || '设备') + ' ' + selPoint.value,
        body: `检测到 ${w.name} 已触发，当前值 ${w.cur}，偏离阈值 ${w.dev}。AI 判断该偏离呈持续累积趋势，若不干预将在未来时段扩大。`,
        eta: ['12分钟', '38分钟', '1.2小时', '2.5小时'][Math.floor(Math.random() * 4)],
        resolve: false
      })
    })
  } else {
    arr.push({ sev: '提示', c: '#34d99e', cls: 'success', title: '趋势平稳', body: `${cur.value?.name||'设备'} ${selPoint.value} 三类预警均未触发，残差/波动/速率均在阈值带内，预计 2 小时内无升级风险。`, eta: '>2h', resolve: false })
  }
  arr.push({
    sev: '研判', c: '#3eaaff', cls: 'primary',
    title: 'AI 趋势研判：' + (cur.value?.name || '设备'),
    body: `基于近 24h 时序建模，${selPoint.value} 呈 ${(Math.random()>0.5?'上升':'缓降')}趋势，预测未来 1h 将${fired.length?'突破预警线':'维持安全区'}。建议${Math.random()>0.5?'加强巡检频次至每 30min':'提前调整运行参数'}。`,
    eta: (40 + Math.floor(Math.random() * 80)) + '分钟',
    resolve: false
  })
  judges.value = arr
}

function hexA(c, a) {
  c = String(c).replace('#','')
  if (c.length === 3) c = c[0]+c[0]+c[1]+c[1]+c[2]+c[2]
  const r = parseInt(c.slice(0,2),16), g = parseInt(c.slice(2,4),16), b = parseInt(c.slice(4,6),16)
  return `rgba(${r},${g},${b},${a})`
}

/* ═══ 图表初始化 ═══ */

function initCharts() {
  initSparks()
  initTrendChart()
  initGaugeChart()
  initCmpChart()
}

function createSpark(el, key) {
  if (!el) return false
  const w = warnTypes.value.find(x => x.key === key)
  if (!w || !w.data || !w.data.length) return false
  if (sparkCharts[key]) { try { sparkCharts[key].dispose() } catch(e) {} }
  const ch = echarts.init(el)
  sparkCharts[key] = ch
  ch.setOption({
    backgroundColor: '#000000',
    grid: { left: 2, right: 2, top: 6, bottom: 2 },
    xAxis: { type: 'category', show: false, data: w.data.map((_, i) => i), boundaryGap: false },
    yAxis: { type: 'value', show: false, scale: true },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(8,20,40,0.92)', borderColor: w.c, textStyle: { color: '#c8e4ff', fontSize: 11 }, formatter: (p) => (key==='resid'?'残差 ':'值 ') + p[0].data },
    series: [{ type: 'line', smooth: true, symbol: 'none', data: w.data, lineStyle: { width: 2, color: w.c }, areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:hexA(w.c,0.35)},{offset:1,color:hexA(w.c,0.02)}]) } }]
  })
  return true
}

function initSparks() {
  const keys = ['resid', 'fluct', 'rate']
  keys.forEach(key => {
    const el = document.querySelector('.wa-spark[data-spark="' + key + '"]')
    if (el) createSpark(el, key)
  })
}

function initTrendChart() {
  if (!trendRef.value) return
  tCh = echarts.init(trendRef.value)
  const s = series.value
  const extraSeries = []
  const colors = ['#fbbf24','#34d399','#22d3ee','#a78bfa','#f87171']
  let ci = 0
  for (const [k,v] of Object.entries(measData.value).slice(0,4)) {
    if (k === selPoint.value) continue
    const base = parseFloat(v[0]) || 50
    const data = Array.from({length:24},(_,i)=> +(base + (Math.sin(i/3+ci*0.7)*base*0.12) + (Math.random()-0.5)*base*0.06).toFixed(1))
    extraSeries.push({ name:k, type:'line', smooth:true, symbol:'circle', symbolSize:3, lineStyle:{width:1.5, color:colors[ci%colors.length], type:'dashed'}, itemStyle:{color:colors[ci%colors.length]}, data })
    ci++
  }
  tCh.setOption({
    backgroundColor:'transparent',
    legend:{top:4,textStyle:{color:'#9fb6cf',fontSize:10},itemWidth:12,itemHeight:8},
    grid:{left:46,right:18,top:32,bottom:26},
    tooltip:{trigger:'axis',backgroundColor:'rgba(8,20,40,0.92)',borderColor:'rgba(62,170,255,0.2)',textStyle:{color:'#d4ecff',fontSize:11}},
    xAxis:{type:'category',data:s.labels,boundaryGap:false,axisLine:{lineStyle:{color:'rgba(90,166,196,0.12)'}},axisLabel:{color:'#7a98b6',fontSize:10},splitLine:{show:false}},
    yAxis:{type:'value',axisLine:{show:false},axisLabel:{color:'#7a98b6',fontSize:10},splitLine:{lineStyle:{color:'rgba(90,166,196,0.08)',type:'dashed'}},splitNumber:4},
    series: [
      { name:'实时', type:'line', smooth:true, symbol:'none', data:s.real, lineStyle:{width:2.5,color:'#3eaaff'}, areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(62,170,255,0.25)'},{offset:1,color:'rgba(62,170,255,0)'}])} },
      { name:'预测', type:'line', smooth:true, symbol:'none', data:s.pred, lineStyle:{width:2,color:'#a78bfa',type:'dashed'} },
      { name:'设定', type:'line', smooth:true, symbol:'none', data:s.set, lineStyle:{width:2,color:'#fbbf24'}, markLine:{silent:true,symbol:'none',lineStyle:{color:'#fbbf24',type:'dotted'},label:{formatter:'阈值带',color:'#fbbf24',fontSize:10},data:[{yAxis:s.limit}]} },
      ...extraSeries
    ]
  }, true)
  aiTrendHtml.value = `<p><span class="tg tg-i">趋势分析</span> 当前各测点参数在近 24 小时内呈现<span class="tg tg-w">小幅波动</span>态势，整体处于可控区间。</p><p><span class="tg tg-w">关注点</span> ${selPoint.value}类参数在第 12~16 时段出现峰值，与机组高负荷时段吻合，属正常热力学响应。</p><p><span class="tg tg-ok">建议</span> 持续监测振动参数的耦合变化趋势，若振幅超过基准 20%，应启动专项诊断流程。</p>`
}

function initGaugeChart() {
  if (!gaugeRef.value) return
  gCh = echarts.init(gaugeRef.value)
  const val = cur.value?.health ?? 88
  const color = val >= 90 ? '#22c55e' : val >= 80 ? '#f59e0b' : '#ef4444'
  const gRed = new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'rgba(239,68,68,0.35)'},{offset:1,color:'rgba(239,68,68,0.18)'}])
  const gYellow = new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'rgba(245,158,11,0.32)'},{offset:1,color:'rgba(245,158,11,0.16)'}])
  const gGreen = new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'rgba(34,197,94,0.30)'},{offset:1,color:'rgba(34,197,94,0.14)'}])
  const gProgressColor = new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:hexA(color,0.6)},{offset:0.5,color:hexA(color,0.85)},{offset:1,color}])
  gCh.setOption({
    backgroundColor:'transparent',
    series:[{
      type:'gauge', startAngle:220, endAngle:-40, min:0,max:100,
      radius:'82%', center:['50%','52%'],
      axisLine:{lineStyle:{width:12, color:[[0.6,gRed],[0.85,gYellow],[1,gGreen]]}},
      splitNumber:10,
      axisTick:{distance:-16,length:5,lineStyle:{color:'rgba(62,170,255,0.35)',width:1}},
      splitLine:{distance:-20,length:12,lineStyle:{color:'rgba(62,170,255,0.45)',width:1.5}},
      axisLabel:{distance:-4,color:'#8fb8db',fontSize:10,fontFamily:'SF Mono,Consolas,monospace'},
      pointer:{show:true,length:'55%',width:4,itemStyle:{color:color,shadowBlur:14,shadowColor:color}},
      anchor:{show:true,size:10,itemStyle:{color,color:'#000000',borderWidth:2.5,shadowBlur:12,shadowColor:color}},
      progress:{show:true,width:12,roundCap:true,itemStyle:{color:gProgressColor,shadowBlur:20,shadowColor:color}},
      detail:{valueAnimation:true,fontSize:38,fontWeight:800,fontFamily:'SF Mono,Consolas,monospace',color:color,offsetCenter:[0,'28%'],formatter:'{value}%',textShadowBlur:24,textShadowColor:color},
      title:{show:true,offsetCenter:[0,'62%'],fontSize:13,fontWeight:600,color:'rgba(180,210,235,0.7)',fontFamily:'SF Mono,Consolas,monospace'},
      data:[{value:val,name:'运行效率'}]
    },{
      type:'gauge', startAngle:220, endAngle:-40, radius:'100%', center:['50%','52%'],
      axisLine:{lineStyle:{width:1.5,color:[[1,'rgba(62,170,255,0.18)']]}},
      axisTick:{show:true,distance:-4,length:6,lineStyle:{color:'rgba(62,170,255,0.28)',width:1}},
      splitLine:{show:false},axisLabel:{show:false},pointer:{show:false},detail:{show:false}
    }]
  }, true)
}

function initCmpChart() {
  if (!cmpRef.value) return
  cmpCh = echarts.init(cmpRef.value)
  const s = series.value
  cmpCh.setOption({
    backgroundColor:'transparent',
    legend:{top:2,textStyle:{color:'#9fb6cf',fontSize:9},itemWidth:10,itemHeight:6},
    grid:{left:40,right:14,top:24,bottom:22},
    tooltip:{trigger:'axis',backgroundColor:'rgba(8,20,40,0.92)',borderColor:'rgba(62,170,255,0.2)',textStyle:{color:'#d4ecff',fontSize:10}},
    xAxis:{type:'category',data:s.labels,boundaryGap:false,axisLine:{lineStyle:{color:'rgba(90,166,196,0.1)'}},axisLabel:{color:'#6a88a6',fontSize:9},splitLine:{show:false}},
    yAxis:{type:'value',axisLine:{show:false},axisLabel:{color:'#6a88a6',fontSize:9},splitLine:{lineStyle:{color:'rgba(90,166,196,0.07)',type:'dashed'}},splitNumber:4},
    series:[
      { name:'实时', type:'line', smooth:true, symbol:'none', data:s.real, lineStyle:{width:2,color:'#3eaaff'}, areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(62,170,255,0.2)'},{offset:1,color:'rgba(62,170,255,0)'}])} },
      { name:'预测', type:'line', smooth:true, symbol:'none', data:s.pred, lineStyle:{width:1.8,color:'#a78bfa',type:'dashed'} },
      { name:'设定', type:'line', smooth:true, symbol:'none', data:s.set, lineStyle:{width:1.8,color:'#fbbf24',type:'dotted'}, areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(251,191,36,0.08)'},{offset:1,color:'rgba(251,191,36,0)'}])} }
    ]
  }, true)
}

function rz() { tCh?.resize(); gCh?.resize(); cmpCh?.resize(); Object.values(sparkCharts).forEach(c => c?.resize()) }

watch([selDevice, selPoint], () => nextTick(() => { tCh?.dispose(); gCh?.dispose(); cmpCh?.dispose(); Object.values(sparkCharts).forEach(c => c?.dispose()); initCharts() }))
watch(activeTab, () => {})

onMounted(() => nextTick(initCharts))
onUnmounted(() => { clearInterval(iv); tCh?.dispose(); gCh?.dispose(); cmpCh?.dispose(); Object.values(sparkCharts).forEach(c => c?.dispose()); window.removeEventListener('resize', rz) })
window.addEventListener('resize', rz)
</script>

<style scoped>
/* ═══ 根容器 ═══ */
.mw-page {
  height: 100%; overflow: hidden;
  display: flex; flex-direction: column; gap: 6px;
  padding: 10px 14px; box-sizing: border-box;
  background-color: #020305;
  position: relative;
}
.mw-page::before {
  content:''; position:absolute; inset:0; pointer-events:none; z-index:0;
  background-image:
    repeating-linear-gradient(0deg, transparent 0px, transparent 27px, rgba(90,166,196,0.07) 27px, rgba(90,166,196,0.07) 28px),
    repeating-linear-gradient(90deg, transparent 0px, transparent 27px, rgba(90,166,196,0.07) 27px, rgba(90,166,196,0.07) 28px);
  background-size: 28px 28px;
}

/* ═══ 顶部导航栏 ═══ */
.mw-head {
  display: flex; align-items: center; gap: 12px;
  padding: 6px 14px; position: relative; z-index: 3;
  flex-shrink: 0;
  background: linear-gradient(180deg, rgba(3,8,15,0.504), rgba(2,6,11,0.595));
  border-bottom: 1px solid rgba(90,166,196,0.12);
}
.mw-head-l { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.mw-back-btn {
  padding: 3px 10px; font-size: 10px; font-weight: 600;
  border: 1px solid rgba(62,170,255,0.2);
  background: rgba(3,9,17,0.42); color: #8eb8dc; cursor: pointer;
  font-family: SF Mono, Consolas, monospace;
  transition: all .2s;
  clip-path: polygon(3px 0, calc(100%-3px) 0, 100% 3px, 100% calc(100%-3px), calc(100%-3px) 100%, 3px 100%, 0 calc(100%-3px), 0 3px);
}
.mw-back-btn:hover { border-color: rgba(62,170,255,0.5); color: #c8e8ff; }
.mw-bar { width: 3px; height: 18px; background: linear-gradient(180deg, #3eaaff, #22d3ee); box-shadow: 0 0 8px rgba(62,170,255,0.5); }
.mw-title { margin: 0; font-size: 15px; font-weight: 600; letter-spacing: 1px; color: #d4ecff; white-space: nowrap; font-family: SF Mono, Consolas, monospace; }

.mw-tabs { display: flex; gap: 2px; flex: 1; justify-content: center; }
.mw-tab {
  padding: 4px 14px; font-size: 11px; font-weight: 600;
  border: none; background: none; color: #6a8caa; cursor: pointer;
  transition: all .25s; letter-spacing: .5px;
  font-family: SF Mono, Consolas, monospace; position: relative;
}
.mw-tab::after {
  content:''; position:absolute; bottom:0; left:50%; transform:translateX(-50%);
  width:0; height:2px; background:#3eaaff; transition:width .25s;
  box-shadow: 0 0 6px rgba(62,170,255,0.5);
}
.mw-tab.active { color: #c8e8ff; }
.mw-tab.active::after { width: 70%; }
.mw-tab:hover { color: #a8d4ff; }

.mw-head-r { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.mw-user { font-size: 10px; color: #7aa0c0; font-family: SF Mono, Consolas, monospace; }
.mw-status-dot { width: 7px; height: 7px; border-radius: 50%; }
.mw-status-dot.on { background: #34d399; box-shadow: 0 0 6px rgba(52,211,153,0.6); }
.mw-status-dot.off { background: #666; }
.mw-status-txt { font-size: 10px; color: #7aa0c0; }
.mw-time { font-size: 11px; color: #a8d4ff; font-family: SF Mono, Consolas, monospace; text-shadow: 0 0 4px rgba(62,170,255,0.3); }
.mw-logout {
  padding: 3px 10px; font-size: 10px; font-weight: 600;
  border: 1px solid rgba(232,95,95,0.25);
  background: rgba(20,4,6,0.35); color: #f07070; cursor: pointer;
  transition: all .25s;
  clip-path: polygon(4px 0, calc(100%-4px) 0, 100% 3px, 100% calc(100%-3px), calc(100%-4px) 100%, 4px 100%, 0 calc(100%-3px), 0 3px);
  font-family: SF Mono, Consolas, monospace;
}
.mw-logout:hover { border-color: rgba(232,95,95,0.55); color: #ffa0a0; }

/* ═══ 筛选控制栏 ═══ */
/* ── 顶部一行：左筛选 + 右时间，左右齐平 ── */
.mw-toprow { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 7px 14px 5px; flex-shrink: 0; }
.mw-toprow .mw-ctrls { display: flex; align-items: center; gap: 8px; }
.mw-toprow .mw-timebar { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
.mw-timebar .ca-ctrl { display: flex; align-items: center; gap: 6px; }
.mw-timebar .ca-ctrl label { font-size: 11px; color: #9fb6cf; }
.mw-sel { width: 180px; }
.mw-sel :deep(.el-input__wrapper) { background: rgba(3,9,17,0.42); box-shadow: 0 0 0 1px rgba(62,170,255,0.15) inset; }
.mw-sel :deep(.el-input__inner) { color: #a8c4d8; font-family: SF Mono, Consolas, monospace; font-size: 12px; }
.mw-gen-btn {
  padding: 5px 16px; font-size: 11px; font-weight: 700;
  border: 1px solid rgba(52,211,153,0.35); border-top-color: rgba(52,211,153,0.55);
  background: linear-gradient(135deg, rgba(10,60,40,0.6), rgba(3,20,14,0.49));
  color: #5eead4; cursor: pointer; transition: all .25s;
  font-family: SF Mono, Consolas, monospace;
  clip-path: polygon(4px 0, calc(100%-4px) 0, 100% 3px, 100% calc(100%-3px), calc(100%-4px) 100%, 4px 100%, 0 calc(100%-3px), 0 3px);
  box-shadow: inset 0 0 8px rgba(52,211,153,0.08);
}
.mw-gen-btn:hover { border-color: rgba(52,211,153,0.65); box-shadow: inset 0 0 12px rgba(52,211,153,0.18); }
.mw-ai-btn {
  padding: 5px 14px; font-size: 11px; font-weight: 600;
  border: 1px solid rgba(62,170,255,0.25); border-top-color: rgba(62,170,255,0.45);
  background: linear-gradient(135deg, rgba(10,30,60,0.6), rgba(3,10,20,0.49));
  color: #8eb8dc; cursor: pointer; transition: all .25s;
  font-family: SF Mono, Consolas, monospace;
  clip-path: polygon(4px 0, calc(100%-4px) 0, 100% 3px, 100% calc(100%-3px), calc(100%-4px) 100%, 4px 100%, 0 calc(100%-3px), 0 3px);
}
.mw-ai-btn:hover { border-color: rgba(62,170,255,0.55); color: #c8e8ff; }

/* ═══ KPI 卡片行 — 深空黑 + 弱网格 + 圆角6px ═══ */
.mw-kpis { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; padding: 0 14px; flex-shrink: 0; z-index: 2; }
.mw-kpi {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px;
  background-color: rgba(2,3,5,0.574);
  background-image:
    linear-gradient(rgba(62,170,255,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(62,170,255,0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  border: 1px solid rgba(62,170,255,0.09);
  border-radius: 6px;
  position: relative; overflow: hidden;
  transition: border-color 0.2s;
}
.mw-kpi:hover { border-color: rgba(62,170,255,0.20); }
/* 左侧霓虹灯带 */
.mw-kpi::before {
  content:''; position:absolute; left:0; top:6px; bottom:6px; width:3px;
  background: var(--kac, #3eaaff);
  box-shadow: 0 0 8px var(--kac, #3eaaff), inset 0 0 3px var(--kac, #3eaaff);
  border-radius:1px;
}
.mw-kpi-body { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
.mw-kpi-l { font-size: 10px; color: #9fb6cf; letter-spacing: 0.3px; font-weight: 500; }
.mw-kpi-v { font-size: 22px; font-weight: 800; font-family: SF Mono, Consolas, monospace; color: #fff; text-shadow: 0 0 8px rgba(62,170,255,0.3); line-height: 1.1; }
.mw-kpi-u { font-size: 10px; color: #8fb0cf; margin-left: 2px; font-style: normal; font-weight: 400; }
.mw-kpi-ring {
  width: 40px; height: 40px; border-radius: 50%;
  background: conic-gradient(var(--rc,#22c55e) calc(var(--p,0)*1%), rgba(90,166,196,0.08) 0);
  position: relative; flex-shrink: 0;
  mask: radial-gradient(transparent 54%, #000 55%);
  -webkit-mask: radial-gradient(transparent 54%, #000 55%);
}
.mw-kpi-ring::after { content:''; position:absolute; inset:5px; border-radius:50%; background: #020305; }
.mw-kpi-st { display: flex; align-items: center; gap: 6px !important; }
.mw-st-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; animation: stPulse 2s infinite; }
.mw-st-dot.ok { background: #22c3ee; box-shadow: 0 0 8px rgba(34,195,238,0.6); }
.mw-st-dot.warn { background: #f59e0b; box-shadow: 0 0 8px rgba(245,158,11,0.6); }
.mw-st-dot.bad { background: #ef4444; box-shadow: 0 0 8px rgba(239,68,68,0.6); animation: stPulse 1s infinite; }
@keyframes stPulse { 0%,100%{opacity:1} 50%{opacity:.4} }

/* ══════════════════════════════════════
   核心：3×2 网格布局（严格对照红框）
   ══════════════════════════════════════ */
.mw-grid {
  display: grid;
  grid-template-columns: 1fr 1.35fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 8px;
  flex: 1; min-height: 0; padding: 0 14px; z-index: 2;
}
.mw-g-cell {
  display: flex; flex-direction: column; overflow: hidden;
  min-height: 0;
}

/* ── 左：预警研判分析图（独占整列） ── */
.warn-analysis { grid-column: 1; grid-row: 1 / 3; }
.wa-body { display: flex; flex-direction: column; gap: 6px; flex: 1; overflow-y: auto; padding: 8px 10px; position:relative; z-index:1; }
.wa-body::-webkit-scrollbar { width: 3px; }
.wa-body::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.wa-cards { display: flex; flex-direction: column; gap: 6px; }

/* 预警条目 — 半透明黑 rgba(4,6,10,0.82) + 弱网格 + 圆角6px */
.wa-item {
  display: flex; gap: 8px; padding: 9px 11px;
  background-color: rgba(2,3,5,0.574);
  background-image:
    linear-gradient(rgba(62,170,255,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(62,170,255,0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  border: 1px solid rgba(62,170,255,0.08);
  border-radius: 6px;
  border-left: none;
  transition: transform 0.15s, border-color 0.15s;
  position: relative;
}
/* 左侧霓虹灯带 */
.wa-item::before {
  content:''; width:4px; height:auto; align-self:stretch; flex-shrink:0;
  margin:3px 0; border-radius:2px;
  box-shadow:0 0 8px currentColor;
}
.wa-item:hover { transform: translateX(3px); border-color: rgba(62,170,255,0.20); }
/* 按预警类型着色 — 暗饱和色板 */
.wa-item.bad { background-color: rgba(224,122,107,0.06); border-color: rgba(224,122,107,0.14); }
.wa-item.bad::before { background:#e07a6b; color:#e07a6b; }
.wa-item.warn { background-color: rgba(224,168,92,0.05); border-color: rgba(224,168,92,0.12); }
.wa-item.warn::before { background:#e0a85c; color:#e0a85c; }
.wa-item.info { background-color: rgba(90,166,196,0.04); border-color: rgba(90,166,196,0.10); }
.wa-item.info::before { background:#5aa6c4; color:#5aa6c4; }
.wa-item.ok { background-color: rgba(91,184,138,0.04); border-color: rgba(91,184,138,0.10); }
.wa-item.ok::before { background:#5bb88a; color:#5bb88a; }

.wa-spark { width: 100px; height: 60px; flex-shrink: 0; background-color: rgba(2,3,5,0.574); background-image: linear-gradient(rgba(62,170,255,0.05) 1px, transparent 1px), linear-gradient(90deg, rgba(62,170,255,0.05) 1px, transparent 1px); background-size: 24px 24px; border-radius: 6px; border: 1px solid rgba(62,170,255,0.08); }
.wa-info { flex: 1; display: flex; flex-direction: column; justify-content: space-around; min-width: 0; }
.wa-name { font-size: 11px; font-weight: 700; color: #d4ecff; display: flex; align-items: center; gap: 6px; font-family:"SF Mono","Consolas",monospace; }
.wa-tag { margin-left: auto; font-size: 9px; }
.wa-rows { display: flex; flex-direction: column; gap: 2px; }
.wa-r { display: flex; justify-content: space-between; font-size: 10px; color: #8ba8c6; }
.wa-r b { font-family: "SF Mono","Consolas",monospace; font-weight: 700; color: #e2e8f0; display: flex; align-items: center; gap: 3px; text-shadow:0 0 5px currentColor; }
.wa-dot { width: 5px; height: 5px; border-radius: 50%; box-shadow: 0 0 4px currentColor; }

/* 关联研判对比图 — 深空黑 + 弱网格 */
.wa-cmp-section { flex: 1; min-height: 120px; display: flex; flex-direction: column;
  padding:7px 9px;
  background-color: rgba(2,3,5,0.574);
  background-image:
    linear-gradient(rgba(62,170,255,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(62,170,255,0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  border:1px solid rgba(62,170,255,0.07);
  border-radius: 6px;
}
.wa-cmp-hd {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 11px; font-weight: 600; color: #a8c8e4; padding: 4px 0;
  font-family:"SF Mono","Consolas",monospace; letter-spacing:0.5px;
}
.wa-leg { font-size: 9px; color: #7a98b6; display: flex; align-items: center; gap: 4px; }
.wa-leg i { width: 12px; height: 2px; border-radius: 1px; display: inline-block; margin-left: 6px; }
.wa-cmp-chart { flex: 1; min-height: 0; }
.wa-cmp-note {
  font-size: 9px; color: #6a86a4; line-height: 1.5;
  padding: 5px 7px; background: rgba(3,9,17,0.315); border-radius: 3px;
  display: flex; gap: 4px; align-items: flex-start;
  border:1px solid rgba(62,170,255,0.05);
}
.wa-cmp-icon { color: #3eaaff; flex-shrink: 0; text-shadow:0 0 5px rgba(62,170,255,0.4); }

/* ── 中上：仪表盘 — 切角底板 + 示波器网格 ── */
.gauge-cell { grid-column: 2; grid-row: 1; display: flex; flex-direction: column; }
.gauge-scene { position: relative; flex: 1 1 auto; min-height: 140px;
  background:
    linear-gradient(rgba(62,170,255,0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(62,170,255,0.07) 1px, transparent 1px);
  background-size: 28px 28px;
}
.gauge-scene::after {
  content:''; position:absolute; width:14px; height:14px;
  top:6px; right:6px;
  border-top: 2px solid rgba(90,166,196,0.35);
  border-right: 2px solid rgba(90,166,196,0.2);
  pointer-events:none;
}
.gs-badge {
  position: absolute; top: 14%; left: 6%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  width: 42px; height: 42px; border-radius: 10px;
  clip-path: polygon(30% 0%, 70% 0%, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0% 70%, 0% 30%);
  z-index: 2;
}
.gs-badge--ok { background: linear-gradient(135deg, rgba(34,197,94,0.15), rgba(21,128,61,0.08)); border: 1.5px solid rgba(34,197,94,0.35); box-shadow: 0 0 12px rgba(34,197,94,0.12); }
.gs-badge--warn { background: linear-gradient(135deg, rgba(245,158,11,0.18), rgba(180,83,9,0.10)); border: 1.5px solid rgba(245,158,11,0.40); box-shadow: 0 0 12px rgba(245,158,11,0.15); }
.gs-badge--alarm { background: linear-gradient(135deg, rgba(239,68,68,0.2), rgba(185,28,28,0.12)); border: 1.5px solid rgba(239,68,68,0.45); box-shadow: 0 0 12px rgba(239,68,68,0.2); }
.gs-badge span:first-child { font-size: 9px; letter-spacing: 1px; text-transform: uppercase; }
.gs-badge span:last-child { font-size: 17px; font-weight: 800; font-family: SF Mono, Consolas, monospace; line-height: 1.1; }
.gs-badge--ok span:first-child { color: #86efac; } .gs-badge--ok span:last-child { color: #22c55e; text-shadow: 0 0 10px rgba(34,197,94,0.4); }
.gs-badge--warn span:first-child { color: #fcd34d; } .gs-badge--warn span:last-child { color: #f59e0b; text-shadow: 0 0 10px rgba(245,158,11,0.4); }
.gs-badge--alarm span:first-child { color: #fca5a5; } .gs-badge--alarm span:last-child { color: #ef4444; text-shadow: 0 0 10px rgba(239,68,68,0.5); }
.gs-unit { position: absolute; top: 14%; right: 5%; display: flex; align-items: center; gap: 4px; z-index: 2; }
.gsu-icon { font-size: 13px; color: #3eaaff; filter: drop-shadow(0 0 4px rgba(62,170,255,0.5)); }
.gsu-text { font-size: 11px; color: #9fb6cf; font-family: SF Mono, Consolas, monospace; letter-spacing: 0.5px; text-shadow: 0 0 6px rgba(62,170,255,0.2); }
.gs-top-arc { position: absolute; top: -2px; left: 0; width: 100%; height: 22px; pointer-events: none; z-index: 1; }
.gs-bottom-bar {
  position: absolute; bottom: 0; left: 10%; right: 10%; height: 2px;
  background: linear-gradient(90deg, transparent, #3eaaff, transparent);
  box-shadow: 0 0 8px rgba(62,170,255,0.5); pointer-events: none;
}
.gauge-dom { position: absolute; inset: 0; }

/* ── 右：预警研判 & 告警统计 & AI研判详情 合并面板（暗色调 HUD） ── */
.ai-merged-cell { grid-column: 3; grid-row: 1 / 3; }
.am-body { display: flex; flex-direction: column; gap: 7px; flex: 1; overflow-y: auto; padding: 8px 10px; position:relative; z-index:1; }
.am-body::-webkit-scrollbar { width: 3px; }
.am-body::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.am-count { margin-left: auto; font-size: 9px; color: #5bb88a; background: rgba(91,184,138,0.10); padding: 1px 7px; border-radius: 8px; font-weight: 600;
  border:1px solid rgba(91,184,138,0.15);
}
.am-sec-hd { font-size: 11px; font-weight: 700; color: #7ec8ff; padding: 4px 0; border-bottom: 1px solid rgba(62,170,255,0.08); margin-bottom: 4px;
  letter-spacing:1px; font-family:"SF Mono","Consolas",monospace;
}

/* 告警分级条目 — 半透明黑 rgba(4,6,10,0.82) + 弱网格 + 圆角6px */
.am-alarm-row { display: flex; flex-direction: column; gap: 7px; }
.am-alarm-item {
  display: flex; align-items: center; gap: 9px; padding: 9px 12px;
  background: rgba(2,3,5,0.574);
  border: 1px solid rgba(62,170,255,0.08);
  border-radius: 6px;
  border-left: none;
  transition: transform 0.15s, border-color 0.15s;
  position: relative;
}
.am-alarm-item::before {
  content:''; width:4px; height:22px; align-self:center; flex-shrink:0; border-radius:2px;
  box-shadow:0 0 8px currentColor;
}
.am-alarm-item.danger { --ac: #e07a6b; background-color: rgba(224,122,107,0.06); border-color: rgba(224,122,107,0.12); }
.am-alarm-item.danger::before { background:#e07a6b; color:#e07a6b; }
.am-alarm-item.warning { --ac: #e0a85c; background-color: rgba(224,168,92,0.05); border-color: rgba(224,168,92,0.10); }
.am-alarm-item.warning::before { background:#e0a85c; color:#e0a85c; }
.am-alarm-item.info { --ac: #5aa6c4; background-color: rgba(90,166,196,0.04); border-color: rgba(90,166,196,0.10); }
.am-alarm-item.info::before { background:#5aa6c4; color:#5aa6c4; }
.am-num { font-size: 18px; font-weight: 900; font-family: "SF Mono","Consolas",monospace; min-width: 28px; text-align: center;
  text-shadow:0 0 8px currentColor, 0 0 3px currentColor;
}
.am-alarm-item.danger .am-num { color: #e07a6b; }
.am-alarm-item.warning .am-num { color: #e0a85c; }
.am-alarm-item.info .am-num { color: #5aa6c4; }
.am-alarm-item b { font-size: 11px; color: #c4d8e8; font-weight: 600; font-family:"SF Mono","Consolas",monospace; }
.am-alarm-item em { font-size: 9px; color: #5a7a98; font-style: normal; margin-left: auto; }

/* AI 研判结论 — 半透明黑 rgba(4,6,10,0.82) + 弱网格 + 圆角6px */
.am-judge { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.am-judge-list { overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 6px; }
.am-judge-list::-webkit-scrollbar { width: 3px; }
.am-judge-list::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.am-j-item {
  padding: 10px 12px;
  background: rgba(2,3,5,0.574);
  border: 1px solid rgba(62,170,255,0.08);
  border-radius: 6px;
  border-left: none;
  transition: all 0.2s; position: relative;
}
/* 左侧霓虹灯带 */
.am-j-item::before {
  content:''; width:3px; height:auto; align-self:stretch; flex-shrink:0;
  margin:4px 0; border-radius:2px;
  box-shadow:0 0 6px currentColor;
}
.am-j-item:hover { transform: translateX(2px); }
.am-j-item.danger { background-color: rgba(224,122,107,0.06); border-color: rgba(224,122,107,0.12); }
.am-j-item.danger::before { background:#e07a6b; color:#e07a6b; }
.am-j-item.warning { background-color: rgba(224,168,92,0.05); border-color: rgba(224,168,92,0.10); }
.am-j-item.warning::before { background:#e0a85c; color:#e0a85c; }
.am-j-item.info { background-color: rgba(90,166,196,0.04); border-color: rgba(90,166,196,0.10); }
.am-j-item.info::before { background:#5aa6c4; color:#5aa6c4; }
.am-j-item.primary { background-color: rgba(62,170,255,0.04); border-color: rgba(62,170,255,0.10); }
.am-j-item.primary::before { background:#3eaaff; color:#3eaaff; }
.am-j-item.success { background-color: rgba(91,184,138,0.04); border-color: rgba(91,184,138,0.10); }
.am-j-item.success::before { background:#5bb88a; color:#5bb88a; }
/* 紧急告警呼吸微光 */
.am-j-item.danger.ai-urg {
  animation: amUrgBreath 2.5s ease-in-out infinite;
  border-color: rgba(224,122,107,0.24);
}
@keyframes amUrgBreath {
  0%,100%{box-shadow:0 0 0 0 rgba(224,122,107,0);}
  50%{box-shadow:0 0 14px 2px rgba(224,122,107,0.14), inset 0 0 6px rgba(224,122,107,0.06);}
}
.am-j-top { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
/* 等级徽章 — 八角切角（暗饱和色） */
.am-j-sev { font-size: 9px; font-weight: 700; color: #fff; padding: 2px 7px; border-radius: 3px; flex-shrink: 0;
  clip-path: polygon(2px 0, 100% 0, 100% calc(100% - 2px), calc(100% - 2px) 100%, 2px 100%, 0 2px);
}
.am-j-title { font-size: 11px; font-weight: 600; color: #e2e8f0; flex: 1; min-width: 0;
  font-family:"SF Mono","Consolas",monospace;
}
/* 标记处置按钮 — 切角霓虹 */
.am-j-resolve { padding: 3px 11px; font-size: 10px; font-weight: 600;
  border: 1px solid rgba(62,170,255,0.28); background: rgba(62,170,255,0.07);
  color: #7ec8ff; cursor: pointer;
  clip-path: polygon(3px 0, calc(100% - 3px) 0, 100% 3px, 100% calc(100% - 3px), calc(100% - 3px) 100%, 3px 100%, 0 calc(100% - 3px), 0 3px);
  font-family:"SF Mono","Consolas",monospace; transition: all .2s; flex-shrink: 0;
}
.am-j-resolve:hover { border-color: rgba(62,170,255,0.55); background: rgba(62,170,255,0.16); color: #c8e8ff;
  box-shadow:inset 0 0 6px rgba(62,170,255,0.10);
}
.am-j-desc { font-size: 10px; color: #b0c4d8; line-height: 1.6; }
.am-j-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 5px; font-size: 9px; color: #8fa8c4; }
.am-j-meta b { font-family: "SF Mono","Consolas",monospace; text-shadow:0 0 4px currentColor; }
.am-j-src { color: #5a7a98; font-family:"SF Mono","Consolas",monospace; letter-spacing:0.3px; }
.am-j-empty { text-align: center; color: #5a8a98; font-size: 11px; padding: 16px 0;
  font-family:"SF Mono","Consolas",monospace;
}

/* ── 左下：关键运行参数 ── */
.meas-cell { grid-column: 1; grid-row: 2; }
.mw-meas-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 5px; overflow-y: auto; padding: 4px; flex: 1;
}
.mw-meas-grid::-webkit-scrollbar { width: 3px; }
.mw-meas-grid::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.15); border-radius: 2px; }
.mw-meas-i {
  display: flex; flex-direction: column; gap: 2px;
  padding: 7px 8px; border: 1px solid rgba(90,166,196,0.07);
  border-radius: 4px; background: rgba(3,8,15,0.21); transition: border-color .2s;
}
.mw-meas-i.over { border-color: rgba(239,68,68,0.2); background: rgba(20,6,8,0.175); }
.mw-mi-top { display: flex; justify-content: space-between; align-items: center; }
.mw-mi-name { font-size: 10px; color: #8fb0cf; font-weight: 600; }
.mw-mi-dot { width: 5px; height: 5px; border-radius: 50%; }
.mw-mi-dot.ok { background: #34d399; box-shadow: 0 0 4px rgba(52,211,153,0.5); }
.mw-mi-dot.bad { background: #f87171; box-shadow: 0 0 4px rgba(248,113,113,0.5); animation: pulse 1.5s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.4} }
.mw-mi-val { font-size: 14px; font-weight: 800; font-family: SF Mono, Consolas, monospace; color: #d4ecff; }
.mw-mi-u { font-size: 9px; color: #5a7a98; font-style: normal; margin-left: 2px; }
.mw-mi-bar { height: 3px; border-radius: 2px; background: rgba(90,166,196,0.08); overflow: hidden; }
.mw-mi-bar span { display: block; height: 100%; background: linear-gradient(90deg, #3eaaff, #22d3ee); transition: width .5s; border-radius: 2px; }
.mw-meas-i.over .mw-mi-bar span { background: linear-gradient(90deg, #f87171, #ef4444); }
.mw-mi-lim { font-size: 9px; color: #4a6a88; }

/* ── 中下：趋势图（继承 .cd 切角卡片） ── */
.trend-cell { grid-column: 2; grid-row: 2; display: flex; flex-direction: column; }
.mw-chart { flex: 1; min-height: 0; position:relative; z-index:1; }
.mw-trend-tools { display: flex; align-items: center; gap: 8px; margin-left: auto; }
/* 迷你切角按钮 */
.mw-mini-btn {
  padding: 2px 8px; font-size: 9px; font-weight: 600;
  border: 1px solid rgba(167,139,250,0.28); background: rgba(70,40,120,0.35);
  color: #c4b5fd; cursor: pointer; font-family:"SF Mono","Consolas",monospace; transition: all .2s;
  clip-path: polygon(2px 0, calc(100% - 2px) 0, 100% 2px, 100% calc(100% - 2px), calc(100% - 2px) 100%, 2px 100%, 0 calc(100% - 2px), 0 2px);
}
.mw-mini-btn:hover { border-color: rgba(167,139,250,0.55); background: rgba(90,55,150,0.45); }
.mw-leg { font-size: 10px; color: #8fb0cf; display: flex; align-items: center; gap: 4px; font-weight: 400; }
.mw-leg i { width: 14px; height: 3px; border-radius: 2px; display: inline-block; margin: 0 2px 0 6px; }

/* ══ 标签胶囊 — 暗饱和色板 + 切角 ══ */
.tg { display: inline-block; padding: 1px 7px; font-size: 10px; font-weight: 700;
  clip-path: polygon(2px 0, 100% 0, 100% calc(100% - 2px), calc(100% - 2px) 100%, 2px 100%, 0 2px);
  margin-right: 4px; font-family:"SF Mono","Consolas",monospace;
}
.tg-i { background: rgba(90,166,196,0.12); color: #5aa6c4; border:1px solid rgba(90,166,196,0.18); }
.tg-w { background: rgba(224,168,92,0.12); color: #e0a85c; border:1px solid rgba(224,168,92,0.18); }
.tg-ok { background: rgba(91,184,138,0.10); color: #5bb88a; border:1px solid rgba(91,184,138,0.16); }

.mw-ai-panel {
  position: absolute; top: 38px; left: 14px; right: 14px; bottom: 14px;
  background-color: rgba(2,5,10,0.672);
  background-image:
    linear-gradient(rgba(167,139,250,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(167,139,250,0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  border: 1px solid rgba(167,139,250,0.2);
  border-radius: 6px; padding: 16px 20px; z-index: 10; backdrop-filter: blur(8px);
}
.mw-ai-close { position: absolute; top: 8px; right: 10px; font-size: 20px; cursor: pointer; color: #6a7a98; }
.mw-ai-close:hover { color: #c4b5fd; }
.mw-ai-panel h4 { margin: 0 0 10px; color: #c4b5fd; font-size: 14px; letter-spacing: 1px; }
.mw-ai-body { font-size: 12px; color: #a8b8cc; line-height: 1.8; }
.mw-ai-body p { margin: 0 0 6px; }

/* ── 右下：AI研判结论详情 ── */
.judge-detail-cell { grid-column: 3; grid-row: 2; }
.jd-body { overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 6px; padding: 6px; }
.jd-body::-webkit-scrollbar { width: 3px; }
.jd-body::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.jd-item {
  padding: 10px 12px; border-radius: 4px; background: rgba(0,0,0,0.245);
  border-left: 3px solid; transition: all 0.2s;
}
.jd-item:hover { transform: translateX(2px); }
.jd-item.danger { border-color: #f87171; }
.jd-item.warning { border-color: #fbbf24; }
.jd.item.info { border-color: #22d3ee; }
.jd-item.primary { border-color: #3eaaff; }
.jd-item.success { border-color: #34d99e; }
.jd-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.jd-sev { font-size: 10px; font-weight: 700; color: #fff; padding: 1px 7px; border-radius: 3px; }
.jd-title { font-size: 12px; font-weight: 600; color: #e2e8f0; flex: 1; }
.jd-resolve {
  padding: 2px 10px; font-size: 10px; font-weight: 600;
  border: 1px solid rgba(62,170,255,0.3); background: rgba(62,170,255,0.08);
  color: #8eb8dc; cursor: pointer; border-radius: 3px;
  font-family: SF Mono, Consolas, monospace; transition: all .2s; flex-shrink: 0;
}
.jd-resolve:hover { border-color: rgba(62,170,255,0.6); background: rgba(62,170,255,0.18); color: #c8e8ff; }
.jd-desc { font-size: 11px; color: #cbd5e1; line-height: 1.7; }
.jd-meta { display: flex; justify-content: space-between; margin-top: 6px; font-size: 9px; color: #8fa8c4; }
.jd-meta b { font-family: SF Mono, Consolas, monospace; }
.jd-source { color: #5a7a98; }
.jd-empty { text-align: center; color: #5a8a98; font-size: 11px; padding: 24px 0; }

/* ═══ 底部 AI 综合分析通栏 ═══ */
.mw-ai-banner { flex: 0 0 58px; z-index: 2; }
.mw-ai-summary { display: flex; flex-direction: column; gap: 3px; overflow-y: auto; max-height: 36px; padding: 4px 0; }
.mw-ai-line { display: flex; align-items: center; gap: 8px; }
.mw-ai-tag {
  padding: 1px 7px; font-size: 9px; font-weight: 700; border-radius: 3px;
  font-family: SF Mono, Consolas, monospace; flex-shrink: 0;
}
.mw-ai-tag.ok { background: rgba(52,211,153,0.12); color: #34d399; }
.mw-ai-tag.bad { background: rgba(239,68,68,0.12); color: #f87171; }
.mw-ai-tag.warn { background: rgba(251,191,36,0.12); color: #fbbf24; }
.mw-ai-tag.info { background: rgba(34,206,238,0.12); color: #22d3ee; }
.mw-ai-tag.primary { background: rgba(62,170,255,0.12); color: #3eaaff; }
.mw-ai-txt { font-size: 11px; color: #a8b8cc; line-height: 1.4; }

.fade-enter-active,.fade-leave-active { transition: opacity .3s; }
.fade-enter-from,.fade-leave-to { opacity: 0; }

/* ══ 卡片暗色调兜底（scoped + !important 覆盖 global.css 的 .cd !important） ══ */
.mw-page .cd {
  background-color: #020305 !important;
  background-image: linear-gradient(180deg, #020305 0%, #010204 50%, #020305 100%) !important;
  border: 1px solid rgba(62,170,255,0.10) !important;
  border-radius: 6px !important;
  padding: 0 !important;
  display: flex; flex-direction: column; overflow: hidden;
  position: relative;
  transition: border-color 0.25s;
  box-shadow: none !important;
}
.mw-page .cd::before {
  content:''; position:absolute; inset:0;
  background-image:
    linear-gradient(rgba(62,170,255,0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(62,170,255,0.07) 1px, transparent 1px);
  background-size: 28px 28px;
  pointer-events:none; z-index:0;
  border: none !important;
  box-shadow: none !important;
}
.mw-page .cd::after {
  content:''; position:absolute; top:0; left:5%; right:5%; height:1px;
  background:linear-gradient(90deg, transparent, rgba(62,170,255,0.30), transparent);
  pointer-events:none; z-index:2;
  border: none !important;
  box-shadow: none !important;
}
.mw-page .cd:hover { border-color: rgba(62,170,255,0.22) !important; transform: none !important; }
/* 标题栏：仅中和因卡片 padding:0 导致的负边距错位，外观沿用 global.css 原样 */
.mw-page .cd-t { margin: 0 !important; }
.mw-page .cd .hud-tl,.mw-page .cd .hud-tr,
.mw-page .cd .hud-bl,.mw-page .cd .hud-br {
  position:absolute; width:14px; height:14px; pointer-events:none; z-index:3;
}
.hud-tl { top:0; left:0; border-top:2px solid rgba(62,170,255,0.45); border-left:2px solid rgba(62,170,255,0.45); }
.hud-tr { top:0; right:0; border-top:2px solid rgba(62,170,255,0.45); border-right:2px solid rgba(62,170,255,0.45); }
.hud-bl { bottom:0; left:0; border-bottom:2px solid rgba(62,170,255,0.45); border-left:2px solid rgba(62,170,255,0.45); }
.hud-br { bottom:0; right:0; border-bottom:2px solid rgba(62,170,255,0.45); border-right:2px solid rgba(62,170,255,0.45); }
.tg { display: inline-block; padding: 1px 7px; font-size: 10px; font-weight: 700; border-radius: 3px; margin-right: 4px; font-family: SF Mono, Consolas, monospace; }
.tg-i { background: rgba(34,206,238,0.15); color: #22d3ee; }
.tg-w { background: rgba(251,191,36,0.15); color: #fbbf24; }
.tg-ok { background: rgba(52,211,153,0.15); color: #34d399; }
</style>

<!-- 非 scoped 覆盖全局样式 — 深空黑 + CRT 网格 HUD（按用户规范） -->
<style>
/* ══ 卡片基础 — #04060a 深空黑 + CRT 网格覆盖 ══ */
.mw-page .cd {
  /* 第1层：纯黑底 #04060a */
  background-color: #04060a !important;
  /* 第2层：纯黑渐变底 */
  background-image:
    linear-gradient(180deg, #04060a 0%, #030508 50%, #04060a 100%) !important;
  /* 半透明黑 + 弱网格 */
  border: 1px solid rgba(62,170,255,0.10) !important;
  border-radius: 6px !important;
  padding: 0 !important;
  display: flex; flex-direction: column; overflow: hidden;
  position: relative;
  transition: border-color 0.25s;
  box-shadow: none !important;
}
/* CRT 网格覆盖（28px / 0.07）— 覆盖所有通用卡片 */
.mw-page .cd::before {
  content:''; position:absolute; inset:0;
  background-image:
    linear-gradient(rgba(62,170,255,0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(62,170,255,0.07) 1px, transparent 1px);
  background-size: 28px 28px;
  pointer-events:none; z-index:0;
  border: none !important;
  box-shadow: none !important;
}
/* 顶部微光扫描线 */
.mw-page .cd::after {
  content:''; position:absolute; top:0; left:5%; right:5%; height:1px;
  background:linear-gradient(90deg, transparent, rgba(62,170,255,0.30), transparent);
  pointer-events:none; z-index:2;
  border: none !important;
  box-shadow: none !important;
}
.mw-page .cd:hover {
  border-color: rgba(62,170,255,0.22);
}

/* 四角 L 型刻度（精简） */
.mw-page .cd .hud-tl,.mw-page .cd .hud-tr,
.mw-page .cd .hud-bl,.mw-page .cd .hud-br {
  position:absolute; width:14px; height:14px; pointer-events:none; z-index:3;
}
.hud-tl { top:0; left:0; border-top:2px solid rgba(62,170,255,0.45); border-left:2px solid rgba(62,170,255,0.45); }
.hud-tr { top:0; right:0; border-top:2px solid rgba(62,170,255,0.45); border-right:2px solid rgba(62,170,255,0.45); }
.hud-bl { bottom:0; left:0; border-bottom:2px solid rgba(62,170,255,0.45); border-left:2px solid rgba(62,170,255,0.45); }
.hud-br { bottom:0; right:0; border-bottom:2px solid rgba(62,170,255,0.45); border-right:2px solid rgba(62,170,255,0.45); }

/* ══ 标题栏 — 保留 global.css 原有样式，不覆盖 ══ */

.tg { display: inline-block; padding: 1px 7px; font-size: 10px; font-weight: 700; border-radius: 3px; margin-right: 4px; font-family: SF Mono, Consolas, monospace; }
.tg-i { background: rgba(34,206,238,0.15); color: #22d3ee; }
.tg-w { background: rgba(251,191,36,0.15); color: #fbbf24; }
.tg-ok { background: rgba(52,211,153,0.15); color: #34d399; }
</style>
