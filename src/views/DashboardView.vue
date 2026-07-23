<template>
<div class="dash-wrap">
  <!-- 机组选择 — 横向卡片 -->
  <div class="unit-panel">
    <div class="up-header">
      <div class="up-title"><i class="ut-ic"></i>机组选择</div>
      <div class="up-hint">点击切换不同机组查看监控数据</div>
    </div>
    <div class="up-cards">
      <div v-for="u in store.units" :key="u.id" class="up-card" :class="{on: store.selectedUnitId===u.id}" @click="switchUnit(u.id)">
        <div class="up-card-accent"></div>
        <div class="up-c1">
          <div class="up-name">{{ u.name }}</div>
          <div class="up-type">{{ u.type }} · {{ u.capacity }}MW</div>
          <span v-if="store.selectedUnitId===u.id" class="up-badge">✓ 当前选中</span>
        </div>
        <div class="up-c2">
          <div class="up-metric"><span class="m-label">设备</span><strong class="m-value">{{ store.unitDevices(u.id).length }}</strong></div>
          <div class="up-metric"><span class="m-label">报警</span><strong class="m-value danger">{{ store.unitAlarms(u.id).length }}</strong></div>
        </div>
      </div>
    </div>
  </div>

  <!-- 专业切换 -->
  <div class="dept-switch">
    <span class="dept-lbl">监控专业：</span>
    <el-radio-group v-model="deptMode" size="small">
      <el-radio-button value="all">全部</el-radio-button>
      <el-radio-button value="锅炉">锅炉</el-radio-button>
      <el-radio-button value="汽轮机">汽轮机</el-radio-button>
    </el-radio-group>
  </div>

  <!-- 核心指标行: 概览 + 排放 并排 -->
  <div class="dash-metrics-row">
    <div class="cd dash-metric-panel">
      <div class="cd-h">
        <span class="cd-t"><span class="sd" :class="overviewStatus==='需关注'?'wn':'ok'"></span>{{ store.selectedUnit.name }} 机组实时概览</span>
        <span class="tg" :class="overviewStatus==='需关注'?'tg-w':'tg-ok'">{{ overviewStatus }}</span>
      </div>
      <div class="metric-grid">
        <div class="mc"><div class="ml">负荷</div><div class="mv in">{{ mLoad }}</div><div class="ms">MW</div></div>
        <div class="mc"><div class="ml">主汽压力</div><div class="mv" :class="mcPress">{{ mPress }}</div><div class="ms">MPa</div></div>
        <div class="mc"><div class="ml">主汽温度</div><div class="mv" :class="mcTemp">{{ mTemp }}</div><div class="ms">℃</div></div>
        <div class="mc"><div class="ml">煤耗</div><div class="mv in">{{ mCoal }}</div><div class="ms">g/kWh</div></div>
      </div>
    </div>
    <div class="cd dash-metric-panel">
      <div class="cd-h">
        <span class="cd-t">环保排放指标</span>
        <span class="tg" :class="emissionOk?'tg-ok':'tg-dg'">{{ emissionOk?'达标':'超标' }}</span>
      </div>
      <div class="metric-grid">
        <div class="mc"><div class="ml">NOx</div><div class="mv" :class="mcNox">{{ mNox }}</div><div class="ms">mg/m³</div></div>
        <div class="mc"><div class="ml">SO2</div><div class="mv" :class="mcSo2">{{ mSo2 }}</div><div class="ms">mg/m³</div></div>
        <div class="mc"><div class="ml">粉尘</div><div class="mv" :class="mcDust">{{ mDust }}</div><div class="ms">mg/m³</div></div>
        <div class="mc"><div class="ml">达标率</div><div class="mv in">{{ mEmRef }}</div><div class="ms">%</div></div>
      </div>
    </div>
  </div>

  <!-- 设备健康度 + 趋势图 混合布局 -->
  <div class="dash-health-row">
    <div v-if="showBoiler" class="cd health-col">
      <div class="cd-t"><span class="sd" :class="boilerAvgHealth >= 90 ? 'ok' : boilerAvgHealth >= 80 ? 'wn' : 'dg'"></span>锅炉设备健康度（{{ store.selectedUnit.name }}）</div>
      <div class="hb-list">
        <div v-for="d in boiler" :key="d.id" class="hb-row">
          <span class="hb-n">{{ d.name }}</span>
          <div class="hb-b"><div class="hb-f" :class="cls(d.health)" :style="{width:d.health+'%'}"></div></div>
          <span class="hb-v" :style="{color:col(d.health)}">{{ d.health.toFixed(1) }}</span>
        </div>
      </div>
    </div>
    <div v-if="showTurbine" class="cd health-col">
      <div class="cd-t"><span class="sd" :class="turbineAvgHealth >= 90 ? 'ok' : turbineAvgHealth >= 80 ? 'wn' : 'dg'"></span>汽轮机设备健康度（{{ store.selectedUnit.name }}）</div>
      <div class="hb-list">
        <div v-for="d in turbine" :key="d.id" class="hb-row">
          <span class="hb-n">{{ d.name }}</span>
          <div class="hb-b"><div class="hb-f" :class="cls(d.health)" :style="{width:d.health+'%'}"></div></div>
          <span class="hb-v" :style="{color:col(d.health)}">{{ d.health.toFixed(1) }}</span>
        </div>
      </div>
    </div>
    <!-- 趋势图区域 -->
    <div class="cd chart-col">
      <div class="cd-t">负荷趋势</div>
      <div ref="ch1" class="dash-chart-sm"></div>
    </div>
    <div class="cd chart-col">
      <div class="cd-t">主汽温度趋势</div>
      <div ref="ch2" class="dash-chart-sm"></div>
    </div>
    <div class="cd chart-col">
      <div class="cd-t">自动/联锁投入率</div>
      <div ref="ch3" class="dash-chart-sm"></div>
    </div>
  </div>

  <!-- AI洞察 -->
  <div class="insight">
    <div class="in-t"><i class="in-ic"></i>AI 实时运行洞察 · {{ store.selectedUnit.name }} <span class="in-tag">实时分析中</span></div>
    <div class="in-b" id="dash-insight" v-html="insightText || '分析中...'"></div>
    <div class="in-problems" id="dash-problems">
      <div v-if="problems.length === 0" class="prob-empty">暂未检测到需关注的设备问题</div>
      <template v-else>
        <div class="prob-h">问题设备 TOP{{ problems.length }}</div>
        <div v-for="p in problems" :key="p.name + p.reason" class="prob-card" :class="p.type">
          <div class="prob-i">{{ p.icon }}</div>
          <div class="prob-info">
            <div class="prob-name">{{ p.name }} <span class="prob-dept">{{ p.dept }}</span>
              <span v-if="p.alarmCount > 0" class="prob-alarm">{{ p.alarmCount }} 条报警</span>
            </div>
            <div class="prob-r">问题：{{ p.reason }}</div>
            <div class="prob-s">建议：{{ p.suggestion }}</div>
          </div>
          <div class="prob-h-val">
            <div class="prob-hl" :style="{color: p.health >= 90 ? '#34d399' : p.health >= 80 ? '#fbbf24' : '#ef4444'}">{{ p.health.toFixed(1) }}</div>
            <div class="prob-hll">健康度</div>
          </div>
        </div>
      </template>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const deptMode = ref('all')
const boiler = computed(() => deptMode.value === '汽轮机' ? [] : store.unitDevices(store.selectedUnitId).filter(d => d.dept === '锅炉'))
const turbine = computed(() => deptMode.value === '锅炉' ? [] : store.unitDevices(store.selectedUnitId).filter(d => d.dept === '汽轮机'))
const showBoiler = computed(() => deptMode.value !== '汽轮机')
const showTurbine = computed(() => deptMode.value !== '锅炉')

const overviewStatus = computed(() => store.unitDevices(store.selectedUnitId).filter(d => d.health < 85).length > 0 ? '需关注' : '运行正常')
const cls = (h) => h >= 90 ? 'ok' : h >= 80 ? 'wn' : 'dg'
const col = (h) => h >= 90 ? '#34d399' : h >= 80 ? '#fbbf24' : '#ef4444'

const mLoad = ref(0), mPress = ref(0), mTemp = ref(0), mCoal = ref(0)
const mNox = ref(0), mSo2 = ref(0), mDust = ref(0), mEmRef = ref(0)
const boilerAvgHealth = computed(() => { const b = boiler.value; return b.length ? b.reduce((s,d)=>s+d.health,0)/b.length : 100 })
const turbineAvgHealth = computed(() => { const t = turbine.value; return t.length ? t.reduce((s,d)=>s+d.health,0)/t.length : 100 })

const mcPress = computed(() => mPress.value > store.selectedUnit.base.press * 1.03 ? 'dg' : 'ok')
const mcTemp = computed(() => mTemp.value > 545 ? 'dg' : 'ok')
const mcNox = computed(() => mNox.value > 50 ? 'dg' : 'ok')
const mcSo2 = computed(() => mSo2.value > 35 ? 'dg' : 'ok')
const mcDust = computed(() => mDust.value > 10 ? 'dg' : 'ok')
const emissionOk = computed(() => mNox.value <= 50 && mSo2.value <= 35 && mDust.value <= 10)

const ch1 = ref(null), ch2 = ref(null), ch3 = ref(null)
let c1, c2, c3, iv

const tsArr = Array.from({ length: 30 }, (_, i) => { const d = new Date(); d.setMinutes(d.getMinutes() - 29 + i); return d.toTimeString().slice(0,5) })

const initCharts = () => {
  if(c1)c1.dispose();if(c2)c2.dispose();if(c3)c3.dispose()

  // 3D风格图表配置
  const lineOpt = (color, data, extra) => ({
    grid:{left:36,right:14,top:12,bottom:22},
    xAxis:{type:'category',data:tsArr,axisLabel:{color:'#8fb0cf',fontSize: 11,interval:5},axisLine:{lineStyle:{color:'rgba(60,140,210,0.12)'}},axisTick:{show:false}},
    yAxis:{type:'value',axisLabel:{color:'#8fb0cf',fontSize: 11},splitLine:{lineStyle:{color:'rgba(60,140,210,0.06)',type:'dashed'}}},
    series:[{
      type:'line',smooth:true,symbolSize:4,data,
      lineStyle:{color,width:2.5,shadowBlur:12,shadowColor:`${color}40`,shadowOffsetY:2},
      itemStyle:{color,borderColor:'#fff',borderWidth:1.5,shadowBlur:6,shadowColor:`${color}50`},
      areaStyle:{color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:`${color}20`},{offset:1,color:`${color}02`}]}},
      symbol:'circle',
      ...extra
    }],
    animationDuration:1200
  })

  c1=echarts.init(ch1.value);c1.setOption(lineOpt('#3eaaff',Array.from({length:30},()=>store.selectedUnit.base.load+Math.random()*20-10),{}))
  c2=echarts.init(ch2.value);c2.setOption({
    ...lineOpt('#fbbf24',Array.from({length:30},()=>store.selectedUnit.base.temp+Math.random()*8),{}),
    series:[...lineOpt('#fbbf24',[],{}).series,
      {type:'line',data:Array(30).fill(545),lineStyle:{color:'#ef4444',width:1,type:'dashed'},symbol:'none'}]
  })
  c3=echarts.init(ch3.value);c3.setOption({
    ...lineOk('#34d399',Array.from({length:30},()=>98.5+Math.random()),{}),
    ...lineOk('#22d3ee',Array.from({length:30},()=>97.2+Math.random()),{})
  })
}

function lineOk(color,data){return{grid:{left:36,right:14,top:12,bottom:22},xAxis:{type:'category',data:tsArr,axisLabel:{color:'#8fb0cf',fontSize: 11,interval:5},axisLine:{lineStyle:{color:'rgba(60,140,210,0.12)'}}},yAxis:{type:'value',min:95,axisLabel:{color:'#8fb0cf',fontSize: 11},splitLine:{lineStyle:{color:'rgba(60,140,210,0.06)',type:'dashed'}}},series:[{type:'line',smooth:true,symbolSize:3,data,lineStyle:{color,width:2,shadowBlur:8,shadowColor:`${color}35`},itemStyle:{color,borderColor:'#fff',borderWidth:1},symbol:'none'}]}}

const updateMetrics = () => {
  const u = store.selectedUnit; if(!u?.base)return
  mLoad.value=+(u.base.load+Math.random()*30-15).toFixed(1)
  mPress.value=+u.base.press.toFixed(2)
  mTemp.value=+(u.base.temp+Math.random()*8).toFixed(1)
  mCoal.value=+(u.base.coal+Math.random()*8).toFixed(1)
  mNox.value=+(u.base.nox+Math.random()*15).toFixed(1)
  mSo2.value=+(u.base.so2+Math.random()*5).toFixed(1)
  mDust.value=+(u.base.dust+Math.random()*3).toFixed(1)
  mEmRef.value=+(99+Math.random()).toFixed(1)
  updateInsight()
}

const insightText = ref('')
const problems = ref([])
const updateInsight = () => {
  const devs=store.unitDevices(store.selectedUnitId),warns=devs.filter(d=>d.health<85)
  const alarms=store.unitAlarms(store.selectedUnitId).filter(a=>a.st==='unhandled'),l1=alarms.filter(a=>a.l===1)
  let text=''
  if(warns.length>0) text+=`<span style="color:#fbbf24">设备健康度：</span>${warns.map(d=>`${d.name} ${d.health.toFixed(1)}`).join('、')} 偏低需关注。`
  else text+=`<span style="color:#34d399">设备健康度：</span>所有设备均在85以上，状态良好。`
  if(alarms.length>0) text+=` <span style="color:#ef4444">未处理报警：</span>${alarms.length}条，${l1.length}条一级需立即响应。`
  else text+=` <span style="color:#34d399">报警状态：</span>无未处理。`
  if(mTemp.value>545) text+=` <span style="color:#fbbf24">温度预警：</span>主汽温度${mTemp.value}°C接近上限545°C，建议调整减温水量。`
  insightText.value=text

  const pList=[]
  warns.forEach(d=>{
    const bp=Object.entries(d.params||{}).filter(([k,v])=>v[0]>=v[1])
    pList.push({type:d.health<70?'danger':'warn',icon:d.health<70?'🚨':'⚠️',name:d.name,dept:d.dept,health:d.health,
      reason:bp.length?`${bp[0][0]}=${bp[0][1][0]}${bp[0][1][2]}（阈值${bp[0][1][1]}${bp[0][1][2]}）`:'健康度低于85，需关注',
      suggestion:d.health<70?'建议立即安排停机检查':'建议加强巡检频次',
      alarmCount:alarms.filter(a=>a.device===d.name).length})
  })
  l1.forEach(a=>{
    if(!pList.find(p=>p.name===a.device)) pList.push({type:'danger',icon:'🚨',name:a.device,dept:a.dept,health:store.deviceById(store.devices.find(d=>d.name===a.device)?.id)?.health||0,reason:a.desc,suggestion:'立即响应，按操作指导处置',alarmCount:1})
  })
  problems.value=pList
}

const switchUnit=(id)=>{store.selectedUnitId=id;deptMode.value='all';nextTick(()=>{initCharts();updateMetrics()})}
watch(()=>store.selectedUnitId,()=>nextTick(()=>{initCharts();updateMetrics()}))
onMounted(()=>{nextTick(()=>{initCharts();updateMetrics()})})
iv=setInterval(updateMetrics,3000)
onUnmounted(()=>{clearInterval(iv);c1?.dispose();c2?.dispose();c3?.dispose()})
</script>

<style scoped>
/* ================================================================
   Dashboard — 专业工业数据平台风格 v2
   ================================================================ */
.dash-wrap { display:flex; flex-direction:column; gap:12px; min-height:100%; }

/* 机组选择 */
.unit-panel {
  background:linear-gradient(135deg, rgba(62,170,255,0.04), rgba(62,170,255,0.01));
  border:1px solid rgba(62,170,255,0.15);
  border-radius:6px;
  padding:14px 16px;
}
.up-header { display:flex; align-items:center; margin-bottom:12px; }
.up-title { font-size:14px; font-weight:600; color:#e0f0ff; display:flex; align-items:center; gap:8px; }
.ut-ic { width:0; height:0; border-left:7px solid #3eaaff; border-top:4px solid transparent; border-bottom:4px solid transparent; filter:drop-shadow(0 0 3px rgba(62,170,255,0.5)); }
.up-hint { font-size:11px; color:#8fb0cf; margin-left:12px; }
.up-cards { display:flex; gap:12px; flex-wrap:wrap; }
.up-card {
  display:flex; align-items:stretch; gap:0; padding:0;
  background:linear-gradient(180deg, rgba(8,20,40,0.6), rgba(6,16,32,0.65));
  border:1px solid rgba(62,170,255,0.12);
  border-radius:6px;
  cursor:pointer; transition:all 0.2s;
  min-width:340px; position:relative; overflow:hidden;
}
.up-card:hover { border-color:rgba(62,170,255,0.35); transform:translateY(-2px); box-shadow:0 6px 20px rgba(0,10,30,0.4); }
.up-card.on { border-color:#3eaaff; box-shadow:0 0 18px rgba(62,170,255,0.25); }
.up-card-accent { position:absolute; left:0; top:0; bottom:0; width:3px; background:#3eaaff; opacity:0; transition:opacity 0.2s; }
.up-card.on .up-card-accent { opacity:1; }

.up-c1 { flex:1; padding:14px 16px; min-width:150px; }
.up-name { font-size:15px; font-weight:600; color:#e2e8f0; margin-bottom:4px; }
.up-type { font-size:11px; color:#8fb0cf; margin-bottom:6px; }
.up-badge { display:inline-block; font-size:11px; padding:2px 8px; border-radius:3px; background:rgba(62,170,255,0.15); color:#5fb3ff; font-weight:500; }
.up-c2 { display:flex; gap:0; border-left:1px solid rgba(62,170,255,0.08); }
.up-metric { display:flex; flex-direction:column; align-items:center; justify-content:center; min-width:68px; padding:14px 16px; }
.up-metric+.up-metric { border-left:1px solid rgba(62,170,255,0.08); }
.m-label { font-size:11px; color:#8fb0cf; margin-bottom:6px; white-space:nowrap; }
.m-value { font-size:22px; color:#5fb3ff; font-weight:600; line-height:1; }
.m-value.danger { color:#ef4444; }

.dept-switch { display:flex; align-items:center; gap:10px; padding:8px 14px; background:linear-gradient(180deg, rgba(8,20,40,0.45), rgba(6,16,32,0.5)); border:1px solid rgba(62,170,255,0.1); border-radius:5px; }
.dept-lbl { font-size:12px; color:#8fb0cf; }
:deep(.dept-switch .el-radio-button__inner) { background:rgba(8,18,32,0.5); border-color:rgba(62,170,255,0.12); color:#8fb0cf; }
:deep(.dept-switch .el-radio-button.is-active .el-radio-button__inner) { background:rgba(62,170,255,0.15); border-color:#3eaaff; color:#3eaaff; box-shadow:none; }

.sd { display:inline-block; width:7px; height:7px; border-radius:50%; margin-right:6px; }
.sd.ok { background:#34d399; box-shadow:0 0 5px rgba(52,211,153,0.5); }
.sd.wn { background:#fbbf24; box-shadow:0 0 5px rgba(251,191,36,0.5); }
.sd.dg { background:#ef4444; box-shadow:0 0 5px rgba(239,68,68,0.5); }
.tg { display:inline-block; padding:2px 8px; border-radius:3px; font-size:11px; margin-left:auto; }
.tg-ok { background:rgba(34,197,94,0.12); color:#34d399; }
.tg-w { background:rgba(245,158,11,0.12); color:#fbbf24; }
.tg-dg { background:rgba(239,68,68,0.12); color:#ef4444; }

/* 指标面板 */
.dash-metrics-row { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.metric-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:8px; padding:12px 8px 8px; }
.mc { background:rgba(8,18,32,0.5); border-radius:5px; padding:12px 8px; text-align:center; border:1px solid rgba(62,170,255,0.06); transition:all 0.2s; }
.mc:hover { border-color:rgba(62,170,255,0.2); background:rgba(8,20,40,0.65); }
.ml { font-size:11px; color:#8fb0cf; margin-bottom:4px; }
.mv { font-size:22px; font-weight:600; font-family:"SF Mono","Consolas",monospace; text-shadow:0 2px 6px rgba(0,0,0,0.3); }
.ms { font-size:11px; color:#8fb0cf; margin-top:2px; }
.mv.in { color:#5fb3ff; }
.mv.ok { color:#34d399; }
.mv.dg { color:#ef4444; }

/* 健康度 + 图表混合行 */
.dash-health-row { display:grid; grid-template-columns:showBoiler 1fr showTurbine 1fr repeat(3,1fr); gap:12px; }
.health-col { min-height:200px; }
.chart-col { min-height:200px; }
.hb-list { padding:8px 4px; display:flex; flex-direction:column; gap:6px; }
.hb-row { display:flex; align-items:center; gap:10px; font-size:12px; }
.hb-n { width:88px; color:#a8c4dc; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.hb-b { flex:1; height:6px; background:rgba(62,170,255,0.08); border-radius:3px; overflow:hidden; }
.hb-f { height:100%; border-radius:3px; transition:width 0.6s; box-shadow:0 0 4px currentColor; }
.hb-f.ok { background:linear-gradient(90deg,#34d399,#22c55e); }
.hb-f.wn { background:linear-gradient(90deg,#fbbf24,#f59e0b); }
.hb-f.dg { background:linear-gradient(90deg,#ef4444,#dc2626); }
.hb-v { width:38px; text-align:right; font-weight:600; font-family:"SF Mono","Consolas",monospace; }

.dash-chart-sm { height:160px; }

/* AI洞察 */
.insight {
  background:linear-gradient(180deg, rgba(10,26,50,0.5), rgba(6,16,32,0.55));
  border:1px solid rgba(62,170,255,0.1);
  border-radius:6px;
  padding:14px 16px;
}
.in-t { font-size:13px; font-weight:600; color:#5fb3ff; margin-bottom:8px; display:flex; align-items:center; gap:8px; }
.in-ic { width:0; height:0; border-left:7px solid #3eaaff; border-top:4px solid transparent; border-bottom:4px solid transparent; filter:drop-shadow(0 0 3px rgba(62,170,255,0.5)); }
.in-tag { font-size:11px; padding:1px 6px; border-radius:3px; background:rgba(52,211,153,0.12); color:#34d399; font-weight:400; }
.in-b { font-size:12px; color:#9fb6cf; line-height:1.75; margin-bottom:10px; }
.in-problems { margin-top:10px; padding-top:10px; border-top:1px dashed rgba(62,170,255,0.12); }
.prob-h { font-size:12px; font-weight:600; color:#ef4444; margin-bottom:8px; }
.prob-empty { text-align:center; padding:20px; color:#34d399; font-size:13px; }
.prob-card { display:flex; gap:12px; padding:10px 12px; border-radius:5px; margin-bottom:8px; border-left:3px solid; transition:transform 0.15s; }
.prob-card:hover { transform:translateX(2px); }
.prob-card.danger { background:rgba(239,68,68,0.05); border-color:#ef4444; }
.prob-card.warn { background:rgba(245,158,11,0.05); border-color:#fbbf24; }
.prob-i { font-size:18px; line-height:1; flex-shrink:0; }
.prob-info { flex:1; min-width:0; }
.prob-name { font-size:13px; font-weight:600; color:#e2e8f0; margin-bottom:4px; display:flex; align-items:center; gap:6px; flex-wrap:wrap; }
.prob-dept { font-size:11px; padding:1px 6px; border-radius:3px; background:rgba(62,170,255,0.1); color:#8fb0cf; }
.prob-alarm { font-size:11px; padding:1px 6px; border-radius:3px; background:rgba(239,68,68,0.15); color:#ef4444; font-weight:500; }
.prob-r { font-size:11px; color:#cbd5e1; margin-bottom:2px; }
.prob-s { font-size:11px; color:#8fb0cf; }
.prob-h-val { text-align:center; flex-shrink:0; min-width:56px; }
.prob-hl { font-size:22px; font-weight:700; font-family:"SF Mono","Consolas",monospace; }
.prob-hll { font-size:11px; color:#8fb0cf; }

@media(max-width:1200px){
  .dash-metrics-row { grid-template-columns:1fr; }
  .dash-health-row { grid-template-columns:1fr 1fr; }
}
@media(max-width:800px){
  .dash-health-row { grid-template-columns:1fr; }
  .metric-grid { grid-template-columns:repeat(2,1fr); }
}
</style>
