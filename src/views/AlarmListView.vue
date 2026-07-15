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

  <div class="cd"><div class="cd-h"><span class="cd-t">报警列表</span>
      <div><el-button size="small" @click="batch('confirmed')">批量确认</el-button><el-button size="small" @click="batch('resolved')">批量完成</el-button></div>
    </div>
    <el-table :data="filtered" @row-click="showDetail" borderless>
      <el-table-column width="80"><template #default="{row}"><el-tag :type="row.l===1?'danger':row.l===2?'warning':'info'" size="small">{{ row.l===1?'一级':row.l===2?'二级':'智能' }}</el-tag></template></el-table-column>
      <el-table-column prop="time" label="时间" width="150" />
      <el-table-column prop="dept" label="专业" width="70" />
      <el-table-column prop="desc" label="描述" />
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="point" label="测点" width="140" />
      <el-table-column label="报警值" width="90"><template #default="{row}"><span style="color:#f59e0b;font-weight:500">{{row.val}}</span></template></el-table-column>
      <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.st==='unhandled'?'danger':row.st==='confirmed'?'warning':'success'" size="small">{{row.st==='unhandled'?'未处理':row.st==='confirmed'?'已确认':'已处理'}}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button v-if="row.st==='unhandled'" link type="primary" size="small" @click.stop="act(row,'confirmed')">确认</el-button>
          <el-button v-if="row.st==='unhandled'" link size="small" @click.stop="act(row,'suppressed')">抑制</el-button>
          <el-button v-if="row.st==='confirmed'" link type="success" size="small" @click.stop="act(row,'resolved')">完成</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div v-if="cur" class="guide"><div class="gt">操作指导 · {{cur.desc}}</div>
    <el-row :gutter="14">
      <el-col :span="12"><div class="gs"><div class="gsh">报警详情</div><p>测点：{{cur.point}} ｜ 当前值：{{cur.val}} ｜ 类型：{{cur.type}}</p></div></el-col>
      <el-col :span="12"><div class="gs"><div class="gsh">处置建议</div><p>{{ cur.l===1?'立即通知值班长，启动应急预案，降低相关设备负荷。':cur.l===2?'现场巡检确认，分析原因并记录，通知专业负责人。':'关注趋势变化，纳入交接班报告。' }}</p></div></el-col>
    </el-row>
  </div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
const filter=ref('all'),cur=ref(null)
const alarms=ref([
  {id:1,l:1,time:'2026-07-15 16:18:32',dept:'锅炉',desc:'A引风机轴承温度异常升高',type:'智能预警',point:'A引风机轴承温度',val:'82.3℃',st:'unhandled'},
  {id:2,l:1,time:'2026-07-15 16:10:15',dept:'汽机',desc:'润滑油压力低于安全阈值',type:'阈值报警',point:'润滑油压力',val:'0.12MPa',st:'unhandled'},
  {id:3,l:2,time:'2026-07-15 15:52:08',dept:'锅炉',desc:'锅炉主汽温度偏高',type:'智能预警',point:'主汽温度',val:'552.8℃',st:'confirmed'},
  {id:4,l:2,time:'2026-07-15 15:35:44',dept:'辅网',desc:'A磨煤机振动幅值超标',type:'阈值报警',point:'A磨煤机振动',val:'4.7mm/s',st:'unhandled'},
  {id:5,l:2,time:'2026-07-15 15:20:11',dept:'汽机',desc:'汽轮机轴向位移接近预警',type:'智能预警',point:'轴向位移',val:'0.68mm',st:'resolved'},
  {id:6,l:3,time:'2026-07-15 14:58:30',dept:'锅炉',desc:'NOx排放浓度上升',type:'智能预警',point:'NOx浓度',val:'68.3mg/m³',st:'resolved'},
  {id:7,l:3,time:'2026-07-15 14:42:05',dept:'辅网',desc:'除氧器水位偏低',type:'阈值报警',point:'除氧器水位',val:'0.82m',st:'confirmed'},
  {id:8,l:3,time:'2026-07-15 14:28:19',dept:'汽机',desc:'给水泵B效率下降预警',type:'智能预警',point:'给水泵B效率',val:'82.1%',st:'resolved'},
  {id:9,l:3,time:'2026-07-15 14:10:53',dept:'锅炉',desc:'B一次风机效率偏低',type:'智能预警',point:'B一次风机效率',val:'78.5%',st:'resolved'},
  {id:10,l:3,time:'2026-07-15 13:52:40',dept:'辅网',desc:'冷却塔入口水温偏高',type:'阈值报警',point:'冷却塔入口水温',val:'33.5℃',st:'resolved'},
])
const bars = computed(()=>[
  {lv:'all',label:'全部报警',cnt:alarms.value.length,color:'#3b82f6'},
  {lv:'1',label:'一级报警',cnt:alarms.value.filter(a=>a.l===1).length,color:'#ef4444'},
  {lv:'2',label:'二级报警',cnt:alarms.value.filter(a=>a.l===2).length,color:'#f59e0b'},
  {lv:'3',label:'智能预警',cnt:alarms.value.filter(a=>a.l===3).length,color:'#06b6d4'},
])
const filtered = computed(()=>filter.value==='all'?alarms.value:alarms.value.filter(a=>a.l===parseInt(filter.value)))
const showDetail = (row) => { cur.value = row }
const act = (row,st) => { row.st=st; cur.value=row }
const batch = (st) => { alarms.value.filter(a=>a.st==='unhandled').forEach(a=>a.st=st) }
</script>
<style scoped>
.lb{background:#0a0e17;border:1px solid transparent;border-radius:8px;padding:14px;text-align:center;cursor:pointer;transition:0.15s}
.lb:hover{border-color:#2a3544}.lb.ac{border-color:#3b82f6;background:rgba(59,130,246,0.08)}
.lb-v{font-size:28px;font-weight:600;margin-bottom:4px}.lb-l{font-size:11px;color:#94a3b8}
.cd{background:#111827;border:0.5px solid #1e293b;border-radius:10px;padding:16px}
.cd-h{display:flex;justify-content:space-between;margin-bottom:12px}
.cd-t{font-size:13px;font-weight:500}
.guide{background:#0a0e17;border:0.5px solid #1e293b;border-radius:10px;padding:14px;margin-top:14px}
.gt{font-size:14px;font-weight:500;color:#f59e0b;margin-bottom:12px}
.gs{background:#111827;border-radius:8px;padding:12px}.gsh{font-size:12px;font-weight:500;color:#3b82f6;margin-bottom:8px}
.gs p{font-size:12px;color:#94a3b8;line-height:1.7}
</style>
