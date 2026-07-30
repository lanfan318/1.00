<template>
<div class="rg-container" ref="containerRef"
     @mousedown="onCanvasMouseDown" @mousemove="onCanvasMouseMove" @mouseup="onCanvasMouseUp" @mouseleave="onCanvasMouseUp"
     @wheel.prevent="onWheel">
  <svg :viewBox="`0 0 ${VW} ${VH}`" class="rg-svg"
       :style="{transform: `translate(${tx}px, ${ty}px) scale(${scale})`, transformOrigin: '0 0'}">
    <defs>
      <!-- 卡片磨砂深色底板（半透明，非实心高饱和） -->
      <linearGradient id="rg-cardbg" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%"  stop-color="rgba(22,38,62,0.95)" />
        <stop offset="100%" stop-color="rgba(10,20,38,0.93)" />
      </linearGradient>
      <!-- 统一霓虹发光滤镜（颜色取元素自身） -->
      <filter id="rg-glow" x="-60%" y="-60%" width="220%" height="220%">
        <feGaussianBlur stdDeviation="2.4" result="b1" />
        <feGaussianBlur stdDeviation="1" result="b2" />
        <feMerge>
          <feMergeNode in="b1" />
          <feMergeNode in="b2" />
          <feMergeNode in="SourceGraphic" />
        </feMerge>
      </filter>
      <!-- 精致锐利小箭头（按层级配色，置于连线末端） -->
      <marker id="rg-ar-user" markerWidth="7" markerHeight="6" refX="5.2" refY="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L6,3 L0,6 L1.6,3 Z" fill="#b794f6" />
      </marker>
      <marker id="rg-ar-sym" markerWidth="7" markerHeight="6" refX="5.2" refY="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L6,3 L0,6 L1.6,3 Z" fill="#e07a6b" />
      </marker>
      <marker id="rg-ar-mid" markerWidth="7" markerHeight="6" refX="5.2" refY="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L6,3 L0,6 L1.6,3 Z" fill="#e0a85c" />
      </marker>
      <marker id="rg-ar-sol" markerWidth="7" markerHeight="6" refX="5.2" refY="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L6,3 L0,6 L1.6,3 Z" fill="#5bb88a" />
      </marker>
    </defs>

    <!-- ═══ 连线：纤细发光电路信号线（直线） ═══ -->
    <g class="links">
      <line v-for="(l, i) in layoutLinks" :key="'l'+i"
            :x1="l.x1" :y1="l.y1" :x2="l.x2" :y2="l.y2"
            :stroke="l.color" :stroke-width="l.width"
            :stroke-dasharray="l.dash || ''"
            :marker-end="`url(#rg-ar-${l.ckey})`"
            filter="url(#rg-glow)" />
    </g>

    <!-- ═══ 关系标签：半透明深色，沿连线居中 ═══ -->
    <g class="rel-labels">
      <g v-for="(l, i) in layoutRelLabels" :key="'rl'+i">
        <rect :x="l.x - l.w/2" :y="l.y - 9" :width="l.w" height="16" rx="4"
              fill="rgba(8,16,32,0.85)" :stroke="l.color" stroke-width="0.5" stroke-opacity="0.6" />
        <text :x="l.x" :y="l.y + 3" text-anchor="middle" font-size="9" :fill="l.color" fill-opacity="0.95">{{ l.text }}</text>
      </g>
    </g>

    <!-- ═══ 节点：切角科技铭牌卡片（参数牌风格） ═══ -->
    <g class="nodes">
      <g v-for="n in layoutNodes" :key="n.id"
         :transform="`translate(${n.x},${n.y})`"
         class="rg-node" :class="{active: selectedId === n.id, hl: highlightIds.includes(n.id), dim: highlightIds.length && !highlightIds.includes(n.id)}"
         @mousedown.stop="onNodeMouseDown($event, n)"
         @click.stop="onNodeClick(n)">
        <!-- 底板（半透明磨砂深色，切角） -->
        <path :d="edgePath(n)" fill="url(#rg-cardbg)" />
        <!-- 底部刻度带（参数牌装饰：极细刻度线） -->
        <line class="rg-ticks" :x1="-n.w/2+10" :y1="n.h/2-4" :x2="n.w/2-10" :y2="n.h/2-4"
              :stroke="n.neon" stroke-width="3" stroke-opacity="0.3" stroke-dasharray="1 3" />
        <!-- 内层细线（微弱内发光质感） -->
        <path class="rg-inner" :d="innerPath(n)" fill="none" :stroke="n.neon" stroke-width="0.5" stroke-opacity="0.55" />
        <!-- 外层霓虹边框（带发光） -->
        <path class="rg-edge" :d="edgePath(n)" fill="none" :stroke="n.neon" stroke-width="1.2" filter="url(#rg-glow)" />
        <!-- 参数牌角标（类型代码小标签） -->
        <g class="rg-tag">
          <rect :x="-n.w/2+6" :y="-n.h/2+3" width="30" height="11" rx="2"
                fill="rgba(8,16,32,0.7)" :stroke="n.neon" stroke-width="0.4" stroke-opacity="0.5" />
          <text :x="-n.w/2+9" :y="-n.h/2+11" font-size="7" :fill="n.neon" fill-opacity="0.85" class="rg-tag-t">{{ n.code }}</text>
        </g>
        <!-- 主标签：等宽科技字体 -->
        <text x="0" :y="n.pct ? -2 : 4" text-anchor="middle" font-size="11" font-weight="500" fill="#d4e8ff" class="rg-lbl">{{ n.label }}</text>
        <!-- 概率数值：放大 + 同色发光 -->
        <text v-if="n.pct" x="0" :y="n.h/2-14" text-anchor="middle" font-size="14" font-weight="700" :fill="n.neon" filter="url(#rg-glow)" class="rg-pct">{{ n.pct }}%</text>
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
    <div class="rg-lg-i"><span class="rg-lg-d" style="background:#b794f6"></span>根节点</div>
    <div class="rg-lg-i"><span class="rg-lg-d" style="background:#e0a85c"></span>原因/中间</div>
    <div class="rg-lg-i"><span class="rg-lg-d" style="background:#e07a6b"></span>故障指标</div>
    <div class="rg-lg-i"><span class="rg-lg-d" style="background:#5bb88a"></span>解决方案</div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch } from 'vue'

const props = defineProps({
  caseData: { type: Object, required: true },
  selectedId: { type: String, default: null },
  legend: { type: Boolean, default: true },
  highlightIds: { type: Array, default: () => [] }
})
const emit = defineEmits(['select', 'update:selectedId'])

const VW = 900, VH = 500

// 层级霓虹配色（低饱和暗调）
const NEON = {
  user:     { line: '#b794f6', key: 'user' },  // 顶层根节点：淡紫
  symptom:  { line: '#e07a6b', key: 'sym' },   // 故障指标：暗红
  middle:   { line: '#e0a85c', key: 'mid' },   // 中间设备：橙黄
  cause:    { line: '#e0a85c', key: 'mid' },   // 原因：橙黄
  solution: { line: '#5bb88a', key: 'sol' }    // 解决方案：青绿
}

// 层级尺寸（根最大 / 原因中等 / 指标最小）；长文本节点保留原宽防溢出
const SIZE = {
  user:     { minW: 240, h: 54 },
  symptom:  { minW: 120, h: 38 },
  middle:   { minW: 150, h: 44 },
  cause:    { minW: 150, h: 48 },
  solution: { minW: 110, h: 34 }
}
const TYPE_CODE = { user: 'ROOT', symptom: 'SYM', middle: 'MID', cause: 'CAU', solution: 'SOL' }

// 切角路径（锐角科技卡片，非圆角）
function chamfer(w, h, r) {
  const x = -w / 2, y = -h / 2
  return `M${x + r},${y} L${x + w - r},${y} L${x + w},${y + r} L${x + w},${y + h - r} L${x + w - r},${y + h} L${x + r},${y + h} L${x},${y + h - r} L${x},${y + r} Z`
}
const edgePath = (n) => chamfer(n.w, n.h, 4)
const innerPath = (n) => chamfer(n.w - 5, n.h - 5, 2)

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
  const subWidth = Math.min(280, 210 + subCount * 30)
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
  const childSpacing = 46
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
  const c = NEON[n.type] || { line: '#5a8ab8', key: 'mid' }
  const s = SIZE[n.type] || { minW: 140, h: 40 }
  return {
    ...n,
    x: ov?.x ?? x,
    y: ov?.y ?? y,
    w: Math.max(s.minW, n.w || s.minW),
    h: s.h,
    neon: c.line,
    ckey: c.key,
    code: TYPE_CODE[n.type] || 'ND'
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
    const c = NEON[a.type] || { line: '#5a8ab8', key: 'mid' }
    return {
      x1: a.x + a.w / 2, y1: a.y,
      x2: b.x - b.w / 2, y2: b.y,
      color: c.line,
      ckey: c.key,
      width: 1.2,
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
    const c = NEON[a.type] || { line: '#5a8ab8', key: 'mid' }
    return {
      x: (a.x + a.w / 2 + b.x - b.w / 2) / 2,
      y: (a.y + b.y) / 2,
      w: r.type.length * 9 + 10,
      text: r.type,
      color: c.line
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
/* 纯深空黑 + 均匀等距青蓝虚线方格示波器栅格 + 中心微弱径向蓝光 */
.rg-container {
  position: relative; width: 100%; height: 100%; overflow: hidden;
  border-radius: 6px; user-select: none; cursor: grab;
  background-color: #000000;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.06) 0px, rgba(90,166,196,0.06) 1px, transparent 1px, transparent 30px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.06) 0px, rgba(90,166,196,0.06) 1px, transparent 1px, transparent 30px),
    radial-gradient(ellipse at 50% 45%, rgba(40,90,130,0.12), transparent 62%);
}
.rg-container:active { cursor: grabbing; }
.rg-svg { width: 100%; height: 100%; display: block; }

/* 节点 */
.rg-node { cursor: grab; }
.rg-node:active { cursor: grabbing; }
.rg-node:hover .rg-edge { stroke-width: 1.7; }
.rg-node.active .rg-edge { stroke-width: 2; }
.rg-node.hl .rg-edge { stroke-width: 2; }
.rg-node.dim { opacity: 0.3; }
.rg-lbl, .rg-pct, .rg-tag-t { font-family: "SF Mono","Consolas","Roboto Mono",monospace; }

/* 关系标签文字 */
.rel-labels text { font-family: "SF Mono","Consolas",monospace; }

.rg-tip { position: absolute; top: 8px; left: 12px; font-size: 11px; color: #7a9cc0; background: rgba(8,16,32,0.7); padding: 4px 10px; border-radius: 4px; pointer-events: none; }
.rg-controls { position: absolute; bottom: 10px; right: 10px; display: flex; align-items: center; gap: 6px; background: rgba(8,16,32,0.85); padding: 4px 8px; border-radius: 6px; border: 0.5px solid rgba(62,170,255,0.08); }
.rg-zoom-txt { font-size: 11px; color: #7a9cc0; min-width: 36px; text-align: center; }
.rg-legend { position: absolute; bottom: 10px; left: 10px; display: flex; gap: 12px; background: rgba(8,16,32,0.85); padding: 6px 12px; border-radius: 6px; font-size: 11px; color: #9ab8d4; border: 0.5px solid rgba(62,170,255,0.08); }
.rg-lg-i { display: flex; align-items: center; gap: 4px; }
.rg-lg-d { width: 12px; height: 12px; border-radius: 2px; display: inline-block; box-shadow: 0 0 5px currentColor; }
</style>
