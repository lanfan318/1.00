<template>
<div>
  <el-row :gutter="14">
    <el-col :span="5">
      <div class="cd" style="height:600px;display:flex;flex-direction:column">
        <div class="cd-t">推理图分类</div>
        <el-menu :default-active="cat" @select="onSelect" class="kg-menu">
          <el-menu-item index="推理案例1">给水泵油压异常</el-menu-item>
          <el-menu-item index="推理案例2">引风机轴承温度高</el-menu-item>
          <el-menu-item index="推理案例3">磨煤机振动超标</el-menu-item>
          <el-menu-item index="推理案例4">主汽温度偏高</el-menu-item>
        </el-menu>
        <div style="margin-top:14px;padding-top:14px;border-top:0.5px solid #1e293b">
          <div class="cd-t" style="margin-bottom:8px">推理统计</div>
          <div class="st-row"><span>用户问题</span><strong style="color:#8b5cf6">{{ stats.user }}</strong></div>
          <div class="st-row"><span>症状</span><strong style="color:#f59e0b">{{ stats.symptom }}</strong></div>
          <div class="st-row"><span>中间现象</span><strong style="color:#fbbf24">{{ stats.middle }}</strong></div>
          <div class="st-row"><span>根本原因</span><strong style="color:#ef4444">{{ stats.cause }}</strong></div>
          <div class="st-row"><span>解决方案</span><strong style="color:#22c55e">{{ stats.solution }}</strong></div>
        </div>
      </div>
    </el-col>
    <el-col :span="19">
      <div class="cd" style="height:600px;display:flex;flex-direction:column">
        <div class="kg-toolbar">
          <span style="font-size:13px;color:#e2e8f0;font-weight:500">知识图谱推理</span>
          <el-button-group>
            <el-button :type="cat==='推理案例1'?'primary':''" size="small" @click="onSelect('推理案例1')">给水泵油压</el-button>
            <el-button :type="cat==='推理案例2'?'primary':''" size="small" @click="onSelect('推理案例2')">引风机轴承</el-button>
            <el-button :type="cat==='推理案例3'?'primary':''" size="small" @click="onSelect('推理案例3')">磨煤机振动</el-button>
            <el-button :type="cat==='推理案例4'?'primary':''" size="small" @click="onSelect('推理案例4')">主汽温度</el-button>
          </el-button-group>
        </div>
        <div class="kg-canvas">
          <ReasoningGraph :case-data="cases[cat]" :selected-id="selectedId" @update:selected-id="val => selectedId = val" @select="onNodeSel" />
        </div>
      </div>
    </el-col>
  </el-row>

  <div class="cd" style="margin-top:14px">
    <div class="cd-t">推理详情</div>
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
  </div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import ReasoningGraph from '@/components/ReasoningGraph.vue'

const router = useRouter()
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
const goDiagnosis = () => router.push('/diagnosis')
const goCondition = () => router.push('/condition')
</script>

<style scoped>
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-t { font-size: 13px; color: #94a3b8; margin-bottom: 10px; font-weight: 500; }
.kg-menu { border: none; background: transparent; }
.kg-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.kg-canvas { flex: 1; min-height: 0; }
.st-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 12px; color: #94a3b8; }
.dt-card { background: #0a0e17; padding: 12px; border-radius: 6px; }
.dt-l { font-size: 11px; color: #94a3b8; margin-bottom: 4px; }
.dt-v { font-size: 14px; color: #e2e8f0; font-weight: 500; }
.dt-rels { background: #0a0e17; border-radius: 6px; padding: 10px; max-height: 200px; overflow-y: auto; }
.dt-rel { display: flex; align-items: center; gap: 10px; padding: 6px 10px; font-size: 12px; color: #94a3b8; border-bottom: 0.5px solid #1e293b; }
.dt-rel-from, .dt-rel-to { font-weight: 500; }
.dt-rel-arrow { color: #64748b; font-size: 11px; }
.empty-hint { text-align: center; padding: 40px; color: #64748b; }
</style>
