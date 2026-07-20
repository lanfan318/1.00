import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      { path: 'dashboard',         component: () => import('@/views/DashboardView.vue'),          meta: { title: '智能监盘' } },
      { path: 'global-alert',      component: () => import('@/views/GlobalAlertView.vue'),       meta: { title: '全域告警大屏' } },
      { path: 'alarms',            component: () => import('@/views/AlarmListView.vue'),         meta: { title: '实时报警' } },
      { path: 'alarm-config',      component: () => import('@/views/AlarmConfigView.vue'),       meta: { title: '报警配置' } },
      { path: 'alarm-grade',       component: () => import('@/views/AlarmGradeView.vue'),        meta: { title: '报警分级' } },
      { path: 'diagnosis',         component: () => import('@/views/DiagnosisView.vue'),         meta: { title: '报警诊断' } },
      { path: 'condition',         component: () => import('@/views/ConditionAnalysisView.vue'), meta: { title: '工况分析' } },
      { path: 'devices',           component: () => import('@/views/DeviceManageView.vue'),      meta: { title: '设备模型' } },
      { path: 'stats',             component: () => import('@/views/StatsView.vue'),             meta: { title: '统计报表' } },
      { path: 'agent',             component: () => import('@/views/AgentView.vue'),             meta: { title: 'AI 运行智能体' } },
      { path: 'trace',             component: () => import('@/views/TraceView.vue'),             meta: { title: '故障溯源' } },
      { path: 'knowledge-graph',   component: () => import('@/views/KnowledgeGraphView.vue'),    meta: { title: '知识图谱' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login') {
    if (userStore.isLoggedIn()) {
      next('/dashboard')
    } else {
      next()
    }
  } else if (to.matched.some(r => r.meta.requiresAuth)) {
    if (!userStore.isLoggedIn()) {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
