<template>
  <div class="rg-wrap" :class="[stateClass, size]">
    <!-- 主圆弧背景 -->
    <svg viewBox="0 0 100 100" class="rg-svg">
      <defs>
        <linearGradient :id="gradId + 'bg'" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="rgba(62,170,255,0.03)" />
          <stop offset="100%" stop-color="rgba(62,170,255,0.01)" />
        </linearGradient>
        <linearGradient :id="gradId + 'fg'" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" :stop-color="colorA" :stop-opacity="0.95" />
          <stop offset="100%" :stop-color="colorB" :stop-opacity="0.7" />
        </linearGradient>
        <radialGradient :id="gradId + 'center'" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="rgba(62,170,255,0.12)" />
          <stop offset="80%" stop-color="rgba(8,20,40,0.4)" />
          <stop offset="100%" stop-color="rgba(8,20,40,0)" />
        </radialGradient>
      </defs>
      <!-- 背景圆 -->
      <circle cx="50" cy="50" r="45" :fill="`url(#${gradId}bg)`" />
      <!-- 多层刻度装饰圆 -->
      <circle cx="50" cy="50" r="44" fill="none" stroke="rgba(62,170,255,0.08)" stroke-width="0.3" />
      <circle cx="50" cy="50" r="40" fill="none" stroke="rgba(62,170,255,0.18)" stroke-width="0.5" stroke-dasharray="1 2" />
      <circle cx="50" cy="50" r="35" fill="none" stroke="rgba(62,170,255,0.15)" stroke-width="0.5" />
      <!-- 刻度线 -->
      <g class="rg-ticks">
        <line v-for="i in 24" :key="i"
              :x1="50 + 40 * Math.cos((i - 6) * 15 * Math.PI / 180)"
              :y1="50 + 40 * Math.sin((i - 6) * 15 * Math.PI / 180)"
              :x2="50 + (i % 5 === 0 ? 43 : 41) * Math.cos((i - 6) * 15 * Math.PI / 180)"
              :y2="50 + (i % 5 === 0 ? 43 : 41) * Math.sin((i - 6) * 15 * Math.PI / 180)"
              :stroke="(i % 5 === 0) ? colorA : 'rgba(120,160,200,0.3)'"
              :stroke-width="(i % 5 === 0) ? 1 : 0.5"
              stroke-linecap="round" />
      </g>
      <!-- 弧形进度 (3层叠加产生 3D 感) -->
      <circle cx="50" cy="50" r="38" fill="none"
              :stroke="`url(#${gradId}fg)`" stroke-width="3"
              stroke-linecap="round"
              :stroke-dasharray="dashLen"
              :transform="'rotate(-90 50 50)'"
              :style="{filter: `drop-shadow(0 0 3px ${colorA})`}" />
      <!-- 内层装饰弧 (淡色) -->
      <circle cx="50" cy="50" r="35" fill="none"
              :stroke="colorA" stroke-width="1" opacity="0.3"
              :stroke-dasharray="dashLen"
              :transform="'rotate(-90 50 50)'" />
      <!-- 中心径向辉光 -->
      <circle cx="50" cy="50" r="32" :fill="`url(#${gradId}center)`" />
      <!-- 刻度文字 (0/50/100) -->
      <text v-if="showLabels" x="50" y="13" text-anchor="middle" :fill="colorA" font-size="5" font-weight="700">{{ Math.round(value*0.25) }}</text>
      <text v-if="showLabels" x="50" y="93" text-anchor="middle" fill="rgba(180,210,240,0.6)" font-size="5" font-weight="600">{{ Math.round(value*0.75) }}</text>
    </svg>
    <!-- 中心数值（独立 SVG 让字体清晰） -->
    <div class="rg-center">
      <div class="rg-val mono" :style="{color: colorA}">{{ value }}{{ unit }}</div>
      <div class="rg-lbl">{{ label }}</div>
    </div>
    <!-- 状态指示灯 -->
    <div v-if="status" class="rg-status" :class="stateClass">
      <i class="rg-status-d"></i><span>{{ status }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 100 },
  unit: { type: String, default: '' },
  label: { type: String, default: '' },
  status: { type: String, default: '' },
  color: { type: String, default: '' }, // auto-pick if empty
  showLabels: { type: Boolean, default: true },
  size: { type: String, default: 'md' } // sm/md/lg
})

const dashLen = computed(() => {
  const c = 2 * Math.PI * 38  // 240
  const v = Math.max(0, Math.min(props.value, props.max)) / props.max
  return [c * v, c]
})

const colorA = computed(() => {
  if (props.color) return props.color
  // 自动按状态颜色
  const v = props.value / props.max
  if (props.label && /温度|压力|temp|press/i.test(props.label)) {
    return v > 0.85 ? '#f87171' : v > 0.6 ? '#fbbf24' : '#5fb3ff'
  }
  if (props.label && /振动|震动|vib/i.test(props.label)) {
    return v > 0.5 ? '#f87171' : v > 0.3 ? '#fbbf24' : '#34d399'
  }
  return v > 0.85 ? '#f87171' : v > 0.6 ? '#fbbf24' : '#5fb3ff'
})

const colorB = computed(() => {
  if (props.color) return props.color
  return colorA.value
})

const stateClass = computed(() => {
  const v = props.value / props.max
  if (v > 0.85) return 'danger'
  if (v > 0.6) return 'warn'
  return 'ok'
})

const gradId = computed(() => 'rg' + Math.floor(Math.random() * 100000))
</script>

<style scoped>
.rg-wrap {
  position: relative;
  display: inline-block;
  width: 110px; height: 110px;
}
.rg-wrap.sm { width: 80px; height: 80px; }
.rg-wrap.lg { width: 140px; height: 140px; }
.rg-svg { width: 100%; height: 100%; display: block; }
.rg-center {
  position: absolute; inset: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  pointer-events: none;
}
.rg-val {
  font-size: 22px; font-weight: 700;
  font-family: "SF Mono","Consolas",monospace;
  line-height: 1; text-shadow: 0 0 8px currentColor;
}
.rg-wrap.sm .rg-val { font-size: 17px; }
.rg-wrap.lg .rg-val { font-size: 28px; }
.rg-lbl {
  font-size: 10px; color: #7a9cc0;
  letter-spacing: 0.5px; margin-top: 4px;
  text-align: center;
}
.rg-status {
  position: absolute; top: 4px; right: 4px;
  display: flex; align-items: center; gap: 4px;
  font-size: 9px; letter-spacing: 0.5px;
  padding: 2px 6px; border-radius: 2px;
  background: rgba(8,20,40,0.6); border: 1px solid rgba(62,170,255,0.2);
}
.rg-status-d {
  width: 5px; height: 5px; border-radius: 50%;
}
.rg-status.ok { color: #34d399; border-color: rgba(52,211,153,0.3); }
.rg-status.ok .rg-status-d { background: #34d399; box-shadow: 0 0 4px #34d399; }
.rg-status.warn { color: #fbbf24; border-color: rgba(251,191,36,0.3); }
.rg-status.warn .rg-status-d { background: #fbbf24; box-shadow: 0 0 4px #fbbf24; }
.rg-status.danger { color: #f87171; border-color: rgba(248,113,113,0.3); }
.rg-status.danger .rg-status-d { background: #f87171; box-shadow: 0 0 4px #f87171; }

/* 整个环根据状态有微动效 */
.rg-wrap.ok { animation: rgPulseOk 3s infinite; }
.rg-wrap.warn { animation: rgPulseWarn 2s infinite; }
.rg-wrap.danger { animation: rgPulseDanger 1.2s infinite; }
@keyframes rgPulseOk { 0%,100%{filter: brightness(1)} 50%{filter: brightness(1.08)} }
@keyframes rgPulseWarn { 0%,100%{filter: brightness(1)} 50%{filter: brightness(1.1)} }
@keyframes rgPulseDanger { 0%,100%{filter: brightness(1) drop-shadow(0 0 4px rgba(248,113,113,0.4))} 50%{filter: brightness(1.15) drop-shadow(0 0 12px rgba(248,113,113,0.6))} }
</style>
