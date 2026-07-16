<template>
<div>
  <div style="margin-bottom:14px;display:flex;align-items:center;gap:10px">
    <span style="font-size:13px;font-weight:500">诊断设备：</span>
    <el-select v-model="did" style="width:320px" filterable>
      <el-option v-for="d in diagList" :key="d.id" :value="d.id" :label="d.name + '（' + d.dept + '）' + (d.alarm ? ' — ' + d.alarm : '')"/>
    </el-select>
  </div>
  <el-row :gutter="14" class="diag-row">
    <el-col :span="12" class="diag-col"><div class="cd diag-card"><div class="cd-t">智能诊断</div>
      <div style="background:#0a0e17;border-radius:8px;padding:14px;margin-bottom:12px">
        <div style="font-size:11px;color:#94a3b8;margin-bottom:4px">诊断对象</div>
        <div style="font-size:14px;font-weight:500">{{ cur.name }} — {{ cur.fault }}</div>
      </div>
      <div class="sect"><div class="cd-t">智能分析结果</div><p>{{ cur.analysis }}</p></div>
      <div class="sect"><div class="cd-t">可能原因</div>
        <div v-for="(c,i) in cur.causes" :key="i" class="cause"><span class="tg" :class="c.conf>90?'tg-w':'tg-i'">{{ c.ref }} · {{ c.conf }}</span> {{ c.text }}</div>
      </div>
      <div class="sect"><div class="cd-t" style="color:#22c55e">操作指导</div>
        <p v-for="(g,i) in cur.guide" :key="i">{{ i+1 }}. {{ g }}</p>
      </div>
    </div></el-col>
    <el-col :span="12" class="diag-col">
      <div class="cd kg-card"><div class="cd-t">知识图谱推理（拖动节点可调整位置）</div><div ref="kg" class="kg-chart"></div></div>
    </el-col>
  </el-row>

  <!-- 引用溯源：全宽放底部 -->
  <div class="cd ref-card">
    <div class="cd-t">引用溯源</div>
    <div class="ref-grid">
      <div v-for="(r,i) in cur.refs" :key="i" class="ref">
        <span class="tg" :class="r.conf>90?'tg-w':'tg-i'">{{ r.id }}</span>
        <span style="color:#64748b;font-size:11px;margin-left:6px">{{ r.date }}</span>
        <div class="ref-desc">{{ r.desc }}</div>
        <div class="ref-conf">置信度 <strong style="color:#3b82f6">{{ r.conf }}</strong></div>
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

// 诊断对象列表：直接从 store.devices 生成
const diagList = computed(() => {
  return store.devices.map(d => {
    const alarm = store.alarms.find(a => a.device === d.name && a.st !== 'resolved')
    return {
      id: d.id,
      name: d.name,
      dept: d.dept,
      fault: alarm ? alarm.desc : '当前无报警'
    }
  })
})

// 当前选中的设备ID，默认第一台
const did = ref(store.devices[0]?.id || '')

// 诊断数据（按设备ID索引）
const allDiagnoses = {
  'U1-idf-a': { name: 'A引风机', fault: '轴承温度异常升高',
    analysis: '检测到轴承温度 82.3℃，较正常工况偏离 37℃，温升速率 +0.8℃/min。振动频谱显示轴承外圈特征频率增强，润滑油铁谱铁磁性颗粒浓度 120ppm。综合判断：轴承润滑不良导致摩擦加剧。',
    causes: [{ ref: 'REF-1', conf: '94.2%', text: '轴承润滑油脂劣化（8000h未更换），油膜减薄' }, { ref: 'REF-2', conf: '87.6%', text: '冷却水流量不足，散热效率下降' }, { ref: 'REF-3', conf: '71.3%', text: '轴承内圈微点蚀，不均匀磨损' }],
    guide: ['立即更换润滑油脂（ISO VG 68）', '降低负荷至额定 80%，控制温升', '准备 6208 轴承备件，48h 内停机检查', '加强巡检至每 15min 一次'],
    refs: [{ id: 'REF-1', date: '2025-06-12', desc: 'A 引风机温度高 — 换油恢复', conf: '94.2%' }, { id: 'REF-2', date: '2025-03-28', desc: '同型号引风机 — 冷却水滤网堵塞', conf: '87.6%' }, { id: 'REF-3', date: '2024-11-15', desc: '轴承更换后运行 8200h', conf: '71.3%' }] },
  'U1-mill-a': { name: 'A磨煤机', fault: '振动幅值超标',
    analysis: '振动 4.7mm/s 超预警阈值。1 倍转频分量增强，判断为磨辊不平衡。电流波动 ±12%，近期煤质变差。',
    causes: [{ ref: 'REF-1', conf: '91.8%', text: '磨辊磨损不均，动不平衡' }, { ref: 'REF-2', conf: '83.2%', text: '入磨粒度偏大，出力下降' }],
    guide: ['降低给煤量至 70%', '检查磨辊磨损，必要时更换', '若振动 > 5.5mm/s，立即停磨'],
    refs: [{ id: 'REF-1', date: '2025-07-03', desc: 'A 磨煤机振动 — 换磨辊恢复', conf: '91.8%' }, { id: 'REF-2', date: '2025-05-20', desc: '煤质分析 — 哈氏可磨指数下降', conf: '83.2%' }] },
  'U1-pump-b': { name: '给水泵B', fault: '效率下降预警',
    analysis: '效率从 92% 降至 82.1%。流体性能曲线分析表明存在气蚀，润滑正常排除轴承问题。',
    causes: [{ ref: 'REF-1', conf: '88.4%', text: '叶轮入口气蚀' }, { ref: 'REF-2', conf: '76.1%', text: '密封环磨损，内泄漏增大' }],
    guide: ['检查给水泵入口压力', '降低转速至 90%', '安排停机检查叶轮与密封环'],
    refs: [{ id: 'REF-1', date: '2025-04-18', desc: '给水泵修复后效率恢复 93%', conf: '88.4%' }, { id: 'REF-2', date: '2025-01-22', desc: '密封环更换记录', conf: '76.1%' }] }
}

// 通用兜底（其他设备显示默认模板）
const defaultDiagnosis = (d) => ({
  name: d.name,
  fault: '该设备暂无历史故障记录',
  analysis: `当前 ${d.name}（${d.dept}）运行状态正常，未触发深度诊断分析。AI 将持续监控该设备的关键参数。`,
  causes: [],
  guide: ['继续保持当前运行参数', '加强日常巡检频次'],
  refs: []
})

// 当前选中设备的诊断
const cur = computed(() => {
  if (allDiagnoses[did.value]) return allDiagnoses[did.value]
  const d = store.devices.find(x => x.id === did.value)
  return d ? defaultDiagnosis(d) : { name: '请选择设备', fault: '', analysis: '', causes: [], guide: [], refs: [] }
})

// 知识图谱
const kg = ref(null)
let kc

const initKG = () => {
  if (kc) kc.dispose()
  const el = kg.value
  if (!el) return
  kc = echarts.init(el)
  const d = cur.value
  const map = {
    'A引风机': [{ n: '轴承温度高', c: '#ef4444' }, { n: '润滑失效', c: '#f59e0b' }, { n: '冷却不足', c: '#f59e0b' }, { n: '轴承磨损', c: '#f59e0b' }, { n: '更换油脂', c: '#22c55e' }, { n: '清洗滤网', c: '#22c55e' }, { n: '安排检修', c: '#22c55e' }],
    'A磨煤机': [{ n: '振动超标', c: '#ef4444' }, { n: '磨辊磨损', c: '#f59e0b' }, { n: '煤质变差', c: '#f59e0b' }, { n: '更换磨辊', c: '#22c55e' }, { n: '调整给煤量', c: '#22c55e' }],
    '给水泵B': [{ n: '效率下降', c: '#ef4444' }, { n: '气蚀', c: '#f59e0b' }, { n: '密封环磨损', c: '#f59e0b' }, { n: '检修叶轮', c: '#22c55e' }, { n: '更换密封环', c: '#22c55e' }]
  }
  const list = map[d.name] || [{ n: '正常运行', c: '#22c55e' }, { n: '状态良好', c: '#22c55e' }]
  const nodes = [{ name: d.name, c: '#3b82f6' }, ...list]
  kc.setOption({
    series: [{
      type: 'graph', layout: 'force', roam: false,
      force: { repulsion: 200, edgeLength: 80 },
      data: nodes.map(n => ({ name: n.n, symbolSize: n.c === '#3b82f6' ? 40 : 26, itemStyle: { color: n.c }, label: { show: true, color: '#e2e8f0', fontSize: 11 } })),
      links: list.map(n => ({ source: d.name, target: n.n })),
      lineStyle: { color: '#2a3544', curveness: 0.2 }
    }]
  })
}

watch(did, () => nextTick(initKG))
onMounted(() => nextTick(initKG))
onUnmounted(() => kc?.dispose())
</script>

<style scoped>
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-t { font-size: 13px; font-weight: 500; margin-bottom: 10px; }
.sect { margin-bottom: 14px; }
.sect-t { color: #f59e0b; font-weight: 500; margin-bottom: 6px; font-size: 12px; }
.sect p { font-size: 12px; color: #94a3b8; line-height: 1.8; margin-bottom: 4px; }
.cause { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; font-size: 12px; color: #94a3b8; }
.tg { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; }
.tg-w { background: rgba(245,158,11,0.12); color: #f59e0b; }
.tg-i { background: rgba(59,130,246,0.12); color: #3b82f6; }

/* 核心：用 flex 强制两列等高，避开 el-row 默认样式 */
:deep(.el-row.diag-row) { display: flex !important; flex-wrap: wrap; align-items: stretch; }
:deep(.el-row.diag-row .el-col) { display: flex; }
.diag-card, .kg-card { flex: 1; min-height: 0; }
.kg-chart { flex: 1; min-height: 0; }

/* 引用溯源：全宽放在底部 */
.ref-card { margin-top: 14px; }
.ref-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 10px; }
.ref { background: #0a0e17; padding: 12px; border-radius: 8px; font-size: 12px; color: #94a3b8; border: 0.5px solid #1e293b; }
.ref-desc { color: #cbd5e1; margin: 6px 0; line-height: 1.5; }
.ref-conf { font-size: 11px; color: #94a3b8; }
</style>
