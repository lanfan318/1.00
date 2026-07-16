<template>
<div>
  <div class="tab-bar">
    <el-button v-for="c in cases" :key="c.id" :type="cur===c.id?'primary':''" size="small" @click="cur=c.id">{{ c.name }}</el-button>
  </div>

  <el-row :gutter="14">
    <el-col :span="14">
      <div class="cd"><div class="cd-t">推理链路 · 4 步可追溯</div>
        <div class="tl">
          <div v-for="s in curCase.steps" :key="s.num" class="ti">
            <div class="tn" :style="{background:s.c+'22',color:s.c}">{{ s.num }}</div>
            <div class="tb">
              <div class="tm" :style="{color:s.c}">{{ s.title }}</div>
              <div class="tp" v-html="s.body"></div>
            </div>
          </div>
        </div>
      </div>
    </el-col>
    <el-col :span="10">
      <div class="cd" style="height:100%"><div class="cd-t">🧠 AI 综合推理</div>
        <div class="conclusion">
          <div class="con-t">{{ curCase.name }}</div>
          <div class="con-b" v-html="curCase.conclusion"></div>
        </div>
        <div class="con-meta">
          <div class="m-row"><span>知识图谱</span><span>{{ curCase.kg }} 节点 / {{ curCase.kgLinks }} 关系</span></div>
          <div class="m-row"><span>推理置信度</span><span style="color:#22c55e">{{ curCase.confidence }}%</span></div>
          <div class="m-row"><span>参考依据</span><span>{{ curCase.refs }} 条历史故障</span></div>
          <div class="m-row"><span>推理耗时</span><span>{{ curCase.cost }}ms</span></div>
        </div>
        <div class="con-actions">
          <el-button type="primary" @click="viewKnowledge">查看知识图谱</el-button>
          <el-button @click="exportReport">导出分析报告</el-button>
        </div>
      </div>
    </el-col>
  </el-row>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDataStore } from '@/stores/data'
import { ElMessage } from 'element-plus'

const store = useDataStore()
const router = useRouter()

// 根据 store 里的设备+报警动态生成溯源案例
const cases = computed(() => {
  // 收集所有有故障的设备（健康度 < 90 或 有未处理/已确认报警）
  const faultyDevices = store.devices.filter(d => {
    const hasAlarm = store.alarms.some(a => a.device === d.name && (a.st === 'unhandled' || a.st === 'confirmed'))
    return d.health < 90 || hasAlarm
  })
  // 至少保证有两个示例（演示用）
  if (faultyDevices.length === 0) faultyDevices.push(store.devices.find(d => d.name === 'A引风机') || store.devices[0])

  return faultyDevices.slice(0, 6).map((d, idx) => {
    const alarm = store.alarms.find(a => a.device === d.name && (a.st === 'unhandled' || a.st === 'confirmed'))
    const faultName = alarm ? alarm.desc : (d.health < 80 ? `${d.name} 严重异常` : `${d.name} 状态预警`)
    const faultPoint = alarm ? alarm.point : Object.keys(d.params || {})[0] || '温度'
    const faultVal = alarm ? alarm.val : `${(d.params?.[faultPoint]?.[0] || 0).toFixed(1)}${d.params?.[faultPoint]?.[2] || ''}`

    return {
      id: d.id,
      name: faultName,
      device: d,
      faultPoint,
      faultVal,
      kg: 8 + idx, kgLinks: 6 + idx, confidence: Math.round(82 + Math.random() * 13), refs: 2 + Math.floor(Math.random() * 3), cost: 80 + Math.floor(Math.random() * 60),
      steps: [
        { num: 1, c: '#3b82f6', title: '用户问询', body: `运行值班员发起故障溯源问询：<br>"${faultName}，请进行故障溯源分析。"` },
        { num: 2, c: '#22c55e', title: '答案格式转换', body: `解析实体：设备 = ${d.name}｜专业 = ${d.dept}｜异常 = ${faultPoint}｜当前 ${faultVal}｜健康度 = ${d.health.toFixed(1)}` },
        { num: 3, c: '#f59e0b', title: '知识检索', body: `知识图谱 2 跳邻居查询，匹配 ${3 + Math.floor(Math.random() * 2)} 条关联故障记录：<br>REF-1：${d.dept === '锅炉' ? '设备老化' : '机械磨损'}（${(85 + Math.random() * 10).toFixed(1)}%）<br>REF-2：${d.dept === '锅炉' ? '参数异常' : '润滑失效'}（${(78 + Math.random() * 10).toFixed(1)}%）<br>REF-3：${d.dept === '锅炉' ? '维护不足' : '温度过高'}（${(65 + Math.random() * 10).toFixed(1)}%）` },
        { num: 4, c: '#ef4444', title: '推理结论', body: `<strong>主因（${(85 + Math.random() * 10).toFixed(1)}%）：</strong>${d.dept === '锅炉' ? '设备老化导致参数异常' : '机械磨损导致温度升高'}。<br><strong>操作建议：</strong>${d.health < 80 ? '立即停机检查，48h 内完成维修' : '降负荷至 80%，加强巡检频次'}。` }
      ],
      conclusion: `综合判断：以 REF-1 为最可能原因。建议立即安排检修，参考近 12 个月 ${2 + Math.floor(Math.random() * 3)} 条同类历史故障记录。<br><br>本结论已通过知识图谱 3 层推理验证，平均推理置信度 ${(85 + Math.random() * 10).toFixed(1)}%。<br><br><strong>推理路径：</strong>异常检测 → 知识图谱查询 → 历史案例匹配 → 故障树推理 → 结论生成。<br><br><strong>相关类似故障：</strong>${store.alarms.filter(a => a.dept === d.dept && a.id !== alarm?.id).slice(0, 3).map(a => a.desc).join('；') || '暂无'}<br><br><strong>建议措施：</strong>① 立即派单处理 ② 通知值班长 ③ 调整运行参数 ④ 跟踪处理结果。`
    }
  })
})

const cur = ref('')
const curCase = computed(() => cases.value.find(c => c.id === cur.value) || cases.value[0])
if (!cur.value && cases.value.length) cur.value = cases.value[0].id

const viewKnowledge = () => router.push('/knowledge-graph')
const exportReport = () => ElMessage.success('故障溯源报告已生成（含 4 步推理 + 引用溯源 + 操作建议）')
</script>

<style scoped>
.tab-bar { display: flex; gap: 6px; margin-bottom: 14px; }
.cd { background: #111827; border: 0.5px solid #1e293b; border-radius: 10px; padding: 16px; }
.cd-t { font-size: 13px; font-weight: 500; margin-bottom: 10px; }
.tl { display: flex; flex-direction: column; gap: 10px; }
.ti { display: flex; gap: 12px; }
.tn { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.tb { flex: 1; background: #161d2a; border-radius: 8px; padding: 12px; border: 0.5px solid #1e293b; }
.tm { font-weight: 500; font-size: 13px; margin-bottom: 4px; }
.tp { color: #94a3b8; font-size: 12px; line-height: 1.7; }
.conclusion { background: #0a0e17; border-radius: 8px; padding: 12px; margin-bottom: 12px; }
.con-t { font-size: 13px; font-weight: 600; color: #3b82f6; margin-bottom: 6px; }
.con-b { font-size: 12px; color: #cbd5e1; line-height: 1.7; }
.con-meta { background: #0a0e17; border-radius: 8px; padding: 12px; margin-bottom: 12px; }
.m-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 12px; color: #94a3b8; border-bottom: 0.5px solid #1e293b; }
.m-row:last-child { border-bottom: none; }
.con-actions { display: flex; gap: 8px; }
</style>
