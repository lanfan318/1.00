<template>
<div>
  <el-row :gutter="14">
    <el-col :span="5">
      <div class="cd" style="height:600px;display:flex;flex-direction:column">
        <div class="cd-t">图谱分类</div>
        <el-menu :default-active="cat" @select="onSelect" class="kg-menu">
          <el-menu-item index="all">全部图谱（综合）</el-menu-item>
          <el-menu-item index="锅炉故障">锅炉故障</el-menu-item>
          <el-menu-item index="汽轮机故障">汽轮机故障</el-menu-item>
          <el-menu-item index="辅机故障">辅机故障</el-menu-item>
          <el-menu-item index="火警关联">火警关联</el-menu-item>
        </el-menu>
        <div style="margin-top:14px;padding-top:14px;border-top:0.5px solid #1e293b">
          <div class="cd-t" style="margin-bottom:8px">实体统计</div>
          <div class="st-row"><span>设备</span><strong style="color:#3b82f6">{{ stats.设备 }}</strong></div>
          <div class="st-row"><span>故障</span><strong style="color:#ef4444">{{ stats.故障 }}</strong></div>
          <div class="st-row"><span>原因</span><strong style="color:#f59e0b">{{ stats.原因 }}</strong></div>
          <div class="st-row"><span>方案</span><strong style="color:#22c55e">{{ stats.方案 }}</strong></div>
        </div>
      </div>
    </el-col>
    <el-col :span="19">
      <div class="cd" style="height:600px">
        <div class="kg-toolbar">
          <el-input v-model="kw" placeholder="搜索实体名称（如 A引风机、轴承温度高、磨辊磨损）" clearable @input="onSearch" style="flex:1" />
          <span class="kg-info" v-if="kw">搜索 " <strong style="color:#3b82f6">{{ kw }}</strong> " 结果：<strong style="color:#22c55e">{{ matchNodes.length }}</strong> 个实体</span>
        </div>
        <div ref="kg" style="height:520px"></div>
      </div>
    </el-col>
  </el-row>

  <div class="cd" style="margin-top:14px">
    <div class="cd-t">实体详情</div>
    <div v-if="selected">
      <el-row :gutter="14">
        <el-col :span="6"><div class="dt-card"><div class="dt-l">名称</div><div class="dt-v">{{ selected.n }}</div></div></el-col>
        <el-col :span="6"><div class="dt-card"><div class="dt-l">类型</div><div class="dt-v" :style="{color:selected.c}">{{ selectedType }}</div></div></el-col>
        <el-col :span="6"><div class="dt-card"><div class="dt-l">关联关系</div><div class="dt-v">{{ relationsOf(selected.n).length }} 条</div></div></el-col>
        <el-col :span="6">
          <div class="dt-card">
            <div class="dt-l">操作</div>
            <div class="dt-v">
              <el-button v-if="selected.did" link type="primary" size="small" @click="goDevice">查看设备详情</el-button>
              <el-button link type="primary" size="small" @click="goAnalysis">查看工况分析</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
      <div v-if="relationsOf(selected.n).length" style="margin-top:12px">
        <div class="dt-rels">
          <div v-for="(r, i) in relationsOf(selected.n)" :key="i" class="dt-rel">
            <span class="dt-rel-from">{{ r.from }}</span>
            <span class="dt-rel-arrow">→ {{ r.type }}</span>
            <span class="dt-rel-to">{{ r.to }}</span>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-hint">点击图谱中的节点查看详情</div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const router = useRouter()
const cat = ref('all')
const kw = ref('')
const kg = ref(null)
const matchNodes = ref([])
const selected = ref(null)
let chart

const currentRelations = computed(() => {
  if (cat.value === 'all') {
    // 全部图谱：合并所有分类的关系
    return Object.values(store.kgRelations).flat()
  }
  return store.kgRelations[cat.value] || []
})

const currentNodes = computed(() => {
  const allNodes = []
  const seen = new Set()
  const rels = currentRelations.value

  // 设备节点（从 store.kgData 取）
  store.kgData.设备.forEach(d => {
    if (!seen.has(d.name)) { allNodes.push({ name: d.name, c: d.c, category: '设备', did: d.deviceId }); seen.add(d.name) }
  })
  // 故障/原因/方案节点
  ;['故障', '原因', '方案'].forEach(cat => {
    store.kgData[cat].forEach(n => {
      if (!seen.has(n.name)) { allNodes.push({ name: n.name, c: n.c, category: cat }); seen.add(n.name) }
    })
  })
  // 只保留在关系中出现的节点
  const relNodes = new Set()
  rels.forEach(r => { relNodes.add(r.from); relNodes.add(r.to) })
  return allNodes.filter(n => relNodes.has(n.name))
})

const stats = computed(() => {
  const nodes = currentNodes.value
  return {
    '设备': nodes.filter(n => n.category === '设备').length,
    '故障': nodes.filter(n => n.category === '故障').length,
    '原因': nodes.filter(n => n.category === '原因').length,
    '方案': nodes.filter(n => n.category === '方案').length
  }
})

const selectedType = computed(() => selected.value ? ({
  '#3b82f6': '设备', '#ef4444': '故障', '#f59e0b': '原因', '#22c55e': '方案'
}[selected.value.c] || '未知') : '')

const relationsOf = (name) => {
  return currentRelations.value.filter(r => r.from === name || r.to === name).map(r => ({
    from: r.from, to: r.to, type: r.from === name ? '导致' : '由...导致'
  }))
}

const initChart = () => {
  if (chart) chart.dispose()
  chart = echarts.init(kg.value)
  const nodes = currentNodes.value
  const rels = currentRelations.value

  // 标记搜索匹配的节点
  const matched = matchNodes.value.length > 0 ? new Set(matchNodes.value.map(n => n.name)) : null

  chart.setOption({
    tooltip: { formatter: (p) => `<b>${p.data.name}</b><br/>${p.data.category || ''}` },
    series: [{
      type: 'graph', layout: 'force', roam: true,
      force: { repulsion: 280, edgeLength: 90, gravity: 0.05 },
      label: { show: true, color: '#e2e8f0', fontSize: 11, position: 'right' },
      data: nodes.map(n => ({
        name: n.name, symbolSize: matched && matched.has(n.name) ? 50 : (n.category === '设备' ? 40 : n.category === '故障' ? 30 : 22),
        itemStyle: { color: matched && matched.has(n.name) ? '#fbbf24' : n.c, borderColor: matched && matched.has(n.name) ? '#f59e0b' : 'transparent', borderWidth: matched ? 3 : 0 },
        category: n.category,
        did: n.did
      })),
      links: rels.map(r => ({ source: r.from, target: r.to, lineStyle: { color: '#2a3544', curveness: 0.1, width: 1.5 } })),
      categories: [{ name: '设备', itemStyle: { color: '#3b82f6' } }, { name: '故障', itemStyle: { color: '#ef4444' } }, { name: '原因', itemStyle: { color: '#f59e0b' } }, { name: '方案', itemStyle: { color: '#22c55e' } }],
      emphasis: { focus: 'adjacency', label: { fontSize: 13 } }
    }]
  })

  // 节点点击事件
  chart.on('click', (params) => {
    if (params.data) {
      selected.value = nodes.find(n => n.name === params.data.name) || null
    }
  })
}

const onSelect = (idx) => { cat.value = idx; selected.value = null }
const onSearch = (val) => {
  if (!val) { matchNodes.value = []; return }
  matchNodes.value = currentNodes.value.filter(n => n.name.toLowerCase().includes(val.toLowerCase()))
}
const goDevice = () => { if (selected.value?.did) { store.selectedDevice = selected.value.did; router.push('/devices') } }
const goAnalysis = () => { if (selected.value?.did) { store.selectedDevice = selected.value.did; router.push('/condition') } }

watch([cat, currentNodes, currentRelations, matchNodes], () => nextTick(initChart), { deep: true })
onMounted(() => nextTick(initChart))
onUnmounted(() => chart?.dispose())
</script>

<style scoped>
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-t { font-size: 13px; color: #94a3b8; margin-bottom: 10px; font-weight: 500; }
.kg-menu { border: none; background: transparent; }
.kg-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.kg-info { font-size: 12px; color: #94a3b8; }
.st-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 12px; color: #94a3b8; }
.dt-card { background: #0a0e17; padding: 12px; border-radius: 6px; }
.dt-l { font-size: 11px; color: #94a3b8; margin-bottom: 4px; }
.dt-v { font-size: 14px; color: #e2e8f0; font-weight: 500; }
.dt-rels { background: #0a0e17; border-radius: 6px; padding: 10px; max-height: 200px; overflow-y: auto; }
.dt-rel { display: flex; align-items: center; gap: 10px; padding: 6px 10px; font-size: 12px; color: #94a3b8; border-bottom: 0.5px solid #1e293b; }
.dt-rel-from, .dt-rel-to { color: #e2e8f0; font-weight: 500; }
.dt-rel-arrow { color: #3b82f6; font-size: 11px; }
.empty-hint { text-align: center; padding: 40px; color: #64748b; }
</style>
