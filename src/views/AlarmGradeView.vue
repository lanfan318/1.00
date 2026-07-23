<template>
<div class="ag-page">
  <div class="ag-head">
    <div class="ag-head-l">
      <span class="ag-bar"></span>
      <h2>报警分级管理 · 设备故障映射</h2>
    </div>
    <p class="ag-desc">系统按严重程度将报警分为三级，每级对应不同的设备故障类型、响应时间和处置规则。</p>
  </div>

  <div class="g-grid">
    <div v-for="(g, lv) in store.alarmLevels" :key="lv" class="g-card" :class="'g'+lv">
      <div class="g-h">
        <div class="g-hl">
          <span class="g-lv-dot" :style="{background: g.color, boxShadow:'0 0 10px '+g.color}"></span>
          <span class="g-lv" :style="{color: g.color}">{{ g.label }}</span>
        </div>
        <span class="g-cnt" :style="{background: g.color+'22', color: g.color, borderColor: g.color+'55'}">{{ countAlarms(parseInt(lv)) }} 条</span>
      </div>
      <div class="g-d">{{ g.desc }}</div>

      <div class="g-stat">
        <div class="g-si"><div class="g-sv" style="color:#34d399">{{ countHandled(parseInt(lv)) }}</div><div class="g-si-l">已处理</div></div>
        <div class="g-si"><div class="g-sv" style="color:#ef4444">{{ countUnhandled(parseInt(lv)) }}</div><div class="g-si-l">未处理</div></div>
        <div class="g-si"><div class="g-sv">{{ g.avgTime }}<span class="g-si-u">分</span></div><div class="g-si-l">平均响应</div></div>
      </div>

      <div class="g-section">
        <div class="g-st"><span class="g-st-ic">▤</span>适用设备故障</div>
        <table class="g-tbl">
          <thead><tr><th>设备</th><th>测点</th><th>常见原因</th></tr></thead>
          <tbody>
            <tr v-for="(d, i) in g.deviceFaults" :key="i">
              <td>{{ d.device }}</td>
              <td><span class="badge" :style="{background:g.color+'22', color:g.color, borderColor:g.color+'44'}">{{ d.point }}</span></td>
              <td>{{ d.reason }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="g-section">
        <div class="g-st"><span class="g-st-ic">⚙</span>升级规则</div>
        <div v-for="(r, i) in g.rules" :key="i" class="g-r"><span class="g-r-dot">·</span>{{ r }}</div>
      </div>
    </div>
  </div>

  <!-- 分级统计 -->
  <div class="cd ag-stat-card">
    <div class="cd-t"><span class="ut-ic">▸</span>分级统计（{{ store.selectedUnit.name }}）</div>
    <div class="ag-stat-body">
      <div class="ag-kpis">
        <div class="kpi" :style="{'--ac':'#ef4444'}">
          <div class="kpi-l">一级响应时长</div>
          <div class="kpi-v" style="color:#ef4444">{{ avgTime(1) }}分</div>
          <div class="kpi-s">应在 1 分钟内</div>
        </div>
        <div class="kpi" :style="{'--ac':'#fbbf24'}">
          <div class="kpi-l">二级响应时长</div>
          <div class="kpi-v" style="color:#fbbf24">{{ avgTime(2) }}分</div>
          <div class="kpi-s">应在 2 小时内</div>
        </div>
        <div class="kpi" :style="{'--ac':'#22d3ee'}">
          <div class="kpi-l">智能预警响应</div>
          <div class="kpi-v" style="color:#22d3ee">{{ avgTime(3) }}分</div>
          <div class="kpi-s">应在 24 小时内</div>
        </div>
        <div class="kpi" :style="{'--ac':'#34d399'}">
          <div class="kpi-l">整体闭环率</div>
          <div class="kpi-v" style="color:#34d399">{{ closeRate }}</div>
          <div class="kpi-s">已处理 / 总数</div>
        </div>
      </div>
      <div class="ag-donut">
        <div ref="donutRef" class="ag-donut-c"></div>
        <div class="ag-donut-cap">报警等级分布</div>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()

const countAlarms = (lv) => store.unitAlarms(store.selectedUnitId).filter(a => a.l === lv).length
const countHandled = (lv) => store.unitAlarms(store.selectedUnitId).filter(a => a.l === lv && a.st === 'resolved').length
const countUnhandled = (lv) => store.unitAlarms(store.selectedUnitId).filter(a => a.l === lv && a.st === 'unhandled').length
const avgTime = (lv) => {
  const arr = store.unitAlarms(store.selectedUnitId).filter(a => a.l === lv && a.st === 'resolved')
  if (arr.length === 0) return store.alarmLevels[lv].avgTime
  return (Math.random() * 2 + store.alarmLevels[lv].avgTime * 0.5).toFixed(1)
}
const closeRate = computed(() => {
  const arr = store.unitAlarms(store.selectedUnitId)
  if (arr.length === 0) return '0%'
  const h = arr.filter(a => a.st === 'resolved').length
  return ((h / arr.length) * 100).toFixed(0) + '%'
})

const donutRef = ref(null)
let donutChart
const renderDonut = () => {
  if (!donutRef.value) return
  if (donutChart) donutChart.dispose()
  donutChart = echarts.init(donutRef.value)
  const data = [
    { value: countAlarms(1), name: '一级报警', color: '#ef4444' },
    { value: countAlarms(2), name: '二级报警', color: '#fbbf24' },
    { value: countAlarms(3), name: '智能预警', color: '#22d3ee' }
  ]
  donutChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(8,20,40,0.92)', borderColor: 'rgba(62,170,255,0.3)', textStyle: { color: '#c8e4ff' }, formatter: '{b}: {c} 条 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#8fb0cf', fontSize: 11 }, icon: 'circle', itemWidth: 8, itemHeight: 8 },
    series: [{
      type: 'pie', radius: ['45%', '72%'], center: ['50%', '44%'],
      avoidLabelOverlap: true, padAngle: 2,
      itemStyle: { borderRadius: 6, borderColor: '#061224', borderWidth: 2,
        shadowBlur: 16, shadowColor: 'rgba(0,0,0,0.4)' },
      label: { show: true, color: '#c8e4ff', fontSize: 11, formatter: '{b}\n{c}' },
      labelLine: { length: 10, length2: 8, lineStyle: { color: 'rgba(62,170,255,0.3)' } },
      data: data.map(d => ({ value: d.value, name: d.name, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: d.color }, { offset: 1, color: d.color + '88' }]) } }))
    }]
  })
}
onMounted(() => nextTick(renderDonut))
watch(() => store.selectedUnitId, () => nextTick(renderDonut))
onUnmounted(() => donutChart?.dispose())
</script>

<style scoped>
.ag-page { display: flex; flex-direction: column; gap: 14px; min-height: 100%; }
.ag-head { display: flex; align-items: baseline; gap: 16px; flex-wrap: wrap; }
.ag-head-l { display: flex; align-items: center; gap: 12px; }
.ag-bar { width: 4px; height: 22px; background: linear-gradient(180deg, #3eaaff, #22d3ee); border-radius: 2px; box-shadow: 0 0 10px rgba(62,170,255,0.5); }
.ag-head h2 { font-size: 17px; font-weight: 600; color: #d4ecff; letter-spacing: 0.5px; }
.ag-desc { color: #8fb0cf; font-size: 12px; }

.g-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; align-items: start; }
.g-card {
  position: relative; padding: 18px; border-radius: 6px; overflow: hidden;
  min-height: 360px;
  background: linear-gradient(135deg, rgba(12,30,52,0.65) 0%, rgba(8,20,38,0.7) 100%);
  border: 1px solid rgba(62,170,255,0.12);
  box-shadow: 0 1px 3px rgba(0,0,0,0.3), 0 4px 16px rgba(0,10,30,0.4), inset 0 1px 0 rgba(62,170,255,0.06);
}
.g-card::after { content:''; position:absolute; top:-1px; right:-1px; width:40px; height:40px; background: radial-gradient(circle at top right, var(--accent, rgba(62,170,255,0.12)), transparent 70%); }
.g1 { border-color: rgba(239,68,68,0.4); --accent: rgba(239,68,68,0.18); }
.g2 { border-color: rgba(245,158,11,0.4); --accent: rgba(245,158,11,0.18); }
.g3 { border-color: rgba(6,182,212,0.4); --accent: rgba(6,182,212,0.18); }
.g-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.g-hl { display: flex; align-items: center; gap: 8px; }
.g-lv-dot { width: 9px; height: 9px; border-radius: 50%; }
.g-lv { font-size: 16px; font-weight: 600; }
.g-cnt { font-size: 12px; padding: 2px 10px; border-radius: 4px; border: 1px solid; font-weight: 600; }
.g-d { font-size: 12px; color: #9fb6cf; line-height: 1.7; margin-bottom: 12px; padding: 10px; background: rgba(6,18,36,0.5); border-radius: 6px; border-left: 2px solid rgba(62,170,255,0.25); }
.g-stat { display: flex; justify-content: space-around; padding: 10px 0; margin-bottom: 12px; background: rgba(6,18,36,0.4); border-radius: 6px; }
.g-si { text-align: center; }
.g-sv { font-size: 22px; font-weight: 700; font-family: "SF Mono","Consolas",monospace; }
.g-si-u { font-size: 11px; }
.g-si-l { font-size: 11px; color: #8fb0cf; margin-top: 2px; }
.g-section { margin-bottom: 12px; }
.g-st { font-size: 12px; font-weight: 600; color: #c8e4ff; margin-bottom: 8px; display: flex; align-items: center; gap: 6px; }
.g-st-ic { color: #3eaaff; font-size: 13px; }
.g-tbl { width: 100%; border-collapse: collapse; font-size: 11px; }
.g-tbl th { text-align: left; padding: 5px 6px; color: #8fb0cf; font-weight: 600; border-bottom: 1px solid rgba(62,170,255,0.12); }
.g-tbl td { padding: 5px 6px; border-bottom: 1px solid rgba(62,170,255,0.06); color: #cbd5e1; }
.badge { display: inline-block; padding: 1px 6px; border-radius: 3px; font-size: 11px; border: 1px solid; }
.g-r { font-size: 11px; color: #9fb6cf; line-height: 1.9; }
.g-r-dot { color: #3eaaff; margin-right: 4px; font-weight: 700; }

/* 统计 */
.ag-stat-card { padding-bottom: 8px; }
.ut-ic { color: #3eaaff; font-weight: 700; margin-right: 2px; }
.ag-stat-body { display: grid; grid-template-columns: 1fr 280px; gap: 14px; align-items: center; }
.ag-kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.ag-kpis .kpi { padding: 16px 10px; }
.ag-kpis .kpi::after { content:''; position:absolute; left:0; top:0; bottom:0; width:3px; background: var(--ac); box-shadow: 0 0 10px var(--ac); }
.ag-donut { display: flex; flex-direction: column; align-items: center; }
.ag-donut-c { width: 100%; height: 200px; }
.ag-donut-cap { font-size: 12px; color: #8fb0cf; margin-top: -6px; }
</style>
