<template>
<div class="wa-page">
  <!-- 顶栏 -->
  <div class="wa-head">
    <div class="wa-head-l">
      <span class="wa-bar"></span>
      <h2>预警研判中心</h2>
      <span class="wa-sub">残差预警 · 波动预警 · 变化速率预警 · AI 趋势研判</span>
    </div>
    <div class="wa-head-r">
      <el-select v-model="selDevice" filterable style="width:200px" size="default">
        <el-option v-for="d in devs" :key="d.id" :value="d.id" :label="d.name + '（' + d.dept + '）'" />
      </el-select>
      <el-select v-model="selPoint" style="width:150px">
        <el-option v-for="p in points" :key="p" :value="p" :label="p" />
      </el-select>
      <el-button type="primary" @click="genJudge"><el-icon><MagicStick /></el-icon>生成研判</el-button>
    </div>
  </div>

  <!-- KPI -->
  <div class="wa-kpis">
    <div class="wa-kpi" v-for="k in kpis" :key="k.k" :style="{'--kc':k.c}">
      <div class="wa-kpi-ic">{{ k.ic }}</div>
      <div class="wa-kpi-l">{{ k.k }}</div>
      <div class="wa-kpi-v">{{ k.v }}<span class="wa-kpi-u">{{ k.u }}</span></div>
    </div>
  </div>

  <div class="wa-grid">
    <!-- 左：三类预警可视化 -->
    <div class="wa-col wa-col-l">
      <div class="cd wa-card" v-for="w in warnTypes" :key="w.key">
        <div class="cd-t"><span class="ut-ic">▸</span>{{ w.name }}
          <el-tag size="small" :type="w.level" effect="plain">{{ w.tag }}</el-tag>
        </div>
        <div class="wa-warn-body">
          <div :data-spark="w.key" class="wa-spark"></div>
          <div class="wa-warn-meta">
            <div class="wa-wm-row"><span>当前值</span><b :style="{color:w.c}">{{ w.cur }}</b></div>
            <div class="wa-wm-row"><span>阈值</span><b>{{ w.limit }}</b></div>
            <div class="wa-wm-row"><span>偏离</span><b :style="{color:w.c}">{{ w.dev }}</b></div>
            <div class="wa-wm-row"><span>状态</span>
              <b :style="{color:w.c}">
                <span class="wa-dot" :style="{background:w.c}"></span>{{ w.state }}
              </b>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中：实时值/预测值/设定值对比 -->
    <div class="cd wa-card wa-cmp">
      <div class="cd-t"><span class="ut-ic">▸</span>实时值 · 预测值 · 设定值 关联研判
        <span class="wa-cmp-legend">
          <i style="background:#3eaaff"></i>实时<i style="background:#a78bfa"></i>预测<i style="background:#fbbf24"></i>设定
        </span>
      </div>
      <div ref="cmpRef" class="wa-cmp-chart"></div>
      <div class="wa-cmp-note">
        <el-icon><InfoFilled /></el-icon>
        <span>当 <b style="color:#3eaaff">实时值</b> 持续偏离 <b style="color:#a78bfa">预测值</b> 形成残差累积，或变化速率突破设定带时，系统自动升级为对应类型预警。</span>
      </div>
    </div>

    <!-- 右：AI 研判结论卡片 -->
    <div class="wa-col wa-col-r">
      <div class="cd wa-card wa-judge">
        <div class="cd-t"><span class="ut-ic">▸</span>AI 预警趋势研判
          <span class="wa-judge-count" v-if="judges.length">{{ judges.length }} 条</span>
        </div>
        <div class="wa-judge-list">
          <div v-for="(j,i) in judges" :key="i" class="wa-j-item" :class="j.cls">
            <div class="wa-j-top">
              <span class="wa-j-sev" :style="{background:j.c}">{{ j.sev }}</span>
              <span class="wa-j-title">{{ j.title }}</span>
            </div>
            <div class="wa-j-body">{{ j.body }}</div>
            <div class="wa-j-foot">
              <span class="wa-j-et">预计触发 <b :style="{color:j.c}">{{ j.eta }}</b></span>
              <el-button link type="primary" size="small" @click="j.resolve=!j.resolve">
                {{ j.resolve ? '已处置 ✓' : '标记处置' }}
              </el-button>
            </div>
          </div>
          <div v-if="!judges.length" class="wa-j-empty">
            <el-icon><MagicStick /></el-icon>
            <p>点击右上角「生成研判」获取 AI 趋势研判结论</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from '@/utils/echarts'
import { useDataStore } from '@/stores/data'
import { MagicStick, InfoFilled } from '@element-plus/icons-vue'

const store = useDataStore()
const devs = computed(() => store.devices)
const selDevice = ref(store.devices.find(d => d.health < 90)?.id || store.devices[0]?.id)
const selPoint = ref('温度')

const device = computed(() => store.devices.find(d => d.id === selDevice.value) || store.devices[0])
const points = computed(() => Object.keys(device.value?.params || {}))
watch(device, () => { if (!points.value.includes(selPoint.value)) selPoint.value = points.value[0] })

// 生成合成时序（实时/预测/设定）
const series = computed(() => {
  const base = device.value?.params?.[selPoint.value]?.[0] || 50
  const limit = device.value?.params?.[selPoint.value]?.[1] || 80
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

// 三类预警指标
const warnTypes = computed(() => {
  const s = series.value
  const last = s.real[s.real.length - 1]
  const lim = s.limit
  // 残差 = 实时-预测 最近累积
  const resid = s.real.map((v, i) => +(v - s.pred[i]).toFixed(1))
  const residSum = resid.slice(-6).reduce((a, b) => a + Math.max(0, b), 0)
  // 波动 = 相邻差绝对值均值
  let fluct = 0
  for (let i = 1; i < s.real.length; i++) fluct += Math.abs(s.real[i] - s.real[i - 1])
  fluct = +(fluct / (s.real.length - 1)).toFixed(2)
  // 速率 = 近3点斜率均值
  const rate = +(((s.real[23] - s.real[20]) / 3) * 60).toFixed(1)
  return [
    { key: 'resid', name: '残差预警', ic: '∑', c: '#f87171', level: 'danger', tag: '一级', cur: residual2str(residSum), limit: '≤ 0', dev: '+' + residSum.toFixed(1), state: residSum > 2 ? '已触发' : '正常',
      data: resid },
    { key: 'fluct', name: '波动预警', ic: '∿', c: '#fbbf24', level: 'warning', tag: '二级', cur: fluct, limit: '≤ 1.5', dev: '+' + (fluct - 1.5).toFixed(2), state: fluct > 1.5 ? '已触发' : '正常',
      data: s.real },
    { key: 'rate', name: '变化速率预警', ic: '↗', c: '#22d3ee', level: 'info', tag: '预警', cur: rate + '/h', limit: '≤ 8/h', dev: (rate - 8 >= 0 ? '+' : '') + (rate - 8).toFixed(1), state: rate > 8 ? '已触发' : '正常',
      data: s.real.map((v, i) => i === 0 ? 0 : +((v - s.real[i - 1]) * 60).toFixed(1)) }
  ]
})
const residual2str = (v) => '+' + v.toFixed(1)

const kpis = computed(() => {
  const wt = warnTypes.value
  const fired = wt.filter(w => w.state === '已触发').length
  return [
    { k: '残差预警', v: wt[0].state === '已触发' ? 1 : 0, u: '项', c: '#f87171', ic: '∑' },
    { k: '波动预警', v: wt[1].state === '已触发' ? 1 : 0, u: '项', c: '#fbbf24', ic: '∿' },
    { k: '速率预警', v: wt[2].state === '已触发' ? 1 : 0, u: '项', c: '#22d3ee', ic: '↗' },
    { k: '研判结论', v: judges.value.length || 3, u: '条', c: '#34d399', ic: '◈' }
  ]
})

const judges = ref([])
const genJudge = () => {
  const wt = warnTypes.value
  const fired = wt.filter(w => w.state === '已触发')
  const arr = []
  if (fired.length) {
    fired.forEach(w => {
      arr.push({
        sev: w.tag, c: w.c, cls: w.level,
        title: w.name + '：' + device.value.name + ' ' + selPoint.value,
        body: `检测到 ${w.name} 已触发，当前值 ${w.cur}，偏离阈值 ${w.dev}。AI 判断该偏离呈持续累积趋势，若不干预将在未来时段扩大。`,
        eta: ['12分钟', '38分钟', '1.2小时', '2.5小时'][Math.floor(Math.random() * 4)]
      })
    })
  } else {
    arr.push({ sev: '提示', c: '#34d399', cls: 'success', title: '趋势平稳', body: `${device.value.name} ${selPoint.value} 三类预警均未触发，残差/波动/速率均在阈值带内，预计 2 小时内无升级风险。`, eta: '>2h' })
  }
  // 始终补充一条趋势研判
  arr.push({
    sev: '研判', c: '#3eaaff', cls: 'primary',
    title: 'AI 趋势研判：' + device.value.name,
    body: `基于近 24h 时序建模，${selPoint.value} 呈 ${(Math.random()>0.5?'上升':'缓降')}趋势，预测未来 1h 将${fired.length?'突破预警线':'维持安全区'}。建议${(Math.random()>0.5?'加强巡检频次至每 30min':'提前调整运行参数')}。`,
    eta: (40 + Math.floor(Math.random() * 80)) + '分钟'
  })
  judges.value = arr
}

// ============ 图表 ============
const cmpRef = ref(null)
let cmpCh = null
const sparkCharts = {}

/** 创建单个迷你图 */
function createSpark(el, key) {
  if (!el) return false
  const w = warnTypes.value.find(x => x.key === key)
  if (!w || !w.data || !w.data.length) return false
  if (sparkCharts[key]) { try { sparkCharts[key].dispose() } catch(e) {} }
  const ch = echarts.init(el)
  sparkCharts[key] = ch
  const color = w.c
  const isResid = key === 'resid'
  ch.setOption({
    backgroundColor: '#000000',
    grid: { left: 2, right: 2, top: 6, bottom: 2 },
    xAxis: { type: 'category', show: false, data: w.data.map((_, i) => i), boundaryGap: false },
    yAxis: { type: 'value', show: false, scale: true },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(8,20,40,0.92)', borderColor: color, textStyle: { color: '#c8e4ff', fontSize: 11 }, formatter: (p) => (isResid ? '残差 ' : '值 ') + p[0].data },
    series: [{
      type: 'line', smooth: true, symbol: 'none', data: w.data,
      lineStyle: { width: 2, color },
      areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color: hexA(color,0.35)},{offset:1,color:hexA(color,0.02)}]) }
    }]
  })
  return true
}

/** 初始化所有迷你图 — 通过 data-spark 属性查找 DOM */
function initSparks() {
  const keys = ['resid', 'fluct', 'rate']
  keys.forEach(key => {
    const el = document.querySelector('.wa-spark[data-spark="' + key + '"]')
    if (el) createSpark(el, key)
  })
}

/** 刷新数据（不重建实例） */
const refreshSparks = () => {
  Object.entries(sparkCharts).forEach(([key, ch]) => {
    if (!ch || ch.isDisposed()) return
    const w = warnTypes.value.find(x => x.key === key)
    if (!w || !w.data) return
    const color = w.c
    ch.setOption({
      xAxis: { data: w.data.map((_, i) => i) },
      series: [{
        data: w.data,
        lineStyle: { width: 2, color },
        areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color: hexA(color,0.35)},{offset:1,color:hexA(color,0.02)}]) }
      }]
    })
  })
}

const initCmp = () => {
  if (!cmpRef.value) return
  if (cmpCh) cmpCh.dispose()
  const s = series.value
  cmpCh = echarts.init(cmpRef.value)
  cmpCh.setOption({
    backgroundColor: '#000000',
    grid: { left: 48, right: 18, top: 24, bottom: 28 },
    legend: { textStyle: { color: '#9fb6cf', fontSize: 11 }, top: 0, right: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 4 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(8,20,40,0.92)', borderColor: 'rgba(62,170,255,0.3)', textStyle: { color: '#c8e4ff' } },
    xAxis: { type: 'category', boundaryGap: false, data: s.labels, axisLabel: { color: '#9fb6cf', fontSize: 10 }, axisLine: { lineStyle: { color: 'rgba(90,166,196,0.12)' } } },
    yAxis: { type: 'value', scale: true, axisLabel: { color: '#9fb6cf' }, splitLine: { lineStyle: { color: 'rgba(90,166,196,0.10)', type: 'dashed' } } },
    series: [
      { name: '实时', type: 'line', smooth: true, symbol: 'none', data: s.real, lineStyle: { width: 2.5, color: '#3eaaff' }, areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(62,170,255,0.25)'},{offset:1,color:'rgba(62,170,255,0)'}]) } },
      { name: '预测', type: 'line', smooth: true, symbol: 'none', data: s.pred, lineStyle: { width: 2, color: '#a78bfa', type: 'dashed' } },
      { name: '设定', type: 'line', smooth: true, symbol: 'none', data: s.set, lineStyle: { width: 2, color: '#fbbf24' }, markArea: { silent: true, itemStyle: { color: 'rgba(251,191,36,0.04)' }, data: [[{ yAxis: s.limit }, { yAxis: s.limit * 1.4 }]] }, markLine: { silent: true, symbol: 'none', lineStyle: { color: '#fbbf24', type: 'dotted' }, label: { formatter: '阈值带', color: '#fbbf24', fontSize: 10 }, data: [{ yAxis: s.limit }] } }
    ]
  })
}

function hexA(c, a) {
  const n = parseInt(c.slice(1), 16)
  return `rgba(${(n>>16)&255},${(n>>8)&255},${n&255},${a})`
}

const rz = () => { cmpCh?.resize(); Object.values(sparkCharts).forEach(c => c?.resize()) }

watch([selDevice, selPoint], () => {
  nextTick(() => { initCmp(); refreshSparks() })
})
onMounted(() => {
  nextTick(() => {
    initCmp()
    genJudge()
    // 双层 nextTick 确保 v-for 渲染完成、ref 已绑定
    nextTick(() => { initSparks() })
  })
  window.addEventListener('resize', rz)
})
onUnmounted(() => { window.removeEventListener('resize', rz); cmpCh?.dispose(); Object.values(sparkCharts).forEach(c => c?.dispose()) })
</script>

<style scoped>
.wa-page { display: flex; flex-direction: column; gap: 14px; height: 100%; overflow: hidden; background-color: #04060a; }
.wa-head { flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.wa-head-l { display: flex; align-items: center; gap: 12px; }
.wa-bar { width: 4px; height: 22px; background: linear-gradient(180deg, #3eaaff, #22d3ee); border-radius: 2px; box-shadow: 0 0 10px rgba(62,170,255,0.5); }
.wa-head h2 { font-size: 17px; font-weight: 600; color: #d4ecff; letter-spacing: 0.5px; }
.wa-sub { font-size: 12px; color: #8fb0cf; }
.wa-head-r { display: flex; gap: 10px; align-items: center; }

/* KPI */
.wa-kpis { flex-shrink: 0; display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.wa-kpi { display: flex; align-items: center; gap: 12px; padding: 14px 18px; background: rgba(4,6,10,0.85); border: 1px solid rgba(90,166,196,0.12); border-radius: 6px; overflow: hidden; position: relative; }
.wa-kpi::before { content:''; position:absolute; left:0; top:0; bottom:0; width:3px; background: var(--kc); box-shadow: 0 0 12px var(--kc); }
.wa-kpi-ic { font-size: 22px; color: var(--kc); filter: drop-shadow(0 0 6px var(--kc)); width: 28px; text-align: center; }
.wa-kpi-l { font-size: 12px; color: #9fb6cf; }
.wa-kpi-v { font-size: 28px; font-weight: 800; font-family: "SF Mono","Consolas",monospace; color: #fff; text-shadow: 0 0 12px rgba(62,170,255,0.3); }
.wa-kpi-u { font-size: 12px; color: #8fb0cf; margin-left: 3px; }

/* 网格 */
.wa-grid { flex: 1; min-height: 0; display: grid; grid-template-columns: 0.9fr 1.3fr 1fr; gap: 14px; }
.wa-col { display: flex; flex-direction: column; gap: 14px; min-height: 0; overflow-y: auto; }
.wa-col::-webkit-scrollbar { width: 3px; }
.wa-col::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.wa-card { flex-shrink: 0; }

.cd { background: linear-gradient(180deg, rgba(8,20,42,0.5), rgba(6,16,30,0.55)); border: 1px solid rgba(62,170,255,0.12); border-radius: 6px; padding: 16px; position: relative; }
.cd-t { font-size: 13px; font-weight: 600; color: #d4ecff; margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; gap: 8px; letter-spacing: 0.5px; }
.ut-ic { color: #3eaaff; font-weight: 700; margin-right: 2px; }

/* 三类预警卡片 */
.wa-warn-body { display: flex; gap: 10px; align-items: stretch; }
.wa-spark { width: 120px; height: 90px; flex-shrink: 0; background: rgba(0,0,0,0.7); border-radius: 6px; border: 1px solid rgba(90,166,196,0.10); position: relative; }
.wa-warn-meta { flex: 1; display: flex; flex-direction: column; justify-content: space-around; }
.wa-wm-row { display: flex; justify-content: space-between; font-size: 12px; color: #9fb6cf; }
.wa-wm-row b { font-family: monospace; font-weight: 700; color: #e2e8f0; display: flex; align-items: center; gap: 5px; }
.wa-dot { width: 7px; height: 7px; border-radius: 50%; box-shadow: 0 0 6px currentColor; }

/* 对比图 */
.wa-cmp { display: flex; flex-direction: column; }
.wa-cmp-legend { font-size: 11px; color: #8fb0cf; display: flex; align-items: center; gap: 4px; font-weight: 400; }
.wa-cmp-legend i { width: 14px; height: 3px; border-radius: 2px; display: inline-block; margin: 0 2px 0 8px; }
.wa-cmp-chart { flex: 1; min-height: 220px; }
.wa-cmp-note { display: flex; gap: 6px; font-size: 11.5px; color: #8fb0cf; line-height: 1.6; padding: 10px 12px; background: rgba(0,0,0,0.35); border-radius: 6px; border: 1px solid rgba(90,166,196,0.08); align-items: flex-start; }
.wa-cmp-note .el-icon { color: #3eaaff; margin-top: 2px; flex-shrink: 0; }

/* 研判卡片 */
.wa-judge { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.wa-judge-count { font-size: 11px; color: #34d399; background: rgba(52,211,153,0.12); padding: 1px 8px; border-radius: 10px; }
.wa-judge-list { flex: 1; overflow-y: auto; min-height: 0; display: flex; flex-direction: column; gap: 10px; }
.wa-judge-list::-webkit-scrollbar { width: 3px; }
.wa-judge-list::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.wa-j-item { padding: 12px 14px; border-radius: 6px; background: rgba(0,0,0,0.4); border-left: 3px solid; transition: all 0.2s; }
.wa-j-item:hover { transform: translateX(2px); }
.wa-j-item.danger { border-color: #f87171; background: rgba(248,113,113,0.05); }
.wa-j-item.warning { border-color: #fbbf24; background: rgba(251,191,36,0.05); }
.wa-j-item.info { border-color: #22d3ee; background: rgba(34,211,238,0.05); }
.wa-j-item.primary { border-color: #3eaaff; background: rgba(62,170,255,0.05); }
.wa-j-item.success { border-color: #34d399; background: rgba(52,211,153,0.05); }
.wa-j-top { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.wa-j-sev { font-size: 10px; font-weight: 700; color: #fff; padding: 1px 7px; border-radius: 3px; }
.wa-j-title { font-size: 13px; font-weight: 600; color: #e2e8f0; }
.wa-j-body { font-size: 12px; color: #cbd5e1; line-height: 1.7; }
.wa-j-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; font-size: 11.5px; color: #9fb6cf; }
.wa-j-et b { font-family: monospace; }
.wa-j-empty { text-align: center; padding: 40px 20px; color: #6a8caa; }
.wa-j-empty .el-icon { font-size: 32px; color: #3eaaff; margin-bottom: 10px; }
.wa-j-empty p { font-size: 12px; }

@media(max-width: 1400px){
  .wa-grid { grid-template-columns: 1fr; overflow-y: auto; }
  .wa-col { overflow: visible; }
}
</style>

<!-- 非 scoped 覆盖块：压制全局 .cd !important 深蓝锁色 -->
<style>
/* 所有面板内容区纯黑 + CRT 虚线方格 */
.wa-page .wa-grid > .cd {
  background-color: #000000 !important;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px) !important;
  border-color: rgba(90,166,196,0.12) !important;
  box-shadow: none !important;
}
/* 标题栏不透明深蓝底，挡住网格 */
.wa-page .wa-grid > .cd > .cd-t {
  background-color: #06121c !important;
  background-image: none !important;
}
</style>
