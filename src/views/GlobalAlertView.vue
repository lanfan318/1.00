<template>
<div class="bs">
  <div class="bs-head">
    <div class="bs-title">火电运行全域告警监控大屏</div>
    <div class="bs-clock">{{ clock }}</div>
  </div>

  <el-row :gutter="12" class="bs-row">
    <el-col :span="4"><div class="kpi"><div class="kpi-l">总告警</div><div class="kpi-v" style="color:#3b82f6">128</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">一级</div><div class="kpi-v" style="color:#ef4444">5</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">二级</div><div class="kpi-v" style="color:#f59e0b">23</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">智能预警</div><div class="kpi-v" style="color:#06b6d4">100</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">未处理</div><div class="kpi-v" style="color:#ef4444">12</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">在线设备</div><div class="kpi-v" style="color:#22c55e">96/96</div></div></el-col>
  </el-row>

  <el-row :gutter="12" class="bs-row">
    <el-col :span="8">
      <div class="cd"><div class="cd-t">告警专业分布</div><div ref="profRef" class="cd-chart"></div></div>
    </el-col>
    <el-col :span="8">
      <div class="cd"><div class="cd-t">近24小时告警趋势</div><div ref="trendRef" class="cd-chart"></div></div>
    </el-col>
    <el-col :span="8">
      <div class="cd"><div class="cd-t">最近告警</div>
        <div class="al-list">
          <div v-for="a in recentAlarms" :key="a.id" class="al-row">
            <span class="al-dot" :style="{background:a.l===1?'#ef4444':a.l===2?'#f59e0b':'#06b6d4'}"></span>
            <span class="al-t">{{ a.time }}</span>
            <span class="al-d">{{ a.desc }}</span>
          </div>
        </div>
      </div>
    </el-col>
  </el-row>

  <div class="cd">
    <div class="cd-t">设备健康度矩阵</div>
    <div class="hg-grid">
      <div v-for="d in devices" :key="d.id" class="hg-cell" :class="d.h>=90?'hg-ok':d.h>=80?'hg-warn':'hg-dg'">
        <div class="hg-n">{{ d.n }}</div>
        <div class="hg-v">{{ d.h.toFixed(1) }}</div>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const clock = ref('')
let timer
onMounted(() => {
  clock.value = new Date().toLocaleString('zh-CN', { hour12: false })
  timer = setInterval(() => { clock.value = new Date().toLocaleString('zh-CN', { hour12: false }) }, 1000)
})

const recentAlarms = [
  { id:1,l:1,time:'16:18:32',desc:'A引风机轴承温度异常'},
  { id:2,l:1,time:'16:10:15',desc:'润滑油压力低于阈值'},
  { id:3,l:2,time:'15:52:08',desc:'锅炉主汽温度偏高'},
  { id:4,l:2,time:'15:35:44',desc:'A磨煤机振动超标'},
  { id:5,l:2,time:'15:20:11',desc:'汽轮机轴向位移预警'},
  { id:6,l:3,time:'14:58:30',desc:'D磨煤机电流波动'},
  { id:7,l:3,time:'14:42:05',desc:'除氧器水位偏低'},
]

const devices = [
  {id:1,n:'A送风机',h:97.2},{id:2,n:'B送风机',h:95.8},
  {id:3,n:'A引风机',h:82.3},{id:4,n:'B引风机',h:96.1},
  {id:5,n:'A磨煤机',h:85.0},{id:6,n:'B磨煤机',h:93.2},
  {id:7,n:'C磨煤机',h:94.7},{id:8,n:'D磨煤机',h:78.5},
  {id:9,n:'A一次风机',h:91.4},{id:10,n:'B一次风机',h:88.9},
  {id:11,n:'给水泵A',h:98.4},{id:12,n:'给水泵B',h:97.7},
  {id:13,n:'润滑油系统',h:94.2},{id:14,n:'凝汽器',h:96.3},
  {id:15,n:'高压加热器',h:89.5},{id:16,n:'除氧器',h:95.6},
]

const profRef = ref(null), trendRef = ref(null)
let pCh, tCh
onMounted(() => {
  pCh = echarts.init(profRef.value)
  pCh.setOption({ series: [{ type:'pie',radius:['45%','72%'],center:['50%','50%'],label:{color:'#94a3b8'},
    data:[{value:56,name:'锅炉',itemStyle:{color:'#f59e0b'}},{value:42,name:'汽机',itemStyle:{color:'#3b82f6'}},{value:30,name:'辅网',itemStyle:{color:'#22c55e'}}] }] })

  tCh = echarts.init(trendRef.value)
  tCh.setOption({ grid:{left:36,right:14,top:14,bottom:24},
    xAxis:{type:'category',data:Array.from({length:24},(_,i)=>i+':00'),axisLabel:{color:'#64748b',fontSize:9,interval:3},axisLine:{lineStyle:{color:'#1e293b'}}},
    yAxis:{type:'value',axisLabel:{color:'#64748b',fontSize:10},splitLine:{lineStyle:{color:'#1e293b'}}},
    legend:{textStyle:{color:'#94a3b8'},top:0,right:0},
    series:[
      {name:'一级',type:'line',smooth:true,data:Array.from({length:24},()=>Math.random()*3),lineStyle:{color:'#ef4444'},areaStyle:{color:'rgba(239,68,68,0.12)'},symbol:'none'},
      {name:'二级',type:'line',smooth:true,data:Array.from({length:24},()=>3+Math.random()*6),lineStyle:{color:'#f59e0b'},areaStyle:{color:'rgba(245,158,11,0.08)'},symbol:'none'},
      {name:'智能预警',type:'line',smooth:true,data:Array.from({length:24},()=>5+Math.random()*8),lineStyle:{color:'#06b6d4'},areaStyle:{color:'rgba(6,182,212,0.06)'},symbol:'none'}
    ]})
})
onUnmounted(() => { clearInterval(timer); pCh?.dispose(); tCh?.dispose() })
</script>

<style scoped>
.bs { min-height:100vh; padding:14px; background:linear-gradient(180deg,#050810,#0a0e17); }
.bs-head { display:flex; justify-content:center; align-items:center; position:relative; padding:8px 0 16px; }
.bs-title { font-size:24px; font-weight:600; background:linear-gradient(90deg,#3b82f6,#06b6d4); -webkit-background-clip:text; -webkit-text-fill-color:transparent; letter-spacing:2px; }
.bs-clock { position:absolute; right:0; color:#94a3b8; font-family:monospace; font-size:13px; }
.bs-row { margin-bottom:12px; }
.kpi { background:rgba(20,27,45,0.6); border:0.5px solid rgba(59,130,246,0.25); border-radius:8px; padding:14px; text-align:center; }
.kpi-l { font-size:12px; color:#94a3b8; margin-bottom:6px; }
.kpi-v { font-size:28px; font-weight:600; }
.cd { background:rgba(20,27,45,0.5); border:0.5px solid #1e293b; border-radius:8px; padding:14px; height:100%; }
.cd-t { font-size:13px; color:#94a3b8; margin-bottom:8px; padding-left:8px; border-left:2px solid #3b82f6; }
.cd-chart { height:220px; }
.al-list { max-height:220px; overflow-y:auto; }
.al-row { display:flex; align-items:center; gap:8px; padding:6px 8px; font-size:12px; border-bottom:0.5px solid #1e293b; }
.al-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.al-t { color:#94a3b8; font-family:monospace; }
.al-d { color:#e2e8f0; }
.hg-grid { display:grid; grid-template-columns:repeat(8,1fr); gap:8px; }
.hg-cell { border-radius:6px; padding:10px; text-align:center; transition:transform 0.15s; }
.hg-cell:hover { transform:scale(1.05); }
.hg-ok { background:rgba(34,197,94,0.15); border:0.5px solid rgba(34,197,94,0.3); }
.hg-warn { background:rgba(245,158,11,0.15); border:0.5px solid rgba(245,158,11,0.3); }
.hg-dg { background:rgba(239,68,68,0.15); border:0.5px solid rgba(239,68,68,0.3); animation:blink 1.5s infinite; }
@keyframes blink { 50%{box-shadow:0 0 12px rgba(239,68,68,0.6)}}
.hg-n { font-size:11px; color:#94a3b8; margin-bottom:4px; }
.hg-v { font-size:18px; font-weight:600; }
.hg-ok .hg-v{color:#22c55e}.hg-warn .hg-v{color:#f59e0b}.hg-dg .hg-v{color:#ef4444}
</style>
