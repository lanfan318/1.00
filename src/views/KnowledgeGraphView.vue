<template>
<div class="pg">
  <div class="pg-h"><h2>知识图谱可视化</h2><el-input v-model="kw" placeholder="搜索实体..." style="width:240px" clearable/></div>
  <el-row :gutter="14">
    <el-col :span="5">
      <div class="cd"><div class="cd-t">图谱分类</div>
        <el-menu :default-active="cat" @select="cat=$event"><el-menu-item index="all">全部图谱</el-menu-item><el-menu-item index="boiler">锅炉故障</el-menu-item><el-menu-item index="turbine">汽轮机故障</el-menu-item><el-menu-item index="aux">辅机故障</el-menu-item></el-menu></div>
    </el-col>
    <el-col :span="19"><div class="cd" style="height:600px"><div ref="kc" style="height:560px"></div></div></el-col>
  </el-row>
  <el-row :gutter="14" style="margin-top:14px">
    <el-col :span="6"><div class="sk"><div class="sk-v" style="color:#3b82f6">156</div><div class="sk-l">设备实体</div></div></el-col>
    <el-col :span="6"><div class="sk"><div class="sk-v" style="color:#ef4444">89</div><div class="sk-l">故障实体</div></div></el-col>
    <el-col :span="6"><div class="sk"><div class="sk-v" style="color:#f59e0b">234</div><div class="sk-l">原因实体</div></div></el-col>
    <el-col :span="6"><div class="sk"><div class="sk-v" style="color:#22c55e">187</div><div class="sk-l">方案实体</div></div></el-col>
  </el-row>
</div>
</template>

<script setup>
import { ref,onMounted,onUnmounted } from 'vue'
import * as echarts from 'echarts'
const kw=ref(''),cat=ref('all'),kc=ref(null)
let chart
onMounted(()=>{
  chart=echarts.init(kc.value)
  chart.setOption({tooltip:{formatter:'{b}'},series:[{type:'graph',layout:'force',roam:true,force:{repulsion:250,edgeLength:80},
    categories:[{name:'设备'},{name:'故障'},{name:'原因'},{name:'方案'}],
    label:{show:true,color:'#e2e8f0',fontSize:11},
    data:[
      {name:'A引风机',symbolSize:38,category:0,itemStyle:{color:'#3b82f6'}},
      {name:'A磨煤机',symbolSize:32,category:0,itemStyle:{color:'#3b82f6'}},
      {name:'给水泵B',symbolSize:32,category:0,itemStyle:{color:'#3b82f6'}},
      {name:'轴承温度高',symbolSize:30,category:1,itemStyle:{color:'#ef4444'}},
      {name:'振动超标',symbolSize:28,category:1,itemStyle:{color:'#ef4444'}},
      {name:'效率下降',symbolSize:26,category:1,itemStyle:{color:'#ef4444'}},
      {name:'润滑失效',symbolSize:24,category:2,itemStyle:{color:'#f59e0b'}},
      {name:'冷却不足',symbolSize:22,category:2,itemStyle:{color:'#f59e0b'}},
      {name:'磨辊磨损',symbolSize:22,category:2,itemStyle:{color:'#f59e0b'}},
      {name:'气蚀',symbolSize:22,category:2,itemStyle:{color:'#f59e0b'}},
      {name:'更换油脂',symbolSize:22,category:3,itemStyle:{color:'#22c55e'}},
      {name:'清洗滤网',symbolSize:20,category:3,itemStyle:{color:'#22c55e'}},
      {name:'更换磨辊',symbolSize:20,category:3,itemStyle:{color:'#22c55e'}},
      {name:'检修叶轮',symbolSize:20,category:3,itemStyle:{color:'#22c55e'}}
    ],
    links:[
      {source:'A引风机',target:'轴承温度高'},{source:'A引风机',target:'效率下降'},
      {source:'A磨煤机',target:'振动超标'},{source:'给水泵B',target:'效率下降'},
      {source:'轴承温度高',target:'润滑失效'},{source:'轴承温度高',target:'冷却不足'},
      {source:'振动超标',target:'磨辊磨损'},{source:'效率下降',target:'气蚀'},
      {source:'润滑失效',target:'更换油脂'},{source:'冷却不足',target:'清洗滤网'},
      {source:'磨辊磨损',target:'更换磨辊'},{source:'气蚀',target:'检修叶轮'}
    ],lineStyle:{color:'#2a3544',curveness:0.1}}]})
})
onUnmounted(()=>chart?.dispose())
</script>
<style scoped>
.pg{background:#111827;padding:20px;border-radius:10px}
.pg-h{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}
h2{font-size:16px;font-weight:500}
.cd{background:#0a0e17;border:0.5px solid #1e293b;border-radius:8px;padding:16px}
.cd-t{font-size:13px;color:#94a3b8;margin-bottom:10px}
.sk{background:#0d1117;padding:14px;border-radius:6px;text-align:center}
.sk-v{font-size:26px;font-weight:600}.sk-l{font-size:11px;color:#94a3b8;margin-top:4px}
</style>
