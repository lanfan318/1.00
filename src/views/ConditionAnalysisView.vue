<template>
<div class="pg">
  <div class="pg-h"><h2>设备工况分析</h2>
    <el-select v-model="sid" style="width:220px"><el-option v-for="d in devs" :key="d.id" :value="d.id" :label="d.n"/></el-select>
  </div>
  <el-row :gutter="14">
    <el-col :span="6"><div class="mc"><div class="ml">健康度</div><div class="mv" :style="{color:c.h>=90?'#22c55e':c.h>=80?'#f59e0b':'#ef4444'}">{{c.h.toFixed(1)}}</div><div class="ms">/100</div></div></el-col>
    <el-col :span="6"><div class="mc"><div class="ml">出力</div><div class="mv" style="color:#3b82f6">{{c.o}}</div><div class="ms">MW</div></div></el-col>
    <el-col :span="6"><div class="mc"><div class="ml">效率</div><div class="mv" :style="{color:c.e>=85?'#22c55e':'#f59e0b'}">{{c.e}}%</div><div class="ms">正常85-95%</div></div></el-col>
    <el-col :span="6"><div class="mc"><div class="ml">累计运行</div><div class="mv" style="color:#06b6d4">{{c.r}}</div><div class="ms">小时</div></div></el-col>
  </el-row>
  <el-row :gutter="14" style="margin-top:14px">
    <el-col :span="12"><div class="cd"><div class="cd-t">关键参数趋势</div><div ref="ct" style="height:240px"></div></div></el-col>
    <el-col :span="12"><div class="cd"><div class="cd-t">效率仪表盘</div><div ref="cg" style="height:240px"></div></div></el-col>
  </el-row>
  <div class="cd" style="margin-top:14px"><div class="cd-t">AI 工况分析</div>
    <div class="ai"><p><span class="tg tg-i">机械负荷</span> 设备出力 500.0 MW，处于额定工况范围。</p>
      <p><span class="tg tg-w">温度</span> 轴承温度 82.3℃，较正常偏高 37℃，建议加强巡检。</p>
      <p><span class="tg tg-i">效率</span> 当前效率 51%，处于正常范围。</p>
      <p><span class="tg tg-ok">综合</span> 整体运行良好，无立即停机风险。</p></div></div>
</div>
</template>

<script setup>
import { ref,reactive,onMounted,onUnmounted,watch } from 'vue'
import * as echarts from 'echarts'
const devs = [{id:1,n:'A引风机',h:82.3,o:500,e:51,r:8432},{id:2,n:'A送风机',h:97.2,o:280,e:78,r:6200},{id:3,n:'A磨煤机',h:85.0,o:95,e:82,r:7100},{id:4,n:'给水泵A',h:98.4,o:180,e:92,r:5400}]
const sid=ref(1)
const c=reactive({h:82.3,o:500,e:51,r:8432})
watch(sid,v=>{const d=devs.find(x=>x.id===v);Object.assign(c,d)})
const ct=ref(null),cg=ref(null)
let tCh,gCh
onMounted(()=>{
  tCh=echarts.init(ct.value)
  tCh.setOption({grid:{left:36,right:14,top:14,bottom:24},legend:{textStyle:{color:'#94a3b8'},top:0,right:0},
    xAxis:{type:'category',data:Array.from({length:20},(_,i)=>i*3+'s'),axisLabel:{color:'#64748b',fontSize:9},axisLine:{lineStyle:{color:'#1e293b'}}},
    yAxis:{type:'value',axisLabel:{color:'#64748b'},splitLine:{lineStyle:{color:'#1e293b'}}},
    series:[{name:'温度',type:'line',smooth:true,data:Array.from({length:20},()=>80+Math.random()*5),lineStyle:{color:'#f59e0b'},areaStyle:{color:'rgba(245,158,11,0.08)'},symbol:'none'},
      {name:'振动',type:'line',smooth:true,data:Array.from({length:20},()=>3+Math.random()*2),lineStyle:{color:'#3b82f6'},symbol:'none'}]})
  gCh=echarts.init(cg.value)
  gCh.setOption({series:[{type:'gauge',min:0,max:100,progress:{show:true,width:8},axisLine:{lineStyle:{width:8,color:[[0.6,'#ef4444'],[0.85,'#f59e0b'],[1,'#22c55e']]}},pointer:{show:false},axisTick:{show:false},splitLine:{show:false},axisLabel:{show:false},data:[{value:51,detail:{valueAnimation:true,fontSize:30,color:'#22c55e',formatter:'{value}%'},name:'效率'}]}]})
})
onUnmounted(()=>{tCh?.dispose();gCh?.dispose()})
</script>
<style scoped>
.pg{background:#111827;padding:20px;border-radius:10px}
.pg-h{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}
h2{font-size:16px;font-weight:500}
.mc{background:#0a0e17;border-radius:8px;padding:18px;text-align:center;border:0.5px solid #1e293b}
.ml{font-size:12px;color:#94a3b8;margin-bottom:4px}
.mv{font-size:28px;font-weight:600}.ms{font-size:11px;color:#64748b;margin-top:4px}
.cd{background:#0a0e17;border:0.5px solid #1e293b;border-radius:8px;padding:16px}
.cd-t{font-size:13px;color:#94a3b8;margin-bottom:10px}
.ai p{line-height:1.8;font-size:13px;color:#cbd5e1;margin-bottom:6px}
.tg{display:inline-block;padding:2px 8px;border-radius:4px;font-size:11px;margin-right:6px}
.tg-i{background:rgba(59,130,246,0.12);color:#3b82f6}.tg-w{background:rgba(245,158,11,0.12);color:#f59e0b}.tg-ok{background:rgba(34,197,94,0.12);color:#22c55e}
</style>
