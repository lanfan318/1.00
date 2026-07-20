<template>
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
          text-color="#94a3b8"
          active-text-color="#3b82f6"
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
.shell { height: 100vh; }
.sidebar { background: #0d1117; border-right: 0.5px solid #1e293b; }
.logo { display: flex; align-items: center; gap: 10px; padding: 20px 16px; border-bottom: 0.5px solid #1e293b; }
.logo-icon { width: 34px; height: 34px; background: linear-gradient(135deg, #3b82f6, #6366f1); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 700; color: #fff; }
.logo-text { font-size: 15px; font-weight: 600; }
.logo-text span { color: #3b82f6; }
.side-menu { border: none; }
.topbar { background: #0d1117; border-bottom: 0.5px solid #1e293b; display: flex; align-items: center; padding: 0 20px; }
.title { font-size: 14px; font-weight: 500; }
.topbar-right { margin-left: auto; display: flex; align-items: center; gap: 16px; font-size: 12px; }
.status { color: #94a3b8; display: flex; align-items: center; gap: 6px; }
.status .dot { width: 8px; height: 8px; border-radius: 50%; background: #22c55e; animation: pulse 2s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.4} }
.clock { color: #94a3b8; font-family: monospace; }
.user-info { color: #94a3b8; display: flex; align-items: center; gap: 5px; }
.logout-btn { color: #64748b; font-size: 12px; }
.logout-btn:hover { color: #ef4444; }
.main { background: #0a0e17; padding: 16px 20px; }
.main-agent { padding: 0; background: #fff; }
:deep(.el-menu) { background: transparent !important; }
:deep(.el-sub-menu__title:hover), :deep(.el-menu-item:hover) { background: #1a2332 !important; }
:deep(.el-menu-item.is-active) { background: rgba(59,130,246,0.12) !important; }
</style>
