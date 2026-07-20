<template>
<div class="ac">
  <div class="ag-cards">
    <div v-for="(f, i) in fns" :key="i" class="ag-ci" :class="{on:mode===i}" @click="selectMode(i)">
      <div class="ag-ic">{{ f.ic }}</div>
      <div class="ag-lb">{{ f.lb }}</div>
    </div>
  </div>

  <div class="ag-mode-hint" v-if="mode>=0">
    <el-icon><InfoFilled /></el-icon>
    <span>已进入 <strong style="color:#3b82f6">{{ fns[mode].lb }}</strong> 模式，输入你的问题，AI 将基于当前数据和知识图谱分析</span>
    <el-button link type="primary" @click="exitMode">退出模式</el-button>
  </div>

  <div class="ag-ms" ref="msgs">
    <div class="am bot"><div class="am-h">HiFire Agent · 运行智能体</div>
      <div class="am-b">您好，我是<span class="hl">火电运行AI智能体</span>。请先<strong>选择上方的功能模块</strong>（设备工况/报警分析/数据分析/故障溯源/知识库/寻优），然后向我提问。AI 会基于该模块的专业知识回答。</div>
    </div>
  </div>

  <div class="ag-quick" v-if="mode>=0">
    <div class="ag-quick-t">💡 该模块推荐问题：</div>
    <div class="ag-quick-l">
      <el-tag v-for="q in currentFn.questions" :key="q" class="qt" effect="plain" @click="askQuick(q)">{{ q }}</el-tag>
    </div>
  </div>

  <div class="ag-inp">
    <el-input v-model="q" :placeholder="mode>=0?'在 '+fns[mode].lb+' 模式下提问...':'先选个功能模块再提问'" @keyup.enter="send" />
    <el-button type="primary" @click="send" :disabled="mode<0">发送</el-button>
  </div>
</div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const q = ref(''), mode = ref(-1), msgs = ref(null)

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

const currentFn = computed(() => fns[mode.value] || fns[0])

const selectMode = (i) => { mode.value = i; q.value = ''; addMsg('bot', `<div class="am-h">${now()} · HiFire Agent</div><div class="am-b">已切换到 <span class="hl">${fns[i].lb}</span> 模式。请输入你的问题，AI 会基于该领域的专业知识分析回答。</div>`) }
const exitMode = () => { mode.value = -1; q.value = '' }
const askQuick = (qq) => { q.value = qq; send() }
const now = () => new Date().toLocaleTimeString('zh-CN', { hour12: false })

const addMsg = (type, html) => {
  const d = document.createElement('div'); d.className = 'am ' + type; d.innerHTML = html
  msgs.value.appendChild(d); scroll()
}
const scroll = () => nextTick(() => { const e = msgs.value; if (e) e.scrollTop = e.scrollHeight })

const send = () => {
  const x = q.value.trim()
  if (!x || mode.value < 0) return
  addMsg('usr', `<div class="am-h">${now()} · ${fns[mode.value].lb}模式</div><div class="am-b">${x}</div>`)
  q.value = ''
  setTimeout(() => {
    addMsg('bot', `<div class="am-h">${now()} · HiFire Agent · ${fns[mode.value].lb}</div>` + generateAnswer(x, mode.value))
  }, 600)
}

const generateAnswer = (q, m) => {
  const devs = store.unitDevices(store.selectedUnitId)
  const alarms = store.unitAlarms(store.selectedUnitId)
  const lowH = devs.filter(d => d.health < 85)
  const l1 = alarms.filter(a => a.l === 1)

  if (m === 0) { // 设备工况
    const target = devs.find(d => q.includes(d.name)) || devs[0]
    return `<div class="am-b"><div style="font-weight:500;margin-bottom:6px">📊 ${target.name} 工况分析</div>
      <div class="metric-line"><span>健康度</span><span class="hl" style="color:${target.health>=90?'#22c55e':target.health>=80?'#f59e0b':'#ef4444'}">${target.health.toFixed(1)}</span></div>
      ${Object.entries(target.params).slice(0, 4).map(([k, v]) => `<div class="metric-line"><span>${k}</span><span style="color:${v[0]>=v[1]?'#ef4444':'#e2e8f0'}">${v[0]} ${v[2]}</span></div>`).join('')}
      <div style="margin-top:8px;padding-top:6px;border-top:0.5px solid #1e293b;color:#f59e0b">综合：${target.health >= 90 ? '运行状态良好' : target.health >= 80 ? '需加强巡检' : '建议尽快安排检修'}。</div>
    </div>`
  }
  if (m === 1) { // 报警分析
    return `<div class="am-b"><div style="font-weight:500;margin-bottom:6px">🔔 ${store.selectedUnit.name} 报警分析</div>
      <div class="metric-line"><span>报警总数</span><span class="hl">${alarms.length} 条</span></div>
      <div class="metric-line"><span>一级未处理</span><span style="color:#ef4444">${l1.filter(a => a.st === 'unhandled').length} 条</span></div>
      <div class="metric-line"><span>二级未处理</span><span style="color:#f59e0b">${alarms.filter(a => a.l === 2 && a.st === 'unhandled').length} 条</span></div>
      <div class="metric-line"><span>智能预警</span><span style="color:#06b6d4">${alarms.filter(a => a.l === 3).length} 条</span></div>
      <div style="margin-top:8px;padding-top:6px;border-top:0.5px solid #1e293b;color:#f59e0b">${l1.length>0?'⚠️ 当前'+l1.length+'条一级报警需立即处置。':'系统无一级报警，运行稳定。'}</div>
    </div>`
  }
  if (m === 2) { // 数据分析
    return `<div class="am-b"><div style="font-weight:500;margin-bottom:6px">📈 数据分析</div>
      <div class="metric-line"><span>负荷稳定性</span><span style="color:#22c55e">±1.2%</span></div>
      <div class="metric-line"><span>主汽温度波动</span><span>±3.5℃</span></div>
      <div class="metric-line"><span>煤耗趋势</span><span style="color:#f59e0b">+1.8g/kWh</span></div>
      <div class="metric-line"><span>NOx排放</span><span style="color:#22c55e">42.8 mg/m³（达标）</span></div>
      <div style="margin-top:8px;padding-top:6px;border-top:0.5px solid #1e293b;color:#f59e0b">AI 建议：主汽温度接近上限，调整减温水量；煤耗缓慢上升，关注燃烧效率。</div>
    </div>`
  }
  if (m === 3) { // 故障溯源
    const a = alarms.find(x => q.includes(x.device) || q.includes(x.desc.slice(0, 3))) || alarms[0]
    if (a) return `<div class="am-b"><div style="font-weight:500;margin-bottom:6px">🔍 ${a.desc} 溯源分析</div>
      <div class="metric-line"><span>报警设备</span><span>${a.device}（${a.dept}）</span></div>
      <div class="metric-line"><span>报警点</span><span>${a.point} = ${a.val}</span></div>
      <div style="margin-top:8px">知识图谱匹配：</div>
      <div style="margin:4px 0"><span class="tg tg-w">REF-1 · 94%</span> 润滑失效/冷却不足</div>
      <div style="margin:4px 0"><span class="tg tg-i">REF-2 · 87%</span> 磨辊磨损/管路泄漏</div>
      <div style="margin:4px 0"><span class="tg tg-i">REF-3 · 71%</span> 轴承内圈磨损</div>
      <div style="margin-top:8px;padding-top:6px;border-top:0.5px solid #1e293b;color:#22c55e">建议：① 通知检修班组 ② 准备备件 ③ 24h 内完成检修。</div>
    </div>`
  }
  if (m === 4) { // 知识库
    return `<div class="am-b"><div style="font-weight:500;margin-bottom:6px">📚 知识库检索结果</div>
      <div style="margin:4px 0">1. <strong>GB 16806</strong>：消防联动控制系统设计规范</div>
      <div style="margin:4px 0">2. <strong>GB 50016</strong>：建筑设计防火规范</div>
      <div style="margin:4px 0">3. <strong>GB 50660</strong>：大中型火力发电厂设计规范</div>
      <div style="margin:4px 0">4. <strong>DL/T 5210</strong>：电力建设施工技术规范</div>
      <div style="margin-top:8px;color:#94a3b8">共匹配 4 条相关规范。当前报警场景下 GB 50016 第 8.3 节对消防设施要求最相关。</div>
    </div>`
  }
  if (m === 5) { // 政策分析
    return `<div class="am-b"><div style="font-weight:500;margin-bottom:6px">📋 政策合规分析</div>
      <div class="metric-line"><span>GB 50660 锅炉房</span><span style="color:#22c55e">✓ 合规</span></div>
      <div class="metric-line"><span>GB 50016 防火</span><span style="color:#22c55e">✓ 合规</span></div>
      <div class="metric-line"><span>GB 16806 消防</span><span style="color:#f59e0b">⚠ 待核</span></div>
      <div class="metric-line"><span>报警响应 <5min</span><span style="color:#22c55e">✓ 合规</span></div>
      <div style="margin-top:8px;padding-top:6px;border-top:0.5px solid #1e293b;color:#f59e0b">建议补充 GB 16806 消防系统检查报告。</div>
    </div>`
  }
  if (m === 6) { // 寻优
    return `<div class="am-b"><div style="font-weight:500;margin-bottom:6px">🎯 寻优操作建议</div>
      <div style="margin:6px 0">1. 优化煤粉细度：调节磨煤机分离器挡板，<strong style="color:#22c55e">预计降煤耗 1.2g/kWh</strong></div>
      <div style="margin:6px 0">2. 调整过量空气系数至 1.15，<strong style="color:#22c55e">-0.5g/kWh</strong></div>
      <div style="margin:6px 0">3. 提高主汽温度 543°C（当前 541°C），循环效率 +0.3%</div>
      <div style="margin:6px 0">4. 检修高加端差，<strong style="color:#22c55e">-0.4g/kWh</strong></div>
      <div style="margin-top:8px;padding-top:6px;border-top:0.5px solid #1e293b;color:#22c55e">预计整体降煤耗 2-3g/kWh，年节省燃料成本 ¥150 万。</div>
    </div>`
  }
  if (m === 7) { // 日志
    const today = new Date().toLocaleDateString('zh-CN')
    return `<div class="am-b"><div style="font-weight:500;margin-bottom:6px">📝 ${today} 运行日志（自动生成）</div>
      <div style="font-size:12px;line-height:1.7;color:#cbd5e1">
        <strong>【运行概况】</strong>${store.selectedUnit.name} 负荷稳定在 ${store.selectedUnit.base.load}MW，主汽温度 ${store.selectedUnit.base.temp}℃ 平稳运行。<br>
        <strong>【设备状态】</strong>${devs.length}台设备监控中，${lowH.length}台健康度低于85。<br>
        <strong>【报警处理】</strong>本班次处理报警 ${alarms.filter(a => a.st === 'resolved').length} 条，未处理 ${alarms.filter(a => a.st === 'unhandled').length} 条。<br>
        <strong>【交接事项】</strong>${l1.length>0?'⚠️ '+l1.length+'条一级报警需重点关注。':''}A引风机轴承温度持续偏高，建议下个班次优先处理。
      </div>
    </div>`
  }
  return `<div class="am-b">已收到：${q}</div>`
}
</script>

<style scoped>
.ac { display: flex; flex-direction: column; height: calc(100vh - 80px); }
.ag-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 12px; }
.ag-ci { background: #161d2a; border: 0.5px solid #1e293b; border-radius: 8px; padding: 14px; cursor: pointer; text-align: center; transition: 0.15s; }
.ag-ci:hover, .ag-ci.on { border-color: #3b82f6; background: rgba(59,130,246,0.12); }
.ag-ci-ic { font-size: 24px; margin-bottom: 6px; }
.ag-ci-lb { font-size: 12px; font-weight: 500; }
.ag-mode-hint { background: rgba(59,130,246,0.08); border: 0.5px solid rgba(59,130,246,0.3); border-radius: 8px; padding: 8px 14px; margin-bottom: 10px; font-size: 12px; color: #94a3b8; display: flex; align-items: center; gap: 8px; }
.ag-ms { flex: 1; overflow-y: auto; margin-bottom: 10px; }
.ag-quick { background: rgba(34,197,94,0.05); border: 0.5px solid rgba(34,197,94,0.2); border-radius: 8px; padding: 8px 12px; margin-bottom: 8px; }
.ag-quick-t { font-size: 11px; color: #22c55e; margin-bottom: 4px; }
.ag-quick-l { display: flex; flex-wrap: wrap; gap: 4px; }
.qt { cursor: pointer; }
.ag-inp { display: flex; gap: 8px; }
.ag-inp .el-input { flex: 1; }
.am { margin-bottom: 12px; padding: 12px 14px; border-radius: 8px; max-width: 90%; }
.am.usr { background: rgba(59,130,246,0.12); margin-left: auto; color: #e2e8f0; }
.am.bot { background: #161d2a; margin-right: auto; border: 0.5px solid #1e293b; }
.am-h { font-size: 11px; color: #64748b; margin-bottom: 6px; }
.am-b { font-size: 13px; line-height: 1.7; }
.hl { color: #3b82f6; font-weight: 500; }
.metric-line { display: flex; justify-content: space-between; padding: 3px 0; font-size: 12px; border-bottom: 0.5px solid #1e293b; }
.tg { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; }
.tg-w { background: rgba(245,158,11,0.12); color: #f59e0b; }
.tg-i { background: rgba(59,130,246,0.12); color: #3b82f6; }
</style>
