<template>
<div>
  <el-row :gutter="14">
    <el-col :span="6"><div class="cd" style="height:calc(100vh - 110px);overflow-y:auto">
      <div class="cd-h"><span class="cd-t">设备树 · {{ store.selectedUnit.name }}</span>
        <div><el-button size="small" type="primary" @click="showAddUnit">+ 机组</el-button><el-button size="small" @click="showAddDev">+ 设备</el-button></div>
      </div>
      <div v-for="u in store.units" :key="u.id" class="unit-block">
        <div class="tu" @click="selectUnit(u.id)" :class="{ac:store.selectedUnitId===u.id}">
          <span class="tu-dot" :style="{background: u.id===store.selectedUnitId?'#3b82f6':'#64748b'}"></span>
          {{ u.name }} · {{ u.type }} · {{ u.capacity }}MW
          <span class="tu-del" @click.stop="delUnit(u.id)" v-if="store.units.length>1">×</span>
        </div>
        <div v-for="d in store.unitDevices(u.id)" :key="d.id" class="td" :class="{ac:store.selectedDevice===d.id}" @click="selectDev(d.id)">
          {{ d.name }}
          <span :style="{color:d.health>=90?'#22c55e':d.health>=80?'#f59e0b':'#ef4444',fontSize:'10px',float:'right'}">{{ d.health.toFixed(0) }}</span>
        </div>
      </div>
    </div></el-col>
    <el-col :span="18">
      <div class="cd" style="margin-bottom:14px">
        <div class="cd-h"><span class="cd-t">设备信息</span>
          <div>
            <el-button size="small" type="primary" @click="showBatchEdit" :disabled="!cdev">批量修改</el-button>
            <el-button size="small" type="danger" @click="delCurDev" :disabled="!cdev">删除设备</el-button>
          </div>
        </div>
        <div v-if="cdev" class="info-grid">
          <div>名称：<span>{{ cdev.name }}</span></div>
          <div>型号：<span>{{ cdev.model }}</span></div>
          <div>所属机组：<span>{{ unitName }}</span></div>
          <div>专业：<span>{{ cdev.dept }}</span></div>
          <div>健康度：<span :style="{color:cdev.health>=90?'#22c55e':cdev.health>=80?'#f59e0b':'#ef4444'}">{{ cdev.health.toFixed(1) }}</span></div>
          <div>运行状态：<span :style="{color:cdev.health>=90?'#22c55e':cdev.health>=80?'#f59e0b':'#ef4444'}">{{ cdev.health>=90?'运行中':cdev.health>=80?'需关注':'预警处置' }}</span></div>
        </div>
        <div v-else class="empty">请选择左侧设备</div>
      </div>
      <div class="cd"><div class="cd-t">测点参数（点击"修改"调整阈值模拟报警）</div>
        <el-table v-if="cdev" :data="measureRows" borderless>
          <el-table-column prop="key" label="测点" width="100" />
          <el-table-column prop="value" label="当前值" width="100" />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column label="阈值上限" width="100"><template #default="{row}">{{ row.limit }}</template></el-table-column>
          <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.ok?'success':'warning'" size="small">{{ row.ok?'正常':'超限' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{row}">
              <el-input-number v-model="row.value" :min="0" :step="row.step" size="small" :controls="false" style="width:80px" />
              <el-button link type="primary" size="small" @click="updateParam(row.key, row.value)">应用</el-button>
              <el-button link type="warning" size="small" @click="triggerAlarm(row)">触发报警</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty">请先选择设备</div>
      </div>
      <div v-if="result" class="in"><div class="in-t">参数修改预测</div><div class="in-b" v-html="result"></div></div>
    </el-col>
  </el-row>

  <!-- 添加机组 -->
  <el-dialog v-model="unitDlg" title="添加机组" width="400">
    <div class="fg"><label>机组名称</label><el-input v-model="uf.n" placeholder="例：#2 机组"/></div>
    <div class="fg"><label>机组类型</label><el-select v-model="uf.type" style="width:100%"><el-option value="火电机组"/><el-option value="燃气机组"/><el-option value="联合循环"/></el-select></div>
    <div class="fg"><label>额定容量 (MW)</label><el-input-number v-model="uf.cap" :min="100" :max="1000" style="width:100%"/></div>
    <template #footer><el-button @click="unitDlg=false">取消</el-button><el-button type="primary" @click="saveUnit">添加</el-button></template>
  </el-dialog>

  <!-- 添加设备 -->
  <el-dialog v-model="devDlg" title="添加设备" width="400">
    <div class="fg"><label>所属机组</label><el-select v-model="df.unit" style="width:100%"><el-option v-for="u in store.units" :key="u.id" :value="u.id" :label="u.name"/></el-select></div>
    <div class="fg"><label>专业</label><el-select v-model="df.dept" style="width:100%"><el-option value="锅炉"/><el-option value="汽轮机"/><el-option value="辅网"/></el-select></div>
    <div class="fg"><label>设备名称</label><el-input v-model="df.n" placeholder="例：E送风机"/></div>
    <div class="fg"><label>设备型号</label><el-input v-model="df.model" placeholder="例：YF-2024E"/></div>
    <template #footer><el-button @click="devDlg=false">取消</el-button><el-button type="primary" @click="saveDev">添加</el-button></template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'

const store = useDataStore()

const cdev = computed(() => store.devices.find(d => d.id === store.selectedDevice))
const unitName = computed(() => store.units.find(u => u.id === store.selectedDevice?.split('-')[0])?.name || '-')
const result = ref('')
const unitDlg = ref(false), devDlg = ref(false)
const uf = reactive({ n: '', type: '火电机组', cap: 600 })
const df = reactive({ unit: 'U1', dept: '锅炉', n: '', model: '' })

const measureRows = ref([])
// 当选中设备变化时，把测点数据填到本地行里（可编辑）
const refreshRows = () => {
  if (!cdev.value) { measureRows.value = []; return }
  measureRows.value = Object.entries(cdev.value.params).map(([k, v]) => ({
    key: k, value: v[0], limit: v[1], unit: v[2], ok: v[0] < v[1], step: v[2] === 'rpm' ? 50 : 1
  }))
}
// 监听设备切换
import { watch } from 'vue'
watch(cdev, refreshRows, { immediate: true })

const selectUnit = (id) => { store.selectedUnitId = id }
const selectDev = (id) => { store.selectedDevice = id; result.value = '' }

const updateParam = (k, v) => {
  if (!cdev.value) return
  const oldH = cdev.value.health
  const oldVal = cdev.value.params[k][0]
  store.updateDeviceParam(cdev.value.id, k, v)
  const newH = cdev.value.health
  const ok = v < cdev.value.params[k][1]
  result.value = `<strong>${cdev.value.name}</strong> · ${k} 从 <strong>${oldVal}</strong> 改为 <strong style="color:${ok?'#22c55e':'#ef4444'}">${v}${cdev.value.params[k][2]}</strong>。健康度：<strong>${oldH.toFixed(1)} → ${newH.toFixed(1)}</strong>。${ok?'参数在安全范围内。':'⚠️ 超过阈值，建议立即处置。'}`
  refreshRows()
}

const triggerAlarm = (row) => {
  if (!cdev.value) return
  const ok = row.value < row.limit
  const level = !ok ? 1 : 3
  const desc = ok ? `${cdev.value.name} ${row.key} 持续偏高预警` : `${cdev.value.name} ${row.key} 严重超限！立即处置！`
  store.triggerAlarm(cdev.value.id, level, desc, row.key, row.value + row.unit)
  ElMessage.success(`已为 ${cdev.value.name} 触发${level===1?'一级':'智能预警'}报警`)
}

const showAddUnit = () => { Object.assign(uf, { n: '', type: '火电机组', cap: 600 }); unitDlg.value = true }
const showBatchEdit = () => {
  ElMessage.info('请使用下表每行的"应用"按钮单独修改每个测点参数')
}
const saveUnit = () => {
  if (!uf.n.trim()) { ElMessage.warning('请输入机组名称'); return }
  store.addUnit({ name: uf.n, type: uf.type, capacity: uf.cap })
  unitDlg.value = false
  ElMessage.success('已添加机组')
}
const delUnit = async (u) => {
  try {
    await ElMessageBox.confirm(`确认删除 ${u.name}？该机组下所有设备也会被删除。`, '危险操作', { type: 'warning' })
    store.delUnit(u.id)
    ElMessage.success('已删除机组')
  } catch (e) {}
}
const showAddDev = () => { Object.assign(df, { unit: store.selectedUnitId, dept: '锅炉', n: '', model: '' }); devDlg.value = true }
const saveDev = () => {
  if (!df.n.trim()) { ElMessage.warning('请输入设备名称'); return }
  store.addDevice(df)
  devDlg.value = false
  ElMessage.success('已添加设备')
}
const delCurDev = async () => {
  if (!cdev.value) return
  try {
    await ElMessageBox.confirm(`确认删除设备「${cdev.value.name}」？`, '危险操作', { type: 'warning' })
    store.delDevice(cdev.value.id)
    store.selectedDevice = null
    ElMessage.success('已删除设备')
  } catch (e) {}
}

// 默认选中第一台
if (!store.selectedDevice && store.devices.length) store.selectedDevice = store.devices[0].id
</script>

<style scoped>
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.cd-t { font-size: 13px; font-weight: 500; }
.unit-block { margin-bottom: 8px; }
.tu { font-weight: 500; padding: 8px 10px; border-radius: 4px; cursor: pointer; font-size: 13px; display: flex; align-items: center; gap: 6px; position: relative; }
.tu:hover { background: #1a2332; }
.tu.ac { background: rgba(59,130,246,0.1); color: #3b82f6; }
.tu-dot { width: 6px; height: 6px; border-radius: 50%; }
.tu-del { position: absolute; right: 8px; color: #64748b; padding: 0 6px; border-radius: 3px; }
.tu-del:hover { color: #ef4444; background: rgba(239,68,68,0.1); }
.td { padding: 6px 8px 6px 24px; cursor: pointer; font-size: 12px; color: #94a3b8; border-radius: 4px; transition: 0.15s; }
.td:hover { background: #1a2332; }
.td.ac { background: rgba(59,130,246,0.12); color: #3b82f6; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 12px; }
.info-grid div { color: #94a3b8; }
.info-grid span { color: #e2e8f0; }
.empty { padding: 40px; text-align: center; color: #64748b; font-size: 13px; }
.in { background: #0a0e17; border: 0.5px solid #1e293b; border-radius: 10px; padding: 14px; margin-top: 14px; }
.in-t { font-size: 12px; font-weight: 500; color: #3b82f6; margin-bottom: 6px; }
.in-b { font-size: 12px; color: #94a3b8; line-height: 1.7; }
.fg { margin-bottom: 12px; }
.fg label { display: block; font-size: 12px; color: #94a3b8; margin-bottom: 4px; }
</style>
