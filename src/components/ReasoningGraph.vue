<template>
<div class="rg-container" ref="containerRef"
     @mousedown="onCanvasMouseDown" @mousemove="onCanvasMouseMove" @mouseup="onCanvasMouseUp" @mouseleave="onCanvasMouseUp"
     @wheel.prevent="onWheel">
  <svg :viewBox="`0 0 ${VW} ${VH}`" class="rg-svg"
       :style="{transform: `translate(${tx}px, ${ty}px) scale(${scale})`, transformOrigin: '0 0'}">
    <defs>
      <marker id="rg-arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L6,3 L0,6 Z" fill="#5a6e85" />
      </marker>
    </defs>

    <g class="links">
      <line v-for="(l, i) in layoutLinks" :key="'l'+i"
            :x1="l.x1" :y1="l.y1" :x2="l.x2" :y2="l.y2"
            :stroke="l.color" :stroke-width="l.width || 1.5"
            :stroke-dasharray="l.dash || ''"
            marker-end="url(#rg-arrow)" />
    </g>

    <g class="rel-labels">
      <g v-for="(l, i) in layoutRelLabels" :key="'rl'+i">
        <rect :x="l.x - l.w/2" :y="l.y - 9" :width="l.w" height="16" rx="3"
              fill="rgba(62,170,255,0.08)" stroke="#5a6e85" stroke-width="0.5" />
        <text :x="l.x" :y="l.y + 3" text-anchor="middle" font-size="9" fill="#7a9cc0">{{ l.text }}</text>
      </g>
    </g>

    <g class="nodes">
      <g v-for="n in layoutNodes" :key="n.id"
         :transform="`translate(${n.x},${n.y})`"
         class="rg-node" :class="{active: selectedId === n.id}"
         @mousedown.stop="onNodeMouseDown($event, n)"
         @click.stop="onNodeClick(n)">
        <rect :x="-n.w/2" :y="-n.h/2" :width="n.w" :height="n.h"
              :rx="n.type==='user' || n.type==='symptom' ? 6 : 4"
              :fill="n.bg" :stroke="n.border" :stroke-width="1.5" />
        <text x="0" y="-2" text-anchor="middle" :fill="n.color" font-size="11" font-weight="500">
          {{ n.icon ? n.icon + ' ' : '' }}{{ n.label }}
        </text>
        <text v-if="n.pct" x="0" y="13" text-anchor="middle" font-size="10" fill="#cbd5e1">{{ n.pct }}%</text>
      </g>
    </g>
  </svg>

  <div class="rg-tip" v-if="!scale">💡 拖动节点可调整位置 · 滚动鼠标可缩放 · 拖动空白处可平移</div>

  <div class="rg-controls">
    <el-button-group size="small">
      <el-button @click="zoomIn" title="放大"><el-icon><Plus /></el-icon></el-button>
      <el-button @click="zoomOut" title="缩小"><el-icon><Minus /></el-icon></el-button>
      <el-button @click="resetView" title="重置"><el-icon><Refresh /></el-icon></el-button>
    </el-button-group>
    <div class="rg-zoom-txt">{{ Math.round(scale * 100) }}%</div>
  </div>

  <div v-if="legend" class="rg-legend">
    <div class="rg-lg-i"><span class="rg-lg-d" style="background:#8b5cf6"></span>用户问题</div>
    <div class="rg-lg-i"><span class="rg-lg-d" style="background:#fbbf24;border:1px solid #f59e0b"></span>症状</div>
    <div class="rg-lg-i"><span class="rg-lg-d" style="background:#fde68a;border:1px solid #f59e0b"></span>中间现象</div>
    <div class="rg-lg-i"><span class="rg-lg-d" style="background:#fecaca;border:1.5px solid #ef4444"></span>原因</div>
    <div class="rg-lg-i"><span class="rg-lg-d" style="background:#bbf7d0;border:1px solid #22c55e"></span>解决方案</div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch } from 'vue'

const props = defineProps({
  caseData: { type: Object, required: true },
  selectedId: { type: String, default: null },
  legend: { type: Boolean, default: true }
})
const emit = defineEmits(['select', 'update:selectedId'])

const VW = 900, VH = 500

const COL = {
  user: { bg: '#ede9fe', border: '#8b5cf6', color: '#6d28d9' },
  symptom: { bg: '#fef3c7', border: '#f59e0b', color: '#b45309' },
  middle: { bg: '#fefce8', border: '#fbbf24', color: '#854d0e' },
  cause: { bg: '#fee2e2', border: '#ef4444', color: '#b91c1c' },
  solution: { bg: '#dcfce7', border: '#22c55e', color: '#15803d' }
}

// 用户可拖动节点位置
const overrides = ref({})
watch(() => props.caseData, () => { overrides.value = {} }, { immediate: false })

// 布局：5 层竖向排版（顶层→症状→中间→原因→方案）
const layoutNodes = computed(() => {
  const data = props.caseData
  if (!data?.nodes) return []
  const layers = { 0: [], 1: [], 2: [], 3: [], 4: [] }
  data.nodes.forEach(n => { layers[n.layer]?.push(n) })

  const out = []

  // layer 0/1 居中
  const yLayer = [28, 95, 175, 245, 320]  // 5 层的 y 起点
  layers[0].forEach(n => out.push(makeNode(n, VW / 2, yLayer[0], overrides.value[n.id])))
  layers[1].forEach(n => out.push(makeNode(n, VW / 2, yLayer[1], overrides.value[n.id])))

  // layer 2/3 共享 sub 列：3 列水平居中分布
  const subSet = new Set()
  ;(layers[2] || []).forEach(n => subSet.add(n.sub))
  ;(layers[3] || []).forEach(n => subSet.add(n.sub))
  const subs = [...subSet].sort((a, b) => a - b)
  const subCount = Math.max(subs.length, 1)
  const subWidth = Math.min(260, 200 + subCount * 30)
  const totalW = subWidth * subCount
  const startX = VW / 2 - totalW / 2 + subWidth / 2
  const getSubX = (sub) => startX + subWidth * (subs.indexOf(sub))

  layers[2].forEach(n => out.push(makeNode(n, getSubX(n.sub), yLayer[2], overrides.value[n.id])))
  layers[3].forEach(n => out.push(makeNode(n, getSubX(n.sub), yLayer[3], overrides.value[n.id])))

  // layer 4 父节点下挂 N 个，垂直堆叠
  const parentMap = {}
  data.rels?.forEach(r => { if (!parentMap[r.to]) parentMap[r.to] = r.from })
  const solByParent = {}
  layers[4].forEach(n => {
    const p = parentMap[n.id]
    if (!solByParent[p]) solByParent[p] = []
    solByParent[p].push(n)
  })
  const childSpacing = 42
  const childBaseY = yLayer[4]
  Object.entries(solByParent).forEach(([pid, sols]) => {
    const parent = out.find(o => o.id === pid)
    if (!parent) return
    sols.forEach((n, i) => {
      out.push(makeNode(n, parent.x, childBaseY + i * childSpacing, overrides.value[n.id]))
    })
  })

  return out
})

function makeNode(n, x, y, ov) {
  return {
    ...n,
    x: ov?.x ?? x,
    y: ov?.y ?? y,
    bg: COL[n.type]?.bg || 'rgba(62,170,255,0.08)',
    border: COL[n.type]?.border || '#5a6e85',
    color: COL[n.type]?.color || '#e2e8f0'
  }
}

const layoutLinks = computed(() => {
  const data = props.caseData
  if (!data?.rels) return []
  const map = Object.fromEntries(layoutNodes.value.map(n => [n.id, n]))
  return data.rels.map(r => {
    const a = map[r.from]
    const b = map[r.to]
    if (!a || !b) return null
    const color = a.layer === 0 ? '#8b5cf6' : a.type === 'cause' ? '#ef4444' : (a.type === 'cause' && b.type === 'solution') ? '#22c55e' : '#f59e0b'
    return {
      x1: a.x + a.w/2, y1: a.y,
      x2: b.x - b.w/2, y2: b.y,
      color, width: 1.2,
      dash: a.type === 'middle' ? '4,3' : ''
    }
  }).filter(Boolean)
})

const layoutRelLabels = computed(() => {
  const data = props.caseData
  if (!data?.rels) return []
  const map = Object.fromEntries(layoutNodes.value.map(n => [n.id, n]))
  return data.rels.map(r => {
    const a = map[r.from]
    const b = map[r.to]
    if (!a || !b) return null
    return {
      x: (a.x + a.w/2 + b.x - b.w/2) / 2,
      y: (a.y + b.y) / 2,
      w: r.type.length * 9 + 10,
      text: r.type
    }
  }).filter(Boolean)
})

// 缩放与平移
const scale = ref(1)
const tx = ref(0)
const ty = ref(0)
const containerRef = ref(null)

const zoomIn = () => scale.value = Math.min(3, +(scale.value + 0.1).toFixed(2))
const zoomOut = () => scale.value = Math.max(0.3, +(scale.value - 0.1).toFixed(2))
const resetView = () => { scale.value = 1; tx.value = 0; ty.value = 0 }

const onWheel = (e) => {
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  const newScale = Math.min(3, Math.max(0.3, +(scale.value + delta).toFixed(2)))
  // 围绕鼠标位置缩放
  if (containerRef.value) {
    const rect = containerRef.value.getBoundingClientRect()
    const mx = e.clientX - rect.left
    const my = e.clientY - rect.top
    const ratio = newScale / scale.value
    tx.value = mx - (mx - tx.value) * ratio
    ty.value = my - (my - ty.value) * ratio
  }
  scale.value = newScale
}

// 拖动节点
let draggingNode = null
let dragOffsetX = 0
let dragOffsetY = 0

const onNodeMouseDown = (e, n) => {
  // 在 SVG 坐标系里换算偏移
  const ctm = containerRef.value.querySelector('svg').getScreenCTM().inverse()
  const px = (e.clientX * ctm.a + e.clientY * ctm.c + ctm.e)
  const py = (e.clientX * ctm.b + e.clientY * ctm.d + ctm.f)
  dragOffsetX = px - n.x
  dragOffsetY = py - n.y
  draggingNode = n
}

const onNodeClick = (n) => {
  emit('update:selectedId', n.id)
  emit('select', n)
}

// 平移画布
let panning = false
let panStartX = 0
let panStartY = 0
let panStartTx = 0
let panStartTy = 0

const onCanvasMouseDown = (e) => {
  if (draggingNode) return
  panning = true
  panStartX = e.clientX
  panStartY = e.clientY
  panStartTx = tx.value
  panStartTy = ty.value
}
const onCanvasMouseMove = (e) => {
  if (draggingNode) {
    const ctm = containerRef.value.querySelector('svg').getScreenCTM().inverse()
    const px = (e.clientX * ctm.a + e.clientY * ctm.c + ctm.e)
    const py = (e.clientX * ctm.b + e.clientY * ctm.d + ctm.f)
    overrides.value[draggingNode.id] = { x: px - dragOffsetX, y: py - dragOffsetY }
    return
  }
  if (panning) {
    tx.value = panStartTx + (e.clientX - panStartX)
    ty.value = panStartTy + (e.clientY - panStartY)
  }
}
const onCanvasMouseUp = () => {
  draggingNode = null
  panning = false
}

onUnmounted(() => { draggingNode = null; panning = false })
</script>

<style scoped>
.rg-container { position: relative; width: 100%; height: 100%; overflow: hidden; background: #061224; border-radius: 6px; user-select: none; cursor: grab; }
.rg-container:active { cursor: grabbing; }
.rg-svg { width: 100%; height: 100%; display: block; }
.rg-node { cursor: grab; transition: filter 0.1s; }
.rg-node:active { cursor: grabbing; }
.rg-node:hover rect { filter: brightness(1.15); }
.rg-node.active rect { stroke-width: 2.5; filter: drop-shadow(0 0 6px currentColor); }
.rg-tip { position: absolute; top: 8px; left: 12px; font-size: 11px; color: #7a9cc0; background: rgba(15,23,42,0.7); padding: 4px 10px; border-radius: 4px; pointer-events: none; }
.rg-controls { position: absolute; bottom: 10px; right: 10px; display: flex; align-items: center; gap: 6px; background: rgba(15,23,42,0.85); padding: 4px 8px; border-radius: 6px; border: 0.5px solid rgba(62,170,255,0.08); }
.rg-zoom-txt { font-size: 11px; color: #7a9cc0; min-width: 36px; text-align: center; }
.rg-legend { position: absolute; bottom: 10px; left: 10px; display: flex; gap: 12px; background: rgba(15,23,42,0.85); padding: 6px 12px; border-radius: 6px; font-size: 11px; color: #7a9cc0; border: 0.5px solid rgba(62,170,255,0.08); }
.rg-lg-i { display: flex; align-items: center; gap: 4px; }
.rg-lg-d { width: 12px; height: 12px; border-radius: 3px; display: inline-block; }
</style>
