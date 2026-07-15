<template>
<div class="ac">
  <div class="ag-cards">
    <div v-for="(f,i) in fns" :key="i" class="ag-ci" :class="{on:mode===i}" @click="quick(i)"><div class="ag-ic">{{f.ic}}</div><div class="ag-lb">{{f.lb}}</div></div>
  </div>
  <div class="ag-ms" ref="msgs">
    <div class="am bot"><div class="am-h">HiFire Agent · 运行智能体</div><div class="am-b">您好，我是<span class="hl">火电运行AI智能体</span>。点击上方功能卡快速切入分析模块，或直接输入问题。</div></div>
  </div>
  <div class="ag-inp"><el-input v-model="q" placeholder="输入问题后按回车..." @keyup.enter="send"/><el-button type="primary" @click="send">发送</el-button></div>
</div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
const q=ref(''),mode=ref(-1),msgs=ref(null)
const fns=[{ic:'📊',lb:'设备工况',p:'请分析A引风机当前工况'},{ic:'🔔',lb:'报警分析',p:'请分析今日报警情况'},{ic:'📈',lb:'数据分析',p:'分析最近1h机组运行趋势'},{ic:'🔍',lb:'故障溯源',p:'A引风机轴承温度异常溯源'},{ic:'📚',lb:'知识库',p:'锅炉主汽温度过高常见原因'},{ic:'📋',lb:'政策分析',p:'当前参数是否符合安全规范'},{ic:'🎯',lb:'寻优操作',p:'给出降低煤耗优化建议'},{ic:'📝',lb:'运行日志',p:'生成本班运行日志'}]
const scroll=()=>nextTick(()=>{const e=msgs.value;if(e)e.scrollTop=e.scrollHeight})
const add=(t,h)=>{const d=document.createElement('div');d.className='am '+t;d.innerHTML=h;msgs.value.appendChild(d);scroll()}
const quick=(i)=>{mode.value=i;q.value=fns[i].p;send();q.value=''}
const send=()=>{const x=q.value.trim();if(!x)return;mode.value=-1;add('usr',`<div class="am-h">${new Date().toLocaleTimeString('zh-CN')} · 运行值班员</div><div class="am-b">${x}</div>`);q.value=''
  setTimeout(()=>{
    let r
    if(x.includes('工况')||x.includes('引风机'))r=`<div class="am-b"><div style="font-weight:500;margin-bottom:8px">A引风机 工况分析</div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px;border-bottom:0.5px solid #1e293b"><span>机械负荷</span><span class="hl">500.0 MW</span></div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px;border-bottom:0.5px solid #1e293b"><span>比功</span><span>2854.9 Nm/kg</span></div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px;border-bottom:0.5px solid #1e293b"><span>体积流量</span><span>157.8 m³/s</span></div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px;border-bottom:0.5px solid #1e293b"><span>效率</span><span style="color:#f59e0b">51%</span></div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px;border-bottom:0.5px solid #1e293b"><span>轴承温度</span><span style="color:#ef4444">82.3℃ ⚠</span></div><div style="margin-top:8px;color:#f59e0b">综合：健康度82.3，轴承温度偏高，建议更换润滑油脂。</div></div>`
    else if(x.includes('报警分析')||x.includes('报警情况'))r=`<div class="am-b"><div style="font-weight:500;margin-bottom:8px">今日报警分析</div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px"><span>总数</span><span class="hl">10 条</span></div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px"><span>未处理</span><span style="color:#ef4444">3 条</span></div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px"><span>一级/二级/预警</span><span>2/3/5 条</span></div><div style="margin-top:8px;color:#f59e0b">关键：A引风机轴承温度、润滑油压力需立即处置。</div></div>`
    else if(x.includes('数据分析')||x.includes('趋势'))r=`<div class="am-b"><div style="font-weight:500;margin-bottom:8px">1h运行数据分析</div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px"><span>负荷稳定性</span><span style="color:#22c55e">±1.2%</span></div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px"><span>主汽温度波动</span><span>±3.5℃</span></div><div style="display:flex;justify-content:space-between;padding:2px 0;font-size:12px"><span>煤耗趋势</span><span style="color:#f59e0b">+1.8g/kWh ↑</span></div><div style="margin-top:8px;color:#f59e0b">趋势预警：煤耗上升，建议关注燃烧效率。</div></div>`
    else if(x.includes('故障')||x.includes('溯源'))r=`<div class="am-b"><div style="font-weight:500;margin-bottom:8px">A引风机 — 故障溯源</div><div style="margin-bottom:4px"><span class="tg tg-w">REF-1 · 94.2%</span> 轴承润滑油脂劣化</div><div style="margin-bottom:4px"><span class="tg tg-i">REF-2 · 87.6%</span> 冷却水流量不足</div><div style="margin-bottom:8px"><span class="tg tg-i">REF-3 · 71.3%</span> 轴承内圈磨损</div><div style="color:#f59e0b">结论：以REF-1为最可能原因，建议更换油脂。</div></div>`
    else if(x.includes('寻优')||x.includes('优化')||x.includes('煤耗'))r=`<div class="am-b"><div style="font-weight:500;margin-bottom:8px">降低煤耗 — 寻优建议</div><div style="line-height:1.8">1. 优化煤粉细度（预计 -1.2g/kWh）<br>2. 调整过量空气系数至1.15（-0.5g/kWh）<br>3. 提高主汽温度至543℃<br><span style="color:#22c55e;margin-top:6px;display:inline-block">预计降煤耗2-3g/kWh，年节省¥150万。</span></div></div>`
    else r=`<div class="am-b">已收到："${x}"。系统正在分析，您也可点击上方功能卡获取精准结果。</div>`
    add('bot',`<div class="am-h">${new Date().toLocaleTimeString('zh-CN')} · HiFire Agent</div>`+r)
  },500)}
</script>
<style scoped>
.ac{display:flex;flex-direction:column;height:calc(100vh - 80px)}
.ag-cards{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-bottom:14px}
.ag-ci{background:#161d2a;border:0.5px solid #1e293b;border-radius:8px;padding:14px;cursor:pointer;text-align:center;transition:0.15s}
.ag-ci:hover,.ag-ci.on{border-color:#3b82f6;background:rgba(59,130,246,0.12)}.ag-ic{font-size:24px;margin-bottom:6px}.ag-lb{font-size:12px;font-weight:500}
.ag-ms{flex:1;overflow-y:auto;margin-bottom:12px}.ag-inp{display:flex;gap:8px}.ag-inp .el-input{flex:1}
.am{margin-bottom:12px;padding:12px 14px;border-radius:8px;max-width:90%}.am.usr{background:rgba(59,130,246,0.12);margin-left:auto;color:#e2e8f0}.am.bot{background:#161d2a;margin-right:auto;border:0.5px solid #1e293b}
.am-h{font-size:11px;color:#64748b;margin-bottom:6px}.am-b{font-size:13px;line-height:1.7}.hl{color:#3b82f6;font-weight:500}
.tg{display:inline-block;padding:2px 8px;border-radius:4px;font-size:11px}.tg-w{background:rgba(245,158,11,0.12);color:#f59e0b}.tg-i{background:rgba(59,130,246,0.12);color:#3b82f6}
</style>
