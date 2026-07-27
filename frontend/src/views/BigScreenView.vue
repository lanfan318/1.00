<template>
<div class="bs" @click="onBg">
  <!-- ══════════════════════════════════════
       八边形科技外框 — 参考"科学计算子系统"
       整体是一个有机的科技界面，不是方框拼接
       ══════════════════════════════════════ -->
  <div class="bs-oct-frame">
    <!-- 四角 L 型装饰块 -->
    <div class="oct-cr tl"><span class="oct-cr-in"></span></div>
    <div class="oct-cr tr"><span class="oct-cr-in"></span></div>
    <div class="oct-cr bl"><span class="oct-cr-in"></span></div>
    <div class="oct-cr br"><span class="oct-cr-in"></span></div>
    <!-- 边缘装饰线 -->
    <div class="oct-edge t"></div><div class="oct-edge b"></div>
    <div class="oct-edge l"></div><div class="oct-edge r"></div>
    <!-- 角落斜向装饰 -->
    <div class="oct-diag tl-d"></div><div class="oct-diag tr-d"></div>
    <div class="oct-diag bl-d"></div><div class="oct-diag br-d"></div>
  </div>

  <!-- ═══ 顶栏 — 融入整体框架 ═══ -->
  <header class="bs-hd">
    <div class="bhd-l">
      <span class="bhd-en">SYSTEM STATUS MONITORING</span>
      <nav class="bhd-nav">
        <button class="bhn-btn" @click="go('/alarm-config')">报警配置</button>
        <button class="bhn-btn" @click="go('/agent')">AI Agent</button>
        <button class="bhn-btn" @click="go('/knowledge-graph')">知识图谱</button>
        <button class="bhn-btn" @click="go('/devices')">设备模型</button>
        <button class="bhn-btn" @click="go('/warn-adjudication')">预警研判</button>
        <button class="bhn-btn" @click="go('/stats')">统计报表</button>
        <button class="bhn-btn" @click="go('/condition')">智能监盘</button>
        <button class="bhn-btn" @click="go('/model-opt')">模型优化</button>
      </nav>
    </div>
    <div class="bhd-c">
      <h1 class="bhd-title">火电运行智能预警监控大屏</h1>
      <div class="bhd-sub">{{ store.selectedUnit.name }} · {{ store.selectedUnit.capacity }}MW · {{ store.selectedUnit.type }}</div>
    </div>
    <div class="bhd-r">
      <span class="bhd-en">REAL-TIME MONITORING</span>
      <div class="bhd-clock"><span class="bhd-clk-val">{{ clock }}</span></div>
      <button class="bhl-btn out" @click="logout">退出</button>
    </div>
  </header>

  <!-- ═══ KPI 参数条 — 无边框融入式设计 ═══ -->
  <div class="bs-kpi-bar">
    <div class="kpi-group params">
      <span class="kpi-g-lbl">关键参数</span>
      <div class="kpi-items">
        <span v-for="(p,i) in rtParams.slice(0,5)" :key="'p'+i" class="kpi-mini">
          <em class="kmi-k">{{ p.k }}</em>
          <strong class="kmi-v" :style="{color:p.c}">{{ p.v }}</strong>
          <small class="kmi-u">{{ p.u }}</small>
        </span>
      </div>
    </div>
    <div class="kpi-sep"></div>
    <div class="kpi-group summary">
      <span v-for="(m,i) in anaSummary" :key="'s'+i" class="kpi-num" :style="{'--ac':m.c}">
        <em>{{ m.k }}</em><strong>{{ m.v }}</strong>
      </span>
    </div>
    <div class="kpi-sep"></div>
    <div class="kpi-group status">
      <span v-for="(s,i) in sysSummary" :key="'st'+i" class="kpi-st" :class="s.cl">
        <b>{{ s.nm }}</b><i :style="{color:s.cv}">{{ s.val }}</i>
      </span>
    </div>
  </div>

  <!-- ═══ Tab 切换 — 极简线条式 ═══ -->
  <nav class="bs-tabs">
    <button class="tab-btn" :class="{active:tab==='monitor'}" @click="tab='monitor'">实时监控中心</button>
    <button class="tab-btn" :class="{active:tab==='analysis'}" @click="tab='analysis'">智能分析平台</button>
    <div class="tab-underline" :style="{left:tab==='monitor'?'0':'50%'}"></div>
  </nav>

  <!-- ══════════════════════════════════════
       TAB 1: 实时监控 — 矩阵+趋势 一体化
       ══════════════════════════════════════ -->
  <section v-show="tab==='monitor'" class="bs-main mon-main">

    <!-- 矩阵区 — 科技边框容器 -->
    <div class="matrix-zone">
      <!-- ═══ 矩阵专属科技边框 — 参考设计重做 ═══ -->
      <div class="mtx-frame">
        <svg class="mf-svg" viewBox="0 0 1000 600" preserveAspectRatio="none">
          <defs>
            <!-- 边框渐变 -->
            <linearGradient id="mfGrad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#3eaaff" stop-opacity="1"/>
              <stop offset="0.5" stop-color="#5fb3ff" stop-opacity="0.8"/>
              <stop offset="1" stop-color="#1d6fb8" stop-opacity="0.6"/>
            </linearGradient>
            <!-- 斜纹图案 -->
            <pattern id="diagStripe" patternUnits="userSpaceOnUse" width="8" height="8" patternTransform="rotate(45)">
              <rect width="8" height="8" fill="rgba(62,170,255,0.08)"/>
              <line x1="0" y1="0" x2="0" y2="8" stroke="rgba(62,170,255,0.5)" stroke-width="2"/>
            </pattern>
            <!-- 整体发光滤镜 -->
            <filter id="mfGlow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="2.5" result="blur"/>
              <feMerge>
                <feMergeNode in="blur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
          </defs>

          <!-- 主框架路径：八边形主体 + 四角延伸 + 上下凹槽（覆盖整个矩阵区） -->
          <path d="
            M 30 14
            L 130 14 L 140 24 L 150 14 L 460 14 L 470 4 L 480 4 L 490 4 L 500 14 L 850 14 L 860 24 L 870 14 L 970 14
            L 996 30 L 996 570
            L 970 586
            L 870 586 L 860 596 L 850 586 L 500 586 L 490 596 L 480 596 L 470 586 L 150 586 L 140 596 L 130 586 L 30 586
            L 4 570 L 4 30
            Z"
            fill="none" stroke="url(#mfGrad)" stroke-width="1.5" filter="url(#mfGlow)"/>

          <!-- 上部斜纹装饰条 -->
          <rect x="460" y="0" width="40" height="14" fill="url(#diagStripe)"/>
          <!-- 下部斜纹装饰条 -->
          <rect x="460" y="586" width="40" height="14" fill="url(#diagStripe)"/>

          <!-- 顶部十字标识 -->
          <g class="mf-cross" transform="translate(480,10)">
            <line x1="-4" y1="0" x2="4" y2="0" stroke="#3eaaff" stroke-width="1"/>
            <line x1="0" y1="-4" x2="0" y2="4" stroke="#3eaaff" stroke-width="1"/>
          </g>
          <!-- 底部十字标识 -->
          <g class="mf-cross" transform="translate(480,590)">
            <line x1="-4" y1="0" x2="4" y2="0" stroke="#3eaaff" stroke-width="1"/>
            <line x1="0" y1="-4" x2="0" y2="4" stroke="#3eaaff" stroke-width="1"/>
          </g>

          <!-- 左右两侧十字标识（移进边框内，避免与图标重叠） -->
          <g class="mf-cross" transform="translate(140,300)">
            <line x1="-3" y1="0" x2="3" y2="0" stroke="rgba(62,170,255,0.5)" stroke-width="0.8"/>
            <line x1="0" y1="-3" x2="0" y2="3" stroke="rgba(62,170,255,0.5)" stroke-width="0.8"/>
          </g>
          <g class="mf-cross" transform="translate(860,300)">
            <line x1="-3" y1="0" x2="3" y2="0" stroke="rgba(62,170,255,0.5)" stroke-width="0.8"/>
            <line x1="0" y1="-3" x2="0" y2="3" stroke="rgba(62,170,255,0.5)" stroke-width="0.8"/>
          </g>

          <!-- 内部装饰边（双层边框效果） -->
          <path d="
            M 36 22
            L 480 22 L 490 12 L 500 22 L 964 22
            L 988 42 L 988 558
            L 964 578
            L 500 578 L 490 588 L 480 578 L 36 578
            L 12 558 L 12 42
            Z"
            fill="none" stroke="rgba(62,170,255,0.25)" stroke-width="0.8"/>
        </svg>

        <!-- 左侧竖向标签 -->
        <div class="mf-label">射频接收子系统</div>
        <!-- 右侧竖向标签 -->
        <div class="mf-label mf-label-r">射频接收子系统</div>
        <!-- 右下角 ONLINE 状态 -->
        <div class="mf-status">
          <span class="mfs-dot ok"></span>ONLINE
        </div>
      </div>

      <!-- 矩阵标题行 -->
      <div class="mtx-title-row">
        <div class="mtx-meta">
          <span>在线 <b>{{ unitDevices.length }}</b> / 槽位 <b>{{ totalSlots }}</b></span>
        </div>
        <h2 class="mtx-tit">设备健康状态监控矩阵</h2>
        <div class="mtx-grades">
          <span class="mg-item mg1">一级<b>{{ cnt(1) }}</b></span>
          <span class="mg-item mg2">二级<b>{{ cnt(2) }}</b></span>
          <span class="mg-item mg3">预警<b>{{ cnt(3) }}</b></span>
        </div>
      </div>

      <!-- 矩阵网格 -->
      <div class="mtx-grid-wrap">
        <div class="mtx-grid" :style="{gridTemplateColumns:'repeat('+MCOLS+',1fr)'}">
          <div v-for="(cell,idx) in mtxCells" :key="idx"
               class="mcell" :class="[cell.cl, cell.empty?'empty':'', cell.edge?'edge-empty':'']"
               @click="!cell.empty && goDevice(cell.dev)"
               :title="cell.dev ? cell.dev.name+' · 健康'+cell.dev.health : '空闲'">
            <template v-if="!cell.empty">
              <div class="mc-hex" :class="'hx-'+cell.dev.dept">
                <span class="hx-txt">{{ hexIcon(cell.dev.dept) }}</span>
              </div>
              <span class="mc-nm">{{ shortNm(cell.dev.name) }}</span>
              <span class="mc-sc" :class="cell.cl">{{ cell.dev.health.toFixed(0) }}</span>
            </template>
            <template v-else>
              <div class="mc-hex hx-emp"><span class="hx-txt">+</span></div>
              <span class="mc-nm emp">待接入</span>
            </template>
          </div>
        </div>
        <!-- 扫描线 -->
        <div class="scan-line"></div>
      </div>
    </div>

    <!-- 趋势图区 — 底部嵌入，无边框隔离感 -->
    <div class="trend-zone">
      <div class="trend-panel">
        <div class="tp-hd">
          <span class="tp-ic"></span>
          <span class="tp-title">设备实时参数趋势</span>
          <span class="tp-live">LIVE</span>
        </div>
        <div ref="rtRef" class="tp-chart"></div>
      </div>
      <div class="trend-panel">
        <div class="tp-hd">
          <span class="tp-ic"></span>
          <span class="tp-title">近24小时告警趋势</span>
        </div>
        <div ref="atRef" class="tp-chart"></div>
      </div>
    </div>
  </section>

  <!-- ══════════════════════════════════════
       TAB 2: 智能分析 — 专业级分析平台
       ══════════════════════════════════════ -->
  <section v-show="tab==='analysis'" class="bs-main ana-main">
    <!-- ══ Row 1: 三栏 ══ -->
    <div class="ana-r1">
      <!-- AI 智能洞察 -->
      <div class="ana-card ac-insights">
        <div class="cd-t">AI 智能洞察</div>
        <div class="ac-body">
          <div v-for="(ins,i) in insights" :key="i" class="ai-item" :class="[ins.type, {'ai-urg': ins.type==='bad'}]">
            <div class="ai-sev" :class="ins.type">{{ ins.sev }}</div>
            <i class="ai-dot" :class="ins.type"></i>
            <div class="ai-txt">
              <b>{{ ins.title }}</b>
              <p>{{ ins.content }}</p>
              <span class="ai-time">{{ ins.time }}</span>
            </div>
            <span v-if="ins.action" class="ai-act" @click="go(ins.action)">{{ ins.actTxt }} ›</span>
          </div>
        </div>
      </div>
      <!-- 告警专业分布 -->
      <div class="ana-card ac-prof">
        <div class="cd-t">告警专业分布</div>
        <div class="ac-body ac-split">
          <!-- 左栏：环形仪表盘 + 分析结论 -->
          <div class="pf-left">
            <div class="pf-wrap">
              <!-- 四角 HUD 刻度短线装饰 -->
              <div class="pf-tl"></div><div class="pf-tr"></div><div class="pf-bl"></div><div class="pf-br"></div>
              <div ref="pfRef" class="pf-c"></div>
              <div class="pf-center">
                <strong>{{ unitAlarms.length }}</strong>
                <span>告警总数</span>
              </div>
            </div>
            <!-- 分析研判结论文本框 -->
            <div class="pf-analysis">
              <div class="pfa-hd"><i class="pfa-ic">▸</i> 智能研判</div>
              <p class="pfa-txt">{{ profAnalysisText }}</p>
              <div class="pfa-tags">
                <span v-for="(t,i) in profTags" :key="i" class="pfa-tag" :style="{borderColor:t.c,color:t.c}">{{ t.label }}</span>
              </div>
            </div>
          </div>
          <!-- 右栏：图例升级为切角参数卡片 -->
          <div class="pf-list">
            <div v-for="(d,i) in profDistFull" :key="i" class="pf-row">
              <i class="pf-ci" :style="{background:d.c}"></i>
              <span class="pf-nm">{{ d.n }}</span>
              <b>{{ d.v }}</b>
              <span class="pf-pct">{{ d.p }}%</span>
              <div class="pf-pg"><div :style="{width:d.p+'%',background:d.c}"></div></div>
            </div>
          </div>
        </div>
      </div>
      <!-- 关键指标卡片区 -->
      <div class="ana-card ac-kpi">
        <div class="cd-t">关键效能指标</div>
        <div class="ac-body kpi-grid">
          <div v-for="(s,i) in slaStats" :key="i" class="sla-item" :class="s.cl">
            <em>{{ s.k }}</em>
            <div class="sla-val"><strong :style="{color:s.c}">{{ s.v }}</strong><small>{{ s.u }}</small></div>
            <div class="sla-pg"><div :style="{width:s.p+'%',background:s.c}"></div></div>
          </div>
          <div class="kpi-divider"></div>
          <div v-for="(r,i) in topRatios" :key="'tr'+i" class="sla-item" :class="r.cl">
            <em>{{ r.k }}</em>
            <div class="sla-val"><strong :style="{color:r.c}">{{ r.v }}</strong><small>{{ r.u||'' }}</small></div>
            <div class="sla-pg"><div :style="{width:(r.p||0)+'%',background:r.c}"></div></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ══ Row 2: 三栏底部 ══ -->
    <div class="ana-r2">
      <!-- 设备风险排名 -->
      <div class="ana-card ac-risk">
        <div class="cd-t">设备风险排名</div>
        <div class="ac-body risk-body">
          <!-- 左侧：风险条形列表 -->
          <div class="risk-left">
            <div v-for="(d,i) in riskDevices" :key="i" class="risk-row" :class="d.hl">
              <span class="risk-rk">{{ i+1 }}</span>
              <span class="risk-nm">{{ d.nm }}</span>
              <span class="risk-dept">{{ d.dept }}</span>
              <div class="risk-bar-wrap">
                <div class="risk-bar" :style="{width:d.hp+'%',background:d.bc}"></div>
              </div>
              <strong class="risk-val" :style="{color:d.bc}">{{ d.hp }}</strong>
            </div>
            <div v-if="!riskDevices.length" class="risk-empty">✓ 全部设备运行正常</div>
          </div>
          <!-- 右侧：健康度分布柱状图 + 分析文字 -->
          <div class="risk-right">
            <div class="risk-ana-txt">{{ riskAnalysisText }}</div>
            <div ref="riskDistRef" class="risk-dist-chart"></div>
            <div class="dist-legend">
              <span class="dl-item"><i style="background:#e07a6b"></i>&lt;60 危险</span>
              <span class="dl-item"><i style="background:#e0a85c"></i>60-75 关注</span>
              <span class="dl-item"><i style="background:#5aa6c4"></i>75-90 正常</span>
            </div>
          </div>
        </div>
      </div>
      <!-- 预测性指标 -->
      <div class="ana-card ac-pred">
        <div class="cd-t">预测性分析</div>
        <div class="ac-body pred-body">
          <!-- 上部：参数铭牌（保持原有4个） -->
          <div class="pred-grid">
            <div v-for="(p,i) in predMetrics" :key="i" class="pred-item" :class="p.dir">
              <div class="pred-hd">
                <em>{{ p.k }}</em>
                <span class="pred-tag" :class="p.tagCl">{{ p.tag }}</span>
              </div>
              <strong class="pred-val" :style="{color:p.c}">{{ p.v }}</strong>
              <div class="pred-bar"><div :style="{width:p.bar+'%',background:p.c}"></div></div>
              <span class="pred-desc">{{ p.desc }}</span>
            </div>
          </div>
          <!-- 下部：健康度趋势曲线 + 风险等级条 -->
          <div class="pred-extra">
            <div ref="predTrendRef" class="pred-trend-chart"></div>
            <div class="pred-risk-level">
              <span class="prl-lbl">综合风险等级</span>
              <div class="prl-bar-wrap">
                <div class="prl-seg" style="background:rgba(224,122,107,0.15)"></div>
                <div class="prl-seg" style="background:rgba(224,168,92,0.12)"></div>
                <div class="prl-seg" style="background:rgba(91,184,138,0.10)"></div>
                <div class="prl-marker" :style="{left: riskLevelPct+'%', borderColor: riskLevelColor}"></div>
              </div>
              <span class="prl-tags">
                <em>危险</em><em>关注</em><em>正常</em>
              </span>
            </div>
          </div>
        </div>
      </div>
      <!-- 综合态势概览 -->
      <div class="ana-card ac-overview">
        <div class="cd-t">综合态势概览</div>
        <div class="ac-body ov-body">
          <!-- 上部：指标铭牌卡片（保持不变） -->
          <div class="ov-grid">
            <div v-for="(r,i) in ratioData" :key="i" class="ov-item" :style="{'--ov-c': r.c, 'borderColor': r.c.replace(')', ',0.25)').replace('rgb', 'rgba')}">
              <em>{{ r.k }}</em>
              <strong :style="{color:r.c}">{{ r.v }}</strong>
              <div class="ov-pg"><div :style="{width:Math.min(r.p,100)+'%',background:r.c}"></div></div>
              <span class="ov-trend" :class="r.trend==='up'?'up':r.trend==='down'?'down':''">{{ r.trendIcon }}{{ r.trendVal }}</span>
            </div>
          </div>
          <!-- 下部：AI 综合研判分析面板 -->
          <div class="ov-ai-zone">
            <div class="oaz-hd"><span class="oaz-ic">◈</span> AI 综合研判</div>
            <div class="oaz-body">
              <div class="oaz-score">
                <span class="oaz-label">机组综合健康评分</span>
                <strong class="oaz-val" :style="{color: aiScore.c}">{{ aiScore.v }}</strong>
                <span class="oaz-grade" :style="{borderColor:aiScore.c, color:aiScore.c}">{{ aiScore.grade }}</span>
              </div>
              <div class="oaz-conclusion">{{ aiConclusion }}</div>
              <div class="oaz-items">
                <div v-for="(t,i) in aiTips" :key="i" class="oaz-tip" :class="t.lv">
                  <span class="oaz-dot"></span>
                  <span class="oaz-txt">{{ t.txt }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick, reactive } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from '@/utils/echarts'
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'

const store = useDataStore()
const userStore = useUserStore()
const router = useRouter()
const clock = ref('')
const tab = ref('monitor')
const rtRef = ref(null), atRef = ref(null), pfRef = ref(null)
const riskDistRef = ref(null), predTrendRef = ref(null)
let rtCh, atCh, pfCh, riskDistCh, predTrendCh, iv

const go = (p) => router.push(p)
const logout = () => { userStore.logout(); router.push('/login') }
const goDevice = (d) => { store.selectedDevice = d.id; router.push({ path:'/devices', query:{deviceId:d.id} }) }

const unitDevices = computed(() => store.unitDevices(store.selectedUnitId))
const unitAlarms = computed(() => store.unitAlarms(store.selectedUnitId))
const cnt = (l) => unitAlarms.value.filter(a => a.l === l).length
const avgHealth = computed(() => {
  const ds = unitDevices.value
  if (!ds.length) return 0
  return (ds.reduce((s,d)=>s+d.health,0)/ds.length).toFixed(1)
})

const grades = computed(() => [
  {l:1,n:'一级',v:cnt(1),c:'#e07a6b'},
  {l:2,n:'二级',v:cnt(2),c:'#e0a85c'},
  {l:3,n:'预警',v:cnt(3),c:'#5aa6c4'}
])

const rtParams = computed(() => [
  {k:'主汽温度',v:(535+Math.random()*12).toFixed(1),u:'°C',c:'#e07a6b'},
  {k:'主汽压力',v:(16.4+Math.random()*0.6).toFixed(2),u:'MPa',c:'#5aa6c4'},
  {k:'机组负荷',v:(480+Math.random()*30).toFixed(0),u:'MW',c:'#5bb88a'},
  {k:'给水流量',v:(1480+Math.random()*80).toFixed(0),u:'t/h',c:'#5aa6c4'},
  {k:'振动',v:(42+Math.random()*18).toFixed(1),u:'μm',c:'#e0a85c'},
  {k:'真空度',v:(94.5+Math.random()*1.5).toFixed(1),u:'kPa',c:'#9b8ab8'}
])
// 实时趋势参数选择状态
const rtSel = reactive({
  '主汽温度': true,
  '主汽压力': false,
  '机组负荷': true,
  '给水流量': false,
  '振动': false,
  '真空度': false
})
// 告警趋势选择状态
const atSel = reactive({ l1: true, l2: true, l3: true })

const MCOLS = 16
const INNER_COLS = 14  // 中间放设备数据
const TOTAL_ROWS = 7   // 14 × 7 = 98 槽位
const mtxCells = computed(() => {
  const devs = unitDevices.value
  const cells = []
  for (let r = 0; r < TOTAL_ROWS; r++) {
    cells.push({dev: null, empty: true, cl: '', edge: true})
    for (let c = 0; c < INNER_COLS; c++) {
      const devIdx = r * INNER_COLS + c
      const dev = devIdx < devs.length ? devs[devIdx] : null
      const h = dev ? dev.health : 0
      cells.push({dev, empty: !dev, cl: h>=90?'ok':h>=80?'warn':h>0?'bad':'', edge: false})
    }
    cells.push({dev: null, empty: true, cl: '', edge: true})
  }
  return cells
})
const totalSlots = computed(() => INNER_COLS * TOTAL_ROWS)

function shortNm(n) {
  if (!n) return '--'
  if (n.length <= 5) return n
  if (n.includes('送风机')) return n.replace('风机','').replace('A','A送').replace('B','B送')
  if (n.includes('引风机')) return n.replace('风机','').replace('A','A引').replace('B','B引')
  if (n.includes('一次风机')) return '一次风'+(n.includes('A')?'A':n.includes('B')?'B':'')
  if (n.includes('磨煤机')) return '磨煤机'+(n.match(/[A-D]/)?.[0]||'')
  if (n.includes('给水泵')) return '给水泵'+(n.match(/[A-B]/)?.[0]||'')
  if (n.includes('高压缸')) return '高压缸'
  if (n.includes('中压缸')) return '中压缸'
  if (n.includes('低压缸')) return '低压缸'
  if (n.includes('发电机')) return '发电机'
  if (n.includes('主变')) return '主变压器'
  return n.length > 5 ? n.slice(0,5) : n
}
function healthTag(h) { return h >= 90 ? '正常' : h >= 80 ? '关注' : '异常' }
const hexIconMap = {'锅炉':'B','汽轮机':'T','电气':'E','热工':'I','辅网':'P'}
function hexIcon(dept) { return hexIconMap[dept] || '?' }

const sysSummary = computed(() => {
  const ds = unitDevices.value, byDept = {}
  ds.forEach(d => { if(!byDept[d.dept]) byDept[d.dept]=[]; byDept[d.dept].push(d) })
  const dm = {'锅炉':{nm:'锅炉'},'汽轮机':{nm:'汽轮机'},'电气':{nm:'电气'},'热工':{nm:'热工'},'辅网':{nm:'辅网'}}
  return Object.entries(dm).map(([k,m]) => {
    const dv = byDept[k]||[], avgH = dv.length?(dv.reduce((s,d)=>s+d.health,0)/dv.length).toFixed(0):'--'
    const bad = dv.filter(d=>d.health<80).length
    const cl = !dv.length?'off':bad>0?'warn':'ok'
    const cv = !dv.length?'#4a6a8a':bad>0?'#e0a85c':'#5bb88a'
    return {...m, nm:m.nm, val:avgH+'%', cv, cl}
  })
})

const insights = computed(() => {
  const o=[], ds=unitDevices.value, ua=unitAlarms.value
  const lh=ds.filter(d=>d.health<80)
  if(lh.length) o.push({type:'bad',sev:'紧急',title:'设备健康告警',content:`${lh.length}台设备健康度低于80，建议立即启动预防性检修。高风险设备：${lh.slice(0,3).map(d=>d.nm).join('、')}。`,time:fmtTime(-2),action:'/devices',actTxt:'查看设备'})
  else o.push({type:'ok',sev:'正常',title:'设备健康良好',content:`全部${ds.length}台设备健康度≥80，运行状态平稳。最优设备健康度${ds.length?Math.max(...ds.map(d=>d.health)):0}。`,time:fmtTime(-5)})
  const l1=ua.filter(a=>a.l===1&&a.st==='unhandled')
  if(l1.length) o.push({type:'bad',sev:'严重',title:'一级未处置告警',content:`${l1.length}条一级报警未处理，最长已超时${l1.length?Math.max(...l1.map(a=>Math.floor((Date.now()-a.t)/60000))):0}分钟，超时将自动升级。`,time:fmtTime(-1),action:'/alarm-config',actTxt:'去处置'})
  else o.push({type:'ok',sev:'良好',title:'一级告警已闭环',content:`所有一级报警均已闭环处理，平均响应时间≤3分钟，无超时风险。`,time:fmtTime(-10)})
  const l2=ua.filter(a=>a.l===2).length
  if(l2>5) o.push({type:'warn',sev:'注意',title:'二级告警集中',content:`近期二级告警达${l2}条，主要集中在${(()=>{const m={};ua.filter(a=>a.l===2).forEach(a=>{m[a.dept]=(m[a.dept]||0)+1});return Object.entries(m).sort((a,b)=>b[1]-a[1])[0]?.[0]||'未知'})()}专业，建议关注关联设备。`,time:fmtTime(-15)})
  const l3=ua.filter(a=>a.l===3)
  if(l3.length>3) o.push({type:'warn',sev:'提示',title:'智能预警活跃',content:`AI模型检测到${l3.length}条潜在异常趋势，涉及振动偏大、温度渐升等早期征兆，建议提前介入。`,time:fmtTime(-8),action:'/condition',actTxt:'查看详情'})
  if(ua.length===0) o.push({type:'ok',sev:'优秀',title:'系统平稳运行',content:`${store.selectedUnit.name}暂无任何级别告警，各参数运行在正常区间，系统状态优良。`,time:fmtTime(-3)})
  else {
    const ohPct = Math.round(ua.filter(a=>a.st!=='unhandled').length/ua.length*100)
    o.push({type:ohPct>=90?'ok':ohPct>=70?'warn':'bad',sev:ohPct>=90?'优良':ohPct>=70?'一般':'待改进',title:'告警处置总览',content:`累计${ua.length}条告警，闭环率${ohPct}%，未处置${ua.filter(a=>a.st==='unhandled').length}条。${ohPct<80?'需加快处置进度。':'处置效率达标。'}`,time:fmtTime(-6)})
  }
  return o
})

function fmtTime(minAgo) {
  const d=new Date(Date.now()+minAgo*60000)
  return d.toLocaleTimeString('zh-CN',{hour12:false}).slice(0,5)
}

const profDistFull = computed(() => {
  const pc={锅炉:0,汽轮机:0,电气:0,热工:0,辅网:0}
  unitAlarms.value.forEach(a=>{pc[a.dept]=(pc[a.dept]||0)+1})
  const t=Math.max(1,Object.values(pc).reduce((s,v)=>s+v,0))
  return [
    {n:'锅炉',v:pc['锅炉']||0,p:((pc['锅炉']||0)/t*100).toFixed(1),c:'#e0a85c'},
    {n:'汽轮机',v:pc['汽轮机']||0,p:((pc['汽轮机']||0)/t*100).toFixed(1),c:'#5aa6c4'},
    {n:'电气',v:pc['电气']||0,p:((pc['电气']||0)/t*100).toFixed(1),c:'#9b8ab8'},
    {n:'热工',v:pc['热工']||0,p:((pc['热工']||0)/t*100).toFixed(1),c:'#5aa6c4'},
    {n:'辅网',v:pc['辅网']||0,p:((pc['辅网']||0)/t*100).toFixed(1),c:'#5bb88a'}
  ]  .filter(d=>d.v>0)
})

// 告警专业分布 — 智能研判分析文本
const profAnalysisText = computed(() => {
  const pc={锅炉:0,汽轮机:0,电气:0,热工:0,辅网:0}
  unitAlarms.value.forEach(a=>{pc[a.dept]=(pc[a.dept]||0)+1})
  const t=Math.max(1,Object.values(pc).reduce((s,v)=>s+v,0))
  const topDept = Object.entries(pc).sort((a,b)=>b[1]-a[1])[0]
  const topPct = topDept ? ((topDept[1]/t)*100).toFixed(1) : '0'
  return `${topDept?.[0]||'锅炉'}告警占比 ${topPct}%，为本阶段风险集中区域，建议优先巡检${topDept?.[0]||'锅炉'}辅机设备，重点关注轴承温度与润滑相关测点。`
})
// 分析标签
const profTags = computed(() => {
  const pc={锅炉:0,汽轮机:0,电气:0,热工:0,辅网:0}
  unitAlarms.value.forEach(a=>{pc[a.dept]=(pc[a.dept]||0)+1})
  const t=Math.max(1,Object.values(pc).reduce((s,v)=>s+v,0))
  return Object.entries(pc)
    .filter(([,v])=>v>0)
    .sort((a,b)=>b[1]-a[1])
    .slice(0,3)
    .map(([n,v])=>{const cm={锅炉:'#e0a85c',汽轮机:'#5aa6c4',电气:'#9b8ab8',辅网:'#5bb88a'};return{label:`${n} ${((v/t)*100).toFixed(0)}%`,c:cm[n]||'#5aa6c4'}})
})

// 设备风险排名 — 分析研判文本
const riskAnalysisText = computed(() => {
  const rd = riskDevices.value
  if(!rd.length) return '当前全部设备运行正常，无风险集中现象。'
  const depts = [...new Set(rd.map(d=>d.dept))]
  return `当前 ${rd.length} 台高风险设备${depts.length>0?`全部属于【${depts.join('、')}】专业`:''}，集中关注轴承温度、润滑相关测点。`
})

// 综合态势概览 — AI 综合研判分析
const aiScore = computed(() => {
  const h = Number(avgHealth.value)
  const s = Math.round(h * 0.85 + (unitAlarms.value.length > 5 ? -5 : 3) + (h > 92 ? 4 : 0))
  return {
    v: Math.min(99, Math.max(60, s)),
    c: s >= 90 ? '#5bb88a' : s >= 78 ? '#e0a85c' : '#e07a6b',
    grade: s >= 90 ? '优良' : s >= 78 ? '正常' : '关注'
  }
})
const aiConclusion = computed(() => {
  const h = Number(avgHealth.value)
  const alarmCount = unitAlarms.value.length
  if (h >= 94 && alarmCount <= 2) return '当前机组运行状态良好，各系统参数稳定在正常区间，建议保持现有运行方式，按计划执行例行巡检。'
  if (h >= 88 && alarmCount <= 6) return '机组整体健康度尚可，锅炉侧存在少量参数偏移，建议重点关注主汽温/再热汽温趋势，适当增加辅机巡检频次。'
  if (h < 88 || alarmCount > 8) return '检测到多组参数偏离正常范围，锅炉与汽轮机侧均存在异常征兆，建议立即安排专项检查，必要时考虑降负荷运行。'
  return '机组处于过渡运行状态，部分测点存在波动，建议持续监控关键参数变化趋势。'
})
const aiTips = computed(() => {
  const tips = []
  const badCount = unitAlarms.value.filter(a => a.level === 1).length
  if (badCount > 0) tips.push({lv:'bad', txt: `发现 ${badCount} 条一级告警，需优先处置`})
  const h = Number(avgHealth.value)
  if (h < 90) tips.push({lv:'warn', txt: `综合健康度 ${h.toFixed(1)}% 较历史均值偏低，建议排查轴承温度测点`})
  tips.push({lv:'info', txt: '锅炉效率近期呈微降趋势，建议检查燃烧优化曲线'})
  tips.push({lv:'ok', txt: '环保排放指标全部达标，脱硫/脱硝/除尘系统运行正常'})
  return tips.slice(0, 4)
})
// 预测性分析 — 趋势数据（模拟7天健康度）
const predTrendData = computed(() => {
  const h = Number(avgHealth.value)
  return Array.from({length:7},(_,i)=>{
    const base = h + (i-3)*(Math.random()*4-2)
    return {day:['一','二','三','四','五','六','日'][i], val: Math.max(60,Math.min(99,base+(Math.random()*6-3))).toFixed(1)}
  })
})
// 风险等级进度条
const riskLevelPct = computed(() => {
  const h = Number(avgHealth.value)
  return Math.max(5, Math.min(95, h))
})
const riskLevelColor = computed(() => {
  const h = Number(avgHealth.value)
  return h < 75 ? '#e07a6b' : h < 88 ? '#e0a85c' : '#5bb88a'
})

const riskDevices = computed(() => {
  return [...unitDevices.value].filter(d=>d.health<90).sort((a,b)=>a.health-b.health).slice(0,8).map(d=>({
    nm:d.nm, dept:d.dept, hp:d.health,
    bc: d.health<60 ? '#e07a6b' : d.health<75 ? '#e0a85c' : '#5aa6c4',
    hl: d.health<60 ? 'critical' : d.health<75 ? 'warn' : ''
  }))
})

const predMetrics = computed(() => {
  const ds=unitDevices.value, ua=unitAlarms.value
  const avgH = Number(avgHealth.value)
  const badCnt = ds.filter(d=>d.health<80).length
  const recentAlarms7d = ua.length // 模拟近7天
  const trend = badCnt > ds.length*0.2 ? 'up' : badCnt < ds.length*0.05 ? 'down' : 'stable'
  return [
    {k:'健康度趋势', v: trend==='up'?'↓ 恶化':trend==='down'?'↑ 改善':'→ 稳定',
     c: trend==='up'?'#e07a6b':trend==='down'?'#5bb88a':'#5aa6c4',
     tag: trend==='up'?'风险':trend==='down'?'优良':'正常', tagCl: trend==='up'?'bad':trend==='down'?'ok':'info',
     bar: trend==='up'?75:trend==='down'?25:50, desc: `基于近30天数据，${badCnt}台设备亚健康`},
    {k:'故障概率', v: (avgH<85?(95-avgH)*1.2:5+(90-avgH)*0.3).toFixed(1)+'%',
     c: avgH<80?'#e07a6b':avgH<88?'#e0a85c':'#5bb88a',
     tag: avgH<80?'高':avgH<88?'中':'低', tagCl: avgH<80?'danger':avgH<88?'warn':'ok',
     bar: avgH<80?80:avgH<88?45:15, desc: '机器学习模型预测未来72h'},
    {k:'维护建议', v: avgH<82?'计划检修':avgH<90?'预防维护':'例行巡检',
     c: avgH<82?'#e07a6b':avgH<90?'#e0a85c':'#5bb88a',
     tag: avgH<82?'优先':avgH<90?'建议':'常规', tagCl: avgH<82?'bad':avgH<90?'warn':'ok',
     bar: avgH<82?85:avgH<90?50:20, desc: `影响${Math.max(1,Math.round(badCnt*1.3))}台设备`},
    {k:'效率评估', v: (92+Math.random()*6).toFixed(1)+'%',
     c: '#5aa6c4', tag:'A级', tagCl:'ok', bar:88, desc: '综合热效率 & 可用率'}
  ]
})

const topRatios = computed(() => {
  const ds=unitDevices.value, ua=unitAlarms.value
  return [
    {k:'异常设备',v:ds.filter(d=>d.health<80).length,c:'#e07a6b',u:'台',p:ds.length?(ds.filter(d=>d.health<80).length/ds.length*100):0,cl:'danger'},
    {k:'健康均分',v:Number(avgHealth.value).toFixed(1),c:Number(avgHealth.value)>=90?'#5bb88a':Number(avgHealth.value)>=80?'#e0a85c':'#e07a6b',u:'',p:Number(avgHealth.value),cl:Number(avgHealth.value)>=90?'ok':Number(avgHealth.value)>=80?'warn':'danger'}
  ]
})

const slaStats = computed(() => {
  const ua=unitAlarms.value, t=Math.max(1,ua.length)
  const l1=ua.filter(a=>a.l===1), l2=ua.filter(a=>a.l===2), l3=ua.filter(a=>a.l===3)
  const oh=ua.filter(a=>a.st!=='unhandled').length
  return [
    {k:'一级1分钟内',v:l1.length?Math.round(l1.filter(a=>a.st!=='unhandled').length/l1.length*100):100,u:'%',p:l1.length?l1.filter(a=>a.st!=='unhandled').length/l1.length*100:100,c:'#e07a6b',cl:'danger'},
    {k:'二级2小时内',v:l2.length?Math.round(l2.filter(a=>a.st!=='unhandled').length/l2.length*100):100,u:'%',p:l2.length?l2.filter(a=>a.st!=='unhandled').length/l2.length*100:100,c:'#e0a85c',cl:'warn'},
    {k:'预警24h',v:l3.length?Math.round(l3.filter(a=>a.st!=='unhandled').length/l3.length*100):100,u:'%',p:l3.length?l3.filter(a=>a.st!=='unhandled').length/l3.length*100:100,c:'#5aa6c4',cl:'info'},
    {k:'闭环率',v:Math.round(oh/t*100),u:'%',p:oh/t*100,c:'#5bb88a',cl:'ok'}
  ]
})

const anaSummary = computed(() => {
  const ua=unitAlarms.value, un=ua.filter(a=>a.st==='unhandled').length, ah=Number(avgHealth.value)
  return [
    {k:'告警总数',v:ua.length,c:'#5aa6c4',s:'本机组'},
    {k:'未处置',v:un,c:'#e07a6b',s:''},
    {k:'异常',v:unitDevices.value.filter(d=>d.health<80).length,c:'#e0a85c',s:''},
    {k:'闭环率',v:ua.length?Math.round(ua.filter(a=>a.st!=='unhandled').length/ua.length*100):100,c:'#5aa6c4',s:'%'},
    {k:'均健康',v:ah,c:ah>=90?'#5bb88a':ah>=80?'#e0a85c':'#e07a6b',s:'%'}
  ]
})

const ratioData = computed(() => {
  const ds=unitDevices.value, ua=unitAlarms.value
  return [
    {k:'异常数',v:ds.filter(d=>d.health<80).length,c:'#e07a6b',p:ds.length?(ds.filter(d=>d.health<80).length/ds.length*100):0, trend:'down', trendVal:'-2', trendIcon:'↓'},
    {k:'一级',v:cnt(1),c:'#e07a6b',p:ua.length?(cnt(1)/ua.length*100):0, trend:cnt(1)>3?'up':'down', trendVal:cnt(1)>3?'+1':'-1', trendIcon:cnt(1)>3?'↑':'↓'},
    {k:'二级',v:cnt(2),c:'#e0a85c',p:ua.length?(cnt(2)/ua.length*100):0, trend:'stable', trendVal:'0', trendIcon:'→'},
    {k:'预警',v:cnt(3),c:'#5aa6c4',p:ua.length?(cnt(3)/ua.length*100):0, trend:'up', trendVal:'+3', trendIcon:'↑'},
    {k:'均健康',v:Number(avgHealth.value).toFixed(1),c:Number(avgHealth.value)>=90?'#5bb88a':Number(avgHealth.value)>=80?'#e0a85c':'#e07a6b',p:Number(avgHealth.value), trend:Number(avgHealth.value)>88?'up':'down', trendVal:'+0.3', trendIcon:'↑'}
  ]
})

// ═══ 图表 ═══
// 样式常量（所有图表共用，避免块级作用域问题）
const bar1Style = { color: '#e07a6b', borderRadius: [4, 4, 0, 0], shadowBlur: 8, shadowColor: 'rgba(224,122,107,0.35)', shadowOffsetY: 2 }
const bar2Style = { color: '#e0a85c', borderRadius: [4, 4, 0, 0], shadowBlur: 8, shadowColor: 'rgba(224,168,92,0.35)', shadowOffsetY: 2 }
const line3Style = { color: '#5aa6c4', width: 2, shadowBlur: 12, shadowColor: 'rgba(90,166,196,0.5)', shadowOffsetY: 2 }
const line3Item = { color: '#5aa6c4', borderColor: '#fff', borderWidth: 2, shadowBlur: 8, shadowColor: 'rgba(90,166,196,0.6)' }
const gd1 = { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(224,122,107,0.35)' }, { offset: 1, color: 'rgba(224,122,107,0.02)' }] }
const gd2 = { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(90,166,196,0.28)' }, { offset: 1, color: 'rgba(90,166,196,0.02)' }] }
const gd3 = { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(91,184,138,0.28)' }, { offset: 1, color: 'rgba(91,184,138,0.02)' }] }
const gd4 = { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(90,166,196,0.22)' }, { offset: 1, color: 'rgba(90,166,196,0.01)' }] }

const initCharts = () => {
  if(rtCh) rtCh.dispose(); if(atCh) atCh.dispose(); if(pfCh) pfCh.dispose()

  // 参数趋势（参考 ConditionAnalysisView：内置 legend + 6 个固定 series）
  if(rtRef.value){
    rtCh=echarts.init(rtRef.value)
    const now=Date.now(), times=Array.from({length:30},(_,i)=>{const d=new Date(now-(29-i)*2000);return d.toLocaleTimeString('zh-CN',{hour12:false}).slice(0,8)})
    const mk = (color, base, amp) => Array.from({length:30}, ()=>+(base+Math.sin(Math.random()*3)*amp+(Math.random()-0.5)*amp*0.3).toFixed(2))
    rtCh.setOption({
      backgroundColor:'transparent',grid:{left:46,right:18,top:36,bottom:28},
      legend:{textStyle:{color:'#9fb6cf',fontSize:11},top:2,right:8,icon:'roundRect',itemWidth:12,itemHeight:4,itemGap:8},
      tooltip:{trigger:'axis',backgroundColor:'rgba(8,20,40,0.92)',borderColor:'rgba(62,170,255,0.3)',textStyle:{color:'#c8e4ff'}},
      xAxis:{type:'category',boundaryGap:false,data:times,axisLabel:{color:'#9fb6cf',fontSize:10,interval:4,fontFamily:'"SF Mono","Consolas",monospace'},axisLine:{lineStyle:{color:'rgba(62,170,255,0.12)'}},axisTick:{show:false},splitLine:{show:true,lineStyle:{color:'rgba(90,166,196,0.1)',type:'dashed'}}},
      yAxis:{type:'value',axisLabel:{color:'#9fb6cf',fontSize:10},splitLine:{show:true,lineStyle:{color:'rgba(90,166,196,0.1)',type:'dashed'}},axisLine:{show:false},axisTick:{show:false}},
      series:[
        {id:'rt_主汽温度',name:'主汽温度',type:'line',smooth:true,symbol:'circle',symbolSize:5,showSymbol:false,data:mk('#e07a6b',535,8),lineStyle:{width:2,color:'#e07a6b',shadowBlur:10,shadowColor:'#e07a6b',shadowOffsetY:4},itemStyle:{color:'#e07a6b',borderColor:'#061224',borderWidth:1,shadowBlur:6,shadowColor:'#e07a6b'},emphasis:{focus:'series'},areaStyle:{opacity:0.7,color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(224,122,107,0.35)'},{offset:1,color:'rgba(224,122,107,0.02)'}]}}},
        {id:'rt_主汽压力',name:'主汽压力',type:'line',smooth:true,symbol:'circle',symbolSize:5,showSymbol:false,data:mk('#5aa6c4',16.4,0.4),lineStyle:{width:2,color:'#5aa6c4',shadowBlur:10,shadowColor:'#5aa6c4',shadowOffsetY:4},itemStyle:{color:'#5aa6c4',borderColor:'#061224',borderWidth:1,shadowBlur:6,shadowColor:'#5aa6c4'},emphasis:{focus:'series'},areaStyle:{opacity:0.7,color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(90,166,196,0.30)'},{offset:1,color:'rgba(90,166,196,0.02)'}]}}},
        {id:'rt_机组负荷',name:'机组负荷',type:'line',smooth:true,symbol:'circle',symbolSize:5,showSymbol:false,data:mk('#5bb88a',480,15),lineStyle:{width:2,color:'#5bb88a',shadowBlur:10,shadowColor:'#5bb88a',shadowOffsetY:4},itemStyle:{color:'#5bb88a',borderColor:'#061224',borderWidth:1,shadowBlur:6,shadowColor:'#5bb88a'},emphasis:{focus:'series'},areaStyle:{opacity:0.7,color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(91,184,138,0.30)'},{offset:1,color:'rgba(91,184,138,0.02)'}]}}},
        {id:'rt_给水流量',name:'给水流量',type:'line',smooth:true,symbol:'circle',symbolSize:5,showSymbol:false,data:mk('#5aa6c4',1480,40),lineStyle:{width:2,color:'#5aa6c4',shadowBlur:10,shadowColor:'#5aa6c4',shadowOffsetY:4},itemStyle:{color:'#5aa6c4',borderColor:'#061224',borderWidth:1,shadowBlur:6,shadowColor:'#5aa6c4'},emphasis:{focus:'series'},areaStyle:{opacity:0.7,color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(90,166,196,0.28)'},{offset:1,color:'rgba(90,166,196,0.02)'}]}}},
        {id:'rt_振动',name:'振动',type:'line',smooth:true,symbol:'circle',symbolSize:5,showSymbol:false,data:mk('#e0a85c',42,8),lineStyle:{width:2,color:'#e0a85c',shadowBlur:10,shadowColor:'#e0a85c',shadowOffsetY:4},itemStyle:{color:'#e0a85c',borderColor:'#061224',borderWidth:1,shadowBlur:6,shadowColor:'#e0a85c'},emphasis:{focus:'series'},areaStyle:{opacity:0.7,color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(224,168,92,0.28)'},{offset:1,color:'rgba(224,168,92,0.02)'}]}}},
        {id:'rt_真空度',name:'真空度',type:'line',smooth:true,symbol:'circle',symbolSize:5,showSymbol:false,data:mk('#9b8ab8',94.5,0.8),lineStyle:{width:2,color:'#9b8ab8',shadowBlur:10,shadowColor:'#9b8ab8',shadowOffsetY:4},itemStyle:{color:'#9b8ab8',borderColor:'#061224',borderWidth:1,shadowBlur:6,shadowColor:'#9b8ab8'},emphasis:{focus:'series'},areaStyle:{opacity:0.7,color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(155,138,184,0.28)'},{offset:1,color:'rgba(155,138,184,0.02)'}]}}}
      ],animationDuration:1200,animationEasing:'cubicOut'
    },true)
  }
  // 告警趋势（参考工况分析：内置 legend + 3 个 series）
  if(atRef.value){
    atCh=echarts.init(atRef.value)
    atCh.setOption({
      backgroundColor:'transparent',grid:{left:46,right:18,top:36,bottom:28},
      legend:{textStyle:{color:'#9fb6cf',fontSize:11},top:2,right:8,icon:'roundRect',itemWidth:12,itemHeight:4,itemGap:10},
      tooltip:{trigger:'axis',backgroundColor:'rgba(8,20,40,0.92)',borderColor:'rgba(62,170,255,0.3)',textStyle:{color:'#c8e4ff'}},
      xAxis:{type:'category',data:Array.from({length:24},(_,i)=>i+':00'),axisLabel:{color:'#9fb6cf',fontSize:10,interval:3},axisLine:{lineStyle:{color:'rgba(62,170,255,0.12)'}},axisTick:{show:false},splitLine:{show:true,lineStyle:{color:'rgba(90,166,196,0.1)',type:'dashed'}}},
      yAxis:{type:'value',axisLabel:{color:'#9fb6cf',fontSize:10},splitLine:{show:true,lineStyle:{color:'rgba(90,166,196,0.1)',type:'dashed'}},axisLine:{show:false},axisTick:{show:false}},
      series:[
        {id:'at_l1',name:'一级',type:'bar',stack:'alarm',data:Array.from({length:24},()=>Math.random()>0.5?Math.floor(Math.random()*5)+1:0),itemStyle:bar1Style,barWidth:'58%'},
        {id:'at_l2',name:'二级',type:'bar',stack:'alarm',data:Array.from({length:24},()=>Math.floor(Math.random()*6)+2),itemStyle:bar2Style,barWidth:'58%'},
        {id:'at_l3',name:'预警',type:'line',smooth:true,symbol:'circle',symbolSize:5,showSymbol:false,data:Array.from({length:24},()=>5+Math.random()*10),lineStyle:line3Style,itemStyle:line3Item,areaStyle:{opacity:0.7,color:gd4}}
      ],animationDuration:1200,animationEasing:'cubicOut'
    },true)
  }
  // 径向栅格刻度环仪表盘（替代平滑饼图）
  if(pfRef.value){
    pfCh=echarts.init(pfRef.value)
    const pc={锅炉:0,汽轮机:0,电气:0,热工:0,辅网:0}
    unitAlarms.value.forEach(a=>{pc[a.dept]=(pc[a.dept]||0)+1})
    const colorMap={锅炉:'#e0a85c',汽轮机:'#5aa6c4',电气:'#9b8ab8',热工:'#5aa6c4',辅网:'#5bb88a'}
    const total = Math.max(1,Object.values(pc).reduce((s,v)=>s+v,0))
    // 按专业占比展开为36根径向刻度条
    const segData = []
    const deptOrder = ['锅炉','汽轮机','电气','热工','辅网']
    deptOrder.forEach(dept => {
      const count = pc[dept] || 0
      const pct = count / total * 100
      const nBars = Math.max(1, Math.round(pct / 100 * 36))
      for (let i = 0; i < nBars; i++) {
        segData.push({ value: Math.round(pct), itemStyle: { color: colorMap[dept] } })
      }
    })
    pfCh.setOption({
      backgroundColor:'transparent',
      polar:{
        center:['50%','52%'],
        radius:['30%','74%']
      },
      angleAxis:{
        type:'category',
        data:Array.from({length:segData.length},()=>''),
        axisLine:{show:false},
        axisTick:{show:false},
        axisLabel:{show:false},
        startAngle:90,
        z:10
      },
      radiusAxis:{
        type:'value',
        min:0,
        max:100,
        axisLine:{show:false},
        axisTick:{show:false},
        axisLabel:{show:false},
        splitLine:{show:false}
      },
      tooltip:{
        trigger:'item',
        formatter:'{b}: {c}%',
        backgroundColor:'rgba(6,14,28,0.96)',
        borderColor:'rgba(62,170,255,0.25)',
        borderWidth:1,
        textStyle:{color:'#c8dae8',fontSize:11}
      },
      series:[{
        type:'bar',
        coordinateSystem:'polar',
        data:segData,
        barWidth:'78%',
        roundCap:true,
        itemStyle:{
          borderRadius:3,
          shadowBlur:8,
          shadowColor:'rgba(0,0,0,0.4)',
          shadowOffsetY:1
        }
      }],
      animationDuration:1200,
      animationEasing:'cubicOut'
    },true)
  }
  // 设备风险排名 — 健康度区间分布纵向柱状图
  if(riskDistRef.value){
    riskDistCh=echarts.init(riskDistRef.value)
    const ds=unitDevices.value
    const bands={danger:0, warn:0, normal:0, good:0}
    ds.forEach(d=>{if(d.health<60)bands.danger++;else if(d.health<75)bands.warn++;else if(d.health<90)bands.normal++;else bands.good++})
    riskDistCh.setOption({
      backgroundColor:'transparent',
      grid:{left:8,right:8,top:10,bottom:20},
      xAxis:{type:'category',data:['危险','关注','正常','优良'],axisLabel:{color:'#7a98b8',fontSize:9,fontFamily:'"SF Mono","Consolas",monospace'},axisLine:{lineStyle:{color:'rgba(62,170,255,0.12)'}},axisTick:{show:false},splitLine:{show:false}},
      yAxis:{type:'value',show:false},
      series:[{
        type:'bar', data:[bands.danger,bands.warn,bands.normal,bands.good],
        barWidth:'50%', itemStyle:{
          borderRadius:[4,4,0,0],
          color:(params)=>[['#e07a6b','#e0a85c','#5aa6c4','#5bb88a'][params.dataIndex]],
          shadowBlur:6, shadowColor:'rgba(0,0,0,0.3)'
        }
      }],
      animationDuration:800, animationEasing:'cubicOut'
    },true)
  }
  // 预测性分析 — 健康度趋势霓虹曲线（7天）
  if(predTrendRef.value){
    predTrendCh=echarts.init(predTrendRef.value)
    const td=predTrendData.value
    predTrendCh.setOption({
      backgroundColor:'transparent',
      grid:{left:30,right:12,top:12,bottom:22},
      tooltip:{trigger:'axis',backgroundColor:'rgba(8,20,40,0.92)',borderColor:'rgba(62,170,255,0.25)',textStyle:{color:'#c8dae8',fontSize:10},formatter:'{b}: {c}%'},
      xAxis:{type:'category',data:td.map(d=>d.day),axisLabel:{color:'#7a98b8',fontSize:9,fontFamily:'"SF Mono","Consolas",monospace'},axisLine:{lineStyle:{color:'rgba(62,170,255,0.12)'}},axisTick:{show:false},splitLine:{show:true,lineStyle:{color:'rgba(90,166,196,0.08)',type:'dashed'}}},
      yAxis:{type:'value',min:50,max:100,axisLabel:{color:'#7a98b8',fontSize:9,formatter:'{value}%'},splitLine:{show:true,lineStyle:{color:'rgba(90,166,196,0.08)',type:'dashed'}},axisLine:{show:false}},
      series:[{
        type:'line', smooth:true, data:td.map(d=>+d.val), symbol:'circle', symbolSize:5, showSymbol:true,
        lineStyle:{width:2.5, color:'#5bb88a', shadowBlur:10, shadowColor:'rgba(91,184,138,0.5)'},
        itemStyle:{color:'#5bb88a', borderColor:'#061224', borderWidth:1.5, shadowBlur:5, shadowColor:'rgba(91,184,138,0.4)'},
        areaStyle:{opacity:0.5, color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(91,184,138,0.3)'},{offset:1,color:'rgba(91,184,138,0.02)'}]}}
      }],
      animationDuration:1200, animationEasing:'cubicOut'
    },true)
  }
}

watch(()=>store.selectedUnitId,()=>nextTick(initCharts))
watch(()=>tab.value,()=>nextTick(initCharts))
onMounted(()=>{
  clock.value=new Date().toLocaleString('zh-CN',{hour12:false})
  iv=setInterval(()=>{clock.value=new Date().toLocaleString('zh-CN',{hour12:false})},1000)
  nextTick(()=>{
    initCharts()
    // 延迟 50ms resize 一次确保尺寸正确
    setTimeout(()=>{rtCh?.resize();atCh?.resize();pfCh?.resize();riskDistCh?.resize();predTrendCh?.resize()},50)
  })
  window.addEventListener('resize',handleRz)
})
onUnmounted(()=>{clearInterval(iv);rtCh?.dispose();atCh?.dispose();pfCh?.dispose();riskDistCh?.dispose();predTrendCh?.dispose();window.removeEventListener('resize',handleRz)})
const handleRz=()=>{rtCh?.resize();atCh?.resize();pfCh?.resize();riskDistCh?.resize();predTrendCh?.resize()}
const onBg=()=>{}
</script>

<style scoped>
/* ================================================================
   火电智能预警监控大屏 v6 — 参考科学计算子系统八边形科技框架
   核心改造：
   1. 外层八边形科技框架（L型角装饰+边缘发光线+斜向装饰）
   2. 去掉所有独立方框卡片感，内容融入整体框架
   3. KPI 条无边框融入式设计
   4. 矩阵区域直接嵌入主体，无独立容器边框
   5. 趋势图底部一体化排列
   ================================================================ */

/* ═══ 根容器 — 纯深黑底 + 青蓝虚线方格栅格（CRT示波器） ═══ */
.bs {
  height: 100vh;
  padding: 10px 14px;
  /* 纯深黑底色，无蓝无青 */
  background-color: #000000;
  position: relative;
  color: #c0d4e8;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
/* 青蓝色虚线方格栅格——纤细、低对比度、CRT示波器屏幕效果 */
.bs::before {
  content: '';
  position: absolute; inset: 0; pointer-events: none; z-index: 0;
  background-image:
    repeating-linear-gradient(0deg,
      transparent 0px, transparent 19px,
      rgba(90,166,196,0.06) 19px, rgba(90,166,196,0.06) 20px),
    repeating-linear-gradient(90deg,
      transparent 0px, transparent 19px,
      rgba(90,166,196,0.06) 20px, rgba(90,166,196,0.06) 21px);
  background-size: 32px 32px;
}

/* ══════════════════════════════════════════════════════════════
   八边形科技外框架 — 核心！参考"科学计算子系统"
   ══════════════════════════════════════════════════════════════ */
.bs-oct-frame {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

/* L型角装饰块 — 参考设计的四角发光L形 */
.oct-cr {
  position: absolute;
  width: 32px;
  height: 32px;
  z-index: 2;
}
.oct-cr-in {
  display: block;
  width: 100%;
  height: 100%;
  /* L型边框：相邻两边有边框，形成L形状 */
  border: 2px solid transparent;
  box-shadow: 0 0 12px rgba(62,170,255,0.5), inset 0 0 8px rgba(62,170,255,0.12);
}
.oct-cr.tl { top: 6px; left: 6px; }
.oct-cr.tl .oct-cr-in { border-top-color: #3eaaff; border-left-color: #3eaaff; border-top-left-radius: 2px; }
.oct-cr.tr { top: 6px; right: 6px; }
.oct-cr.tr .oct-cr-in { border-top-color: #3eaaff; border-right-color: #3eaaff; border-top-right-radius: 2px; }
.oct-cr.bl { bottom: 6px; left: 6px; }
.oct-cr.bl .oct-cr-in { border-bottom-color: #3eaaff; border-left-color: #3eaaff; border-bottom-left-radius: 2px; }
.oct-cr.br { bottom: 6px; right: 6px; }
.oct-cr.br .oct-cr-in { border-bottom-color: #3eaaff; border-right-color: #3eaaff; border-bottom-right-radius: 2px; }

/* 边缘装饰线 */
.oct-edge {
  position: absolute;
  background: #3eaaff;
  opacity: 0.35;
}
.oct-edge.t { top: 0; left: 44px; right: 44px; height: 1px; box-shadow: 0 0 8px rgba(62,170,255,0.4); opacity: 0.5; }
.oct-edge.b { bottom: 0; left: 44px; right: 44px; height: 1px; }
.oct-edge.l { left: 0; top: 44px; bottom: 44px; width: 1px; box-shadow: 0 0 8px rgba(62,170,255,0.4); opacity: 0.5; }
.oct-edge.r { right: 0; top: 44px; bottom: 44px; width: 1px; }

/* 斜向装饰短线 — 角落对角线方向 */
.oct-diag {
  position: absolute;
  width: 28px;
  height: 2px;
  background: linear-gradient(90deg, #3eaaff, transparent);
  opacity: 0.4;
}
.oct-diag.tl-d { top: 10px; left: 36px; transform: rotate(-20deg); }
.oct-diag.tr-d { top: 10px; right: 36px; transform: rotate(20deg); }
.oct-diag.bl-d { bottom: 10px; left: 36px; transform: rotate(20deg); }
.oct-diag.br-d { bottom: 10px; right: 36px; transform: rotate(-20deg); }

/* ═══ 顶栏 — 融入框架，无独立背景 ═══ */
.bs-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px 10px;
  position: relative;
  z-index: 5;
  gap: 16px;
  flex-shrink: 0;
}
.bhd-l { display: flex; align-items: center; gap: 6px; flex-shrink: 0; max-width: 42%; }
.bhd-en {
  font-size: 9px; color: #5a8ab8; letter-spacing: 2px; text-transform: uppercase; font-weight: 600;
  white-space: nowrap; padding-right: 8px;
  border-right: 1px solid rgba(62,170,255,0.18);
}
.bhd-nav { display: flex; gap: 2px; }
.bhn-btn {
  padding: 2px 7px; font-size: 9px; font-weight: 500;
  border: 1px solid rgba(62,170,255,0.12);
  background: transparent;
  color: #7aa0c0; cursor: pointer;
  transition: all 0.2s;
  border-radius: 2px;
  white-space: nowrap;
}
.bhn-btn:hover { border-color: rgba(62,170,255,0.45); color: #c8e4ff; background: rgba(20,50,90,0.3); }
.bhd-c { text-align: center; position: absolute; left: 50%; top: 6px; transform: translateX(-50%); z-index: 6; min-width: 0; white-space: nowrap; }
.bhd-title {
  font-size: 20px; font-weight: 800; letter-spacing: 4px;
  color: #e8f4ff; text-shadow: 0 0 20px rgba(62,170,255,0.3), 0 2px 4px rgba(0,0,0,0.5);
  margin: 0; white-space: nowrap;
}
.bhd-sub { font-size: 11px; color: #7aa0c0; margin-top: 2px; letter-spacing: 1px; }
.bhd-r { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.bhd-clock { text-align: right; }
.bhd-clk-val { font-size: 13px; color: #a8d4ff; font-family: "SF Mono","Consolas",monospace; text-shadow: 0 0 8px rgba(62,170,255,0.3); }
.bhl-btn {
  padding: 4px 12px; font-size: 10px; font-weight: 500;
  border: 1px solid rgba(62,170,255,0.18);
  background: transparent;
  color: #8ab4d4; cursor: pointer;
  transition: all 0.25s; border-radius: 2px;
}
.bhl-btn:hover { border-color: rgba(62,170,255,0.4); color: #a8d4ff; }
.bhl-btn.out:hover { border-color: rgba(248,113,113,0.35); color: #f87171; }

/* ═══ KPI 参数条 — 无边框融入式 ═══ */
.bs-kpi-bar {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 8px 18px 6px;
  position: relative;
  z-index: 5;
  flex-shrink: 0;
}
.kpi-group { display: flex; align-items: center; gap: 8px; }
.kpi-g-lbl {
  font-size: 10px; font-weight: 700; color: #5a9ac8;
  letter-spacing: 1.5px; padding-right: 10px;
  border-right: 1px solid rgba(62,170,255,0.12);
  margin-right: 4px;
}
.kpi-items { display: flex; gap: 10px; }
.kpi-mini {
  display: flex; align-items: baseline; gap: 2px;
  font-family: "SF Mono","Consolas",monospace;
}
.kmi-k { font-size: 9px; color: #7a9cc0; }
.kmi-v { font-size: 13px; font-weight: 700; }
.kmi-u { font-size: 9px; color: #5a7a98; margin-left: 1px; }

.kpi-sep {
  width: 1px; height: 24px;
  background: linear-gradient(180deg, transparent, rgba(62,170,255,0.2), transparent);
  flex-shrink: 0;
}
.kpi-summary { gap: 8px; }
.kpi-num {
  display: flex; flex-direction: column; align-items: center; gap: 0;
  padding: 3px 10px;
}
.kpi-num em { font-size: 9px; color: #7a9cc0; font-style: normal; }
.kpi-num strong { font-size: 18px; font-weight: 800; font-family: "SF Mono","Consolas",monospace; color: var(--ac, #5fb3ff); }

.kpi-status { gap: 6px; }
.kpi-st {
  display: flex; flex-direction: column; align-items: center; gap: 0;
  padding: 3px 8px;
  border-radius: 3px;
  background: rgba(8,20,40,0.35);
}
.kpi-st b { font-size: 9px; color: #8fb0cf; font-weight: 600; }
.kpi-st i { font-size: 13px; font-weight: 800; font-family: "SF Mono","Consolas",monospace; font-style: normal; }
.kpi-st.ok { border-left: 2px solid #34d399; }
.kpi-st.warn { border-left: 2px solid #fbbf24; }
.kpi-st.off { border-left: 2px solid #4a6a8a; opacity: 0.6; }

/* ═══ Tab 导航 — 极简线条 ═══ */
.bs-tabs {
  display: flex; position: relative;
  padding: 6px 18px 0;
  border-bottom: 1px solid rgba(62,170,255,0.08);
  flex-shrink: 0;
  z-index: 5;
}
.tab-btn {
  padding: 8px 26px; font-size: 12.5px; font-weight: 600;
  color: #5a8aaa; background: none; border: none;
  cursor: pointer; transition: all 0.3s;
  letter-spacing: 1.5px; z-index: 2;
}
.tab-btn.active { color: #e0f0ff; }
.tab-underline {
  position: absolute; bottom: -1px; left: 0;
  width: 50%; height: 2px;
  background: #3eaaff;
  box-shadow: 0 0 10px rgba(62,170,255,0.5);
  transition: left 0.3s ease; z-index: 1;
}

/* ═══ 主体区域 ═══ */
.bs-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  gap: 8px;
  padding: 0 4px;
  position: relative;
  z-index: 3;
}

/* ═══ 矩阵区 — 科技边框容器（纯黑底+虚线网格继承自.bs） ═══ */
.matrix-zone {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  margin-top: 10px;
  position: relative;
  /* 纯深黑底色，让虚线网格从根容器透上来 */
  background-color: #000000;
  clip-path: polygon(
    0 0,
    calc(100% - 24px) 0, 100% 24px,
    100% calc(100% - 24px), calc(100% - 24px) 100%,
    24px 100%, 0 calc(100% - 24px)
  );
  box-shadow:
    0 0 30px rgba(62,170,255,0.08),
    0 0 60px rgba(62,170,255,0.04),
    inset 0 0 40px rgba(62,170,255,0.03);
}

/* ═══ 矩阵专属科技边框 — 参考设计（SVG 多边形 + 斜纹） ═══ */
.mtx-frame {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 2;
}

/* SVG 容器 — 铺满整个矩阵区 */
.mf-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
}

/* SVG 路径动画 — 流动发光 */
.mf-svg path:first-of-type {
  animation: mfBorderPulse 3s ease-in-out infinite;
}
@keyframes mfBorderPulse {
  0%, 100% { stroke-opacity: 1; filter: drop-shadow(0 0 3px rgba(62,170,255,0.6)); }
  50% { stroke-opacity: 0.85; filter: drop-shadow(0 0 8px rgba(62,170,255,0.9)); }
}

/* + 十字标识 — 微发光 */
.mf-cross line {
  filter: drop-shadow(0 0 2px rgba(62,170,255,0.7));
}

/* 左侧竖向标签 — 中文（贴边） */
.mf-label {
  position: absolute;
  left: 2px;
  top: 50%;
  transform: translateY(-50%) rotate(-90deg);
  transform-origin: center center;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 3px;
  color: rgba(62,170,255,0.6);
  white-space: nowrap;
  z-index: 3;
  text-shadow: 0 0 6px rgba(62,170,255,0.4);
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
  pointer-events: none;
}
.mf-label-r {
  left: auto;
  right: 2px;
  transform: translateY(-50%) rotate(90deg);
}

/* 右下角 ONLINE 状态 */
.mf-status {
  position: absolute;
  right: 60px;
  bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 3px;
  color: rgba(52,211,153,0.8);
  z-index: 3;
  text-shadow: 0 0 8px rgba(52,211,153,0.5);
}
.mfs-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  animation: mfsBlink 2s infinite;
}
.mfs-dot.ok {
  background: #34d399;
  box-shadow: 0 0 10px rgba(52,211,153,0.9), 0 0 20px rgba(52,211,153,0.45);
}
@keyframes mfsBlink { 0%,100%{opacity:1} 50%{opacity:.3} }

.mtx-title-row {
  display: flex; align-items: center; justify-content: space-between;
  gap: 14px;
  padding: 12px 70px 8px;
  flex-shrink: 0;
  min-height: 42px;
  border-bottom: 1px solid rgba(62,170,255,0.1);
  position: relative;
  z-index: 4;
}
.mtx-tit {
  font-size: 16px; font-weight: 700; color: #e0f0ff;
  letter-spacing: 2px; margin: 0;
  text-align: center;
  text-shadow: 0 0 10px rgba(62,170,255,0.25);
  flex: 1;
}
.mtx-meta { font-size: 10px; color: #7aa0c0; white-space: nowrap; flex: 0 0 210px; }
.mtx-meta b { color: #3eaaff; font-family: "SF Mono","Consolas",monospace; font-weight: 700; }

.mtx-grades { display: flex; gap: 4px; flex-shrink: 0; flex: 0 0 210px; justify-content: flex-end; }
.mg-item {
  display: flex; align-items: center; gap: 3px;
  padding: 1px 6px;
  border-radius: 2px;
  font-size: 9px; color: #8fb0cf;
  border-left: 2px solid;
}
.mg-item.mg1 { border-color: #f87171; background: rgba(248,113,113,0.05); }
.mg-item.mg2 { border-color: #fbbf24; background: rgba(251,191,36,0.05); }
.mg-item.mg3 { border-color: #22d3ee; background: rgba(34,211,238,0.05); }
.mg-item b { font-size: 11px; font-weight: 800; font-family: "SF Mono","Consolas",monospace; }
.leg-d.ok { background: #34d399; box-shadow: 0 0 5px rgba(52,211,153,0.5); }
.leg-d.w { background: #fbbf24; box-shadow: 0 0 5px rgba(251,191,36,0.5); }
.leg-d.b { background: #f87171; box-shadow: 0 0 5px rgba(248,113,113,0.5); }

/* 矩阵网格容器 */
.mtx-grid-wrap {
  flex: 1;
  position: relative;
  padding: 12px 30px;
  overflow: hidden;
  min-height: 0;
  z-index: 1;
  display: flex;
  align-items: flex-start;
}
.scan-line {
  position: absolute; top: 0; left: 0; right: 0; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(62,170,255,0.2), transparent);
  animation: scanMove 4s linear infinite;
  pointer-events: none; z-index: 3;
}
@keyframes scanMove { 0%{top:0} 100%{top:100%} }

.mtx-grid {
  display: grid;
  gap: 4px;
  width: 100%;
  height: auto;
  align-content: start;
  padding-top: 8px;
}

/* ═══ 矩阵格子 — 14列更宽 ═══ */
.mcell {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 0;
  min-width: 0;
  width: 100%;
  height: 52px;
  padding: 3px 2px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  background: linear-gradient(180deg, rgba(8,20,40,0.4), rgba(6,14,28,0.45));
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  border: none;
  overflow: hidden;
}
.mcell::before {
  content: '';
  position: absolute; inset: 0;
  border: 1px solid rgba(62,170,255,0.12);
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  pointer-events: none;
  transition: border-color 0.2s;
}
.mcell:not(.empty):hover {
  transform: scale(1.08); z-index: 10;
}
.mcell:not(.empty):hover::before {
  border-color: #3eaaff;
  box-shadow: 0 0 16px rgba(62,170,255,0.3);
}
.mcell.ok::before { border-color: rgba(52,211,153,0.22); }
.mcell.warn::before { border-color: rgba(251,191,36,0.22); }
.mcell.bad::before { border-color: rgba(248,113,113,0.28); animation: badPl 2s infinite; }
.mcell.empty { opacity: 0.55; cursor: default; }
.mcell.edge-empty { visibility: hidden; pointer-events: none; }
@keyframes badPl { 0%,100%{box-shadow:none} 50%{box-shadow:inset 0 0 10px rgba(248,113,113,0.12)} }

/* 部门标识 — 微型六边形霓虹铭牌 */
.mc-hex {
  width: 22px; height: 18px;
  position: relative;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 1px; flex-shrink: 0;
}
/* 外层：深色半透明底板 + 双层纤细霓虹边框 */
.mc-hex::before {
  content: '';
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(10,20,40,0.7), rgba(6,14,28,0.75));
  /* 六边形 */
  clip-path: polygon(50% 0, 100% 25%, 100% 75%, 50% 100%, 0 75%, 0 25%);
  border: none;
  box-shadow:
    inset 0 0 5px var(--hx-gl, rgba(62,170,255,0.25)),
    0 0 0 0.5px var(--hx-f, rgba(62,170,255,0.35));
}
/* 内层：第二层细线 */
.mc-hex::after {
  content: '';
  position: absolute; inset: 1.5px;
  clip-path: polygon(50% 0, 100% 25%, 100% 75%, 50% 100%, 0 75%, 0 25%);
  border: 0.5px solid var(--hx-f, rgba(62,170,255,0.18));
  background: transparent;
}
.hx-txt {
  position: relative; z-index: 1;
  font-size: 9px; font-weight: 800;
  font-family: "SF Mono","Consolas",monospace;
  color: var(--hx-txt, #a8d4ff);
  line-height: 1;
  text-shadow: 0 0 4px var(--hx-gl, rgba(62,170,255,0.45));
  letter-spacing: 0.3px;
}
.hx-锅炉 { --hx-f:#f97316; --hx-t:#c2410c; --hx-gl:rgba(249,115,22,0.5); --hx-txt:#fdba74; }
.hx-汽轮机 { --hx-f:#3b82f6; --hx-t:#1d4ed8; --hx-gl:rgba(59,130,246,0.5); --hx-txt:#93c5fd; }
.hx-电气 { --hx-f:#a78bfa; --hx-t:#7c3aed; --hx-gl:rgba(167,139,250,0.5); --hx-txt:#c4b5fd; }
.hx-热工 { --hx-f:#2dd4bf; --hx-t:#0d9488; --hx-gl:rgba(45,212,191,0.5); --hx-txt:#99f6e4; }
.hx-辅网 { --hx-f:#38bdf8; --hx-t:#0284c7; --hx-gl:rgba(56,189,248,0.5); --hx-txt:#7dd3fc; }
.hx-emp { --hx-f:#3a4a5e; --hx-t:#1e293b; --hx-gl:transparent; --hx-txt:#6a7e96; }
.hx-emp::before { opacity: 0.6; box-shadow: none; }

/* 格子文字 */
.mc-nm {
  font-size: 10px; font-weight: 600; color: #c0d8f0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  max-width: 100%; text-align: center; line-height: 1.1;
}
.mc-nm.emp { color: #5a7088; font-size: 9px; }
.mc-sc {
  font-size: 12px; font-weight: 800;
  font-family: "SF Mono","Consolas",monospace;
  line-height: 1;
}
.mc-sc.ok { color: #3af4a0; text-shadow: 0 0 4px rgba(52,211,153,0.4); }
.mc-sc.warn { color: #ffd042; text-shadow: 0 0 4px rgba(251,191,36,0.4); }
.mc-sc.bad { color: #ff6464; text-shadow: 0 0 4px rgba(248,113,113,0.5); }

/* ═══ 趋势图区 — 底部固定高度 ═══ */
.trend-zone {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  flex-shrink: 0;
  height: 260px;
  min-height: 0;
}
.trend-panel {
  display: flex; flex-direction: column;
  /* 纯深黑底色，与矩阵区统一 */
  background-color: #000000;
  overflow: hidden;
  position: relative;
  /* 微妙的上边框线，不用完整边框 */
  border-top: 1px solid rgba(62,170,255,0.12);
  /* 切角科技感 */
  clip-path: polygon(
    0 0,
    calc(100% - 12px) 0, 100% 12px,
    100% calc(100% - 8px), calc(100% - 8px) 100%,
    8px 100%, 0 calc(100% - 8px)
  );
}
.tp-hd {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 12px;
  font-size: 11px; font-weight: 600; color: #d4ecff;
  flex-shrink: 0; letter-spacing: 0.5px;
}
.tp-title { flex: 1; }
.tp-ic {
  width: 0; height: 0;
  border-left: 5px solid #3eaaff;
  border-top: 3px solid transparent;
  border-bottom: 3px solid transparent;
  filter: drop-shadow(0 0 3px rgba(62,170,255,0.6));
}
.tp-live {
  font-size: 8px; font-weight: 700; color: #f87171;
  background: rgba(248,113,113,0.12);
  padding: 1px 5px; border-radius: 2px;
  margin-left: auto; animation: lvB 2s infinite; letter-spacing: 1px;
}
@keyframes lvB { 0%,100%{opacity:1} 50%{opacity:.3} }
.tp-leg {
  display: flex; gap: 8px; font-size: 10px; color: #7aa0c0;
  padding: 2px 12px;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.tp-leg i { display: inline-block; width: 10px; height: 3px; border-radius: 2px; vertical-align: middle; }
.tp-cb {
  display: inline-flex; align-items: center; gap: 4px;
  cursor: pointer; padding: 2px 8px; border-radius: 2px;
  background: transparent; border: 1px solid rgba(62,170,255,0.1);
  transition: all 0.15s;
  user-select: none;
}
.tp-cb i { box-shadow: 0 0 4px currentColor; opacity: 0.4; transition: opacity 0.15s; }
.tp-cb:hover { background: rgba(62,170,255,0.08); border-color: rgba(62,170,255,0.25); }
.tp-cb.on { background: rgba(62,170,255,0.12); border-color: rgba(62,170,255,0.4); }
.tp-cb.on i { opacity: 1; }
.tp-chart { flex: 1; min-height: 0; width: 100%; }

/* ═══ 分析页 — 专业级智能分析平台（增强氛围）══ */
.ana-main {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  overflow: hidden;
  padding: 8px;
  /* 微弱径向渐变中心聚光，减少边缘空洞感 */
  background: radial-gradient(ellipse at center, rgba(10,24,48,0.15) 0%, transparent 70%);
}
/* Row 1: 三栏等高 */
.ana-r1 {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr;
  gap: 8px;
  flex: 1 1 auto;
  min-height: 0;
}
/* Row 2: 三栏底部 */
.ana-r2 {
  display: grid;
  grid-template-columns: 1.1fr 1fr 1.1fr;
  gap: 8px;
  flex: 1 1 auto;
  min-height: 0;
}

/* ══ 通用卡片 — 加厚底色+增强边框+内发光 ══ */
.ana-card {
  display: flex; flex-direction: column;
  min-height: 0; overflow: hidden;
  background: linear-gradient(180deg, rgba(10,24,44,0.80), rgba(6,16,30,0.70));
  border: 1px solid rgba(62,170,255,0.18);
  border-radius: 8px;
  position: relative;
  padding: 16px;
  box-shadow:
    0 4px 24px rgba(0,10,30,0.4),
    inset 0 1px 0 rgba(62,170,255,0.06),
    inset 0 0 20px rgba(62,170,255,0.02);
}
/* 卡片四角 L 型装饰 — 增强亮度 */
.ana-card::before, .ana-card::after {
  content:''; position:absolute; width:16px; height:16px;
  border:1.5px solid #3eaaff; pointer-events:none;
  box-shadow:0 0 8px rgba(62,170,255,0.5), inset 0 0 3px rgba(62,170,255,0.15); opacity:0.6;
}
.ana-card::before { top:-1px; left:-1px; border-right:none; border-bottom:none; }
.ana-card::after { bottom:-1px; right:-1px; border-left:none; border-top:none; }
/* 四角微型 HUD 刻度短线（额外装饰 — 用 box-shadow 模拟） */
.ana-card {
  /* 右上角刻度 */
  --cr-tl: radial-gradient(circle at 0 0, transparent 5px, #3eaaff 5px, #3eaaff 6px, transparent 7px);
}

/* 模块标题栏直接复用全局 .cd-t 模板（测点参数同款），此处不再自定义 */

.ac-body { flex: 1; overflow-y: auto; padding: 8px 12px; min-height: 0;
  /* 微弱内部示波器网格填充空白 */
  background-image:
    repeating-linear-gradient(0deg, transparent 0px, transparent 29px, rgba(62,170,255,0.02) 29px, rgba(62,170,255,0.02) 30px),
    repeating-linear-gradient(90deg, transparent 0px, transparent 29px, rgba(62,170,255,0.02) 30px, rgba(62,170,255,0.02) 31px);
  background-size: 40px 40px;
}
.ac-body::-webkit-scrollbar { width: 2.5px; }
.ac-body::-webkit-scrollbar-thumb { background: rgba(62,170,255,0.2); border-radius: 2px; }

/* ══ AI 智能洞察 — 独立切角深色卡片 + 完整霓虹灯带 + 分隔线 ══ */
.ac-insights .ac-body { display:flex; flex-direction:column; gap:0; padding:10px 12px; }
.ai-item {
  display: flex; gap: 10px; padding: 11px 13px;
  /* 微切角卡片 — 加厚底色 */
  clip-path: polygon(6px 0, calc(100% - 6px) 0, 100% 6px, 100% calc(100% - 6px), calc(100% - 6px) 100%, 6px 100%, 0 calc(100% - 6px), 0 6px);
  transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
  background: linear-gradient(135deg, rgba(10,22,42,0.78) 0%, rgba(6,16,30,0.58) 100%);
  border: 1px solid rgba(62,170,255,0.10);
  border-bottom: 1px solid rgba(62,170,255,0.05);
  flex-shrink: 0;
  position: relative;
  align-items: flex-start;
}
/* 条目间细微分隔线（除最后一项） */
.ai-item:not(:last-child)::after {
  content:''; position:absolute; left:18px; right:12px; bottom:-1px;
  height:1px;
  background:linear-gradient(90deg, transparent, rgba(62,170,255,0.08), transparent);
}
/* 左侧霓虹发光灯带 — 加粗加亮（5px） */
.ai-item::before {
  content:''; width:5px; height:auto; align-self:stretch; flex-shrink:0;
  margin: 5px 0; border-radius:2px;
  transition: box-shadow 0.3s, background 0.3s;
}
.ai-item:hover { transform: translateX(3px); border-color: rgba(62,170,255,0.20); background: linear-gradient(135deg, rgba(12,26,48,0.85) 0%, rgba(8,18,34,0.65) 100%); }
/* bad — 暗红系，最醒目 */
.ai-item.bad {
  background: linear-gradient(135deg, rgba(224,122,107,0.09) 0%, rgba(8,20,40,0.55) 100%);
  border-color: rgba(224,122,107,0.16);
}
.ai-item.bad::before { background:linear-gradient(180deg, #e07a6b, #c96a5c); box-shadow:0 0 12px rgba(224,122,107,0.6), inset 0 0 5px rgba(224,122,107,0.25); }
/* warn — 橙黄系 */
.ai-item.warn {
  background: linear-gradient(135deg, rgba(224,168,92,0.08) 0%, rgba(8,20,40,0.50) 100%);
  border-color: rgba(224,168,92,0.14);
}
.ai-item.warn::before { background:linear-gradient(180deg, #e0a85c, #c99850); box-shadow:0 0 10px rgba(224,168,92,0.55), inset 0 0 4px rgba(224,168,92,0.22); }
/* ok — 青绿系 */
.ai-item.ok {
  background: linear-gradient(135deg, rgba(91,184,138,0.06) 0%, rgba(8,20,40,0.48) 100%);
  border-color: rgba(91,184,138,0.12);
}
.ai-item.ok::before { background:linear-gradient(180deg, #5bb88a, #4aa579); box-shadow:0 0 8px rgba(91,184,138,0.5), inset 0 0 3px rgba(91,184,138,0.18); }

/* 紧急告警红色呼吸微光 — 增强版：边框同步呼吸 */
.ai-item.bad.ai-urg {
  animation: urgBreath 2.5s ease-in-out infinite;
  border-color: rgba(224,122,107,0.28);
}
@keyframes urgBreath {
  0%,100%{box-shadow:0 0 0 0 rgba(224,122,107,0), 0 0 0 0 rgba(224,122,107,0);}
  50%{box-shadow:0 0 16px 2px rgba(224,122,107,0.18), inset 0 0 8px rgba(224,122,107,0.06);}
}

/* 告警等级徽章 — 按 type 类着色（加粗边框+内发光） */
.ai-sev {
  flex-shrink: 0; width:36px; text-align:center; padding:4px 0;
  font-size:10px; font-weight:700; letter-spacing:1px;
  line-height:1.2; margin-top:1px;
  clip-path: polygon(2px 0, 100% 0, 100% calc(100% - 2px), calc(100% - 2px) 100%, 2px 100%, 0 2px);
}
.ai-sev.bad { background:rgba(224,122,107,0.15); color:#e07a6b; border:1px solid rgba(224,122,107,0.28); box-shadow:inset 0 0 4px rgba(224,122,107,0.10); }
.ai-sev.warn { background:rgba(224,168,92,0.13); color:#e0a85c; border:1px solid rgba(224,168,92,0.24); box-shadow:inset 0 0 3px rgba(224,168,92,0.08); }
.ai-sev.ok { background:rgba(91,184,138,0.11); color:#5bb88a; border:1px solid rgba(91,184,138,0.24); box-shadow:inset 0 0 3px rgba(91,184,138,0.08); }

/* 原圆点隐藏（已被 ::before 灯带替代） */
.ai-dot { display:none; }

.ai-txt { flex:1; min-width:0; }
.ai-txt b { font-size:13px; font-weight:700; color:#f0f6ff; display:block; margin-bottom:3px; font-family:"SF Mono","Consolas",monospace; text-shadow:0 0 8px rgba(232,244,255,0.10); line-height:1.25; }
.ai-txt p { font-size:11.5px; line-height:1.55; color:#a0c0e0; margin:0; }
.ai-time { font-size:10px; color:#6088a8; margin-top:5px; display:block; font-family:"SF Mono","Consolas",monospace; }

/* 查看设备按钮 — 切角霓虹按钮（增强版） */
.ai-act {
  flex-shrink: 0; align-self: center; font-size:11px; font-weight:600;
  color:#7ec8ff; cursor: pointer; white-space: nowrap;
  padding:5px 12px;
  clip-path: polygon(4px 0, calc(100% - 4px) 0, 100% 4px, 100% calc(100% - 4px), calc(100% - 4px) 100%, 4px 100%, 0 calc(100% - 4px), 0 4px);
  background: linear-gradient(135deg, rgba(20,55,95,0.35), rgba(14,40,70,0.25));
  border: 1px solid rgba(62,170,255,0.22);
  font-family:"SF Mono","Consolas",monospace;
  transition: all 0.15s;
  box-shadow: inset 0 0 6px rgba(62,170,255,0.04);
}
.ai-act:hover {
  background: linear-gradient(135deg, rgba(25,65,110,0.45), rgba(18,50,85,0.32));
  border-color: rgba(62,170,255,0.42);
  color:#b8e2ff;
  box-shadow: inset 0 0 10px rgba(62,170,255,0.10), 0 0 8px rgba(62,170,255,0.12);
}

/* ══ 告警专业分布 — 双层机械刻度环形仪表盘 ══ */
.ac-split { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; align-items: center; padding: 4px; }
.pf-wrap {
  position:relative; min-height:190px; display:flex; align-items:center; justify-content:center;
  /* 示波器虚线网格背景 — 加深加清晰 */
  background-image:
    repeating-linear-gradient(0deg, transparent 0px, transparent 19px, rgba(90,166,196,0.06) 19px, rgba(90,166,196,0.06) 20px),
    repeating-linear-gradient(90deg, transparent 0px, transparent 19px, rgba(90,166,196,0.06) 19px, rgba(90,166,196,0.06) 20px);
  background-size: 24px 24px;
}
/* 外圈刻度环 — 长短交替细刻度（72根：长12短） */
.pf-wrap::before {
  content:''; position:absolute; inset:0; border-radius:50%;
  pointer-events:none;
  /* 用 conic-gradient 模拟长短交替刻度 */
  background: conic-gradient(
    from 0deg,
    rgba(62,170,255,0.30) 0deg 1.5deg, transparent 1.5deg 3deg,
    rgba(62,170,255,0.15) 3deg 4deg, transparent 4deg 7deg
  );
  -webkit-mask: radial-gradient(transparent calc(100% - 22px), black calc(100% - 21px), black calc(100% - 2px), transparent calc(100% - 1px));
  mask: radial-gradient(transparent calc(100% - 22px), black calc(100% - 21px), black calc(100% - 2px), transparent calc(100% - 1px));
}
/* 外圈辅助细线环 */
.pf-wrap::after {
  content:''; position:absolute; inset:4px; border-radius:50%; pointer-events:none;
  border: 0.5px solid rgba(62,170,255,0.10);
  box-shadow: 0 0 0 1px rgba(62,170,255,0.04);
}
.pf-c { width:100%; height:100%; min-height:180px; }
/* 中心深色圆形信息牌 — 加大加强 */
.pf-center {
  position:absolute; top:50%; left:50%; transform:translate(-50%,-50%);
  text-align:center; pointer-events:none; z-index:2;
  width:80px; height:80px; border-radius:50%;
  background:linear-gradient(180deg, rgba(8,18,32,0.96), rgba(4,10,20,0.94));
  border: 1.5px solid rgba(62,170,255,0.25);
  box-shadow:
    inset 0 0 25px rgba(0,0,0,0.7),
    0 0 16px rgba(0,0,0,0.4),
    0 0 0 1px rgba(62,170,255,0.08),
    inset 0 0 12px rgba(62,170,255,0.05);
  display:flex; flex-direction:column; align-items:center; justify-content:center;
}
.pf-center strong {
  display:block; font-size:28px; font-weight:800;
  font-family:"SF Mono","Consolas",monospace;
  color:#e8f4ff; text-shadow:0 0 16px rgba(62,170,255,0.45), 0 0 4px rgba(62,170,255,0.2); line-height:1;
}
.pf-center span { display:block; font-size:9px; color:#7a98b6; margin-top:3px; letter-spacing:1.5px; }

.pf-list { display: flex; flex-direction: column; gap: 5px; overflow-y: auto; padding-right:4px;
  /* 微弱左侧刻度线装饰 */
  border-left: 1px solid rgba(62,170,255,0.06);
  padding-left: 6px;
}
/* 专业分布行 — 微切角（加厚底色） */
.pf-row {
  display: flex; align-items: center; gap: 6px; padding: 7px 9px;
  clip-path: polygon(4px 0, calc(100% - 4px) 0, 100% 4px, 100% calc(100% - 4px), calc(100% - 4px) 100%, 4px 100%, 0 calc(100% - 4px), 0 4px);
  background: rgba(20,35,60,0.45);
  border: 1px solid rgba(62,170,255,0.09);
  transition: background 0.15s, border-color 0.15s;
}
.pf-row:hover { background: rgba(25,45,75,0.55); border-color: rgba(62,170,255,0.16); }
.pf-ci { width: 3px; height: 20px; border-radius: 1px; flex-shrink: 0; box-shadow: 0 0 7px currentColor, 0 0 2px currentColor; }
.pf-nm { font-size: 11px; color: #c0d8f0; flex: 1; font-family:"SF Mono","Consolas",monospace; }
.pf-row b { font-family: "SF Mono","Consolas",monospace; font-weight: 700; font-size: 15px; color: #e8f0f8; text-shadow:0 0 5px currentColor, 0 0 2px currentColor; }
.pf-pct { font-size: 10px; color: #88a8c8; font-family: "SF Mono","Consolas",monospace; width: 32px; text-align: right; }
/* LED 霓虹光带进度条 — 加高加亮 */
.pf-pg { width: 48px; height: 6px; background: rgba(62,170,255,0.08); border-radius: 2px; overflow: hidden; position:relative; }
.pf-pg > div { height: 100%; border-radius: 2px; transition: width 0.6s ease;
  box-shadow: 0 0 6px currentColor, 0 0 2px currentColor, inset 0 0 3px rgba(255,255,255,0.12); }
/* 上边缘荧光高光线 */
.pf-pg > div::after {
  content:''; position:absolute; top:0; left:0; height:1.5px; width:inherit;
  background:linear-gradient(90deg, transparent, rgba(255,255,255,0.35), transparent);
  border-radius:2px 2px 0 0;
}

/* ══ 告警专业分布 — 左栏（环形+分析结论）+ HUD四角刻度装饰 ══ */
.pf-left { display:flex; flex-direction:column; gap:6px; min-width:0; }
/* 环形四周 HUD 刻度短线 — 四角定位 */
.pf-tl,.pf-tr,.pf-bl,.pf-br {
  position:absolute; width:10px; height:10px; pointer-events:none; z-index:3;
}
.pf-tl { top:-1px; left:-1px; border-top:1.5px solid rgba(62,170,255,0.35); border-left:1.5px solid rgba(62,170,255,0.35); }
.pf-tr { top:-1px; right:-1px; border-top:1.5px solid rgba(62,170,255,0.35); border-right:1.5px solid rgba(62,170,255,0.35); }
.pf-bl { bottom:-1px; left:-1px; border-bottom:1.5px solid rgba(62,170,255,0.35); border-left:1.5px solid rgba(62,170,255,0.35); }
.pf-br { bottom:-1px; right:-1px; border-bottom:1.5px solid rgba(62,170,255,0.35); border-right:1.5px solid rgba(62,170,255,0.35); }

/* 分析研判结论文本框 — 切角深色卡片 */
.pf-analysis {
  padding:8px 10px;
  clip-path: polygon(4px 0, calc(100% - 4px) 0, 100% 4px, 100% calc(100% - 4px), calc(100% - 4px) 100%, 4px 100%, 0 calc(100% - 4px), 0 4px);
  background:linear-gradient(135deg, rgba(8,20,40,0.55), rgba(6,14,30,0.38));
  border:1px solid rgba(62,170,255,0.08);
}
.pfa-hd {
  display:flex; align-items:center; gap:5px;
  font-size:10px; font-weight:700; color:#7ec8ff;
  letter-spacing:1px; margin-bottom:5px;
  font-family:"SF Mono","Consolas",monospace;
}
.pfa-ic { color:#3eaaff; text-shadow:0 0 5px rgba(62,170,255,0.5); }
.pfa-txt {
  font-size:10.5px; line-height:1.55; color:#98b8d8; margin:0 0 6px;
}
/* 分析标签胶囊 */
.pfa-tags { display:flex; gap:5px; flex-wrap:wrap; }
.pfa-tag {
  font-size:9px; padding:1px 8px; letter-spacing:0.3px;
  border:1px solid; border-radius:2px;
  background:rgba(10,22,42,0.4);
  font-family:"SF Mono","Consolas",monospace;
}

/* ══ 关键效能指标 — 切角子卡片 + 饱满 LED 霓虹光带 ══ */
.kpi-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 7px; padding: 8px 7px; align-content: start; }
.kpi-divider { grid-column: 1 / -1; height: 1px; background: linear-gradient(90deg, transparent, rgba(62,170,255,0.12), transparent); margin: 5px 0; }

/* 微型切角子卡片 — 加厚底色 */
.sla-item {
  padding: 9px 10px;
  clip-path: polygon(5px 0, calc(100% - 5px) 0, 100% 5px, 100% calc(100% - 5px), calc(100% - 5px) 100%, 5px 100%, 0 calc(100% - 5px), 0 5px);
  background: linear-gradient(135deg, rgba(10,22,40,0.72), rgba(6,14,30,0.52));
  position: relative; overflow: hidden;
  border: 1px solid rgba(62,170,255,0.10);
  transition: border-color 0.2s, background 0.2s;
}
.sla-item:hover { border-color: rgba(62,170,255,0.20); background: linear-gradient(135deg, rgba(14,28,50,0.80), rgba(8,18,34,0.60)); }
/* 顶部霓虹高光线 */
.sla-item::before { content:''; position:absolute; top:0; left:0; right:0; height:1.5px; background:var(--sc, rgba(62,170,255,0.35)); box-shadow: 0 0 8px var(--sc, rgba(62,170,255,0.22)); }
.sla-item.danger { --sc: rgba(224,122,107,0.55); border-color: rgba(224,122,107,0.14); }
.sla-item.warn { --sc: rgba(224,168,92,0.55); border-color: rgba(224,168,92,0.12); }
.sla-item.info { --sc: rgba(90,166,196,0.55); border-color: rgba(90,166,196,0.11); }
.sla-item.ok { --sc: rgba(91,184,138,0.55); border-color: rgba(91,184,138,0.12); }
.sla-item em { display: block; font-size: 10px; color: #98b8d4; font-style: normal; margin-bottom: 5px; font-weight: 500; letter-spacing: 0.3px; }
.sla-val { display: flex; align-items: baseline; gap: 3px; margin-bottom: 6px; }
.sla-val strong {
  font-size: 21px; font-weight: 700; font-family: "SF Mono","Consolas",monospace;
  text-shadow: 0 0 10px currentColor, 0 0 3px currentColor;
}
.sla-val small { font-size: 11px; font-weight: 500; opacity: 0.6; }
/* LED 霓虹光带进度条 — 加高+边缘荧光 */
.sla-pg { height: 5px; opacity: 0.95; border-radius: 2px; overflow: hidden; background: rgba(62,170,255,0.08); position:relative; }
.sla-pg > div { display: block; height: 100%; border-radius: 2px;
  box-shadow: 0 0 7px currentColor, 0 0 3px currentColor, inset 0 0 3px rgba(255,255,255,0.10);
}

/* ══ 设备风险排名 — 饱满霓虹光带条 + 八角序号 ══ */
.risk-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 11px;
  clip-path: polygon(4px 0, calc(100% - 4px) 0, 100% 4px, 100% calc(100% - 4px), calc(100% - 4px) 100%, 4px 100%, 0 calc(100% - 4px), 0 4px);
  background: linear-gradient(135deg, rgba(10,22,40,0.55), rgba(6,16,30,0.42));
  border: 1px solid rgba(62,170,255,0.08);
  transition: all 0.15s;
  margin-bottom: 5px;
}
.risk-row:hover { background: linear-gradient(135deg, rgba(15,30,54,0.65), rgba(10,20,38,0.50)); border-color: rgba(62,170,255,0.15); }
.risk-row.critical { border-color: rgba(224,122,107,0.20); background: linear-gradient(135deg, rgba(224,122,107,0.07), rgba(8,20,40,0.45)); }
.risk-row.warn { border-color: rgba(224,168,92,0.16); background: linear-gradient(135deg, rgba(224,168,92,0.06), rgba(8,20,40,0.42)); }
/* 八角序号铭牌 — 增强发光 */
.risk-rk {
  width:22px; height:22px; display:flex; align-items:center; justify-content:center;
  font-size:10px; font-weight:800; flex-shrink:0;
  font-family:"SF Mono","Consolas",monospace;
  clip-path: polygon(30% 0%, 70% 0%, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0% 70%, 0% 30%);
  background:rgba(62,170,255,0.12); color:#8ab4d8;
  box-shadow: inset 0 0 4px rgba(62,170,255,0.12);
}
.risk-row.critical .risk-rk { background:rgba(224,122,107,0.16); color:#e07a6b; box-shadow: inset 0 0 4px rgba(224,122,107,0.20), 0 0 6px rgba(224,122,107,0.15); }
.risk-row.warn .risk-rk { background:rgba(224,168,92,0.13); color:#e0a85c; box-shadow: inset 0 0 4px rgba(224,168,92,0.16); }
.risk-nm { font-size:11px; font-weight:600; color:#dcefff; flex:0 0 72px; white-space:nowrap;overflow:hidden;text-overflow:ellipsis; font-family:"SF Mono","Consolas",monospace; }
.risk-dept { font-size:10px; color:#7898b8; flex:0 0 36px; }
.risk-bar-wrap { flex:1; height:7px; background:rgba(62,170,255,0.07); border-radius:2px; overflow:hidden; position:relative; }
/* 霓虹渐变光带条 — 加高+内侧荧光+边缘高光 */
.risk-bar { height:100%; border-radius:2px; transition:width 0.5s ease;
  box-shadow: 0 0 7px var(--rb-c, currentColor), 0 0 3px var(--rb-c, currentColor), inset 0 0 3px rgba(255,255,255,0.08);
}
/* 光带上边缘荧光线 */
.risk-bar::after {
  content:''; position:absolute; top:0; left:0; height:1.5px; border-radius:2px 2px 0 0;
  background:linear-gradient(90deg, transparent, rgba(255,255,255,0.28), transparent);
}
.risk-val {
  font-size:14px; font-weight:700; font-family:"SF Mono","Consolas",monospace;
  flex:0 0 32px; text-align:right;
  text-shadow: 0 0 8px currentColor, 0 0 3px currentColor;
}
.risk-empty { text-align:center; padding:24px 0; color:#5bb88a; font-size:13px; font-weight:500; }

/* ══ 设备风险排名 — 左右双栏布局（列表+分布图+分析）══ */
.risk-body { display:flex; gap:8px; min-height:0; }
.risk-left { flex:1; display:flex; flex-direction:column; gap:4px; min-width:0; overflow-y:auto; padding-right:3px; }
.risk-right {
  width:120px; flex-shrink:0;
  display:flex; flex-direction:column; gap:6px;
  align-items:center; justify-content:flex-start;
  padding-top:4px;
}
/* 右侧分析文字 */
.risk-ana-txt {
  font-size:9.5px; line-height:1.5; color:#88a8c8;
  text-align:center; padding:5px 6px;
  clip-path: polygon(3px 0, calc(100% - 3px) 0, 100% 3px, 100% calc(100% - 3px), calc(100% - 3px) 100%, 3px 100%, 0 calc(100% - 3px), 0 3px);
  background:rgba(10,22,40,0.35); border:1px solid rgba(62,170,255,0.06);
}
/* 健康度分布柱状图 */
.risk-dist-chart { width:100%; height:110px; flex-shrink:0; }
/* 分布图例 */
.dist-legend { display:flex; flex-direction:column; gap:2px; width:100%; }
.dl-item { font-size:8px; color:#7a98b8; display:flex; align-items:center; gap:4px; font-family:"SF Mono","Consolas",monospace; }
.dl-item i { width:8px; height:4px; border-radius:1px; flex-shrink:0; }

/* ══ 预测性分析 — 切角参数铭牌 + 八角发光标签 + 霓虹光带 ══ */
.pred-grid { display:grid; grid-template-columns:1fr 1fr; gap:7px; padding:8px; align-content:start; }
/* 微型切角参数铭牌 — 加厚底色 */
.pred-item {
  padding:11px 11px;
  clip-path: polygon(5px 0, calc(100% - 5px) 0, 100% 5px, 100% calc(100% - 5px), calc(100% - 5px) 100%, 5px 100%, 0 calc(100% - 5px), 0 5px);
  background:linear-gradient(135deg, rgba(10,24,44,0.65), rgba(6,16,30,0.45));
  border:1px solid rgba(62,170,255,0.10);
  display:flex; flex-direction:column; gap:6px;
  transition: transform 0.15s, border-color 0.15s, background 0.15s;
}
.pred-item:hover { transform: translateY(-1.5px); border-color: rgba(62,170,255,0.20); background: linear-gradient(135deg, rgba(14,30,54,0.72), rgba(8,20,38,0.52)); }
.pred-hd { display:flex; justify-content:space-between; align-items:center; }
.pred-hd em { font-size:11px; color:#c0d8f0; font-style:normal; font-weight:500; letter-spacing:0.3px; }
/* 八角发光标签 — 增强内发光 */
.pred-tag {
  font-size:9px; font-weight:700; padding:2px 8px;
  letter-spacing:0.5px;
  clip-path: polygon(25% 0%, 75% 0%, 100% 25%, 100% 75%, 75% 100%, 25% 100%, 0% 75%, 0% 25%);
}
.pred-tag.danger { color:#e07a6b; background:rgba(224,122,107,0.14); border:1px solid rgba(224,122,107,0.22); box-shadow: inset 0 0 4px rgba(224,122,107,0.12); }
.pred-tag.warn { color:#e0a85c; background:rgba(224,168,92,0.11); border:1px solid rgba(224,168,92,0.20); box-shadow: inset 0 0 4px rgba(224,168,92,0.09); }
.pred-tag.ok, .pred-tag.info { color:#5bb88a; background:rgba(91,184,138,0.11); border:1px solid rgba(91,184,138,0.20); box-shadow: inset 0 0 4px rgba(91,184,138,0.09); }
.pred-tag.bad { color:#e07a6b; background:rgba(224,122,107,0.14); border:1px solid rgba(224,122,107,0.22); box-shadow: inset 0 0 4px rgba(224,122,107,0.12); }
/* 核心数值放大发光 */
.pred-val { font-size:18px; font-weight:700; font-family:"SF Mono","Consolas",monospace; text-shadow:0 0 10px currentColor, 0 0 3px currentColor; line-height:1; }
/* 霓虹光带进度条 — 加高+荧光 */
.pred-bar { height:5px; background:rgba(62,170,255,0.07); border-radius:2px; overflow:hidden; position:relative; }
.pred-bar > div { height:100%; border-radius:2px;
  box-shadow: 0 0 7px var(--pb-c, currentColor), 0 0 3px var(--pb-c, currentColor), inset 0 0 3px rgba(255,255,255,0.08);
}
.pred-desc { font-size:9.5px; color:#7898b8; line-height:1.4; }

/* ══ 预测性分析 — 上下分区（铭牌+趋势+风险条）══ */
.pred-body { display:flex; flex-direction:column; gap:6px; min-height:0; }
/* 下部附加区域：趋势曲线 + 风险等级 */
.pred-extra { display:flex; gap:6px; flex:1; min-height:0; }
.pred-trend-chart {
  flex:1; min-width:0; min-height:80px;
}
/* 风险等级横向进度条 */
.pred-risk-level {
  width:110px; flex-shrink:0;
  display:flex; flex-direction:column; gap:3px;
  padding:6px 8px;
  clip-path: polygon(3px 0, calc(100% - 3px) 0, 100% 3px, 100% calc(100% - 3px), calc(100% - 3px) 100%, 3px 100%, 0 calc(100% - 3px), 0 3px);
  background:rgba(10,22,40,0.4); border:1px solid rgba(62,170,255,0.08);
}
.prl-lbl {
  font-size:9px; color:#7a98b8; font-weight:600;
  letter-spacing:0.5px; font-family:"SF Mono","Consolas",monospace;
}
.prl-bar-wrap {
  position:relative; height:6px;
  display:flex; border-radius:2px; overflow:hidden;
  background:rgba(62,170,255,0.04);
}
.prl-seg { flex:1; height:100%; }
.prl-marker {
  position:absolute; top:-2px; width:2px; height:10px;
  border-left:1.5px solid; transform:translateX(-50%);
  box-shadow:0 0 5px currentColor;
  transition:left 0.5s ease;
}
.prl-tags { display:flex; justify-content:space-between; }
.prl-tags em { font-size:7.5px; color:#5a7898; font-style:normal; font-family:"SF Mono","Consolas",monospace; }

/* ══ 综合态势概览 — 切角仪表盘铭牌卡片（按等级区分边框色）══ */
.ov-grid { display:flex; gap:9px; padding:12px 10px; overflow-x:auto; }
.ov-grid::-webkit-scrollbar { height:2.5px; }
.ov-grid::-webkit-scrollbar-thumb { background:rgba(62,170,255,0.20); border-radius:2px; }
/* 小型切角参数铭牌卡片 */
.ov-item {
  flex:1; display:flex; flex-direction:column; align-items:center; gap:4px;
  padding:10px 6px;
  clip-path: polygon(6px 0, calc(100% - 6px) 0, 100% 6px, 100% calc(100% - 6px), calc(100% - 6px) 100%, 6px 100%, 0 calc(100% - 6px), 0 6px);
  background:linear-gradient(180deg, rgba(10,24,44,0.65), rgba(6,14,30,0.48));
  min-width:0; position:relative;
  border: 1px solid rgba(62,170,255,0.12);
  box-shadow:
    inset 0 0 0 0.5px rgba(62,170,255,0.05),
    inset 0 0 12px rgba(62,170,255,0.02);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.ov-item:hover {
  border-color: rgba(62,170,255,0.24);
  box-shadow:
    inset 0 0 0 0.5px rgba(62,170,255,0.08),
    inset 0 0 16px rgba(62,170,255,0.04),
    0 0 8px rgba(62,170,255,0.06);
}
.ov-item em { font-size:10px; color:#98b4d0; font-style:normal; font-weight:500; letter-spacing:0.3px; }
.ov-item strong {
  font-size:23px; font-weight:800; font-family:"SF Mono","Consolas",monospace;
  text-shadow:0 0 12px currentColor, 0 0 4px currentColor; line-height:1;
}
/* 进度条内侧发光 */
.ov-pg { width:100%; height:4px; background:rgba(62,170,255,0.07); border-radius:2px; overflow:hidden; margin-top:2px; position:relative; }
.ov-pg > div { height:100%; border-radius:2px;
  box-shadow: 0 0 7px currentColor, 0 0 3px currentColor, inset 0 0 3px rgba(255,255,255,0.08);
}
.ov-trend { font-size:10px; font-weight:700; font-family:"SF Mono","Consolas",monospace; }
.ov-trend.up { color:#5bb88a; text-shadow:0 0 5px rgba(91,184,138,0.35); }
.ov-trend.down { color:#e07a6b; text-shadow:0 0 5px rgba(224,122,107,0.35); }

/* ══ 综合态势概览 — 上下分区（指标卡片+AI研判面板）══ */
.ov-body { display:flex; flex-direction:column; gap:6px; min-height:0; overflow:hidden; }
/* AI 综合研判分析面板 */
.ov-ai-zone {
  flex:1;
  display:flex; flex-direction:column; gap:4px;
  padding:5px 8px;
  clip-path: polygon(4px 0, calc(100% - 4px) 0, 100% 4px, 100% calc(100% - 4px), calc(100% - 4px) 100%, 4px 100%, 0 calc(100% - 4px), 0 4px);
  background:linear-gradient(135deg, rgba(8,20,40,0.35), rgba(6,14,30,0.20));
  border:1px solid rgba(62,170,255,0.08);
}
.oaz-hd {
  display:flex; align-items:center; gap:4px;
  font-size:9.5px; font-weight:700; color:#7ec8ff;
  letter-spacing:0.5px; flex-shrink:0;
  font-family:"SF Mono","Consolas",monospace;
}
.oaz-ic { color:#5aa6c4; text-shadow:0 0 4px rgba(90,166,196,0.45); }
.oaz-body {
  flex:1; display:flex; flex-direction:column; gap:4px;
}
/* 健康评分行 */
.oaz-score {
  display:flex; align-items:center; gap:8px;
  padding:4px 6px;
  background:linear-gradient(90deg, rgba(62,170,255,0.04), transparent);
  border-left:2.5px solid #5aa6c4;
}
.oaz-label { font-size:9px; color:#7a98b8; white-space:nowrap; }
.oaz-val {
  font-size:22px; font-weight:800;
  font-family:"SF Mono","Consolas",monospace;
  text-shadow:0 0 10px currentColor, 0 0 3px currentColor; line-height:1;
}
.oaz-grade {
  font-size:9px; font-weight:700; letter-spacing:1px;
  padding:1px 8px; border-radius:2px;
  border:1px solid; font-family:"SF Mono","Consolas",monospace;
}
/* AI 结论文本 */
.oaz-conclusion {
  font-size:10.5px; line-height:1.55; color:#b8d0e8;
  padding:5px 8px;
  background:rgba(8,18,32,0.35);
  border:1px solid rgba(62,170,255,0.06);
  border-radius:3px;
}
/* 分析要点列表 */
.oaz-items { display:flex; flex-direction:column; gap:2px; }
.oaz-tip {
  display:flex; align-items:flex-start; gap:6px;
  padding:3px 6px 3px 4px;
  font-size:9.5px; line-height:1.45;
  border-left:2px solid transparent;
  transition:border-color 0.15s, background 0.15s;
}
.oaz-tip.bad { border-color:rgba(224,122,107,0.4); background:rgba(224,122,107,0.04); }
.oaz-tip.warn { border-color:rgba(224,168,92,0.35); background:rgba(224,168,92,0.03); }
.oaz-tip.info { border-color:rgba(90,166,196,0.3); background:rgba(90,166,196,0.03); }
.oaz-tip.ok { border-color:rgba(91,184,138,0.3); background:rgba(91,184,138,0.03); }
.oaz-dot {
  width:5px; height:5px; border-radius:50%; flex-shrink:0; margin-top:4.5px;
}
.oaz-tip.bad .oaz-dot { background:#e07a6b; box-shadow:0 0 5px rgba(224,122,107,0.5); }
.oaz-tip.warn .oaz-dot { background:#e0a85c; box-shadow:0 0 4px rgba(224,168,92,0.45); }
.oaz-tip.info .oaz-dot { background:#5aa6c4; box-shadow:0 0 4px rgba(90,166,196,0.4); }
.oaz-tip.ok .oaz-dot { background:#5bb88a; box-shadow:0 0 4px rgba(91,184,138,0.4); }
.oaz-txt { color:#a8c4e0; }

/* ═══ 滚动条 — 增强 ═══ */
.mtx-grid-wrap::-webkit-scrollbar, .ac-body::-webkit-scrollbar, .pf-list::-webkit-scrollbar,
.ov-grid::-webkit-scrollbar, .pred-grid::-webkit-scrollbar { width: 2.5px; height: 2.5px; }
.mtx-grid-wrap::-webkit-scrollbar-thumb, .ac-body::-webkit-scrollbar-thumb, .pf-list::-webkit-scrollbar-thumb,
.ov-grid::-webkit-scrollbar-thumb, .pred-grid::-webkit-scrollbar-thumb {
  background: rgba(62,170,255,0.22);
  border-radius: 2px;
  box-shadow: inset 0 0 3px rgba(62,170,255,0.15);
}
.mtx-grid-wrap::-webkit-scrollbar-thumb:hover, .ac-body::-webkit-scrollbar-thumb:hover,
.pf-list::-webkit-scrollbar-thumb:hover, .ov-grid::-webkit-scrollbar-thumb:hover,
.pred-grid::-webkit-scrollbar-thumb:hover { background: rgba(62,170,255,0.35); }

/* ══ 模块四角微型 HUD 刻度装饰 — 用 box-shadow 在 L 角外侧延伸短线 ══ */
/* 左上角额外刻度线 */
.ac-insights { --corner-ac: rgba(62,170,255,0.18); }
.ac-prof { --corner-ac: rgba(224,168,92,0.16); }
.ac-kpi { --corner-ac: rgba(90,166,196,0.16); }
.ac-risk { --corner-ac: rgba(224,122,107,0.14); }
.ac-pred { --corner-ac: rgba(91,184,138,0.14); }
.ac-overview { --corner-ac: rgba(167,139,250,0.14); }

/* ═══ 响应式 ═══ */
@media(max-width: 1500px){
  .ana-r1 { grid-template-columns: 1.3fr 1fr 1fr; }
  .ana-r2 { grid-template-columns: 1fr 1fr 1fr; }
}
@media(max-width: 1200px){
  .bs-kpi-bar { flex-wrap: wrap; }
  .trend-zone { grid-template-columns: 1fr; }
  .ana-r1 { grid-template-columns: 1fr 1fr; height:auto; flex:1 1 auto; }
  .ana-r1 > .ana-card:nth-child(3) { grid-column: 1 / -1; }
  .ana-r2 { grid-template-columns: 1fr 1fr; }
  .ov-grid { flex-wrap:wrap; }
  .ov-item { min-width:calc(33% - 6px); }
}
@media(max-width: 900px){
  .bs-hd { flex-direction: column; gap: 6px; }
  .bhd-l, .bhd-r { width: 100%; justify-content: space-between; }
  .bhd-title { font-size: 17px; }
  .bs-kpi-bar { display: none; }
}
</style>
