<template>
<div>
  <el-row :gutter="14" style="margin-bottom:14px">
    <el-col :span="6"><div class="sc"><div class="sl">报警总数</div><div class="sv" style="color:#3b82f6">128</div></div></el-col>
    <el-col :span="6"><div class="sc"><div class="sl">未处理</div><div class="sv" style="color:#ef4444">12</div></div></el-col>
    <el-col :span="6"><div class="sc"><div class="sl">平均响应</div><div class="sv" style="color:#f59e0b">2.3</div><div class="ss">分钟</div></div></el-col>
    <el-col :span="6"><div class="sc"><div class="sl">智能过滤</div><div class="sv" style="color:#22c55e">8</div><div class="ss">条误报</div></div></el-col>
  </el-row>
  <el-row :gutter="14">
    <el-col :span="12"><div class="cd"><div class="cd-t">报警专业占比</div><div ref="p1" style="height:220px"></div></div></el-col>
    <el-col :span="12"><div class="cd"><div class="cd-t">级别占比</div><div ref="p2" style="height:220px"></div></div></el-col>
  </el-row>
  <el-row :gutter="14" style="margin-top:14px">
    <el-col :span="12"><div class="cd"><div class="cd-t">报警次数 Top10</div><div ref="b1" style="height:240px"></div></div></el-col>
    <el-col :span="12"><div class="cd"><div class="cd-t">报警时长 Top10</div><div ref="b2" style="height:240px"></div></div></el-col>
  </el-row>
  <div class="in"><div class="in-t">AI 数据分析洞察</div><div class="in-b">
    1. 今日锅炉专业报警占比 41.7% 最高，引风机与磨煤机为主。<br>
    2. 平均响应 2.3 分钟优于行业平均 5 分钟。<br>
    3. A引风机本周已触发 3 次，建议安排预防性检修。<br>
    4. 智能预警过滤 8 条潜在误报，减少不必要现场出动。
  </div></div>
</div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
const p1=ref(null),p2=ref(null),b1=ref(null),b2=ref(null)
let charts=[]
onMounted(()=>{
  const pie=(el,data)=>{const c=echarts.init(el.value);c.setOption({series:[{type:'pie',radius:['45%','72%'],center:['50%','50%'],data,label:{color:'#94a3b8',fontSize:11}}]});charts.push(c)}
  const bar=(el,data,color)=>{const c=echarts.init(el.value);const d=data.map(x=>x[0]).reverse();const v=data.map(x=>x[1]).reverse();c.setOption({grid:{left:115,right:16,top:6,bottom:18},xAxis:{type:'value',axisLabel:{color:'#64748b',fontSize:10},splitLine:{lineStyle:{color:'#1e293b'}}},yAxis:{type:'category',data:d,axisLabel:{color:'#94a3b8',fontSize:10},axisLine:{lineStyle:{color:'#1e293b'}}},series:[{type:'bar',data:v,itemStyle:{color,borderRadius:[0,3,3,0]}}]});charts.push(c)}
  pie(p1,[{value:56,name:'锅炉',itemStyle:{color:'#f59e0b'}},{value:42,name:'汽机',itemStyle:{color:'#3b82f6'}},{value:30,name:'辅网',itemStyle:{color:'#22c55e'}}])
  pie(p2,[{value:5,name:'一级',itemStyle:{color:'#ef4444'}},{value:23,name:'二级',itemStyle:{color:'#f59e0b'}},{value:100,name:'智能预警',itemStyle:{color:'#3b82f6'}}])
  bar(b1,[['A引风机温度',3],['主汽温度',4],['润滑油压力',3],['NOx浓度',5],['A磨煤机振动',2],['除氧器水位',3],['D磨煤机电流',2],['给水泵B效率',1],['冷却塔水温',2],['高加上端差',1]],'#f59e0b')
  bar(b2,[['A引风机',48],['润滑油压力',35],['D磨煤机',28],['主汽温度',22],['A磨煤机',18],['除氧器水位',15],['NOx浓度',12],['给水泵B',8],['冷却塔',5],['高加端差',3]],'#ef4444')
})
onUnmounted(()=>charts.forEach(c=>c.dispose()))
</script>
<style scoped>
.sc{background:#0a0e17;border-radius:8px;padding:18px;text-align:center;border:0.5px solid #1e293b}.sl{font-size:11px;color:#94a3b8;margin-bottom:6px}.sv{font-size:28px;font-weight:600}.ss{font-size:10px;color:#64748b;margin-top:4px}
.cd{background:#111827;border:0.5px solid #1e293b;border-radius:10px;padding:16px}.cd-t{font-size:13px;font-weight:500;margin-bottom:10px}
.in{background:#0a0e17;border:0.5px solid #1e293b;border-radius:10px;padding:14px;margin-top:14px}.in-t{font-size:12px;font-weight:500;color:#3b82f6;margin-bottom:6px}.in-b{font-size:12px;color:#94a3b8;line-height:1.8}
</style>
