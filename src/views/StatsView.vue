<template>
<div class="stats-wrap">
  <!-- 时间切换 -->
  <div class="period-header">
    <el-tabs v-model="period" class="period-tabs">
      <el-tab-pane label="今日" name="today" />
      <el-tab-pane label="本周" name="week" />
      <el-tab-pane label="本月" name="month" />
    </el-tabs>
  </div>

  <!-- 统计概览 — 4列等宽 -->
  <div class="stats-overview">
    <div class="sc"><div class="sc-icon" style="background:rgba(95,179,255,0.15);color:#5fb3ff"></div><div class="sl">报警总数</div><div class="sv in">{{ stats.total }}</div><div class="ss">{{ store.selectedUnit.name }}</div></div>
    <div class="sc"><div class="sc-icon" style="background:rgba(248,113,113,0.15);color:#f87171"></div><div class="sl">未处理</div><div class="sv dg">{{ stats.unhandled }}</div><div class="ss">条需响应</div></div>
    <div class="sc"><div class="sc-icon" style="background:rgba(52,211,153,0.15);color:#34d399"></div><div class="sl">已处理</div><div class="sv ok">{{ stats.handled }}</div><div class="ss">条已闭环</div></div>
    <div class="sc"><div class="sc-icon" style="background:rgba(34,211,238,0.15);color:#22d3ee"></div><div class="sl">智能过滤</div><div class="sv ok">{{ filterCnt }}</div><div class="ss">条误报</div></div>
  </div>

  <!-- 饼图行: 专业占比 + 级别占比 + 处理状态 -->
  <div class="pie-row">
    <div class="cd pie-col">
      <div class="cd-h"><span class="cd-t">报警专业占比</span><span class="cd-sub">锅炉 {{ profCnt['锅炉'] }} / 汽机 {{ profCnt['汽机'] }} / 辅网 {{ profCnt['辅网'] }}</span></div>
      <div ref="chProf" class="pie-chart"></div>
    </div>
    <div class="cd pie-col">
      <div class="cd-h"><span class="cd-t">级别占比</span><span class="cd-sub">一级 {{ cnt(1) }} / 二级 {{ cnt(2) }} / 预警 {{ cnt(3) }}</span></div>
      <div ref="chLevel" class="pie-chart"></div>
    </div>
    <div class="cd pie-col">
      <div class="cd-h"><span class="cd-t">处理状态</span><span class="cd-sub">已处理 {{ stats.handled }} / 未处理 {{ stats.unhandled }} / 抑制 {{ suppressed }}</span></div>
      <div ref="chStatus" class="pie-chart"></div>
    </div>
  </div>

  <!-- 柱状图行: Top10次数 + Top10时长 -->
  <div class="bar-row">
    <div class="cd bar-col">
      <div class="cd-h"><span class="cd-t">报警次数 Top10（按测点）</span><span class="cd-sub">单位：次 | 本期累计 <strong style="color:#fbbf24">{{ totalTimes }}</strong> 次</span></div>
      <div ref="chTop10" class="bar-chart"></div>
    </div>
    <div class="cd bar-col">
      <div class="cd-h"><span class="cd-t">报警时长 Top10（按测点）</span><span class="cd-sub">单位：分钟 | 本期累计 <strong style="color:#ef4444">{{ totalMins }}</strong> 分钟</span></div>
      <div ref="chDur" class="bar-chart"></div>
    </div>
  </div>

  <!-- AI洞察 -->
  <div class="insight-block">
    <div class="insight-t"><i class="it-ic"></i>AI 深度数据分析洞察</div>
    <div class="insight-list">
      <div v-for="(ins,i) in insights" :key="i" class="ins-row" :class="ins.type">
        <div class="ins-i">{{ ins.icon }}</div>
        <div class="ins-b">
          <div class="ins-title">{{ ins.title }}</div>
          <div class="ins-content" v-html="ins.content"></div>
        </div>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const period = ref('today')
const chProf = ref(null), chLevel = ref(null), chStatus = ref(null), chTop10 = ref(null), chDur = ref(null)
let charts = []

const stats = computed(() => store.stats)
const cnt = (l) => store.unitAlarms(store.selectedUnitId).filter(a => a.l === l).length
const suppressed = computed(() => store.unitAlarms(store.selectedUnitId).filter(a => a.st === 'suppressed').length)
const filterCnt = computed(() => Math.floor(store.stats.total * 0.05 + Math.random() * 3))
const totalTimes = computed(() => 28 + Math.floor(Math.random() * 5))
const totalMins = computed(() => 196 + Math.floor(Math.random() * 20))

const profCnt = computed(() => {
  const c = { '锅炉': 0, '汽机': 0, '辅网': 0 }
  store.unitAlarms(store.selectedUnitId).forEach(a => { c[a.dept] = (c[a.dept] || 0) + 1 })
  return c
})

const insights = computed(() => {
  const out = [], arr = store.unitAlarms(store.selectedUnitId), total = arr.length
  if (total === 0) return [{ type:'ok', icon:'✅', title:'当前无报警', content:'系统运行平稳。' }]
  const p = profCnt.value
  const maxProf = Object.entries(p).sort((a,b)=>b[1]-a[1])[0]
  const ratio = ((maxProf[1]/total)*100).toFixed(0)
  out.push({ type:'info', icon:'📊', title:`报警集中度分析`, content:`本期报警主要集中在<strong style="color:#fbbf24">${maxProf[0]}</strong>专业（${ratio}%）。` })
  const l1 = arr.filter(a=>a.l===1)
  if(l1.length>0) out.push({ type:'danger', icon:'🚨', title:'一级报警处置分析', content:`当前有 <strong style="color:#ef4444">${l1.length}</strong> 条一级报警，平均响应时长 <strong>${store.alarmLevels[1].avgTime}分钟</strong>。` })
  const lowH = store.unitDevices(store.selectedUnitId).filter(d=>d.health<85)
  if(lowH.length>0) out.push({ type:'warn', icon:'⚠️', title:'设备健康度预警', content:`${lowH.length} 台设备健康度低于85，建议安排预防性检修。` })
  out.push({ type:'info', icon:'🎯', title:'智能过滤效果', content:`AI 模式识别自动过滤了 <strong style="color:#34d399">${filterCnt.value} 条</strong> 疑似误报。` })
  out.push({ type:'info', icon:'📈', title:'趋势预测', content:`下周报警总量预计 <strong>${(total*(0.9+Math.random()*0.2)).toFixed(0)} 条</strong>。` })
  out.push({ type:'ok', icon:'💡', title:'AI 处置建议', content:`建议加强 ${maxProf[0]} 设备巡检频次；对连续触发2次以上测点配置自动联动。` })
  return out
})

const initCharts = () => {
  charts.forEach(c => c.dispose()); charts = []
  const p = profCnt.value

  // 3D饼图配置工厂
  const pieOpt = (data, name) => ({
    backgroundColor:'transparent',
    tooltip:{trigger:'item',formatter:'{b}: {c} ({d}%)',backgroundColor:'rgba(6,14,28,0.96)',borderColor:'rgba(62,170,255,0.2)',textStyle:{color:'#c8dae8',fontSize:11}},
    series:[{
      type:'pie',radius:['40%','70%'],center:['50%','54%'],
      avoidLabelOverlap:true,
      itemStyle:{borderColor:'rgba(6,18,48,0.8)',borderWidth:3,borderRadius:4,shadowBlur:14,shadowColor:'rgba(0,0,0,0.45)',shadowOffsetY:3},
      emphasis:{scale:true,scaleSize:6,itemStyle:{shadowBlur:22,shadowColor:'rgba(0,0,0,0.6)'}},
      label:{color:'#a8c4dc',fontSize:11,fontWeight:500,textShadowColor:'rgba(0,0,0,0.35)',textShadowBlur:2},
      labelLine:{lineStyle:{color:'rgba(62,170,255,0.18)',width:1},length:12,length2:8},
      data,animationType:'expansion',animationEasing:'elasticOut'
    }],
    animationDuration:1400
  })

  let c1=echarts.init(chProf.value)
  c1.setOption(pieOpt([
    {value:p['锅炉'],name:`锅炉 ${p['锅炉']}条`,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#fcd34d'},{offset:1,color:'#f59e0b'}])}},
    {value:p['汽机'],name:`汽机 ${p['汽机']}条`,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#60a5fa'},{offset:1,color:'#3b82f6'}])}},
    {value:p['辅网'],name:`辅网 ${p['辅网']}条`,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#4ade80'},{offset:1,color:'#22c55e'}])}}
  ].filter(d=>d.value>0)))
  charts.push(c1)

  let c2=echarts.init(chLevel.value)
  c2.setOption(pieOpt([
    {value:cnt(1),name:`一级 ${cnt(1)}条`,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#f87171'},{offset:1,color:'#dc2626'}])}},
    {value:cnt(2),name:`二级 ${cnt(2)}条`,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#fcd34d'},{offset:1,color:'#f59e0b'}])}},
    {value:cnt(3),name:`预警 ${cnt(3)}条`,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#22d3ee'},{offset:1,color:'#0891b2'}])}}
  ].filter(d=>d.value>0)))
  charts.push(c2)

  let c3=echarts.init(chStatus.value)
  c3.setOption(pieOpt([
    {value:stats.value.handled,name:`已处理 ${stats.value.handled}条`,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#34d399'},{offset:1,color:'#16a34a'}])}},
    {value:stats.value.unhandled,name:`未处理 ${stats.value.unhandled}条`,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#f87171'},{offset:1,color:'#dc2626'}])}},
    {value:suppressed.value,name:`抑制 ${suppressed.value}条`,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,1,[{offset:0,color:'#94a3b8'},{offset:1,color:'#64748b'}])}}
  ]))
  charts.push(c3)

  // 3D柱状图配置
  const barOpt = (yData, sData, colorGrad, xName) => ({
    backgroundColor:'transparent',
    grid:{left:120,right:30,top:8,bottom:24},
    xAxis:{type:'value',name:xName,axisLabel:{color:'#8fb0cf',fontSize: 11},splitLine:{lineStyle:{color:'rgba(60,140,210,0.06)',type:'dashed'}}},
    yAxis:{type:'category',data:yData.reverse(),axisLabel:{color:'#9fb6cf',fontSize: 11},axisLine:{lineStyle:{color:'rgba(60,140,210,0.12)'}}},
    tooltip:{trigger:'axis',backgroundColor:'rgba(6,14,28,0.96)',borderColor:'rgba(62,170,255,0.2)',textStyle:{color:'#c8dae8',fontSize:11}},
    series:[{
      type:'bar',data:sData.reverse(),
      itemStyle:{ color: colorGrad, borderRadius:[0,4,4,0], shadowBlur:10,shadowColor:'rgba(0,0,0,0.3)',shadowOffsetY:2 },
      barWidth:'55%', emphasis:{itemStyle:{shadowBlur:18}}
    }],
    animationDuration:1200,animationEasing:'cubicOut'
  })

  const top10Data=[['A引风机轴承温度',3],['主汽温度',4],['润滑油压力',3],['NOx浓度',5],['A磨煤机振动',2],['除氧器水位',3],['D磨煤机电流',2],['给水泵B效率',1],['冷却塔水温',2],['高加上端差',1]]
  const durData=[['A引风机轴承',48],['润滑油压力',35],['D磨煤机',28],['主汽温度',22],['A磨煤机',18],['除氧器水位',15],['NOx浓度',12],['给水泵B',8],['冷却塔',5],['高加端差',3]]

  let c4=echarts.init(chTop10.value)
  c4.setOption(barOpt(top10Data.map(d=>d[0]),top10Data.map(d=>d[1]), new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#fbbf24'},{offset:1,color:'rgba(251,191,36,0.6)'}]), '次'))
  charts.push(c4)

  let c5=echarts.init(chDur.value)
  c5.setOption(barOpt(durData.map(d=>d[0]),durData.map(d=>d[1]), new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#f87171'},{offset:1,color:'rgba(248,113,113,0.6)'}]), '分钟'))
  charts.push(c5)
}

watch(()=>[store.selectedUnitId, period.value, store.alarms.length], ()=>nextTick(initCharts))
onMounted(()=>nextTick(initCharts))
onUnmounted(()=>charts.forEach(c=>c.dispose()))
</script>

<style scoped>
.stats-wrap { display:flex; flex-direction:column; gap:12px; min-height:100%; }

/* 时间切换 */
.period-header { background:linear-gradient(180deg, rgba(8,20,40,0.45), rgba(6,16,32,0.5)); border:1px solid rgba(62,170,255,0.1); border-radius:5px; padding:6px 16px; }
.period-tabs { --el-bg-color: transparent; }
:deep(.period-tabs .el-tabs__nav-wrap::after) { background-color: transparent; }
:deep(.period-tabs .el-tabs__item) { color:#8fb0cf; font-size:13px; padding:0 16px; }
:deep(.period-tabs .el-tabs__item.is-active) { color:#3eaaff; font-weight:600; }
:deep(.period-tabs .el-tabs__active-bar) { background:#3eaaff; box-shadow:0 0 8px rgba(62,170,255,0.4); }

/* 统计概览 */
.stats-overview { display:grid; grid-template-columns:repeat(4,1fr); gap:12px; }
.sc {
  position:relative;
  display:flex; flex-direction:column; align-items:center; gap:4px;
  padding:16px 12px;
  background:linear-gradient(180deg, rgba(8,20,40,0.55), rgba(6,16,32,0.6));
  border:1px solid rgba(62,170,255,0.12);
  border-radius:5px;
  transition:all 0.25s;
}
.sc:hover { border-color:rgba(62,170,255,0.3); transform:translateY(-2px); box-shadow:0 6px 18px rgba(0,10,30,0.35); }
.sc-icon { width:32px; height:32px; border-radius:8px; display:flex; align-items:center; justify-content:center; font-size:14px; margin-bottom:4px; }
.sl { font-size:11px; color:#8fb0cf; letter-spacing:0.5px; }
.sv { font-size:28px; font-weight:700; font-family:"SF Mono","Consolas",monospace; line-height:1; text-shadow:0 2px 8px rgba(0,0,0,0.3); }
.ss { font-size:11px; color:#8fb0cf; margin-top:2px; }
.sv.in { color:#5fb3ff; }
.sv.ok { color:#34d399; }
.sv.dg { color:#ef4444; }

/* 饼图行 */
.pie-row { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; }
.pie-col { min-height:260px; display:flex; flex-direction:column; }
.pie-chart { flex:1; min-height:220px; }
.cd-sub { font-size:11px; color:#8fb0cf; }

/* 柱状图行 */
.bar-row { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.bar-col { min-height:300px; display:flex; flex-direction:column; }
.bar-chart { flex:1; min-height:260px; }

/* AI洞察 */
.insight-block {
  background:linear-gradient(180deg, rgba(10,26,50,0.5), rgba(6,16,32,0.55));
  border:1px solid rgba(62,170,255,0.1);
  border-radius:6px;
  padding:14px 16px;
}
.insight-t { font-size:14px; font-weight:600; color:#5fb3ff; margin-bottom:12px; display:flex; align-items:center; gap:8px; }
.it-ic { width:0; height:0; border-left:7px solid #3eaaff; border-top:4px solid transparent; border-bottom:4px solid transparent; filter:drop-shadow(0 0 3px rgba(62,170,255,0.5)); }
.insight-list { display:flex; flex-direction:column; gap:8px; }
.ins-row { display:flex; gap:12px; padding:12px 14px; border-radius:5px; border-left:3px solid; transition:transform 0.15s; }
.ins-row:hover { transform:translateX(2px); }
.ins-row.danger { background:rgba(239,68,68,0.04); border-color:#ef4444; }
.ins-row.warn { background:rgba(245,158,11,0.04); border-color:#fbbf24; }
.ins-row.info { background:rgba(62,170,255,0.04); border-color:#3eaaff; }
.ins-row.ok { background:rgba(52,211,153,0.04); border-color:#34d399; }
.ins-i { font-size:20px; line-height:1.3; flex-shrink:0; }
.ins-b { flex:1; }
.ins-title { font-size:13px; font-weight:600; color:#e2e8f0; margin-bottom:4px; }
.ins-content { font-size:12px; color:#8fb0cf; line-height:1.65; }

@media(max-width:1200px){
  .stats-overview { grid-template-columns:repeat(2,1fr); }
  .pie-row { grid-template-columns:1fr 1fr; }
  .pie-row>.pie-col:last-child { grid-column:span 2; }
  .bar-row { grid-template-columns:1fr; }
}
@media(max-width:800px){
  .stats-overview { grid-template-columns:1fr 1fr; }
  .pie-row { grid-template-columns:1fr; }
}
</style>
