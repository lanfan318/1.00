<template>
<div class="dg-page">
  <!-- 头部：设备选择 -->
  <div class="dg-head">
    <div class="dg-head-l">
      <span class="dg-bar"></span>
      <h2>智能报警诊断</h2>
      <span class="dg-sub">基于知识图谱的设备故障推理与溯源</span>
    </div>
    <div class="dg-dev-sel">
      <span class="dg-dev-lbl">诊断设备</span>
      <el-select v-model="did" style="width:340px" filterable>
        <el-option v-for="d in diagList" :key="d.id" :value="d.id" :label="d.name + '（' + uName(d.unit) + ' · ' + d.dept + '）' + (d.alarm ? ' — ' + d.alarm : '')"/>
      </el-select>
    </div>
  </div>

  <div class="dg-row">
    <!-- 左：智能诊断 -->
    <div class="cd dg-diag">
      <div class="cd-t"><span class="ut-ic">▸</span>智能诊断</div>

      <div class="dg-obj">
        <div class="dg-obj-l">诊断对象</div>
        <div class="dg-obj-v">
          <span class="attr-unit">{{ curDev ? uName(curDev.unit) : '-' }}</span>
          <span class="attr-sep">·</span>
          <span class="attr-dept">{{ curDev ? curDev.dept : '-' }}</span>
          <span class="attr-sep">·</span>
          <span class="attr-dev">{{ cur.name }}</span>
          <span class="dg-fault" style="margin-left:8px">{{ cur.fault }}</span>
        </div>
      </div>

      <div class="dg-sect">
        <div class="dg-sect-h"><span class="dg-dot ok"></span>智能分析结果</div>
        <p class="dg-ans">{{ cur.analysis }}</p>
      </div>

      <div class="dg-sect">
        <div class="dg-sect-h"><span class="dg-dot warn"></span>可能原因</div>
        <div class="dg-causes">
          <div v-for="(c,i) in cur.causes" :key="i" class="dg-cause">
            <span class="tg" :class="c.conf>90?'tg-w':'tg-i'">{{ c.ref }}</span>
            <span class="dg-cause-conf">{{ c.conf }}</span>
            <span class="dg-cause-txt">{{ c.text }}</span>
          </div>
        </div>
      </div>

      <div class="dg-sect">
        <div class="dg-sect-h dg-guide-h"><span class="dg-dot suc"></span>操作指导</div>
        <div class="dg-guide">
          <div v-for="(g,i) in cur.guide" :key="i" class="dg-guide-i">
            <span class="dg-gn">{{ i+1 }}</span><span class="dg-gt">{{ g }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右：知识图谱推理 -->
    <div class="cd dg-kg">
      <div class="cd-t"><span class="ut-ic">▸</span>知识图谱推理</div>
      <div class="kg-chart">
        <ReasoningGraph :case-data="currentGraph" :selected-id="selNodeId" @update:selected-id="v => selNodeId = v" />
      </div>
    </div>
  </div>

  <!-- 引用溯源 -->
  <div class="cd dg-ref">
    <div class="cd-t"><span class="ut-ic">▸</span>引用溯源</div>
    <div class="ref-grid">
      <div v-for="(r,i) in cur.refs" :key="i" class="ref">
        <div class="ref-h">
          <span class="tg" :class="r.conf>90?'tg-w':'tg-i'">{{ r.id }}</span>
          <span class="ref-date">{{ r.date }}</span>
        </div>
        <div class="ref-desc">{{ r.desc }}</div>
        <div class="ref-foot">
          <span class="ref-foot-l">置信度</span>
          <span class="ref-conf">{{ r.conf }}</span>
          <div class="ref-bar"><span :style="{width: r.conf}"></span></div>
        </div>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useDataStore } from '@/stores/data'
import ReasoningGraph from '@/components/ReasoningGraph.vue'

const store = useDataStore()

const diagList = computed(() => {
  return store.devices.map(d => {
    const alarm = store.alarms.find(a => a.device === d.name && a.st !== 'resolved')
    return {
      id: d.id, name: d.name, dept: d.dept, unit: d.unit,
      fault: alarm ? alarm.desc : '当前无报警'
    }
  })
})

const uName = (uid) => store.units.find(u => u.id === uid)?.name || uid
const curDev = computed(() => store.deviceById(did.value))

const did = ref(store.devices[0]?.id || '')

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

// 通用兜底：根据设备健康度动态生成诊断结论
const defaultDiagnosis = (d) => {
  const h = d.health
  if (h >= 90) {
    return {
      name: d.name, fault: '该设备暂无历史故障记录',
      analysis: `当前 ${uName(d.unit)} · ${d.dept} · ${d.name} 健康度 ${h.toFixed(1)}，运行状态良好，未触发深度诊断分析。AI 将持续监控该设备的关键参数。`,
      causes: [], guide: ['继续保持当前运行参数', '加强日常巡检频次'], refs: []
    }
  }
  if (h >= 80) {
    return {
      name: d.name, fault: '健康度偏低，需加强关注',
      analysis: `${uName(d.unit)} · ${d.dept} · ${d.name} 健康度 ${h.toFixed(1)}，处于需关注区间。建议结合历史趋势分析潜在风险。`,
      causes: [{ ref: 'HEALTH-1', conf: '85%', text: '健康度接近预警阈值' }],
      guide: ['加强设备巡检频次', '记录关键参数变化趋势', '必要时安排预防性检修'],
      refs: []
    }
  }
  return {
    name: d.name, fault: '健康度超限预警',
    analysis: `${uName(d.unit)} · ${d.dept} · ${d.name} 健康度仅 ${h.toFixed(1)}，已低于预警阈值 80。可能存在潜在故障，建议立即排查。`,
    causes: [{ ref: 'HEALTH-CRIT', conf: '92%', text: '健康度持续低于 80，需立即排查' }],
    guide: ['立即通知运维班组', '降低设备负荷或停机检查', '调取近期历史曲线辅助分析', '24 小时内完成检修评估'],
    refs: []
  }
}

const cur = computed(() => {
  if (allDiagnoses[did.value]) return allDiagnoses[did.value]
  const d = store.devices.find(x => x.id === did.value)
  return d ? defaultDiagnosis(d) : { name: '请选择设备', fault: '', analysis: '', causes: [], guide: [], refs: [] }
})

// 各设备对应的分层推理图
const graphCases = {
  'A引风机': {
    title: 'A引风机轴承温度异常',
    nodes: [
      { id: 'u1', label: 'A引风机轴承温度>82℃', type: 'user', layer: 0, sub: 0, w: 220, h: 36 },
      { id: 's1', label: 'A引风机轴承', type: 'symptom', layer: 1, sub: 0, w: 130, h: 32 },
      { id: 'm1', label: '润滑油脂劣化', type: 'middle', layer: 2, sub: 0, w: 110, h: 32 },
      { id: 'm2', label: '冷却水管路堵塞', type: 'middle', layer: 2, sub: 1, w: 130, h: 32 },
      { id: 'm3', label: '轴向载荷异常', type: 'middle', layer: 2, sub: 2, w: 120, h: 32 },
      { id: 'c1', label: '润滑失效', type: 'cause', layer: 3, sub: 0, w: 100, h: 40, pct: 94 },
      { id: 'c2', label: '冷却不足', type: 'cause', layer: 3, sub: 1, w: 100, h: 40, pct: 88 },
      { id: 'c3', label: '轴承内圈磨损', type: 'cause', layer: 3, sub: 2, w: 130, h: 40, pct: 71 },
      { id: 'r1', label: '更换油脂 100%', type: 'solution', layer: 4, sub: 0, w: 110, h: 28 },
      { id: 'r2', label: '清洗滤网 100%', type: 'solution', layer: 4, sub: 0, w: 110, h: 28 },
      { id: 'r3', label: '检修冷却器 100%', type: 'solution', layer: 4, sub: 1, w: 120, h: 28 },
      { id: 'r4', label: '清理管路 100%', type: 'solution', layer: 4, sub: 1, w: 110, h: 28 },
      { id: 'r5', label: '动平衡校正 100%', type: 'solution', layer: 4, sub: 2, w: 120, h: 28 },
      { id: 'r6', label: '更换轴承 100%', type: 'solution', layer: 4, sub: 2, w: 110, h: 28 }
    ],
    rels: [
      { from: 'u1', to: 's1', type: '触发' },
      { from: 's1', to: 'm1', type: '导致' }, { from: 's1', to: 'm2', type: '导致' }, { from: 's1', to: 'm3', type: '导致' },
      { from: 'm1', to: 'c1', type: '由...导致' }, { from: 'm2', to: 'c2', type: '由...导致' }, { from: 'm3', to: 'c3', type: '由...导致' },
      { from: 'c1', to: 'r1', type: '解决' }, { from: 'c1', to: 'r2', type: '解决' },
      { from: 'c2', to: 'r3', type: '解决' }, { from: 'c2', to: 'r4', type: '解决' },
      { from: 'c3', to: 'r5', type: '解决' }, { from: 'c3', to: 'r6', type: '解决' }
    ]
  },
  'A磨煤机': {
    title: 'A磨煤机振动幅值超标',
    nodes: [
      { id: 'u1', label: 'A磨煤机振动>4.7mm/s', type: 'user', layer: 0, sub: 0, w: 220, h: 36 },
      { id: 's1', label: 'A磨煤机', type: 'symptom', layer: 1, sub: 0, w: 100, h: 32 },
      { id: 'm1', label: '磨辊磨损', type: 'middle', layer: 2, sub: 0, w: 100, h: 32 },
      { id: 'm2', label: '煤质硬度偏高', type: 'middle', layer: 2, sub: 1, w: 120, h: 32 },
      { id: 'm3', label: '基础松动', type: 'middle', layer: 2, sub: 2, w: 100, h: 32 },
      { id: 'c1', label: '磨辊磨损', type: 'cause', layer: 3, sub: 0, w: 100, h: 40, pct: 92 },
      { id: 'c2', label: '煤质变差', type: 'cause', layer: 3, sub: 1, w: 100, h: 40, pct: 83 },
      { id: 'c3', label: '地脚螺栓松动', type: 'cause', layer: 3, sub: 2, w: 130, h: 40, pct: 65 },
      { id: 'r1', label: '更换磨辊 100%', type: 'solution', layer: 4, sub: 0, w: 120, h: 28 },
      { id: 'r2', label: '调整给煤量 100%', type: 'solution', layer: 4, sub: 0, w: 130, h: 28 },
      { id: 'r3', label: '调整给煤量 100%', type: 'solution', layer: 4, sub: 1, w: 130, h: 28 },
      { id: 'r4', label: '煤质掺配 100%', type: 'solution', layer: 4, sub: 1, w: 110, h: 28 },
      { id: 'r5', label: '紧固螺栓 100%', type: 'solution', layer: 4, sub: 2, w: 110, h: 28 },
      { id: 'r6', label: '重新找正 100%', type: 'solution', layer: 4, sub: 2, w: 110, h: 28 }
    ],
    rels: [
      { from: 'u1', to: 's1', type: '触发' },
      { from: 's1', to: 'm1', type: '导致' }, { from: 's1', to: 'm2', type: '导致' }, { from: 's1', to: 'm3', type: '导致' },
      { from: 'm1', to: 'c1', type: '由...导致' }, { from: 'm2', to: 'c2', type: '由...导致' }, { from: 'm3', to: 'c3', type: '由...导致' },
      { from: 'c1', to: 'r1', type: '解决' }, { from: 'c1', to: 'r2', type: '解决' },
      { from: 'c2', to: 'r3', type: '解决' }, { from: 'c2', to: 'r4', type: '解决' },
      { from: 'c3', to: 'r5', type: '解决' }, { from: 'c3', to: 'r6', type: '解决' }
    ]
  },
  '给水泵B': {
    title: '给水泵B效率下降',
    nodes: [
      { id: 'u1', label: '给水泵B效率<85%', type: 'user', layer: 0, sub: 0, w: 220, h: 36 },
      { id: 's1', label: '给水泵B', type: 'symptom', layer: 1, sub: 0, w: 100, h: 32 },
      { id: 'm1', label: '叶轮入口气蚀', type: 'middle', layer: 2, sub: 0, w: 120, h: 32 },
      { id: 'm2', label: '密封环磨损', type: 'middle', layer: 2, sub: 1, w: 120, h: 32 },
      { id: 'c1', label: '气蚀', type: 'cause', layer: 3, sub: 0, w: 100, h: 40, pct: 88 },
      { id: 'c2', label: '内泄漏', type: 'cause', layer: 3, sub: 1, w: 100, h: 40, pct: 76 },
      { id: 'r1', label: '提高入口压力 100%', type: 'solution', layer: 4, sub: 0, w: 130, h: 28 },
      { id: 'r2', label: '检修叶轮 100%', type: 'solution', layer: 4, sub: 0, w: 110, h: 28 },
      { id: 'r3', label: '更换密封环 100%', type: 'solution', layer: 4, sub: 1, w: 130, h: 28 }
    ],
    rels: [
      { from: 'u1', to: 's1', type: '触发' },
      { from: 's1', to: 'm1', type: '导致' }, { from: 's1', to: 'm2', type: '导致' },
      { from: 'm1', to: 'c1', type: '由...导致' }, { from: 'm2', to: 'c2', type: '由...导致' },
      { from: 'c1', to: 'r1', type: '解决' }, { from: 'c1', to: 'r2', type: '解决' },
      { from: 'c2', to: 'r3', type: '解决' }
    ]
  }
}

const defaultGraph = (name, health) => {
  const status = health >= 90 ? '状态良好' : health >= 80 ? '需关注' : '存在风险'
  return {
    title: name + ' 状态评估',
    nodes: [
      { id: 'u1', label: name + ' ' + status, type: 'user', layer: 0, sub: 0, w: 220, h: 36 },
      { id: 's1', label: name, type: 'symptom', layer: 1, sub: 0, w: 100, h: 32 },
      { id: 'c1', label: status, type: 'cause', layer: 3, sub: 0, w: 100, h: 40, pct: Math.round(health) },
      { id: 'r1', label: '继续监控 100%', type: 'solution', layer: 4, sub: 0, w: 120, h: 28 }
    ],
    rels: [
      { from: 'u1', to: 's1', type: '触发' },
      { from: 's1', to: 'c1', type: '由...导致' },
      { from: 'c1', to: 'r1', type: '解决' }
    ]
  }
}

const currentGraph = computed(() => {
  const c = cur.value
  if (graphCases[c.name]) return graphCases[c.name]
  const d = store.devices.find(x => x.name === c.name)
  return defaultGraph(c.name, d?.health || 100)
})

const selNodeId = ref(null)
</script>

<style scoped>
.dg-page { display: flex; flex-direction: column; gap: 14px; min-height: 100%; }
.dg-head { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; }
.dg-head-l { display: flex; align-items: center; gap: 12px; }
.dg-bar { width: 4px; height: 22px; background: linear-gradient(180deg, #3eaaff, #22d3ee); border-radius: 2px; box-shadow: 0 0 10px rgba(62,170,255,0.5); }
.dg-head h2 { font-size: 17px; font-weight: 600; color: #d4ecff; letter-spacing: 0.5px; }
.dg-sub { font-size: 11px; color: #8fb0cf; }
.dg-dev-sel { display: flex; align-items: center; gap: 10px; }
.dg-dev-lbl { font-size: 12px; color: #8fb0cf; white-space: nowrap; }
.ut-ic { color: #3eaaff; font-weight: 700; margin-right: 2px; }

.dg-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.dg-diag { display: flex; flex-direction: column; }
.dg-kg { display: flex; flex-direction: column; }
.dg-kg .cd-t { flex-shrink: 0; }
.kg-chart { flex: 1; min-height: 460px; background: linear-gradient(180deg, rgba(6,18,36,0.4), rgba(8,22,42,0.25)); border-radius: 6px; }

/* 诊断对象 */
.dg-obj { background: linear-gradient(135deg, rgba(10,24,42,0.6), rgba(6,16,30,0.55)); border: 1px solid rgba(62,170,255,0.15); border-radius: 6px; padding: 14px 16px; margin-bottom: 14px; box-shadow: 0 2px 12px rgba(0,10,30,0.2); }
.dg-obj-l { font-size: 12px; color: #9fb6cf; margin-bottom: 6px; font-weight: 500; }
.dg-obj-v { font-size: 16px; font-weight: 600; color: #e2e8f0; }
.dg-fault { color: #fbbf24; }

.dg-sect { margin-bottom: 14px; }
.dg-sect-h { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #d4ecff; margin-bottom: 10px; }
.dg-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.dg-dot.ok { background: #3eaaff; box-shadow: 0 0 8px #3eaaff; }
.dg-dot.warn { background: #fbbf24; box-shadow: 0 0 8px #fbbf24; }
.dg-dot.suc { background: #34d399; box-shadow: 0 0 8px #34d399; }
.dg-ans { font-size: 13px; color: #cbd5e1; line-height: 2; background: linear-gradient(180deg, rgba(6,18,36,0.7), rgba(8,22,42,0.5)); border-left: 3px solid rgba(62,170,255,0.4); padding: 12px 14px; border-radius: 0 6px 6px 0; box-shadow: inset 0 0 20px rgba(62,170,255,0.03); }

.dg-causes { display: flex; flex-direction: column; gap: 10px; }
.dg-cause { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #cbd5e1; background: linear-gradient(180deg, rgba(6,18,36,0.7), rgba(8,22,42,0.5)); padding: 10px 12px; border-radius: 6px; border: 1px solid rgba(62,170,255,0.1); transition: all 0.2s; }
.dg-cause:hover { border-color: rgba(62,170,255,0.25); transform: translateX(2px); }
.dg-cause-conf { font-family: "SF Mono","Consolas",monospace; color: #fbbf24; font-weight: 600; }
.dg-cause-txt { color: #cbd5e1; }

.dg-guide { display: flex; flex-direction: column; gap: 8px; }
.dg-guide-i { display: flex; align-items: flex-start; gap: 10px; font-size: 13px; color: #cbd5e1; background: linear-gradient(90deg, rgba(52,211,153,0.08), transparent); padding: 10px 12px; border-radius: 6px; border-left: 2px solid rgba(52,211,153,0.3); transition: all 0.2s; }
.dg-guide-i:hover { background: linear-gradient(90deg, rgba(52,211,153,0.12), transparent); border-left-color: rgba(52,211,153,0.5); }
.dg-gn { width: 20px; height: 20px; flex-shrink: 0; border-radius: 50%; background: rgba(52,211,153,0.18); color: #34d399; border: 1.5px solid rgba(52,211,153,0.4); display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 800; box-shadow: 0 0 8px rgba(52,211,153,0.2); }
.dg-gt { line-height: 1.6; padding-top: 1px; }

/* 引用溯源 */
.dg-ref { margin-top: 0; }
.ref-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 10px; }
.ref { background: linear-gradient(180deg, rgba(10,24,42,0.6), rgba(6,16,32,0.55)); padding: 14px 16px; border-radius: 6px; border: 1px solid rgba(62,170,255,0.14); transition: all 0.25s; }
.ref:hover { border-color: rgba(62,170,255,0.35); transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,10,30,0.3); }
.ref-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.ref-date { font-size: 11px; color: #8fb0cf; font-family: "SF Mono","Consolas",monospace; }
.ref-desc { color: #cbd5e1; margin: 6px 0; line-height: 1.6; font-size: 12px; }
.ref-foot { display: flex; align-items: center; gap: 8px; }
.ref-foot-l { font-size: 11px; color: #8fb0cf; }
.ref-conf { font-size: 12px; color: #3eaaff; font-weight: 700; font-family: "SF Mono","Consolas",monospace; }
.ref-bar { flex: 1; height: 4px; border-radius: 2px; background: rgba(62,170,255,0.1); overflow: hidden; }
.ref-bar span { display: block; height: 100%; background: linear-gradient(90deg, #3eaaff, #22d3ee); border-radius: 2px; }

.tg { display: inline-block; padding: 3px 10px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.tg-w { background: rgba(245,158,11,0.15); color: #fbbf24; border: 1px solid rgba(245,158,11,0.25); }
.tg-i { background: rgba(62,170,255,0.15); color: #3eaaff; border: 1px solid rgba(62,170,255,0.25); }
</style>
