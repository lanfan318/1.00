<template>
<div class="bs">
  <div class="bs-head">
    <div class="bs-title">火电运行全域告警监控大屏 · {{ store.selectedUnit.name }}</div>
    <div class="bs-clock">{{ clock }}</div>
  </div>

  <el-row :gutter="12" class="bs-row">
    <el-col :span="4"><div class="kpi"><div class="kpi-l">总告警</div><div class="kpi-v" style="color:#3b82f6">{{ unitAlarms.length }}</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">一级</div><div class="kpi-v" style="color:#ef4444">{{ cnt(1) }}</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">二级</div><div class="kpi-v" style="color:#f59e0b">{{ cnt(2) }}</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">智能预警</div><div class="kpi-v" style="color:#06b6d4">{{ cnt(3) }}</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">未处理</div><div class="kpi-v" style="color:#ef4444">{{ unitAlarms.filter(a=>a.st==='unhandled').length }}</div></div></el-col>
    <el-col :span="4"><div class="kpi"><div class="kpi-l">设备健康度</div><div class="kpi-v" style="color:#22c55e">{{ avgHealth }}%</div></div></el-col>
  </el-row>

  <el-row :gutter="12" class="bs-row">
    <el-col :span="6">
      <div class="cd"><div class="cd-t">设备健康度矩阵（{{ store.selectedUnit.name }}）</div>
        <div class="hg-grid">
          <div v-for="d in store.unitDevices(store.selectedUnitId)" :key="d.id" class="hg-cell" :class="d.health>=90?'hg-ok':d.health>=80?'hg-warn':'hg-dg'" @click="goDevice(d)">
            <div class="hg-n">{{ d.name }}</div>
            <div class="hg-v">{{ d.health.toFixed(1) }}</div>
          </div>
        </div>
      </div>
    </el-col>
    <el-col :span="6">
      <div class="cd"><div class="cd-t">告警专业分布</div><div ref="profRef" style="height:300px"></div></div>
    </el-col>
    <el-col :span="6">
      <div class="cd"><div class="cd-t">近 24 小时告警趋势</div><div ref="trendRef" style="height:300px"></div></div>
    </el-col>
    <el-col :span="6">
      <div class="cd"><div class="cd-t">最近告警</div>
        <div class="al-list">
          <div v-for="a in recent" :key="a.id" class="al-row">
            <span class="al-dot" :style="{background:a.l===1?'#ef4444':a.l===2?'#f59e0b':'#06b6d4'}"></span>
            <span class="al-t">{{ fmt(a.time) }}</span>
            <span class="al-d">{{ a.desc }}</span>
            <span class="al-st" :class="a.st==='unhandled'?'st-uh':a.st==='confirmed'?'st-cf':'st-ok'">{{ stTxt(a.st) }}</span>
          </div>
        </div>
      </div>
    </el-col>
  </el-row>

  <el-row :gutter="12">
    <el-col :span="12">
      <div class="cd"><div class="cd-t">⚠️ 重点关注设备 TOP5</div>
        <table class="tbl">
          <thead><tr><th style="width:160px">设备</th><th>当前问题</th><th style="width:90px">健康度</th><th>建议</th></tr></thead>
          <tbody>
            <tr v-for="d in topWarn" :key="d.id">
              <td>{{ d.name }}</td>
              <td style="color:#ef4444">{{ d.problem }}</td>
              <td :style="{color:d.health>=90?'#22c55e':d.health>=80?'#f59e0b':'#ef4444'}">{{ d.health.toFixed(1) }}</td>
              <td style="color:#94a3b8">{{ d.suggest }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </el-col>
    <el-col :span="12">
      <div class="cd"><div class="cd-t">🤖 AI 智能洞察</div>
        <div class="ai-insight">
          <div v-for="(ins, i) in insights" :key="i" class="ai-row" :class="ins.type">
            <div class="ai-i">{{ ins.icon }}</div>
            <div class="ai-b">
              <div class="ai-t">{{ ins.title }}</div>
              <div class="ai-c">{{ ins.content }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-col>
  </el-row>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const router = useRouter()
const clock = ref('')
const profRef = ref(null), trendRef = ref(null)
let pCh, tCh, iv

const unitAlarms = computed(() => store.unitAlarms(store.selectedUnitId))
const cnt = (l) => unitAlarms.value.filter(a => a.l === l).length
const avgHealth = computed(() => {
  const ds = store.unitDevices(store.selectedUnitId)
  if (!ds.length) return 0
  return (ds.reduce((s, d) => s + d.health, 0) / ds.length).toFixed(1)
})

const fmt = (t) => { const d = new Date(t); return d.toLocaleTimeString('zh-CN', { hour12: false }).slice(0, 8) }
const stTxt = (st) => st === 'unhandled' ? '未处理' : st === 'confirmed' ? '已确认' : '已处理'

const recent = computed(() => unitAlarms.value.slice(0, 6))

const topWarn = computed(() => {
  const ds = store.unitDevices(store.selectedUnitId).filter(d => d.health < 90)
  return ds.sort((a, b) => a.health - b.health).slice(0, 5).map(d => ({
    ...d,
    problem: d.health < 70 ? '严重异常，建议立即停机检查' : d.health < 80 ? '健康度偏低，需安排检修' : '部分参数超限，需关注',
    suggest: d.dept === '汽轮机' ? '检查轴承润滑/振动情况' : d.name.includes('磨') ? '检查磨辊磨损/煤质' : '加强巡检频次'
  }))
})

const insights = computed(() => {
  const out = []
  const ds = store.unitDevices(store.selectedUnitId)
  const lowH = ds.filter(d => d.health < 80)
  if (lowH.length) out.push({ type: 'danger', icon: '🚨', title: `设备健康度告警`, content: `${store.selectedUnit.name} 有 ${lowH.length} 台设备健康度低于 80：${lowH.map(d => d.name).join('、')}。建议立即启动预防性检修流程。` })
  const l1 = unitAlarms.value.filter(a => a.l === 1 && a.st === 'unhandled')
  if (l1.length) out.push({ type: 'danger', icon: '⚠️', title: '一级报警未处置', content: `${l1.length} 条一级报警处于未处理状态，超时将自动升级。立即查看并处理：${l1.slice(0, 2).map(a => a.desc).join('；')}。` })
  const idfWarn = ds.find(d => d.name === 'A引风机' && d.health < 85)
  if (idfWarn) out.push({ type: 'warn', icon: '🌡️', title: 'A引风机温度预警', content: `轴承温度偏离正常值，温升速率加快。AI 建议：48h 内安排润滑油脂更换，避免发展到停机检修。` })
  const millWarn = ds.find(d => d.name.includes('磨煤机') && d.health < 85)
  if (millWarn) out.push({ type: 'warn', icon: '⚙️', title: '磨煤机振动预警', content: `磨辊磨损不均导致动不平衡。AI 建议：降低给煤量至 70%，安排磨辊更换备件。` })
  const emCheck = unitAlarms.value.filter(a => a.point?.includes('NOx'))
  if (emCheck.length === 0) out.push({ type: 'info', icon: '✅', title: '环保排放正常', content: `NOx/SO2/粉尘均在国标限值 70% 以下，达标率 ${(99 + Math.random()).toFixed(1)}%。建议保持当前燃烧参数。` })
  const boilerWarn = unitAlarms.value.filter(a => a.dept === '锅炉').length
  const total = unitAlarms.value.length
  if (total > 0) {
    const ratio = ((boilerWarn / total) * 100).toFixed(0)
    out.push({ type: 'info', icon: '📊', title: '报警分布洞察', content: `锅炉专业报警占比 ${ratio}%，主要集中在引风机和磨煤机。AI 建议：本周对锅炉设备开展一次专项检查。` })
  }
  if (out.length === 0) out.push({ type: 'ok', icon: '✅', title: '系统运行良好', content: `${store.selectedUnit.name} 各设备运行正常，无需特别关注。` })
  return out
})

const goDevice = (d) => {
  store.selectedDevice = d.id
  router.push('/devices')
}

const initCharts = () => {
  if (pCh) pCh.dispose(); if (tCh) tCh.dispose()
  pCh = echarts.init(profRef.value)
  const profCnt = { '锅炉': 0, '汽机': 0, '辅网': 0 }
  unitAlarms.value.forEach(a => { profCnt[a.dept] = (profCnt[a.dept] || 0) + 1 })
  pCh.setOption({
    legend: { textStyle: { color: '#94a3b8' }, top: 0, right: 0 },
    series: [{ type: 'pie', radius: ['45%', '72%'], center: ['50%', '55%'],
      data: [
        { value: profCnt['锅炉'], name: '锅炉', itemStyle: { color: '#f59e0b' } },
        { value: profCnt['汽机'], name: '汽机', itemStyle: { color: '#3b82f6' } },
        { value: profCnt['辅网'], name: '辅网', itemStyle: { color: '#22c55e' } }
      ].filter(d => d.value > 0),
      label: { color: '#94a3b8', fontSize: 12 } }]
  })

  tCh = echarts.init(trendRef.value)
  tCh.setOption({
    grid: { left: 36, right: 14, top: 24, bottom: 22 },
    legend: { textStyle: { color: '#94a3b8' }, top: 0, right: 0 },
    xAxis: { type: 'category', data: Array.from({ length: 24 }, (_, i) => i + ':00'), axisLabel: { color: '#64748b', fontSize: 9, interval: 3 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'value', axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [
      { name: '一级', type: 'line', smooth: true, data: Array.from({ length: 24 }, () => Math.random() * 2), lineStyle: { color: '#ef4444' }, areaStyle: { color: 'rgba(239,68,68,0.12)' }, symbol: 'none' },
      { name: '二级', type: 'line', smooth: true, data: Array.from({ length: 24 }, () => 3 + Math.random() * 6), lineStyle: { color: '#f59e0b' }, areaStyle: { color: 'rgba(245,158,11,0.08)' }, symbol: 'none' },
      { name: '智能预警', type: 'line', smooth: true, data: Array.from({ length: 24 }, () => 5 + Math.random() * 8), lineStyle: { color: '#06b6d4' }, areaStyle: { color: 'rgba(6,182,212,0.06)' }, symbol: 'none' }
    ]
  })
}

watch(() => store.selectedUnitId, () => nextTick(initCharts))
onMounted(() => {
  clock.value = new Date().toLocaleString('zh-CN', { hour12: false })
  iv = setInterval(() => { clock.value = new Date().toLocaleString('zh-CN', { hour12: false }) }, 1000)
  nextTick(initCharts)
})
onUnmounted(() => { clearInterval(iv); pCh?.dispose(); tCh?.dispose() })
</script>

<style scoped>
.bs { min-height: 100vh; padding: 14px; background: linear-gradient(180deg, #050810, #0a0e17); }
.bs-head { display: flex; justify-content: center; align-items: center; position: relative; padding: 8px 0 16px; }
.bs-title { font-size: 22px; font-weight: 600; background: linear-gradient(90deg, #3b82f6, #06b6d4); -webkit-background-clip: text; -webkit-text-fill-color: transparent; letter-spacing: 2px; }
.bs-clock { position: absolute; right: 0; color: #94a3b8; font-family: monospace; }
.bs-row { margin-bottom: 12px; }
.kpi { background: rgba(20,27,45,0.6); border: 0.5px solid rgba(59,130,246,0.25); border-radius: 8px; padding: 14px; text-align: center; }
.kpi-l { font-size: 12px; color: #94a3b8; margin-bottom: 6px; }
.kpi-v { font-size: 28px; font-weight: 600; }
.cd { background: rgba(20,27,45,0.5); border: 0.5px solid #1e293b; border-radius: 8px; padding: 14px; height: 100%; }
.cd-t { font-size: 13px; color: #94a3b8; margin-bottom: 8px; padding-left: 8px; border-left: 2px solid #3b82f6; }
.hg-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
.hg-cell { border-radius: 5px; padding: 8px 4px; text-align: center; cursor: pointer; transition: transform 0.15s; }
.hg-cell:hover { transform: scale(1.05); }
.hg-ok { background: rgba(34,197,94,0.12); border: 0.5px solid rgba(34,197,94,0.3); }
.hg-warn { background: rgba(245,158,11,0.15); border: 0.5px solid rgba(245,158,11,0.3); }
.hg-dg { background: rgba(239,68,68,0.15); border: 0.5px solid rgba(239,68,68,0.3); animation: blink 1.5s infinite; }
@keyframes blink { 50% { box-shadow: 0 0 12px rgba(239,68,68,0.6); } }
.hg-n { font-size: 11px; color: #94a3b8; margin-bottom: 3px; }
.hg-v { font-size: 16px; font-weight: 600; }
.hg-ok .hg-v { color: #22c55e; } .hg-warn .hg-v { color: #f59e0b; } .hg-dg .hg-v { color: #ef4444; }
.al-list { max-height: 300px; overflow-y: auto; }
.al-row { display: flex; align-items: center; gap: 8px; padding: 6px 4px; font-size: 12px; border-bottom: 0.5px solid #1e293b; }
.al-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.al-t { color: #94a3b8; font-family: monospace; font-size: 11px; }
.al-d { color: #e2e8f0; flex: 1; }
.al-st { font-size: 10px; padding: 1px 6px; border-radius: 3px; flex-shrink: 0; }
.st-uh { background: rgba(239,68,68,0.15); color: #ef4444; }
.st-cf { background: rgba(245,158,11,0.15); color: #f59e0b; }
.st-ok { background: rgba(34,197,94,0.15); color: #22c55e; }
.tbl { width: 100%; border-collapse: collapse; font-size: 12px; }
.tbl th { text-align: left; padding: 6px 8px; color: #64748b; font-weight: 500; font-size: 11px; border-bottom: 0.5px solid #1e293b; }
.tbl td { padding: 8px; border-bottom: 0.5px solid #1e293b; color: #94a3b8; }
.ai-insight { display: flex; flex-direction: column; gap: 8px; max-height: 220px; overflow-y: auto; padding-right: 4px; }
.ai-row { display: flex; gap: 10px; padding: 10px 12px; border-radius: 6px; border-left: 3px solid; }
.ai-row.danger { background: rgba(239,68,68,0.08); border-color: #ef4444; }
.ai-row.warn { background: rgba(245,158,11,0.08); border-color: #f59e0b; }
.ai-row.info { background: rgba(59,130,246,0.08); border-color: #3b82f6; }
.ai-row.ok { background: rgba(34,197,94,0.08); border-color: #22c55e; }
.ai-i { font-size: 20px; line-height: 1.4; flex-shrink: 0; }
.ai-b { flex: 1; }
.ai-t { font-size: 13px; font-weight: 600; color: #e2e8f0; margin-bottom: 3px; }
.ai-c { font-size: 12px; color: #94a3b8; line-height: 1.5; }
</style>
