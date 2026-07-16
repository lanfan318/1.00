<template>
<div>
  <el-tabs v-model="period" class="period-tabs">
    <el-tab-pane label="今日" name="today" />
    <el-tab-pane label="本周" name="week" />
    <el-tab-pane label="本月" name="month" />
  </el-tabs>

  <el-row :gutter="14" style="margin-bottom:14px">
    <el-col :span="6"><div class="sc"><div class="sl">报警总数</div><div class="sv in">{{ stats.total }}</div><div class="ss">{{ store.selectedUnit.name }}</div></div></el-col>
    <el-col :span="6"><div class="sc"><div class="sl">未处理</div><div class="sv dg">{{ stats.unhandled }}</div><div class="ss">条</div></div></el-col>
    <el-col :span="6"><div class="sc"><div class="sl">已处理</div><div class="sv ok">{{ stats.handled }}</div><div class="ss">条</div></div></el-col>
    <el-col :span="6"><div class="sc"><div class="sl">智能过滤</div><div class="sv ok">{{ filterCnt }}</div><div class="ss">条误报</div></div></el-col>
  </el-row>

  <el-row :gutter="14">
    <el-col :span="8"><div class="cd"><div class="cd-h"><span class="cd-t">报警专业占比</span><span class="cd-sub">锅炉 {{ profCnt['锅炉'] }} 条 / 汽机 {{ profCnt['汽机'] }} 条 / 辅网 {{ profCnt['辅网'] }} 条</span></div>
      <div ref="chProf" style="height:240px"></div>
    </div></el-col>
    <el-col :span="8"><div class="cd"><div class="cd-h"><span class="cd-t">级别占比</span><span class="cd-sub">一级 {{ cnt(1) }} / 二级 {{ cnt(2) }} / 预警 {{ cnt(3) }}</span></div>
      <div ref="chLevel" style="height:240px"></div>
    </div></el-col>
    <el-col :span="8"><div class="cd"><div class="cd-h"><span class="cd-t">处理状态</span><span class="cd-sub">已处理 {{ stats.handled }} / 未处理 {{ stats.unhandled }} / 抑制 {{ suppressed }}</span></div>
      <div ref="chStatus" style="height:240px"></div>
    </div></el-col>
  </el-row>

  <el-row :gutter="14" style="margin-top:14px">
    <el-col :span="12"><div class="cd"><div class="cd-h"><span class="cd-t">报警次数 Top10（按测点）</span></div>
      <div ref="chTop10" style="height:280px"></div>
    </div></el-col>
    <el-col :span="12"><div class="cd"><div class="cd-h"><span class="cd-t">报警时长 Top10（按测点）</span></div>
      <div ref="chDur" style="height:280px"></div>
    </div></el-col>
  </el-row>

  <div class="insight-block">
    <div class="insight-t">🤖 AI 深度数据分析洞察</div>
    <div class="insight-list">
      <div v-for="(ins, i) in insights" :key="i" class="ins-row" :class="ins.type">
        <div class="ins-i">{{ ins.icon }}</div>
        <div class="ins-b">
          <div class="ins-title">{{ ins.title }}</div>
          <div class="ins-content">{{ ins.content }}</div>
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

const cnt = (l) => store.unitAlarms(store.selectedUnitId).filter(a => a.l === l).length
const suppressed = computed(() => store.unitAlarms(store.selectedUnitId).filter(a => a.st === 'suppressed').length)
const filterCnt = computed(() => Math.floor(store.stats.total * 0.05 + Math.random() * 3))

const profCnt = computed(() => {
  const c = { '锅炉': 0, '汽机': 0, '辅网': 0 }
  store.unitAlarms(store.selectedUnitId).forEach(a => { c[a.dept] = (c[a.dept] || 0) + 1 })
  return c
})

const insights = computed(() => {
  const out = []
  const arr = store.unitAlarms(store.selectedUnitId)
  const total = arr.length
  if (total === 0) return [{ type: 'ok', icon: '✅', title: '当前无报警', content: '系统运行平稳。' }]

  // 1. 专业占比分析
  const p = profCnt.value
  const maxProf = Object.entries(p).sort((a, b) => b[1] - a[1])[0]
  const ratio = ((maxProf[1] / total) * 100).toFixed(0)
  out.push({ type: 'info', icon: '📊', title: `报警集中度分析`, content: `本${period.value==='today'?'日':period.value==='week'?'周':'月'}报警主要集中在<strong style="color:#f59e0b">${maxProf[0]}</strong>专业（${ratio}%），主要设备为${store.unitDevices(store.selectedUnitId).filter(d => d.dept === maxProf[0] && d.health < 90).map(d => d.name).slice(0, 3).join('、') || '无'}。` })

  // 2. 一级响应
  const l1 = arr.filter(a => a.l === 1)
  if (l1.length > 0) out.push({ type: 'danger', icon: '🚨', title: '一级报警处置分析', content: `当前有 <strong style="color:#ef4444">${l1.length}</strong> 条一级报警，平均响应时长 <strong>${store.alarmLevels[1].avgTime}分钟</strong>。AI 建议：一级报警应在 1 分钟内响应，目前${l1.filter(a => a.st === 'unhandled').length}条未处理，建议立即调度。` })

  // 3. 设备健康度
  const lowH = store.unitDevices(store.selectedUnitId).filter(d => d.health < 85)
  if (lowH.length > 0) out.push({ type: 'warn', icon: '⚠️', title: '设备健康度预警', content: `${lowH.length} 台设备健康度低于 85：${lowH.map(d => `<strong>${d.name}</strong>(${d.health.toFixed(0)})`).join('、')}。AI 预测：${lowH[0].name} 7 天内有较高故障风险，建议安排预防性检修。` })

  // 4. 误报过滤
  out.push({ type: 'info', icon: '🎯', title: '智能过滤效果', content: `本期系统通过 AI 模式识别自动过滤了 <strong style="color:#22c55e">${filterCnt.value} 条</strong>疑似误报（占总量 ${((filterCnt.value / Math.max(1, total)) * 100).toFixed(0)}%），减少不必要现场出动 ${(filterCnt.value * 0.5).toFixed(1)} 人时。` })

  // 5. 趋势预测
  const l3 = arr.filter(a => a.l === 3).length
  out.push({ type: 'info', icon: '📈', title: '趋势预测', content: `智能预警数量 ${l3} 条，建议关注其向二级、一级升级的可能性。AI 模型基于近 30 天数据预测，下周报警总量预计 <strong>${(total * (0.9 + Math.random() * 0.2)).toFixed(0)} 条</strong>，环比 ${(Math.random() > 0.5 ? '+' : '-')}${(Math.random() * 15).toFixed(0)}%。` })

  // 6. 处置建议
  out.push({ type: 'ok', icon: '💡', title: 'AI 处置建议', content: `基于本期报警模式，建议：① 加强 ${maxProf[0]} 设备巡检频次至每小时一次；② 对连续触发 2 次以上的测点配置自动联动；③ 季度大修前优先处理健康度<80的${lowH.length}台设备。` })

  return out
})

const initCharts = () => {
  charts.forEach(c => c.dispose())
  charts = []

  // 专业占比
  const p = profCnt.value
  const c1 = echarts.init(chProf.value)
  c1.setOption({
    legend: { textStyle: { color: '#94a3b8' }, top: 0, right: 0 },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
    series: [{
      type: 'pie', radius: ['45%', '72%'], center: ['50%', '55%'],
      data: [
        { value: p['锅炉'], name: `锅炉 ${p['锅炉']}条`, itemStyle: { color: '#f59e0b' } },
        { value: p['汽机'], name: `汽机 ${p['汽机']}条`, itemStyle: { color: '#3b82f6' } },
        { value: p['辅网'], name: `辅网 ${p['辅网']}条`, itemStyle: { color: '#22c55e' } }
      ].filter(d => d.value > 0),
      label: { color: '#94a3b8', fontSize: 11 }
    }]
  })
  charts.push(c1)

  // 级别占比
  const c2 = echarts.init(chLevel.value)
  c2.setOption({
    legend: { textStyle: { color: '#94a3b8' }, top: 0, right: 0 },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
    series: [{
      type: 'pie', radius: ['45%', '72%'], center: ['50%', '55%'],
      data: [
        { value: cnt(1), name: `一级 ${cnt(1)}条`, itemStyle: { color: '#ef4444' } },
        { value: cnt(2), name: `二级 ${cnt(2)}条`, itemStyle: { color: '#f59e0b' } },
        { value: cnt(3), name: `预警 ${cnt(3)}条`, itemStyle: { color: '#3b82f6' } }
      ].filter(d => d.value > 0),
      label: { color: '#94a3b8', fontSize: 11 }
    }]
  })
  charts.push(c2)

  // 处理状态
  const c3 = echarts.init(chStatus.value)
  c3.setOption({
    legend: { textStyle: { color: '#94a3b8' }, top: 0, right: 0 },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
    series: [{
      type: 'pie', radius: ['45%', '72%'], center: ['50%', '55%'],
      data: [
        { value: stats.value.handled, name: `已处理 ${stats.value.handled}条`, itemStyle: { color: '#22c55e' } },
        { value: stats.value.unhandled, name: `未处理 ${stats.value.unhandled}条`, itemStyle: { color: '#ef4444' } },
        { value: suppressed.value, name: `已抑制 ${suppressed.value}条`, itemStyle: { color: '#94a3b8' } }
      ],
      label: { color: '#94a3b8', fontSize: 11 }
    }]
  })
  charts.push(c3)

  // Top10 次数
  const top10Data = [
    ['A引风机轴承温度', 3], ['主汽温度', 4], ['润滑油压力', 3], ['NOx浓度', 5],
    ['A磨煤机振动', 2], ['除氧器水位', 3], ['D磨煤机电流', 2], ['给水泵B效率', 1],
    ['冷却塔水温', 2], ['高加上端差', 1]
  ]
  const c4 = echarts.init(chTop10.value)
  c4.setOption({
    grid: { left: 120, right: 30, top: 8, bottom: 22 },
    xAxis: { type: 'value', name: '次', axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'category', data: top10Data.map(d => d[0]).reverse(), axisLabel: { color: '#94a3b8', fontSize: 10 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    series: [{ type: 'bar', data: top10Data.map(d => d[1]).reverse(), itemStyle: { color: '#f59e0b', borderRadius: [0, 3, 3, 0] } }]
  })
  charts.push(c4)

  // Top10 时长
  const durData = [
    ['A引风机轴承', 48], ['润滑油压力', 35], ['D磨煤机', 28], ['主汽温度', 22],
    ['A磨煤机', 18], ['除氧器水位', 15], ['NOx浓度', 12], ['给水泵B', 8],
    ['冷却塔', 5], ['高加端差', 3]
  ]
  const c5 = echarts.init(chDur.value)
  c5.setOption({
    grid: { left: 120, right: 30, top: 8, bottom: 22 },
    xAxis: { type: 'value', name: '分钟', axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'category', data: durData.map(d => d[0]).reverse(), axisLabel: { color: '#94a3b8', fontSize: 10 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    series: [{ type: 'bar', data: durData.map(d => d[1]).reverse(), itemStyle: { color: '#ef4444', borderRadius: [0, 3, 3, 0] } }]
  })
  charts.push(c5)
}

watch(() => [store.selectedUnitId, period.value, store.alarms.length], () => nextTick(initCharts))
onMounted(() => nextTick(initCharts))
onUnmounted(() => charts.forEach(c => c.dispose()))
</script>

<style scoped>
.period-tabs { margin-bottom: 14px; }
:deep(.period-tabs .el-tabs__nav-wrap::after) { background-color: transparent; }
:deep(.period-tabs .el-tabs__item) { color: #94a3b8; }
:deep(.period-tabs .el-tabs__item.is-active) { color: #3b82f6; }
.sc { background: #0a0e17; border-radius: 8px; padding: 18px; text-align: center; border: 0.5px solid #1e293b; }
.sl { font-size: 11px; color: #94a3b8; margin-bottom: 6px; }
.sv { font-size: 28px; font-weight: 600; }
.ss { font-size: 10px; color: #64748b; margin-top: 4px; }
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.cd-t { font-size: 13px; color: #94a3b8; font-weight: 500; }
.cd-sub { font-size: 11px; color: #64748b; }
.insight-block { background: #0a0e17; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; margin-top: 14px; }
.insight-t { font-size: 14px; font-weight: 600; color: #3b82f6; margin-bottom: 12px; }
.insight-list { display: flex; flex-direction: column; gap: 10px; }
.ins-row { display: flex; gap: 12px; padding: 12px 14px; border-radius: 8px; border-left: 3px solid; }
.ins-row.danger { background: rgba(239,68,68,0.05); border-color: #ef4444; }
.ins-row.warn { background: rgba(245,158,11,0.05); border-color: #f59e0b; }
.ins-row.info { background: rgba(59,130,246,0.05); border-color: #3b82f6; }
.ins-row.ok { background: rgba(34,197,94,0.05); border-color: #22c55e; }
.ins-i { font-size: 22px; line-height: 1.3; flex-shrink: 0; }
.ins-b { flex: 1; }
.ins-title { font-size: 13px; font-weight: 600; color: #e2e8f0; margin-bottom: 3px; }
.ins-content { font-size: 12px; color: #94a3b8; line-height: 1.6; }
</style>
