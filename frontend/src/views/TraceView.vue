<template>
<div class="tr-page">
  <div class="tr-head">
    <div class="tr-head-l">
      <span class="tr-bar"></span>
      <h2>故障溯源 · AI 综合推理</h2>
    </div>
    <span class="tr-sub">从异常检测到结论生成的完整可追溯链路</span>
  </div>

  <div class="case-bar">
    <span class="case-lbl">故障案例（点击切换）：</span>
    <div class="case-cards">
      <div v-for="c in cases" :key="c.id" class="case-card" :class="{on:cur===c.id}" @click="cur=c.id">
        <div class="cc-dot" :style="{background: c.device.health<70?'#ef4444':c.device.health<85?'#fbbf24':'#3eaaff', boxShadow:'0 0 8px '+(c.device.health<70?'#ef4444':c.device.health<85?'#fbbf24':'#3eaaff')}"></div>
        <div class="cc-info">
          <div class="cc-name">{{ c.name }}</div>
          <div class="cc-dept">{{ c.device.dept }} · 健康度 {{ c.device.health.toFixed(1) }}</div>
        </div>
        <div class="cc-conf" :style="{color: c.device.health<70?'#ef4444':c.device.health<85?'#fbbf24':'#34d399'}">{{ c.confidence }}%</div>
      </div>
    </div>
  </div>

  <div class="tr-main">
    <div class="tr-top">
      <el-row :gutter="14" style="height:100%">
        <el-col :span="14" style="height:100%">
          <div class="cd tr-links" style="height:100%"><div class="cd-t"><span class="ut-ic">▸</span>推理链路 · 4 步可追溯（问答 → 判断）</div>
            <div class="tl">
              <div v-for="s in curCase.steps" :key="s.num" class="ti">
                <div class="tn" :style="{background:s.c+'22',color:s.c, boxShadow:'0 0 10px '+s.c+'55', borderColor:s.c+'66'}">{{ s.num }}</div>
                <div class="tb">
                  <div class="tm" :style="{color:s.c}">
                    <span class="tm-dot" :style="{background:s.c}"></span>{{ s.title }}
                  </div>
                  <div class="tp" v-html="s.body"></div>
                </div>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="10" style="height:100%">
          <div class="cd tr-concl" style="height:100%"><div class="cd-t"><span class="ut-ic">▸</span>AI 综合推理</div>
            <div class="conclusion">
              <div class="con-t">{{ curCase.name }}</div>
              <div class="con-b" v-html="curCase.conclusion"></div>
            </div>
            <div class="con-meta">
              <div class="m-row"><span class="m-k">知识图谱</span><span class="m-v">{{ curCase.kg }} 节点 / {{ curCase.kgLinks }} 关系</span></div>
              <div class="m-row"><span class="m-k">推理置信度</span><span class="m-v" :style="{color: curCase.confidence >= 90 ? '#34d399' : curCase.confidence >= 70 ? '#fbbf24' : '#ef4444'}">{{ curCase.confidence }}%</span></div>
              <div class="m-row"><span class="m-k">参考依据</span><span class="m-v">{{ curCase.refs }} 条历史故障</span></div>
              <div class="m-row"><span class="m-k">推理耗时</span><span class="m-v">{{ curCase.cost }}ms</span></div>
            </div>
            <div class="con-actions">
              <el-button type="primary" @click="viewKnowledge">查看知识图谱</el-button>
              <el-button @click="exportReport">导出分析报告</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 操作审计时间线：问答 → 判断 → 操作 → 结果 -->
    <div class="tr-audit">
      <div class="cd" style="height:100%">
        <div class="cd-t"><span class="ut-ic">▸</span>操作审计时间线 · 问答 → 判断 → 操作 → 结果（与监盘日志 / 操作记录打通）</div>
        <div class="audit-wrap">
          <div v-for="(p, i) in auditTimeline" :key="p.key" class="aud" :class="{last: i === auditTimeline.length - 1}">
            <div class="aud-line">
              <div class="aud-dot" :style="{background:p.color, boxShadow:'0 0 10px '+p.color}"></div>
              <div v-if="i < auditTimeline.length - 1" class="aud-bar"></div>
            </div>
            <div class="aud-card">
              <div class="aud-ph" :style="{color:p.color}">{{ p.phase }}</div>
              <div class="aud-meta"><span class="aud-actor">{{ p.actor }}</span><span class="aud-time">{{ p.time }}</span></div>
              <div class="aud-tt">{{ p.title }}</div>
              <div class="aud-detail">{{ p.detail }}</div>
              <div class="aud-link">
                <span class="aud-link-k">{{ p.link }}</span>
                <span class="aud-link-v">{{ p.linkText }}</span>
              </div>
              <div v-if="p.key === 'result'" class="aud-actions">
                <el-button size="small" :disabled="p.st !== 'unhandled'" @click="markResult('confirmed')">标记处理中</el-button>
                <el-button size="small" type="primary" :disabled="p.st === 'resolved'" @click="markResult('resolved')">标记已处置</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
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
  const faultyDevices = store.devices.filter(d => {
    const hasAlarm = store.alarms.some(a => a.device === d.name && (a.st === 'unhandled' || a.st === 'confirmed'))
    return d.health < 90 || hasAlarm
  })
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
      alarm: alarm || null,
      alarmId: alarm?.id || null,
      alarmTime: alarm?.time || new Date(Date.now() - 30 * 60000),
      kg: 8 + idx, kgLinks: 6 + idx, confidence: Math.min(98, Math.round(76 + (100 - d.health) * 0.5)), refs: 2 + (idx % 3), cost: 92 + idx * 11,
      steps: [
        { num: 1, c: '#3eaaff', title: '用户问询', body: `运行值班员发起故障溯源问询：<br>"${faultName}，请进行故障溯源分析。"` },
        { num: 2, c: '#34d399', title: '答案格式转换', body: `解析实体：设备 = ${d.name}｜专业 = ${d.dept}｜异常 = ${faultPoint}｜当前 ${faultVal}｜健康度 = ${d.health.toFixed(1)}` },
        { num: 3, c: '#fbbf24', title: '知识检索', body: `知识图谱 2 跳邻居查询，匹配 ${3 + (idx % 2)} 条关联故障记录：<br>REF-1：${d.dept === '锅炉' ? '设备老化' : '机械磨损'}（${(86 + (idx * 3) % 10).toFixed(1)}%）<br>REF-2：${d.dept === '锅炉' ? '参数异常' : '润滑失效'}（${(79 + (idx * 5) % 10).toFixed(1)}%）<br>REF-3：${d.dept === '锅炉' ? '维护不足' : '温度过高'}（${(66 + (idx * 7) % 10).toFixed(1)}%）` },
        { num: 4, c: '#ef4444', title: '推理结论', body: `<strong>主因（${(86 + (idx * 3) % 10).toFixed(1)}%）：</strong>${d.dept === '锅炉' ? '设备老化导致参数异常' : '机械磨损导致温度升高'}。<br><strong>操作建议：</strong>${d.health < 80 ? '立即停机检查，48h 内完成维修' : '降负荷至 80%，加强巡检频次'}。` }
      ],
      conclusion: `综合判断：以 REF-1 为最可能原因。建议立即安排检修，参考近 12 个月 ${2 + (idx % 3)} 条同类历史故障记录。<br><br>本结论已通过知识图谱 3 层推理验证，平均推理置信度 ${(86 + (idx * 3) % 10).toFixed(1)}%。<br><br><strong>推理路径：</strong>异常检测 → 知识图谱查询 → 历史案例匹配 → 故障树推理 → 结论生成。<br><br><strong>相关类似故障：</strong>${store.alarms.filter(a => a.dept === d.dept && a.id !== alarm?.id).slice(0, 3).map(a => a.desc).join('；') || '暂无'}<br><br><strong>建议措施：</strong>① 立即派单处理 ② 通知值班长 ③ 调整运行参数 ④ 跟踪处理结果。`
    }
  })
})

const cur = ref('')
const curCase = computed(() => cases.value.find(c => c.id === cur.value) || cases.value[0])
if (!cur.value && cases.value.length) cur.value = cases.value[0].id

const viewKnowledge = () => router.push('/knowledge-graph')
const exportReport = () => ElMessage.success('故障溯源报告已生成（含 4 步推理 + 引用溯源 + 操作建议）')

// ════════════════════════════════════════
// 操作审计时间线 —— 问答 → 判断 → 操作 → 结果
// 与监盘日志 / 操作记录打通，处置闭环可标记
// ════════════════════════════════════════
const STATUS_LABEL = { unhandled: '未处置', confirmed: '处理中', resolved: '已处置' }
const statusLabel = (s) => STATUS_LABEL[s] || '未知'

const markResult = (st) => {
  const c = curCase.value
  if (c && c.alarmId) {
    store.setAlarmStatus(c.alarmId, st)
    ElMessage.success('处置状态已更新：' + statusLabel(st))
  } else {
    ElMessage.info('当前案例无关联实时告警，仅作演示')
  }
}

const auditTimeline = computed(() => {
  const c = curCase.value
  if (!c) return []
  const base = c.alarmTime instanceof Date ? c.alarmTime : new Date(c.alarmTime)
  const fmt = (d) => { const p = n => String(n).padStart(2, '0'); return `${p(d.getHours())}:${p(d.getMinutes())}` }
  const st = c.alarm?.st || 'unhandled'
  const qaT = new Date(base.getTime() - 8 * 60000)
  const judgeT = new Date(base.getTime() - 2 * 60000)
  const opT = new Date(base.getTime() + 5 * 60000)
  const resT = st === 'resolved' ? new Date(base.getTime() + 45 * 60000) : st === 'confirmed' ? opT : null
  const wo = '#WO-' + (c.alarmId || '000').toUpperCase()
  return [
    { key: 'qa', phase: '问答', color: '#3eaaff', actor: '运行值班员', time: fmt(qaT), title: '发起故障溯源问询',
      detail: `"${c.name}，请进行故障溯源分析。"`,
      link: '监盘日志', linkText: `监盘发现 ${c.device.name} ${c.faultPoint} ${c.faultVal}` },
    { key: 'judge', phase: '判断', color: '#34d399', actor: 'AI 智能体', time: fmt(judgeT), title: '生成推理结论',
      detail: `主因匹配完成，置信度 ${c.confidence}%，已输出根因与操作建议。`,
      link: '推理记录', linkText: `知识图谱 ${c.kg} 节点 / ${c.kgLinks} 关系` },
    { key: 'op', phase: '操作', color: '#fbbf24', actor: '值班长', time: fmt(opT), title: '派单与操作指导',
      detail: '已生成处置工单并下发操作指导（处置步骤 → 风险提示 → 规程依据）。',
      link: '操作记录', linkText: st === 'unhandled' ? '待派单' : '工单执行中 ' + wo },
    { key: 'result', phase: '结果', color: st === 'resolved' ? '#22c55e' : st === 'confirmed' ? '#fbbf24' : '#ef4444', actor: '系统 / 值班长', time: resT ? fmt(resT) : '—', title: '处置闭环', st,
      detail: st === 'resolved' ? '故障已闭环，设备恢复正常运行。' : st === 'confirmed' ? '处置进行中，等待现场反馈。' : '尚未处置，等待派单。',
      link: '闭环状态', linkText: statusLabel(st) }
  ]
})
</script>

<style scoped>
.tr-page { display: flex; flex-direction: column; gap: 14px; height: 100%; overflow: hidden; }
.tr-page :deep(.el-row) { height: 100%; }
.tr-main { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 12px; }
.tr-top { flex: 1.1; min-height: 0; }
.tr-top :deep(.el-row) { height: 100%; }
.tr-top :deep(.el-col) { height: 100%; }
.tr-audit { flex: 0.95; min-height: 0; }
.tr-audit .cd { overflow: auto; }
.tr-head { flex-shrink: 0; display: flex; align-items: baseline; gap: 16px; flex-wrap: wrap; }
.tr-head-l { display: flex; align-items: center; gap: 12px; }
.tr-bar { width: 4px; height: 22px; background: linear-gradient(180deg, #3eaaff, #22d3ee); border-radius: 2px; box-shadow: 0 0 10px rgba(62,170,255,0.5); }
.tr-head h2 { font-size: 17px; font-weight: 600; color: #d4ecff; letter-spacing: 0.5px; }
.tr-sub { font-size: 13px; color: #9fb6cf; }
.ut-ic { color: #3eaaff; font-weight: 700; margin-right: 2px; }

.case-bar { flex-shrink: 0; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.case-lbl { font-size: 13px; color: #9fb6cf; flex-shrink: 0; font-weight: 500; }
.case-cards { display: flex; gap: 8px; flex-wrap: wrap; flex: 1; }
.case-card { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: linear-gradient(180deg, rgba(5,12,21,0.42), rgba(3,8,15,0.385)); border: 1px solid rgba(62,170,255,0.12); border-radius: 7px; cursor: pointer; transition: all 0.2s; min-width: 190px; position: relative; overflow: hidden; }
.case-card:hover { border-color: rgba(62,170,255,0.35); background: linear-gradient(180deg, rgba(6,14,25,0.455), rgba(4,10,19,0.406)); transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,5,15,0.21); }
.case-card.on { border-color: #3eaaff; background: linear-gradient(180deg, rgba(18,45,80,0.5), rgba(12,30,52,0.45)); box-shadow: 0 0 20px rgba(62,170,255,0.18), inset 0 0 24px rgba(62,170,255,0.04); }
.cc-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.cc-name { font-size: 13px; color: #e2e8f0; font-weight: 600; margin-bottom: 2px; max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cc-dept { font-size: 11px; color: #9fb6cf; }
.cc-conf { margin-left: auto; font-size: 15px; font-weight: 800; font-family: "SF Mono","Consolas","Orbitron",monospace; text-shadow: 0 0 10px currentColor; }

.cd { background: linear-gradient(135deg, rgba(12,30,52,0.7) 0%, rgba(4,10,19,0.455) 100%); border: 1px solid rgba(62,170,255,0.16); border-radius: 7px; padding: 18px; box-shadow: 0 2px 12px rgba(0,5,15,0.21), inset 0 1px 0 rgba(62,170,255,0.06); }
.cd-t { font-size: 14px; font-weight: 700; color: #d4ecff; margin-bottom: 14px; display: flex; align-items: center; letter-spacing: 0.5px; }

.tl { display: flex; flex-direction: column; gap: 12px; }
.ti { display: flex; gap: 14px; }
.tn { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 800; flex-shrink: 0; border: 2px solid; box-shadow: 0 0 12px currentColor; }
.tb { flex: 1; background: linear-gradient(180deg, rgba(5,12,21,0.42), rgba(3,8,15,0.385)); border: 1px solid rgba(62,170,255,0.12); border-radius: 7px; padding: 14px 16px; transition: all 0.2s; }
.tb:hover { border-color: rgba(62,170,255,0.25); }
.tm { font-weight: 700; font-size: 14px; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.tm-dot { width: 7px; height: 7px; border-radius: 50%; box-shadow: 0 0 6px currentColor; }
.tp { color: #cbd5e1; font-size: 13px; line-height: 1.9; }

.tr-concl { display: flex; flex-direction: column; overflow-y: auto; }
.tr-links { overflow-y: auto; }
.conclusion { background: linear-gradient(180deg, rgba(5,12,21,0.42), rgba(3,8,15,0.385)); border: 1px solid rgba(62,170,255,0.15); border-left: 3px solid #3eaaff; border-radius: 7px; padding: 16px; margin-bottom: 14px; box-shadow: 0 2px 12px rgba(0,5,15,0.14); }
.con-t { font-size: 15px; font-weight: 700; color: #3eaaff; margin-bottom: 10px; text-shadow: 0 0 8px rgba(62,170,255,0.3); }
.con-b { font-size: 13px; color: #cbd5e1; line-height: 1.9; }
.con-meta { background: linear-gradient(180deg, rgba(3,9,18,0.42), rgba(4,11,21,0.35)); border-radius: 7px; padding: 14px 16px; margin-bottom: 14px; border: 1px solid rgba(62,170,255,0.08); }
.m-row { display: flex; justify-content: space-between; padding: 7px 0; font-size: 13px; color: #9fb6cf; border-bottom: 1px solid rgba(62,170,255,0.08); }
.m-row:last-child { border-bottom: none; }
.m-k { color: #8fb0cf; font-weight: 500; }
.m-v { color: #e2e8f0; font-weight: 600; }
.con-actions { display: flex; gap: 8px; margin-top: auto; }

/* ════════════════════════════════════════
   操作审计时间线 新增样式
   ════════════════════════════════════════ */
.audit-wrap { display: flex; gap: 0; padding-top: 4px; }
.aud { flex: 1; display: flex; flex-direction: column; padding: 0 6px; }
.aud-line { display: flex; align-items: center; height: 24px; }
.aud-dot { width: 16px; height: 16px; border-radius: 50%; flex-shrink: 0; border: 2px solid rgba(255,255,255,0.25); }
.aud-bar { flex: 1; height: 2px; margin: 0 6px; background: linear-gradient(90deg, rgba(62,170,255,0.55), rgba(62,170,255,0.12)); border-radius: 2px; }
.aud-card {
  margin-top: 10px; padding: 12px 14px; border-radius: 8px;
  background: linear-gradient(180deg, rgba(5,12,21,0.42), rgba(3,8,15,0.385));
  border: 1px solid rgba(62,170,255,0.12); border-top: 2px solid rgba(62,170,255,0.4);
  min-height: 150px; position: relative; transition: all 0.2s;
}
.aud-card:hover { border-color: rgba(62,170,255,0.28); transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,10,30,0.3); }
.aud-ph { font-size: 14px; font-weight: 800; letter-spacing: 1px; margin-bottom: 6px; text-shadow: 0 0 8px currentColor; }
.aud-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 11px; }
.aud-actor { color: #9fd0ff; background: rgba(62,170,255,0.1); padding: 1px 8px; border-radius: 8px; border: 1px solid rgba(62,170,255,0.18); }
.aud-time { color: #8fb0cf; font-family: "SF Mono","Consolas",monospace; }
.aud-tt { font-size: 13px; color: #e2e8f0; font-weight: 600; margin-bottom: 6px; }
.aud-detail { font-size: 12px; color: #aebfd2; line-height: 1.7; margin-bottom: 10px; }
.aud-link {
  display: flex; gap: 8px; align-items: baseline; padding: 7px 10px;
  background: rgba(3,9,18,0.385); border-radius: 6px; border: 1px solid rgba(62,170,255,0.1);
  font-size: 11.5px;
}
.aud-link-k { color: #6a8caa; flex-shrink: 0; font-weight: 600; }
.aud-link-v { color: #cbd5e1; line-height: 1.5; }
.aud-actions { display: flex; gap: 6px; margin-top: 10px; }
.aud-actions :deep(.el-button) { font-size: 11px; padding: 5px 10px; }
</style>
