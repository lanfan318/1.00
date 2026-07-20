<template>
<div>
  <div class="pg-h">
    <h2>设备工况分析</h2>
    <el-select v-model="selectedId" style="width:260px" filterable>
      <el-option v-for="d in devs" :key="d.id" :value="d.id" :label="d.name + '（' + d.dept + '）' + ' 健康度' + d.health.toFixed(1)" />
    </el-select>
  </div>

  <el-row :gutter="14">
    <el-col :span="6"><div class="mc"><div class="ml">健康度</div><div class="mv" :style="{color:cur.health>=90?'#22c55e':cur.health>=80?'#f59e0b':'#ef4444'}">{{ cur.health.toFixed(1) }}</div><div class="ms">/100</div></div></el-col>
    <el-col :span="6"><div class="mc"><div class="ml">出力/负荷</div><div class="mv" style="color:#3b82f6">{{ cur.output || '-' }}</div><div class="ms">MW / A</div></div></el-col>
    <el-col :span="6"><div class="mc"><div class="ml">运行状态</div><div class="mv" :style="{color:cur.health>=80?'#22c55e':'#f59e0b'}" style="font-size:18px">{{ cur.health>=80?'正常':'告警' }}</div><div class="ms">实时</div></div></el-col>
    <el-col :span="6"><div class="mc"><div class="ml">所属机组</div><div class="mv" style="color:#06b6d4;font-size:18px">{{ unitName }}</div><div class="ms">{{ cur.dept }}</div></div></el-col>
  </el-row>

  <el-row :gutter="14" style="margin-top:14px">
    <el-col :span="12"><div class="cd"><div class="cd-t">关键参数趋势</div><div ref="ct" style="height:240px"></div></div></el-col>
    <el-col :span="12"><div class="cd"><div class="cd-t">效率仪表盘</div><div ref="cg" style="height:240px"></div></div></el-col>
  </el-row>

  <div class="cd" style="margin-top:14px"><div class="cd-t">测点实时值</div>
    <table class="tbl">
      <thead><tr><th>测点</th><th>当前值</th><th>单位</th><th>阈值</th><th>状态</th></tr></thead>
      <tbody>
        <tr v-for="(v, k) in cur.params" :key="k">
          <td>{{ k }}</td>
          <td :style="{color: v[0] >= v[1] ? '#ef4444' : '#e2e8f0', fontWeight: 500}">{{ v[0] }}</td>
          <td>{{ v[2] }}</td>
          <td>{{ v[1] }}</td>
          <td><el-tag :type="v[0] < v[1] ? 'success' : 'danger'" size="small">{{ v[0] < v[1] ? '正常' : '超限' }}</el-tag></td>
        </tr>
      </tbody>
    </table>
  </div>

  <div class="cd" style="margin-top:14px"><div class="cd-t">AI 工况分析</div>
    <div class="ai">
      <p><span class="tg tg-i">健康度</span> 当前设备健康度为 <strong :style="{color:cur.health>=90?'#22c55e':cur.health>=80?'#f59e0b':'#ef4444'}">{{ cur.health.toFixed(1) }}</strong>，{{ cur.health >= 90 ? '运行状态良好，无需特别处理' : cur.health >= 80 ? '需关注，建议加强巡检' : '需立即安排检修' }}。</p>
      <p v-for="ins in aiInsights" :key="ins.key"><span class="tg" :class="ins.cls">{{ ins.key }}</span> {{ ins.text }}</p>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const ct = ref(null), cg = ref(null)
let tCh, gCh

const devs = computed(() => store.unitDevices(store.selectedUnitId))
const selectedId = ref(store.devices[0]?.id || '')
watch(() => store.devices, () => {
  if (!selectedId.value && store.devices[0]) selectedId.value = store.devices[0].id
}, { deep: true })

const cur = computed(() => store.devices.find(d => d.id === selectedId.value) || store.devices[0] || { health: 0, params: {}, name: '无设备', dept: '-' })
const unitName = computed(() => store.units.find(u => u.id === cur.value?.unit)?.name || '-')

const aiInsights = computed(() => {
  const d = cur.value
  if (!d) return []
  const out = []
  // 检查每个测点
  for (const [k, v] of Object.entries(d.params || {})) {
    if (v[0] >= v[1]) {
      out.push({ key: k, cls: 'tg-w', text: `当前 ${v[0]}${v[2]} 已超阈值 ${v[1]}${v[2]}，建议立即处置。` })
    } else if (v[0] > v[1] * 0.9) {
      out.push({ key: k, cls: 'tg-i', text: `当前 ${v[0]}${v[2]} 接近阈值 (90% 警戒线)，持续关注。` })
    }
  }
  if (out.length === 0) out.push({ key: '综合', cls: 'tg-ok', text: '各测点运行平稳，状态良好。' })
  return out
})

const initCharts = () => {
  if (tCh) tCh.dispose(); if (gCh) gCh.dispose()
  tCh = echarts.init(ct.value)
  tCh.setOption({
    grid: { left: 40, right: 14, top: 14, bottom: 24 },
    legend: { textStyle: { color: '#94a3b8' }, top: 0, right: 0 },
    xAxis: { type: 'category', data: Array.from({ length: 20 }, (_, i) => i * 3 + 's'), axisLabel: { color: '#64748b', fontSize: 9 }, axisLine: { lineStyle: { color: '#1e293b' } } },
    yAxis: { type: 'value', axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [
      { name: '温度', type: 'line', smooth: true, data: Array.from({ length: 20 }, () => cur.value?.params?.温度?.[0] || 50 + Math.random() * 10), lineStyle: { color: '#f59e0b' }, areaStyle: { color: 'rgba(245,158,11,0.08)' }, symbol: 'none' },
      { name: '振动', type: 'line', smooth: true, data: Array.from({ length: 20 }, () => cur.value?.params?.振动?.[0] || 2 + Math.random() * 3), lineStyle: { color: '#3b82f6' }, symbol: 'none' }
    ]
  })
  const eff = cur.value?.health >= 80 ? 80 + (cur.value.health - 80) * 0.5 : cur.value?.health * 0.8
  const effRounded = parseFloat(eff.toFixed(1))
  gCh = echarts.init(cg.value)
  gCh.setOption({
    series: [{
      type: 'gauge', min: 0, max: 100,
      progress: { show: true, width: 12 },
      axisLine: { lineStyle: { width: 12, color: [[0.6, '#ef4444'], [0.85, '#f59e0b'], [1, '#22c55e']] } },
      pointer: { show: false }, axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false },
      title: { color: '#94a3b8', fontSize: 12, offsetCenter: [0, '70%'] },
      detail: { valueAnimation: true, fontSize: 28, color: '#22c55e', offsetCenter: [0, '0%'], formatter: '{value}%' },
      data: [{ value: effRounded, name: '运行效率' }]
    }]
  })
}

watch(selectedId, () => nextTick(initCharts))
watch(() => cur.value?.id, () => nextTick(initCharts))
onMounted(() => nextTick(initCharts))
onUnmounted(() => { tCh?.dispose(); gCh?.dispose() })
</script>

<style scoped>
.pg-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
h2 { font-size: 16px; font-weight: 500; }
.mc { background: #0a0e17; border-radius: 8px; padding: 18px; text-align: center; border: 0.5px solid #1e293b; }
.ml { font-size: 12px; color: #94a3b8; margin-bottom: 4px; }
.mv { font-size: 28px; font-weight: 600; }
.ms { font-size: 11px; color: #64748b; margin-top: 4px; }
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 8px; padding: 16px; }
.cd-t { font-size: 13px; color: #94a3b8; margin-bottom: 10px; font-weight: 500; }
.tbl { width: 100%; border-collapse: collapse; font-size: 12px; }
.tbl th { text-align: left; padding: 8px 10px; color: #64748b; font-weight: 500; font-size: 11px; border-bottom: 0.5px solid #1e293b; }
.tbl td { padding: 10px; border-bottom: 0.5px solid #1e293b; color: #94a3b8; }
.ai p { line-height: 1.8; font-size: 13px; color: #cbd5e1; margin-bottom: 6px; }
.tg { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; margin-right: 6px; }
.tg-i { background: rgba(59, 130, 246, 0.12); color: #3b82f6; }
.tg-w { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
.tg-ok { background: rgba(34, 197, 94, 0.12); color: #22c55e; }
</style>
