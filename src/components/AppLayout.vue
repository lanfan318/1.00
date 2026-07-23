<template>
  <div class="shell-wrapper">
    <!-- 素材包(5)全屏边框装饰 -->
    <div class="bs-frame">
      <span class="fc tl"></span>
      <span class="fc tr"></span>
      <span class="fc bl"></span>
      <span class="fc br"></span>
      <span class="ft-bar"></span>
      <span class="fb-bar"></span>
    </div>
    <el-container class="shell">
    <el-aside v-if="!isAgentPage" width="220px" class="sidebar">
      <div class="logo">
        <div class="logo-icon">HF</div>
        <div class="logo-text">HiFire<span> Agent</span></div>
      </div>
      <el-menu
          :default-active="$route.path"
          router
          background-color="transparent"
          text-color="#7a9cc0"
          active-text-color="#3eaaff"
          class="side-menu"
      >
        <el-sub-menu index="monitor">
          <template #title><el-icon><Monitor /></el-icon><span>监控中心</span></template>
          <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon>智能监盘</el-menu-item>
          <el-menu-item index="/global-alert"><el-icon><FullScreen /></el-icon>全域告警大屏</el-menu-item>
          <el-menu-item index="/alarms"><el-icon><Bell /></el-icon>实时报警</el-menu-item>
          <el-menu-item index="/condition"><el-icon><Histogram /></el-icon>工况分析</el-menu-item>
          <el-menu-item index="/diagnosis"><el-icon><Warning /></el-icon>报警诊断</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="manage">
          <template #title><el-icon><Setting /></el-icon><span>设备与配置</span></template>
          <el-menu-item index="/devices"><el-icon><Cpu /></el-icon>设备模型</el-menu-item>
          <el-menu-item index="/alarm-config"><el-icon><Tools /></el-icon>报警配置</el-menu-item>
          <el-menu-item index="/alarm-grade"><el-icon><CollectionTag /></el-icon>报警分级</el-menu-item>
          <el-menu-item index="/stats"><el-icon><PieChart /></el-icon>统计报表</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="ai">
          <template #title><el-icon><MagicStick /></el-icon><span>智能分析</span></template>
          <el-menu-item index="/agent"><el-icon><ChatDotRound /></el-icon>AI 运行智能体</el-menu-item>
          <el-menu-item index="/trace"><el-icon><Connection /></el-icon>故障溯源</el-menu-item>
          <el-menu-item index="/knowledge-graph"><el-icon><Share /></el-icon>知识图谱</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header v-if="!isAgentPage" class="topbar">
        <span class="title">{{ $route.meta?.title || '' }}</span>
        <div class="topbar-right">
          <span class="user-info"><el-icon><UserFilled /></el-icon>{{ userStore.username }} · {{ userStore.role }}</span>
          <span class="status"><span class="dot"></span>系统运行中</span>
          <span class="clock">{{ clock }}</span>
          <el-button link class="logout-btn" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main :class="{'main': !isAgentPage, 'main-agent': isAgentPage}">
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
import { UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isAgentPage = computed(() => route.path === '/agent')

const clock = ref('')
let timer
const update = () => { clock.value = new Date().toLocaleString('zh-CN', { hour12: false }) }
onMounted(() => { update(); timer = setInterval(update, 1000) })
onUnmounted(() => clearInterval(timer))

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.shell-wrapper { position: relative; height: 100vh; }
.bs-frame { position: absolute; inset: 0; pointer-events: none; z-index: 9999; }
.fc { position: absolute; width: 16px; height: 16px; border: 2px solid #3eaaff; box-shadow: 0 0 8px rgba(62,170,255,0.5); }
.fc.tl { top: 8px; left: 8px; border-right: none; border-bottom: none; }
.fc.tr { top: 8px; right: 8px; border-left: none; border-bottom: none; }
.fc.bl { bottom: 8px; left: 8px; border-right: none; border-top: none; }
.fc.br { bottom: 8px; right: 8px; border-left: none; border-top: none; }
.ft-bar { position: absolute; left: 50%; top: 0; width: 240px; height: 2px; transform: translateX(-50%); background: linear-gradient(90deg, transparent, #3eaaff, transparent); box-shadow: 0 0 10px rgba(62,170,255,0.6); }
.fb-bar { position: absolute; left: 50%; bottom: 0; width: 240px; height: 2px; transform: translateX(-50%); background: linear-gradient(90deg, transparent, #3eaaff, transparent); box-shadow: 0 0 10px rgba(62,170,255,0.6); }

.shell { height: 100vh; }
.sidebar {
  background: linear-gradient(180deg, #040810 0%, #061224 50%, #04091a 100%);
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

/* 顶栏: 素材包风格 */
.topbar {
  background: linear-gradient(180deg, #061224 0%, #040810 100%);
  border-bottom: 1px solid rgba(62,170,255,0.1);
  box-shadow: 0 2px 16px rgba(0,0,0,0.2);
  display: flex; align-items: center; padding: 0 22px;
}
.title { font-size: 14px; font-weight: 600; letter-spacing: 0.5px; color: #c8e4ff; }
.topbar-right { margin-left: auto; display: flex; align-items: center; gap: 16px; font-size: 12px; }
.status { color: #a8d4ff; display: flex; align-items: center; gap: 6px; }
.status .dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 6px rgba(52,211,153,0.5);
  animation: pulse 2.5s infinite;
}
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.35} }
.clock { color: #3eaaff; font-family: "SF Mono","Consolas",monospace; letter-spacing: 1px; }
.user-info { color: #c8e4ff; display: flex; align-items: center; gap: 5px; }
.logout-btn { color: #5a7894; font-size: 12px; transition: color 0.2s; }
.logout-btn:hover { color: #f87171; }

.main { background: #040910; padding: 16px 20px; }
.main-agent { padding: 0; background: #040910; }

/* 菜单: cyan 激活态 */
:deep(.el-menu) { background: transparent !important; }
:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:hover) { background: rgba(62,170,255,0.08) !important; }
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(62,170,255,0.18), transparent) !important;
  border-right: 2px solid #3eaaff;
  color: #3eaaff !important;
  text-shadow: 0 0 6px rgba(62,170,255,0.25);
}
:deep(.el-sub-menu__title) { color: #c8e4ff !important; }
:deep(.el-menu) { color: #7a9cc0 !important; }
</style>
