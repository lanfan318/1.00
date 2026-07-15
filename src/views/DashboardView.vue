<template>
<div>
  <el-row :gutter="14" style="margin-bottom:14px">
    <el-col :span="12"><div class="cd"><div class="cd-h"><span class="cd-t"><span class="sd ok"></span>机组实时概览</span><span class="tg tg-ok">运行正常</span></div>
      <el-row :gutter="10">
        <el-col :span="6"><div class="mc"><div class="ml">负荷</div><div class="mv" style="color:#3b82f6">{{ load }}</div><div class="ms">MW</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">主汽压力</div><div class="mv" style="color:#22c55e">{{ press }}</div><div class="ms">MPa</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">主汽温度</div><div class="mv" style="color:#22c55e">{{ temp }}</div><div class="ms">℃</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">煤耗</div><div class="mv" style="color:#3b82f6">{{ coal }}</div><div class="ms">g/kWh</div></div></el-col>
      </el-row></div></el-col>
    <el-col :span="12"><div class="cd"><div class="cd-h"><span class="cd-t">环保排放指标</span><span class="tg tg-ok">达标</span></div>
      <el-row :gutter="10">
        <el-col :span="6"><div class="mc"><div class="ml">NOx</div><div class="mv" style="color:#22c55e">{{ nox }}</div><div class="ms">mg/m³</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">SO2</div><div class="mv" style="color:#22c55e">{{ so2 }}</div><div class="ms">mg/m³</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">粉尘</div><div class="mv" style="color:#22c55e">{{ dust }}</div><div class="ms">mg/m³</div></div></el-col>
        <el-col :span="6"><div class="mc"><div class="ml">达标率</div><div class="mv" style="color:#3b82f6">{{ em }}</div><div class="ms">%</div></div></el-col>
      </el-row></div></el-col>
  </el-row>

  <el-row :gutter="14" style="margin-bottom:14px">
    <el-col :span="12"><div class="cd"><div class="cd-t"><span class="sd ok"></span>锅炉设备健康度</div>
      <div v-for="d in boiler" :key="d.n" class="hb-row">
        <span class="hb-n">{{ d.n }}</span>
        <div class="hb-b"><div class="hb-f" :class="d.h>=90?'ok':d.h>=80?'wn':'dg'" :style="{width:d.h+'%'}"></div></div>
        <span class="hb-v" :style="{color:d.h>=90?'#22c55e':d.h>=80?'#f59e0b':'#ef4444'}">{{ d.h.toFixed(1) }}</span>
      </div></div></el-col>
    <el-col :span="12"><div class="cd"><div class="cd-t"><span class="sd ok"></span>汽轮机设备健康度</div>
      <div v-for="d in turbine" :key="d.n" class="hb-row">
        <span class="hb-n">{{ d.n }}</span>
        <div class="hb-b"><div class="hb-f" :class="d.h>=90?'ok':d.h>=80?'wn':'dg'" :style="{width:d.h+'%'}"></div></div>
        <span class="hb-v" :style="{color:d.h>=90?'#22c55e':d.h>=80?'#f59e0b':'#ef4444'}">{{ d.h.toFixed(1) }}</span>
      </div></div></el-col>
  </el-row>

  <el-row :gutter="14">
    <el-col :span="8"><div class="cd"><div class="cd-t">负荷趋势</div><div ref="ch1" style="height:200px"></div></div></el-col>
    <el-col :span="8"><div class="cd"><div class="cd-t">主汽温度趋势</div><div ref="ch2" style="height:200px"></div></div></el-col>
    <el-col :span="8"><div class="cd"><div class="cd-t">自动/联锁投入率</div><div ref="ch3" style="height:200px"></div></div></el-col>
  </el-row>

  <div class="insight"><div class="in-t">AI 运行洞察</div><div class="in-b">A引风机健康度 82.3 偏低，D磨煤机 78.5 需关注。主汽温度 541℃ 运行平稳。建议优先安排 A引风机润滑油脂更换。</div></div>
</div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const load = ref('500.0')
const press = ref('16.72')
const temp = ref('541.2')
const coal = ref('298.5')
const nox = ref('42.8')
const so2 = ref('18.3')
const dust = ref('5.8')
const em = ref('99.7')

const boiler = [
  { n: 'A送风机', h: 97.2 }, { n: 'B送风机', h: 95.8 },
  { n: 'A引风机', h: 82.3 }, { n: 'B引风机', h: 96.1 },
  { n: 'A一次风机', h: 91.4 }, { n: 'B一次风机', h: 88.9 },
  { n: 'A磨煤机', h: 85.0 }, { n: 'D磨煤机', h: 78.5 }
]
const turbine = [
  { n: '给水泵A', h: 98.4 }, { n: '给水泵B', h: 97.7 },
  { n: '润滑油系统', h: 94.2 }, { n: '轴封系统', h: 91.8 },
  { n: '凝汽器', h: 96.3 }, { n: '高压加热器', h: 89.5 },
  { n: '除氧器', h: 95.6 }
]

const ch1 = ref(null)
const ch2 = ref(null)
const ch3 = ref(null)
let c1, c2, c3, iv

onMounted(() => {
  const ts = Array.from({ length: 30 }, (_, i) => {
    const d = new Date()
    d.setMinutes(d.getMinutes() - 29 + i)
    return d.toTimeString().slice(0, 5)
  })

  c1 = echarts.init(ch1.value)
  c1.setOption({
    grid: { left: 36, right: 14, top: 12, bottom: 22 },
    xAxis: { type: 'category', data: ts, axisLabel: { color: '#64748b', fontSize: 8, interval: 5 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'value', name: 'MW', axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [{
      type: 'line', smooth: true,
      data: Array.from({ length: 30 }, () => 488 + Math.random() * 24),
      lineStyle: { color: '#3b82f6', width: 1.5 },
      areaStyle: { color: 'rgba(59,130,246,0.08)' },
      symbol: 'none'
    }]
  })

  c2 = echarts.init(ch2.value)
  c2.setOption({
    grid: { left: 36, right: 14, top: 12, bottom: 22 },
    xAxis: { type: 'category', data: ts, axisLabel: { color: '#64748b', fontSize: 8, interval: 5 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'value', name: '℃', axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [
      { type: 'line', smooth: true, data: Array.from({ length: 30 }, () => 538 + Math.random() * 8), lineStyle: { color: '#f59e0b', width: 1.5 }, areaStyle: { color: 'rgba(245,158,11,0.06)' }, symbol: 'none' },
      { type: 'line', data: Array(30).fill(545), lineStyle: { color: '#ef4444', width: 1, type: 'dashed' }, symbol: 'none' }
    ]
  })

  c3 = echarts.init(ch3.value)
  c3.setOption({
    grid: { left: 36, right: 14, top: 12, bottom: 22 },
    xAxis: { type: 'category', data: ts, axisLabel: { color: '#64748b', fontSize: 8, interval: 5 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'value', name: '%', min: 95, axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [
      { type: 'line', smooth: true, data: Array.from({ length: 30 }, () => 98.5 + Math.random()), lineStyle: { color: '#22c55e', width: 1.5 }, symbol: 'none', name: '自动' },
      { type: 'line', smooth: true, data: Array.from({ length: 30 }, () => 97.2 + Math.random()), lineStyle: { color: '#06b6d4', width: 1.5 }, symbol: 'none', name: '联锁' }
    ]
  })

  iv = setInterval(() => {
    load.value = (488 + Math.random() * 24).toFixed(1)
    temp.value = (538 + Math.random() * 8).toFixed(1)
    coal.value = (296 + Math.random() * 5).toFixed(1)
    nox.value = (38 + Math.random() * 18).toFixed(1)
  }, 3000)
})

onUnmounted(() => {
  clearInterval(iv)
  c1 && c1.dispose()
  c2 && c2.dispose()
  c3 && c3.dispose()
})
</script>

<style scoped>
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-h { display: flex; justify-content: space-between; margin-bottom: 12px; }
.cd-t { font-size: 13px; font-weight: 500; }
.sd { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 6px; }
.sd.ok { background: #22c55e; }
.sd.wn { background: #f59e0b; }
.sd.dg { background: #ef4444; }
.tg { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; }
.tg-ok { background: rgba(34,197,94,0.12); color: #22c55e; }
.tg-w { background: rgba(245,158,11,0.12); color: #f59e0b; }
.mc { background: #0a0e17; border-radius: 6px; padding: 14px; text-align: center; }
.ml { font-size: 11px; color: #94a3b8; margin-bottom: 4px; }
.mv { font-size: 24px; font-weight: 600; }
.ms { font-size: 10px; color: #64748b; margin-top: 2px; }
.hb-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; font-size: 12px; }
.hb-n { width: 75px; color: #94a3b8; }
.hb-b { flex: 1; height: 5px; background: #1e293b; border-radius: 3px; overflow: hidden; }
.hb-f { height: 100%; border-radius: 3px; transition: width 0.6s; }
.hb-f.ok { background: #22c55e; }
.hb-f.wn { background: #f59e0b; }
.hb-f.dg { background: #ef4444; }
.hb-v { width: 36px; text-align: right; font-weight: 500; }
.insight { background: #0a0e17; border: 0.5px solid #1e293b; border-radius: 10px; padding: 14px; margin-top: 14px; }
.in-t { font-size: 12px; font-weight: 500; color: #f59e0b; margin-bottom: 6px; }
.in-b { font-size: 12px; color: #94a3b8; line-height: 1.7; }
</style>
