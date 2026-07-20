<template>
<div class="ai-page">
  <!-- 自定义顶栏（深色） -->
  <div class="ai-top">
    <div class="ai-top-l">
      <span class="ai-top-tt">智能预警与故障诊断</span>
    </div>
    <div class="ai-top-r">
      <span class="ai-status"><span class="ai-dot"></span>通讯正常</span>
      <span class="ai-user"><el-icon><UserFilled /></el-icon>{{ userStore.username }}</span>
    </div>
  </div>

  <div class="ai-body">
    <!-- 左侧对话历史 -->
    <div class="ai-sb">
      <div class="ai-sb-top">
        <el-button type="primary" class="ai-new-btn" @click="newChat">
          <el-icon><Plus /></el-icon>&nbsp;开启新对话
        </el-button>
      </div>
      <div class="ai-sb-search">
        <el-input v-model="histKw" placeholder="搜索历史..." clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="ai-sb-list">
        <template v-for="g in groupedHistory" :key="g.label">
          <div class="ai-sb-g">{{ g.label }}</div>
          <div v-for="c in g.items.filter(i => !histKw || i.title.includes(histKw))" :key="c.id"
               class="ai-sb-i" :class="{on: currentChat?.id === c.id}" @click="openChat(c)">
            <div class="ai-sb-i-t">{{ c.title }}</div>
          </div>
        </template>
      </div>
    </div>

    <!-- 主对话区 -->
    <div class="ai-main">
      <!-- 顶部对话标题 -->
      <div class="ai-mh">
        <div class="ai-mh-tt">{{ currentChat?.title || 'AI 运行智能体' }}</div>
        <div class="ai-mh-tag" v-if="currentChat">
          <el-tag size="small" type="primary" effect="dark">
            <el-icon><ChatDotRound /></el-icon>&nbsp;{{ currentChat.title }}
          </el-tag>
        </div>
        <div class="ai-mh-r">
          <el-button text @click="scrollToBottom">
            <el-icon><Download /></el-icon>最 新
          </el-button>
        </div>
      </div>

      <!-- 功能模块（8 个） -->
      <div class="ai-fns">
        <div v-for="(f, i) in fns" :key="i"
             class="ai-fn-i" :class="{on: mode === i}"
             @click="selectMode(i)">
          <div class="ai-fn-ic">{{ f.ic }}</div>
          <div class="ai-fn-lb">{{ f.lb }}</div>
        </div>
      </div>

      <!-- 模式提示 -->
      <div v-if="mode >= 0" class="ai-mode-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>已进入 <strong style="color:#3b82f6">{{ fns[mode].lb }}</strong> 模式，输入你的问题，AI 将基于当前数据和知识图谱分析</span>
        <el-button link type="primary" @click="exitMode">退出模式</el-button>
      </div>

      <!-- 消息列表 -->
      <div class="ai-msgs" ref="msgsRef">
        <div v-for="(m, i) in currentChat?.msgs || []" :key="i" class="ai-msg" :class="m.role">
          <!-- 思考过程 -->
          <div v-if="m.thinking" class="ai-thinking">
            <div class="ai-th-h" @click="m.thinkOpen = !m.thinkOpen">
              <el-icon class="ai-th-fire"><Brush /></el-icon>
              <span>思考过程</span>
              <el-icon class="ai-th-arrow"><component :is="m.thinkOpen ? 'CaretBottom' : 'CaretRight'" /></el-icon>
            </div>
            <div v-if="m.thinkOpen" class="ai-th-c">{{ m.thinking }}</div>
          </div>

          <!-- 报告卡片 -->
          <div v-if="m.card" class="ai-card">
            <div class="ai-card-h">
              <span class="ai-card-title">{{ m.card.title }}</span>
              <span class="ai-card-stat" v-if="m.card.stat">
                <span class="ai-cs-dot" :style="{background: m.card.statColor || '#22c55e'}"></span>
                {{ m.card.stat }}
              </span>
            </div>
            <div class="ai-card-c" v-html="m.card.body"></div>
            <div v-if="m.card.ok" class="ai-card-ok">
              <el-icon><Check /></el-icon> {{ m.card.ok }}
            </div>
          </div>

          <!-- 趋势图 -->
          <div v-if="m.chart" class="ai-chart">
            <div class="ai-chart-h">
              <span class="ai-chart-l">
                <el-icon><Picture /></el-icon> {{ m.chart.source }} <span class="ai-chart-id">{{ m.chart.id }}</span>
              </span>
              <span class="ai-chart-tt">{{ m.chart.title }}</span>
              <el-button size="small" v-if="m.chart.toggle" @click="m.chartShow = !m.chartShow">
                {{ m.chartShow ? '隐藏工况' : '显示工况' }}
              </el-button>
            </div>
            <div v-show="m.chartShow !== false" :ref="el => { if (el) chartRefs[m.chart.id] = el }" class="ai-chart-c"></div>
          </div>

          <!-- 纯文本消息 -->
          <div v-if="m.text && !m.card && !m.chart" class="ai-text">{{ m.text }}</div>
        </div>
      </div>

      <!-- 你可能想问 -->
      <div class="ai-quick-q" v-if="currentFn?.questions?.length">
        <div class="ai-quick-q-t">💡 {{ mode >= 0 ? fns[mode].lb + ' · ' : '' }}推荐问题：</div>
        <div class="ai-quick-q-l">
          <div v-for="q in currentFn.questions" :key="q" class="ai-qq" @click="askQuick(q)">{{ q }}</div>
        </div>
      </div>

      <!-- 快捷分析模板 -->
      <div class="ai-templates" v-if="quickTemplates.length">
        <div v-for="t in quickTemplates" :key="t" class="ai-tpl" @click="askQuick(t)">{{ t }}</div>
      </div>

      <!-- 输入框 -->
      <div class="ai-inp">
        <el-input v-model="q" :placeholder="mode >= 0 ? '在 ' + fns[mode].lb + ' 模式下提问...' : '先选个功能模块再提问'" @keyup.enter="send" />
        <el-button class="ai-inp-ic"><el-icon><Paperclip /></el-icon></el-button>
        <el-button class="ai-inp-ic"><el-icon><Microphone /></el-icon></el-button>
        <el-button type="primary" class="ai-inp-send" :disabled="mode < 0" @click="send"><el-icon><Promotion /></el-icon></el-button>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, watch, onUnmounted, reactive } from 'vue'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'

const store = useDataStore()
const userStore = useUserStore()
const q = ref('')
const histKw = ref('')
const mode = ref(-1)
const msgsRef = ref(null)
const chartRefs = reactive({})
const charts = {}

// ============ 8 个功能模块 ============
const fns = [
  { ic: '📊', lb: '设备工况', questions: ['A引风机当前工况如何？', 'D磨煤机为什么健康度这么低？', '给我水泵的运行情况'] },
  { ic: '🔔', lb: '报警分析', questions: ['今日有哪些一级报警？', '为什么A引风机轴承温度会偏高？', '最近一周哪些设备报警最多？'] },
  { ic: '📈', lb: '数据分析', questions: ['最近1小时负荷变化趋势', '主汽温度近24小时走势', 'NOx排放是否超标？'] },
  { ic: '🔍', lb: '故障溯源', questions: ['A引风机轴承温度异常的原因？', 'D磨煤机振动超标怎么办？', '润滑油压力低如何处理？'] },
  { ic: '📚', lb: '知识库', questions: ['锅炉主汽温度过高的原因', '磨煤机振动常见原因有哪些？', '电厂消防主要规范有哪些？'] },
  { ic: '📋', lb: '政策分析', questions: ['当前运行参数是否符合GB 50660？', '电厂消防标准GB 50016要求？', '报警响应时间合规吗？'] },
  { ic: '🎯', lb: '寻优操作', questions: ['如何降低机组煤耗？', '如何提高燃烧效率？', '如何延长设备使用寿命？'] },
  { ic: '📝', lb: '运行日志', questions: ['生成本班运行日志', '生成本周巡检总结', '生成月度运行报告'] }
]

const currentFn = computed(() => mode.value >= 0 ? fns[mode.value] : null)

const quickTemplates = computed(() => {
  if (mode.value < 0) {
    return ['A引风机工况情况', 'B引风机工况情况', 'A给水泵工况情况']
  }
  return fns[mode.value].questions.slice(0, 3)
})

// ============ 历史对话数据 ============
const chatHistory = ref([
  { id: 'c1', title: 'A引风机工况情况', time: '今天', unit: 'U1', msgs: [
    { role: 'bot', thinkOpen: false,
      thinking: '用户询问 A引风机工况情况。需要从 store.devices 中找到 A引风机，提取其健康度和运行参数，并基于此生成分析报告和趋势图。',
      card: {
        title: 'A引风机 · 出力正常 · 健康度 97.3',
        stat: '无残差预警',
        statColor: '#22c55e',
        body: `<div class="ai-mt"><span>机组的负荷</span><strong>500.0 MW</strong></div>
<div class="ai-mt"><span>比功</span><strong>2854 Nm/kg</strong></div>
<div class="ai-mt"><span>体积流量</span><strong>157.9 m³/s</strong></div>
<div class="ai-mt"><span>效率</span><strong>51%</strong>，距失谐告警线较远</div>
<div class="ai-mt"><span>稍度</span><strong>196.0 t/h</strong></div>
<div class="ai-mt"><span>电流平衡</span><strong>99.58%</strong></div>
<div class="ai-mt"><span>流量平衡</span><strong>98.99%</strong></div>`,
        ok: '整体运行良好'
      }
    },
    { role: 'bot', chartShow: true,
      chart: { id: 'chart-1', source: 'Surface', id: 's5_b8391b357e7', title: '引风机工况分析', toggle: true } }
  ]},
  { id: 'c2', title: 'B引风机工况情况', time: '30天内', unit: 'U1', msgs: [
    { role: 'bot', text: 'B引风机当前运行平稳，健康度 96.1，振动 1.5mm/s，电流 125A，主要参数均处于正常范围内。' }
  ]},
  { id: 'c3', title: 'A引风机工况情况', time: '30天内', unit: 'U1', msgs: [{ role: 'bot', text: 'A引风机当前健康度 82.3，轴承温度 82.3℃（已超限 75℃），存在异常。' }] },
  { id: 'c4', title: 'A引风机工况情况', time: '30天内', unit: 'U1', msgs: [{ role: 'bot', text: 'A引风机历史工况良好。' }] },
  { id: 'c5', title: '2026-07报警统计分析', time: '30天内', unit: 'U1', msgs: [{ role: 'bot', text: '本月共发生 12 条报警，其中一级 2 条、二级 5 条、智能预警 5 条。' }] },
  { id: 'c6', title: 'A引风机工况情况', time: '30天内', unit: 'U1', msgs: [{ role: 'bot', text: 'A引风机上周运行情况...' }] },
  { id: 'c7', title: 'A引风机工况情况', time: '30天内', unit: 'U1', msgs: [{ role: 'bot', text: 'A引风机本月运行报告...' }] },
  { id: 'c8', title: 'A引风机工况情况', time: '30天内', unit: 'U1', msgs: [{ role: 'bot', text: 'A引风机...' }] },
  { id: 'c9', title: 'A引风机工况情况', time: '30天内', unit: 'U1', msgs: [{ role: 'bot', text: 'A引风机...' }] },
  { id: 'c10', title: 'A引风机工况情况', time: '30天内', unit: 'U1', msgs: [{ role: 'bot', text: 'A引风机...' }] }
])

const currentChat = ref(chatHistory.value[0])

const groupedHistory = computed(() => {
  const groups = {}
  chatHistory.value.forEach(c => {
    if (!groups[c.time]) groups[c.time] = []
    groups[c.time].push(c)
  })
  return Object.entries(groups).map(([label, items]) => ({ label, items }))
})

const newChat = () => {
  const nc = { id: 'c' + Date.now(), title: '新对话', time: '今天', unit: 'U1',
    msgs: [{ role: 'bot', text: '您好，我是火电运行 AI 智能体。请先选择上方的功能模块（设备工况/报警分析/数据分析/故障溯源/知识库/政策分析/寻优操作/运行日志），然后向我提问。' }]
  }
  chatHistory.value.unshift(nc)
  currentChat.value = nc
  mode.value = -1
}

const openChat = (c) => { currentChat.value = c; mode.value = -1; nextTick(() => renderAllCharts()) }

const selectMode = (i) => {
  mode.value = i
  if (!currentChat.value) newChat()
  currentChat.value.msgs.push({
    role: 'bot',
    text: '已切换到 ' + fns[i].lb + ' 模式。请输入你的问题，AI 会基于该领域的专业知识分析回答。'
  })
  scrollToBottom()
}

const exitMode = () => { mode.value = -1 }

const askQuick = (qq) => { q.value = qq; send() }

const scrollToBottom = () => {
  if (msgsRef.value) msgsRef.value.scrollTop = msgsRef.value.scrollHeight
}

// ============ 趋势图渲染 ============
const renderChart = (id, el) => {
  if (!el) return
  if (charts[id]) { charts[id].dispose(); delete charts[id] }
  const chart = echarts.init(el)
  charts[id] = chart

  if (id === 'chart-1') {
    chart.setOption({
      backgroundColor: 'transparent',
      grid: { left: 50, right: 20, top: 20, bottom: 40 },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category', name: '体积流量(m³/s)',
        data: [100, 120, 140, 160, 180, 200, 220, 240, 260],
        axisLine: { lineStyle: { color: '#475569' } },
        axisLabel: { color: '#94a3b8' },
        nameTextStyle: { color: '#94a3b8' }
      },
      yAxis: {
        type: 'value', name: '比功(Nm/Kg)', min: 6000, max: 14000,
        axisLine: { lineStyle: { color: '#475569' } },
        axisLabel: { color: '#94a3b8' },
        splitLine: { lineStyle: { color: '#1e293b' } },
        nameTextStyle: { color: '#94a3b8' }
      },
      series: [
        { name: '失谐线', type: 'line', smooth: true, symbol: 'none',
          lineStyle: { color: '#ef4444', width: 2 },
          data: [13200, 13100, 12900, 12600, 12300, 12000, 11700, 11400, 11100] },
        { name: '当前工况', type: 'line', smooth: true, symbol: 'circle', symbolSize: 8,
          lineStyle: { color: '#3b82f6', width: 2.5 },
          itemStyle: { color: '#3b82f6' },
          data: [12000, 12200, 12400, 12600, 12400, 11800, 11000, 10000, 9500] },
        { name: '等熵曲线', type: 'line', smooth: true, symbol: 'none',
          lineStyle: { color: '#6366f1', width: 1, type: 'dashed' },
          data: [10000, 10500, 11000, 11500, 11500, 11000, 10500, 9500, 8500] },
        { name: '等熵曲线2', type: 'line', smooth: true, symbol: 'none',
          lineStyle: { color: '#6366f1', width: 1, type: 'dashed' },
          data: [9500, 10000, 10500, 11000, 11000, 10500, 10000, 9000, 8000] },
        { name: '等熵曲线3', type: 'line', smooth: true, symbol: 'none',
          lineStyle: { color: '#6366f1', width: 1, type: 'dashed' },
          data: [9000, 9500, 10000, 10500, 10500, 10000, 9500, 8500, 7500] },
        { name: '等熵曲线4', type: 'line', smooth: true, symbol: 'none',
          lineStyle: { color: '#6366f1', width: 1, type: 'dashed' },
          data: [8500, 9000, 9500, 10000, 10000, 9500, 9000, 8000, 7000] }
      ]
    })
  }
}

const renderAllCharts = () => {
  Object.entries(chartRefs).forEach(([id, el]) => {
    if (el) renderChart(id, el)
  })
}

watch(() => currentChat.value?.msgs, () => {
  nextTick(() => {
    renderAllCharts()
    scrollToBottom()
  })
}, { deep: true })

// ============ 发送消息 ============
const send = () => {
  const x = q.value.trim()
  if (!x) return
  if (mode.value < 0) return
  if (!currentChat.value) newChat()
  const chat = currentChat.value
  chat.msgs.push({ role: 'usr', text: x })
  q.value = ''
  scrollToBottom()

  setTimeout(() => {
    chat.msgs.push({
      role: 'bot',
      thinking: '正在分析 "' + x + '" ...',
      thinkOpen: false,
      card: generateCard(x),
      chart: generateChart(x)
    })
    nextTick(() => {
      renderAllCharts()
      scrollToBottom()
    })
  }, 600)
}

// ============ 设备识别 + 智能回答 ============

// 设备中文名 → store.devices 中的关键字
const findDevice = (q) => {
  const devs = store.devices
  // 1) 精确匹配：先试 A/B/C/D + 设备名
  const letterMatch = q.match(/([A-D])\s*(引风机|送风机|一次风机|磨煤机|给水泵|凝汽器|高压加热器|除氧器|润滑油)/)
  if (letterMatch) {
    const target = letterMatch[2]
    const prefix = letterMatch[1]
    const found = devs.find(d => d.name.startsWith(prefix) && d.name.includes(target))
    if (found) return found
  }
  // 2) 仅按设备类型匹配（取第一个）
  for (const kw of ['引风机', '送风机', '一次风机', '磨煤机', '给水泵', '凝汽器', '高压加热器', '除氧器', '润滑油']) {
    if (q.includes(kw)) {
      return devs.find(d => d.name.includes(kw))
    }
  }
  return null
}

// 设备健康度状态描述
const healthLabel = (h) => {
  if (h >= 95) return { text: '运行状态良好', color: '#22c55e' }
  if (h >= 85) return { text: '需加强巡检', color: '#f59e0b' }
  if (h >= 75) return { text: '建议尽快安排检修', color: '#f59e0b' }
  return { text: '已超限，需立即处置', color: '#ef4444' }
}

// 设备卡片：真实数据驱动
const deviceCard = (d, q) => {
  const params = d.params || {}
  const h = d.health
  const hl = healthLabel(h)
  // 取前 5 个参数做展示
  const lines = Object.entries(params).slice(0, 6).map(([k, v]) => {
    const over = v[0] >= v[1]
    return `<div class="ai-mt"><span>${k}</span><strong style="color:${over ? '#ef4444' : '#e2e8f0'}">${v[0]} ${v[2]}</strong></div>`
  }).join('')
  return {
    title: `${d.name} · ${hl.text} · 健康度 ${h.toFixed(1)}`,
    stat: h >= 95 ? '无异常' : (h >= 85 ? '需关注' : '存在风险'),
    statColor: hl.color,
    body: `<div class="ai-mt"><span>所属机组</span><strong>${d.unit}</strong></div>
<div class="ai-mt"><span>所属专业</span><strong>${d.dept}</strong></div>
<div class="ai-mt"><span>设备型号</span><strong>${d.model}</strong></div>
${lines}`,
    ok: hl.text + (q.includes('为什么') || q.includes('原因') ? '。详细原因可查看诊断页。' : '')
  }
}

const generateCard = (q) => {
  // 报警相关
  if (q.includes('报警') || q.includes('告警')) {
    const a = store.alarms
    const l1 = a.filter(x => x.l === 1).length
    const l2 = a.filter(x => x.l === 2).length
    const l3 = a.filter(x => x.l === 3).length
    const uh = a.filter(x => x.st === 'unhandled').length
    return {
      title: store.selectedUnit.name + ' · 当前报警统计',
      stat: '待处理 ' + uh + ' 条',
      statColor: uh > 0 ? '#ef4444' : '#22c55e',
      body: `<div class="ai-mt"><span>一级报警</span><strong style="color:#ef4444">${l1} 条</strong></div>
<div class="ai-mt"><span>二级报警</span><strong style="color:#f59e0b">${l2} 条</strong></div>
<div class="ai-mt"><span>智能预警</span><strong style="color:#06b6d4">${l3} 条</strong></div>
<div class="ai-mt"><span>未处理合计</span><strong style="color:${uh > 0 ? '#ef4444' : '#22c55e'}">${uh} 条</strong></div>`,
      ok: l1 > 0 ? '⚠️ 当前 ' + l1 + ' 条一级报警需立即处置' : '系统无一级报警'
    }
  }

  // 负荷/趋势
  if (q.includes('负荷') || q.includes('趋势')) {
    const u = store.selectedUnit
    return {
      title: u.name + ' · 实时负荷与运行参数',
      stat: '当前负荷 ' + u.base.load + ' MW',
      statColor: '#22c55e',
      body: `<div class="ai-mt"><span>当前负荷</span><strong>${u.base.load} MW</strong></div>
<div class="ai-mt"><span>主汽压力</span><strong>${u.base.press} MPa</strong></div>
<div class="ai-mt"><span>主汽温度</span><strong>${u.base.temp} ℃</strong></div>
<div class="ai-mt"><span>煤耗</span><strong>${u.base.coal} g/kWh</strong></div>
<div class="ai-mt"><span>NOx排放</span><strong>${u.base.nox} mg/m³</strong></div>
<div class="ai-mt"><span>SO₂排放</span><strong>${u.base.so2} mg/m³</strong></div>
<div class="ai-mt"><span>烟尘</span><strong>${u.base.dust} mg/m³</strong></div>`,
      ok: '负荷稳定，运行正常'
    }
  }

  // 设备相关（含工况、健康度等）
  const dev = findDevice(q)
  if (dev) return deviceCard(dev, q)

  // 兜底
  return {
    title: '查询结果',
    stat: '已分析',
    statColor: '#22c55e',
    body: `<div class="ai-mt"><span>查询</span><strong>${q}</strong></div>
<div class="ai-mt"><span>匹配度</span><strong>92%</strong></div>
<div class="ai-mt"><span>建议</span><strong>切换功能模块或精确输入设备名</strong></div>`,
    ok: '已收到您的问题'
  }
}

const generateChart = (q) => {
  if (q.includes('引风机') || q.includes('工况') || q.includes('风机') || q.includes('送风')) {
    return { id: 'chart-' + Date.now(), source: 'Surface', id: 's5_b8391b357e7', title: '风机工况分析', toggle: true }
  }
  if (q.includes('负荷') || q.includes('趋势') || q.includes('主汽')) {
    return { id: 'chart-trend-' + Date.now(), source: 'Trend', id: 'load_24h', title: '负荷变化趋势', toggle: true }
  }
  if (q.includes('温度') || q.includes('压力')) {
    return { id: 'chart-trend-' + Date.now(), source: 'Trend', id: 'tag_recent', title: '测点近24h趋势', toggle: true }
  }
  return null
}

onMounted(() => nextTick(() => renderAllCharts()))
onUnmounted(() => Object.values(charts).forEach(c => c?.dispose()))
</script>

<style scoped>
.ai-page { position: fixed; inset: 0; background: #0a0e17; display: flex; flex-direction: column; z-index: 100; color: #e2e8f0; }

/* 顶栏（深色） */
.ai-top { display: flex; align-items: center; padding: 0 24px; height: 48px; background: #0d1117; border-bottom: 0.5px solid #1e293b; }
.ai-top-tt { font-size: 15px; font-weight: 600; color: #e2e8f0; }
.ai-top-r { margin-left: auto; display: flex; align-items: center; gap: 16px; font-size: 12px; color: #94a3b8; }
.ai-status { display: flex; align-items: center; gap: 6px; color: #22c55e; font-weight: 500; }
.ai-dot { width: 8px; height: 8px; border-radius: 50%; background: #22c55e; box-shadow: 0 0 6px #22c55e; }
.ai-user { display: flex; align-items: center; gap: 4px; color: #cbd5e1; }

.ai-body { flex: 1; display: grid; grid-template-columns: 260px 1fr; min-height: 0; }

/* 左侧（深色） */
.ai-sb { background: #0d1117; border-right: 0.5px solid #1e293b; display: flex; flex-direction: column; }
.ai-sb-top { padding: 14px 14px 6px; }
.ai-new-btn { width: 100%; background: linear-gradient(135deg, #6366f1, #8b5cf6); border: none; }
.ai-sb-search { padding: 6px 14px 10px; }
.ai-sb-search :deep(.el-input__wrapper) { background: #111827; box-shadow: 0 0 0 0.5px #1e293b inset; }
.ai-sb-search :deep(.el-input__inner) { color: #e2e8f0; }
.ai-sb-search :deep(.el-input__inner::placeholder) { color: #64748b; }
.ai-sb-list { flex: 1; overflow-y: auto; padding: 4px 8px 14px; }
.ai-sb-g { font-size: 11px; color: #64748b; font-weight: 600; padding: 10px 8px 4px; }
.ai-sb-i { padding: 10px 12px; border-radius: 8px; cursor: pointer; transition: all 0.15s; }
.ai-sb-i:hover { background: #1a2332; }
.ai-sb-i.on { background: rgba(59,130,246,0.15); }
.ai-sb-i-t { font-size: 13px; color: #cbd5e1; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-sb-i.on .ai-sb-i-t { color: #e2e8f0; }

/* 主区（深色） */
.ai-main { display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.ai-mh { display: flex; align-items: center; padding: 14px 24px; border-bottom: 0.5px solid #1e293b; gap: 12px; }
.ai-mh-tt { font-size: 16px; font-weight: 600; color: #e2e8f0; }
.ai-mh-tag { display: flex; gap: 6px; }
.ai-mh-r { margin-left: auto; color: #94a3b8; }

/* 8 个功能模块 */
.ai-fns { display: grid; grid-template-columns: repeat(8, 1fr); gap: 8px; padding: 8px 24px; background: #0a0e17; border-bottom: 0.5px solid #1e293b; }
.ai-fn-i { background: #161d2a; border: 0.5px solid #1e293b; border-radius: 8px; padding: 8px 6px; cursor: pointer; text-align: center; transition: 0.15s; }
.ai-fn-i:hover { border-color: #3b82f6; background: rgba(59,130,246,0.08); }
.ai-fn-i.on { border-color: #3b82f6; background: rgba(59,130,246,0.18); box-shadow: 0 0 0 1px #3b82f6; }
.ai-fn-ic { font-size: 18px; margin-bottom: 2px; }
.ai-fn-lb { font-size: 11px; color: #cbd5e1; font-weight: 500; }
.ai-fn-i.on .ai-fn-lb { color: #60a5fa; }

/* 模式提示 */
.ai-mode-hint { display: flex; align-items: center; gap: 8px; padding: 6px 24px; background: rgba(59,130,246,0.08); border-bottom: 0.5px solid #1e293b; font-size: 12px; color: #94a3b8; }
.ai-mode-hint .el-icon { color: #3b82f6; }

/* 消息列表 */
.ai-msgs { flex: 1; overflow-y: auto; padding: 18px 24px; background: #0a0e17; min-height: 0; }
.ai-msg { margin-bottom: 14px; max-width: 92%; }
.ai-msg.bot { margin-right: auto; }
.ai-msg.usr { margin-left: auto; }

/* 思考过程（深色） */
.ai-thinking { background: #1a2332; border: 0.5px solid #2a3544; border-radius: 8px; padding: 8px 12px; margin-bottom: 8px; }
.ai-th-h { display: flex; align-items: center; gap: 6px; cursor: pointer; font-size: 12px; color: #a78bfa; font-weight: 500; }
.ai-th-fire { color: #a78bfa; }
.ai-th-arrow { margin-left: auto; }
.ai-th-c { margin-top: 6px; padding-top: 6px; border-top: 0.5px dashed #2a3544; font-size: 12px; color: #c4b5fd; line-height: 1.6; }

/* 报告卡片（深色） */
.ai-card { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 14px 16px; }
.ai-card-h { display: flex; justify-content: space-between; align-items: center; padding-bottom: 8px; border-bottom: 0.5px solid #1e293b; margin-bottom: 10px; }
.ai-card-title { font-size: 14px; font-weight: 600; color: #e2e8f0; }
.ai-card-stat { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #22c55e; }
.ai-cs-dot { width: 8px; height: 8px; border-radius: 50%; }
.ai-card-c { font-size: 13px; color: #cbd5e1; line-height: 1.8; }
.ai-card-c :deep(.ai-mt) { display: flex; justify-content: space-between; padding: 4px 0; }
.ai-card-c :deep(.ai-mt span) { color: #94a3b8; }
.ai-card-c :deep(.ai-mt strong) { color: #e2e8f0; font-weight: 600; }
.ai-card-ok { margin-top: 10px; padding-top: 8px; border-top: 0.5px solid #1e293b; font-size: 13px; color: #22c55e; display: flex; align-items: center; gap: 4px; font-weight: 500; }

/* 趋势图（深色） */
.ai-chart { background: #0a0e17; border: 0.5px solid #1e293b; border-radius: 10px; padding: 12px 16px; margin-top: 8px; }
.ai-chart-h { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.ai-chart-l { font-size: 12px; color: #94a3b8; display: flex; align-items: center; gap: 4px; }
.ai-chart-id { color: #64748b; font-family: monospace; }
.ai-chart-tt { font-size: 14px; font-weight: 600; color: #e2e8f0; margin: 0 12px; }
.ai-chart-c { width: 100%; height: 320px; background: #0a0e17; }

/* 文本消息（深色） */
.ai-text { background: #1a2332; padding: 10px 14px; border-radius: 8px; font-size: 13px; color: #e2e8f0; line-height: 1.7; border: 0.5px solid #2a3544; }
.ai-msg.usr .ai-text { background: rgba(59,130,246,0.15); border-color: #3b82f6; color: #e2e8f0; }

/* 你可能想问 */
.ai-quick-q { padding: 6px 24px; background: rgba(34,197,94,0.05); border-top: 0.5px solid #1e293b; }
.ai-quick-q-t { font-size: 11px; color: #22c55e; margin-bottom: 4px; font-weight: 500; }
.ai-quick-q-l { display: flex; flex-wrap: wrap; gap: 6px; }
.ai-qq { padding: 4px 12px; background: rgba(34,197,94,0.1); border: 0.5px solid rgba(34,197,94,0.3); border-radius: 16px; font-size: 11px; color: #86efac; cursor: pointer; transition: all 0.15s; }
.ai-qq:hover { border-color: #22c55e; background: rgba(34,197,94,0.2); color: #bbf7d0; }

/* 模板 */
.ai-templates { display: flex; gap: 8px; padding: 6px 24px; background: #0a0e17; border-top: 0.5px solid #1e293b; }
.ai-tpl { flex: 1; padding: 6px 10px; background: #111827; border: 0.5px solid #1e293b; border-radius: 6px; font-size: 11px; color: #cbd5e1; cursor: pointer; text-align: center; transition: all 0.15s; }
.ai-tpl:hover { border-color: #3b82f6; color: #60a5fa; background: rgba(59,130,246,0.1); }

/* 输入框（深色） */
.ai-inp { display: flex; align-items: center; gap: 8px; padding: 10px 24px; background: #0d1117; border-top: 0.5px solid #1e293b; }
.ai-inp :deep(.el-input__wrapper) { background: #111827; box-shadow: 0 0 0 0.5px #1e293b inset; border-radius: 20px; padding: 4px 14px; }
.ai-inp :deep(.el-input__inner) { color: #e2e8f0; }
.ai-inp :deep(.el-input__inner::placeholder) { color: #64748b; }
.ai-inp-ic { border-radius: 50%; width: 36px; height: 36px; padding: 0; background: #111827; border: 0.5px solid #1e293b; color: #94a3b8; }
.ai-inp-ic:hover { border-color: #3b82f6; color: #60a5fa; }
.ai-inp-send { border-radius: 50%; width: 36px; height: 36px; padding: 0; background: linear-gradient(135deg, #6366f1, #8b5cf6); border: none; }
.ai-inp-send:disabled { background: #1e293b; }
</style>
