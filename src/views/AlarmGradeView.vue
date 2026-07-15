<template>
<div class="pg"><h2 style="margin-bottom:16px;font-size:16px;font-weight:500">报警分级管理</h2>
<el-row :gutter="14">
  <el-col :span="8" v-for="g in grades" :key="g.lv">
    <div class="gc" :class="'g'+g.lv">
      <div class="gh"><span class="gl">{{g.lv===1?'一级报警':g.lv===2?'二级报警':'智能预警'}}</span><span class="gn">{{g.t}}条</span></div>
      <div class="gd">{{g.d}}</div>
      <div class="gs">
        <div class="gsi"><div class="gsv" style="color:#22c55e">{{g.h}}</div><div>已处理</div></div>
        <div class="gsi"><div class="gsv" style="color:#ef4444">{{g.u}}</div><div>未处理</div></div>
        <div class="gsi"><div class="gsv">{{g.a}}分</div><div>平均响应</div></div>
      </div>
      <el-divider />
      <div class="gr-t">升级规则</div>
      <div class="gr-v" v-for="r in g.rs" :key="r">· {{r}}</div>
    </div>
  </el-col>
</el-row></div>
</template>

<script setup>
const grades = [
  {lv:1,t:8,h:3,u:5,a:1.2,d:'紧急：可能造成设备损坏或人员安全威胁，需立即处置',rs:['30分钟未处置自动升级至场站负责人','同步推送短信+钉钉+邮件','触发联动控制逻辑']},
  {lv:2,t:23,h:18,u:5,a:4.5,d:'关注：参数异常但暂无安全风险，需在当班内确认',rs:['2小时未确认自动升级为一级','推送站内消息+短信','生成巡检工单']},
  {lv:3,t:100,h:90,u:10,a:12,d:'预警：智能预测发现潜在风险趋势，建议关注',rs:['24小时未关闭需交接班处理','仅推送站内消息','纳入趋势分析报告']}
]
</script>

<style scoped>
.pg{background:#111827;padding:20px;border-radius:10px}
.gc{padding:18px;border-radius:10px;border:0.5px solid #1e293b;background:#0a0e17}
.g1{border-color:rgba(239,68,68,0.4)}.g2{border-color:rgba(245,158,11,0.4)}.g3{border-color:rgba(6,182,212,0.4)}
.gh{display:flex;justify-content:space-between;margin-bottom:10px}
.gl{font-size:16px;font-weight:600}.g1 .gl{color:#ef4444}.g2 .gl{color:#f59e0b}.g3 .gl{color:#06b6d4}
.gn{font-size:12px;color:#94a3b8}.gd{font-size:12px;color:#94a3b8;line-height:1.6;margin-bottom:16px}
.gs{display:flex;justify-content:space-around}.gsi{text-align:center}
.gsv{font-size:22px;font-weight:600}.gsi div:nth-child(2){font-size:11px;color:#94a3b8}
.gr-t{font-size:12px;font-weight:500;color:#3b82f6;margin-bottom:8px}
.gr-v{font-size:12px;color:#94a3b8;line-height:1.8}
</style>
