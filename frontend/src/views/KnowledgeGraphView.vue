<template>
<div class="kg-page">
  <!-- 实时告警联动条：检测到匹配告警时自动弹出 -->
  <div v-if="liveAlarm" class="kg-alarmbar">
    <div class="kg-ab-left">
      <span class="kg-ab-ic">⚠</span>
      <span class="kg-ab-t">实时告警联动</span>
      <span class="kg-ab-d">{{ liveAlarm.device }} · {{ liveAlarm.desc }}（{{ liveAlarm.val }}）</span>
      <span class="kg-ab-tag" :class="'st-' + liveAlarm.st">{{ statusLabel(liveAlarm.st) }}</span>
    </div>
    <div class="kg-ab-right">
      <el-button size="small" :disabled="liveAlarm.st !== 'unhandled'" @click="markStatus('confirmed')">标记处理中</el-button>
      <el-button size="small" type="primary" :disabled="liveAlarm.st === 'resolved'" @click="markStatus('resolved')">标记已处置</el-button>
      <el-button size="small" link type="primary" @click="goDiagnosis">查看诊断</el-button>
    </div>
  </div>

  <el-row :gutter="14">
    <el-col :span="5">
      <div class="cd kg-left-col">
        <div class="cd-t">推理图分类</div>
        <el-menu :default-active="cat" @select="onSelect" class="kg-menu">
          <el-menu-item index="推理案例1"><span class="kg-mi">给水泵油压异常<em v-if="caseHasAlarm('推理案例1')" class="kg-dot"></em></span></el-menu-item>
          <el-menu-item index="推理案例2"><span class="kg-mi">引风机轴承温度高<em v-if="caseHasAlarm('推理案例2')" class="kg-dot"></em></span></el-menu-item>
          <el-menu-item index="推理案例3"><span class="kg-mi">磨煤机振动超标<em v-if="caseHasAlarm('推理案例3')" class="kg-dot"></em></span></el-menu-item>
          <el-menu-item index="推理案例4"><span class="kg-mi">主汽温度偏高<em v-if="caseHasAlarm('推理案例4')" class="kg-dot"></em></span></el-menu-item>
        </el-menu>
        <div class="kg-stats-section">
          <div class="cd-t">推理统计</div>
          <div class="st-row"><span>用户问题</span><strong style="color:#8b5cf6">{{ stats.user }}</strong></div>
          <div class="st-row"><span>症状</span><strong style="color:#fbbf24">{{ stats.symptom }}</strong></div>
          <div class="st-row"><span>中间现象</span><strong style="color:#fbbf24">{{ stats.middle }}</strong></div>
          <div class="st-row"><span>根本原因</span><strong style="color:#ef4444">{{ stats.cause }}</strong></div>
          <div class="st-row"><span>解决方案</span><strong style="color:#34d399">{{ stats.solution }}</strong></div>
        </div>
      </div>
    </el-col>
    <el-col :span="19">
      <div class="kg-right-col">
        <div class="cd kg-graph-card">
          <div class="kg-toolbar">
            <span style="font-size:13px;color:#e2e8f0;font-weight:500">知识图谱推理</span>
            <el-button-group>
              <el-button :type="cat==='推理案例1'?'primary':''" size="small" @click="onSelect('推理案例1')">给水泵油压<em v-if="caseHasAlarm('推理案例1')" class="kg-bdot"></em></el-button>
              <el-button :type="cat==='推理案例2'?'primary':''" size="small" @click="onSelect('推理案例2')">引风机轴承<em v-if="caseHasAlarm('推理案例2')" class="kg-bdot"></em></el-button>
              <el-button :type="cat==='推理案例3'?'primary':''" size="small" @click="onSelect('推理案例3')">磨煤机振动<em v-if="caseHasAlarm('推理案例3')" class="kg-bdot"></em></el-button>
              <el-button :type="cat==='推理案例4'?'primary':''" size="small" @click="onSelect('推理案例4')">主汽温度<em v-if="caseHasAlarm('推理案例4')" class="kg-bdot"></em></el-button>
            </el-button-group>
          </div>
          <div class="kg-canvas">
            <ReasoningGraph :case-data="cases[cat]" :selected-id="selectedId" :highlight-ids="pathIds" @update:selected-id="val => selectedId = val" @select="onNodeSel" />
          </div>
        </div>
        <div class="cd kg-detail">
          <div class="cd-t">推理详情与处置指导</div>
          <div v-if="selected">
            <el-row :gutter="14">
              <el-col :span="6"><div class="dt-card"><div class="dt-l">节点</div><div class="dt-v">{{ selected.label }}</div></div></el-col>
              <el-col :span="6"><div class="dt-card"><div class="dt-l">类型</div><div class="dt-v" :style="{color: COL[selected.type]?.color}">{{ typeName(selected.type) }}</div></div></el-col>
              <el-col :span="6"><div class="dt-card"><div class="dt-l">关联关系</div><div class="dt-v">{{ relationsOf(selected.id).length }} 条</div></div></el-col>
              <el-col :span="6">
                <div class="dt-card">
                  <div class="dt-l">操作</div>
            <div class="dt-v">
              <el-button link type="primary" size="small" @click="goDiagnosis">查看诊断</el-button>
              <el-button link type="primary" size="small" @click="goCondition">工况分析</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
      <div v-if="relationsOf(selected.id).length" style="margin-top:12px">
        <div class="dt-rels">
          <div v-for="(r, i) in relationsOf(selected.id)" :key="i" class="dt-rel">
            <span class="dt-rel-from" :style="{color: r.fromColor}">{{ r.from }}</span>
            <span class="dt-rel-arrow">{{ r.type }}</span>
            <span class="dt-rel-to" :style="{color: r.toColor}">{{ r.to }}</span>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-hint">点击图谱中的节点查看详情</div>

          <!-- 结构化操作指导：处置步骤 → 风险提示 → 规程依据 -->
          <div class="kg-guide">
            <div class="kg-guide-h">
              <span>📋 结构化操作指导</span>
              <span class="kg-guide-sub">处置步骤 → 风险提示 → 规程依据</span>
            </div>
            <div class="kg-guide-grid">
              <div class="gcol gcol-step">
                <div class="gcol-h">处置步骤</div>
                <ol class="glist">
                  <li v-for="(s, i) in guidance.steps" :key="i">{{ s }}</li>
                </ol>
              </div>
              <div class="gcol gcol-risk">
                <div class="gcol-h">风险提示</div>
                <ul class="glist">
                  <li v-for="(s, i) in guidance.risks" :key="i">{{ s }}</li>
                </ul>
              </div>
              <div class="gcol gcol-basis">
                <div class="gcol-h">规程依据</div>
                <ul class="glist">
                  <li v-for="(s, i) in guidance.basis" :key="i">{{ s }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-col>
  </el-row>
</div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ReasoningGraph from '@/components/ReasoningGraph.vue'
import { useDataStore } from '@/stores/data'

const router = useRouter()
const store = useDataStore()
const cat = ref('推理案例1')
const selectedId = ref(null)

const COL = {
  user: { color: '#6d28d9' }, symptom: { color: '#b45309' },
  middle: { color: '#854d0e' }, cause: { color: '#b91c1c' }, solution: { color: '#15803d' }
}

// 4 个推理案例（与 ReasoningGraph 组件共享）
const cases = {
  '推理案例1': {
    title: '给水泵A#1给油压力小于5.5',
    nodes: [
      { id: 'u1', label: '给水泵A#1给油压力小于5.5', type: 'user', layer: 0, sub: 0, w: 230, h: 36 },
      { id: 's1', label: '给水泵', type: 'symptom', layer: 1, sub: 0, w: 100, h: 32 },
      { id: 'm1', label: '清理泵进口滤网', type: 'middle', layer: 2, sub: 0, w: 130, h: 32 },
      { id: 'm2', label: '摩擦增大', type: 'middle', layer: 2, sub: 1, w: 100, h: 32 },
      { id: 'm3', label: '预测模型未考虑驱动端特性', type: 'middle', layer: 2, sub: 2, w: 200, h: 32 },
      { id: 'c1', label: '清理泵进口滤网', type: 'cause', layer: 3, sub: 0, w: 130, h: 40, pct: 95 },
      { id: 'c2', label: '摩擦增大', type: 'cause', layer: 3, sub: 1, w: 100, h: 40, pct: 95 },
      { id: 'c3', label: '预测模型未考虑驱动端...', type: 'cause', layer: 3, sub: 2, w: 200, h: 40, pct: 95 },
      { id: 'r1', label: '压力 100%', type: 'solution', layer: 4, sub: 0, w: 90, h: 28 },
      { id: 'r2', label: '油温 100%', type: 'solution', layer: 4, sub: 0, w: 90, h: 28 },
      { id: 'r3', label: '流量 100%', type: 'solution', layer: 4, sub: 0, w: 90, h: 28 },
      { id: 'r4', label: '压力 100%', type: 'solution', layer: 4, sub: 1, w: 90, h: 28 },
      { id: 'r5', label: '油温 100%', type: 'solution', layer: 4, sub: 1, w: 90, h: 28 },
      { id: 'r6', label: '流量 100%', type: 'solution', layer: 4, sub: 1, w: 90, h: 28 },
      { id: 'r7', label: '压力 100%', type: 'solution', layer: 4, sub: 2, w: 90, h: 28 },
      { id: 'r8', label: '油温 100%', type: 'solution', layer: 4, sub: 2, w: 90, h: 28 },
      { id: 'r9', label: '流量 100%', type: 'solution', layer: 4, sub: 2, w: 90, h: 28 }
    ],
    rels: [
      { from: 'u1', to: 's1', type: '触发' },
      { from: 's1', to: 'm1', type: '导致' }, { from: 's1', to: 'm2', type: '导致' }, { from: 's1', to: 'm3', type: '导致' },
      { from: 'm1', to: 'c1', type: '由...导致' }, { from: 'm2', to: 'c2', type: '由...导致' }, { from: 'm3', to: 'c3', type: '由...导致' },
      { from: 'c1', to: 'r1', type: '解决' }, { from: 'c1', to: 'r2', type: '解决' }, { from: 'c1', to: 'r3', type: '解决' },
      { from: 'c2', to: 'r4', type: '解决' }, { from: 'c2', to: 'r5', type: '解决' }, { from: 'c2', to: 'r6', type: '解决' },
      { from: 'c3', to: 'r7', type: '解决' }, { from: 'c3', to: 'r8', type: '解决' }, { from: 'c3', to: 'r9', type: '解决' }
    ]
  },
  '推理案例2': {
    title: 'A引风机轴承温度异常升高',
    nodes: [
      { id: 'u1', label: 'A引风机轴承温度>82℃', type: 'user', layer: 0, sub: 0, w: 220, h: 36 },
      { id: 's1', label: 'A引风机轴承', type: 'symptom', layer: 1, sub: 0, w: 120, h: 32 },
      { id: 'm1', label: '润滑油脂劣化', type: 'middle', layer: 2, sub: 0, w: 110, h: 32 },
      { id: 'm2', label: '冷却水管路堵塞', type: 'middle', layer: 2, sub: 1, w: 130, h: 32 },
      { id: 'm3', label: '轴向载荷异常', type: 'middle', layer: 2, sub: 2, w: 120, h: 32 },
      { id: 'c1', label: '润滑失效', type: 'cause', layer: 3, sub: 0, w: 100, h: 40, pct: 88 },
      { id: 'c2', label: '冷却不足', type: 'cause', layer: 3, sub: 1, w: 100, h: 40, pct: 76 },
      { id: 'c3', label: '叶轮积灰失衡', type: 'cause', layer: 3, sub: 2, w: 120, h: 40, pct: 65 },
      { id: 'r1', label: '更换油脂 100%', type: 'solution', layer: 4, sub: 0, w: 110, h: 28 },
      { id: 'r2', label: '清洗滤网 100%', type: 'solution', layer: 4, sub: 0, w: 110, h: 28 },
      { id: 'r3', label: '检修冷却器 100%', type: 'solution', layer: 4, sub: 1, w: 120, h: 28 },
      { id: 'r4', label: '清理管路 100%', type: 'solution', layer: 4, sub: 1, w: 110, h: 28 },
      { id: 'r5', label: '动平衡校正 100%', type: 'solution', layer: 4, sub: 2, w: 120, h: 28 },
      { id: 'r6', label: '检查叶轮 100%', type: 'solution', layer: 4, sub: 2, w: 110, h: 28 }
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
  '推理案例3': {
    title: 'A磨煤机振动幅值超标',
    nodes: [
      { id: 'u1', label: 'A磨煤机振动>4.7mm/s', type: 'user', layer: 0, sub: 0, w: 220, h: 36 },
      { id: 's1', label: 'A磨煤机', type: 'symptom', layer: 1, sub: 0, w: 100, h: 32 },
      { id: 'm1', label: '磨辊磨损', type: 'middle', layer: 2, sub: 0, w: 100, h: 32 },
      { id: 'm2', label: '煤质硬度偏高', type: 'middle', layer: 2, sub: 1, w: 120, h: 32 },
      { id: 'm3', label: '基础松动', type: 'middle', layer: 2, sub: 2, w: 100, h: 32 },
      { id: 'c1', label: '磨辊磨损', type: 'cause', layer: 3, sub: 0, w: 100, h: 40, pct: 92 },
      { id: 'c2', label: '煤质变差', type: 'cause', layer: 3, sub: 1, w: 100, h: 40, pct: 81 },
      { id: 'c3', label: '地脚螺栓松动', type: 'cause', layer: 3, sub: 2, w: 130, h: 40, pct: 58 },
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
  '推理案例4': {
    title: '锅炉主汽温度偏高',
    nodes: [
      { id: 'u1', label: '锅炉主汽温度>552℃', type: 'user', layer: 0, sub: 0, w: 220, h: 36 },
      { id: 's1', label: '锅炉主汽', type: 'symptom', layer: 1, sub: 0, w: 100, h: 32 },
      { id: 'm1', label: '减温水量不足', type: 'middle', layer: 2, sub: 0, w: 120, h: 32 },
      { id: 'm2', label: '燃料量过大', type: 'middle', layer: 2, sub: 1, w: 100, h: 32 },
      { id: 'm3', label: '给水温度偏低', type: 'middle', layer: 2, sub: 2, w: 120, h: 32 },
      { id: 'c1', label: '减温水调节阀卡涩', type: 'cause', layer: 3, sub: 0, w: 150, h: 40, pct: 84 },
      { id: 'c2', label: '给煤量异常', type: 'cause', layer: 3, sub: 1, w: 120, h: 40, pct: 72 },
      { id: 'c3', label: '高加端差大', type: 'cause', layer: 3, sub: 2, w: 120, h: 40, pct: 68 },
      { id: 'r1', label: '检修调节阀 100%', type: 'solution', layer: 4, sub: 0, w: 130, h: 28 },
      { id: 'r2', label: '加大减温水 100%', type: 'solution', layer: 4, sub: 0, w: 130, h: 28 },
      { id: 'r3', label: '调整给煤量 100%', type: 'solution', layer: 4, sub: 1, w: 130, h: 28 },
      { id: 'r4', label: '降低燃料率 100%', type: 'solution', layer: 4, sub: 1, w: 130, h: 28 },
      { id: 'r5', label: '清理高加 100%', type: 'solution', layer: 4, sub: 2, w: 120, h: 28 },
      { id: 'r6', label: '提高给水温度 100%', type: 'solution', layer: 4, sub: 2, w: 140, h: 28 }
    ],
    rels: [
      { from: 'u1', to: 's1', type: '触发' },
      { from: 's1', to: 'm1', type: '导致' }, { from: 's1', to: 'm2', type: '导致' }, { from: 's1', to: 'm3', type: '导致' },
      { from: 'm1', to: 'c1', type: '由...导致' }, { from: 'm2', to: 'c2', type: '由...导致' }, { from: 'm3', to: 'c3', type: '由...导致' },
      { from: 'c1', to: 'r1', type: '解决' }, { from: 'c1', to: 'r2', type: '解决' },
      { from: 'c2', to: 'r3', type: '解决' }, { from: 'c2', to: 'r4', type: '解决' },
      { from: 'c3', to: 'r5', type: '解决' }, { from: 'c3', to: 'r6', type: '解决' }
    ]
  }
}

const selected = computed(() => {
  if (!selectedId.value) return null
  const data = cases[cat.value]
  return data?.nodes.find(n => n.id === selectedId.value) || null
})

const stats = computed(() => {
  const data = cases[cat.value]
  if (!data) return { user: 0, symptom: 0, middle: 0, cause: 0, solution: 0 }
  const c = { user: 0, symptom: 0, middle: 0, cause: 0, solution: 0 }
  data.nodes.forEach(n => { c[n.type] = (c[n.type] || 0) + 1 })
  return c
})

const onNodeSel = (n) => { selectedId.value = n.id }

const typeName = (t) => ({ user: '用户问题', symptom: '症状', middle: '中间现象', cause: '根本原因', solution: '解决方案' }[t] || '未知')

const relationsOf = (id) => {
  const data = cases[cat.value]
  if (!data) return []
  return data.rels.filter(r => r.from === id || r.to === id).map(r => {
    const a = data.nodes.find(n => n.id === r.from)
    const b = data.nodes.find(n => n.id === r.to)
    return {
      from: a?.label, to: b?.label, type: r.type,
      fromColor: COL[a?.type]?.color, toColor: COL[b?.type]?.color
    }
  })
}

const onSelect = (idx) => { cat.value = idx; selectedId.value = null }

// ════════════════════════════════════════
// 实时告警联动 —— 告警触发后自动定位推理路径
// ════════════════════════════════════════
const caseAlarmMap = {
  '推理案例1': ['润滑油系统', '润滑油压力', '给水泵', '油压'],
  '推理案例2': ['引风机', '轴承温度'],
  '推理案例3': ['磨煤机', '振动'],
  '推理案例4': ['主汽温度', '主蒸汽', '锅炉主汽']
}
const findCaseForAlarm = (a) => {
  for (const [key, kws] of Object.entries(caseAlarmMap)) {
    if (kws.some(k => (a.device + a.point + a.desc).includes(k))) return key
  }
  return null
}
// 取第一条未处置/处理中的、能匹配到推理案例的实时告警
const liveAlarm = computed(() => {
  const actives = store.alarms.filter(a => a.st === 'unhandled' || a.st === 'confirmed')
  for (const a of actives) {
    const key = findCaseForAlarm(a)
    if (key) return { ...a, caseKey: key }
  }
  return null
})
const caseHasAlarm = (key) => !!liveAlarm.value && liveAlarm.value.caseKey === key

// 告警命中时，自动切换到对应推理案例并选中症状节点，弹出推理路径与根因
watch(liveAlarm, (a) => {
  if (a) {
    cat.value = a.caseKey
    selectedId.value = 's1'
  }
}, { immediate: true })

// 推理路径高亮（症状 → 原因 → 方案）
const pathIds = computed(() => {
  if (liveAlarm.value && liveAlarm.value.caseKey === cat.value) {
    const data = cases[cat.value]
    if (data) return data.nodes.filter(n => ['user', 'symptom', 'middle', 'cause', 'solution'].includes(n.type)).map(n => n.id)
  }
  return []
})

// ════════════════════════════════════════
// 结构化处置指导（处置步骤 → 风险提示 → 规程依据）
// ════════════════════════════════════════
const guidanceMap = {
  '推理案例1': {
    steps: ['立即检查润滑油泵出口压力与滤网压差', '切换至备用润滑油泵并隔离故障泵', '清理或更换泵进口滤网', '确认油压恢复至 0.25MPa 以上后恢复运行'],
    risks: ['油压持续低于 0.12MPa 将导致轴瓦烧损，须立即停机', '切换油泵时防止断油，操作应平稳缓慢'],
    basis: ['《汽轮机运行规程》8.2 润滑油系统运行要求', '《防止电力生产重大事故二十五项重点要求》第 9 项']
  },
  '推理案例2': {
    steps: ['降低引风机负荷，监视轴承温度趋势', '检查润滑油站油位、油质与冷却水温', '切换备用油脂或补充润滑脂', '清理冷却水管路滤网，恢复冷却水量', '温度持续升至 90℃ 申请停风机检修'],
    risks: ['轴承温度 >95℃ 有抱死风险，须紧急停运', '清理冷却管路时防止带压作业'],
    basis: ['《锅炉运行规程》引风机轴承温度保护定值', '《旋转电机预防性试验规程》']
  },
  '推理案例3': {
    steps: ['降低磨煤机出力，监视振动幅值', '检查磨辊磨损与加载力设定', '调整给煤量与煤质掺配，避免硬质煤', '检查地脚螺栓紧固状态', '振动 >6.5mm/s 申请停磨检修'],
    risks: ['振动超标伴异响可能为磨辊脱落先兆', '紧固螺栓需断电挂牌后作业'],
    basis: ['《制粉系统运行规程》磨煤机振动保护', '《火力发电厂设备检修规程》']
  },
  '推理案例4': {
    steps: ['增加减温水投入量，监视主汽温度', '检查减温水调节阀开度与卡涩情况', '适当降低燃料量与给煤速率', '检查高加端差与给水温度', '温度 >560℃ 申请降负荷运行'],
    risks: ['主汽温度超限将危及过热器管材寿命', '调整燃料时需协调汽温与汽压'],
    basis: ['《锅炉运行规程》主蒸汽温度控制要求', '《电力锅炉安全技术监察规程》']
  }
}
const guidance = computed(() => guidanceMap[cat.value] || { steps: [], risks: [], basis: [] })

// ════════════════════════════════════════
// 处置闭环 —— 标记并持久化告警处置状态
// ════════════════════════════════════════
const STATUS_LABEL = { unhandled: '未处置', confirmed: '处理中', resolved: '已处置' }
const statusLabel = (s) => STATUS_LABEL[s] || '未知'
const markStatus = (st) => {
  if (liveAlarm.value) {
    store.setAlarmStatus(liveAlarm.value.id, st)
    ElMessage.success('处置状态已更新：' + statusLabel(st))
  }
}

const goDiagnosis = () => router.push('/diagnosis')
const goCondition = () => router.push('/condition')
</script>

<style scoped>
/* ════════════════════════════════════════
   知识图谱 — 工业科技风 v2
   ════════════════════════════════════════ */

/* 页面根容器 — 一屏显示 */
.kg-page { height: 100%; display: flex; flex-direction: column; overflow: hidden; background-color: #04060a; }
.kg-page :deep(.el-row) { flex: 1; min-height: 0; }
.kg-page :deep(.el-col) { height: 100%; }

/* 左右栏布局 */
.kg-left-col { height: 100%; display: flex; flex-direction: column; padding: 16px; }
.kg-right-col { height: 100%; display: flex; flex-direction: column; gap: 12px; }
.kg-graph-card { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.kg-canvas { flex: 1; min-height: 0; overflow: hidden; }
.kg-stats-section { margin-top: 14px; padding-top: 14px; border-top: 0.5px solid rgba(62,170,255,0.12); flex: 1; min-height: 0; overflow-y: auto; }
.kg-detail { flex: 0 0 auto; max-height: 360px; overflow: auto; }

/* 通用面板 — 科技风 */
.cd {
  background: linear-gradient(180deg, rgba(10,24,44,0.7), rgba(6,16,30,0.65));
  border: 1px solid rgba(62,170,255,0.15);
  border-radius: 8px; padding: 16px;
  position: relative;
  box-shadow: 0 4px 20px rgba(0,10,30,0.3), inset 0 1px 0 rgba(62,170,255,0.04);
}
/* 面板四角装饰 */
.cd::before, .cd::after {
  content:''; position:absolute; width:14px; height:14px;
  border:1.5px solid #3eaaff; pointer-events:none;
  box-shadow:0 0 6px rgba(62,170,255,0.4); opacity:0.5;
}
.cd::before { top:-1px; left:-1px; border-right:none; border-bottom:none; }
.cd::after { bottom:-1px; right:-1px; border-left:none; border-top:none; }

/* ═══ 仅「推理详情与处置指导」面板纯黑+CRT虚线方格（压制全局 .cd !important） ═══ */
.kg-page .kg-detail.cd {
  background-color: #000000 !important;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px) !important;
  border-color: rgba(90,166,196,0.12) !important;
  box-shadow: none !important;
}
.kg-page .kg-detail.cd .cd-t {
  background: #000000 !important;
  border-bottom-color: rgba(90,166,196,0.15);
}
.kg-detail.cd::before, .kg-detail.cd::after { display: none !important; }

/* 面板标题 */
.cd-t {
  font-size: 13.5px; color: #d4ecff; margin-bottom: 12px;
  font-weight: 600; letter-spacing: 0.5px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(62,170,255,0.12);
  display: flex; align-items: center; gap: 8px;
  text-shadow: 0 0 8px rgba(62,170,255,0.15);
}
.cd-t::before {
  content:''; width:3px; height:14px;
  background:#3eaaff; border-radius:2px;
  box-shadow:0 0 6px rgba(62,170,255,0.5);
}

/* 左侧菜单 */
.kg-menu { border: none !important; background: transparent !important; }
.kg-menu :deep(.el-menu-item) {
  color: #a0bed8 !important;
  background: transparent !important;
  border-radius: 5px; margin-bottom: 3px;
  border: 1px solid transparent;
  transition: all 0.25s;
  font-size: 12px;
}
.kg-menu :deep(.el-menu-item:hover) {
  color: #e0f0ff !important;
  background: rgba(62,170,255,0.08) !important;
  border-color: rgba(62,170,255,0.15) !important;
}
.kg-menu :deep(.el-menu-item.is-active) {
  color: #3eaaff !important;
  background: linear-gradient(90deg, rgba(62,170,255,0.15), transparent) !important;
  border-color: rgba(62,170,255,0.3) !important;
  box-shadow: inset 2px 0 8px -2px rgba(62,170,255,0.3);
}

/* 统计行 */
.st-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 10px; font-size: 11.5px; color: #9fb6cf;
  background: linear-gradient(180deg, rgba(8,20,40,0.45), rgba(6,14,28,0.4));
  border-radius: 4px; margin-bottom: 4px;
  border-left: 2.5px solid transparent;
  transition: all 0.2s;
}
.st-row:hover { background: rgba(62,170,255,0.05); }
.st-row strong { font-family: "SF Mono","Consolas",monospace; font-weight: 700; font-size: 13px; }

/* 工具栏 — 标题与下方 .cd-t 保持一致的左侧蓝条 + 下划线风格 */
.kg-toolbar {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 12px; flex-wrap: wrap;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(62,170,255,0.12);
}
.kg-toolbar span {
  font-size: 13.5px; color: #d4ecff; font-weight: 600;
  letter-spacing: 0.5px;
  display: flex; align-items: center; gap: 8px;
  text-shadow: 0 0 8px rgba(62,170,255,0.15);
}
.kg-toolbar span::before {
  content:''; width:3px; height:14px;
  background:#3eaaff; border-radius:2px;
  box-shadow:0 0 6px rgba(62,170,255,0.5);
}
.kg-toolbar :deep(.el-button-group .el-button) {
  font-size: 11px !important;
  background: rgba(8,20,40,0.55) !important;
  border-color: rgba(62,170,255,0.18) !important;
  color: #a0bed8 !important;
  transition: all 0.2s;
}
.kg-toolbar :deep(.el-button-group .el-button:hover) {
  border-color: #3eaaff !important;
  color: #e0f0ff !important;
  box-shadow: 0 0 8px rgba(62,170,255,0.2);
}
.kg-toolbar :deep(.el-button-group .el-button--primary) {
  background: rgba(62,170,255,0.2) !important;
  border-color: #3eaaff !important;
  color: #e0f0ff !important;
  box-shadow: 0 0 10px rgba(62,170,255,0.2);
}

/* 图谱画布 */
.kg-canvas { flex: 1; min-height: 420px; position: relative; overflow: hidden; }

/* 详情卡片 */
.dt-card {
  background: linear-gradient(180deg, rgba(8,22,42,0.6), rgba(6,16,32,0.55));
  padding: 12px 14px; border-radius: 6px;
  border: 1px solid rgba(62,170,255,0.1);
  transition: all 0.2s; position: relative; overflow: hidden;
}
.dt-card:hover { border-color: rgba(62,170,255,0.25); transform: translateY(-1px); }
.dt-card::before {
  content:''; position:absolute; top:0; left:0; right:0; height:2px;
  background:linear-gradient(90deg,#3eaaff,transparent);
  opacity:0.5;
}
.dt-l { font-size: 10.5px; color: #7a98b4; margin-bottom: 4px; letter-spacing: 0.3px; text-transform: uppercase; }
.dt-v { font-size: 15px; color: #e2e8f0; font-weight: 600; }

/* 关系列表 */
.dt-rels {
  background: linear-gradient(180deg, rgba(8,20,40,0.5), rgba(6,14,28,0.45));
  border-radius: 6px; padding: 10px 12px;
  max-height: 200px; overflow-y: auto;
  border: 1px solid rgba(62,170,255,0.08);
}
.dt-rels::-webkit-scrollbar { width: 3px; }
.dt-rels::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }
.dt-rel {
  display: flex; align-items: center; gap: 10px;
  padding: 7px 10px; font-size: 11.5px; color: #b0c8e0;
  border-bottom: 0.5px solid rgba(62,170,255,0.08);
  border-radius: 4px; transition: all 0.15s;
}
.dt-rel:hover { background: rgba(62,170,255,0.04); transform: translateX(3px); }
.dt-rel-from, .dt-rel-to { font-weight: 600; }
.dt-rel-arrow {
  color: #3eaaff; font-size: 10.5px; font-weight: 500;
  padding: 2px 8px; background: rgba(62,170,255,0.08);
  border-radius: 3px; border: 1px solid rgba(62,170,255,0.15);
  flex-shrink: 0;
}

.empty-hint {
  text-align: center; padding: 48px 20px;
  color: #6a8caa; font-size: 13px;
  background: linear-gradient(180deg, rgba(8,20,40,0.3), transparent);
  border-radius: 6px;
  border: 1px dashed rgba(62,170,255,0.12);
}

/* ════════════════════════════════════════
   实时告警联动 + 结构化指导 新增样式
   ════════════════════════════════════════ */

/* 顶部告警联动条 */
.kg-alarmbar {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 9px 16px; border-radius: 8px;
  background: linear-gradient(90deg, rgba(239,68,68,0.16), rgba(239,68,68,0.04));
  border: 1px solid rgba(239,68,68,0.35);
  box-shadow: 0 2px 14px rgba(239,68,68,0.15), inset 0 1px 0 rgba(239,68,68,0.1);
  margin-bottom: 12px; flex: 0 0 auto;
  animation: kgalm 1.6s ease-in-out infinite;
}
@keyframes kgalm {
  0%, 100% { box-shadow: 0 2px 14px rgba(239,68,68,0.12), inset 0 1px 0 rgba(239,68,68,0.1); }
  50% { box-shadow: 0 2px 20px rgba(239,68,68,0.3), inset 0 1px 0 rgba(239,68,68,0.15); }
}
.kg-ab-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.kg-ab-ic { font-size: 16px; filter: drop-shadow(0 0 6px rgba(239,68,68,0.6)); }
.kg-ab-t { font-size: 12.5px; font-weight: 700; color: #fecaca; letter-spacing: 1px; white-space: nowrap; }
.kg-ab-d { font-size: 12.5px; color: #fde2e2; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kg-ab-tag {
  font-size: 11px; padding: 2px 9px; border-radius: 10px; font-weight: 600; white-space: nowrap;
  border: 1px solid transparent;
}
.kg-ab-tag.st-unhandled { color: #ffb4b4; background: rgba(239,68,68,0.22); border-color: rgba(239,68,68,0.5); }
.kg-ab-tag.st-confirmed { color: #fcd34d; background: rgba(245,158,11,0.18); border-color: rgba(245,158,11,0.45); }
.kg-ab-tag.st-resolved { color: #86efac; background: rgba(34,197,94,0.16); border-color: rgba(34,197,94,0.4); }
.kg-ab-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

/* 菜单/按钮 实时告警指示点 */
.kg-mi { display: flex; align-items: center; gap: 6px; }
.kg-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #ef4444;
  box-shadow: 0 0 6px rgba(239,68,68,0.8); display: inline-block;
  animation: kgalm2 1.2s ease-in-out infinite;
}
@keyframes kgalm2 { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }
.kg-bdot {
  width: 6px; height: 6px; border-radius: 50%; background: #ef4444;
  box-shadow: 0 0 6px rgba(239,68,68,0.8); display: inline-block; margin-left: 4px; vertical-align: middle;
  animation: kgalm2 1.2s ease-in-out infinite;
}

/* 结构化操作指导 */
.kg-guide {
  margin-top: 14px; padding-top: 12px;
  border-top: 1px dashed rgba(62,170,255,0.18);
}
.kg-guide-h {
  display: flex; align-items: baseline; gap: 10px; margin-bottom: 10px;
  font-size: 12.5px; color: #d4ecff; font-weight: 600;
}
.kg-guide-sub { font-size: 11px; color: #6a8caa; font-weight: 400; letter-spacing: 0.5px; }
.kg-guide-grid { display: grid; grid-template-columns: 1.4fr 1fr 1fr; gap: 10px; }
.gcol {
  background: linear-gradient(180deg, rgba(8,22,42,0.55), rgba(6,16,32,0.5));
  border: 1px solid rgba(62,170,255,0.12); border-radius: 6px; padding: 10px 12px;
  min-height: 96px;
}
.gcol-step { border-left: 3px solid #3eaaff; }
.gcol-risk { border-left: 3px solid #f59e0b; }
.gcol-basis { border-left: 3px solid #22c55e; }
.gcol-h {
  font-size: 11.5px; font-weight: 700; margin-bottom: 8px; letter-spacing: 0.5px;
  display: flex; align-items: center; gap: 6px;
}
.gcol-step .gcol-h { color: #7dd3fc; }
.gcol-risk .gcol-h { color: #fcd34d; }
.gcol-basis .gcol-h { color: #86efac; }
.glist { margin: 0; padding-left: 18px; }
.gcol-risk .glist, .gcol-basis .glist { list-style: none; padding-left: 0; }
.glist li {
  font-size: 11.5px; color: #b8cfe6; line-height: 1.7; margin-bottom: 5px;
  position: relative;
}
.gcol-step .glist li::marker { color: #3eaaff; font-weight: 700; }
.gcol-risk .glist li, .gcol-basis .glist li {
  padding-left: 14px;
}
.gcol-risk .glist li::before, .gcol-basis .glist li::before {
  content: ''; position: absolute; left: 0; top: 8px; width: 5px; height: 5px; border-radius: 50%;
}
.gcol-risk .glist li::before { background: #f59e0b; box-shadow: 0 0 5px rgba(245,158,11,0.6); }
.gcol-basis .glist li::before { background: #22c55e; box-shadow: 0 0 5px rgba(34,197,94,0.6); }
</style>

<!-- 非 scoped 覆盖块：压制全局 .cd !important 深蓝锁色 -->
<style>
/* 所有面板内容区纯黑 + CRT 虚线方格 */
.kg-page .kg-left-col,
.kg-page .kg-graph-card,
.kg-page .kg-detail {
  background-color: #000000 !important;
  background-image:
    repeating-linear-gradient(90deg, rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px),
    repeating-linear-gradient(0deg,   rgba(90,166,196,0.09) 0px, rgba(90,166,196,0.09) 1px, transparent 1px, transparent 28px) !important;
  border-color: rgba(90,166,196,0.12) !important;
  box-shadow: none !important;
}
/* 标题栏不透明深蓝底，挡住网格 */
.kg-page .kg-left-col > .cd-t,
.kg-page .kg-graph-card > .kg-toolbar,
.kg-page .kg-detail > .cd-t {
  background-color: #06121c !important;
  background-image: none !important;
}
/* 左侧统计区域标题 */
.kg-page .kg-stats-section > .cd-t {
  background-color: #06121c !important;
  background-image: none !important;
}
</style>
