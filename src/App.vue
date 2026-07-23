<template>
  <div id="app-root">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
/* ==========================================
   火电智能体 — 全局科技感背景
   ========================================== */
html, body, #app { height: 100%; margin: 0; padding: 0; }
body {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
  background: #050a14;
  color: #e2e8f0;
}

/* 全局暗底 */
body {
  position: relative;
}
body::before {
  content: '';
  position: fixed; inset: 0;
  background:
    radial-gradient(ellipse 80% 60% at 50% 50%, rgba(59,130,246,0.06) 0%, transparent 70%),
    radial-gradient(ellipse 40% 40% at 20% 10%, rgba(251,146,60,0.04) 0%, transparent 60%),
    radial-gradient(ellipse 35% 45% at 85% 90%, rgba(6,182,212,0.03) 0%, transparent 60%),
    linear-gradient(180deg, #060d1a 0%, #0a1628 50%, #050e1c 100%);
  z-index: -2;
  pointer-events: none;
}

/* 科技网格 */
body::after {
  content: '';
  position: fixed; inset: 0;
  background-image:
    linear-gradient(rgba(30,41,59,0.25) 1px, transparent 1px),
    linear-gradient(90deg, rgba(30,41,59,0.25) 1px, transparent 1px);
  background-size: 80px 80px;
  z-index: -2;
  pointer-events: none;
}

/* 扫描线 — 放在 #app-root 上 */
@keyframes scan-line {
  0% { top: -2px; }
  100% { top: 100%; }
}
#app-root {
  position: relative; min-height: 100vh;
}
#app-root::after {
  content: '';
  position: fixed; top: -2px; left: 0; right: 0; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(59,130,246,0.12), transparent);
  z-index: 10000; pointer-events: none;
  animation: scan-line 8s ease-in-out infinite;
}

/* 侧边蓝光条 */
#app-root::before {
  content: '';
  position: fixed; top: 0; left: 0; bottom: 0; width: 1px;
  background: linear-gradient(180deg, rgba(59,130,246,0), rgba(59,130,246,0.3) 40%, rgba(59,130,246,0.6) 50%, rgba(59,130,246,0.3) 60%, rgba(59,130,246,0));
  z-index: 10000; pointer-events: none;
}
</style>
