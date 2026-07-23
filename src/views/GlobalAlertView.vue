<template>
<div class="bs">
  <!-- 全屏边框 -->
  <div class="bs-frame">
    <span class="fc tl"></span><span class="fc tr"></span>
    <span class="fc bl"></span><span class="fc br"></span>
    <span class="ft"></span><span class="fb"></span>
  </div>

  <!-- 顶栏 -->
  <div class="bs-head">
    <div class="bs-head-l">
      <div class="bs-title-bar">
        <i class="bs-ti-ind"></i>
        <span class="bs-title">火电运行全域告警监控大屏</span>
        <i class="bs-ti-seg"></i>
      </div>
      <div class="bs-sub">{{ store.selectedUnit.name }} · {{ store.selectedUnit.capacity }}MW · {{ store.selectedUnit.type }}</div>
    </div>
    <div class="bs-head-r">
      <div class="bs-switch">
        <button class="bs-sw" :class="{on: activeTab==='monitor'}" @click="activeTab='monitor'"><i class="bs-sw-ic"></i>设备监控</button>
        <button class="bs-sw" :class="{on: activeTab==='analysis'}" @click="activeTab='analysis'"><i class="bs-sw-ic"></i>智能分析</button>
      </div>
      <div class="bs-clock"><i class="bs-clock-lbl">系统时间</i>{{ clock }}</div>
    </div>
  </div>

  <!-- 机组选择 -->
  <div class="bs-unit-bar">
    <div v-for="u in store.units" :key="u.id" class="bs-u-chip" :class="{on: store.selectedUnitId===u.id}" @click="switchUnit(u.id)">
      <span class="bs-u-n">{{ u.name }}</span>
      <span v-if="store.unitAlarms(u.id).length" class="bs-u-a">{{ store.unitAlarms(u.id).length }}</span>
    </div>
  </div>

  <!-- ====== 设备监控面板 ====== -->
  <div v-show="activeTab==='monitor'" class="mon-layout">

    <!-- 第一行: KPI指标 + 实时参数监测 并排，无缝衔接 -->
    <div class="mon-top-row">
      <!-- 左侧: KPI 六宫格 -->
      <div class="kpi-grid">
        <div class="kpi-hex" v-for="(m,i) in kpiData" :key="i">
          <div class="kpi-icon" :style="{color: m.c}">
            <svg viewBox="0 0 60 60" width="52" height="52">
              <polygon points="30,2 56,16 56,44 30,58 4,44 4,16"
                       fill="rgba(8,30,60,0.5)" stroke="#3eaaff" stroke-width="1.2" opacity="0.9"/>
              <polygon points="30,7 51,19 51,41 30,53 9,41 9,19"
                       fill="none" stroke="#3eaaff" stroke-width="0.6" opacity="0.5" stroke-dasharray="2 2"/>
              <text x="30" y="34" text-anchor="middle" :fill="m.c" font-size="14" font-weight="700">{{ m.v }}</text>
            </svg>
          </div>
          <div class="kpi-l">{{ m.k }}</div>
        </div>
      </div>

      <!-- 右侧: 实时参数监测 — 紧凑横向卡片阵列 -->
      <div class="rtm-panel">
        <div class="rtm-head">
          <i class="ms-ti-ind"></i>
          <span class="rtm-t">设备状态实时监测</span>
          <span class="cc-live">LIVE</span>
          <span class="rtm-unit">{{ store.selectedUnit.name }}</span>
        </div>
        <div class="rtm-grid">
          <div class="rtm-card" v-for="(p,i) in rtParams" :key="i" :data-ac="p.c">
            <div class="rtm-card-inner">
              <div class="rtm-label">{{ p.k }}</div>
              <div class="rtm-val" :style="{color: p.c}">{{ p.v }}<span class="rtm-unit-s">{{ p.u }}</span></div>
              <div class="rtm-sub" :class="p.st==='normal'?'ok':p.st==='warn'?'warn':'bad'">
                <i class="rtm-dot"></i>{{ p.s }}
              </div>
            </div>
            <div class="rtm-glow"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二行: 设备健康度矩阵 — 全宽 -->
    <div class="matrix-screen">
      <div class="ms-panel">
        <span class="pp-c tl"></span><span class="pp-c tr"></span>
        <span class="pp-c bl"></span><span class="pp-c br"></span>
        <div class="ms-title">
          <i class="ms-ti-ind"></i>
          <span class="ms-ti-t">设备健康度矩阵</span>
          <i class="ms-ti-seg"></i>
          <span class="ms-ti-sub">{{ unitDevices.length }} / {{ totalSlots }} 位 · 已配置/总槽位</span>
          <div class="ms-ti-legend">
            <span class="ml"><i class="ml-d ok"></i>健康≥90</span>
            <span class="ml"><i class="ml-d w"></i>关注80~90</span>
            <span class="ml"><i class="ml-d b"></i>异常&lt;80</span>
            <span class="ml"><i class="ml-d e"></i>未配置</span>
          </div>
        </div>
        <div class="ms-body">
          <div class="matrix-bg"></div>
          <div class="matrix-grid" :style="{width:gridW+'px',height:gridH+'px'}">
            <div v-for="(c,idx) in hCells" :key="c.d?c.d.id:'e'+idx"
                 class="hc" :class="[c.cl, c.empty?'e':'']"
                 :style="c.st"
                 @click="c.d && goDevice(c.d)">
              <template v-if="!c.empty">
                <div class="hc-icon"><svg viewBox="0 0 60 60" width="40" height="40"><polygon points="30,4 54,17 54,43 30,56 6,43 6,17" :fill="hexFill(c.cl)" :stroke="hexStroke(c.cl)" stroke-width="2"/><text x="30" y="37" text-anchor="middle" :fill="hexText(c.cl)" font-size="22" font-weight="800">{{ tLet(c.d) }}</text></svg></div>
                <div class="hc-n" :title="c.d.name">{{ sNm(c.d.name) }}</div>
                <div class="hc-hl" :class="c.cl">{{ c.d.health.toFixed(1) }}</div>
              </template>
              <template v-else>
                <div class="hc-empty">
                  <svg viewBox="0 0 60 60" width="56" height="56">
                    <polygon points="30,5 53,18 53,42 30,55 7,42 7,18" fill="rgba(8,18,32,0.3)" stroke="rgba(60,120,180,0.22)" stroke-width="1" stroke-dasharray="4 3"/>
                    <polygon points="30,11 48,21 48,39 30,49 12,39 12,21" fill="none" stroke="rgba(60,120,180,0.15)" stroke-width="0.5"/>
                    <text x="30" y="38" text-anchor="middle" fill="rgba(110,170,230,0.7)" font-size="22" font-weight="700">?</text>
                  </svg>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 第三行: 图表左右并排 — 充分利用宽度 -->
    <div class="chart-row">
      <div class="cc-panel chart-flex">
        <span class="pp-c tl"></span><span class="pp-c tr"></span>
        <span class="pp-c bl"></span><span class="pp-c br"></span>
        <div class="cc-title">
          <i class="ms-ti-ind"></i><span>设备实时参数趋势</span>
          <span class="cc-live">LIVE</span>
          <i class="ms-ti-seg right"></i>
        </div>
        <div ref="rtRef" class="cc-body chart-body-tall"></div>
      </div>
      <div class="cc-panel chart-flex">
        <span class="pp-c tl"></span><span class="pp-c tr"></span>
        <span class="pp-c bl"></span><span class="pp-c br"></span>
        <div class="cc-title">
          <i class="ms-ti-ind"></i><span>近24小时告警趋势</span>
          <div class="cc-leg">
            <span><i style="background:#f87171"></i>一级</span>
            <span><i style="background:#fbbf24"></i>二级</span>
            <span><i style="background:#22d3ee"></i>预警</span>
          </div>
          <i class="ms-ti-seg right"></i>
        </div>
        <div ref="atRef" class="cc-body chart-body-tall"></div>
      </div>
    </div>
  </div>

  <!-- ====== 智能分析面板 ====== -->
  <div v-show="activeTab==='analysis'" class="ana-layout">

    <!-- 概览 banner -->
    <div class="ana-banner">
      <div class="ab-tile" v-for="(m,i) in anaSummary" :key="i">
        <span class="ab-l">{{ m.k }}</span>
        <span class="ab-v" :style="{color: m.c}">{{ m.v }}</span>
        <span class="ab-s">{{ m.s }}</span>
      </div>
    </div>

    <!-- 行: TOP5 + 填充统计 -->
    <div class="cc-panel ana-row-block">
      <span class="pp-c tl"></span><span class="pp-c tr"></span>
      <span class="pp-c bl"></span><span class="pp-c br"></span>
      <div class="cc-title"><i class="ms-ti-ind"></i>重点关注设备 TOP<span class="ana-num">{{ topWarn.length }}</span><i class="ms-ti-seg right"></i></div>
      <div class="ana-body-fill">
      <table class="ana-tb">
        <thead><tr><th>设备</th><th>专业</th><th>问题</th><th>健康度</th><th>建议</th></tr></thead>
        <tbody>
          <tr v-for="d in topWarn" :key="d.id" :class="d.health<70?'rb':d.health<80?'rw':''">
            <td><span class="ti" :class="tCls(d)">{{ tLet(d) }}</span>{{ d.name }}</td>
            <td class="gm">{{ d.dept }}</td>
            <td :class="d.health<70?'rd':d.health<80?'yw':''">{{ d.problem }}</td>
            <td><span class="hlt" :class="d.health>=90?'ok':d.health>=80?'w':'bd'">{{ d.health.toFixed(1) }}</span></td>
            <td class="gm fs-xs">{{ d.suggest }}</td>
          </tr>
          <tr v-if="!topWarn.length"><td colspan="5" class="mt">所有设备运行正常</td></tr>
        </tbody>
      </table>
      <div class="ana-filler" v-if="unitDevices.length<=20">
        <div class="af-h"><span class="af-t">▶ 设备专业监视覆盖</span><span class="af-sum">{{ unitDevices.length }} / {{ totalSlots }} 位 · 装机率 {{ Math.round(unitDevices.length/Math.max(1,totalSlots)*100) }}%</span></div>
        <div class="af-bars">
          <div class="af-b" v-for="dept in deptDist" :key="dept.k">
            <div class="af-bi"><span class="af-bn">{{ dept.k }}</span><span class="af-bv">{{ dept.v }} 台</span></div>
            <div class="af-bg"><span class="af-bf" :style="{width:dept.p+'%', background:dept.c}"></span></div>
          </div>
        </div>
        <div class="af-stats">
          <div class="af-s"><span class="af-sl">监测点</span><span class="af-sv">{{ Object.keys(unitDevices).length * 5 + 12 }}</span><span class="af-su">个</span></div>
          <div class="af-s"><span class="af-sl">日采集</span><span class="af-sv">2.4</span><span class="af-su">万条</span></div>
          <div class="af-s"><span class="af-sl">模型检测</span><span class="af-sv">98.6</span><span class="af-su">%准确</span></div>
          <div class="af-s"><span class="af-sl">闭环率</span><span class="af-sv">{{ Math.round(unitAlarms.filter(a=>a.st!=='unhandled').length/Math.max(1,unitAlarms.length)*100) }}</span><span class="af-su">%</span></div>
        </div>
      </div>
      </div>
    </div>

    <!-- AI洞察 -->
    <div class="cc-panel ana-row-block">
      <span class="pp-c tl"></span><span class="pp-c tr"></span>
      <span class="pp-c bl"></span><span class="pp-c br"></span>
      <div class="cc-title"><i class="ms-ti-ind"></i>AI 智能洞察<i class="ms-ti-seg right"></i></div>
      <div class="ai-insights-row">
        <div v-for="(ins,i) in insights" :key="i" class="aii" :class="ins.type">
          <i class="ii-d" :class="ins.type"></i>
          <div class="ii-b">
            <div class="ii-t">{{ ins.title }}</div>
            <div class="ii-x gm">{{ ins.content }}</div>
          </div>
        </div>
        <div class="aii-fill">
          <div class="aii-fill-h">▶ 常规运行建议（基于 AI 模型历史训练）</div>
          <div class="aii-fill-grid">
            <div class="aii-fill-i"><span class="aii-fill-d ok"></span><span>温度巡检: 每 8h 记录关键测点</span></div>
            <div class="aii-fill-i"><span class="aii-fill-d ok"></span><span>振动监测: 巡检周期 ≤ 4h</span></div>
            <div class="aii-fill-i"><span class="aii-fill-d warn"></span><span>润滑系统: 按上次维护 + 2000h</span></div>
            <div class="aii-fill-i"><span class="aii-fill-d warn"></span><span>磨辊间隙: 每班次检查 1 次</span></div>
            <div class="aii-fill-i"><span class="aii-fill-d info"></span><span>脱硝效率: 实时跟踪均值</span></div>
            <div class="aii-fill-i"><span class="aii-fill-d info"></span><span>环保排放: NOx/SO2 限值 70%</span></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部三列: 分布 + 最近告警 + 时效 -->
    <div class="ana-duo-row">
      <div class="cc-panel">
        <span class="pp-c tl"></span><span class="pp-c tr"></span>
        <span class="pp-c bl"></span><span class="pp-c br"></span>
        <div class="cc-title"><i class="ms-ti-ind"></i>告警专业分布<i class="ms-ti-seg right"></i></div>
        <div class="dist-flex">
          <div ref="pfRef" class="dist-chart"></div>
          <div class="dist-legend">
            <div class="dl-i" v-for="(d,i) in profDist" :key="i">
              <i class="dl-d" :style="{background:d.c}"></i>
              <div class="dl-b">
                <div class="dl-n"><span>{{ d.n }}</span><span class="dl-v">{{ d.v }}</span></div>
                <div class="dl-bar"><span :style="{width:d.p+'%', background:d.c}"></span></div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="cc-panel">
        <span class="pp-c tl"></span><span class="pp-c tr"></span>
        <span class="pp-c bl"></span><span class="pp-c br"></span>
        <div class="cc-title"><i class="ms-ti-ind"></i>最近告警 <span class="ana-num">{{ recent.length }}</span><i class="ms-ti-seg right"></i></div>
        <div class="alarm-grid">
          <div v-for="a in recent.slice(0,6)" :key="a.id" class="ait-card" :class="a.l===1?'l1':a.l===2?'l2':'l3'">
            <div class="ait-top">
              <span class="ait-dev">{{ a.device }}</span>
              <span class="ait-st" :class="a.st==='unhandled'?'sn':a.st==='confirmed'?'sa':'sd'">{{ stTxt(a.st) }}</span>
              <span class="ait-tm mono">{{ fmt(a.time) }}</span>
            </div>
            <div class="ait-desc">{{ a.desc }}</div>
          </div>
        </div>
      </div>
      <div class="cc-panel">
        <span class="pp-c tl"></span><span class="pp-c tr"></span>
        <span class="pp-c bl"></span><span class="pp-c br"></span>
        <div class="cc-title"><i class="ms-ti-ind"></i>告警处理时效<i class="ms-ti-seg right"></i></div>
        <div class="sl-grid">
          <div class="sl-i" v-for="(s,i) in slaStats" :key="i" :class="s.cl">
            <div class="sl-l">{{ s.k }}</div>
            <div class="sl-v" :style="{color: s.c}">{{ s.v }}<span class="sl-u">{{ s.u }}</span></div>
            <div class="sl-bg" :style="{width: s.p+'%', background: s.c}"></div>
          </div>
        </div>
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
const activeTab = ref('monitor')
const clock = ref('')
const rtRef = ref(null), atRef = ref(null), pfRef = ref(null)
let rtCh, atCh, pfCh, iv

const switchUnit = (id) => { store.selectedUnitId = id; nextTick(initCharts) }
const goDevice = (d) => {
  store.selectedDevice = d.id
  router.push({ path: '/alarms', query: { deviceId: d.id, deviceName: d.name } })
}

const unitDevices = computed(() => store.unitDevices(store.selectedUnitId))
const unitAlarms = computed(() => store.unitAlarms(store.selectedUnitId))
const cnt = (l) => unitAlarms.value.filter(a => a.l === l).length
const avgHealth = computed(() => {
  const ds = unitDevices.value
  if (!ds.length) return 0
  return (ds.reduce((s,d)=>s+d.health,0)/ds.length).toFixed(1)
})

// KPI 数据
const kpiData = computed(()=>{
  const ua=unitAlarms.value, un=ua.filter(a=>a.st==='unhandled').length, ah=Number(avgHealth.value)
  return [
    {k:'总告警',v:ua.length,c:'#5fb3ff'},
    {k:'一级报警',v:cnt(1),c:'#f87171'},
    {k:'二级报警',v:cnt(2),c:'#fbbf24'},
    {k:'智能预警',v:cnt(3),c:'#22d3ee'},
    {k:'未处理',v:un,c:'#f87171'},
    {k:'平均健康度',v:ah+'%',c:ah>=90?'#34d399':ah>=80?'#fbbf24':'#f87171'}
  ]
})

// ========== 实时参数数据 — 紧凑卡片展示 ==========
const rtParams = computed(() => {
  const base = [
    {k:'主汽温度', v:(535+Math.random()*12).toFixed(1), u:'°C', c:'#f87171', st:'normal', s:'正常范围 535~548'},
    {k:'主汽压力', v:(16.4+Math.random()*0.6).toFixed(2), u:'MPa', c:'#5fb3ff', st:'normal', s:'额定 16.7MPa'},
    {k:'机组负荷', v:(480+Math.random()*30).toFixed(0), u:'MW', c:'#34d399', st:'normal', s:'经济负荷区间'},
    {k:'给水流量', v:(1480+Math.random()*80).toFixed(0), u:'t/h', c:'#22d3ee', st:'warn', s:'偏高 +3.2%'},
    {k:'汽轮机振动', v:(42+Math.random()*18).toFixed(1), u:'μm', c:'#fbbf24', st:'normal', s:'≤80 合格'},
    {k:'真空度', v:(94.5+Math.random()*1.5).toFixed(1), u:'kPa', c:'#a78bfa', st:'normal', s:'≥93 正常'},
    {k:'再热汽温', v:(538+Math.random()*8).toFixed(1), u:'°C', c:'#f87171', st:'normal', s:'目标 540°C'},
    {k:'排烟温度', v:(125+Math.random()*8).toFixed(0), u:'°C', c:'#fb923c', st:'warn', s:'偏高 +5°C 优化燃烧'},
    {k:'炉膛负压', v:(-68+Math.random()*20-10).toFixed(0), u:'Pa', c:'#5fb3ff', st:'normal', s:'-50~-100 Pa'},
    {k:'含氧量', v:(3.2+Math.random()*1.2).toFixed(1), u:'%', c:'#34d399', st:'normal', s:'3~5% 最佳'}
  ]
  return base.map(p => ({...p, v: typeof p.v === 'number' ? p.v : p.v}))
})

// 蜂窝矩阵配置
const HEX_W = 120
const HEX_H = 96
const HEX_GAP = 8
const COLS = 10
const ROWS = 8
const hCells = computed(()=>{
  const devs = unitDevices.value
  const arr = []
  let di = 0
  for (let r=0; r<ROWS; r++) {
    const n = r%2===0 ? COLS : COLS-1
    const off = r%2===0 ? 0 : (HEX_W+HEX_GAP)/2
    for (let c=0; c<n; c++) {
      const dev = di < devs.length ? devs[di] : null
      const h = dev ? dev.health : 0
      arr.push({
        d: dev,
        empty: !dev,
        cl: h>=90?'ok':h>=80?'warn':h>0?'bad':'',
        st:{ left: c*(HEX_W+HEX_GAP)+off+'px', top: r*(HEX_H*0.82+HEX_GAP)+'px' }
      })
      di++
    }
  }
  return arr
})
const gridW = computed(() => COLS*(HEX_W+HEX_GAP)+(HEX_W+HEX_GAP)/2+20)
const gridH = computed(() => ROWS*(HEX_H*0.82+HEX_GAP)+20)
const totalSlots = computed(() => hCells.value.length)

function tCls(d){
  const n=d.name||''
  if(n.includes('风机'))return'tc-f';if(n.includes('磨煤机'))return'tc-m'
  if(n.includes('泵'))return'tc-p';if(n.includes('油'))return'tc-o'
  if(n.includes('凝汽器'))return'tc-c';if(n.includes('加热器'))return'tc-h'
  if(n.includes('除氧器'))return'tc-d';return'tc-g'
}
function tLet(d){
  const n=d.name||''
  if(n.includes('风机'))return'F';if(n.includes('磨煤机'))return'M'
  if(n.includes('泵'))return'P';if(n.includes('油'))return'O'
  if(n.includes('凝汽器'))return'C';if(n.includes('加热器'))return'H'
  if(n.includes('除氧器'))return'D';return'G'
}
function hexStroke(cl) {
  return cl==='ok' ? '#34d399' : cl==='warn' ? '#fbbf24' : cl==='bad' ? '#f87171' : '#3eaaff'
}
function hexFill(cl) {
  return cl==='ok' ? 'rgba(52,211,153,0.15)' : cl==='warn' ? 'rgba(251,191,36,0.15)' : cl==='bad' ? 'rgba(248,113,113,0.2)' : 'rgba(62,170,255,0.12)'
}
function hexText(cl) {
  return cl==='ok' ? '#3af4a0' : cl==='warn' ? '#ffd042' : cl==='bad' ? '#ff6464' : '#3eaaff'
}
function sNm(n){return n.length<=5?n:n.replace(/[ABCD]/g,'').slice(0,5)}

const fmt=(t)=>{const d=new Date(t);return d.toLocaleTimeString('zh-CN',{hour12:false}).slice(0,8)}
const stTxt=(s)=>s==='unhandled'?'未处理':s==='confirmed'?'已确认':'已处理'

const recent=computed(()=>unitAlarms.value.slice(0,10))

const topWarn=computed(()=>{
  return unitDevices.value.filter(d=>d.health<90).sort((a,b)=>a.health-b.health).slice(0,5).map(d=>({
    ...d,
    problem:d.health<70?'严重异常，建议立即停机检查':d.health<80?'健康度偏低，需安排检修':'部分参数超限',
    suggest:d.dept==='汽轮机'?'检查轴承润滑与振动':d.name.includes('磨')?'检查磨辊磨损':'加强巡检频次'
  }))
})

const deptDist = computed(()=>{
  const m = { 锅炉:{n:0,c:'#fbbf24'}, 汽轮机:{n:0,c:'#5fb3ff'}, 辅网:{n:0,c:'#34d399'} }
  unitDevices.value.forEach(d=>{ const dp = d.dept; if (m[dp]) m[dp].n++; else m[dp] = { n:1, c:'#8fb0cf' } })
  const total = Math.max(1, Object.values(m).reduce((s,v)=>s+v.n,0))
  return Object.entries(m).map(([k,v])=>({ k, v:v.n, c:v.c, p: Math.round(v.n/total*100) }))
})

const insights=computed(()=>{
  const o=[], ds=unitDevices.value
  const lh=ds.filter(d=>d.health<80)
  if(lh.length)o.push({type:'bad',title:'设备健康度告警',content:`${store.selectedUnit.name} 有 ${lh.length} 台设备健康度低于 80，建议立即启动预防性检修流程。`})
  const l1=unitAlarms.value.filter(a=>a.l===1&&a.st==='unhandled')
  if(l1.length)o.push({type:'bad',title:'一级报警未处置',content:`${l1.length} 条一级报警处于未处理状态，超时将自动升级。`})
  const idf=ds.find(d=>d.name==='A引风机'&&d.health<85)
  if(idf)o.push({type:'warn',title:'A引风机温度预警',content:'轴承温度偏离正常值，温升速率加快。建议48h内安排润滑油脂更换。'})
  const ml=ds.find(d=>d.name.includes('磨煤机')&&d.health<85)
  if(ml)o.push({type:'warn',title:'磨煤机振动预警',content:'磨辊磨损不均导致动不平衡。建议降低给煤量至70%。'})
  if(!o.length)o.push({type:'ok',title:'系统状态正常',content:`${store.selectedUnit.name} 各设备运行平稳，暂未检测到需关注的告警。`})
  return o
})

const profDist = computed(()=>{
  const pc = { 锅炉:0, 汽轮机:0, 辅网:0 }
  unitAlarms.value.forEach(a=>{ pc[a.dept] = (pc[a.dept]||0) + 1 })
  const total = Math.max(1, Object.values(pc).reduce((s,v)=>s+v,0))
  return [
    { n:'锅炉', v:pc['锅炉']||0, p:((pc['锅炉']||0)/total*100).toFixed(0), c:'#fbbf24' },
    { n:'汽轮机', v:pc['汽轮机']||0, p:((pc['汽轮机']||0)/total*100).toFixed(0), c:'#5fb3ff' },
    { n:'辅网', v:pc['辅网']||0, p:((pc['辅网']||0)/total*100).toFixed(0), c:'#34d399' }
  ].filter(d=>d.v>0)
})

const slaStats = computed(()=>{
  const ua = unitAlarms.value
  const total = Math.max(1, ua.length)
  const l1 = ua.filter(a=>a.l===1), l2 = ua.filter(a=>a.l===2), l3 = ua.filter(a=>a.l===3)
  const l1h = l1.filter(a=>a.st!=='unhandled').length, l2h = l2.filter(a=>a.st!=='unhandled').length
  const l3h = l3.filter(a=>a.st!=='unhandled').length
  const overall = ua.filter(a=>a.st!=='unhandled').length
  return [
    { k:'一级 1分钟内', v:l1.length?Math.round(l1h/l1.length*100):100, u:'%', p:l1.length?l1h/l1.length*100:100, c:'#f87171', cl:'danger' },
    { k:'二级 2小时内', v:l2.length?Math.round(l2h/l2.length*100):100, u:'%', p:l2.length?l2h/l2.length*100:100, c:'#fbbf24', cl:'warn' },
    { k:'智能预警 24h', v:l3.length?Math.round(l3h/l3.length*100):100, u:'%', p:l3.length?l3h/l3.length*100:100, c:'#22d3ee', cl:'info' },
    { k:'整体闭环率', v:Math.round(overall/total*100), u:'%', p:overall/total*100, c:'#34d399', cl:'ok' }
  ]
})

const anaSummary = computed(()=>{
  const ua = unitAlarms.value
  const un = ua.filter(a=>a.st==='unhandled').length
  const ah = Number(avgHealth.value)
  return [
    { k:'当前告警总数', v: ua.length, c:'#5fb3ff', s:'本机组' },
    { k:'未处置告警', v: un, c:'#f87171', s:'需立即响应' },
    { k:'异常设备', v: unitDevices.value.filter(d=>d.health<80).length, c:'#fbbf24', s:'健康度 < 80' },
    { k:'关注设备', v: unitDevices.value.filter(d=>d.health>=80 && d.health<90).length, c:'#22d3ee', s:'健康度 80-90' },
    { k:'已处理占比', v: ua.length?Math.round(ua.filter(a=>a.st!=='unhandled').length/ua.length*100):100, c:'#34d399', s:'闭环率 %' },
    { k:'平均健康度', v: ah, c: ah>=90?'#34d399':ah>=80?'#fbbf24':'#f87171', s:'设备平均 %' }
  ]
})

// ========== 图表初始化 — 专业 3D 风格 ==========
const initCharts=()=>{
  if(rtCh)rtCh.dispose();if(atCh)atCh.dispose();if(pfCh)pfCh.dispose()

  // ---- 实时参数趋势图 — 多层渐变 + 光晕线条 ----
  if(rtRef.value){
    rtCh=echarts.init(rtRef.value)
    const gd1={type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(248,113,113,0.35)'},{offset:1,color:'rgba(248,113,113,0.02)'}]}
    const gd2={type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(95,179,255,0.28)'},{offset:1,color:'rgba(95,179,255,0.02)'}]}
    const gd3={type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(52,211,153,0.28)'},{offset:1,color:'rgba(52,211,153,0.02)'}]}
    const now=Date.now()
    const times=Array.from({length:30},(_,i)=>{const d=new Date(now-(29-i)*2000);return d.toLocaleTimeString('zh-CN',{hour12:false}).slice(0,8)})
    rtCh.setOption({
      backgroundColor: 'transparent',
      grid:{left:50,right:20,top:28,bottom:32},
      tooltip:{
        trigger:'axis',
        backgroundColor:'rgba(6,14,28,0.96)',
        borderColor:'rgba(62,170,255,0.25)',
        borderWidth:1,
        textStyle:{color:'#c8dae8',fontSize:11},
        extraCssText:'box-shadow: 0 4px 20px rgba(0,0,0,0.4), backdrop-filter: blur(8px);'
      },
      xAxis:{
        type:'category',data:times,
        axisLabel:{color:'#8fb0cf',fontSize: 11,interval:4,fontFamily:'"SF Mono","Consolas",monospace'},
        axisLine:{lineStyle:{color:'rgba(60,140,210,0.18)',width:1}},
        axisTick:{show:false},
        splitLine:{show:false}
      },
      yAxis:{
        type:'value',
        axisLabel:{color:'#8fb0cf',fontSize: 11},
        splitLine:{lineStyle:{color:'rgba(60,140,210,0.06)',type:'dashed',width:1}}
      },
      series:[
        {
          name:'主汽温度',type:'line',smooth:true,symbolSize:6,
          data:Array.from({length:30},()=>535+Math.random()*12),
          lineStyle:{color:'#f87171',width:3,shadowBlur:18,shadowColor:'rgba(248,113,113,0.45)',shadowOffsetY:3},
          itemStyle:{color:'#f87171',borderColor:'#fff',borderWidth:2,shadowBlur:8,shadowColor:'rgba(248,113,113,0.5)'},
          areaStyle:{color:gd1},
          symbol:'circle',
          emphasis:{lineStyle:{width:4},itemStyle:{scale:true,symbolSize:10},areaStyle:{color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(248,113,113,0.5)'},{offset:1,color:'rgba(248,113,113,0.05)'}]}}}
        },
        {
          name:'负荷',type:'line',smooth:true,symbolSize:6,
          data:Array.from({length:30},()=>480+Math.random()*30),
          lineStyle:{color:'#5fb3ff',width:3,shadowBlur:18,shadowColor:'rgba(95,179,255,0.45)',shadowOffsetY:3},
          itemStyle:{color:'#5fb3ff',borderColor:'#fff',borderWidth:2,shadowBlur:8,shadowColor:'rgba(95,179,255,0.5)'},
          areaStyle:{color:gd2},
          symbol:'circle',
          emphasis:{lineStyle:{width:4},itemStyle:{scale:true,symbolSize:10}}
        },
        {
          name:'主汽压力',type:'line',smooth:true,symbolSize:6,
          data:Array.from({length:30},()=>16.4+Math.random()*0.6),
          lineStyle:{color:'#34d399',width:3,shadowBlur:18,shadowColor:'rgba(52,211,153,0.45)',shadowOffsetY:3},
          itemStyle:{color:'#34d399',borderColor:'#fff',borderWidth:2,shadowBlur:8,shadowColor:'rgba(52,211,153,0.5)'},
          areaStyle:{color:gd3},
          symbol:'circle',
          emphasis:{lineStyle:{width:4},itemStyle:{scale:true,symbolSize:10}}
        }
      ],
      animationDuration: 1200,
      animationEasing: 'cubicOut'
    },true)
  }

  // ---- 24h告警趋势 — 3D柱状 + 发光折线 ----
  if(atRef.value){
    atCh=echarts.init(atRef.value)
    atCh.setOption({
      backgroundColor: 'transparent',
      grid:{left:50,right:20,top:36,bottom:32},
      legend:{textStyle:{color:'#7aa0c0',fontSize: 11},top:4,right:0,itemWidth:14,itemHeight:4,itemGap:12},
      tooltip:{
        trigger:'axis',
        backgroundColor:'rgba(6,14,28,0.96)',
        borderColor:'rgba(62,170,255,0.25)',
        borderWidth:1,
        textStyle:{color:'#c8dae8',fontSize:11},
        extraCssText:'box-shadow: 0 4px 20px rgba(0,0,0,0.4);'
      },
      xAxis:{
        type:'category',data:Array.from({length:24},(_,i)=>i+':00'),
        axisLabel:{color:'#8fb0cf',fontSize: 11,interval:3,fontFamily:'"SF Mono","Consolas",monospace'},
        axisLine:{lineStyle:{color:'rgba(60,140,210,0.18)',width:1}},
        axisTick:{show:false}
      },
      yAxis:{
        type:'value',
        axisLabel:{color:'#8fb0cf',fontSize: 11},
        splitLine:{lineStyle:{color:'rgba(60,140,210,0.06)',type:'dashed'}}
      },
      series:[
        {
          name:'一级',type:'bar',stack:'alarm',
          data:Array.from({length:24},()=>Math.random()>0.75?Math.floor(Math.random()*3):0),
          itemStyle:{
            color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#f87171'},{offset:0.5,color:'#ef4444'},{offset:1,color:'#dc2626'}]),
            borderRadius:[4,4,0,0],
            shadowBlur:10,shadowColor:'rgba(248,113,113,0.35)',shadowOffsetY:2
          },
          barWidth:'58%',
          emphasis:{itemStyle:{shadowBlur:18,shadowColor:'rgba(248,113,113,0.55)'}}
        },
        {
          name:'二级',type:'bar',stack:'alarm',
          data:Array.from({length:24},()=>Math.floor(Math.random()*5)),
          itemStyle:{
            color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#fcd34d'},{offset:0.5,color:'#f59e0b'},{offset:1,color:'#d97706'}]),
            borderRadius:[4,4,0,0],
            shadowBlur:10,shadowColor:'rgba(251,191,36,0.35)',shadowOffsetY:2
          },
          emphasis:{itemStyle:{shadowBlur:18,shadowColor:'rgba(251,191,36,0.55)'}}
        },
        {
          name:'智能预警',type:'line',smooth:true,symbolSize:7,
          data:Array.from({length:24},()=>4+Math.random()*8),
          lineStyle:{color:'#22d3ee',width:3,shadowBlur:16,shadowColor:'rgba(34,211,238,0.5)',shadowOffsetY:2},
          itemStyle:{color:'#22d3ee',borderColor:'#fff',borderWidth:2,shadowBlur:10,shadowColor:'rgba(34,211,238,0.6)'},
          areaStyle:{color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(34,211,238,0.22)'},{offset:1,color:'rgba(34,211,238,0.01)'}]}},
          symbol:'circle',
          emphasis:{lineStyle:{width:4},itemStyle:{scale:true,symbolSize:11}}
        }
      ],
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    },true)
  }

  // ---- 饼图 — 3D立体感 ----
  if(pfRef.value){
    pfCh=echarts.init(pfRef.value)
    const pc={'锅炉':0,'汽轮机':0,'辅网':0}
    unitAlarms.value.forEach(a=>{pc[a.dept]=(pc[a.dept]||0)+1})
    pfCh.setOption({
      backgroundColor: 'transparent',
      tooltip:{trigger:'item',formatter:'{b}: {c} ({d}%)',backgroundColor:'rgba(6,14,28,0.96)',borderColor:'rgba(62,170,255,0.25)',borderWidth:1,textStyle:{color:'#c8dae8',fontSize:11}},
      series:[{
        type:'pie',radius:['38%','72%'],center:['50%','52%'],
        avoidLabelOverlap:true,
        itemStyle:{
          borderColor:'rgba(6,18,48,0.85)',
          borderWidth:4,
          borderRadius:6,
          shadowBlur:20,
          shadowColor:'rgba(0,0,0,0.5)',
          shadowOffsetY:4
        },
        emphasis:{
          scale:true,scaleSize:6,
          itemStyle:{shadowBlur:30,shadowColor:'rgba(0,0,0,0.65)'}
        },
        label:{color:'#a8c4dc',fontSize:12,fontWeight:500,textShadowColor:'rgba(0,0,0,0.4)',textShadowBlur:3},
        labelLine:{lineStyle:{color:'rgba(62,170,255,0.2)',width:1},length:12,length2:8},
        data:[
            {value:pc['锅炉'],name:'锅炉',itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#fcd34d'},{offset:1,color:'#f59e0b'}])}},
            {value:pc['汽轮机'],name:'汽机',itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#60a5fa'},{offset:1,color:'#3b82f6'}])}},
            {value:pc['辅网'],name:'辅网',itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#4ade80'},{offset:1,color:'#22c55e'}])}}
        ].filter(d=>d.value>0),
        animationType:'expansion',
        animationEasing:'elasticOut'
      }]
    },true)
  }
}

watch(()=>store.selectedUnitId,()=>nextTick(initCharts))
watch(activeTab,(v)=>{ if(v==='analysis'){ nextTick(()=>setTimeout(()=>{ initCharts(); handleRz() },80)) } else { nextTick(initCharts) } })

onMounted(()=>{
  clock.value=new Date().toLocaleString('zh-CN',{hour12:false})
  iv=setInterval(()=>{clock.value=new Date().toLocaleString('zh-CN',{hour12:false})},1000)
  nextTick(initCharts)
  window.addEventListener('resize',handleRz)
})
onUnmounted(()=>{clearInterval(iv);rtCh?.dispose();atCh?.dispose();pfCh?.dispose();window.removeEventListener('resize',handleRz)})
const handleRz=()=>{rtCh?.resize();atCh?.resize();pfCh?.resize()}
</script>

<style scoped>
/* ================================================================
   全域告警监控大屏 — 专业工业数据平台风格 v3
   设计原则: 无空区 / 3D图表 / 紧凑布局 / 视觉层次分明
   ================================================================ */

/* ========== 全局基底 ========== */
.bs {
  min-height: 100vh;
  padding: 16px 20px;
  background:
    radial-gradient(ellipse 1200px 550px at 20% -5%, rgba(20,75,140,0.12), transparent 65%),
    radial-gradient(ellipse 900px 450px at 80% 105%, rgba(12,85,150,0.08), transparent 60%),
    linear-gradient(180deg, #040810 0%, #060e1c 40%, #040a15 100%);
  position: relative;
  color: #c0d4e8;
  overflow-x: hidden;
}
.bs::before {
  content:''; position:absolute; inset:0; pointer-events:none;
  background-image:
    linear-gradient(rgba(60,140,210,0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(60,140,210,0.02) 1px, transparent 1px);
  background-size: 56px 56px;
}

/* 全屏边框 */
.bs-frame { position:absolute; inset:0; pointer-events:none; z-index:1; }
.fc { position:absolute; width:18px; height:18px; border:2px solid #3eaaff; box-shadow:0 0 8px rgba(62,170,255,0.5); }
.fc.tl { top:6px; left:6px; border-right:none; border-bottom:none; }
.fc.tr { top:6px; right:6px; border-left:none; border-bottom:none; }
.fc.bl { bottom:6px; left:6px; border-right:none; border-top:none; }
.fc.br { bottom:6px; right:6px; border-left:none; border-top:none; }
.ft { position:absolute; left:50%; top:0; width:220px; height:1.5px; transform:translateX(-50%); background:linear-gradient(90deg,transparent,#3eaaff,transparent); box-shadow:0 0 8px rgba(62,170,255,0.5); }
.fb { position:absolute; left:50%; bottom:0; width:220px; height:1.5px; transform:translateX(-50%); background:linear-gradient(90deg,transparent,#3eaaff,transparent); }

/* ========== 顶栏 ========== */
.bs-head { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:12px; position:relative; z-index:5; }
.bs-head-l { display:flex; flex-direction:column; gap:5px; }
.bs-title-bar { display:flex; align-items:center; gap:10px; padding:6px 14px; background:linear-gradient(90deg,rgba(62,170,255,0.18),transparent); border-left:3px solid #3eaaff; box-shadow:-8px 0 16px -4px rgba(62,170,255,0.2); }
.bs-ti-ind { width:0; height:0; border-left:8px solid #3eaaff; border-top:5px solid transparent; border-bottom:5px solid transparent; filter:drop-shadow(0 0 4px rgba(62,170,255,0.6)); flex-shrink:0; }
.bs-title { font-size:22px; font-weight:700; letter-spacing:2px; color:#e8f4ff; text-shadow:0 0 10px rgba(62,170,255,0.3); }
.bs-ti-seg { margin-left:auto; width:60px; height:8px; background:repeating-linear-gradient(90deg,#3eaaff 0 4px,transparent 4px 6px); }
.bs-sub { font-size:12px; color:#8fb0cf; letter-spacing:0.5px; padding-left:14px; }

.bs-head-r { display:flex; align-items:center; gap:14px; }
.bs-switch { display:flex; gap:6px; }
.bs-sw { padding:8px 18px; font-size:13px; font-weight:500; border:1px solid rgba(62,170,255,0.2); background:rgba(8,20,40,0.5); color:#8ab4d4; cursor:pointer; transition:all 0.25s; position:relative; clip-path:polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px)); }
.bs-sw-ic { display:inline-block; width:6px; height:6px; background:#8fb0cf; margin-right:6px; transform:rotate(45deg); }
.bs-sw.on { background:rgba(62,170,255,0.2); border-color:#3eaaff; color:#a8d4ff; box-shadow:0 0 10px rgba(62,170,255,0.2); }
.bs-sw.on .bs-sw-ic { background:#3eaaff; box-shadow:0 0 6px #3eaaff; }
.bs-sw:hover:not(.on) { border-color:rgba(62,170,255,0.4); color:#a8d4ff; }

.bs-clock { display:flex; flex-direction:column; align-items:flex-end; gap:2px; }
.bs-clock-lbl { font-size:11px; color:#8fb0cf; letter-spacing:1px; }
.bs-clock { font-size:14px; color:#a8d4ff; font-family:"SF Mono","Consolas",monospace; text-shadow:0 0 8px rgba(62,170,255,0.25); }

/* 机组选择 */
.bs-unit-bar { display:flex; gap:10px; margin-bottom:12px; position:relative; z-index:5; }
.bs-u-chip { display:flex; align-items:center; gap:8px; padding:7px 16px; background:rgba(8,20,40,0.6); border:1px solid rgba(62,170,255,0.15); cursor:pointer; transition:all 0.25s; font-size:13px; clip-path:polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px)); }
.bs-u-chip:hover { border-color:rgba(62,170,255,0.4); background:rgba(20,40,75,0.5); }
.bs-u-chip.on { border-color:#3eaaff; background:rgba(20,60,110,0.4); box-shadow:0 0 12px rgba(62,170,255,0.2); }
.bs-u-n { font-weight:600; color:#c8e0f4; letter-spacing:0.5px; }
.bs-u-a { min-width:18px; height:18px; border-radius:3px; background:rgba(248,113,113,0.2); color:#f87171; font-size:11px; font-weight:700; display:inline-flex; align-items:center; justify-content:center; border:1px solid rgba(248,113,113,0.3); }

/* ================================================================
   监控面板布局 — 核心改动: KPI + 实时参数 并排无空隙
   ================================================================ */
.mon-layout { display:flex; flex-direction:column; gap:12px; position:relative; z-index:5; }

/* 顶部行: KPI(左) | 实时参数(右) */
.mon-top-row {
  display:grid;
  grid-template-columns: 320px 1fr;
  gap:12px;
  align-items:stretch;
}

/* KPI 网格 — 2×3 紧凑排列 */
.kpi-grid {
  display:grid;
  grid-template-columns: repeat(3, 1fr);
  gap:8px;
  background: linear-gradient(180deg, rgba(10,26,50,0.55), rgba(6,16,32,0.6));
  border:1px solid rgba(62,170,255,0.12);
  padding:12px;
  position:relative;
  box-shadow: 0 1px 3px rgba(0,0,0,0.3), 0 4px 14px rgba(0,10,30,0.4);
}
.kpi-grid::before {
  content:''; position:absolute; top:-1px; left:-1px; width:12px; height:12px;
  border-top:1.5px solid #3eaaff; border-left:1.5px solid #3eaaff;
  box-shadow: -1px -1px 4px rgba(62,170,255,0.25);
}
.kpi-grid::after {
  content:''; position:absolute; bottom:-1px; right:-1px; width:12px; height:12px;
  border-bottom:1.5px solid #3eaaff; border-right:1.5px solid #3eaaff;
  box-shadow: 1px 1px 4px rgba(62,170,255,0.25);
}

.kpi-hex { display:flex; flex-direction:column; align-items:center; gap:4px; padding:8px 4px; transition:all 0.25s; position:relative; }
.kpi-hex:hover { transform:translateY(-2px); }
.kpi-hex::before { content:''; position:absolute; inset:0; border-radius:4px; opacity:0; transition:opacity 0.25s; background:radial-gradient(ellipse at center, rgba(62,170,255,0.08), transparent 70%); }
.kpi-hex:hover::before { opacity:1; }
.kpi-icon { display:flex; align-items:center; justify-content:center; }
.kpi-l { font-size:11px; color:#8fb0cf; letter-spacing:0.5px; white-space:nowrap; }

/* ========== 实时参数监测面板 — 核心: 替代径向仪表盘 ========== */
.rtm-panel {
  background: linear-gradient(180deg, rgba(10,26,50,0.55), rgba(6,16,32,0.6));
  border:1px solid rgba(62,170,255,0.12);
  padding:0;
  display:flex; flex-direction:column;
  overflow:hidden;
  position:relative;
  box-shadow:
    0 1px 3px rgba(0,0,0,0.3),
    0 4px 14px rgba(0,10,30,0.4),
    inset 0 1px 0 rgba(62,170,255,0.04);
}
.rtm-panel::before, .rtm-panel::after { content:''; position:absolute; width:14px; height:14px; border:1.5px solid #3eaaff; box-shadow:0 0 6px rgba(62,170,255,0.45); pointer-events:none; z-index:2; }
.rtm-panel::before { top:-1px; left:-1px; border-right:none; border-bottom:none; }
.rtm-panel::after { bottom:-1px; right:-1px; border-left:none; border-top:none; }

/* 实时参数标题栏 */
.rtm-head {
  display:flex; align-items:center; gap:8px;
  padding:8px 14px;
  background:linear-gradient(90deg, rgba(62,170,255,0.15), rgba(62,170,255,0.03));
  border-bottom:1px solid rgba(62,170,255,0.08);
  flex-shrink:0;
}
.rtm-t { font-size:14px; font-weight:600; color:#e0f0ff; letter-spacing:1px; }
.rtm-unit { margin-left:auto; font-size:11px; color:#8fb0cf; font-family:"SF Mono","Consolas",monospace; }

/* 参数卡片网格 — 自适应填充 */
.rtm-grid {
  display:grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap:6px;
  padding:10px 12px 12px;
  flex:1;
  align-content:start;
}

/* 单个参数卡片 — 玻璃态 + 色彩边框发光 */
.rtm-card {
  position:relative;
  background:linear-gradient(135deg, rgba(8,20,40,0.6), rgba(6,14,28,0.7));
  border:1px solid rgba(62,170,255,0.1);
  border-radius:5px;
  padding:10px 12px;
  overflow:hidden;
  transition:all 0.3s cubic-bezier(0.4,0,0.2,1);
  cursor:default;
}
.rtm-card:hover {
  border-color:attr(data-ac);
  transform:translateY(-2px);
  box-shadow:0 6px 20px rgba(0,10,30,0.4);
}
/* 卡片顶部色条 */
.rtm-card::before {
  content:''; position:absolute; top:0; left:0; right:0; height:2px;
  background:linear-gradient(90deg, attr(data-ac), transparent 80%);
  opacity:0.8;
}
/* 卡片内部光晕 */
.rtm-glow {
  position:absolute; top:-50%; right:-50%; width:100%; height:200%;
  background:radial-gradient(ellipse at center, rgba(62,170,255,0.06), transparent 70%);
  pointer-events:none; opacity:0;
  transition:opacity 0.4s;
}
.rtm-card:hover .rtm-glow { opacity:1; }

.rtm-card-inner { position:relative; z-index:1; }
.rtm-label { font-size:11px; color:#8fb0cf; letter-spacing:0.5px; margin-bottom:4px; text-transform:uppercase; }
.rtm-val { font-size:22px; font-weight:700; font-family:"SF Mono","Consolas",monospace; line-height:1.1; text-shadow:0 2px 8px rgba(0,0,0,0.3); }
.rtm-unit-s { font-size:11px; font-weight:400; opacity:0.6; margin-left:2px; }
.rtm-sub { font-size:11px; margin-top:4px; display:flex; align-items:center; gap:4px; }
.rtm-dot { width:5px; height:5px; border-radius:50%; flex-shrink:0; }
.rtm-sub.ok { color:#34d399; } .rtm-sub.ok .rtm-dot { background:#34d399; box-shadow:0 0 6px rgba(52,211,153,0.5); }
.rtm-sub.warn { color:#fbbf24; } .rtm-sub.warn .rtm-dot { background:#fbbf24; box-shadow:0 0 6px rgba(251,191,36,0.5); }
.rtm-sub.bad { color:#f87171; } .rtm-sub.bad .rtm-dot { background:#f87171; box-shadow:0 0 6px rgba(248,113,113,0.5); animation:dotBlink 1.5s infinite; }
@keyframes dotBlink { 0%,100%{opacity:1} 50%{opacity:.3} }

/* ========== 设备矩阵 ========== */
.matrix-screen { position:relative; z-index:5; }
.ms-panel {
  position:relative;
  background:linear-gradient(180deg, rgba(10,26,50,0.55) 0%, rgba(6,16,32,0.6) 100%);
  border:1px solid rgba(62,170,255,0.12);
  box-shadow:0 1px 3px rgba(0,0,0,0.3), 0 6px 22px rgba(0,10,30,0.4), inset 0 1px 0 rgba(62,170,255,0.04);
  padding:14px;
}
.pp-c { position:absolute; width:14px; height:14px; border:1.5px solid #3eaaff; box-shadow:0 0 6px rgba(62,170,255,0.45); }
.pp-c.tl { top:-1px; left:-1px; border-right:none; border-bottom:none; }
.pp-c.tr { top:-1px; right:-1px; border-left:none; border-bottom:none; }
.pp-c.bl { bottom:-1px; left:-1px; border-right:none; border-top:none; }
.pp-c.br { bottom:-1px; right:-1px; border-left:none; border-top:none; }

.ms-title { display:flex; align-items:center; gap:10px; margin-bottom:12px; padding:8px 12px; background:linear-gradient(90deg, rgba(62,170,255,0.15), rgba(62,170,255,0.02)); border-left:3px solid #3eaaff; }
.ms-ti-ind { width:0; height:0; border-left:8px solid #3eaaff; border-top:5px solid transparent; border-bottom:5px solid transparent; filter:drop-shadow(0 0 4px rgba(62,170,255,0.6)); flex-shrink:0; }
.ms-ti-t { font-size:17px; font-weight:700; color:#e0f0ff; letter-spacing:1.5px; text-shadow:0 0 6px rgba(62,170,255,0.25); }
.ms-ti-seg { width:40px; height:8px; background:repeating-linear-gradient(90deg,#3eaaff 0 3px,transparent 3px 5px); }
.ms-ti-seg.right { margin-left:auto; }
.ms-ti-sub { font-size:11px; color:#8fb0cf; padding-left:8px; border-left:1px solid rgba(62,170,255,0.2); margin-left:8px; }
.ms-ti-legend { display:flex; gap:14px; margin-left:auto; }
.ml { display:flex; align-items:center; gap:6px; font-size:12px; color:#a8c4dc; font-weight:500; }
.ml-d { width:10px; height:10px; border-radius:50%; flex-shrink:0; }
.ml-d.ok { background:#34d399; box-shadow:0 0 6px rgba(52,211,153,0.5); }
.ml-d.w { background:#fbbf24; box-shadow:0 0 6px rgba(251,191,36,0.5); }
.ml-d.b { background:#f87171; box-shadow:0 0 6px rgba(248,113,113,0.5); }
.ml-d.e { background:transparent; border:1px dashed rgba(80,140,180,0.45); box-shadow:none; }

.ms-body { position:relative; padding:10px 0; overflow-x:auto; overflow-y:hidden; }
.matrix-bg { position:absolute; inset:10px 0; background:radial-gradient(ellipse 80% 60% at 50% 50%, rgba(62,170,255,0.06), transparent 60%); pointer-events:none; }
.matrix-grid { position:relative; margin:0 auto; }

.hc { position:absolute; width:120px; height:96px; display:flex; flex-direction:column; align-items:center; justify-content:center; cursor:pointer; transition:transform 0.3s,filter 0.3s; gap:1px; padding:4px 6px; }
.hc-icon { display:flex; align-items:center; justify-content:center; filter:drop-shadow(0 0 6px rgba(62,170,255,0.2)); }
.hc-icon svg { width:40px; height:40px; }
.hc-empty svg { width:56px; height:56px; }
.hc-n { font-size:14px; font-weight:700; color:#e8f4ff; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:96px; text-align:center; letter-spacing:0.5px; margin-top:2px; text-shadow:0 1px 2px rgba(0,0,0,0.5); }
.hc-hl { font-size:17px; font-weight:800; font-family:"SF Mono","Consolas",monospace; line-height:1.1; letter-spacing:0.5px; }
.hc-hl.ok { color:#3af4a0; text-shadow:0 0 8px rgba(52,211,153,0.55); }
.hc-hl.warn { color:#ffd042; text-shadow:0 0 8px rgba(251,191,36,0.55); }
.hc-hl.bad { color:#ff6464; text-shadow:0 0 8px rgba(248,113,113,0.65); }
.hc:not(.e):hover { transform:translateY(-4px) scale(1.08); z-index:10; filter:drop-shadow(0 0 16px rgba(62,170,255,0.5)) drop-shadow(0 4px 12px rgba(0,0,0,0.4)); }
.hc.e { cursor:pointer; opacity:0.75; filter:brightness(0.7); }

/* ========== 图表行 — 左右并排 ========== */
.chart-row {
  display:grid;
  grid-template-columns: 1fr 1fr;
  gap:12px;
  position:relative; z-index:5;
}
.chart-flex { display:flex; flex-direction:column; min-height:0; overflow:hidden; }

.cc-panel {
  position:relative;
  background: linear-gradient(180deg, rgba(10,24,45,0.55), rgba(6,16,30,0.6));
  border:1px solid rgba(62,170,255,0.1);
  padding:12px 14px;
  box-shadow:0 1px 3px rgba(0,0,0,0.3), 0 4px 14px rgba(0,8,24,0.4), inset 0 1px 0 rgba(62,170,255,0.04);
  transition: all 0.25s cubic-bezier(0.4,0,0.2,1);
}
.cc-panel:hover { border-color: rgba(62,170,255,0.2); box-shadow: 0 1px 3px rgba(0,0,0,0.3), 0 6px 20px rgba(0,12,30,0.45), inset 0 1px 0 rgba(62,170,255,0.06); }

.cc-title { display:flex; align-items:center; gap:10px; margin-bottom:10px; padding:9px 14px 9px 18px; background:linear-gradient(90deg, rgba(62,170,255,0.15), rgba(62,170,255,0.02)); border-left:4px solid #3eaaff; font-size:14px; font-weight:600; color:#e0f0ff; letter-spacing:1px; min-height:36px; box-shadow:-4px 0 12px -3px rgba(62,170,255,0.2); position:relative; flex-shrink:0; }
.cc-title > span { flex:0 0 auto; }
.cc-live { font-size:9px; font-weight:700; color:#f87171; background:rgba(248,113,113,0.15); padding:1px 6px; border-radius:2px; border:1px solid rgba(248,113,113,0.3); animation:lvB 2s infinite; letter-spacing:1px; margin-left:6px; }
@keyframes lvB { 0%,100%{opacity:1} 50%{opacity:.3} }
.cc-leg { display:flex; gap:10px; margin-left:auto; font-size:11px; color:#8fb0cf; }
.cc-leg i { width:10px; height:3px; border-radius:2px; display:inline-block; }
.cc-body { min-height:130px; padding:0 4px; }
.chart-body-tall { min-height:240px; }

/* ========== 智能分析面板 ========== */
.ana-layout { display:flex; flex-direction:column; gap:12px; position:relative; z-index:5; min-height:0; }

.ana-banner { display:grid; grid-template-columns:repeat(6, 1fr); gap:10px; position:relative; z-index:2; }
.ab-tile {
  position:relative;
  display:flex; flex-direction:column; align-items:center; justify-content:center; gap:3px;
  padding:12px 6px; min-height:68px;
  background:linear-gradient(180deg, rgba(8,24,48,0.55), rgba(6,16,32,0.55));
  border:1px solid rgba(62,170,255,0.18); border-radius:5px;
  transition:all 0.25s; overflow:hidden;
}
.ab-tile::before, .ab-tile::after { content:''; position:absolute; width:8px; height:8px; border:1.5px solid #3eaaff; pointer-events:none; }
.ab-tile::before { top:3px; left:3px; border-right:none; border-bottom:none; }
.ab-tile::after { bottom:3px; right:3px; border-left:none; border-top:none; }
.ab-tile:hover { border-color:rgba(62,170,255,0.35); transform:translateY(-1px); }
.ab-l { font-size:11px; color:#8fb0cf; letter-spacing:0.5px; }
.ab-v { font-size:24px; font-weight:700; font-family:"SF Mono","Consolas",monospace; line-height:1.1; }
.ab-s { font-size:11px; color:#8fb0cf; }

.ana-row-block { display:flex; flex-direction:column; min-height:280px; overflow:hidden; }
.ana-row-block > .cc-title { flex-shrink:0; }
.ana-body-fill { flex:1; min-height:0; overflow:auto; padding-right:4px; }

/* AI洞察 */
.ai-insights-row { display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:10px; padding:8px 4px 0; }
.aii { display:flex; gap:10px; padding:12px 14px; border-radius:4px; border-left:3px solid; transition:transform 0.15s; background:rgba(8,20,40,0.4); }
.aii:hover { transform:translateX(2px); }
.aii.bad { background:rgba(248,113,113,0.04); border-color:rgba(248,113,113,0.5); }
.aii.warn { background:rgba(251,191,36,0.04); border-color:rgba(251,191,36,0.5); }
.aii.ok { background:rgba(52,211,153,0.04); border-color:rgba(52,211,153,0.5); }

.ana-duo-row { display:grid; grid-template-columns:1.4fr 0.8fr 1fr; gap:12px; min-height:240px; }
.dist-flex { display:grid; grid-template-columns:1fr 1fr; gap:12px; padding:8px 12px 12px; align-items:center; min-height:200px; }
.dist-chart { min-height:200px; }
.dist-legend { display:flex; flex-direction:column; gap:14px; padding:0 6px; }
.dl-i { display:flex; align-items:center; gap:10px; }
.dl-d { width:4px; height:32px; border-radius:2px; flex-shrink:0; box-shadow:0 0 8px currentColor; }
.dl-b { flex:1; min-width:0; }
.dl-n { display:flex; justify-content:space-between; align-items:baseline; margin-bottom:6px; font-size:12px; color:#c0d4e8; }
.dl-v { font-family:"SF Mono","Consolas",monospace; font-weight:700; font-size:14px; }
.dl-bar { height:6px; background:rgba(62,170,255,0.08); border-radius:3px; overflow:hidden; }
.dl-bar > span { display:block; height:100%; border-radius:3px; transition:width 0.6s; }

.sl-grid { display:grid; grid-template-columns:repeat(2, 1fr); gap:12px; padding:14px; }
.sl-i { position:relative; padding:14px 12px; background:rgba(8,18,32,0.6); border:1px solid rgba(62,170,255,0.12); border-radius:4px; overflow:hidden; }
.sl-i::before { content:''; position:absolute; top:0; left:0; right:0; height:2px; background:var(--c, rgba(62,170,255,0.3)); }
.sl-i.danger { --c: rgba(248,113,113,0.5); }
.sl-i.warn { --c: rgba(251,191,36,0.5); }
.sl-i.info { --c: rgba(34,211,238,0.5); }
.sl-i.ok { --c: rgba(52,211,153,0.5); }
.sl-l { font-size:11px; color:#8fb0cf; margin-bottom:6px; letter-spacing:0.5px; }
.sl-v { font-size:26px; font-weight:700; font-family:"SF Mono","Consolas",monospace; line-height:1.1; }
.sl-u { font-size:13px; font-weight:500; margin-left:2px; opacity:0.7; }
.sl-bg { position:absolute; left:0; right:0; bottom:0; height:3px; opacity:0.6; }

/* 表格样式 */
.ana-tb { width:100%; border-collapse:collapse; font-size:12px; }
.ana-tb th { padding:9px 10px; text-align:left; font-weight:600; color:#8fb0cf; border-bottom:1px solid rgba(62,170,255,0.12); font-size:11px; text-transform:uppercase; letter-spacing:0.5px; background:rgba(8,20,40,0.4); }
.ana-tb td { padding:9px 10px; border-bottom:1px solid rgba(62,170,255,0.05); color:#b8cee4; }
.ti { width:22px; height:22px; border-radius:50%; display:inline-flex; align-items:center; justify-content:center; font-size:11px; font-weight:700; margin-right:8px; }
.tc-f { background:rgba(96,165,250,0.15); color:#5fb3ff; }
.tc-m { background:rgba(251,191,36,0.15); color:#fbbf24; }
.tc-p { background:rgba(34,211,238,0.15); color:#22d3ee; }
.tc-o { background:rgba(167,139,250,0.15); color:#a78bfa; }
.tc-c { background:rgba(52,211,153,0.15); color:#34d399; }
.tc-h { background:rgba(248,113,113,0.15); color:#f87171; }
.tc-d { background:rgba(148,163,184,0.15); color:#8aabc9; }
.tc-g { background:rgba(100,130,160,0.15); color:#7896ac; }

.hlt { padding:2px 8px; border-radius:3px; font-size:11px; font-weight:600; font-family:"SF Mono","Consolas",monospace; }
.hlt.ok { background:rgba(52,211,153,0.12); color:#34d399; border:1px solid rgba(52,211,153,0.25); }
.hlt.w { background:rgba(251,191,36,0.12); color:#fbbf24; border:1px solid rgba(251,191,36,0.25); }
.hlt.bd { background:rgba(248,113,113,0.12); color:#f87171; border:1px solid rgba(248,113,113,0.25); }
.rb td { background:rgba(248,113,113,0.04); }
.rw td { background:rgba(251,191,36,0.03); }
.mt { text-align:center; padding:24px 0 !important; color:#4a6a8a !important; }

.ii-d { width:8px; height:8px; border-radius:50%; flex-shrink:0; margin-top:6px; }
.ii-d.bad { background:#f87171; box-shadow:0 0 8px rgba(248,113,113,0.5); }
.ii-d.warn { background:#fbbf24; box-shadow:0 0 8px rgba(251,191,36,0.4); }
.ii-d.ok { background:#34d399; box-shadow:0 0 8px rgba(52,211,153,0.4); }
.ii-b { flex:1; min-width:0; }
.ii-t { font-size:13px; font-weight:600; color:#c8e4ff; margin-bottom:4px; }
.ii-x { font-size:12px; line-height:1.55; }

/* 填充区域 */
.ana-filler { margin:10px 0 2px; padding:12px 14px; border-top:1px dashed rgba(62,170,255,0.2); background:linear-gradient(180deg, rgba(8,18,36,0.4), transparent); }
.af-h { display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px; }
.af-t { font-size:13px; font-weight:600; color:#c8e4ff; letter-spacing:0.5px; }
.af-sum { font-size:11px; color:#8fb0cf; font-family:'SF Mono','Consolas',monospace; }
.af-bars { display:flex; flex-direction:column; gap:8px; margin-bottom:10px; }
.af-b { display:grid; grid-template-columns:60px 1fr; gap:12px; align-items:center; }
.af-bi { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.af-bn { font-size:11px; color:#a8c4dc; }
.af-bv { font-size:11px; color:#c8e4ff; font-family:'SF Mono','Consolas',monospace; font-weight:600; }
.af-bg { height:5px; background:rgba(62,170,255,0.08); border-radius:3px; overflow:hidden; min-width:80px; }
.af-bf { display:block; height:100%; border-radius:3px; transition:width 0.6s; }
.af-stats { display:grid; grid-template-columns:repeat(4, 1fr); gap:8px; margin-top:8px; padding-top:10px; border-top:1px solid rgba(62,170,255,0.08); }
.af-s { display:flex; flex-direction:column; align-items:center; gap:3px; padding:8px; background:rgba(8,20,40,0.4); border:1px solid rgba(62,170,255,0.08); border-radius:4px; }
.af-sl { font-size:11px; color:#8fb0cf; }
.af-sv { font-size:17px; font-weight:700; color:#c8e4ff; font-family:'SF Mono','Consolas',monospace; line-height:1.1; }
.af-su { font-size:11px; color:#8fb0cf; }

/* 告警卡片 */
.alarm-grid { display:grid; grid-template-columns:repeat(2, minmax(0, 1fr)); gap:8px; padding:8px 4px 8px; flex:1; overflow-y:auto; min-height:0; align-content:flex-start; width:100%; box-sizing:border-box; }
.ait-card { padding:10px 12px; border-radius:4px; background:rgba(8,20,40,0.45); border:1px solid rgba(62,170,255,0.1); border-left:3px solid; transition:transform 0.15s; display:flex; flex-direction:column; gap:4px; min-height:88px; }
.ait-card .ait-desc { display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; line-height:1.4; max-height:2.8em; }
.ait-card.l1 { border-left-color:#f87171; background:linear-gradient(90deg, rgba(248,113,113,0.04), rgba(248,113,113,0.01)); }
.ait-card.l2 { border-left-color:#fbbf24; background:linear-gradient(90deg, rgba(251,191,36,0.04), rgba(251,191,36,0.01)); }
.ait-card.l3 { border-left-color:#22d3ee; background:linear-gradient(90deg, rgba(34,211,238,0.04), rgba(34,211,238,0.01)); }
.ait-card:hover { transform:translateX(2px); background:rgba(62,170,255,0.06); }
.ait-top { display:flex; align-items:center; gap:6px; margin-bottom:3px; flex-wrap:wrap; }
.ait-dev { font-size:12px; font-weight:600; color:#c8e4ff; }
.ait-st { font-size:11px; padding:1px 7px; border-radius:2px; font-weight:500; }
.sn { background:rgba(248,113,113,0.12); color:#f87171; border:1px solid rgba(248,113,113,0.25); }
.sa { background:rgba(251,191,36,0.12); color:#fbbf24; border:1px solid rgba(251,191,36,0.25); }
.sd { background:rgba(52,211,153,0.12); color:#34d399; border:1px solid rgba(52,211,153,0.25); }
.ait-tm { color:#8fb0cf; margin-left:auto; }
.ait-desc { font-size:11px; color:#9fb6cf; line-height:1.4; margin-bottom:2px; }

/* AI洞察填充 */
.aii-fill { margin-top:12px; padding-top:12px; border-top:1px dashed rgba(62,170,255,0.15); }
.aii-fill-h { font-size:12px; font-weight:600; color:#c8e4ff; margin-bottom:8px; letter-spacing:0.5px; }
.aii-fill-grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(220px, 1fr)); gap:8px; }
.aii-fill-i { display:flex; align-items:center; gap:8px; font-size:11px; color:#a8c4dc; padding:6px 10px; background:rgba(8,20,40,0.3); border-radius:4px; border:1px solid rgba(62,170,255,0.06); }
.aii-fill-d { width:6px; height:6px; border-radius:50%; flex-shrink:0; }
.aii-fill-d.ok { background:#34d399; box-shadow:0 0 4px rgba(52,211,153,0.5); }
.aii-fill-d.warn { background:#fbbf24; box-shadow:0 0 4px rgba(251,191,36,0.5); }
.aii-fill-d.info { background:#22d3ee; box-shadow:0 0 4px rgba(34,211,238,0.5); }

/* 工具类 */
.gm { color:#8fb0cf; }
.rd { color:#f87171; }
.yw { color:#fbbf24; }
.fs-xs { font-size:11px; }
.mono { font-family:"SF Mono","Consolas",monospace; }
.ana-num { font-family:"SF Mono","Consolas",monospace; font-size:12px; color:#3eaaff; margin-left:4px; }

/* 滚动条 */
.ana-body-fill::-webkit-scrollbar { width:4px; }
.ana-body-fill::-webkit-scrollbar-thumb { background:rgba(62,170,255,0.18); border-radius:3px; }

/* 响应式 */
@media(max-width:1400px){
  .mon-top-row { grid-template-columns:280px 1fr; }
  .ana-banner { grid-template-columns:repeat(3, 1fr); }
  .ana-duo-row { grid-template-columns:1fr; }
  .chart-row { grid-template-columns:1fr; }
}
@media(max-width:1000px){
  .mon-top-row { grid-template-columns:1fr; }
  .kpi-grid { grid-template-columns:repeat(6, 1fr); }
  .ana-banner { grid-template-columns:repeat(2, 1fr); }
  .ai-insights-row { grid-template-columns:1fr; }
  .dist-flex { grid-template-columns:1fr; }
  .rtm-grid { grid-template-columns:repeat(auto-fit, minmax(130px, 1fr)); }
}
</style>
