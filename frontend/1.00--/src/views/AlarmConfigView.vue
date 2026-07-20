<template>
<div>
  <div class="pg-h">
    <h2>报警规则配置</h2>
    <el-input v-model="kw" placeholder="搜索设备/测点" clearable style="width:220px"/>
    <el-button type="primary" @click="showAdd">+ 新增规则</el-button>
  </div>

  <el-table :data="filtered" borderless>
    <el-table-column prop="device" label="所属设备" width="100" />
    <el-table-column prop="point" label="测点" width="100" />
    <el-table-column width="80" label="级别">
      <template #default="{row}"><el-tag :type="row.level===1?'danger':row.level===2?'warning':'info'" size="small">{{ lvlTxt(row.level) }}</el-tag></template>
    </el-table-column>
    <el-table-column width="100" label="触发条件" prop="cond" />
    <el-table-column width="80" label="阈值" prop="val" />
    <el-table-column width="240" label="通知方式">
      <template #default="{row}">
        <el-tag v-for="c in row.channels" :key="c" :type="c==='站内'?'info':c==='短信'?'warning':c==='钉钉'?'primary':'success'" size="small" style="margin-right:3px">
          {{ chLabel(c) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column width="70" label="启用">
      <template #default="{row}"><el-switch v-model="row.enabled" /></template>
    </el-table-column>
    <el-table-column width="120" label="操作">
      <template #default="{row}"><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="danger" @click="del(row)">删除</el-button></template>
    </el-table-column>
  </el-table>

  <div class="cd" style="margin-top:16px">
    <div class="cd-t">通知方式说明（后续将对接实际服务）</div>
    <el-row :gutter="12">
      <el-col :span="8" v-for="ch in store.channels" :key="ch.key">
        <div class="ch-card">
          <div class="ch-ic">{{ ch.icon }}</div>
          <div class="ch-info">
            <div class="ch-lb">{{ ch.label }} <span class="ch-key">({{ ch.key }})</span></div>
            <div class="ch-desc">{{ ch.desc }}</div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>

  <el-dialog v-model="dlg" :title="f.id?'编辑规则':'新增规则'" width="500">
    <el-form :model="f" label-width="80px">
      <el-form-item label="设备"><el-input v-model="f.device"/></el-form-item>
      <el-form-item label="测点"><el-input v-model="f.point"/></el-form-item>
      <el-form-item label="级别"><el-select v-model="f.level"><el-option :value="1" label="一级"/><el-option :value="2" label="二级"/><el-option :value="3" label="智能预警"/></el-select></el-form-item>
      <el-form-item label="条件"><el-select v-model="f.cond"><el-option value=">" label="大于"/><el-option value="<" label="小于"/><el-option value="trend" label="趋势异常"/></el-select></el-form-item>
      <el-form-item label="阈值"><el-input v-model.number="f.val" type="number"/></el-form-item>
      <el-form-item label="通知方式">
        <el-checkbox-group v-model="f.channels">
          <el-checkbox v-for="ch in store.channels" :key="ch.key" :value="ch.key">{{ ch.label }}</el-checkbox>
        </el-checkbox-group>
        <div style="font-size:11px;color:#64748b;margin-top:6px">提示：实际推送需在「系统设置 → 通知渠道」中配置对应服务的接入凭证（API Key、Webhook 等），后续版本会接入。</div>
      </el-form-item>
    </el-form>
    <template #footer><el-button @click="dlg=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
const kw = ref(''), dlg = ref(false)
const f = ref({ id: null, device: '', point: '', level: 2, cond: '>', val: 0, channels: ['站内'], enabled: true })

const filtered = computed(() => kw.value ? store.alarmRules.filter(r => r.device.includes(kw.value) || r.point.includes(kw.value)) : store.alarmRules)
const lvlTxt = (l) => l === 1 ? '一级' : l === 2 ? '二级' : '智能预警'
const chLabel = (key) => store.channels.find(c => c.key === key)?.label || key

const showAdd = () => { f.value = { id: null, device: '', point: '', level: 2, cond: '>', val: 0, channels: ['站内'], enabled: true }; dlg.value = true }
const edit = (row) => { f.value = JSON.parse(JSON.stringify(row)); dlg.value = true }
const save = () => {
  if (f.value.id) {
    const i = store.alarmRules.findIndex(r => r.id === f.value.id)
    if (i >= 0) store.alarmRules[i] = { ...f.value }
  } else {
    store.addRule({ ...f.value })
  }
  dlg.value = false; ElMessage.success('已保存')
}
const del = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除「${row.device}-${row.point}」规则？`, '提示', { type: 'warning' })
    store.delRule(row.id)
    ElMessage.success('已删除')
  } catch (e) {}
}
</script>

<style scoped>
.pg-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
h2 { font-size: 16px; font-weight: 500; }
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-t { font-size: 13px; color: #94a3b8; margin-bottom: 12px; font-weight: 500; }
.ch-card { display: flex; gap: 10px; padding: 12px; background: #0a0e17; border-radius: 8px; margin-bottom: 8px; }
.ch-ic { font-size: 24px; line-height: 1.2; }
.ch-lb { font-size: 13px; color: #e2e8f0; font-weight: 500; margin-bottom: 2px; }
.ch-lb .ch-key { color: #64748b; font-size: 11px; font-weight: 400; }
.ch-desc { font-size: 11px; color: #94a3b8; }
</style>
