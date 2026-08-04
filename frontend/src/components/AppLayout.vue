<template>
  <div
    class="shell-wrapper"
    :class="{ 'subpage-shell': !isBigScreen, 'big-screen-shell': isBigScreen }"
    :style="{
      '--shell-outer-frame': `url(${outerFrameUrl})`,
      '--shell-panel-frame': `url(${panelFrameUrl})`,
      '--shell-title-rail': `url(${titleRailUrl})`
    }"
  >
    <!-- 素材包(5)全屏边框装饰 -->
    <div v-if="!isBigScreen" class="bs-frame" aria-hidden="true">
      <span class="fc tl"></span>
      <span class="fc tr"></span>
      <span class="fc bl"></span>
      <span class="fc br"></span>
      <span class="ft-bar"></span>
      <span class="fb-bar"></span>
    </div>
    <el-container class="shell">
    <el-container>
      <el-header v-if="!isAgentPage && !isBigScreen" class="topbar">
        <div class="topbar-left">
          <el-button link class="back-btn" @click="goHome"><el-icon><ArrowLeft /></el-icon>返回大屏</el-button>
          <span class="title">{{ $route.meta?.title || '' }}</span>
        </div>
        <div class="topbar-right">
          <span class="user-info"><el-icon><UserFilled /></el-icon>{{ userStore.username }} · {{ userStore.role }}</span>
          <span class="status"><span class="dot"></span>系统运行中</span>
          <span class="clock">{{ clock }}</span>
          <el-button link class="logout-btn" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main :class="{'main': !isAgentPage && !isBigScreen, 'main-agent': isAgentPage, 'main-big': isBigScreen}">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { UserFilled, ArrowLeft } from '@element-plus/icons-vue'
import outerFrameUrl from '@/assets/hud/outer-frame-9slice-clean.webp'
import panelFrameUrl from '@/assets/hud/panel-frame-9slice.webp'
import titleRailUrl from '@/assets/hud/title-rail.png'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isAgentPage = computed(() => route.path === '/agent')
const isBigScreen = computed(() => route.path === '/')

const clock = ref('')
let timer
const update = () => { clock.value = new Date().toLocaleString('zh-CN', { hour12: false }) }
onMounted(() => { update(); timer = setInterval(update, 1000) })
onUnmounted(() => clearInterval(timer))

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
const goHome = () => router.push('/')
</script>

<style scoped>
.shell-wrapper { position: relative; height: 100vh; }
.bs-frame { position: absolute; inset: 0; pointer-events: none; z-index: 9999; }
/* 四角 L 型外框 — 暗蓝 HUD */
.fc { position: absolute; width: 22px; height: 22px; border: 2px solid rgba(0, 170, 255, 0.4); box-shadow: inset 0 0 6px rgba(0, 160, 255, 0.18); }
.fc.tl { top: 8px; left: 8px; border-right: none; border-bottom: none; }
.fc.tr { top: 8px; right: 8px; border-left: none; border-bottom: none; }
.fc.bl { bottom: 8px; left: 8px; border-right: none; border-top: none; }
.fc.br { bottom: 8px; right: 8px; border-left: none; border-top: none; }
/* 顶/底横向装饰条 — 暗蓝渐隐 */
.ft-bar { position: absolute; left: 50%; top: 0; width: 240px; height: 2px; transform: translateX(-50%); background: linear-gradient(90deg, transparent, rgba(0,170,255,0.5), transparent); box-shadow: 0 0 8px rgba(0,160,255,0.25); }
.fb-bar { position: absolute; left: 50%; bottom: 0; width: 240px; height: 2px; transform: translateX(-50%); background: linear-gradient(90deg, transparent, rgba(0,170,255,0.5), transparent); box-shadow: 0 0 8px rgba(0,160,255,0.25); }

.shell { height: 100vh; }
.sidebar {
  background: linear-gradient(180deg, #020610 0%, #030c1b 50%, #020611 100%);
  border-right: 1px solid rgba(62,170,255,0.14);
  box-shadow: 2px 0 24px rgba(0,0,0,0.3);
}
.logo {
  display: flex; align-items: center; gap: 10px; padding: 22px 16px;
  border-bottom: 1px solid rgba(62,170,255,0.14);
  background: linear-gradient(180deg, rgba(62,170,255,0.06) 0%, transparent 100%);
}
/* Logo: 六边形图标 (参考 icon.png) */
.logo-icon {
  width: 38px; height: 42px;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: linear-gradient(135deg, rgba(62,170,255,0.7) 0%, rgba(30,100,180,0.5) 100%);
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 800; color: #fff; letter-spacing: -1px;
  box-shadow: 0 0 12px rgba(62,170,255,0.35);
}
.logo-text { font-size: 15px; font-weight: 700; letter-spacing: 0.5px; color: #e0f0ff; }
.logo-text span { color: #3eaaff; }

.side-menu { border: none; }

/* 顶栏: 暗蓝 HUD 标题栏 */
.topbar {
  background: linear-gradient(180deg, rgba(0, 16, 34, 0.5), rgba(0, 9, 20, 0.42));
  border-bottom: 1px solid rgba(30, 74, 112, 0.7);
  box-shadow: inset 0 1px 0 rgba(0, 170, 255, 0.12), 0 2px 16px rgba(0, 0, 0, 0.25);
  display: flex; align-items: center; padding: 0 22px;
  position: relative;
}
.topbar::before {
  content: ''; position: absolute; left: 0; top: 0; width: 12px; height: 100%;
  background: linear-gradient(90deg, rgba(0, 170, 255, 0.4), transparent);
  clip-path: polygon(0 0, 100% 25%, 100% 75%, 0 100%);
}
.topbar-left { display: flex; align-items: center; gap: 12px; }
.back-btn { color: #7ec8ff; font-size: 13px; font-weight: 600; transition: all 0.2s; display: flex; align-items: center; gap: 4px; padding: 6px 12px; border: 1px solid rgba(0, 170, 255, 0.28); border-radius: 4px; background: rgba(0, 170, 255, 0.06); }
.back-btn:hover { color: #c8e8ff; background: rgba(0, 170, 255, 0.14); border-color: rgba(0, 170, 255, 0.5); }
.title { font-size: 14px; font-weight: 600; letter-spacing: 0.5px; color: #d4ecff; position: absolute; left: 50%; transform: translateX(-50%); white-space: nowrap; }
.topbar-right { margin-left: auto; display: flex; align-items: center; gap: 16px; font-size: 12px; }
.status { color: #a8d4ff; display: flex; align-items: center; gap: 6px; }
.status .dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--alarm-green);
  box-shadow: 0 0 6px rgba(91, 184, 138, 0.5);
  animation: pulse 2.5s infinite;
}
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.35} }
.clock { color: #7ec8ff; font-family: "SF Mono","Consolas",monospace; letter-spacing: 1px; }
.user-info { color: #c8e4ff; display: flex; align-items: center; gap: 5px; }
.logout-btn { color: #5a7894; font-size: 12px; transition: color 0.2s; }
.logout-btn:hover { color: var(--alarm-red); }

.main { background: #02060e; padding: 16px 20px; height: 100%; overflow: hidden; }
.main-agent { padding: 0; background: #02060e; }
.main-big { padding: 0; background: transparent; }

/* 菜单: 暗蓝 HUD 激活态 */
:deep(.el-menu) { background: transparent !important; }
:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:hover) { background: rgba(0, 170, 255, 0.08) !important; }
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(0, 170, 255, 0.16), transparent) !important;
  border-right: 2px solid var(--hud-blue-2);
  color: #7ec8ff !important;
  text-shadow: 0 0 6px rgba(0, 160, 255, 0.22);
}
:deep(.el-sub-menu__title) { color: #c8e4ff !important; }
:deep(.el-menu) { color: #7a9cc0 !important; }

/* ═══ 子页面共享贴图框架 ═══ */
.subpage-shell {
  background:
    repeating-linear-gradient(0deg, transparent 0 31px, rgba(57, 151, 206, 0.03) 31px 32px),
    repeating-linear-gradient(90deg, transparent 0 31px, rgba(57, 151, 206, 0.03) 31px 32px),
    #01060e;
  overflow: hidden;
}
.subpage-shell .bs-frame {
  inset: 0;
  z-index: 9999;
  border: 26px solid transparent;
  border-image-source: var(--shell-outer-frame);
  border-image-slice: 12 28 8 28;
  border-image-width: 18px 30px 12px;
  border-image-repeat: stretch;
  border-image-outset: 0;
  box-sizing: border-box;
  filter: brightness(0.82) drop-shadow(0 0 8px rgba(38, 164, 226, 0.32));
}
.subpage-shell .bs-frame > * {
  display: none !important;
}
.subpage-shell .shell {
  height: 100vh;
  padding: 10px 30px 12px;
  box-sizing: border-box;
  background: transparent;
}
.subpage-shell .shell > .el-container {
  min-width: 0;
  min-height: 0;
}
.subpage-shell .topbar {
  height: 62px;
  padding: 0 28px;
  background-color: transparent;
  background-image: var(--shell-title-rail);
  background-repeat: no-repeat;
  background-position: center;
  background-size: 100% 100%;
  border: 0;
  box-shadow: none;
}
.subpage-shell .topbar::before {
  display: none;
}
.subpage-shell .title {
  color: #e5f4ff;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 2px;
  text-shadow: 0 0 8px rgba(67, 174, 238, 0.42);
}
.subpage-shell .main {
  padding: 10px 8px 8px;
  background: transparent;
  min-width: 0;
  min-height: 0;
}
.subpage-shell .main-agent {
  padding: 8px;
  background: transparent;
  min-width: 0;
  min-height: 0;
}
.subpage-shell :deep(.ai-page),
.subpage-shell :deep(.bs) {
  height: 100% !important;
  min-height: 0;
  box-sizing: border-box;
}

/* 页面内部标题横框（报警诊断 .dg-head 不需要，与外壳框架重复） */
.subpage-shell :deep(.ag-head),
.subpage-shell :deep(.ca-head),
.subpage-shell :deep(.tr-head),
.subpage-shell :deep(.ai-top),
.subpage-shell :deep(.bs-head) {
  background-color: transparent !important;
  background-image: var(--shell-title-rail) !important;
  background-repeat: no-repeat !important;
  background-position: center !important;
  background-size: 100% 100% !important;
  border: 0 !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  box-sizing: border-box;
}

/* 主要业务面板统一使用同一张九宫格贴图，不改变其原尺寸与内部布局 */
.subpage-shell :deep(.cd),
.subpage-shell :deep(.ac-left),
.subpage-shell :deep(.ac-right),
.subpage-shell :deep(.g-card),
.subpage-shell :deep(.als-card),
.subpage-shell :deep(.guide),
.subpage-shell :deep(.ca-ctrls),
.subpage-shell :deep(.ca-kpi),
.subpage-shell :deep(.unit-panel),
.subpage-shell :deep(.up-card),
.subpage-shell :deep(.dept-switch),
.subpage-shell :deep(.bs-unit-panel),
.subpage-shell :deep(.bs-up-card),
.subpage-shell :deep(.kpi),
.subpage-shell :deep(.kg-guide),
.subpage-shell :deep(.gcol),
.subpage-shell :deep(.mo-kpi),
.subpage-shell :deep(.mw-timebar),
.subpage-shell :deep(.mw-kpi),
.subpage-shell :deep(.mw-ai-panel),
.subpage-shell :deep(.stats-overview),
.subpage-shell :deep(.sc),
.subpage-shell :deep(.ratio-card),
.subpage-shell :deep(.chart-panel),
.subpage-shell :deep(.insight-block),
.subpage-shell :deep(.case-bar),
.subpage-shell :deep(.case-card),
.subpage-shell :deep(.ai-sb),
.subpage-shell :deep(.ai-main),
.subpage-shell :deep(.ai-card),
.subpage-shell :deep(.ai-chart),
.subpage-shell :deep(.ai-qb) {
  position: relative;
  background-color: #03101e !important;
  background-image: var(--shell-panel-frame) !important;
  background-repeat: no-repeat !important;
  background-position: center !important;
  background-size: 100% 100% !important;
  border-color: transparent !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  box-sizing: border-box;
}


/* 旧 CSS 角标由贴图接管，避免双重边框 */
.subpage-shell :deep(.cd::before),
.subpage-shell :deep(.cd::after) {
  display: none !important;
}

@media (max-width: 1100px) {
  .subpage-shell .shell {
    padding-right: 24px;
    padding-left: 24px;
  }
  .subpage-shell .topbar-right {
    gap: 8px;
  }
  .subpage-shell .user-info {
    display: none;
  }
}
@media (max-height: 820px) and (min-width: 901px) {
  .subpage-shell :deep(.dash-wrap) {
    gap: 6px;
  }
  .subpage-shell :deep(.unit-panel) {
    padding: 8px 12px !important;
  }
  .subpage-shell :deep(.up-header) {
    margin-bottom: 4px;
  }
  .subpage-shell :deep(.up-c1),
  .subpage-shell :deep(.up-metric) {
    padding-top: 7px;
    padding-bottom: 7px;
  }
  .subpage-shell :deep(.dept-switch) {
    padding-top: 4px;
    padding-bottom: 4px;
  }
  .subpage-shell :deep(.dash-metric-panel) {
    padding: 9px !important;
  }
  .subpage-shell :deep(.dash-metric-panel .cd-t) {
    min-height: 28px;
    padding-top: 5px !important;
    padding-bottom: 5px !important;
    margin-bottom: 5px !important;
  }
  .subpage-shell :deep(.metric-grid) {
    gap: 4px;
    padding: 2px 4px 3px;
  }
  .subpage-shell :deep(.metric-grid .mc) {
    padding: 5px 4px;
  }
  .subpage-shell :deep(.dash-health-row) {
    min-height: 132px;
  }
  .subpage-shell :deep(.insight) {
    max-height: 94px;
    padding: 9px 12px !important;
  }
  .subpage-shell :deep(.insight .cd-t) {
    min-height: 28px;
    padding-top: 5px !important;
    padding-bottom: 5px !important;
    margin-bottom: 4px !important;
  }
}
</style>
