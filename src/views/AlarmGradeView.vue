<template>
<div>
  <h2 class="pg-t">报警分级管理 · 设备故障映射</h2>
  <p class="pg-d">系统按严重程度将报警分为三级，每级对应不同的设备故障类型、响应时间和处置规则。</p>

  <div class="g-grid">
    <div v-for="(g, lv) in store.alarmLevels" :key="lv" class="g-card" :class="'g'+lv">
      <div class="g-h">
        <span class="g-lv" :style="{color: g.color}">{{ g.label }}</span>
        <span class="g-cnt" :style="{background: g.color+'22', color: g.color}">{{ countAlarms(parseInt(lv)) }} 条</span>
      </div>
      <div class="g-d">{{ g.desc }}</div>
      <div class="g-stat">
        <div class="g-si"><div class="g-sv" style="color:#22c55e">{{ countHandled(parseInt(lv)) }}</div><div>已处理</div></div>
        <div class="g-si"><div class="g-sv" style="color:#ef4444">{{ countUnhandled(parseInt(lv)) }}</div><div>未处理</div></div>
        <div class="g-si"><div class="g-sv">{{ g.avgTime }}分</div><div>平均响应</div></div>
      </div>

      <div class="g-section">
        <div class="g-st">📋 适用设备故障</div>
        <table class="g-tbl">
          <thead><tr><th>设备</th><th>测点</th><th>常见原因</th></tr></thead>
          <tbody>
            <tr v-for="(d, i) in g.deviceFaults" :key="i">
              <td>{{ d.device }}</td>
              <td><span class="badge" :style="{background:g.color+'22', color:g.color}">{{ d.point }}</span></td>
              <td>{{ d.reason }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="g-section">
        <div class="g-st">⚙️ 升级规则</div>
        <div v-for="(r, i) in g.rules" :key="i" class="g-r">· {{ r }}</div>
      </div>
    </div>
  </div>

  <div class="cd" style="margin-top:14px">
    <div class="cd-t">分级统计（{{ store.selectedUnit.name }}）</div>
    <el-row :gutter="12">
      <el-col :span="6"><div class="kpi"><div class="kpi-l">一级响应时长</div><div class="kpi-v" style="color:#ef4444">{{ avgTime(1) }}分</div><div class="kpi-s">应在 1 分钟内</div></div></el-col>
      <el-col :span="6"><div class="kpi"><div class="kpi-l">二级响应时长</div><div class="kpi-v" style="color:#f59e0b">{{ avgTime(2) }}分</div><div class="kpi-s">应在 2 小时内</div></div></el-col>
      <el-col :span="6"><div class="kpi"><div class="kpi-l">智能预警响应</div><div class="kpi-v" style="color:#06b6d4">{{ avgTime(3) }}分</div><div class="kpi-s">应在 24 小时内</div></div></el-col>
      <el-col :span="6"><div class="kpi"><div class="kpi-l">整体闭环率</div><div class="kpi-v" style="color:#22c55e">{{ closeRate }}</div><div class="kpi-s">已处理 / 总数</div></div></el-col>
    </el-row>
  </div>
</div>
</template>

<script setup>
import { computed } from 'vue'
import { useDataStore } from '@/stores/data'

const store = useDataStore()

const countAlarms = (lv) => store.unitAlarms(store.selectedUnitId).filter(a => a.l === lv).length
const countHandled = (lv) => store.unitAlarms(store.selectedUnitId).filter(a => a.l === lv && a.st === 'resolved').length
const countUnhandled = (lv) => store.unitAlarms(store.selectedUnitId).filter(a => a.l === lv && a.st === 'unhandled').length
const avgTime = (lv) => {
  // 模拟：取该等级已处理报警的平均间隔
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
</script>

<style scoped>
.pg-t { font-size: 16px; font-weight: 500; margin-bottom: 6px; }
.pg-d { color: #94a3b8; font-size: 12px; margin-bottom: 16px; }
.g-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.g-card { padding: 18px; border-radius: 10px; border: 0.5px solid #1e293b; background: #111827; }
.g1 { border-color: rgba(239,68,68,0.4); }
.g2 { border-color: rgba(245,158,11,0.4); }
.g3 { border-color: rgba(6,182,212,0.4); }
.g-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.g-lv { font-size: 16px; font-weight: 600; }
.g-cnt { font-size: 12px; padding: 2px 10px; border-radius: 4px; }
.g-d { font-size: 12px; color: #94a3b8; line-height: 1.6; margin-bottom: 12px; padding: 8px 10px; background: rgba(0,0,0,0.2); border-radius: 6px; }
.g-stat { display: flex; justify-content: space-around; padding: 8px 0; margin-bottom: 12px; }
.g-si { text-align: center; }
.g-sv { font-size: 22px; font-weight: 600; }
.g-si div:last-child { font-size: 11px; color: #94a3b8; }
.g-section { margin-bottom: 12px; }
.g-st { font-size: 12px; font-weight: 600; color: #94a3b8; margin-bottom: 6px; }
.g-tbl { width: 100%; border-collapse: collapse; font-size: 11px; }
.g-tbl th { text-align: left; padding: 4px 6px; color: #64748b; font-weight: 500; border-bottom: 0.5px solid #1e293b; }
.g-tbl td { padding: 5px 6px; border-bottom: 0.5px solid #1e293b; color: #cbd5e1; }
.badge { display: inline-block; padding: 1px 6px; border-radius: 3px; font-size: 10px; }
.g-r { font-size: 11px; color: #94a3b8; line-height: 1.8; }
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-t { font-size: 13px; color: #94a3b8; margin-bottom: 12px; font-weight: 500; }
.kpi { background: #0a0e17; padding: 14px; border-radius: 8px; text-align: center; }
.kpi-l { font-size: 11px; color: #94a3b8; margin-bottom: 6px; }
.kpi-v { font-size: 26px; font-weight: 600; }
.kpi-s { font-size: 10px; color: #64748b; margin-top: 4px; }
</style>
