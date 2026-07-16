<template>
<div>
  <el-row :gutter="12" style="margin-bottom:14px">
    <el-col :span="6" v-for="b in bars" :key="b.lv">
      <div class="lb" :class="{ac:filter===b.lv}" @click="filter=b.lv">
        <div class="lb-v" :style="{color:b.color}">{{ b.cnt }}</div>
        <div class="lb-l">{{ b.label }}</div>
      </div>
    </el-col>
  </el-row>

  <div class="cd"><div class="cd-h">
    <span class="cd-t">报警列表 · {{ store.selectedUnit.name }}（{{ filtered.length }} 条）</span>
    <div>
      <el-button size="small" @click="batchConfirm">批量确认</el-button>
      <el-button size="small" @click="batchResolve">批量完成</el-button>
      <el-button size="small" type="primary" @click="showSimulate">模拟新报警</el-button>
    </div>
  </div>
    <el-table :data="filtered" borderless>
      <el-table-column width="80"><template #default="{row}"><el-tag :type="row.l===1?'danger':row.l===2?'warning':'info'" size="small">{{ row.l===1?'一级':row.l===2?'二级':'智能' }}</el-tag></template></el-table-column>
      <el-table-column prop="time" label="时间" width="150">
        <template #default="{row}">{{ fmt(row.time) }}</template>
      </el-table-column>
      <el-table-column prop="unit" label="机组" width="80"><template #default="{row}">{{ unitName(row.unit) }}</template></el-table-column>
      <el-table-column prop="dept" label="专业" width="70" />
      <el-table-column prop="device" label="设备" width="120" />
      <el-table-column prop="desc" label="描述" />
      <el-table-column prop="point" label="测点" width="140" />
      <el-table-column label="报警值" width="90"><template #default="{row}"><span style="color:#f59e0b;font-weight:500">{{row.val}}</span></template></el-table-column>
      <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.st==='unhandled'?'danger':row.st==='confirmed'?'warning':'success'" size="small">{{ stTxt(row.st) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button v-if="row.st==='unhandled'" link type="primary" size="small" @click.stop="act(row,'confirmed')">确认</el-button>
          <el-button v-if="row.st==='unhandled'" link size="small" @click.stop="act(row,'suppressed')">抑制</el-button>
          <el-button v-if="row.st==='confirmed'" link type="success" size="small" @click.stop="act(row,'resolved')">处理完成</el-button>
          <el-button v-if="row.st==='resolved'" link size="small" @click.stop="act(row,'unhandled')">重新打开</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div v-if="cur" class="guide"><div class="gt">操作指导 · {{cur.desc}}</div>
    <el-row :gutter="14">
      <el-col :span="12"><div class="gs"><div class="gsh">报警详情</div>
        <p>测点：<span>{{cur.point}}</span> ｜ 当前值：<span style="color:#f59e0b">{{cur.val}}</span> ｜ 类型：{{cur.type}}</p>
        <p>设备：<span>{{cur.device}}</span>（{{cur.dept}}）｜ 所属机组：<span>{{ unitName(cur.unit) }}</span></p>
        <p>状态：<span>{{ stTxt(cur.st) }}</span> ｜ 报警时间：<span>{{ fmt(cur.time) }}</span></p>
      </div></el-col>
      <el-col :span="12"><div class="gs"><div class="gsh">处置建议</div>
        <p>{{ cur.l===1?'① 立即通知值班长和检修班组 ② 启动应急预案 ③ 降低相关设备负荷至安全范围 ④ 准备备品备件 ⑤ 若参数持续恶化，执行紧急停机程序':
              cur.l===2?'① 现场巡检确认报警真实性 ② 分析异常原因并记录 ③ 通知相关专业负责人关注 ④ 若升级为一级报警，按一级响应程序处置':
                       '① 关注报警趋势变化 ② 记录异常数据，纳入交接班报告 ③ 分析报警产生原因，判断是否为误报 ④ 若频繁出现，建议安排专项检查' }}</p>
      </div></el-col>
    </el-row>
  </div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const filter = ref('all')
const cur = ref(null)

const filtered = computed(() => {
  const uid = store.selectedUnitId
  let arr = store.alarms.filter(a => a.unit === uid)
  if (filter.value !== 'all') arr = arr.filter(a => a.l === parseInt(filter.value))
  return arr
})

const bars = computed(() => {
  const arr = store.unitAlarms(store.selectedUnitId)
  return [
    { lv: 'all', label: '全部报警', cnt: arr.length, color: '#3b82f6' },
    { lv: '1', label: '一级', cnt: arr.filter(a => a.l === 1).length, color: '#ef4444' },
    { lv: '2', label: '二级', cnt: arr.filter(a => a.l === 2).length, color: '#f59e0b' },
    { lv: '3', label: '智能预警', cnt: arr.filter(a => a.l === 3).length, color: '#06b6d4' }
  ]
})

const fmt = (t) => { const d = new Date(t); return d.toLocaleString('zh-CN', { hour12: false }) }
const stTxt = (st) => st === 'unhandled' ? '未处理' : st === 'confirmed' ? '已确认' : st === 'suppressed' ? '已抑制' : '已处理'
const unitName = (id) => store.units.find(u => u.id === id)?.name || id

const act = (row, st) => {
  // 已处理的报警点击"重新打开"会重置为未处理
  store.setAlarmStatus(row.id, st)
  cur.value = { ...row, st }
  ElMessage.success(st === 'resolved' ? '已处理' : st === 'confirmed' ? '已确认' : st === 'suppressed' ? '已抑制（10分钟内同设备同类报警不再触发）' : '已重新打开')
}

const batchConfirm = () => {
  const cnt = store.alarms.filter(a => a.unit === store.selectedUnitId && a.st === 'unhandled').length
  store.alarms.filter(a => a.unit === store.selectedUnitId && a.st === 'unhandled').forEach(a => store.setAlarmStatus(a.id, 'confirmed'))
  ElMessage.success(`已批量确认 ${cnt} 条`)
}
const batchResolve = () => {
  const cnt = store.alarms.filter(a => a.unit === store.selectedUnitId && (a.st === 'unhandled' || a.st === 'confirmed')).length
  store.alarms.filter(a => a.unit === store.selectedUnitId && (a.st === 'unhandled' || a.st === 'confirmed')).forEach(a => store.setAlarmStatus(a.id, 'resolved'))
  ElMessage.success(`已批量完成 ${cnt} 条`)
}

const showSimulate = async () => {
  // 模拟设备故障触发新报警
  const devs = store.unitDevices(store.selectedUnitId).filter(d => d.health < 90)
  if (devs.length === 0) { ElMessage.warning('当前机组没有健康度<90的设备'); return }
  try {
    const choices = devs.map(d => d.name).join('/')
    const { value } = await ElMessageBox.prompt('选择要模拟故障的设备：' + choices, '模拟新报警', { confirmButtonText: '触发', cancelButtonText: '取消' })
    // 这里简化：用 prompt 输设备名
    ElMessage.info('已演示模式：实际触发应从设备选择器选择')
  } catch (e) {}
}
</script>

<style scoped>
.lb { background: #0a0e17; border: 1px solid transparent; border-radius: 8px; padding: 14px; text-align: center; cursor: pointer; transition: 0.15s; }
.lb:hover { border-color: #2a3544; }
.lb.ac { border-color: #3b82f6; background: rgba(59,130,246,0.08); }
.lb-v { font-size: 28px; font-weight: 600; margin-bottom: 4px; }
.lb-l { font-size: 11px; color: #94a3b8; }
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.cd-t { font-size: 13px; font-weight: 500; }
.guide { background: #0a0e17; border: 0.5px solid #1e293b; border-radius: 10px; padding: 14px; margin-top: 14px; }
.gt { font-size: 14px; font-weight: 500; color: #f59e0b; margin-bottom: 12px; }
.gs { background: #111827; border-radius: 8px; padding: 12px; }
.gsh { font-size: 12px; font-weight: 500; color: #3b82f6; margin-bottom: 8px; }
.gs p { font-size: 12px; color: #94a3b8; line-height: 1.7; margin-bottom: 4px; }
.gs span { color: #e2e8f0; }
</style>
