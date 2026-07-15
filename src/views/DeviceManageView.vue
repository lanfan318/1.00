<template>
<div>
  <el-row :gutter="14">
    <el-col :span="6"><div class="cd" style="height:calc(100vh-110px);overflow-y:auto">
      <div class="cd-h"><span class="cd-t">设备树</span><div><el-button size="small" type="primary" @click="addUnit">+ 机组</el-button><el-button size="small" @click="addDev">+ 设备</el-button></div></div>
      <div v-for="u in units" :key="u.id"><div class="tu" @click="selUnit=u.id">{{ u.n }} · {{ u.type }} · {{ u.cap }}MW</div>
        <div v-for="d in devsByUnit(u.id)" :key="d.id" class="td" :class="{ac:selDev===d.id}" @click="selDev=d.id">{{ d.n }} <span :style="{color:d.h>=90?'#22c55e':d.h>=80?'#f59e0b':'#ef4444'}">{{d.h}}</span></div>
      </div>
    </div></el-col>
    <el-col :span="18">
      <div class="cd" style="margin-bottom:14px"><div class="cd-h"><span class="cd-t">设备信息</span><el-button size="small" @click="editing=true" :disabled="!cdev">编辑参数</el-button></div>
        <div v-if="cdev" class="info-grid">
          <div>名称：<span>{{cdev.n}}</span></div><div>型号：<span>{{cdev.model}}</span></div>
          <div>所属机组：<span>{{cdev.unit}}</span></div><div>专业：<span>{{cdev.dept}}</span></div>
          <div>健康度：<span :style="{color:cdev.h>=90?'#22c55e':cdev.h>=80?'#f59e0b':'#ef4444'}">{{cdev.h.toFixed(1)}}</span></div><div>运行状态：<span style="color:#22c55e">运行中</span></div>
        </div>
      </div>
      <div class="cd"><div class="cd-t">测点参数</div>
        <el-table :data="measures" borderless>
          <el-table-column prop="k" label="测点" /><el-table-column prop="v" label="当前值" /><el-table-column prop="u" label="单位" width="60" />
          <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.ok?'success':'warning'" size="small">{{row.ok?'正常':'偏高'}}</el-tag></template></el-table-column>
          <el-table-column prop="lim" label="阈值" width="80" /><el-table-column label="操作" width="80"><template #default="{row}"><el-button link type="primary" size="small" @click="editParam(row)">修改</el-button></template></el-table-column>
        </el-table>
      </div>
      <div v-if="result" class="in"><div class="in-t">参数修改预测</div><div class="in-b">{{result}}</div></div>
    </el-col>
  </el-row>

  <el-dialog v-model="editing" title="编辑参数" width="480">
    <div class="fg" v-for="m in measures" :key="m.k"><label>{{m.k}}（{{m.u}}）— 当前:{{m.v}}，阈值:{{m.lim}}</label><el-input v-model.number="m.v" type="number" size="small"/></div>
    <div class="fg"><label>模拟工况</label><el-select v-model="scenario" style="width:100%"><el-option value="normal" label="正常运行"/><el-option value="warn" label="轻度异常"/><el-option value="danger" label="严重故障"/></el-select></div>
    <template #footer><el-button @click="editing=false">取消</el-button><el-button type="primary" @click="saveParams">保存</el-button></template>
  </el-dialog>

  <el-dialog v-model="unitDlg" title="添加机组" width="400">
    <div class="fg"><label>名称</label><el-input v-model="uf.n"/></div><div class="fg"><label>类型</label><el-select v-model="uf.type" style="width:100%"><el-option value="火电机组"/><el-option value="燃气机组"/></el-select></div><div class="fg"><label>容量(MW)</label><el-input v-model.number="uf.cap" type="number"/></div>
    <template #footer><el-button @click="unitDlg=false">取消</el-button><el-button type="primary" @click="saveUnit">添加</el-button></template>
  </el-dialog>

  <el-dialog v-model="devDlg" title="添加设备" width="400">
    <div class="fg"><label>机组</label><el-select v-model="df.unit" style="width:100%"><el-option v-for="u in units" :key="u.id" :value="u.id" :label="u.n"/></el-select></div>
    <div class="fg"><label>专业</label><el-select v-model="df.dept" style="width:100%"><el-option value="锅炉"/><el-option value="汽轮机"/><el-option value="辅网"/></el-select></div>
    <div class="fg"><label>名称</label><el-input v-model="df.n"/></div>
    <template #footer><el-button @click="devDlg=false">取消</el-button><el-button type="primary" @click="saveDev">添加</el-button></template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
const selUnit=ref('U1'),selDev=ref('d1'),editing=ref(false),unitDlg=ref(false),devDlg=ref(false),scenario=ref('normal'),result=ref('')
const uf=reactive({n:'',type:'火电机组',cap:600}),df=reactive({unit:'U1',dept:'锅炉',n:''})
const units=ref([{id:'U1',n:'#1机组',type:'火电机组',cap:600}])
const devs=ref([
  {id:'d1',unit:'U1',dept:'锅炉',n:'A引风机',model:'YF-2024C',h:82.3,ms:[{k:'电流',v:142,u:'A',lim:150,ok:true},{k:'温度',v:82.3,u:'℃',lim:75,ok:false},{k:'振动',v:3.8,u:'mm/s',lim:5,ok:true},{k:'转速',v:1450,u:'rpm',lim:1600,ok:true}]},
  {id:'d2',unit:'U1',dept:'锅炉',n:'A送风机',model:'YF-2024A',h:97.2,ms:[{k:'电流',v:120,u:'A',lim:150,ok:true},{k:'温度',v:45,u:'℃',lim:75,ok:true},{k:'振动',v:1.2,u:'mm/s',lim:5,ok:true},{k:'转速',v:2980,u:'rpm',lim:3200,ok:true}]},
  {id:'d3',unit:'U1',dept:'锅炉',n:'A磨煤机',model:'MM-2024B',h:85.0,ms:[{k:'电流',v:95,u:'A',lim:130,ok:true},{k:'温度',v:68,u:'℃',lim:80,ok:true},{k:'振动',v:4.7,u:'mm/s',lim:6,ok:true},{k:'转速',v:24,u:'rpm',lim:28,ok:true}]},
  {id:'d4',unit:'U1',dept:'汽轮机',n:'给水泵A',model:'ZB-2024',h:98.4,ms:[{k:'电流',v:180,u:'A',lim:220,ok:true},{k:'温度',v:42,u:'℃',lim:70,ok:true},{k:'振动',v:0.8,u:'mm/s',lim:3,ok:true},{k:'转速',v:2980,u:'rpm',lim:3100,ok:true}]},
])
const devsByUnit = (uid) => devs.value.filter(d=>d.unit===uid)
const cdev = computed(()=>devs.value.find(d=>d.id===selDev.value))
const measures = computed(()=>cdev.value?cdev.value.ms:[])
const editParam = (row) => { const v=prompt(`修改${row.k}：`,row.v); if(v!==null){row.v=parseFloat(v);row.ok=row.v<row.lim;cdev.value.h=Math.min(100,Math.max(0,cdev.value.h*0.3+70*(row.ok?0.95:0.6)+15));result.value=`${cdev.value.n} · ${row.k} 修改为 ${row.v}${row.u}。${row.ok?'在安全阈值内。':'⚠ 超出阈值，将触发报警。'} 健康度：${cdev.value.h.toFixed(1)}`}}
const addUnit=()=>{unitDlg.value=true;uf.n='';uf.cap=600}
const saveUnit=()=>{if(!uf.n)return;units.value.push({id:'U'+Date.now(),n:uf.n,type:uf.type,cap:uf.cap});unitDlg.value=false;ElMessage.success('已添加')}
const addDev=()=>{devDlg.value=true;df.n='';df.unit=selUnit.value}
const saveDev=()=>{if(!df.n)return;devs.value.push({id:'d'+Date.now(),unit:df.unit,dept:df.dept,n:df.n,model:'',h:95,ms:[{k:'电流',v:100,u:'A',lim:150,ok:true},{k:'温度',v:45,u:'℃',lim:75,ok:true},{k:'振动',v:1.5,u:'mm/s',lim:5,ok:true}]});devDlg.value=false;ElMessage.success('已添加')}
const saveParams=()=>{measures.value.forEach(m=>{m.ok=m.v<m.lim});editing.value=false;result.value=`参数已保存。${scenario.value==='warn'?'已切换至轻度异常模式。':scenario.value==='danger'?'已切换至严重故障模式。':''}`}
</script>
<style scoped>
.cd{background:#111827;border:0.5px solid #1e293b;border-radius:10px;padding:16px}.cd-h{display:flex;justify-content:space-between;margin-bottom:12px}.cd-t{font-size:13px;font-weight:500}
.tu{font-weight:500;padding:8px;border-radius:4px;cursor:pointer;font-size:13px}.tu:hover{background:#1a2332}.td{padding:6px 8px 6px 24px;cursor:pointer;font-size:12px;color:#94a3b8;border-radius:4px;transition:0.15s}.td:hover{background:#1a2332}.td.ac{background:rgba(59,130,246,0.12);color:#3b82f6}
.info-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;font-size:12px}.info-grid div{color:#94a3b8}.info-grid span{color:#e2e8f0}
.fg{margin-bottom:12px}.fg label{display:block;font-size:12px;color:#94a3b8;margin-bottom:4px}
.in{background:#0a0e17;border:0.5px solid #1e293b;border-radius:10px;padding:14px;margin-top:14px}.in-t{font-size:12px;font-weight:500;color:#3b82f6;margin-bottom:6px}.in-b{font-size:12px;color:#94a3b8}
</style>
