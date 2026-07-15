<template>
<div class="pg">
  <div class="pg-h"><h2>报警规则配置</h2>
    <div><el-input v-model="kw" placeholder="搜索设备/测点" clearable style="width:220px"/><el-button type="primary" style="margin-left:8px" @click="add">+ 新增</el-button></div>
  </div>
  <el-table :data="filtered" borderless>
    <el-table-column prop="device" label="所属设备" />
    <el-table-column prop="point" label="测点" />
    <el-table-column width="100" label="级别">
      <template #default="{row}"><el-tag :type="row.lv===1?'danger':row.lv===2?'warning':'info'" size="small">{{ row.lv===1?'一级':row.lv===2?'二级':'智能预警' }}</el-tag></template>
    </el-table-column>
    <el-table-column width="140" label="触发条件" prop="cond" />
    <el-table-column width="80" label="阈值" prop="val" />
    <el-table-column width="180" label="通知方式">
      <template #default="{row}"><el-tag v-for="c in row.ch" :key="c" size="small" style="margin-right:3px">{{c}}</el-tag></template>
    </el-table-column>
    <el-table-column width="80" label="启用">
      <template #default="{row}"><el-switch v-model="row.on" /></template>
    </el-table-column>
    <el-table-column width="140" label="操作">
      <template #default="{row}"><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="danger" @click="del(row)">删除</el-button></template>
    </el-table-column>
  </el-table>

  <el-dialog v-model="dlg" :title="f.id?'编辑规则':'新增规则'" width="500">
    <el-form :model="f" label-width="80px">
      <el-form-item label="设备"><el-input v-model="f.device"/></el-form-item>
      <el-form-item label="测点"><el-input v-model="f.point"/></el-form-item>
      <el-form-item label="级别"><el-select v-model="f.lv"><el-option :value="1" label="一级"/><el-option :value="2" label="二级"/><el-option :value="3" label="智能预警"/></el-select></el-form-item>
      <el-form-item label="条件"><el-select v-model="f.cond"><el-option value=">" label="大于"/><el-option value="<" label="小于"/><el-option value="trend" label="趋势异常"/></el-select></el-form-item>
      <el-form-item label="阈值"><el-input v-model.number="f.val" type="number"/></el-form-item>
      <el-form-item label="通知方式"><el-checkbox-group v-model="f.ch"><el-checkbox value="站内"/>站内<el-checkbox value="短信"/>短信<el-checkbox value="邮件"/>邮件<el-checkbox value="钉钉"/>钉钉</el-checkbox-group></el-form-item>
    </el-form>
    <template #footer><el-button @click="dlg=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
const kw = ref(''), dlg = ref(false)
const f = ref({id:null,device:'',point:'',lv:2,cond:'>',val:0,ch:['站内'],on:true})
const rules = ref([
  {id:1,device:'A引风机',point:'轴承温度',lv:1,cond:'>',val:80,ch:['站内','短信','钉钉'],on:true},
  {id:2,device:'A引风机',point:'振动幅值',lv:1,cond:'>',val:5,ch:['站内','短信'],on:true},
  {id:3,device:'A磨煤机',point:'振动',lv:2,cond:'>',val:4.5,ch:['站内'],on:true},
  {id:4,device:'锅炉',point:'主汽温度',lv:2,cond:'>',val:545,ch:['站内','短信'],on:true},
  {id:5,device:'锅炉',point:'NOx浓度',lv:3,cond:'trend',val:0,ch:['站内'],on:true},
  {id:6,device:'汽机',point:'润滑油压力',lv:1,cond:'<',val:0.15,ch:['站内','短信','钉钉'],on:true},
])
const filtered = computed(() => kw.value?rules.value.filter(r=>r.device.includes(kw.value)||r.point.includes(kw.value)):rules.value)
const add = () => { f.value = {id:null,device:'',point:'',lv:2,cond:'>',val:0,ch:['站内'],on:true}; dlg.value=true }
const edit = (row) => { f.value = JSON.parse(JSON.stringify(row)); dlg.value=true }
const save = () => {
  if(f.value.id){ const i=rules.value.findIndex(r=>r.id===f.value.id); rules.value[i]={...f.value} }
  else { f.value.id=Date.now(); rules.value.push({...f.value}) }
  dlg.value=false; ElMessage.success('已保存')
}
const del = async (row) => { await ElMessageBox.confirm('删除？','提示',{type:'warning'}); rules.value=rules.value.filter(r=>r.id!==row.id); ElMessage.success('已删除') }
</script>
<style scoped>
.pg{background:#111827;padding:20px;border-radius:10px;}
.pg-h{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}
h2{font-size:16px;font-weight:500}
</style>
