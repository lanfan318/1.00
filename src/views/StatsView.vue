<template>
<div class="stats-wrap">
  <!-- 时间切换 -->
  <div class="period-header">
    <el-tabs v-model="period" class="period-tabs">
      <el-tab-pane label="今日" name="today" />
      <el-tab-pane label="本周" name="week" />
      <el-tab-pane label="本月" name="month" />
    </el-tabs>
  </div>

  <!-- 控制条：周期对比 + 交叉分析 + AI 报告 -->
  <div class="st-ctrls">
    <div class="st-ctrl">
      <label>周期对比</label>
      <el-radio-group v-model="cmpMode" size="small">
        <el-radio-button value="hb">环比</el-radio-button>
        <el-radio-button value="tb">同比</el-radio-button>
      </el-radio-group>
    </div>
    <div class="st-ctrl">
      <label>交叉维度</label>
      <el-select v-model="crossDim" size="small" style="width:130px">
        <el-option value="dept" label="专业 × 级别" />
        <el-option value="type" label="类型 × 级别" />
        <el-option value="status" label="状态 × 级别" />
      </el-select>
    </div>
    <el-button type="primary" size="small" class="st-ai-btn" @click="openReport">
      <el-icon><MagicStick /></el-icon> 生成 AI 统计报告
    </el-button>
    <span class="st-ctrl-tip">对比基准：{{ cmpMode === 'hb' ? '上一周期' : '去年同期' }}</span>
  </div>

  <!-- 统计概览 — 紧凑横向KPI条（压缩为更小的横排） -->
  <div class="stats-overview">
    <div class="sc"><span class="sc-lbl">报警总数</span><span class="sc-val in">{{ stats.total }}</span><span class="sc-sub">{{ store.selectedUnit.name }}</span><span class="sc-delta" :class="deltas.total.dir">{{ deltas.total.dir === 'up' ? '▲' : '▼' }} {{ deltas.total.pct }}%<i>{{ cmpMode === 'hb' ? '环比' : '同比' }}</i></span></div>
    <div class="sc"><span class="sc-lbl">未处理</span><span class="sc-val dg">{{ stats.unhandled }}</span><span class="sc-sub">条需响应</span><span class="sc-delta" :class="deltas.unhandled.dir">{{ deltas.unhandled.dir === 'up' ? '▲' : '▼' }} {{ deltas.unhandled.pct }}%<i>{{ cmpMode === 'hb' ? '环比' : '同比' }}</i></span></div>
    <div class="sc"><span class="sc-lbl">已处理</span><span class="sc-val ok">{{ stats.handled }}</span><span class="sc-sub">条已闭环</span><span class="sc-delta" :class="deltas.handled.dir">{{ deltas.handled.dir === 'up' ? '▲' : '▼' }} {{ deltas.handled.pct }}%<i>{{ cmpMode === 'hb' ? '环比' : '同比' }}</i></span></div>
    <div class="sc"><span class="sc-lbl">智能过滤</span><span class="sc-val cy">{{ filterCnt }}</span><span class="sc-sub">条误报</span><span class="sc-delta" :class="deltas.filter.dir">{{ deltas.filter.dir === 'up' ? '▲' : '▼' }} {{ deltas.filter.pct }}%<i>{{ cmpMode === 'hb' ? '环比' : '同比' }}</i></span></div>
  </div>

  <!-- 占比指标行 — 复用全域大屏底部设计 -->
  <div class="ratio-row">
    <div class="ratio-card" v-for="(r,i) in ratioMetrics" :key="i" :style="{'--rc': r.c}">
      <div class="ratio-top">
        <span class="ratio-lbl">{{ r.k }}</span>
        <span class="ratio-tag" :class="r.cl">{{ r.tag }}</span>
      </div>
      <div class="ratio-val" :style="{color:r.c}">{{ r.v }}<span class="ratio-u">{{ r.u }}</span></div>
      <div class="ratio-bar-track"><span class="ratio-bar-fill" :style="{width:r.p+'%',background:r.c}"></span></div>
      <div class="ratio-sub">{{ r.s }}</div>
    </div>
  </div>

  <!-- 图表置换面板：饼图 / 柱状图 / 交叉分析 三页切换 -->
  <div class="chart-panel">
    <div class="chart-panel-tabs">
      <button v-for="(t, i) in chartTabs" :key="t.key"
        class="cpt-tab" :class="{active: chartTab === t.key}"
        @click="switchChartTab(t.key)">
        <span class="cpt-ic">{{ t.ic }}</span>{{ t.label }}
      </button>
      <div class="cpt-indicator" :style="{transform:`translateX(${chartTabIndex * 100}%)`}"></div>
    </div>
    <div class="chart-panel-body">
      <!-- 页1：饼图分布（始终挂载，v-show 切换） -->
      <div v-show="chartTab==='pie'" class="cp-page cp-pie">
        <div class="cd pie-col">
          <div class="cd-h"><span class="cd-t">报警专业占比</span><span class="cd-sub">锅炉 {{ profCnt['锅炉'] }} / 汽机 {{ profCnt['汽机'] }} / 辅网 {{ profCnt['辅网'] }}</span></div>
          <div class="hud-stage">
            <div ref="chProf" class="pie-chart"></div>
            <!-- 四角八角形状态卡片（与canvas同级，z-index在canvas之上） -->
            <div class="hud-corner tl"><div class="hud-corner-inner"><em>锅炉</em><strong>{{ profCnt['锅炉'] }}</strong></div></div>
            <div class="hud-corner tr"><div class="hud-corner-inner"><em>汽机</em><strong>{{ profCnt['汽机'] }}</strong></div></div>
            <div class="hud-corner bl"><div class="hud-corner-inner"><em>辅网</em><strong>{{ profCnt['辅网'] }}</strong></div></div>
            <div class="hud-corner br"><div class="hud-corner-inner"><em>合计</em><strong>{{ profCnt['锅炉']+profCnt['汽机']+profCnt['辅网'] }}</strong></div></div>
            <!-- 独立中心信息牌 -->
            <div class="hud-center-plate">
              <span class="hcp-sub">设备总数</span>
              <span class="hcp-val">{{ profCnt['锅炉']+profCnt['汽机']+profCnt['辅网'] }}</span>
            </div>
          </div>
        </div>
        <div class="cd pie-col">
          <div class="cd-h"><span class="cd-t">级别占比</span><span class="cd-sub">一级 {{ cnt(1) }} / 二级 {{ cnt(2) }} / 预警 {{ cnt(3) }}</span></div>
          <div class="hud-stage">
            <div ref="chLevel" class="pie-chart"></div>
            <div class="hud-corner tl"><div class="hud-corner-inner"><em>一级</em><strong>{{ cnt(1) }}</strong></div></div>
            <div class="hud-corner tr"><div class="hud-corner-inner"><em>二级</em><strong>{{ cnt(2) }}</strong></div></div>
            <div class="hud-corner bl"><div class="hud-corner-inner"><em>预警</em><strong>{{ cnt(3) }}</strong></div></div>
            <div class="hud-corner br"><div class="hud-corner-inner"><em>总计</em><strong>{{ stats.total }}</strong></div></div>
            <div class="hud-center-plate">
              <span class="hcp-sub">报警总数</span>
              <span class="hcp-val">{{ stats.total }}</span>
            </div>
          </div>
        </div>
        <div class="cd pie-col">
          <div class="cd-h"><span class="cd-t">处理状态</span><span class="cd-sub">已处理 {{ stats.handled }} / 未处理 {{ stats.unhandled }} / 抑制 {{ suppressed }}</span></div>
          <div class="hud-stage">
            <div ref="chStatus" class="pie-chart"></div>
            <div class="hud-corner tl"><div class="hud-corner-inner"><em>已处理</em><strong>{{ stats.handled }}</strong></div></div>
            <div class="hud-corner tr"><div class="hud-corner-inner"><em>未处理</em><strong>{{ stats.unhandled }}</strong></div></div>
            <div class="hud-corner bl"><div class="hud-corner-inner"><em>抑制</em><strong>{{ suppressed }}</strong></div></div>
            <div class="hud-corner br"><div class="hud-corner-inner"><em>总计</em><strong>{{ stats.handled+stats.unhandled+suppressed }}</strong></div></div>
            <div class="hud-center-plate">
              <span class="hcp-sub">处置总数</span>
              <span class="hcp-val">{{ stats.handled+stats.unhandled+suppressed }}</span>
            </div>
          </div>
        </div>
      </div>
      <!-- 页2：柱状排行（始终挂载） -->
      <div v-show="chartTab==='bar'" class="cp-page cp-bar">
        <div class="cd bar-col">
          <div class="cd-h"><span class="cd-t">报警次数 Top10（按测点）</span><span class="cd-sub">单位：次 | 本期累计 <strong style="color:#fbbf24">{{ totalTimes }}</strong> 次</span></div>
          <div ref="chTop10" class="bar-chart"></div>
        </div>
        <div class="cd bar-col">
          <div class="cd-h"><span class="cd-t">报警时长 Top10（按测点）</span><span class="cd-sub">单位：分钟 | 本期累计 <strong style="color:#ef4444">{{ totalMins }}</strong> 分钟</span></div>
          <div ref="chDur" class="bar-chart"></div>
        </div>
      </div>
      <!-- 页3：交叉分析（始终挂载） -->
      <div v-show="chartTab==='cross'" class="cp-page cp-cross">
        <div class="cd cross-inner">
          <!-- 切角科技边框标题牌 -->
          <div class="crt-title-plate">
            <span class="ctp-main">交叉分析 · {{ crossDimLabel }}</span>
            <span class="ctp-sub">堆叠展示各级别分布，支持自定义维度切换</span>
          </div>
          <div ref="chCross" class="cross-chart crt-stage"></div>
        </div>
      </div>
    </div>
  </div>

  <!-- AI洞察 -->
  <div class="insight-block">
    <div class="insight-t"><i class="it-ic"></i>AI 深度数据分析洞察</div>
    <div class="insight-list">
      <div v-for="(ins,i) in insights" :key="i" class="ins-row" :class="ins.type">
        <div class="ins-i">{{ ins.icon }}</div>
        <div class="ins-b">
          <div class="ins-title">{{ ins.title }}</div>
          <div class="ins-content" v-html="ins.content"></div>
        </div>
      </div>
    </div>
  </div>

  <!-- AI 统计报告对话框 -->
  <el-dialog v-model="reportVisible" title="AI 统计分析报告" width="640px" class="st-report-dlg" append-to-body>
    <div class="st-report" v-html="aiReport"></div>
    <template #footer>
      <el-button @click="reportVisible = false">关闭</el-button>
      <el-button type="primary" @click="exportReport"><el-icon><Download /></el-icon> 导出报告</el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from '@/utils/echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const period = ref('today')
const chTop10 = ref(null), chDur = ref(null), chCross = ref(null)
const chProf = ref(null), chLevel = ref(null), chStatus = ref(null)
let charts = []

// 图表面板 Tab 切换
const chartTab = ref('pie')
const chartTabs = [
  { key: 'pie', label: '分布分析', ic: '◉' },
  { key: 'bar', label: '排行统计', ic: '▤' },
  { key: 'cross', label: '交叉维度', ic: '⊞' }
]
const chartTabIndex = computed(() => chartTabs.findIndex(t => t.key === chartTab.value))

// 饼图页卡片数据（已内联到模板，保留供未来扩展）

const stats = computed(() => store.stats)
const cnt = (l) => store.unitAlarms(store.selectedUnitId).filter(a => a.l === l).length
const suppressed = computed(() => store.unitAlarms(store.selectedUnitId).filter(a => a.st === 'suppressed').length)
const filterCnt = computed(() => Math.floor(store.stats.total * 0.05 + Math.random() * 3))
const totalTimes = computed(() => 28 + Math.floor(Math.random() * 5))
const totalMins = computed(() => 196 + Math.floor(Math.random() * 20))

// ============ 环比/同比 + 交叉分析 + AI 报告 ============
const cmpMode = ref('hb')   // hb 环比 / tb 同比
const crossDim = ref('dept')
const reportVisible = ref(false)
const crossDimLabel = computed(() => ({ dept: '专业 × 级别', type: '类型 × 级别', status: '状态 × 级别' }[crossDim.value]))

// 上一周期（合成）基数
const prevFactor = computed(() => cmpMode.value === 'hb'
  ? { total: 0.92, unhandled: 1.06, handled: 0.88, filter: 0.84 }
  : { total: 0.74, unhandled: 1.18, handled: 0.7, filter: 0.62 })
const deltaOf = (cur, key) => {
  const f = prevFactor.value[key]
  const prev = Math.max(1, Math.round(cur * f))
  const pct = Math.round((cur - prev) / prev * 100)
  return { pct: Math.abs(pct), dir: cur >= prev ? 'up' : 'down' }
}
const deltas = computed(() => ({
  total: deltaOf(stats.value.total, 'total'),
  unhandled: deltaOf(stats.value.unhandled, 'unhandled'),
  handled: deltaOf(stats.value.handled, 'handled'),
  filter: deltaOf(filterCnt.value, 'filter')
}))

// 交叉分析数据
const crossData = computed(() => {
  const arr = store.unitAlarms(store.selectedUnitId)
  let cats
  if (crossDim.value === 'dept') cats = ['锅炉', '汽轮机', '辅网', '电气', '热工']
  else if (crossDim.value === 'type') cats = [...new Set(arr.map(a => a.type))].slice(0, 6)
  else cats = ['未处理', '已处置', '抑制']
  const grpOf = (a) => crossDim.value === 'dept' ? a.dept
    : crossDim.value === 'type' ? a.type
    : (a.st === 'unhandled' ? '未处理' : a.st === 'suppressed' ? '抑制' : '已处置')
  const l1 = [], l2 = [], l3 = []
  cats.forEach(c => {
    const g = arr.filter(a => grpOf(a) === c)
    l1.push(g.filter(a => a.l === 1).length)
    l2.push(g.filter(a => a.l === 2).length)
    l3.push(g.filter(a => a.l === 3).length)
  })
  return { cats, l1, l2, l3 }
})

// AI 报告
const aiReport = computed(() => {
  const arr = store.unitAlarms(store.selectedUnitId)
  const p = profCnt.value
  const maxProf = Object.entries(p).sort((a, b) => b[1] - a[1])[0]
  const periodTxt = { today: '今日', week: '本周', month: '本月' }[period.value]
  const cmpTxt = cmpMode.value === 'hb' ? '环比上一周期' : '同比去年同期'
  return `<h3 style="color:#3eaaff;margin:0 0 10px">${store.selectedUnit.name} · ${periodTxt}报警统计分析报告</h3>
<p><b>一、总体概况</b><br>${periodTxt}共产生报警 <b style="color:#5fb3ff">${stats.value.total}</b> 条，其中未处理 <b style="color:#ef4444">${stats.value.unhandled}</b> 条、已闭环 <b style="color:#34d399">${stats.value.handled}</b> 条。${cmpTxt}变化：总量 <b>${deltas.value.total.dir === 'up' ? '上升' : '下降'} ${deltas.value.total.pct}%</b>，未处理 <b>${deltas.value.unhandled.dir === 'up' ? '上升' : '下降'} ${deltas.value.unhandled.pct}%</b>。</p>
<p><b>二、专业分布</b><br>报警集中度最高的专业为 <b style="color:#fbbf24">${maxProf[0]}</b>（${maxProf[1]} 条，占比 ${Math.round(maxProf[1] / stats.value.total * 100)}%），建议作为重点治理对象。</p>
<p><b>三、级别结构</b><br>一级 ${cnt(1)} 条、二级 ${cnt(2)} 条、智能预警 ${cnt(3)} 条。一级报警需在 ${store.alarmLevels[1].avgTime} 分钟内响应，避免升级。</p>
<p><b>四、趋势研判</b><br>AI 模型预测下周报警总量约 <b>${(stats.value.total * (0.9 + 0.2 * Math.random())).toFixed(0)}</b> 条。建议加强 ${maxProf[0]} 设备巡检频次，并对连续触发 2 次以上测点配置自动联动处置。</p>
<p><b>五、处置建议</b><br>① 对未处理一级报警优先派单；② 对 ${maxProf[0]} 专业开展专项排查；③ 复核智能预警阈值，降低误报率（本期 AI 已过滤 ${filterCnt.value} 条疑似误报）。</p>`
})
const openReport = () => { reportVisible.value = true }
const exportReport = () => {
  const text = aiReport.value.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').replace(/\n/g, '\n')
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `${store.selectedUnit.name}_${period.value}_报警分析报告.txt`
  a.click()
  URL.revokeObjectURL(a.href)
}

const profCnt = computed(() => {
  const c = { '锅炉': 0, '汽轮机': 0, '辅网': 0, '电气': 0, '热工': 0 }
  store.unitAlarms(store.selectedUnitId).forEach(a => { c[a.dept] = (c[a.dept] || 0) + 1 })
  return c
})

// 占比指标 — 复用全域大屏底部卡片设计
const ratioMetrics = computed(() => {
  const arr = store.unitAlarms(store.selectedUnitId)
  const total = arr.length || 1
  const devs = store.unitDevices(store.selectedUnitId)
  const abnormal = devs.filter(d => d.health < 80).length
  const l1 = arr.filter(a => a.l === 1).length
  const l2 = arr.filter(a => a.l === 2).length
  const l3 = arr.filter(a => a.l === 3).length
  const handled = arr.filter(a => a.st !== 'unhandled').length
  const avgH = devs.length ? (devs.reduce((s,d)=>s+d.health,0)/devs.length).toFixed(1) : '0'
  return [
    { k:'设备异常', v:abnormal, u:'台', c:'#f87171', p:Math.round(abnormal/Math.max(1,devs.length)*100), s:'健康度 < 80', tag:'需关注', cl:'danger' },
    { k:'一级告警', v:l1, u:'条', c:'#f87171', p:Math.round(l1/total*100), s:`占总量 ${Math.round(l1/total*100)}%`, tag:'紧急', cl:'danger' },
    { k:'二级告警', v:l2, u:'条', c:'#fbbf24', p:Math.round(l2/total*100), s:`占总量 ${Math.round(l2/total*100)}%`, tag:'关注', cl:'warn' },
    { k:'智能预警', v:l3, u:'条', c:'#22d3ee', p:Math.round(l3/total*100), s:`占总量 ${Math.round(l3/total*100)}%`, tag:'AI', cl:'info' },
    { k:'平均健康度', v:avgH, u:'%', c:Number(avgH)>=90?'#34d399':Number(avgH)>=80?'#fbbf24':'#f87171', p:Number(avgH), s:Number(avgH)>=90?'运行良好':'需关注', tag:Number(avgH)>=90?'正常':'偏低', cl:Number(avgH)>=90?'ok':'warn' }
  ]
})

const insights = computed(() => {
  const out = [], arr = store.unitAlarms(store.selectedUnitId), total = arr.length
  if (total === 0) return [{ type:'ok', icon:'✅', title:'当前无报警', content:'系统运行平稳。' }]
  const p = profCnt.value
  const maxProf = Object.entries(p).sort((a,b)=>b[1]-a[1])[0]
  const ratio = ((maxProf[1]/total)*100).toFixed(0)
  out.push({ type:'info', icon:'📊', title:`报警集中度分析`, content:`本期报警主要集中在<strong style="color:#fbbf24">${maxProf[0]}</strong>专业（${ratio}%）。` })
  const l1 = arr.filter(a=>a.l===1)
  if(l1.length>0) out.push({ type:'danger', icon:'🚨', title:'一级报警处置分析', content:`当前有 <strong style="color:#ef4444">${l1.length}</strong> 条一级报警，平均响应时长 <strong>${store.alarmLevels[1].avgTime}分钟</strong>。` })
  const lowH = store.unitDevices(store.selectedUnitId).filter(d=>d.health<85)
  if(lowH.length>0) out.push({ type:'warn', icon:'⚠️', title:'设备健康度预警', content:`${lowH.length} 台设备健康度低于85，建议安排预防性检修。` })
  out.push({ type:'info', icon:'🎯', title:'智能过滤效果', content:`AI 模式识别自动过滤了 <strong style="color:#34d399">${filterCnt.value} 条</strong> 疑似误报。` })
  out.push({ type:'info', icon:'📈', title:'趋势预测', content:`下周报警总量预计 <strong>${(total*(0.9+Math.random()*0.2)).toFixed(0)} 条</strong>。` })
  out.push({ type:'ok', icon:'💡', title:'AI 处置建议', content:`建议加强 ${maxProf[0]} 设备巡检频次；对连续触发2次以上测点配置自动联动。` })
  return out
})

// 图表初始化（v-show 方案：所有容器始终挂载，只初始化一次）
const rz = () => charts.forEach(c => { try { c.resize() } catch(e){} })

const initCharts = () => {
  // 销毁旧实例
  charts.forEach(c => { try { c.dispose() } catch(e){} })
  charts = []
  const p = profCnt.value

  // 低饱和告警配色（延续系统告警色规范）
  const LOW = {
    red:   ['#e07a6b', '#c96055'],   // 一级 / 未处理 / 紧急
    orange:['#e0a85c', '#cc8f44'],   // 二级 / 关注
    yellow:['#d9c25a', '#bba53f'],   // 预警 / 提示（暗黄）
    cyan:  ['#5aa6c4', '#488aa6'],   // 正常 / 提示 / 智能
    green: ['#5bb88a', '#47a173'],   // 已处理 / 正常
    gray:  ['#7d8da0', '#63718a'],   // 抑制
    amber: ['#d9b06a', '#c4984f'],   // 锅炉
    blue:  ['#6a9ec4', '#527fa0'],   // 汽机
  }
  const grad = (a, b) => new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: a }, { offset: 1, color: b }])

  // ── 工业级 HUD 科幻仪表盘环形图（离散发光块栅格环）──
  // 核心：纯黑高对比底 / 环体=独立发光小色块(带间隙) / 锐利内发光+明暗渐变 / 外圈主副刻度 / 中心等宽粗体发光数字
  const hudRingOpt = (data, total, totalLabel) => {
    const sum = data.reduce((s, d) => s + d.value, 0) || 1
    const nameTotals = {}
    data.forEach(d => { nameTotals[d.name] = d.value })

    // 离散发光块：将整圈栅格化为 BLOCKS 个独立小色块，按数值比例分配给各维度，维度间插入空块做分隔
    const BLOCKS = 64
    const cellCat = []
    data.forEach(d => {
      const span = Math.max(1, Math.round(d.value / sum * BLOCKS))
      for (let k = 0; k < span; k++) cellCat.push(d)
      cellCat.push(null) // 维度间空块分隔
    })
    while (cellCat.length < BLOCKS) cellCat.push(null)

    const blockData = cellCat.map(c => {
      if (!c) { // 空块：极暗轨道，无发光
        return { value: 1, name: '', silent: true, itemStyle: { color: 'rgba(120,160,200,0.05)', borderColor: 'transparent', borderWidth: 0, shadowBlur: 0 } }
      }
      const base = c.color[0], dark = c.color[1]
      return {
        value: 1, name: c.name,
        itemStyle: {
          // 明暗渐变（左上亮→右下暗）制造体积感
          color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: base }, { offset: 1, color: dark }]),
          shadowBlur: 10, shadowColor: base + '55', shadowOffsetY: 1,   // 霓虹内发光
          borderColor: 'rgba(2,6,12,0.9)', borderWidth: 1, borderRadius: 1.5,
          opacity: 0.96
        },
        emphasis: { itemStyle: { shadowBlur: 22, shadowColor: base + 'aa', opacity: 1 } }
      }
    })

    // 外圈细密主/副刻度线（机械仪表盘纹理）
    const TICKS = 100
    const tickData = Array.from({ length: TICKS }, (_, i) => ({
      value: 1, name: '',
      itemStyle: {
        color: i % 5 === 0 ? 'rgba(120,185,220,0.5)' : 'rgba(120,185,220,0.13)', // 主刻度更亮
        borderColor: 'transparent', borderWidth: 0
      }
    }))

    // 聚合维度（仅用于外侧纤细引线标签，扇区透明不可见）
    const labelSeries = {
      type: 'pie', radius: ['46%', '66%'], center: ['50%', '45%'],
      silent: true, animation: false, tooltip: { show: false },
      label: {
        color: '#c2d6ea', fontSize: 11, lineHeight: 15, formatter: '{b}\n{c}',
        fontFamily: '"SF Mono","Consolas",monospace',
        textShadowColor: 'rgba(0,0,0,0.65)', textShadowBlur: 3
      },
      labelLine: { length: 12, length2: 12, lineStyle: { color: 'rgba(150,180,215,0.2)', width: 1 } },
      itemStyle: { opacity: 0, color: 'transparent', borderColor: 'transparent', borderWidth: 0 },
      emphasis: { disabled: true },
      data: data.map(d => ({ name: d.name, value: d.value, label: { color: d.color[0] } })),
      z: 1
    }

    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        formatter: (p) => p.name ? `${p.name}：${nameTotals[p.name] ?? 0} 条（${((nameTotals[p.name] ?? 0) / sum * 100).toFixed(1)}%）` : '',
        backgroundColor: 'rgba(2,6,14,0.97)',
        borderColor: 'rgba(120,185,220,0.3)',
        borderWidth: 1,
        textStyle: { color: '#c2d4e6', fontSize: 11 },
        padding: [7, 11],
        extraCssText: 'box-shadow: 0 0 14px rgba(120,185,220,0.2);'
      },
      legend: {
        bottom: 0, left: 'center', orient: 'horizontal',
        data: data.map(d => d.name),
        itemWidth: 8, itemHeight: 8, itemGap: 14, icon: 'circle',
        textStyle: { color: '#8a9fb5', fontSize: 10, fontFamily: '"SF Mono","Consolas",monospace' }
      },
      series: [
        // Series 0：外圈主/副刻度线
        {
          type: 'pie', radius: ['78%', '84%'], center: ['50%', '45%'],
          silent: true, animation: false, startAngle: 90,
          itemStyle: { borderColor: 'rgba(2,6,14,0.95)', borderWidth: 1 },
          label: { show: false }, labelLine: { show: false },
          data: tickData, padAngle: 0.6
        },
        // Series 1：离散发光块主环（栅格化刻度块）
        {
          type: 'pie', radius: ['44%', '66%'], center: ['50%', '45%'],
          silent: false, padAngle: 0.035, roundCap: true,
          label: { show: false }, labelLine: { show: false },
          data: blockData,
          z: 3
        },
        // Series 2：外侧纤细引线标签（透明扇区）
        labelSeries
      ],
      animationDuration: 1000,
      animationEasing: 'cubicOut'
    }
  }

  // 环形图（始终初始化，v-show 控制显示）
  if (chProf.value) {
    const pd = [{ value: p['锅炉'], name: '锅炉', color: LOW.amber }, { value: p['汽机'], name: '汽机', color: LOW.blue }, { value: p['辅网'], name: '辅网', color: LOW.green }].filter(d => d.value > 0)
    const c1 = echarts.init(chProf.value)
    c1.setOption(hudRingOpt(pd, pd.reduce((s, d) => s + d.value, 0), '设备总数'))
    charts.push(c1)
  }
  if (chLevel.value) {
    const ld = [{ value: cnt(1), name: '一级', color: LOW.red }, { value: cnt(2), name: '二级', color: LOW.orange }, { value: cnt(3), name: '预警', color: LOW.cyan }].filter(d => d.value > 0)
    const c2 = echarts.init(chLevel.value)
    c2.setOption(hudRingOpt(ld, stats.value.total, '报警总数'))
    charts.push(c2)
  }
  if (chStatus.value) {
    const sd = [{ value: stats.value.handled, name: '已处理', color: LOW.green }, { value: stats.value.unhandled, name: '未处理', color: LOW.red }, { value: suppressed.value, name: '抑制', color: LOW.gray }]
    const c3 = echarts.init(chStatus.value)
    c3.setOption(hudRingOpt(sd, sd.reduce((s, d) => s + d.value, 0), '处置总数'))
    charts.push(c3)
  }

  // ── 横向条形图：工业 LED 光带刻度条（与环形图 HUD 仪表盘视觉统一）──
  // 核心：LED光带渐变+锐利内发光 / 精密机械刻度线 / 等宽科技字体数值标签 / 纯深空黑底
  const hbarOpt = (items, unit) => {
    const sorted = [...items].sort((a, b) => b[1] - a[1])
    const names = sorted.map(d => d[0])
    const vals = sorted.map(d => d[1])
    const maxV = Math.max(...vals, 1)
    // 按告警等级分色：高值→红(一级), 中→橙(二级), 低→青蓝(预警)
    const levelOf = (v) => { const r = v / maxV; return r > 0.66 ? 'red' : r > 0.33 ? 'orange' : 'cyan' }

    return {
      backgroundColor: 'transparent',
      grid: { left: 120, right: 52, top: 8, bottom: 22 },
      tooltip: {
        trigger: 'axis', axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(90,166,196,0.04)' } },
        backgroundColor: 'rgba(2,6,14,0.97)',
        borderColor: 'rgba(120,185,220,0.25)', borderWidth: 1,
        textStyle: { color: '#c2d4e6', fontSize: 11, fontFamily: '"SF Mono","Consolas",monospace' },
        padding: [7, 11],
        extraCssText: 'box-shadow: 0 0 12px rgba(120,185,220,0.15);'
      },
      xAxis: {
        type: 'value', name: unit,
        nameTextStyle: { color: '#5a7a98', fontSize: 10, fontFamily: '"SF Mono","Consolas",monospace' },
        axisLabel: { color: '#5a7894', fontSize: 10, fontFamily: '"SF Mono","Consolas",monospace' },
        axisLine: { lineStyle: { color: 'rgba(120,185,220,0.12)' } },
        // 精密机械刻度：长短交替，模拟仪表盘纹理
        axisTick: {
          show: true, inside: true, length: 3,
          lineStyle: { color: 'rgba(120,185,220,0.18)' }
        },
        splitLine: {
          // 极淡横向刻度线（主刻度稍亮，副刻度更淡）
          interval: function(i) { return i % 2 === 0; },  // 只画偶数位
          lineStyle: { color: 'rgba(120,185,220,0.06)', width: 1 }
        },
        splitArea: { show: false }
      },
      yAxis: {
        type: 'category', data: names, inverse: true,
        axisLabel: {
          color: '#9ab8d2', fontSize: 10.5,
          fontFamily: '"SF Mono","Consolas",monospace',
          margin: 10
        },
        axisLine: { show: true, lineStyle: { color: 'rgba(120,185,220,0.08)' } },
        axisTick: { show: false }
      },
      series: [{
        type: 'bar',
        data: vals.map(v => {
          const col = LOW[levelOf(v)]
          return {
            value: v,
            itemStyle: {
              // LED 光带：左亮右暗渐变，模拟霓虹灯管从根部到末端的衰减
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: col[0] },
                { offset: 0.65, color: col[1] },
                { offset: 1, color: col[1] + '55' }  // 尾部暗化
              ]),
              borderRadius: [0, 4, 4, 0],
              // 锐利内发光——用自身颜色做霓虹辉光
              shadowBlur: 14,
              shadowColor: col[0] + '44',
              shadowOffsetX: 0,
              shadowOffsetY: 1,
              // 微弱外边框区分条形边界
              borderColor: 'rgba(2,6,12,0.75)',
              borderWidth: 1
            },
            label: {
              show: true, position: 'right',
              color: col[0], fontSize: 11, fontWeight: 700,
              fontFamily: '"Orbitron","SF Mono","Consolas",monospace',
              formatter: '{c}',
              textShadowColor: col[0] + '33',
              textShadowBlur: 6,
              textShadowOffsetY: 1,
              offset: [4, 0]
            },
            emphasis: {
              itemStyle: {
                shadowBlur: 24,
                shadowColor: col[0] + '88'
              }
            }
          }
        }),
        barWidth: '46%',
        barCategoryGap: '40%',
        animationDuration: 1000,
        animationEasing: 'cubicOut',
        animationDelay: (idx) => idx * 60
      }]
    }
  }

  const top10Data=[['A引风机轴承温度',3],['主汽温度',4],['润滑油压力',3],['NOx浓度',5],['A磨煤机振动',2],['除氧器水位',3],['D磨煤机电流',2],['给水泵B效率',1],['冷却塔水温',2],['高加上端差',1]]
  const durData=[['A引风机轴承',48],['润滑油压力',35],['D磨煤机',28],['主汽温度',22],['A磨煤机',18],['除氧器水位',15],['NOx浓度',12],['给水泵B',8],['冷却塔',5],['高加端差',3]]

  // 柱状图（始终初始化）
  if (chTop10.value) {
    const c4 = echarts.init(chTop10.value)
    c4.setOption(hbarOpt(top10Data, '次'))
    charts.push(c4)
  }
  if (chDur.value) {
    const c5 = echarts.init(chDur.value)
    c5.setOption(hbarOpt(durData, '分钟'))
    charts.push(c5)
  }

  // ── 交叉分析：CRT 示波器 HUD（分组并列空心线框柱，左右并排）──
  // ⚠️ 绝对禁止堆叠！同一X分类下4根空心方框左右并排紧贴
  // 柱体：独立空心矩形 / 透明无填充 / 彩色霓虹边框 / 直角无圆角 / 向上生长
  if (chCross.value) {
    const cd = crossData.value
    const devs = store.unitDevices(store.selectedUnitId)
    const l0 = cd.cats.map((c, i) => {
      if (crossDim.value === 'dept') {
        // 真实健康测点数 = 该专业设备总数 − 当前报警数（数据来自 store.devices）
        const total = devs.filter(d => d.dept === c).length
        const alarmsInCat = cd.l1[i] + cd.l2[i] + cd.l3[i]
        return Math.max(total - alarmsInCat, 1)
      }
      // type/status 维度设备无对应字段，按报警规模推算基线（占位）
      return Math.max(cd.l1[i] + cd.l2[i] + cd.l3[i], 1) * 3 + 6
    })
    const maxV = Math.max(...l0, ...cd.l3, ...cd.l2, ...cd.l1, 1)
    const yMax = Math.ceil(maxV * 1.12)

    // 独立空心霓虹方框柱：transparent 填充 + 彩色边框 + 内发光，直角无圆角
    // 4 根柱子在同一 X 分类下左右并列紧贴（barGap 控制间距）
    const hollowBar = (name, data, col) => ({
      name,
      type: 'bar',
      data,
      barWidth: '18%',          // 每根柱占带宽的 18%，4根共 72% + 间隙
      itemStyle: {
        color: 'transparent',   // 完全镂空无填充
        borderColor: col[0],     // 霓虹轮廓线框
        borderWidth: 1.8,
        borderRadius: 0,         // 直角！无圆角
        shadowBlur: 10,
        shadowColor: col[0] + '55'
      },
      emphasis: {
        itemStyle: { borderWidth: 2.6, shadowBlur: 18, shadowColor: col[0] + '88' }
      },
      label: { show: false }
    })

    const c6 = echarts.init(chCross.value)
    c6.setOption({
      backgroundColor: 'transparent',
      grid: { left: 48, right: 18, top: 44, bottom: 30 },

      legend: {
        data: ['正常/提示', '预警', '二级', '一级'],
        textStyle: { color: '#6a85a0', fontSize: 10, fontFamily: '"SF Mono","Consolas",monospace' },
        top: 8, right: 10,
        icon: 'line', itemWidth: 16, itemHeight: 2, itemGap: 14,
        lineStyle: { width: 2 }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(90,166,196,0.03)' } },
        backgroundColor: 'rgba(2,6,14,0.97)',
        borderColor: 'rgba(90,166,196,0.28)', borderWidth: 1,
        textStyle: { color: '#b8d0e8', fontSize: 11, fontFamily: '"SF Mono","Consolas",monospace' },
        padding: [7, 11],
        extraCssText: 'box-shadow: 0 0 14px rgba(90,166,196,0.12);'
      },
      xAxis: {
        type: 'category', data: cd.cats, boundaryGap: true,
        axisLabel: {
          color: '#5a7894', fontSize: 10.5,
          fontFamily: '"SF Mono","Consolas",monospace', margin: 10
        },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value', min: 0, max: yMax,
        axisLabel: {
          color: '#4a6580', fontSize: 10,
          fontFamily: '"Orbitron","SF Mono","Consolas",monospace',
          textShadowColor: 'rgba(90,166,196,0.15)', textShadowBlur: 2
        },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }, splitArea: { show: false }
      },
      series: [
        hollowBar('正常/提示', l0, LOW.cyan),     // 青蓝
        hollowBar('预警', cd.l3, LOW.yellow),       // 浅黄
        hollowBar('二级', cd.l2, LOW.orange),       // 橙黄
        hollowBar('一级', cd.l1, LOW.red)           // 红色
      ]
    })
    charts.push(c6)
  }
}

// Tab 切换只做 resize（图表已全部初始化）
const switchChartTab = (key) => {
  if (chartTab.value === key) return
  chartTab.value = key
  nextTick(() => rz())
}

// 数据变化时重新初始化所有图表
watch(()=>[store.selectedUnitId, period.value, store.alarms.length, cmpMode.value, crossDim.value], () => nextTick(() => initCharts()))

onMounted(() => {
  nextTick(() => initCharts())
  window.addEventListener('resize', rz)
})
onUnmounted(() => {
  window.removeEventListener('resize', rz)
  charts.forEach(c => { try { c.dispose() } catch(e){} })
  charts = []
})
</script>

<style scoped>
.stats-wrap { display:flex; flex-direction:column; gap:6px; height:100%; overflow:hidden; }

/* 时间切换 — 紧凑 */
.period-header { flex-shrink:0; background:linear-gradient(180deg, rgba(4,10,20,0.315), rgba(3,8,16,0.35)); border:1px solid rgba(62,170,255,0.1); clip-path: polygon(0 0, calc(100% - 6px) 0, 100% 6px, 100% 100%, 6px 100%, 0 calc(100% - 6px)); padding:2px 12px; }
.period-tabs { --el-bg-color: transparent; }
:deep(.period-tabs .el-tabs__nav-wrap::after) { background-color: transparent; }
:deep(.period-tabs .el-tabs__item) { color:#8fb0cf; font-size:12px; padding:0 12px; height:28px; line-height:28px; }
:deep(.period-tabs .el-tabs__item.is-active) { color:#3eaaff; font-weight:600; }
:deep(.period-tabs .el-tabs__active-bar) { background:#3eaaff; box-shadow:0 0 6px rgba(62,170,255,0.4); }

/* 控制条 — 单行紧凑 */
.st-ctrls { flex-shrink:0; display:flex; align-items:center; gap:12px; flex-wrap:wrap; padding:5px 10px; background:linear-gradient(180deg, rgba(4,10,20,0.315), rgba(3,8,15,0.35)); border:1px solid rgba(62,170,255,0.1); clip-path: polygon(0 0, calc(100% - 6px) 0, 100% 6px, 100% 100%, 6px 100%, 0 calc(100% - 6px)); }
.st-ctrl { display:flex; align-items:center; gap:4px; }
.st-ctrl label { font-size:10px; color:#9fb6cf; white-space:nowrap; }
.st-ai-btn { background:linear-gradient(135deg, #3eaaff, #22d3ee); border:none; height:24px; font-size:11px; }
.st-ctrl-tip { margin-left:auto; font-size:10px; color:#5a7894; }
/* 统计概览 — 超紧凑KPI条 */
.stats-overview { flex-shrink:0; display:flex; gap:6px; }
.sc {
  display:flex; align-items:center; gap:5px;
  padding: 4px 10px;
  background:linear-gradient(180deg, rgba(4,10,20,0.385), rgba(3,8,15,0.42));
  border:1px solid rgba(62,170,255,0.1);
  clip-path: polygon(0 0, calc(100% - 5px) 0, 100% 5px, 100% 100%, 5px 100%, 0 calc(100% - 5px));
  transition:all 0.2s;
  flex:1;
}
.sc:hover { border-color:rgba(62,170,255,0.25); }
.sc-lbl { font-size:10px; color:#8fb0cf; letter-spacing:0.3px; white-space:nowrap; }
.sc-val { font-size:18px; font-weight:700; font-family:"SF Mono","Consolas",monospace; line-height:1; text-shadow:0 1px 4px rgba(0,0,0,0.3); }
.sc-sub { font-size:10px; color:#6a8caa; white-space:nowrap; opacity:0.8; }
.sc-val.in { color:#5fb3ff; }
.sc-val.ok { color:#34d399; }
.sc-val.dg { color:#ef4444; }
.sc-val.cy { color:#22d3ee; }

/* 占比指标行 — 紧凑 */
.ratio-row {
  flex-shrink:0;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}
.ratio-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 10px;
  background: linear-gradient(180deg, rgba(4,10,20,0.385), rgba(3,8,16,0.42));
  border: 1px solid rgba(62,170,255,0.1);
  clip-path: polygon(0 0, calc(100% - 5px) 0, 100% 5px, 100% 100%, 5px 100%, 0 calc(100% - 5px));
  overflow: hidden;
}
.ratio-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--rc, #3eaaff);
  opacity: 0.8;
}
.ratio-card:hover {
  border-color: rgba(62,170,255,0.28);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,10,30,0.4);
}
.ratio-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ratio-lbl {
  font-size: 10px;
  color: #8fb0cf;
  letter-spacing: 0.3px;
  font-weight: 500;
}
.ratio-tag {
  font-size: 9px;
  font-weight: 600;
  padding: 0px 5px;
  border-radius: 2px;
}
.ratio-tag.danger { background: rgba(248,113,113,0.12); color: #f87171; border: 1px solid rgba(248,113,113,0.25); }
.ratio-tag.warn { background: rgba(251,191,36,0.12); color: #fbbf24; border: 1px solid rgba(251,191,36,0.25); }
.ratio-tag.info { background: rgba(34,211,238,0.12); color: #22d3ee; border: 1px solid rgba(34,211,238,0.25); }
.ratio-tag.ok { background: rgba(52,211,153,0.12); color: #34d399; border: 1px solid rgba(52,211,153,0.25); }
.ratio-val {
  font-size: 20px;
  font-weight: 700;
  font-family: "SF Mono","Consolas",monospace;
  line-height: 1.1;
}
.ratio-u {
  font-size: 11px;
  margin-left: 1px;
  opacity: 0.7;
}
.ratio-bar-track {
  height: 3px;
  background: rgba(62,170,255,0.08);
  border-radius: 2px;
  overflow: hidden;
}
.ratio-bar-fill {
  display: block;
  height: 100%;
  border-radius: 2px;
  transition: width 0.6s ease;
  box-shadow: 0 0 4px currentColor;
}
.ratio-sub {
  font-size: 9px;
  color:#6a8caa;
}

/* 图表面板 — Tab 页面置换 */
.chart-panel {
  flex: 1; min-height: 0;
  display: flex; flex-direction: column;
  background: linear-gradient(180deg, rgba(4,10,21,0.28), rgba(3,8,15,0.315));
  border: 1px solid rgba(62,170,255,0.1);
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
  overflow: hidden;
}
.chart-panel-tabs {
  flex-shrink: 0; display: flex; gap: 0;
  position: relative;   padding: 1px 8px 0;
  border-bottom: 1px solid rgba(62,170,255,0.1); background: rgba(3,7,14,0.28);
}
.cpt-tab {
  position: relative; z-index: 1;
  padding: 6px 14px; font-size:12px; font-weight:500;
  color: #7a9bb8; cursor: pointer; border: none; background: none;
  transition: color 0.25s; letter-spacing: 0.3px;
}
.cpt-tab:hover { color: #b8d4ed; }
.cpt-tab.active { color: #d4ecff; }
.cpt-ic { margin-right: 5px; opacity: 0.7; }
.cpt-tab.active .cpt-ic { opacity: 1; }
.cpt-indicator {
  position: absolute; bottom: 0; left: 8px;
  width: calc((100% - 16px) / 3); height: 2px;
  background: linear-gradient(90deg, transparent, #3eaaff, transparent);
  box-shadow: 0 0 8px rgba(62,170,255,0.5);
  transition: transform 0.35s cubic-bezier(0.4,0,0.2,1);
}
.chart-panel-body {
  flex: 1; min-height: 0;
  display: flex; flex-direction: column;
  overflow: hidden;
}
.cp-page {
  display: grid; gap: 8px;
  padding: 6px 10px;
  min-height: 0;
  flex: 1;
}
.cp-pie { grid-template-columns: repeat(3, 1fr); }
.cp-bar { grid-template-columns: 1fr 1fr; }
.cp-cross { grid-template-columns: 1fr; }
.cp-page > * { min-height: 0; }

/* 饼图/柱状图/交叉分析内部样式 */
.pie-col { display:flex; flex-direction:column; min-height:0; position:relative; }
.pie-col::before, .pie-col::after {
  content:''; position:absolute; width:14px; height:14px;
  pointer-events:none; z-index:2;
}
/* 左上角 ┌ 科技线条 */
.pie-col::before {
  top:-1px; left:-1px;
  border-top: 1.5px solid rgba(90,166,196,0.35);
  border-left: 1.5px solid rgba(90,166,196,0.35);
  box-shadow: 0 0 6px rgba(90,166,196,0.12);
}
/* 右下角 ┘ 科技线条 */
.pie-col::after {
  bottom:-1px; right:-1px;
  border-bottom: 1.5px solid rgba(90,166,196,0.35);
  border-right: 1.5px solid rgba(90,166,196,0.35);
  box-shadow: 0 0 6px rgba(90,166,196,0.12);
}
.pie-chart {
  position:absolute; inset:0; z-index:1;
}

/* ── HUD 舞台容器（相对定位，承载canvas+四角卡片+中心牌）── */
.hud-stage {
  position:relative; flex:1; min-height:0; height:200px;
  border-radius:10px; overflow:hidden;
  /* 纯黑高对比舞台，让霓虹发光凸显 */
  background:
    radial-gradient(circle at 50% 46%, #070b13 0%, #03060d 58%, #000308 100%);
  box-shadow: inset 0 0 22px rgba(0,0,0,0.7), inset 0 0 1px rgba(120,185,220,0.12);
}

/* ── 四角八角形状态卡片（双层边框+青蓝霓虹内发光）── */
.hud-corner {
  position:absolute; width:64px; height:48px;
  display:flex; align-items:center; justify-content:center;
  z-index:10;
}
.hud-corner.tl { top:6px; left:6px; }
.hud-corner.tr { top:6px; right:6px; }
.hud-corner.bl { bottom:28px; left:6px; }   /* 底部留图例空间 */
.hud-corner.br { bottom:28px; right:6px; }

/* 双层八角形边框：外层粗框 + 内层细高光 */
.hud-corner-inner {
  width:100%; height:100%;
  display:flex; flex-direction:column; align-items:center; justify-content:center;
  gap:1px;
  clip-path: polygon(20% 0%, 80% 0%, 100% 20%, 100% 80%, 80% 100%, 20% 100%, 0% 80%, 0% 20%);
  background:
    linear-gradient(135deg, rgba(8,18,36,0.88) 0%, rgba(4,12,24,0.92) 100%);
  border: none;
  box-shadow:
    0 0 10px rgba(90,166,196,0.15),
    inset 0 0 8px rgba(90,166,196,0.08),
    inset 0 0 1px rgba(120,200,240,0.25);
}
/* 外层八角描边（双层效果） */
.hud-corner::before {
  content:''; position:absolute; inset:-1.5px;
  clip-path: polygon(20% 0%, 80% 0%, 100% 20%, 100% 80%, 80% 100%, 20% 100%, 0% 80%, 0% 20%);
  border:none;
  background:transparent;
  box-shadow: 0 0 6px rgba(90,166,196,0.25), inset 0 0 3px rgba(90,166,196,0.06);
  pointer-events:none;
}

.hud-corner-inner em {
  font-style:normal; font-size:9px; color:#7a96b2; letter-spacing:0.5px;
  font-family:"SF Mono","Consolas",monospace;
}
.hud-corner-inner strong {
  font-size:16px; font-weight:700; color:#78c8f0;
  font-family:"Orbitron","SF Mono","Consolas",monospace;
  text-shadow: 0 0 10px rgba(120,200,240,0.45), 0 0 2px rgba(120,200,240,0.2);
  line-height:1;
}

/* ── 独立中心信息牌（深色半透明圆底板+冷青色等宽数字）── */
.hud-center-plate {
  position:absolute; left:50%; top:50%;
  transform:translate(-50%,-50%);
  z-index:10;
  display:flex; flex-direction:column; align-items:center; justify-content:center;
  gap:2px;
  width:72px; height:72px; border-radius:50%;
  background:
    radial-gradient(circle at 45% 35%, rgba(14,26,48,0.95) 0%, rgba(4,10,22,0.98) 65%, #02050a 100%);
  border:1px solid rgba(90,166,196,0.18);
  box-shadow:
    0 0 16px rgba(0,0,0,0.7),
    0 0 24px rgba(90,166,196,0.07),
    inset 0 0 12px rgba(0,0,0,0.5),
    inset 0 0 1px rgba(120,200,240,0.15);
}
.hcp-sub {
  font-size:9px; color:#6b85a2; letter-spacing:1px;
  font-family:"SF Mono","Consolas",monospace;
}
.hcp-val {
  font-size:26px; font-weight:700; color:#7dd4fc;
  font-family:"Orbitron","SF Mono","Consolas",monospace;
  line-height:1;
  text-shadow:
    0 0 12px rgba(125,212,252,0.55),
    0 0 3px rgba(125,212,252,0.25),
    0 1px 2px rgba(0,0,0,0.6);
}
.bar-col { display:flex; flex-direction:column; min-height:0; position:relative; }
.bar-col::before, .bar-col::after {
  content:''; position:absolute; width:14px; height:14px;
  pointer-events:none; z-index:2;
}
.bar-col::before {
  top:-1px; left:-1px;
  border-top: 1.5px solid rgba(90,166,196,0.35);
  border-left: 1.5px solid rgba(90,166,196,0.35);
  box-shadow: 0 0 6px rgba(90,166,196,0.12);
}
.bar-col::after {
  bottom:-1px; right:-1px;
  border-bottom: 1.5px solid rgba(90,166,196,0.35);
  border-right: 1.5px solid rgba(90,166,196,0.35);
  box-shadow: 0 0 6px rgba(90,166,196,0.12);
}
.bar-chart {
  flex:1; min-height:0; height:220px; border-radius:10px; overflow:hidden;
  /* 纯黑高对比舞台，与环形图统一 */
  background:
    radial-gradient(circle at 50% 45%, #070b13 0%, #03060d 55%, #000308 100%);
  box-shadow: inset 0 0 22px rgba(0,0,0,0.7), inset 0 0 1px rgba(120,185,220,0.12);
}
.cross-inner { display:flex; flex-direction:column; min-height:0; }
.cross-chart { flex:1; min-height:0; height:220px;
  /* 纯深黑底色——无蓝无青 */
  background-color: #000000;
  border-radius: 6px;
  overflow:hidden;
  box-shadow: inset 0 0 30px rgba(0,0,0,0.95), inset 0 0 1px rgba(90,166,196,0.04);
  /* 青蓝色虚线方格栅格——CRT 示波器屏幕网格（纤细、低对比度、虚线非实线） */
  background-image:
    repeating-linear-gradient(0deg,
      transparent 0px, transparent 19px,
      rgba(90,166,196,0.07) 19px, rgba(90,166,196,0.07) 20px),
    repeating-linear-gradient(90deg,
      transparent 0px, transparent 19px,
      rgba(90,166,196,0.07) 20px, rgba(90,166,196,0.07) 21px);
  background-size: 28px 28px;
}

/* ── CRT 切角科技边框标题牌 ─────────────────── */
.crt-title-plate {
  display:flex; align-items:center; gap:10px;
  padding: 7px 14px 7px 13px;
  margin-bottom: 6px;
  /* 双层嵌套切角 */
  clip-path: polygon(
    0 0, calc(100% - 9px) 0, 100% 9px,
    100% 100%, 9px 100%, 0 calc(100% - 9px)
  );
  background: linear-gradient(135deg, rgba(4,10,19,0.644), rgba(2,6,12,0.665));
  border: 1px solid rgba(90,166,196,0.2);
  position:relative;
}
/* 外层发光边框 */
.crt-title-plate::before {
  content:''; position:absolute; inset:-1px;
  clip-path: polygon(
    0 0, calc(100% - 9px) 0, 100% 9px,
    100% 100%, 9px 100%, 0 calc(100% - 9px)
  );
  border: 1px solid rgba(90,166,196,0.3);
  pointer-events:none;
  box-shadow: inset 0 0 8px rgba(90,166,196,0.06), 0 0 10px rgba(90,166,196,0.08);
  z-index:-1;
}
/* 左侧竖线装饰（青蓝内发光） */
.crt-title-plate::after {
  content:''; display:block; width:2.5px; height:16px; flex-shrink:0;
  background: linear-gradient(180deg, #5aa6c4, transparent 70%);
  box-shadow: 0 0 6px rgba(90,166,196,0.35);
  border-radius:1px;
}
.ctp-main {
  font-size: 11.5px; font-weight:700; color:#b8d4ec;
  font-family: '"SF Mono","Consolas",monospace';
  letter-spacing: 0.6px;
  text-shadow: 0 0 8px rgba(90,166,196,0.18);
}
.ctp-sub {
  font-size: 9px; color: #4a6580;
  letter-spacing: 0.3px;
}
.cd-sub { font-size:11px; color:#8fb0cf; }

/* Tab 切换动画（v-show 不需要 Transition，保留备用） */

/* AI洞察 — 紧凑 */
.insight-block {
  flex-shrink:0;
  max-height:100px;
  overflow:auto;
  background:linear-gradient(180deg, rgba(10,26,50,0.5), rgba(3,8,16,0.385));
  border:1px solid rgba(62,170,255,0.1);
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
  padding:14px 16px;
}
.insight-t { font-size:14px; font-weight:600; color:#5fb3ff; margin-bottom:12px; display:flex; align-items:center; gap:8px; }
.it-ic { width:0; height:0; border-left:7px solid #3eaaff; border-top:4px solid transparent; border-bottom:4px solid transparent; filter:drop-shadow(0 0 3px rgba(62,170,255,0.5)); }
.insight-list { display:flex; flex-direction:column; gap:8px; }
.ins-row { display:flex; gap:12px; padding:12px 14px; border-radius:5px; border-left:3px solid; transition:transform 0.15s; }
.ins-row:hover { transform:translateX(2px); }
.ins-row.danger { background:rgba(239,68,68,0.04); border-color:#ef4444; }
.ins-row.warn { background:rgba(245,158,11,0.04); border-color:#fbbf24; }
.ins-row.info { background:rgba(62,170,255,0.04); border-color:#3eaaff; }
.ins-row.ok { background:rgba(52,211,153,0.04); border-color:#34d399; }
.ins-i { font-size:20px; line-height:1.3; flex-shrink:0; }
.ins-b { flex:1; }
.ins-title { font-size:13px; font-weight:600; color:#e2e8f0; margin-bottom:4px; }
.ins-content { font-size:12px; color:#8fb0cf; line-height:1.65; }

/* 控制条 */
.st-ctrls { flex-shrink:0; display:flex; align-items:flex-end; gap:16px; flex-wrap:wrap; padding:10px 14px; background:linear-gradient(180deg, rgba(4,10,20,0.35), rgba(3,8,15,0.385)); border:1px solid rgba(62,170,255,0.12); border-radius:6px; }
.st-ctrl { display:flex; flex-direction:column; gap:5px; }
.st-ctrl label { font-size:11px; color:#9fb6cf; }
.st-ai-btn { align-self:flex-end; background:linear-gradient(135deg, #3eaaff, #22d3ee); border:none; }
.st-ctrl-tip { margin-left:auto; font-size:11px; color:#5a7894; align-self:flex-end; }

/* 环比/同比标记 */
.sc { position:relative; }
.sc-delta { font-size:10.5px; font-weight:600; margin-left:auto; display:flex; align-items:center; gap:3px; }
.sc-delta i { font-style:normal; font-size:9px; opacity:0.7; font-weight:400; }
.sc-delta.up { color:#ef4444; }
.sc-delta.down { color:#34d399; }

/* AI 报告弹窗 */
.st-report-dlg :deep(.el-dialog__body) { padding:10px 20px; }
.st-report { font-size:13px; color:#cbd5e1; line-height:1.8; max-height:60vh; overflow-y:auto; }
.st-report h3 { font-size:15px; }
.st-report p { margin:0 0 12px; }
.st-report b { color:#e2e8f0; }

@media(max-width:1200px){
  .stats-overview { grid-template-columns:repeat(2,1fr); }
  .ratio-row { grid-template-columns:repeat(3,1fr); }
  .pie-row { grid-template-columns:1fr 1fr; }
  .pie-row>.pie-col:last-child { grid-column:span 2; }
  .bar-row { grid-template-columns:1fr; }
}
@media(max-width:800px){
  .stats-overview { grid-template-columns:1fr 1fr; }
  .ratio-row { grid-template-columns:repeat(2,1fr); }
  .ratio-row>.ratio-card:last-child { grid-column:span 2; }
  .pie-row { grid-template-columns:1fr; }
}
</style>
