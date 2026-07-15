<template>
<div>
  <div style="margin-bottom:14px;display:flex;gap:6px">
    <el-button v-for="c in cases" :key="c.id" :type="cur===c.id?'primary':''" size="small" @click="cur=c.id">{{c.n}}</el-button>
  </div>
  <div class="tl">
    <div v-for="s in steps" :key="s.num" class="ti"><div class="tn" :style="{background:s.c+'22',color:s.c}">{{s.num}}</div><div class="tb"><div class="tm" :style="{color:s.c}">{{s.title}}</div><div class="tp" v-html="s.body"></div></div></div>
  </div>
  <div class="in"><div class="in-t">AI 综合推理结论</div><div class="in-b" v-html="conclusion"></div></div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
const cur=ref('bearing')
const cases=[
  {id:'bearing',n:'A引风机轴承温度异常',steps:[
    {num:1,c:'#3b82f6',title:'用户问询',body:'运行值班员发起故障溯源问询：<br>"A引风机轴承温度异常，请进行故障溯源分析。"'},
    {num:2,c:'#22c55e',title:'答案格式转换',body:'解析实体：设备=A引风机｜异常=轴承温度｜当前82.3℃｜正常45℃｜变化率+0.8℃/min'},
    {num:3,c:'#f59e0b',title:'知识检索',body:'知识图谱2跳邻居查询，匹配3条关联故障记录：<br>REF-1: 润滑油脂劣化（94.2%）<br>REF-2: 冷却水流量不足（87.6%）<br>REF-3: 轴承内圈磨损（71.3%）'},
    {num:4,c:'#ef4444',title:'推理结论',body:'<strong>主因（94.2%）：</strong>轴承润滑油脂劣化，8000h未更换。<br><strong>操作建议：</strong>立即更换ISO VG 68油脂，降负荷至80%，48h内停机检查。'}
  ],conclusion:'综合判断：以REF-1（润滑油脂劣化，置信度94.2%）为最可能原因。建议立即安排润滑油脂更换，同步检查冷却水系统。若温度超过90℃，立即手动停机并启动备用引风机。'},
  {id:'mill',n:'A磨煤机振动超标',steps:[
    {num:1,c:'#3b82f6',title:'用户问询',body:'智能预警系统自动推送：A磨煤机振动超标，振动值4.7mm/s。'},
    {num:2,c:'#22c55e',title:'答案格式转换',body:'解析：设备=A磨煤机｜振动=4.7mm/s（阈值4.5）｜电流波动±12%'},
    {num:3,c:'#f59e0b',title:'知识检索',body:'匹配4条关联记录。振动频谱分析显示1倍转频分量增强，判断为磨辊不平衡。'},
    {num:4,c:'#ef4444',title:'推理结论',body:'<strong>主因（91.8%）：</strong>磨辊磨损不均，动不平衡。<br><strong>操作建议：</strong>降低给煤量至70%，检查磨辊磨损。若振动>5.5mm/s，立即停磨。'}
  ],conclusion:'以REF-1（磨辊磨损，91.8%）为最可能原因。建议降低给煤量，安排更换磨损磨辊。'}
]
const steps = computed(()=>cases.find(c=>c.id===cur.value)?.steps||[])
const conclusion = computed(()=>cases.find(c=>c.id===cur.value)?.conclusion||'')
</script>
<style scoped>
.tl{display:flex;flex-direction:column;gap:12px;margin-bottom:14px}
.ti{display:flex;gap:12px}.tn{width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:600;flex-shrink:0}
.tb{flex:1;background:#161d2a;border-radius:8px;padding:12px;border:0.5px solid #1e293b}.tm{font-weight:500;font-size:13px;margin-bottom:4px}.tp{color:#94a3b8;font-size:12px;line-height:1.7}
.in{background:#0a0e17;border:0.5px solid #1e293b;border-radius:10px;padding:14px}.in-t{font-size:12px;font-weight:500;color:#3b82f6;margin-bottom:6px}.in-b{font-size:12px;color:#94a3b8;line-height:1.8}
</style>
