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
const cur = ref('bearing')

const cases = [
  { id: 'bearing', name: 'A引风机轴承温度异常', kg: 8, kgLinks: 7, confidence: 94, refs: 3, cost: 128,
    steps: [
      { num: 1, c: '#3b82f6', title: '用户问询', body: '运行值班员发起故障溯源问询：<br>"A 引风机轴承温度异常，请进行故障溯源分析。"' },
      { num: 2, c: '#22c55e', title: '答案格式转换', body: '解析实体：设备 = A 引风机｜异常 = 轴承温度｜当前 82.3℃｜正常 45℃｜变化率 +0.8℃/min' },
      { num: 3, c: '#f59e0b', title: '知识检索', body: '知识图谱 2 跳邻居查询，匹配 3 条关联故障记录：<br>REF-1：润滑油脂劣化（94.2%）<br>REF-2：冷却水流量不足（87.6%）<br>REF-3：轴承内圈磨损（71.3%）' },
      { num: 4, c: '#ef4444', title: '推理结论', body: '<strong>主因（94.2%）：</strong>轴承润滑油脂劣化，8000h 未更换。<br><strong>操作建议：</strong>立即更换 ISO VG 68 油脂，降负荷至 80%，48h 内停机检查。' }
    ],
    conclusion: `综合判断：以 REF-1（润滑油脂劣化，置信度 94.2%）为最可能原因。建议立即安排润滑油脂更换，同步检查冷却水系统。<br><br>若温度超过 90℃，立即手动停机并启动备用引风机。<br><br>本结论已通过知识图谱 3 层推理验证，并参考近 12 个月 ${3} 条历史故障记录。` },
  { id: 'mill', name: 'A磨煤机振动超标', kg: 6, kgLinks: 5, confidence: 91, refs: 2, cost: 95,
    steps: [
      { num: 1, c: '#3b82f6', title: '用户问询', body: '智能预警系统自动推送：A 磨煤机振动超标，振动值 4.7mm/s。' },
      { num: 2, c: '#22c55e', title: '答案格式转换', body: '解析：设备 = A 磨煤机｜振动 = 4.7mm/s（阈值 4.5）｜电流波动 ±12%' },
      { num: 3, c: '#f59e0b', title: '知识检索', body: '匹配 4 条关联记录。振动频谱分析显示 1 倍转频分量增强，判断为磨辊不平衡。' },
      { num: 4, c: '#ef4444', title: '推理结论', body: '<strong>主因（91.8%）：</strong>磨辊磨损不均，动不平衡。<br><strong>操作建议：</strong>降低给煤量至 70%，检查磨辊磨损。' }
    ],
    conclusion: '以 REF-1（磨辊磨损，91.8%）为最可能原因。建议降低给煤量，安排更换磨损磨辊。' }
]

const curCase = computed(() => cases.find(c => c.id === cur.value) || cases[0])

const viewKnowledge = () => router.push('/knowledge-graph')
const exportReport = () => ElMessage.success('故障溯源报告已生成（含 4 步推理 + 引用溯源 + 操作建议），可在「统计报表 → 报告中心」下载')
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
