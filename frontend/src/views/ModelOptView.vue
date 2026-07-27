<template>
<div class="mo-page">
  <!-- ═══ 顶栏 ═══ -->
  <div class="mo-head">
    <div class="mo-head-l">
      <span class="mo-bar"></span>
      <h2>模型自适应优化</h2>
      <span class="mo-sub">模型适配状态 · 工况匹配度 · 迭代历史 · 效果对比</span>
    </div>
    <div class="mo-head-r">
      <el-tag :type="modelState.adapted ? 'success' : 'warning'" effect="dark" size="small" class="mo-tag-hud">
        <span class="mo-pulse" :style="{background: modelState.adapted ? '#34d399' : '#fbbf24'}"></span>
        {{ modelState.adapted ? '已适配当前工况' : '优化进行中' }}
      </el-tag>
      <span class="mo-cv">当前版本 v{{ model.version }}</span>
    </div>
  </div>

  <!-- ═══ KPI 指标条 — 切角霓虹参数面板 ═══ -->
  <div class="mo-kpis">
    <div class="mo-kpi" v-for="k in kpis" :key="k.k" :style="{'--kc':k.c}">
      <div class="mo-kpi-inner">
        <!-- 顶部刻度装饰线 -->
        <div class="mo-kpi-ticks"></div>
        <!-- 左侧霓虹竖线 -->
        <div class="mo-kpi-accent"></div>
        <div class="mo-kpi-body">
          <div class="mo-kpi-l">{{ k.k }}</div>
          <div class="mo-kpi-v-row">
            <span class="mo-kpi-v">{{ k.v }}</span><span class="mo-kpi-u">{{ k.u }}</span>
          </div>
          <!-- 微型环比刻度条（替代简易三角） -->
          <div class="mo-kpi-trend-bar">
            <div class="mo-kpi-trend-fill" :class="k.up ? 'up':'down'" :style="{width: Math.min(100, Math.abs(parseFloat(k.t))*15) + '%'}"></div>
            <span class="mo-kpi-trend-txt" :class="k.up ? 'up':'down'">{{ k.up ? '▲' : '▼' }} {{ k.t }}</span>
          </div>
        </div>
      </div>
      <!-- 切角装饰（CSS clip-path） -->
      <div class="mo-kpi-corner mo-kpi-corner-tl"></div>
      <div class="mo-kpi-corner mo-kpi-corner-br"></div>
    </div>
  </div>

  <div class="mo-grid">
    <!-- ═══ 左列：模型适配状态 + 实时优化进度 ═══ -->
    <div class="mo-col mo-col-l">
      <!-- 模型适配状态面板 -->
      <div class="cd mo-card">
        <div class="cd-t"><span class="ut-ic">▸</span>模型适配状态</div>
        <div class="mo-state">
          <div class="mo-gauge-wrap">
            <div ref="gaugeRef" class="mo-gauge"></div>
            <div class="mo-gauge-cap">工况匹配度</div>
          </div>
          <div class="mo-state-list">
            <div class="mo-sl" v-for="s in fitItems" :key="s.k">
              <span class="mo-sl-k">{{ s.k }}</span>
              <div class="mo-sl-bar"><span :style="{width:s.p+'%', background:s.c}"></span></div>
              <span class="mo-sl-v" :style="{color:s.c}">{{ s.v }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 实时优化进度面板 -->
      <div class="cd mo-card">
        <div class="cd-t"><span class="ut-ic">▸</span>实时优化进度</div>
        <div class="mo-opt">
          <!-- 八角切角进度节点 -->
          <div class="mo-opt-stage">
            <div class="mo-stage" v-for="(st,i) in stages" :key="i"
                 :class="{done: st.done, active: st.active}">
              <div class="mo-stage-dot">
                <span class="mo-stage-ic">{{ st.done ? '✓' : (i+1) }}</span>
              </div>
              <div class="mo-stage-lb">{{ st.lb }}</div>
              <!-- 状态微标签 -->
              <span v-if="st.done" class="mo-stage-st done">完成</span>
              <span v-else-if="st.active" class="mo-stage-st active">执行中</span>
              <span v-else class="mo-stage-st wait">待执行</span>
            </div>
          </div>
          <!-- 霓虹进度管线 -->
          <div class="mo-opt-bar">
            <span :style="{width: optProgress + '%'}"></span>
            <div class="mo-opt-bar-glow"></div>
          </div>
          <div class="mo-opt-meta">
            <span>当前阶段：<b style="color:#3eaaff">{{ activeStage }}</b></span>
            <span>已完成 {{ optProgress }}% · 预计 {{ etaMin }} 分钟后完成本轮迭代</span>
          </div>
          <div class="mo-opt-log">
            <div v-for="(l,i) in optLog" :key="i" class="mo-log-i">
              <span class="mo-log-t">{{ l.t }}</span>
              <span class="mo-log-x" :class="l.cl">{{ l.x }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ 右列：效果对比 + 迭代历史 ═══ -->
    <div class="mo-col mo-col-r">
      <!-- 优化前后效果对比 -->
      <div class="cd mo-card">
        <div class="cd-t"><span class="ut-ic">▸</span>优化前后效果对比
          <span class="mo-legend"><i style="background:#5fb3ff"></i>优化前<i style="background:#34d399"></i>优化后</span>
        </div>
        <div ref="cmpRef" class="mo-cmp"></div>
      </div>

      <!-- 模型迭代历史 — 窄切角卡片列表 -->
      <div class="cd mo-card mo-hist">
        <div class="cd-t"><span class="ut-ic">▸</span>模型迭代历史
          <el-button text size="small" class="mo-refresh" @click="refreshIter"><el-icon><Refresh /></el-icon>刷新</el-button>
        </div>
        <div class="mo-timeline">
          <div v-for="(it,i) in iterations" :key="i" class="mo-tl-i" :class="it.cur ? 'cur':''">
            <div class="mo-tl-axis">
              <!-- 发光状态指示灯 -->
              <span class="mo-tl-dot" :class="it.cur ? 'active' : (it.type==='success'?'ok':'warn')"></span>
              <span v-if="i < iterations.length-1" class="mo-tl-line"></span>
            </div>
            <!-- 窄切角卡片 -->
            <div class="mo-tl-body">
              <div class="mo-tl-h">
                <span class="mo-tl-v">v{{ it.version }}</span>
                <span class="mo-tl-date">{{ it.date }}</span>
                <el-tag size="small" :type="it.type" effect="plain" class="mo-tl-tag">{{ it.tag }}</el-tag>
              </div>
              <div class="mo-tl-d">{{ it.desc }}</div>
              <div class="mo-tl-metrics">
                <span><i>准确率</i><b class="glow-green">{{ it.acc }}%</b><em v-if="it.accGain>=0" class="up">+{{ it.accGain }}</em><em v-else class="down">{{ it.accGain }}</em></span>
                <span><i>误报率</i><b class="glow-yellow">{{ it.fpr }}%</b><em :class="it.fprGain<=0 ? 'up':'down'">{{ it.fprGain>0?'+':'' }}{{ it.fprGain }}</em></span>
                <span><i>提前量</i><b class="glow-blue">{{ it.lead }}min</b><em class="up">+{{ it.leadGain }}</em></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- ═══ HUD 角落几何装饰 ═══ -->
  <div class="mo-dec mo-dec-tl"></div>
  <div class="mo-dec mo-dec-tr"></div>
  <div class="mo-dec mo-dec-bl"></div>
  <div class="mo-dec mo-dec-br"></div>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from '@/utils/echarts'
import { Refresh } from '@element-plus/icons-vue'

// ============ 数据（完全保留原逻辑） ============
const model = ref({ version: '3.2.1' })
const modelState = ref({ adapted: true })

const kpis = computed(() => [
  { k: '工况匹配度', v: 94.6, u: '%', c: '#34d399', up: true, t: '2.3' },
  { k: '预测准确率', v: 96.8, u: '%', c: '#5fb3ff', up: true, t: '1.1' },
  { k: '误报率', v: 3.2, u: '%', c: '#fbbf24', up: false, t: '0.8' },
  { k: '平均预警提前', v: 42, u: 'min', c: '#22d3ee', up: true, t: '6' },
  { k: '本轮迭代', v: 128, u: '次', c: '#3eaaff', up: true, t: '12' },
  { k: '在线样本', v: '8.4', u: '万', c: '#a78bfa', up: true, t: '0.6' }
])

const fitItems = computed(() => [
  { k: '锅炉工况', v: '96%', p: 96, c: '#34d399' },
  { k: '汽机工况', v: '93%', p: 93, c: '#34d399' },
  { k: '辅网工况', v: '91%', p: 91, c: '#5fb3ff' },
  { k: '变负荷段', v: '88%', p: 88, c: '#fbbf24' },
  { k: '启停机段', v: '82%', p: 82, c: '#fbbf24' }
])

const stages = ref([
  { lb: '数据采集', done: true, active: false },
  { lb: '特征工程', done: true, active: false },
  { lb: '模型训练', done: false, active: true },
  { lb: '在线验证', done: false, active: false },
  { lb: '灰度发布', done: false, active: false }
])
const optProgress = ref(58)
const etaMin = computed(() => Math.max(2, Math.round((100 - optProgress.value) / 8)))
const activeStage = computed(() => stages.value.find(s => s.active)?.lb || '—')

const optLog = ref([
  { t: '14:32:10', x: '加载 U1 变负荷段样本 12,480 条', cl: 'info' },
  { t: '14:32:46', x: '特征重要性重算完成，振动特征权重 +0.12', cl: 'ok' },
  { t: '14:33:20', x: '训练批次 #128 loss=0.021（↓3.4%）', cl: 'ok' },
  { t: '14:34:02', x: '检测到 A磨煤机残差偏离，触发局部微调', cl: 'warn' },
  { t: '14:34:55', x: '在线 A/B 验证：召回 +1.1% / 误报 -0.8%', cl: 'ok' }
])

const iterations = ref([
  { version: '3.2.1', date: '2026-07-24', type: 'success', tag: '已发布', cur: true, desc: '引入残差预警头 + 工况自适应归一化，变负荷段匹配度提升 6%', acc: 96.8, accGain: 1.1, fpr: 3.2, fprGain: -0.8, lead: 42, leadGain: 6 },
  { version: '3.1.0', date: '2026-07-10', type: 'success', tag: '已发布', cur: false, desc: '新增变化速率预警类型，解决缓变故障漏报问题', acc: 95.7, accGain: 0.9, fpr: 4.0, fprGain: -0.5, lead: 36, leadGain: 4 },
  { version: '3.0.2', date: '2026-06-22', type: 'success', tag: '已发布', cur: false, desc: '修复 U2 启停机段误报，引入停机状态屏蔽', acc: 94.8, accGain: 0.6, fpr: 4.5, fprGain: -1.2, lead: 32, leadGain: 2 },
  { version: '3.0.0', date: '2026-05-30', type: 'warning', tag: '回滚', cur: false, desc: '大模型一次性重构导致误报率飙升，回滚至 2.x', acc: 94.2, accGain: -2.1, fpr: 5.7, fprGain: 2.3, lead: 30, leadGain: -3 },
  { version: '2.4.5', date: '2026-05-12', type: 'success', tag: '已发布', cur: false, desc: '知识图谱融合推理上线，根因定位准确率提升', acc: 96.3, accGain: 1.8, fpr: 3.4, fprGain: -0.9, lead: 33, leadGain: 5 },
  { version: '2.3.0', date: '2026-04-18', type: 'success', tag: '已发布', cur: false, desc: '初始 Transformer 时序预测骨干网络', acc: 94.5, accGain: 3.4, fpr: 4.3, fprGain: -1.5, lead: 28, leadGain: 8 }
])

const refreshIter = () => {
  optProgress.value = Math.min(100, optProgress.value + 6)
  if (optProgress.value >= 100) {
    stages.value.forEach(s => { s.done = true; s.active = false })
    modelState.value.adapted = true
  } else {
    const idx = stages.value.findIndex(s => !s.done)
    stages.value.forEach((s, i) => { s.active = i === idx })
  }
}

// ============ 图表（升级为 HUD 风格） ============
const gaugeRef = ref(null), cmpRef = ref(null)
let gaugeCh, cmpCh

/** 径向栅格刻度仪表盘 —— 机械传感器质感 */
const initGauge = () => {
  if (!gaugeRef.value) return
  if (gaugeCh) gaugeCh.dispose()
  gaugeCh = echarts.init(gaugeRef.value)
  gaugeCh.setOption({
    backgroundColor: 'transparent',
    series: [{
      type: 'gauge',
      min: 0, max: 100,
      radius: '90%',
      center: ['50%', '55%'],
      startAngle: 210,
      endAngle: -30,
      // 径向栅格刻度条（分段式，非平滑弧形）
      progress: {
        show: true,
        width: 11,
        roundCap: false,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#22d3ee' },
            { offset: 1, color: '#34d399' }
          ]),
          shadowBlur: 0,
          borderColor: '#34d399',
          borderWidth: 0.5
        }
      },
      // 轨道底色
      axisLine: {
        lineStyle: {
          width: 11,
          color: [[1, 'rgba(62,170,255,0.08)']]
        }
      },
      // 外圈长短交替机械刻度环
      axisTick: {
        distance: -16,
        length: 5,
        lineStyle: { color: 'rgba(90,166,196,0.35)', width: 1.2 },
        interval: 3
      },
      splitLine: {
        distance: -20,
        length: 12,
        lineStyle: { color: 'rgba(90,166,196,0.55)', width: 1.5 }
      },
      axisLabel: {
        distance: -4,
        color: '#7aa8c8',
        fontSize: 10,
        fontFamily: '"SF Mono","Consolas",monospace',
        fontWeight: 600
      },
      pointer: {
        show: true,
        length: '58%',
        width: 2.5,
        itemStyle: { color: '#22d3ee' }
      },
      anchor: {
        show: true,
        size: 7,
        itemStyle: { color: '#22d3ee', borderColor: '#000', borderWidth: 2 }
      },
      detail: {
        valueAnimation: true,
        fontSize: 32,
        fontWeight: 800,
        fontFamily: '"SF Mono","Consolas",monospace',
        color: '#34d399',
        offsetCenter: [0, '40%'],
        formatter: '{value}%',
        textShadowColor: 'rgba(52,211,153,0.5)',
        textShadowBlur: 8
      },
      title: { show: false },
      data: [{ value: 94.6 }]
    }]
  })
}

/** LED 霓虹光带条形图 */
const initCmp = () => {
  if (!cmpRef.value) return
  if (cmpCh) cmpCh.dispose()
  cmpCh = echarts.init(cmpRef.value)
  const cats = ['预测准确率', '预警召回', '根因命中', '提前量(min)', '工况匹配']
  const before = [93.5, 90.2, 86.0, 34, 88]
  const after = [96.8, 95.4, 93.1, 42, 94.6]

  cmpCh.setOption({
    backgroundColor: 'transparent',
    grid: { left: 72, right: 28, top: 18, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(6,14,28,0.95)',
      borderColor: 'rgba(90,166,196,0.25)',
      textStyle: { color: '#c8e4ff', fontFamily: '"SF Mono","Consolas",monospace', fontSize: 11 }
    },
    xAxis: {
      type: 'value',
      max: 105,
      axisLabel: {
        color: '#7aa8c8',
        fontSize: 10,
        fontFamily: '"SF Mono","Consolas",monospace'
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(90,166,196,0.09)',
          type: 'dashed',
          dashArray: [4, 4]
        }
      },
      axisLine: { lineStyle: { color: 'rgba(90,166,196,0.15)' } }
    },
    yAxis: {
      type: 'category',
      data: cats,
      axisLabel: {
        color: '#9fb6cf',
        fontSize: 11,
        fontFamily: '"SF Mono","Consolas",monospace'
      },
      axisLine: { lineStyle: { color: 'rgba(90,166,196,0.15)' } },
      axisTick: { show: false }
    },
    series: [
      {
        name: '优化前',
        type: 'bar',
        data: before,
        barWidth: 10,
        barGap: '40%',
        itemStyle: {
          color: 'rgba(95,179,255,0.18)',
          borderColor: '#5fb3ff',
          borderWidth: 1,
          borderRadius: [0, 2, 2, 0]
        },
        label: {
          show: true,
          position: 'right',
          color: '#5fb3ff',
          fontSize: 11,
          fontFamily: '"SF Mono","Consolas",monospace',
          fontWeight: 600,
          formatter: '{c}%'
        }
      },
      {
        name: '优化后',
        type: 'bar',
        data: after,
        barWidth: 10,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: 'rgba(34,211,238,0.25)' },
            { offset: 1, color: 'rgba(52,211,153,0.45)' }
          ]),
          borderColor: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#22d3ee' },
            { offset: 1, color: '#34d399' }
          ]),
          borderWidth: 1.2,
          borderRadius: [0, 2, 2, 0],
          shadowBlur: 6,
          shadowColor: 'rgba(52,211,153,0.2)'
        },
        label: {
          show: true,
          position: 'right',
          color: '#34d399',
          fontSize: 11,
          fontFamily: '"SF Mono","Consolas",monospace',
          fontWeight: 700,
          formatter: '{c}%'
        }
      }
    ]
  })
}

const rz = () => { gaugeCh?.resize(); cmpCh?.resize() }
onMounted(() => { nextTick(() => { initGauge(); initCmp() }); window.addEventListener('resize', rz) })
onUnmounted(() => { window.removeEventListener('resize', rz); gaugeCh?.dispose(); cmpCh?.dispose() })
</script>

<style scoped>
/* ═══ 全局变量 & 根容器 ═══ */
.mo-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
  overflow: hidden;
  background-color: #030508;
  position: relative;
  font-family: "SF Mono","Consolas","JetBrains Mono",monospace;
}

/* ═══ 顶栏 ═══ */
.mo-head { flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; }
.mo-head-l { display: flex; align-items: center; gap: 12px; }
.mo-bar {
  width: 4px; height: 22px;
  background: linear-gradient(180deg, #3eaaff, #22d3ee);
  border-radius: 2px;
  box-shadow: 0 0 10px rgba(62,170,255,0.4);
}
.mo-head h2 {
  font-size: 17px; font-weight: 600; color: #d4ecff;
  letter-spacing: 0.5px;
  font-family: "SF Mono","Consolas",monospace;
}
.mo-sub { font-size: 12px; color: #6a8faa; letter-spacing: 0.3px; }
.mo-head-r { display: flex; align-items: center; gap: 12px; }

.mo-tag-hud {
  border: 1px solid rgba(90,166,196,0.2) !important;
  background: rgba(4,10,20,0.85) !important;
  font-family: "SF Mono","Consolas",monospace !important;
  font-size: 11px !important;
}
.mo-pulse {
  width: 7px; height: 7px; border-radius: 50%; display: inline-block; margin-right: 5px;
  animation: mpulse 2s infinite;
}
@keyframes mpulse { 0%,100%{opacity:1} 50%{opacity:.3} }
.mo-cv { font-size: 12px; color: #6a8faa; font-family: "SF Mono","Consolas",monospace; }

/* ═══ KPI 指标卡 — 切角霓虹参数面板 ═══ */
.mo-kpis {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}
.mo-kpi {
  position: relative;
  padding: 0;
  overflow: visible;
  /* 切角效果 */
  clip-path: polygon(
    0 0, calc(100% - 10px) 0, 100% 10px,
    100% 100%, 10px 100%, 0 calc(100% - 10px)
  );
}
.mo-kpi-inner {
  padding: 14px 16px;
  height: 100%;
  background:
    linear-gradient(135deg, rgba(12,22,40,0.92) 0%, rgba(6,12,24,0.88) 100%);
  border: 1px solid rgba(90,166,196,0.13);
  display: flex;
  flex-direction: column;
  gap: 2px;
  position: relative;
  overflow: hidden;
}
/* 内部极淡横向细分隔纹理 */
.mo-kpi-inner::after {
  content: '';
  position: absolute;
  left: 0; right: 0; top: 0; bottom: 0;
  background:
    repeating-linear-gradient(
      0deg,
      transparent 0px,
      transparent 7px,
      rgba(90,166,196,0.025) 7px,
      rgba(90,166,196,0.025) 8px
    );
  pointer-events: none;
}
/* 顶部刻度装饰线 */
.mo-kpi-ticks {
  position: absolute;
  top: 0; left: 16px; right: 16px;
  height: 1px;
  background:
    repeating-linear-gradient(
      90deg,
      transparent 0px,
      transparent 6px,
      rgba(90,166,196,0.12) 6px,
      rgba(90,166,196,0.12) 7px
    );
}
/* 左侧霓虹竖线 */
.mo-kpi-accent {
  position: absolute;
  left: 0; top: 10px; bottom: 10px;
  width: 2px;
  background: var(--kc);
  box-shadow: 0 0 6px var(--kc), inset 0 0 4px var(--kc);
  opacity: 0.75;
}
.mo-kpi-body {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.mo-kpi-l {
  font-size: 11px;
  color: #6a8faa;
  letter-spacing: 0.3px;
  font-weight: 500;
}
.mo-kpi-v-row {
  display: flex;
  align-items: baseline;
  gap: 3px;
}
.mo-kpi-v {
  font-size: 26px;
  font-weight: 800;
  font-family: "SF Mono","Consolas",monospace;
  color: #e8f4ff;
  text-shadow: 0 0 12px rgba(62,170,255,0.2);
  line-height: 1;
}
.mo-kpi-u {
  font-size: 12px;
  color: #5a7894;
  font-weight: 400;
}
/* 微型环比刻度条（替代简易三角） */
.mo-kpi-trend-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  height: 4px;
  background: rgba(90,166,196,0.06);
  border-radius: 1px;
  overflow: hidden;
  position: relative;
}
.mo-kpi-trend-fill {
  height: 100%;
  border-radius: 1px;
  transition: width 0.5s;
}
.mo-kpi-trend-fill.up { background: rgba(52,211,153,0.55); box-shadow: 0 0 4px rgba(52,211,153,0.3); }
.mo-kpi-trend-fill.down { background: rgba(248,113,113,0.45); box-shadow: 0 0 4px rgba(248,113,113,0.25); }
.mo-kpi-trend-txt {
  font-size: 10px;
  font-weight: 600;
  font-family: "SF Mono","Consolas",monospace;
  white-space: nowrap;
}
.mo-kpi-trend-txt.up { color: #34d399; }
.mo-kpi-trend-txt.down { color: #f87171; }

/* 切角装饰角标 */
.mo-kpi-corner {
  position: absolute;
  width: 10px; height: 10px;
  pointer-events: none;
}
.mo-kpi-corner-tl { top: 0; left: 0; border-top: 1.5px solid rgba(90,166,196,0.3); border-left: 1.5px solid rgba(90,166,196,0.3); }
.mo-kpi-corner-br { bottom: 0; right: 0; border-bottom: 1.5px solid rgba(90,166,196,0.3); border-right: 1.5px solid rgba(90,166,196,0.3); }

/* ═══ 网格布局 ═══ */
.mo-grid {
  flex: 1; min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1.25fr;
  gap: 14px;
}
.mo-col { display: flex; flex-direction: column; gap: 14px; min-height: 0; }
.mo-card { display: flex; flex-direction: column; min-height: 0; flex: 1; }

/* ═══ 面板卡片 — 统一切角微玻璃拟态 ═══ */
.cd {
  background: rgba(6,12,22,0.85);
  border: 1px solid rgba(90,166,196,0.12);
  border-radius: 4px;
  padding: 14px 16px;
  position: relative;
  /* 微弱内发光 */
  box-shadow:
    inset 0 1px 0 rgba(160,200,255,0.04),
    inset 0 0 20px rgba(90,166,196,0.03);
}
.cd::before {
  content: '';
  position: absolute;
  top: 0; left: 12px; right: 12px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(90,166,196,0.15), transparent);
}
.cd-t {
  font-size: 13px;
  font-weight: 600;
  color: #c8e4ff;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  letter-spacing: 0.5px;
  font-family: "SF Mono","Consolas",monospace;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(90,166,196,0.08);
}
.ut-ic { color: #3eaaff; font-weight: 700; margin-right: 4px; font-size: 12px; }
.mo-legend {
  font-size: 10px; color: #6a8faa;
  display: flex; align-items: center; gap: 4px; font-weight: 400;
  font-family: "SF Mono","Consolas",monospace;
}
.mo-legend i {
  width: 16px; height: 3px;
  border-radius: 1px; display: inline-block; margin: 0 3px 0 8px;
  box-shadow: 0 0 4px currentColor;
}
.mo-refresh { color: #5fb3ff; margin-left: auto; font-family: "SF Mono","Consolas",monospace; }

/* ═══ 仪表盘区 ═══ */
.mo-state { display: flex; gap: 18px; align-items: center; flex: 1; min-height: 0; }
.mo-gauge-wrap { position: relative; width: 165px; height: 175px; flex-shrink: 0; }
.mo-gauge { width: 100%; height: 100%; }
.mo-gauge-cap {
  position: absolute; bottom: 6px; left: 0; right: 0;
  text-align: center; font-size: 11px; color: #6a8faa;
  font-family: "SF Mono","Consolas",monospace;
  letter-spacing: 1px;
}
.mo-state-list { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.mo-sl { display: flex; align-items: center; gap: 10px; font-size: 12px; }
.mo-sl-k { width: 64px; color: #8aa8c4; flex-shrink: 0; font-family: "SF Mono","Consolas",monospace; font-size: 11px; }
.mo-sl-bar {
  flex: 1; height: 6px;
  background: rgba(90,166,196,0.06);
  border-radius: 1px;
  overflow: hidden;
  position: relative;
}
.mo-sl-bar span {
  display: block; height: 100%;
  border-radius: 1px;
  box-shadow: 0 0 5px currentColor, inset 0 0 2px currentColor;
}
.mo-sl-v {
  width: 36px; text-align: right;
  font-weight: 700; font-family: "SF Mono","Consolas",monospace;
  font-size: 12px;
  text-shadow: 0 0 6px currentColor;
}

/* ═══ 优化进度 — 八角切角节点 + 霓虹管线 ═══ */
.mo-opt { display: flex; flex-direction: column; gap: 12px; flex: 1; min-height: 0; }
.mo-opt-stage {
  display: flex; justify-content: space-between;
  position: relative;
  padding: 0 4px;
}
.mo-stage {
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  position: relative; z-index: 1; flex: 1;
}
/* 八角切角点位 */
.mo-stage-dot {
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(6,12,22,0.9);
  border: 1.5px solid rgba(90,166,196,0.2);
  /* 八角切角 */
  clip-path: polygon(30% 0%, 70% 0%, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0% 70%, 0% 30%);
  transition: all 0.3s;
  box-shadow: inset 0 0 6px rgba(90,166,196,0.08);
}
.mo-stage-ic { font-size: 12px; font-weight: 700; color: #5a7894; font-family: "SF Mono","Consolas",monospace; }
/* 完成 */
.mo-stage.done .mo-stage-dot {
  border-color: rgba(52,211,153,0.5);
  box-shadow: inset 0 0 8px rgba(52,211,153,0.15), 0 0 6px rgba(52,211,153,0.2);
}
.mo-stage.done .mo-stage-ic { color: #34d399; }
/* 进行中 */
.mo-stage.active .mo-stage-dot {
  border-color: rgba(62,170,255,0.6);
  box-shadow: inset 0 0 10px rgba(62,170,255,0.2), 0 0 10px rgba(62,170,255,0.3);
  animation: spulse 1.5s infinite;
}
.mo-stage.active .mo-stage-ic { color: #3eaaff; }
@keyframes spulse {
  0%,100%{box-shadow: inset 0 0 8px rgba(62,170,255,0.15), 0 0 6px rgba(62,170,255,0.2)}
  50%{box-shadow: inset 0 0 14px rgba(62,170,255,0.3), 0 0 14px rgba(62,170,255,0.5)}
}
.mo-stage-lb {
  font-size: 10.5px; color: #6a8faa;
  font-family: "SF Mono","Consolas",monospace;
  letter-spacing: 0.3px;
}
.mo-stage.active .mo-stage-lb { color: #3eaaff; }

/* 状态微标签 */
.mo-stage-st {
  font-size: 9px;
  font-family: "SF Mono","Consolas",monospace;
  padding: 1px 6px;
  border-radius: 2px;
  letter-spacing: 0.5px;
  font-weight: 600;
}
.mo-stage-st.done { color: #34d399; background: rgba(52,211,153,0.1); border: 1px solid rgba(52,211,153,0.2); }
.mo-stage-st.active { color: #3eaaff; background: rgba(62,170,255,0.1); border: 1px solid rgba(62,170,255,0.25); animation: stagPulse 1.5s infinite; }
.mo-stage-st.wait { color: #4a6080; background: rgba(90,166,196,0.05); border: 1px solid rgba(90,166,196,0.1); }
@keyframes stagPulse { 0%,100%{opacity:1} 50%{opacity:.5} }

/* 霓虹进度管线 */
.mo-opt-bar {
  height: 4px;
  background: rgba(90,166,196,0.06);
  border-radius: 1px;
  overflow: hidden;
  margin-top: 2px;
  position: relative;
}
.mo-opt-bar span {
  display: block; height: 100%;
  background: linear-gradient(90deg, #22d3ee, #34d399);
  border-radius: 1px;
  transition: width 0.5s;
  box-shadow: 0 0 8px rgba(52,211,153,0.35), inset 0 0 2px rgba(255,255,255,0.15);
}
.mo-opt-bar-glow {
  position: absolute;
  top: -2px; left: 0;
  height: 8px;
  background: linear-gradient(90deg, transparent, rgba(52,211,153,0.15), transparent);
  filter: blur(3px);
  pointer-events: none;
}

.mo-opt-meta {
  display: flex; justify-content: space-between;
  font-size: 11px; color: #6a8faa;
  flex-wrap: wrap; gap: 4px;
  font-family: "SF Mono","Consolas",monospace;
}
.mo-opt-log {
  flex: 1; min-height: 0; overflow-y: auto;
  background: rgba(4,8,16,0.5);
  border: 1px solid rgba(90,166,196,0.07);
  border-radius: 3px;
  padding: 8px 10px;
  font-family: "SF Mono","Consolas",monospace;
}
.mo-opt-log::-webkit-scrollbar { width: 3px; }
.mo-opt-log::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.mo-log-i {
  display: flex; gap: 8px;
  font-size: 10.5px;
  padding: 3px 0;
  border-bottom: 0.5px dashed rgba(62,170,255,0.06);
}
.mo-log-t { color: #4a6080; font-family: "SF Mono","Consolas",monospace; flex-shrink: 0; }
.mo-log-x { color: #a8c4e0; }
.mo-log-x.ok { color: #34d399; }
.mo-log-x.warn { color: #fbbf24; }
.mo-log-x.info { color: #5fb3ff; }

/* ═══ 对比图 ═══ */
.mo-cmp { flex: 1; min-height: 180px; }

/* ═══ 迭代历史 — 窄切角卡片列表 ═══ */
.mo-hist { flex: 1.4; }
.mo-timeline { flex: 1; overflow-y: auto; min-height: 0; }
.mo-timeline::-webkit-scrollbar { width: 3px; }
.mo-timeline::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.mo-tl-i { display: flex; gap: 12px; }
.mo-tl-axis { display: flex; flex-direction: column; align-items: center; width: 12px; flex-shrink: 0; }
/* 发光状态指示灯 */
.mo-tl-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  flex-shrink: 0; margin-top: 4px;
  transition: all 0.3s;
}
.mo-tl-dot.ok {
  background: #34d399;
  box-shadow: 0 0 6px rgba(52,211,153,0.5);
}
.mo-tl-dot.active {
  background: #3eaaff;
  box-shadow: 0 0 8px rgba(62,170,255,0.6);
  animation: dotPulse 2s infinite;
}
.mo-tl-dot.warn {
  background: #fbbf24;
  box-shadow: 0 0 5px rgba(251,191,36,0.4);
}
@keyframes dotPulse { 0%,100%{box-shadow:0 0 6px rgba(62,170,255,0.4)} 50%{box-shadow:0 0 14px rgba(62,170,255,0.7)} }
.mo-tl-line {
  width: 1.5px; flex: 1;
  background: linear-gradient(180deg, rgba(90,166,196,0.2), rgba(90,166,196,0.06));
  margin: 2px 0;
}

/* 窄切角卡片 */
.mo-tl-body {
  flex: 1;
  padding: 10px 14px;
  margin-bottom: 10px;
  background: rgba(6,12,22,0.6);
  border: 1px solid rgba(90,166,196,0.1);
  border-radius: 3px;
  /* 轻微切角 */
  clip-path: polygon(
    0 0, calc(100% - 6px) 0, 100% 6px,
    100% 100%, 6px 100%, 0 calc(100% - 6px)
  );
  transition: all 0.25s;
  position: relative;
}
.mo-tl-body::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(90,166,196,0.12), transparent 60%);
}
.mo-tl-i.cur .mo-tl-body {
  border-color: rgba(62,170,255,0.3);
  box-shadow: inset 0 0 15px rgba(62,170,255,0.04), 0 0 12px rgba(62,170,255,0.06);
}
.mo-tl-h { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.mo-tl-v {
  font-size: 13px; font-weight: 700; color: #3eaaff;
  font-family: "SF Mono","Consolas",monospace;
}
.mo-tl-date { font-size: 10.5px; color: #5a7894; font-family: "SF Mono","Consolas",monospace; }
.mo-tl-tag { font-family: "SF Mono","Consolas",monospace; font-size: 10px !important; }
.mo-tl-d {
  font-size: 11.5px; color: #9fb6cf;
  line-height: 1.6; margin-bottom: 8px;
}
.mo-tl-metrics { display: flex; gap: 16px; flex-wrap: wrap; }
.mo-tl-metrics span {
  font-size: 10.5px; color: #6a8faa;
  display: flex; align-items: baseline; gap: 4px;
  font-family: "SF Mono","Consolas",monospace;
}
.mo-tl-metrics i { font-style: normal; color: #4a6080; }
.mo-tl-metrics b {
  font-family: "SF Mono","Consolas",monospace;
  font-size: 13px;
  font-weight: 700;
}
/* 发光数字 */
.mo-tl-metrics b.glow-green { color: #34d399; text-shadow: 0 0 6px rgba(52,211,153,0.4); }
.mo-tl-metrics b.glow-yellow { color: #fbbf24; text-shadow: 0 0 6px rgba(251,191,36,0.35); }
.mo-tl-metrics b.glow-blue { color: #5fb3ff; text-shadow: 0 0 6px rgba(95,179,255,0.35); }
.mo-tl-metrics em { font-style: normal; font-size: 10.5px; font-weight: 600; }
.mo-tl-metrics em.up { color: #34d399; }
.mo-tl-metrics em.down { color: #f87171; }

/* ═══ HUD 角落几何装饰 ═══ */
.mo-dec {
  position: absolute;
  pointer-events: none;
  opacity: 0.35;
}
.mo-dec-tl { top: 8px; left: 8px; width: 20px; height: 20px;
  border-top: 1.5px solid rgba(90,166,196,0.3); border-left: 1.5px solid rgba(90,166,196,0.3); }
.mo-dec-tr { top: 8px; right: 8px; width: 20px; height: 20px;
  border-top: 1.5px solid rgba(90,166,196,0.3); border-right: 1.5px solid rgba(90,166,196,0.3); }
.mo-dec-bl { bottom: 8px; left: 8px; width: 20px; height: 20px;
  border-bottom: 1.5px solid rgba(90,166,196,0.3); border-left: 1.5px solid rgba(90,166,196,0.3); }
.mo-dec-br { bottom: 8px; right: 8px; width: 20px; height: 20px;
  border-bottom: 1.5px solid rgba(90,166,196,0.3); border-right: 1.5px solid rgba(90,166,196,0.3); }

/* ═══ 响应式 ═══ */
@media(max-width: 1400px){
  .mo-kpis { grid-template-columns: repeat(3, 1fr); }
  .mo-grid { grid-template-columns: 1fr; overflow-y: auto; }
  .mo-col { min-height: auto; }
}
</style>

<!-- ═══ 非scoped覆盖：压制全局 .cd !important + CRT 示波器网格 ═══ -->
<style>
/* 所有面板内容区：纯黑 + 均匀等距青蓝虚线示波器栅格 */
.mo-page .mo-grid .cd {
  background-color: #030508 !important;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.07) 0px, rgba(90,166,196,0.07) 1px, transparent 1px, transparent 28px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.07) 0px, rgba(90,166,196,0.07) 1px, transparent 1px, transparent 28px) !important;
  border-color: rgba(90,166,196,0.14) !important;
  box-shadow:
    inset 0 1px 0 rgba(160,200,255,0.04),
    inset 0 0 20px rgba(90,166,196,0.025) !important;
}
/* 标题栏：深蓝不透明底挡网格 */
.mo-page .mo-grid .cd > .cd-t {
  background-color: #06101a !important;
  background-image: none !important;
}
</style>
